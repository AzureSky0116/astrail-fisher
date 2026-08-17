package dev.astrail.client.api.module;

/**
 * Capability for modules that keep the client world ticking while a screen is
 * open or the window is unfocused.
 *
 * <p>{@code MinecraftPauseMixin} suppresses the pause-screen tick freeze while
 * any <em>enabled</em> module implements this interface; the runtime never
 * consults disabled modules. {@code MinecraftAttackMixin} additionally skips
 * the vanilla controller's {@code stopDestroyBlock()} reset while one of them
 * reports {@link #preservesControllerMining()}.
 */
public interface BackgroundRunning {

    /**
     * Whether an in-flight vanilla block-break session owned by this module
     * must survive the controller reset that normally fires when a screen
     * opens or the window loses focus.
     */
    default boolean preservesControllerMining() {
        return false;
    }
}
