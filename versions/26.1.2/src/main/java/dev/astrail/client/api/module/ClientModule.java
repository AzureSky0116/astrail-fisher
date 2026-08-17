package dev.astrail.client.api.module;

import dev.astrail.client.api.setting.Setting;
import dev.astrail.client.api.setting.KeybindSetting;
import java.util.List;

public interface ClientModule {
    ModuleMetadata metadata();

    ModuleState state();

    List<Setting<?>> settings();

    KeybindSetting keybind();

    /**
     * Whether the module's current state reflects a user choice that should
     * survive a restart. Set by a successful enable and cleared only by a
     * deliberate user disable or a repeated-failure disable; world-change and
     * client-stopping teardown keep it, so a transient shutdown never erases
     * the user's preference.
     */
    default boolean wantsPersistedEnabled() {
        return false;
    }

    void enable();

    void disable(DisableReason reason);
}
