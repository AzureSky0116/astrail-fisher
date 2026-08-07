package dev.astrail.client.core.module;

import dev.astrail.client.api.module.ClientModule;
import dev.astrail.client.api.module.DisableReason;
import dev.astrail.client.api.module.ModuleState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class ModuleManager {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();

    /**
     * Registration-order snapshot rebuilt only when a module is added.
     *
     * <p>{@link #all()} is read once per client tick for keybinds and several times
     * per frame by the screen and the HUD, so copying the registry on each call put
     * a list allocation on both hot paths for a collection that never changes after
     * startup.
     */
    private List<ClientModule> ordered = List.of();

    private Runnable changeListener = () -> { };
    private Consumer<ClientModule> stateChangeListener = module -> { };

    public void register(ClientModule module) {
        ClientModule previous = modules.putIfAbsent(module.metadata().id(), module);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate module id: " + module.metadata().id());
        }
        ordered = List.copyOf(modules.values());
    }

    public Optional<ClientModule> find(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public List<ClientModule> all() {
        return ordered;
    }

    /**
     * Toggles a module and notifies listeners even when the transition fails.
     *
     * <p>A failed {@code enable} leaves the module in {@code FAILED}, which is a
     * state the HUD and the saved config both need to see. Letting the exception
     * skip the listeners left the card rendering as if nothing had happened and
     * kept the on-disk {@code enabled} flag from being corrected, so the same
     * failing module was re-enabled on the next launch.
     */
    public void setEnabled(String id, boolean enabled) {
        ClientModule module = require(id);
        try {
            if (enabled) {
                module.enable();
            } else {
                module.disable(DisableReason.USER);
            }
        } finally {
            stateChangeListener.accept(module);
            changeListener.run();
        }
    }

    public void toggle(String id) {
        ClientModule module = require(id);
        setEnabled(id, module.state() != ModuleState.ENABLED);
    }

    public void configurationChanged() {
        changeListener.run();
    }

    public void onChange(Runnable listener) {
        this.changeListener = listener;
    }

    public void onStateChange(Consumer<ClientModule> listener) {
        this.stateChangeListener = listener;
    }

    public void disableWorldScoped() {
        for (ClientModule module : modules.values()) {
            if (module.metadata().disableOnWorldChange() && module.state() != ModuleState.DISABLED) {
                module.disable(DisableReason.WORLD_CHANGED);
            }
        }
    }

    public void disableAll(DisableReason reason) {
        for (ClientModule module : modules.values()) {
            if (module.state() != ModuleState.DISABLED) {
                module.disable(reason);
            }
        }
    }

    private ClientModule require(String id) {
        ClientModule module = modules.get(id);
        if (module == null) {
            throw new IllegalArgumentException("Unknown module: " + id);
        }
        return module;
    }
}
