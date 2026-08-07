package dev.astrail.client.core;

import dev.astrail.client.api.event.ClientTickEvent;
import dev.astrail.client.api.event.ContainerSlotUpdateEvent;
import dev.astrail.client.api.event.WorldChangedEvent;
import dev.astrail.client.api.module.BackgroundRunning;
import dev.astrail.client.api.module.ClientModule;
import dev.astrail.client.api.module.DisableReason;
import dev.astrail.client.api.module.ModuleState;
import dev.astrail.client.api.setting.KeybindSetting;
import dev.astrail.client.core.config.ConfigManager;
import dev.astrail.client.core.diagnostic.DiagnosticService;
import dev.astrail.client.core.event.EventBus;
import dev.astrail.client.core.module.ModuleManager;
import dev.astrail.client.core.scheduler.ClientScheduler;
import dev.astrail.client.core.world.WorldContext;
import dev.astrail.client.feature.macro.fishing.AutoFishModule;
import dev.astrail.client.platform.minecraft.AstrailKeybinds;
import dev.astrail.client.platform.minecraft.MinecraftInputService;
import dev.astrail.client.platform.minecraft.MinecraftInteractionService;
import dev.astrail.client.platform.minecraft.MinecraftRotationService;
import dev.astrail.client.render.hud.HudLayout;
import dev.astrail.client.render.hud.HudNotificationRenderer;
import dev.astrail.client.ui.screen.AutoFishScreen;
import dev.astrail.client.ui.state.UiPreferences;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger("Astrail/Fisher");
    private static final int FAILURE_THRESHOLD = 3;

    private final EventBus events = new EventBus();
    private final ModuleManager modules = new ModuleManager();
    private final WorldContext world = new WorldContext();
    private final ClientScheduler scheduler = new ClientScheduler(world::generation);
    private final DiagnosticService diagnostics = new DiagnosticService();
    private final MinecraftInputService inputs = new MinecraftInputService(diagnostics);
    private final MinecraftRotationService rotations = new MinecraftRotationService(diagnostics);
    private final MinecraftInteractionService interactions = new MinecraftInteractionService();
    private final UiPreferences uiPreferences = new UiPreferences();
    private final HudLayout hudLayout = new HudLayout();
    private final HudNotificationRenderer notifications = new HudNotificationRenderer(hudLayout);
    private final ClientServices services = new ClientServices(events, inputs, rotations, interactions);
    private final ConfigManager config = new ConfigManager(
        FabricLoader.getInstance().getConfigDir(), modules, uiPreferences, hudLayout
    );
    private final Map<String, Integer> failureCounts = new HashMap<>();
    private final Map<String, Boolean> moduleKeyStates = new HashMap<>();
    /** Modules left enabled by the user, waiting for a world to run in. */
    private final List<String> restorePending = new ArrayList<>();

    private int ticksSinceConfigFlush;

    /** Ticks between periodic flushes of coalesced config changes (5 seconds). */
    private static final int CONFIG_FLUSH_INTERVAL_TICKS = 100;

    public void initialize() {
        modules.register(new AutoFishModule(services));
        config.load();
        modules.onChange(config::markDirty);
        modules.onStateChange(notifications::showModuleState);
        // Counters are keyed by source, not by module: with one shared counter,
        // every successful event dispatch wiped the tally of task failures and
        // the auto-disable threshold could never be reached.
        events.onFailure((owner, error) -> onModuleFailure("event:" + owner, owner, error));
        events.onSuccess(owner -> failureCounts.remove("event:" + owner));
        scheduler.onFailure((owner, error) -> onModuleFailure("task:" + owner, owner, error));
        notifications.register();
        AstrailKeybinds.openFishingGui();
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> worldChanged());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> stop());
        LOGGER.info("Astrail Fisher {} initialized for Minecraft 26.2", FabricLoader.getInstance()
            .getModContainer("astrail-fisher").orElseThrow().getMetadata().getVersion().getFriendlyString());
    }

    public ModuleManager modules() {
        return modules;
    }

    public ConfigManager config() {
        return config;
    }

    public MinecraftInputService inputs() {
        return inputs;
    }



    public boolean shouldKeepWorldRunning() {
        for (ClientModule module : modules.all()) {
            if (module instanceof BackgroundRunning && module.state() == ModuleState.ENABLED) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldPreserveControllerMining() {
        for (ClientModule module : modules.all()) {
            if (module.state() == ModuleState.ENABLED
                && module instanceof BackgroundRunning background
                && background.preservesControllerMining()) {
                return true;
            }
        }
        return false;
    }

    public void handleContainerSlotUpdate(ClientboundContainerSetSlotPacket packet) {
        events.post(new ContainerSlotUpdateEvent(
            packet.getContainerId(), packet.getStateId(), packet.getSlot(), packet.getItem()
        ));
    }

    private void tick(Minecraft client) {
        handleMenuKey(client);
        handleModuleKeys(client);
        if (client.level != null && client.player != null && !restorePending.isEmpty()) {
            List<String> pending = List.copyOf(restorePending);
            restorePending.clear();
            for (String id : pending) {
                modules.find(id).ifPresent(module -> {
                    if (module.state() != ModuleState.ENABLED) {
                        try {
                            module.enable();
                        } catch (RuntimeException error) {
                            LOGGER.error("Failed to restore module after world change: {}", id, error);
                        }
                    }
                });
            }
        }
        scheduler.tick();
        events.post(new ClientTickEvent(client, world.generation()));
        rotations.tick();
        if (++ticksSinceConfigFlush >= CONFIG_FLUSH_INTERVAL_TICKS) {
            ticksSinceConfigFlush = 0;
            config.flushIfDirty();
        }
    }

    private void handleMenuKey(Minecraft client) {
        if (!AstrailKeybinds.openFishingGui().consumeClick()) return;
        Screen currentScreen = client.gui.screen();
        if (currentScreen instanceof AutoFishScreen fishScreen) {
            if (!fishScreen.isCapturingKeybind()) {
                fishScreen.requestClose();
            }
            return;
        }
        boolean canOpen = currentScreen == null || currentScreen instanceof TitleScreen;
        if (canOpen) {
            client.gui.setScreen(new AutoFishScreen(this, currentScreen));
        }
    }

    private void handleModuleKeys(Minecraft client) {
        boolean hasScreen = client.gui.screen() != null;
        for (ClientModule module : modules.all()) {
            int keyCode = module.keybind().get();
            boolean pressed = keyCode != KeybindSetting.UNBOUND
                && keyCode != AstrailKeybinds.boundGuiKey()
                && InputConstants.isKeyDown(client.getWindow(), keyCode);
            boolean previous = moduleKeyStates.getOrDefault(module.metadata().id(), false);
            moduleKeyStates.put(module.metadata().id(), pressed);
            if (!hasScreen && pressed && !previous) {
                toggleWithFeedback(client, module);
            }
        }
    }

    private void toggleWithFeedback(Minecraft client, ClientModule module) {
        try {
            modules.toggle(module.metadata().id());
        } catch (RuntimeException error) {
            LOGGER.error("Failed to toggle module: {}", module.metadata().id(), error);
        }
    }

    private void worldChanged() {
        long generation = world.advanceGeneration();
        failureCounts.clear();
        modules.disableWorldScoped();
        // World-scoped modules are torn down on every level change, but a
        // teardown is not a choice: remember which ones the user left enabled
        // and bring them back once the next world is actually present.
        restorePending.clear();
        for (ClientModule module : modules.all()) {
            if (module.metadata().persistEnabled()
                    && module.wantsPersistedEnabled()
                    && module.state() != ModuleState.ENABLED) {
                restorePending.add(module.metadata().id());
            }
        }
        inputs.clear();
        rotations.clear();
        scheduler.clear();
        diagnostics.clear();
        events.post(new WorldChangedEvent(generation));
    }

    private void stop() {
        modules.disableAll(DisableReason.CLIENT_STOPPING);
        inputs.clear();
        rotations.clear();
        scheduler.clear();
        diagnostics.clear();
        config.save();
    }

    private void onModuleFailure(String counterKey, String owner, Throwable error) {
        LOGGER.error("Module failure: owner={}", owner, error);
        int failures = failureCounts.merge(counterKey, 1, Integer::sum);
        if (failures < FAILURE_THRESHOLD) {
            return;
        }
        modules.find(owner).ifPresent(module -> {
            module.disable(DisableReason.FAILURE);
            config.save();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(
                    Component.literal(module.metadata().displayName() + " disabled after repeated errors")
                        .withStyle(ChatFormatting.RED)
                );
            }
        });
        failureCounts.remove(counterKey);
    }
}
