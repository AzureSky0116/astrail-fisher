package dev.astrail.client.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.astrail.client.api.module.ClientModule;
import dev.astrail.client.api.module.ModuleState;
import dev.astrail.client.api.setting.Setting;
import dev.astrail.client.api.setting.KeybindSetting;
import dev.astrail.client.core.module.ModuleManager;
import dev.astrail.client.api.module.DisableReason;
import dev.astrail.client.render.hud.HudLayout;
import dev.astrail.client.ui.state.UiPreferences;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Astrail/Config");
    private static final int SCHEMA_VERSION = 5;
    private static final int MAX_QUARANTINE_FILES = 20;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ModuleManager modules;
    private final Path configFile;
    private final UiPreferences uiPreferences;
    private final HudLayout hudLayout;
    private JsonObject document = new JsonObject();
    private String activeProfile = "default";

    /** Set when the on-disk config belongs to a newer build; the session never writes. */
    private boolean saveBlocked;
    private boolean saveFailureLogged;

    /** Set by {@link #markDirty()}; drained by {@link #flushIfDirty()}. */
    private volatile boolean dirty;

    /**
     * Records that in-memory state has drifted from disk, without writing.
     *
     * <p>Routine UI traffic — module toggles, checkbox clicks, dropdown picks —
     * used to run a full serialize + fsync + backup copy synchronously on the
     * render thread for every interaction. Those paths now mark and return; the
     * runtime flushes on a timer, and moments that must not lose data (screen
     * close, profile switch, client shutdown) still call {@link #save()} directly.
     */
    public void markDirty() {
        dirty = true;
    }

    /** Writes at most once per call, and only if something changed since the last write. */
    public synchronized void flushIfDirty() {
        if (!dirty) {
            return;
        }
        dirty = false;
        save();
    }

    public ConfigManager(Path configDirectory, ModuleManager modules, UiPreferences uiPreferences, HudLayout hudLayout) {
        this.modules = modules;
        this.uiPreferences = uiPreferences;
        this.hudLayout = hudLayout;
        this.configFile = configDirectory.resolve("astrail").resolve("config.json");
    }

    public synchronized void load() {
        if (!Files.exists(configFile)) {
            return;
        }
        JsonObject root = readDocument(configFile);
        if (root == null) {
            // The rolling backup is one save behind, which is a far better outcome
            // than defaults. It was written on every save and never read until now.
            root = readDocument(backupFile());
            if (root != null) LOGGER.warn("Recovered configuration from {}", backupFile());
        }
        if (root == null) {
            quarantineUnreadableConfig();
            return;
        }

        int schemaVersion;
        try {
            schemaVersion = root.has("schemaVersion") ? root.get("schemaVersion").getAsInt() : 0;
        } catch (RuntimeException error) {
            LOGGER.error("Ignoring unreadable schema version in {}", configFile, error);
            schemaVersion = 0;
        }
        if (schemaVersion > SCHEMA_VERSION) {
            // A config written by a newer build is intact, not corrupt. Quarantining
            // or overwriting it would destroy a working configuration the moment the
            // user downgraded once, so the session runs on defaults and stays
            // read-only until they launch the newer build again.
            saveBlocked = true;
            LOGGER.error(
                "Config schema {} is newer than this client ({}); running with defaults and saving is disabled",
                schemaVersion, SCHEMA_VERSION
            );
            return;
        }

        try {
            document = migrateToProfiles(root);
            activeProfile = readActiveProfile(document);
            ensureProfile(activeProfile);
        } catch (RuntimeException error) {
            LOGGER.error("Failed to prepare config document: {}", configFile, error);
            quarantineUnreadableConfig();
            return;
        }

        // Each section is recovered independently: a hand-edited HUD block or a
        // stale UI key must not cost the user every module setting they own.
        int version = schemaVersion;
        loadSection("ui", () -> loadUi(document));
        loadSection("hud", () -> loadHud(document));
        loadSection("modules", () -> loadModuleObjects(activeModules(), version));
    }

    private JsonObject readDocument(Path path) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                LOGGER.error("Config is not a JSON object: {}", path);
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (IOException | RuntimeException error) {
            LOGGER.error("Failed to read config: {}", path, error);
            return null;
        }
    }

    private static String readActiveProfile(JsonObject document) {
        JsonElement active = document.get("activeProfile");
        return active != null && active.isJsonPrimitive() && active.getAsJsonPrimitive().isString()
            ? active.getAsString()
            : "default";
    }

    private void loadSection(String name, Runnable section) {
        try {
            section.run();
        } catch (RuntimeException error) {
            LOGGER.error("Ignoring unreadable config section: {}", name, error);
        }
    }

    /**
     * Moves a config the client could not parse out of the way instead of letting
     * the next save overwrite it.
     *
     * <p>Without this, one malformed file cost the user everything: the load left
     * the in-memory document empty, the next module toggle serialized defaults over
     * the original, and the rolling {@code .bak} had already been replaced by a copy
     * of the same broken file. Renaming it preserves whatever is recoverable and
     * lets the client start from defaults.
     */
    private void quarantineUnreadableConfig() {
        for (int attempt = 1; attempt <= MAX_QUARANTINE_FILES; attempt++) {
            Path target = configFile.resolveSibling(configFile.getFileName() + ".corrupt-" + attempt);
            if (Files.exists(target)) {
                continue;
            }
            try {
                Files.move(configFile, target);
                LOGGER.error("Moved unreadable config aside: {}", target);
            } catch (IOException error) {
                LOGGER.error("Could not preserve unreadable config: {}", configFile, error);
            }
            return;
        }
        LOGGER.error("Too many quarantined configs; leaving {} in place", configFile);
    }

    public synchronized void save() {
        if (saveBlocked) {
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            activeProfileObject().add("modules", serializeModules());
            document.addProperty("schemaVersion", SCHEMA_VERSION);
            document.addProperty("activeProfile", activeProfile);
            document.add("ui", serializeUi());
            document.add("hud", serializeHud());
            writeAtomically(gson.toJson(document));
            saveFailureLogged = false;
        } catch (Exception error) {
            // Repeated identical stack traces every time the user touches a setting
            // drown the log, so only the first failure of a run carries one.
            if (saveFailureLogged) {
                LOGGER.error("Failed to save config: {} ({})", configFile, error.toString());
            } else {
                saveFailureLogged = true;
                LOGGER.error("Failed to save config: {}", configFile, error);
            }
        }
    }

    public synchronized String activeProfile() {
        return activeProfile;
    }

    public synchronized List<String> profiles() {
        ensureDocument();
        ArrayList<String> result = new ArrayList<>();
        document.getAsJsonObject("profiles").keySet().forEach(result::add);
        return List.copyOf(result);
    }

    public synchronized boolean createProfile(String name, boolean copyCurrent) {
        String id = normalizeProfileName(name);
        ensureDocument();
        JsonObject profiles = document.getAsJsonObject("profiles");
        if (profileKey(id) != null) return false;
        JsonObject profile = new JsonObject();
        profile.add("modules", copyCurrent ? serializeModules() : new JsonObject());
        profiles.add(id, profile);
        return true;
    }

    /**
     * Renames a stored profile, keeping its settings, its position in the list
     * and following the active pointer. Returns false when the source is
     * missing, the target name is invalid, or another profile already holds
     * that name (ignoring case).
     *
     * <p>The list position is rebuilt rather than letting the key move to the
     * end: the manager renames live on every keystroke, and a row that jumped
     * to the bottom of the list mid-word was unusable.
     *
     * <p>Marks the config dirty instead of writing immediately, for the same
     * reason — one write per keystroke is wasted I/O. The manager saves on
     * close and the flush timer covers everything else.
     */
    public synchronized boolean renameProfile(String from, String to) {
        ensureDocument();
        String target;
        try {
            target = normalizeProfileName(to);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        JsonObject profiles = document.getAsJsonObject("profiles");
        if (!profiles.has(from)) return false;
        String clash = profileKey(target);
        if (clash != null && !clash.equals(from)) return false;
        if (target.equals(from)) return true;
        JsonObject reordered = new JsonObject();
        for (String key : List.copyOf(profiles.keySet())) {
            if (key.equals(from)) reordered.add(target, profiles.get(from));
            else reordered.add(key, profiles.get(key));
        }
        document.add("profiles", reordered);
        if (activeProfile.equals(from)) activeProfile = target;
        markDirty();
        return true;
    }

    /**
     * Deletes a stored profile. The last remaining profile is never deleted;
     * deleting the active profile first switches to another one, so the client
     * is never left without live settings.
     */
    public synchronized boolean deleteProfile(String name) {
        ensureDocument();
        JsonObject profiles = document.getAsJsonObject("profiles");
        if (!profiles.has(name) || profiles.keySet().size() <= 1) return false;
        if (activeProfile.equals(name)) {
            String fallback = profiles.keySet().stream()
                .filter(other -> !other.equals(name))
                .findFirst()
                .orElse(null);
            if (fallback == null || !switchProfile(fallback)) return false;
        }
        profiles.remove(name);
        save();
        return true;
    }

    /**
     * Duplicates a stored profile's saved settings under a new name — the
     * source profile's on-disk state, not the live settings (which may belong
     * to a different active profile).
     */
    public synchronized boolean copyProfile(String source, String newName) {
        ensureDocument();
        String target;
        try {
            target = normalizeProfileName(newName);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        JsonObject profiles = document.getAsJsonObject("profiles");
        if (!profiles.has(source) || profileKey(target) != null) return false;
        if (source.equals(activeProfile)) {
            // The active profile's stored copy may lag the live settings.
            activeProfileObject().add("modules", serializeModules());
        }
        profiles.add(target, profiles.get(source).deepCopy());
        save();
        return true;
    }

    public synchronized boolean switchProfile(String name) {
        ensureDocument();
        String id = profileKey(normalizeProfileName(name));
        if (id == null || id.equals(activeProfile)) return false;
        // Malformed data in the target profile surfaces here, on a click handler,
        // where an escaped exception crashes the whole client via the screen event
        // pipeline. Failing partway leaves the new profile active with defaults,
        // and the save below then rewrites it as clean serialized state.
        try {
            activeProfileObject().add("modules", serializeModules());
            modules.disableAll(DisableReason.USER);
            modules.all().forEach(module -> module.settings().forEach(Setting::reset));
            activeProfile = id;
            ensureProfile(activeProfile);
            loadModuleObjects(activeModules(), SCHEMA_VERSION);
        } catch (RuntimeException error) {
            LOGGER.error("Failed to switch to profile: {}", id, error);
        }
        save();
        return true;
    }

    public UiPreferences uiPreferences() {
        return uiPreferences;
    }

    public HudLayout hudLayout() {
        return hudLayout;
    }


    private void loadModuleObjects(JsonObject moduleObjects, int schemaVersion) {
        for (ClientModule module : modules.all()) {
            String id = module.metadata().id();
            JsonElement entry = moduleObjects.get(id);
            if (entry == null || !entry.isJsonObject()) continue;
            // One malformed module entry must not cost the modules after it: the
            // registration order is stable, so without this guard everything past
            // the bad entry silently fell back to defaults.
            try {
                loadModule(module, entry.getAsJsonObject(), schemaVersion);
            } catch (RuntimeException error) {
                LOGGER.warn("Ignoring invalid module config: {}", id, error);
            }
        }
    }

    private void loadModule(ClientModule module, JsonObject object, int schemaVersion) {
        JsonElement rawSettings = object.get("settings");
        JsonObject settingValues = rawSettings != null && rawSettings.isJsonObject()
            ? rawSettings.getAsJsonObject()
            : new JsonObject();
        for (Setting<?> setting : module.settings()) {
            JsonElement value = settingValues.get(setting.id());
            if (value == null && setting.id().equals("real_nuker")) {
                value = settingValues.get("packet_mining");
            }
            if (value == null) {
                continue;
            }
            try {
                if (module.metadata().id().equals("macro.nuker") && setting.id().equals("mode")) {
                    value = migrateNukerModes(value);
                }
                if (schemaVersion < 2
                    && module.metadata().id().equals("macro.nuker")
                    && setting instanceof KeybindSetting
                    && value.getAsInt() == GLFW.GLFW_KEY_N) {
                    setting.reset();
                    continue;
                }
                setting.fromJson(value);
            } catch (RuntimeException error) {
                LOGGER.warn("Ignoring invalid setting: module={}, setting={}", module.metadata().id(), setting.id(), error);
            }
        }
        JsonElement enabled = object.get("enabled");
        if (module.metadata().persistEnabled()
            && enabled != null
            && enabled.isJsonPrimitive()
            && enabled.getAsJsonPrimitive().isBoolean()
            && enabled.getAsBoolean()) {
            try {
                module.enable();
            } catch (RuntimeException error) {
                LOGGER.error("Failed to restore enabled module: {}", module.metadata().id(), error);
            }
        }
    }

    static JsonElement migrateNukerModes(JsonElement value) {
        if (!value.isJsonPrimitive() && !value.isJsonArray()) {
            return value;
        }
        Set<String> migrated = new LinkedHashSet<>();
        if (value.isJsonPrimitive()) {
            addMigratedNukerMode(migrated, value.getAsString());
        } else {
            for (JsonElement element : value.getAsJsonArray()) {
                addMigratedNukerMode(migrated, element.getAsString());
            }
        }
        JsonArray result = new JsonArray();
        migrated.forEach(result::add);
        return result;
    }

    private static void addMigratedNukerMode(Set<String> result, String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        switch (normalized) {
            case "stone_with_cobblestone" -> {
                result.add("stone");
                result.add("cobblestone");
            }
            case "mithril_with_titanium" -> {
                result.add("mithril");
                result.add("titanium");
            }
            case "gemstone_with_pane" -> result.add("gemstone");
            case "gold" -> result.add("gold_block");
            case "diamond" -> result.add("diamond_block");
            default -> result.add(normalized);
        }
    }

    private JsonObject serializeModule(ClientModule module) {
        JsonObject object = new JsonObject();
        object.addProperty(
            "enabled",
            module.metadata().persistEnabled() && module.wantsPersistedEnabled()
        );
        JsonObject settingValues = new JsonObject();
        for (Setting<?> setting : module.settings()) {
            settingValues.add(setting.id(), setting.toJson());
        }
        object.add("settings", settingValues);
        return object;
    }

    private JsonObject serializeModules() {
        JsonObject moduleObjects = new JsonObject();
        for (ClientModule module : modules.all()) {
            moduleObjects.add(module.metadata().id(), serializeModule(module));
        }
        return moduleObjects;
    }

    private JsonObject migrateToProfiles(JsonObject root) {
        if (root.has("profiles") && root.get("profiles").isJsonObject()) {
            return root.deepCopy();
        }
        JsonObject migrated = new JsonObject();
        migrated.addProperty("schemaVersion", SCHEMA_VERSION);
        migrated.addProperty("activeProfile", "default");
        JsonObject profile = new JsonObject();
        profile.add("modules", root.has("modules") ? root.getAsJsonObject("modules").deepCopy() : new JsonObject());
        JsonObject profiles = new JsonObject();
        profiles.add("default", profile);
        migrated.add("profiles", profiles);
        if (root.has("ui")) migrated.add("ui", root.get("ui").deepCopy());
        if (root.has("hud")) migrated.add("hud", root.get("hud").deepCopy());
        return migrated;
    }

    private void ensureDocument() {
        if (!document.has("profiles")) {
            document.addProperty("schemaVersion", SCHEMA_VERSION);
            document.addProperty("activeProfile", activeProfile);
            document.add("profiles", new JsonObject());
        }
        ensureProfile(activeProfile);
    }

    /**
     * Guarantees {@code profiles.<name>.modules} exists and has the right JSON
     * type at every level.
     *
     * <p>Replaces malformed nodes (a profile that is a string, a {@code modules}
     * that is an array) instead of casting into them. A bare cast made one
     * hand-edited entry poison the whole session: {@code save()} went through
     * {@code ensureDocument()}, the ClassCastException landed in its catch-all,
     * and every save for the rest of the run silently wrote nothing.
     */
    private void ensureProfile(String name) {
        JsonElement rawProfiles = document.get("profiles");
        JsonObject profiles;
        if (rawProfiles == null || !rawProfiles.isJsonObject()) {
            profiles = new JsonObject();
            document.add("profiles", profiles);
        } else {
            profiles = rawProfiles.getAsJsonObject();
        }
        JsonElement existing = profiles.get(name);
        if (existing == null || !existing.isJsonObject()) {
            JsonObject profile = new JsonObject();
            profile.add("modules", new JsonObject());
            profiles.add(name, profile);
            return;
        }
        JsonObject profile = existing.getAsJsonObject();
        JsonElement moduleObjects = profile.get("modules");
        if (moduleObjects == null || !moduleObjects.isJsonObject()) {
            profile.add("modules", new JsonObject());
        }
    }

    private JsonObject activeProfileObject() {
        ensureDocument();
        return document.getAsJsonObject("profiles").getAsJsonObject(activeProfile);
    }

    private JsonObject activeModules() {
        return activeProfileObject().getAsJsonObject("modules");
    }

    private void loadUi(JsonObject root) {
        if (!root.has("ui") || !root.get("ui").isJsonObject()) return;
        JsonObject ui = root.getAsJsonObject("ui");
        uiPreferences.replaceFavorites(readKnownModuleIds(ui, "favorites"));
        uiPreferences.replaceRecent(readKnownModuleIds(ui, "recent"));
    }

    private List<String> readKnownModuleIds(JsonObject parent, String key) {
        return readStringArray(parent, key).stream()
            .filter(id -> modules.find(id).isPresent())
            .toList();
    }

    private static List<String> readStringArray(JsonObject parent, String key) {
        List<String> result = new ArrayList<>();
        if (parent.has(key) && parent.get(key).isJsonArray()) {
            for (JsonElement value : parent.getAsJsonArray(key)) {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    result.add(value.getAsString());
                }
            }
        }
        return result;
    }

    private JsonObject serializeUi() {
        JsonObject ui = new JsonObject();
        JsonArray favorites = new JsonArray();
        uiPreferences.favorites().forEach(favorites::add);
        JsonArray recent = new JsonArray();
        uiPreferences.recent().forEach(recent::add);
        ui.add("favorites", favorites);
        ui.add("recent", recent);
        return ui;
    }

    /**
     * Positions are optional overrides: a pair is applied only when both axes
     * are present and numeric, otherwise the layout keeps its built-in default
     * anchor. Legacy task-HUD keys ({@code taskX} and friends) are simply
     * ignored and dropped on the next save.
     */
    private void loadHud(JsonObject root) {
        if (!root.has("hud") || !root.get("hud").isJsonObject()) return;
        JsonObject hud = root.getAsJsonObject("hud");
        double toastX = readNumber(hud, "toastX");
        double toastY = readNumber(hud, "toastY");
        if (!Double.isNaN(toastX) && !Double.isNaN(toastY)) hudLayout.setToastPosition(toastX, toastY);
        double modulesX = readNumber(hud, "modulesX");
        double modulesY = readNumber(hud, "modulesY");
        if (!Double.isNaN(modulesX) && !Double.isNaN(modulesY)) hudLayout.setModulesPosition(modulesX, modulesY);
        double statsX = readNumber(hud, "statsX");
        double statsY = readNumber(hud, "statsY");
        if (!Double.isNaN(statsX) && !Double.isNaN(statsY)) hudLayout.setStatsPosition(statsX, statsY);
        hudLayout.setToastVisible(readBoolean(hud, "toastVisible", hudLayout.toastVisible()));
        hudLayout.setModulesVisible(readBoolean(hud, "modulesVisible", hudLayout.modulesVisible()));
        hudLayout.setStatsVisible(readBoolean(hud, "statsVisible", hudLayout.statsVisible()));
    }

    private JsonObject serializeHud() {
        JsonObject hud = new JsonObject();
        if (hudLayout.hasToastPosition()) {
            hud.addProperty("toastX", hudLayout.toastX());
            hud.addProperty("toastY", hudLayout.toastY());
        }
        if (hudLayout.hasModulesPosition()) {
            hud.addProperty("modulesX", hudLayout.modulesX());
            hud.addProperty("modulesY", hudLayout.modulesY());
        }
        if (hudLayout.hasStatsPosition()) {
            hud.addProperty("statsX", hudLayout.statsX());
            hud.addProperty("statsY", hudLayout.statsY());
        }
        hud.addProperty("toastVisible", hudLayout.toastVisible());
        hud.addProperty("modulesVisible", hudLayout.modulesVisible());
        hud.addProperty("statsVisible", hudLayout.statsVisible());
        return hud;
    }



    private static double readNumber(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
            ? value.getAsDouble()
            : Double.NaN;
    }

    private static boolean readBoolean(JsonObject parent, String key, boolean fallback) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
            ? value.getAsBoolean()
            : fallback;
    }

    /**
     * Validates a profile name and returns it as its storage key.
     *
     * <p>Case is preserved — the name doubles as the label shown in the profile
     * manager, and folding it to lower case made {@code Combat} impossible to
     * type. Collisions are still resolved case-insensitively by
     * {@link #profileKey}, so {@code combat} and {@code Combat} cannot coexist
     * as two indistinguishable rows.
     */
    private static String normalizeProfileName(String name) {
        String id = name.trim().replace(' ', '_');
        if (!id.matches("[A-Za-z0-9_.-]{1,32}")) {
            throw new IllegalArgumentException("Profile name must contain only letters, digits, dot, dash or underscore");
        }
        return id;
    }

    /** The stored key equal to {@code name} ignoring case, or null when free. */
    private String profileKey(String name) {
        for (String existing : document.getAsJsonObject("profiles").keySet()) {
            if (existing.equalsIgnoreCase(name)) return existing;
        }
        return null;
    }

    private Path backupFile() {
        return configFile.resolveSibling(configFile.getFileName() + ".bak");
    }

    private void writeAtomically(String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Path temporaryFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                temporaryFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            if (Files.exists(configFile)) {
                Files.copy(configFile, backupFile(), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporaryFile, configFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException error) {
                // The only branch that can tear config.json mid-write; if a user
                // ever reports corruption, this line is how we know they were on it.
                LOGGER.warn("Atomic replace unsupported on {}; falling back to plain move", configFile.getParent());
                Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
