package dev.astrail.client.ui.component;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Exact one-to-one mapping between the approved Image2 assets and GUI parts. */
public final class UiAssets {
    private static final String ROOT = "textures/gui/reference_v1/";
    // The production masters remain 4x. Runtime PNGs are Lanczos-prefiltered to
    // logical size because standalone Minecraft GUI textures expose no mip
    // chain; direct 4x minification produced the visible stair-step edges.
    private static final int PHYSICAL_SCALE = 1;
    private static final int TOGGLE_FRAME_COUNT = 13;

    public static final Texture WINDOW_SHELL = texture("window_shell_1140x720.png", 1_140, 720);
    public static final Texture SIDEBAR = texture("sidebar_surface_270x720.png", 270, 720);
    public static final Texture LOGO = texture("astrail_logo_128x128.png", 128, 128, 2);
    public static final Texture WORDMARK = texture("astrail_wordmark_200x32.png", 200, 32, 2);
    public static final Texture SEARCH = texture("search_pill_430x54.png", 430, 54);
    public static final Texture SEARCH_FOCUS = texture("search_focus_430x54.png", 430, 54);
    public static final Texture SEARCH_ICON = icon("icons/search_32x32_idle.png", 32, 32);
    public static final Texture CATEGORY_IDLE = texture("category_pill_idle_104x52.png", 104, 52);
    public static final Texture CATEGORY_ACTIVE = texture("category_pill_active_84x52.png", 84, 52);
    public static final Texture NAV_SELECTED = texture("nav_selected_230x58.png", 230, 58);
    public static final Texture NAV_HOVER = texture("nav_hover_230x58.png", 230, 58);
    public static final Texture MODULE_OFF = texture("module_card_off_778x94.png", 778, 94);
    public static final Texture MODULE_ON = texture("module_card_on_778x94.png", 778, 94);
    public static final Texture MODULE_HOVER = texture("module_card_hover_778x94.png", 778, 94);
    public static final Texture STATUS_OFF = texture("status_off_62x62.png", 62, 62);
    public static final Texture STATUS_ON = texture("status_on_62x62.png", 62, 62);
    public static final Texture FOOTER_BUTTON = texture("footer_button_52x52.png", 52, 52);
    public static final Texture FOOTER_HOVER = texture("footer_hover_52x52.png", 52, 52);
    public static final Texture SETTINGS_DRAWER = texture("settings_drawer_450x692.png", 450, 692);
    public static final Texture SETTING_ROW_BASE = texture("setting_row_base_410x84.png", 410, 84);
    public static final Texture SETTING_ROW_HOVER = texture("setting_row_hover_410x84.png", 410, 84);
    public static final Texture CONTROL_DROPDOWN = texture("control_dropdown_128x52.png", 128, 52);
    public static final Texture CONTROL_VALUE = texture("control_value_104x52.png", 104, 52);
    public static final Texture CHOICE_OPTION_SELECTED = texture("choice_option_selected_138x26.png", 138, 26);
    public static final Texture TOGGLE_SHEET = spriteSheet("toggle_62x32_sheet.png", 62, 32, TOGGLE_FRAME_COUNT, 2);
    public static final Texture SLIDER_TRACK = texture("slider_track_190x12.png", 190, 12);
    public static final Texture SLIDER_FILL = texture("slider_fill_190x12.png", 190, 12);
    public static final Texture SLIDER_KNOB = texture("slider_knob_14x14.png", 14, 14);
    public static final Texture MENU = icon("icons/menu_24x24_idle.png", 24, 24);
    public static final Texture SETTINGS = icon("icons/settings_24x24_idle.png", 24, 24);
    public static final Texture CLOSE = icon("icons/close_24x24_idle.png", 24, 24);
    public static final Texture CHEVRON = icon("icons/chevron-down_16x16_idle.png", 16, 16);
    public static final Texture CHEVRON_DOWN = CHEVRON;
    public static final Texture CHECK = icon("icons/check_16x16_active.png", 16, 16);
    public static final Texture FAVORITE_IDLE = icon("icons/favorite_24x24_idle.png", 24, 24);
    public static final Texture FAVORITE_ACTIVE = icon("icons/favorite_24x24_active.png", 24, 24);
    public static final Texture KEYBIND = icon("icons/keybind_20x20_idle.png", 20, 20);
    public static final Texture CHOICE = icon("icons/choice_20x20_idle.png", 20, 20);
    public static final Texture NUMBER = icon("icons/number_20x20_idle.png", 20, 20);
    public static final Texture NOTIFICATION_TOAST = texture("notification_toast_240x60.png", 240, 60, 8);
    public static final Texture HUD_TASK_CARD = texture("hud_task_card_190x45.png", 190, 45);
    public static final Texture HUD_TASK_ACTIVE = icon("icons/task_active_20x20_active.png", 20, 20);
    public static final Texture HUD_TASK_PAUSED = icon("icons/task_paused_20x20_idle.png", 20, 20);

    private static final Texture CHOICE_MENU_40 = texture("choice_menu_150x40.png", 150, 40);
    private static final Texture CHOICE_MENU_70 = texture("choice_menu_150x70.png", 150, 70);
    private static final Texture CHOICE_MENU_100 = texture("choice_menu_150x100.png", 150, 100);
    private static final Texture CHOICE_MENU_130 = texture("choice_menu_150x130.png", 150, 130);
    private static final Texture CHOICE_MENU_160 = texture("choice_menu_150x160.png", 150, 160);
    private static final Texture CHOICE_MENU_190 = texture("choice_menu_150x190.png", 150, 190);

    private static final Texture MODULES_IDLE = icon("icons/modules_32x32_idle.png", 32, 32);
    private static final Texture MODULES_ACTIVE = icon("icons/modules_32x32_active.png", 32, 32);
    private static final Texture MISC_IDLE = icon("icons/misc_32x32_idle.png", 32, 32);
    private static final Texture MISC_ACTIVE = icon("icons/misc_32x32_active.png", 32, 32);
    private static final Texture MINING_IDLE = icon("icons/mining_32x32_idle.png", 32, 32);
    private static final Texture MINING_ACTIVE = icon("icons/mining_32x32_active.png", 32, 32);
    private static final Texture RENDER_IDLE = icon("icons/render_32x32_idle.png", 32, 32);
    private static final Texture RENDER_ACTIVE = icon("icons/render_32x32_active.png", 32, 32);
    private static final Texture QOL_IDLE = icon("icons/qol_32x32_idle.png", 32, 32);
    private static final Texture QOL_ACTIVE = icon("icons/qol_32x32_active.png", 32, 32);
    private static final Texture ROUTE = icon("icons/route_28x28_idle.png", 28, 28);
    private static final Texture HUD = icon("icons/hud_28x28_idle.png", 28, 28);
    private static final Texture CONFIG = icon("icons/config_28x28_idle.png", 28, 28);
    private static final Texture EXIT = icon("icons/exit_28x28_idle.png", 28, 28);

    private UiAssets() {
    }

    public static Texture navigation(String name, boolean active) {
        return switch (name) {
            case "modules" -> active ? MODULES_ACTIVE : MODULES_IDLE;
            case "misc" -> active ? MISC_ACTIVE : MISC_IDLE;
            case "mining" -> active ? MINING_ACTIVE : MINING_IDLE;
            case "render" -> active ? RENDER_ACTIVE : RENDER_IDLE;
            case "qol" -> active ? QOL_ACTIVE : QOL_IDLE;
            default -> throw new IllegalArgumentException("Unknown navigation icon: " + name);
        };
    }

    public static Texture footer(String name) {
        return switch (name) {
            case "route" -> ROUTE;
            case "hud" -> HUD;
            case "config" -> CONFIG;
            case "exit" -> EXIT;
            default -> throw new IllegalArgumentException("Unknown footer icon: " + name);
        };
    }

    /** Returns the authored multi-select menu surface for its existing logical height. */
    public static Texture choiceMenu(int height) {
        return switch (height) {
            case 40 -> CHOICE_MENU_40;
            case 70 -> CHOICE_MENU_70;
            case 100 -> CHOICE_MENU_100;
            case 130 -> CHOICE_MENU_130;
            case 160 -> CHOICE_MENU_160;
            case 190 -> CHOICE_MENU_190;
            default -> throw new IllegalArgumentException("Unsupported choice menu height: " + height);
        };
    }

    private static Texture texture(String path, int width, int height) {
        return texture(path, width, height, PHYSICAL_SCALE);
    }

    private static Texture texture(String path, int width, int height, int physicalScale) {
        return new Texture(
            Identifier.fromNamespaceAndPath("astrail", ROOT + path),
            width,
            height,
            width * physicalScale,
            height * physicalScale
        );
    }

    private static Texture icon(String path, int width, int height) {
        return texture(path, width, height);
    }

    private static Texture spriteSheet(String path, int width, int height, int frames, int physicalScale) {
        int frameTextureWidth = width * physicalScale;
        int frameTextureHeight = height * physicalScale;
        return new Texture(
            Identifier.fromNamespaceAndPath("astrail", ROOT + path),
            width,
            height,
            frameTextureWidth,
            frameTextureHeight * frames,
            frameTextureWidth,
            frameTextureHeight,
            frames
        );
    }

    public record Texture(
        Identifier id,
        int width,
        int height,
        int textureWidth,
        int textureHeight,
        int frameTextureWidth,
        int frameTextureHeight,
        int frameCount
    ) {
        public Texture(Identifier id, int width, int height, int textureWidth, int textureHeight) {
            this(id, width, height, textureWidth, textureHeight, textureWidth, textureHeight, 1);
        }

        /** Draw at the authored native size. UI scaling is applied by UiScaleSystem. */
        public void draw(GuiGraphicsExtractor graphics, int x, int y) {
            drawFrame(graphics, x, y, 0);
        }

        /** Draw with ARGB modulation for crossfades and interaction feedback. */
        public void draw(GuiGraphicsExtractor graphics, int x, int y, int color) {
            drawFrame(graphics, x, y, 0, color);
        }

        /** Draw one vertical sprite-sheet frame at the authored native size. */
        public void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int frame) {
            drawFrame(graphics, x, y, frame, -1);
        }

        /** Draw one vertical sprite-sheet frame with ARGB modulation. */
        public void drawFrame(GuiGraphicsExtractor graphics, int x, int y, int frame, int color) {
            int clampedFrame = Math.max(0, Math.min(frame, frameCount - 1));
            graphics.blit(
                RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, clampedFrame * (float) frameTextureHeight,
                width, height, frameTextureWidth, frameTextureHeight, textureWidth, textureHeight,
                UiMotion.applyFrameOpacity(color)
            );
        }

        /** Draw the full frame resampled to a custom logical size. */
        public void drawScaled(GuiGraphicsExtractor graphics, int x, int y, int targetWidth, int targetHeight) {
            drawScaled(graphics, x, y, targetWidth, targetHeight, -1);
        }

        /** Draw the full frame resampled to a custom logical size, with ARGB modulation. */
        public void drawScaled(GuiGraphicsExtractor graphics, int x, int y, int targetWidth, int targetHeight, int color) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F,
                targetWidth, targetHeight, frameTextureWidth, frameTextureHeight, textureWidth, textureHeight,
                UiMotion.applyFrameOpacity(color)
            );
        }

        /** Draws a left-anchored portion without stretching the source texture. */
        public void drawCropped(GuiGraphicsExtractor graphics, int x, int y, int visibleWidth, int color) {
            int clampedWidth = Math.max(0, Math.min(width, visibleWidth));
            if (clampedWidth == 0) return;
            int sourceWidth = Math.max(1, Math.round(frameTextureWidth * (clampedWidth / (float) width)));
            graphics.blit(
                RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F,
                clampedWidth, height, sourceWidth, frameTextureHeight, textureWidth, textureHeight,
                UiMotion.applyFrameOpacity(color)
            );
        }
    }
}
