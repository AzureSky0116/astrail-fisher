package dev.astrail.client.ui.component;

/** Frame-rate independent easing helpers for compact product UI motion. */
public final class UiMotion {
    /**
     * Opacity multiplier for the render pass in flight.
     *
     * <p>Deliberately a plain field rather than a {@code ThreadLocal}: every glyph,
     * icon and fill routes its color through {@link #applyFrameOpacity}, so the
     * read happens tens of thousands of times per frame and a thread-local map
     * lookup at that rate is measurable. Both callers -- the screen and the HUD
     * elements -- run on Minecraft's single render thread, and each push is
     * balanced by a pop in a {@code finally}, so a shared field is confined in
     * practice.
     */
    private static float frameOpacity = 1.0F;

    private UiMotion() {
    }

    public static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public static float easeOutQuint(float progress) {
        float remaining = 1.0F - clamp01(progress);
        return 1.0F - remaining * remaining * remaining * remaining * remaining;
    }

    public static float easeInOutCubic(float progress) {
        float value = clamp01(progress);
        return value < 0.5F
            ? 4.0F * value * value * value
            : 1.0F - (float) Math.pow(-2.0F * value + 2.0F, 3.0F) * 0.5F;
    }

    /** Settles with a slight overshoot; suited to short entrance slides. */
    public static float easeOutBack(float progress) {
        float value = clamp01(progress) - 1.0F;
        return 1.0F + value * value * ((OVERSHOOT + 1.0F) * value + OVERSHOOT);
    }

    private static final float OVERSHOOT = 1.70158F;

    public static float damp(float current, float target, float deltaSeconds, float responseSeconds) {
        if (responseSeconds <= 0.0F) return target;
        float amount = 1.0F - (float) Math.exp(-Math.max(0.0F, deltaSeconds) / responseSeconds);
        return current + (target - current) * amount;
    }

    public static int multiplyAlpha(int color, float opacity) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * clamp01(opacity));
        return color & 0x00FFFFFF | alpha << 24;
    }

    /**
     * Installs a scoped opacity multiplier, composed onto whatever scope is
     * already active.
     *
     * <p>Multiplicative on purpose: a toast fading itself in while the HUD
     * editor dims the whole preview must yield the product of both opacities.
     * The earlier overwrite semantics let the innermost scope win, silently
     * discarding every enclosing fade.
     */
    public static float pushFrameOpacity(float opacity) {
        float previous = frameOpacity;
        frameOpacity = clamp01(previous * clamp01(opacity));
        return previous;
    }

    public static void popFrameOpacity(float previous) {
        frameOpacity = clamp01(previous);
    }

    /** Applies the current render-pass opacity without changing local alpha. */
    public static int applyFrameOpacity(int color) {
        return multiplyAlpha(color, frameOpacity);
    }
}
