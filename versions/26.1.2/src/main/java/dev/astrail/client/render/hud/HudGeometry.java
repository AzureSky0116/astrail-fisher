package dev.astrail.client.render.hud;

/**
 * Shared, pure anchor and scale math for the movable HUD elements.
 *
 * <p>The live renderers and the HUD editor's draggable previews both resolve
 * their placement through these functions, so the preview a user drags and the
 * element the game later draws cannot drift apart. Everything operates on
 * primitives — window size, GUI scale, screen size — which keeps the math unit
 * testable without a Minecraft runtime.
 *
 * <p>Both elements draw in a fixed design space scaled by {@code ELEMENT_SCALE
 * x (window / 1920x1080 viewport fit) / guiScale}, so their on-screen size
 * tracks the physical window rather than the GUI scale setting.
 */
public final class HudGeometry {
    public static final int TOAST_CARD_WIDTH = 240;
    public static final int TOAST_CARD_HEIGHT = 60;
    public static final int TOAST_CARD_GAP = 6;
    public static final int MODULES_ROW_PITCH = 19;
    public static final int MODULES_ROW_HEIGHT = 18;
    public static final int STATS_CARD_WIDTH = 270;
    public static final int STATS_CARD_HEIGHT = 54;
    public static final float ELEMENT_SCALE = 1.5F;
    public static final int TOAST_EDGE_MARGIN_PHYSICAL = 20;
    public static final int MODULES_EDGE_MARGIN_PHYSICAL = 10;
    public static final int MODULES_TOP_MARGIN_PHYSICAL = 16;
    public static final int STATS_LEFT_MARGIN_PHYSICAL = 16;
    public static final int STATS_TOP_MARGIN_PHYSICAL = 16;

    private static final float REFERENCE_VIEWPORT_WIDTH = 1_920.0F;
    private static final float REFERENCE_VIEWPORT_HEIGHT = 1_080.0F;

    /** An element's anchor point in GUI-scaled screen coordinates. */
    public record Anchor(float x, float y) {
    }

    private HudGeometry() {
    }

    /** Conversion from physical design pixels to GUI-scaled coordinates. */
    public static float physicalToScaled(float windowWidth, float windowHeight, float guiScale) {
        float viewportScale = Math.min(
            windowWidth / REFERENCE_VIEWPORT_WIDTH,
            windowHeight / REFERENCE_VIEWPORT_HEIGHT
        );
        return viewportScale / Math.max(1.0F, guiScale);
    }

    /** Scale applied to an element's design-space content. */
    public static float displayScale(float physicalToScaled) {
        return ELEMENT_SCALE * physicalToScaled;
    }

    /** The toast stack's built-in bottom-right anchor, before any override. */
    public static Anchor toastDefaultAnchor(int screenWidth, int screenHeight, float physicalToScaled) {
        return new Anchor(
            screenWidth - TOAST_EDGE_MARGIN_PHYSICAL * physicalToScaled,
            screenHeight - TOAST_EDGE_MARGIN_PHYSICAL * physicalToScaled
        );
    }

    /** The module list's built-in top-right anchor, before any override. */
    public static Anchor modulesDefaultAnchor(int screenWidth, int screenHeight, float physicalToScaled) {
        return new Anchor(
            screenWidth - MODULES_EDGE_MARGIN_PHYSICAL * physicalToScaled,
            MODULES_TOP_MARGIN_PHYSICAL * physicalToScaled
        );
    }

    /** The module-stats card's built-in top-left anchor. */
    public static Anchor statsDefaultAnchor(int screenWidth, int screenHeight, float physicalToScaled) {
        return new Anchor(
            STATS_LEFT_MARGIN_PHYSICAL * physicalToScaled,
            STATS_TOP_MARGIN_PHYSICAL * physicalToScaled
        );
    }

    /**
     * Bottom-right corner of the toast stack's base card. Clamped so the base
     * card can never be dragged or restored fully off screen.
     */
    public static Anchor toastAnchor(HudLayout layout, int screenWidth, int screenHeight, float physicalToScaled) {
        float scale = displayScale(physicalToScaled);
        Anchor fallback = toastDefaultAnchor(screenWidth, screenHeight, physicalToScaled);
        float x = layout.hasToastPosition() ? layout.toastX() * screenWidth : fallback.x();
        float y = layout.hasToastPosition() ? layout.toastY() * screenHeight : fallback.y();
        return new Anchor(
            clamp(x, TOAST_CARD_WIDTH * scale, screenWidth),
            clamp(y, TOAST_CARD_HEIGHT * scale, screenHeight)
        );
    }

    /**
     * Top-right corner of the module list's first row. The list is
     * right-aligned text extending left of the anchor, so the lower x bound
     * only has to keep a readable sliver on screen.
     */
    public static Anchor modulesAnchor(HudLayout layout, int screenWidth, int screenHeight, float physicalToScaled) {
        float scale = displayScale(physicalToScaled);
        Anchor fallback = modulesDefaultAnchor(screenWidth, screenHeight, physicalToScaled);
        float x = layout.hasModulesPosition() ? layout.modulesX() * screenWidth : fallback.x();
        float y = layout.hasModulesPosition() ? layout.modulesY() * screenHeight : fallback.y();
        return new Anchor(
            clamp(x, MODULES_ROW_HEIGHT * scale, screenWidth),
            clamp(y, 0.0F, Math.max(0.0F, screenHeight - MODULES_ROW_PITCH * scale))
        );
    }

    /** Top-left corner of the compact module-stats card. */
    public static Anchor statsAnchor(HudLayout layout, int screenWidth, int screenHeight, float physicalToScaled) {
        float scale = displayScale(physicalToScaled);
        Anchor fallback = statsDefaultAnchor(screenWidth, screenHeight, physicalToScaled);
        float x = layout.hasStatsPosition() ? layout.statsX() * screenWidth : fallback.x();
        float y = layout.hasStatsPosition() ? layout.statsY() * screenHeight : fallback.y();
        return new Anchor(
            clamp(x, 0.0F, Math.max(0.0F, screenWidth - STATS_CARD_WIDTH * scale)),
            clamp(y, 0.0F, Math.max(0.0F, screenHeight - STATS_CARD_HEIGHT * scale))
        );
    }

    private static float clamp(float value, float minimum, float maximum) {
        if (maximum < minimum) {
            return maximum;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
