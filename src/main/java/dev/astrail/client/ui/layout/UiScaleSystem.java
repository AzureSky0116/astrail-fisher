package dev.astrail.client.ui.layout;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Fixed-coordinate bridge for the 1536 x 1024 Image2 design canvas.
 *
 * <p>The layout is never recomputed from arbitrary percentages. The complete
 * design canvas is uniformly scaled and letterboxed, so every authored
 * position, size, padding and margin remains proportional at every GUI scale.</p>
 */
public final class UiScaleSystem {
    public static final String ID = "UI_SCALE_SYSTEM";
    public static final int DESIGN_WIDTH = 1_536;
    public static final int DESIGN_HEIGHT = 1_024;
    public static final int PANEL_X = 198;
    public static final int PANEL_Y = 152;
    public static final int PANEL_WIDTH = 1_140;
    public static final int PANEL_HEIGHT = 720;

    private final float scale;
    private final float offsetX;
    private final float offsetY;

    private UiScaleSystem(float scale, float offsetX, float offsetY) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public static UiScaleSystem fit(int viewportWidth, int viewportHeight) {
        float scale = Math.min(
            viewportWidth / (float) DESIGN_WIDTH,
            viewportHeight / (float) DESIGN_HEIGHT
        );
        float offsetX = (viewportWidth - DESIGN_WIDTH * scale) * 0.5F;
        float offsetY = (viewportHeight - DESIGN_HEIGHT * scale) * 0.5F;
        return new UiScaleSystem(scale, offsetX, offsetY);
    }

    public void push(GuiGraphicsExtractor graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(offsetX, offsetY);
        graphics.pose().scale(scale, scale);
    }

    public void pop(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public double designX(double viewportX) {
        return (viewportX - offsetX) / scale;
    }

    public double designY(double viewportY) {
        return (viewportY - offsetY) / scale;
    }

    public float scale() {
        return scale;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }
}
