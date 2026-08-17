package dev.astrail.client.ui.component;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Immediate-mode primitives used by the custom Screen components. */
public final class UiDraw {
    private UiDraw() {
    }

    public static void shadow(GuiGraphicsExtractor graphics, UiRect rect, int radius) {
        // Three faint, concentric layers read as a soft falloff. They stay
        // horizontally centred on the card: the old +2px x offset pushed the
        // shadow out from under one edge only, which showed up as a hard
        // asymmetric rim rather than a shadow.
        fillRounded(graphics, rect.expand(6).translate(0, 5), radius + 6, 0x14000000);
        fillRounded(graphics, rect.expand(3).translate(0, 4), radius + 3, 0x22000000);
        fillRounded(graphics, rect.expand(1).translate(0, 2), radius + 1, 0x38000000);
    }

    /**
     * Draws a panel surface with an optional 1px border ring.
     *
     * <p>A fully transparent {@code border} means "no ring": the fill then
     * covers the whole rectangle. Insetting the fill regardless left the
     * outermost pixel ring unpainted, so the drop shadow underneath showed
     * through it as a doubled, stair-stepped edge.
     */
    public static void card(GuiGraphicsExtractor graphics, UiRect rect, int radius, int fill, int border) {
        shadow(graphics, rect, radius);
        boolean ringed = (border >>> 24) != 0 && rect.width() > 2 && rect.height() > 2;
        if (!ringed) {
            fillRounded(graphics, rect, radius, fill);
            return;
        }
        fillRounded(graphics, rect, radius, border);
        fillRounded(graphics, rect.inset(1), Math.max(0, radius - 1), fill);
    }

    /**
     * Antialiased rounded rectangle.
     *
     * <p>The corners are resolved by pixel coverage rather than a per-row
     * integer inset. The integer version quantised every arc to whole pixels,
     * which the non-integer GUI scale then resampled into the visible
     * stair-step speckle along card edges.
     */
    public static void fillRounded(GuiGraphicsExtractor graphics, UiRect rect, int radius, int color) {
        if (rect.width() <= 0 || rect.height() <= 0) return;
        color = UiMotion.applyFrameOpacity(color);
        if ((color >>> 24) == 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(rect.width(), rect.height()) / 2));
        if (r == 0) {
            graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), color);
            return;
        }
        graphics.fill(rect.x(), rect.y() + r, rect.right(), rect.bottom() - r, color);
        double inner = r - 0.5D;
        double outer = r + 0.5D;
        for (int row = 0; row < r; row++) {
            // Vertical distance from the arc centre to this pixel row's centre.
            double dy = r - row - 0.5D;
            double innerHalf = halfChord(inner, dy);
            double outerHalf = halfChord(outer, dy);
            if (outerHalf < 0.0D) continue;
            int solidFrom = innerHalf < 0.0D ? r : (int) Math.ceil(r - 0.5D - innerHalf);
            int partialFrom = Math.max(0, (int) Math.ceil(r - 0.5D - outerHalf));
            int topY = rect.y() + row;
            int bottomY = rect.bottom() - row - 1;
            if (rect.right() - solidFrom > rect.x() + solidFrom) {
                graphics.fill(rect.x() + solidFrom, topY, rect.right() - solidFrom, topY + 1, color);
                graphics.fill(rect.x() + solidFrom, bottomY, rect.right() - solidFrom, bottomY + 1, color);
            }
            for (int column = partialFrom; column < solidFrom; column++) {
                double dx = r - column - 0.5D;
                int shaded = coverageColor(color, coverage(dx, dy, r));
                if ((shaded >>> 24) == 0) continue;
                int left = rect.x() + column;
                int right = rect.right() - column - 1;
                graphics.fill(left, topY, left + 1, topY + 1, shaded);
                graphics.fill(right, topY, right + 1, topY + 1, shaded);
                graphics.fill(left, bottomY, left + 1, bottomY + 1, shaded);
                graphics.fill(right, bottomY, right + 1, bottomY + 1, shaded);
            }
        }
    }

    /**
     * Antialiased filled disc with a sub-pixel centre and radius, for dots and
     * indicators small enough that a quantised outline reads as a staircase.
     */
    public static void circle(GuiGraphicsExtractor graphics, float centerX, float centerY, float radius, int color) {
        color = UiMotion.applyFrameOpacity(color);
        if (radius <= 0.0F || (color >>> 24) == 0) return;
        double inner = radius - 0.5D;
        double outer = radius + 0.5D;
        int top = (int) Math.floor(centerY - outer);
        int bottom = (int) Math.ceil(centerY + outer);
        for (int y = top; y < bottom; y++) {
            double dy = y + 0.5D - centerY;
            double outerHalf = halfChord(outer, dy);
            if (outerHalf < 0.0D) continue;
            double innerHalf = halfChord(inner, dy);
            int from = (int) Math.floor(centerX - outerHalf);
            int to = (int) Math.ceil(centerX + outerHalf);
            int solidFrom = to;
            int solidTo = to;
            if (innerHalf >= 0.0D) {
                solidFrom = (int) Math.ceil(centerX - innerHalf - 0.5D);
                solidTo = (int) Math.floor(centerX + innerHalf - 0.5D) + 1;
                if (solidTo > solidFrom) graphics.fill(solidFrom, y, solidTo, y + 1, color);
            }
            for (int x = from; x < to; x++) {
                if (x >= solidFrom && x < solidTo) continue;
                int shaded = coverageColor(color, coverage(x + 0.5D - centerX, dy, radius));
                if ((shaded >>> 24) == 0) continue;
                graphics.fill(x, y, x + 1, y + 1, shaded);
            }
        }
    }

    /** Half the horizontal chord of a circle of {@code radius} at height {@code dy}. */
    private static double halfChord(double radius, double dy) {
        double squared = radius * radius - dy * dy;
        return squared <= 0.0D ? -1.0D : Math.sqrt(squared);
    }

    /** Signed-distance coverage of the pixel centred at ({@code dx}, {@code dy}). */
    private static float coverage(double dx, double dy, double radius) {
        return UiMotion.clamp01((float) (0.5D + radius - Math.sqrt(dx * dx + dy * dy)));
    }

    private static int coverageColor(int color, float coverage) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * coverage);
        return color & 0x00FFFFFF | alpha << 24;
    }

    public static String pretty(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    public static int lerpColor(int from, int to, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >>> 16) & 0xFF) + (((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * t);
        int g = Math.round(((from >>> 8) & 0xFF) + (((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** High-DPI status badge used by module cards and the settings header. */
    public static void statusBadge(GuiGraphicsExtractor graphics, int x, int y, boolean enabled, int color) {
        (enabled ? UiAssets.STATUS_ON : UiAssets.STATUS_OFF).draw(graphics, x, y, color);
    }

    /**
     * Draws one of the authored high-DPI toggle frames. The input progress is
     * deliberately continuous so the setting animation remains independent of
     * the texture atlas frame count.
     */
    public static void toggleSwitch(
        GuiGraphicsExtractor graphics,
        UiRect rect,
        boolean enabled,
        float progress,
        int color
    ) {
        float amount = Float.isFinite(progress)
            ? Math.max(0.0F, Math.min(1.0F, progress))
            : enabled ? 1.0F : 0.0F;
        int frame = Math.round(amount * 12.0F);
        UiAssets.Texture sheet = UiAssets.TOGGLE_SHEET;
        int frameHeight = Math.max(1, sheet.textureHeight() / 13);
        drawTextureRegion(
            graphics,
            sheet,
            rect.x(),
            rect.y(),
            0,
            frame * frameHeight,
            sheet.textureWidth(),
            frameHeight,
            rect.width(),
            rect.height(),
            color
        );
    }

    /** High-DPI field with a filtered vector-derived chevron. */
    public static void controlField(GuiGraphicsExtractor graphics, UiRect rect, int color) {
        UiAssets.Texture field = rect.width() <= UiAssets.CONTROL_VALUE.width()
            ? UiAssets.CONTROL_VALUE
            : UiAssets.CONTROL_DROPDOWN;
        field.drawScaled(graphics, rect.x(), rect.y(), rect.width(), rect.height(), color);
    }

    /** High-DPI choice field with a filtered vector-derived chevron. */
    public static void dropdown(
        GuiGraphicsExtractor graphics,
        UiRect rect,
        float expandedAmount,
        int border,
        int fill,
        int color
    ) {
        controlField(graphics, rect, color);

        // The focus state increases contrast instead of redrawing a second
        // low-resolution arrow. The chevron squashes through zero height on the
        // way to its mirrored state, so opening and closing read as one flip.
        float amount = Math.max(0.0F, Math.min(1.0F, expandedAmount));
        int chevronColor = multiplyAlpha(color, 0.74F + 0.26F * amount);
        int iconX = rect.right() - UiAssets.CHEVRON.width() - 10;
        int iconY = rect.y() + (rect.height() - UiAssets.CHEVRON.height()) / 2;
        float flip = 1.0F - 2.0F * amount;
        if (Math.abs(flip) < 0.04F) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX + UiAssets.CHEVRON.width() * 0.5F, iconY + UiAssets.CHEVRON.height() * 0.5F);
        graphics.pose().scale(1.0F, flip);
        graphics.pose().translate(-iconX - UiAssets.CHEVRON.width() * 0.5F, -iconY - UiAssets.CHEVRON.height() * 0.5F);
        UiAssets.CHEVRON.draw(graphics, iconX, iconY, chevronColor);
        graphics.pose().popMatrix();
    }

    /**
     * Renders the number control's track, fill and knob from a single
     * high-DPI asset family. The caller retains ownership of the surrounding
     * hit rectangle and value field.
     */
    public static void slider(GuiGraphicsExtractor graphics, UiRect rect, float progress, int color) {
        if (rect.width() <= 0 || rect.height() <= 0) return;

        float amount = Float.isFinite(progress)
            ? Math.max(0.0F, Math.min(1.0F, progress))
            : 0.0F;
        int alpha = (color >>> 24) & 0xFF;
        int neutral = multiplyAlpha(0xFFFFFFFF, alpha / 255.0F);
        UiAssets.SLIDER_TRACK.drawScaled(graphics, rect.x(), rect.y(), rect.width(), rect.height(), neutral);

        int fillWidth = Math.round(rect.width() * amount);
        if (fillWidth > 0) {
            UiAssets.Texture fill = UiAssets.SLIDER_FILL;
            int sourceWidth = Math.max(1, Math.round(fill.textureWidth() * (fillWidth / (float) rect.width())));
            drawTextureRegion(
                graphics,
                fill,
                rect.x(),
                rect.y(),
                0,
                0,
                sourceWidth,
                fill.textureHeight(),
                fillWidth,
                rect.height(),
                color
            );
        }

        UiAssets.Texture knob = UiAssets.SLIDER_KNOB;
        int knobX = rect.x() + Math.round((rect.width() - knob.width()) * amount);
        int knobY = rect.y() + (rect.height() - knob.height()) / 2;
        knob.draw(graphics, knobX, knobY, neutral);
    }

    private static void drawTextureRegion(
        GuiGraphicsExtractor graphics,
        UiAssets.Texture texture,
        int x,
        int y,
        int sourceX,
        int sourceY,
        int sourceWidth,
        int sourceHeight,
        int destinationWidth,
        int destinationHeight,
        int color
    ) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || destinationWidth <= 0 || destinationHeight <= 0) return;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture.id(),
            x,
            y,
            sourceX,
            sourceY,
            destinationWidth,
            destinationHeight,
            sourceWidth,
            sourceHeight,
            texture.textureWidth(),
            texture.textureHeight(),
            color
        );
    }

    private static int multiplyAlpha(int color, float opacity) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, opacity)));
        return color & 0x00FFFFFF | alpha << 24;
    }

}
