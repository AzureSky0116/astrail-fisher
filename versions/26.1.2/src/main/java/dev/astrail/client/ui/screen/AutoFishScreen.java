package dev.astrail.client.ui.screen;

import dev.astrail.client.api.module.ClientModule;
import dev.astrail.client.api.module.ModuleState;
import dev.astrail.client.api.setting.BooleanSetting;
import dev.astrail.client.api.setting.KeybindSetting;
import dev.astrail.client.api.setting.NumberSetting;
import dev.astrail.client.api.setting.Setting;
import dev.astrail.client.core.ClientRuntime;
import dev.astrail.client.ui.component.TextRenderer;
import dev.astrail.client.ui.component.UiAssets;
import dev.astrail.client.ui.component.UiDraw;
import dev.astrail.client.ui.component.UiMotion;
import dev.astrail.client.ui.component.UiRect;
import dev.astrail.client.ui.component.UiTextStyle;
import dev.astrail.client.ui.component.UiTheme;
import dev.astrail.client.ui.layout.UiScaleSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Single-module settings page in the Astrail style. The vanilla "Open Auto
 * Fishing GUI" key binding (default Right Shift, rebindable in Options ->
 * Controls) opens exactly the Auto Fishing module page and nothing else.
 */
public final class AutoFishScreen extends Screen {
    private static final TextRenderer UI = TextRenderer.instance();
    private static final String MODULE_ID = "macro.auto_fish";
    private static final boolean REDUCE_MOTION = Boolean.getBoolean("astrail.reduceMotion");

    private static final int PANEL_X = UiScaleSystem.PANEL_X;
    private static final int PANEL_Y = UiScaleSystem.PANEL_Y;
    private static final int PANEL_WIDTH = UiScaleSystem.PANEL_WIDTH;
    private static final int CONTENT_X = PANEL_X + 40;
    private static final int CONTENT_WIDTH = PANEL_WIDTH - 80;
    private static final int COLUMN_GAP = 24;
    private static final int COLUMNS = 3;
    private static final int ROW_WIDTH = (CONTENT_WIDTH - COLUMN_GAP * (COLUMNS - 1)) / COLUMNS;
    private static final int ROW_HEIGHT = 88;
    private static final int ROW_Y = PANEL_Y + 244;
    private static final int ROW_STEP = 94;
    /** Vertical spacing between wrapped description lines (AUXILIARY line height). */
    private static final int DESCRIPTION_LINE_HEIGHT = 15;

    /** Motion timings mirror the Astrail menu screen exactly. */
    private static final long OPEN_DURATION_MILLIS = 220L;
    private static final long CLOSE_DURATION_MILLIS = 190L;
    private static final long ROW_STAGGER_MILLIS = 24L;
    private static final long ROW_DURATION_MILLIS = 200L;
    private static final long ROW_PULSE_MILLIS = 180L;
    private static final long STATUS_PULSE_MILLIS = 260L;
    private static final float HOVER_RESPONSE_SECONDS = 0.055F;

    private final ClientRuntime runtime;
    private final Screen parent;
    private final List<SettingRow> rows = new ArrayList<>();

    private final long openedAt = System.currentTimeMillis();
    private boolean closing;
    private long closeStartedAt;
    private long lastFrameNanos = System.nanoTime();
    private float frameDeltaSeconds = 1.0F / 60.0F;
    private final Map<String, Float> hoverAmounts = new HashMap<>();
    private final Map<Setting<?>, Float> booleanToggleAmounts = new HashMap<>();
    private final Map<Setting<?>, Long> settingPulseStartedAt = new HashMap<>();
    private long statusToggleStartedAt;
    private boolean previousStatus;

    private UiRect statusHit = UiRect.EMPTY;
    private UiRect closeHit = UiRect.EMPTY;
    private UiRect keybindHit = UiRect.EMPTY;
    private boolean capturingKeybind;
    private Setting<?> draggingSetting;
    private double mouseDesignX;
    private double mouseDesignY;

    private record SettingRow(Setting<?> setting, UiRect row, UiRect slider) {
    }

    public AutoFishScreen(ClientRuntime runtime, Screen parent) {
        super(Component.literal("Auto Fishing"));
        this.runtime = runtime;
        this.parent = parent;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float visibility = closing ? closingProgress() : 1.0F;
        if (!closing) graphics.blurBeforeThisStratum();
        graphics.fill(0, 0, width, height, UiMotion.multiplyAlpha(0x7203070D, visibility));
        graphics.fillGradient(
            0, 0, width, height,
            UiMotion.multiplyAlpha(0x74101B2D, visibility),
            UiMotion.multiplyAlpha(0xA004080F, visibility)
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiScaleSystem uiScale = UiScaleSystem.fit(width, height);
        mouseDesignX = uiScale.designX(mouseX);
        mouseDesignY = uiScale.designY(mouseY);
        updateMotion();
        if (closing && closingProgress() <= 0.0F) {
            finishClose();
            return;
        }
        rows.clear();
        uiScale.push(graphics);
        float presentation = presentationProgress();
        float presentationScale = REDUCE_MOTION ? 1.0F : 0.972F + 0.028F * presentation;
        float previousFrameOpacity = UiMotion.pushFrameOpacity(closing ? closingProgress() : 1.0F);
        try {
            graphics.pose().translate(
                UiScaleSystem.DESIGN_WIDTH * 0.5F,
                UiScaleSystem.DESIGN_HEIGHT * 0.5F + (1.0F - presentation) * 8.0F
            );
            graphics.pose().scale(presentationScale, presentationScale);
            graphics.pose().translate(-UiScaleSystem.DESIGN_WIDTH * 0.5F, -UiScaleSystem.DESIGN_HEIGHT * 0.5F);
            drawPanel(graphics);
        } finally {
            UiMotion.popFrameOpacity(previousFrameOpacity);
            uiScale.pop(graphics);
        }
        if (!closing && presentation < 1.0F) {
            int alpha = Math.round((1.0F - presentation) * 150.0F);
            graphics.fill(0, 0, width, height, alpha << 24);
        }
    }

    private void drawShell(GuiGraphicsExtractor graphics) {
        UiRect panel = new UiRect(PANEL_X, PANEL_Y, PANEL_WIDTH, UiScaleSystem.PANEL_HEIGHT);
        UiDraw.card(graphics, panel, 20, UiTheme.GLASS, UiTheme.BORDER_BRIGHT);

        UiRect header = new UiRect(panel.x() + 2, panel.y() + 2, panel.width() - 4, 116);
        UiDraw.fillRounded(graphics, header, 18, UiTheme.HEADER);
        UiDraw.fillRounded(
            graphics,
            new UiRect(header.x(), header.y() + 52, header.width(), header.height() - 52),
            0,
            UiTheme.HEADER
        );
        UiDraw.fillRounded(graphics, new UiRect(CONTENT_X, PANEL_Y + 116, CONTENT_WIDTH, 1), 0, UiTheme.BORDER);
    }

    private void drawPanel(GuiGraphicsExtractor graphics) {
        drawShell(graphics);
        ClientModule module = module();
        if (module == null) {
            UI.drawCenteredText(
                graphics, "AUTO FISHING NOT AVAILABLE",
                PANEL_X + PANEL_WIDTH / 2.0F, PANEL_Y + 60,
                UiTextStyle.MODULE_TITLE, UiTheme.DANGER
            );
            return;
        }
        boolean enabled = module.state() == ModuleState.ENABLED;

        UiAssets.WORDMARK.draw(graphics, PANEL_X + 36, PANEL_Y + 30);
        UI.drawCenteredText(
            graphics, "AUTO FISHING", PANEL_X + PANEL_WIDTH / 2.0F, PANEL_Y + 42,
            UiTextStyle.MODULE_TITLE, UiTheme.TEXT
        );
        UI.drawCenteredText(
            graphics,
            enabled ? "Running - the fishing loop is active." : "Ready - the fishing loop is paused.",
            PANEL_X + PANEL_WIDTH / 2.0F, PANEL_Y + 74,
            UiTextStyle.AUXILIARY, UiTheme.TEXT_SOFT
        );

        statusHit = new UiRect(PANEL_X + PANEL_WIDTH - 132, PANEL_Y + 25, 62, 62);
        drawStatusBadge(graphics, enabled);

        closeHit = new UiRect(PANEL_X + PANEL_WIDTH - 54, PANEL_Y + 40, 32, 32);
        drawCloseButton(graphics);

        drawKeybindStrip(graphics, module, new UiRect(CONTENT_X, PANEL_Y + 130, CONTENT_WIDTH, 68));

        UI.drawLeftText(graphics, "OPTIONS", CONTENT_X, PANEL_Y + 228,
            UiTextStyle.SETTING_LABEL, UiTheme.TEXT_SOFT);
        UiDraw.fillRounded(
            graphics,
            new UiRect(CONTENT_X + 108, PANEL_Y + 227, CONTENT_WIDTH - 108, 1),
            0,
            UiTheme.BORDER
        );

        int settingIndex = 0;
        for (Setting<?> setting : module.settings()) {
            if (setting instanceof KeybindSetting) {
                continue;
            }
            int column = settingIndex % COLUMNS;
            int rowNumber = settingIndex / COLUMNS;
            float rowEase = REDUCE_MOTION
                ? 1.0F
                : staggeredEase(openedAt, settingIndex, ROW_STAGGER_MILLIS, ROW_DURATION_MILLIS);
            float rowOpacity = 0.38F + rowEase * 0.62F;
            int y = ROW_Y + rowNumber * ROW_STEP + Math.round((1.0F - rowEase) * 12.0F);
            int x = CONTENT_X + column * (ROW_WIDTH + COLUMN_GAP);
            UiRect row = new UiRect(x, y, ROW_WIDTH, ROW_HEIGHT);
            boolean interactive = rowEase >= 0.9F;
            boolean hovered = interactive && row.contains(mouseDesignX, mouseDesignY);
            float hover = hoverAmount("setting:" + setting.id(), hovered);
            drawSettingRow(graphics, setting, row, hover, rowOpacity);
            if (interactive) {
                rows.add(toRow(setting, row));
            }
            settingIndex++;
        }
    }

    private void drawStatusBadge(GuiGraphicsExtractor graphics, boolean enabled) {
        UiDraw.statusBadge(graphics, statusHit.x(), statusHit.y(), enabled, 0xFFFFFFFF);
        if (statusToggleStartedAt == 0L) return;
        float progress = UiMotion.clamp01(
            (System.currentTimeMillis() - statusToggleStartedAt) / (float) STATUS_PULSE_MILLIS
        );
        if (progress >= 1.0F) {
            statusToggleStartedAt = 0L;
            return;
        }
        if (previousStatus != enabled) {
            float blend = UiMotion.easeOutQuint(progress);
            UiDraw.statusBadge(
                graphics, statusHit.x(), statusHit.y(), previousStatus,
                UiMotion.multiplyAlpha(0xFFFFFFFF, (1.0F - blend) * 0.9F)
            );
        }
        float pulse = (float) Math.sin(Math.PI * progress);
        if (pulse > 0.01F) {
            UiDraw.fillRounded(
                graphics, statusHit.expand(3), 15,
                UiMotion.multiplyAlpha(enabled ? UiTheme.SUCCESS : UiTheme.DANGER, pulse * (enabled ? 0.55F : 0.35F))
            );
        }
    }

    private void drawCloseButton(GuiGraphicsExtractor graphics) {
        boolean hovered = closeHit.contains(mouseDesignX, mouseDesignY);
        float hover = hoverAmount("close", hovered);
        UiDraw.fillRounded(
            graphics, closeHit, 8,
            UiDraw.lerpColor(UiTheme.CONTROL, UiTheme.CONTROL_HOVER, hover)
        );
        UiAssets.CLOSE.draw(
            graphics, closeHit.x() + 4, closeHit.y() + 4,
            UiDraw.lerpColor(UiTheme.TEXT_SOFT, UiTheme.TEXT, hover)
        );
    }

    private void drawKeybindStrip(GuiGraphicsExtractor graphics, ClientModule module, UiRect strip) {
        boolean hovered = strip.contains(mouseDesignX, mouseDesignY);
        float hover = hoverAmount("keybind", hovered);
        UiDraw.fillRounded(graphics, strip, 10, UiDraw.lerpColor(UiTheme.CARD, UiTheme.CARD_HOVER, hover));
        UiAssets.KEYBIND.draw(graphics, strip.x() + 18, strip.y() + 24);
        UI.drawLeftText(graphics, "KEYBIND", strip.x() + 50, strip.y() + 24,
            UiTextStyle.SETTING_LABEL, UiTheme.TEXT);

        KeybindSetting keybind = module.keybind();
        UiRect keybindField = new UiRect(strip.right() - 224, strip.y() + 8, 200, 52);
        keybindHit = keybindField;
        UiDraw.controlField(graphics, keybindField, 0xFFFFFFFF);
        UI.drawCenteredText(
            graphics,
            capturingKeybind ? "PRESS KEY" : keybind.displayValue(),
            keybindField.x() + keybindField.width() / 2.0F,
            keybindField.y() + 28,
            UiTextStyle.AUXILIARY_MEDIUM,
            capturingKeybind ? UiMotion.multiplyAlpha(UiTheme.WARNING, cursorPulse()) : UiTheme.TEXT
        );
    }

    private void drawSettingRow(
        GuiGraphicsExtractor graphics,
        Setting<?> setting,
        UiRect row,
        float hover,
        float rowOpacity
    ) {
        UiAssets.SETTING_ROW_BASE.drawScaled(
            graphics, row.x(), row.y(), row.width(), row.height(),
            UiMotion.multiplyAlpha(0xFFFFFFFF, rowOpacity)
        );
        if (hover > 0.01F) {
            UiAssets.SETTING_ROW_HOVER.drawScaled(
                graphics, row.x(), row.y(), row.width(), row.height(),
                UiMotion.multiplyAlpha(0xFFFFFFFF, hover * rowOpacity)
            );
        }
        Long pulseStarted = settingPulseStartedAt.get(setting);
        if (pulseStarted != null) {
            float pulse = 1.0F - UiMotion.clamp01(
                (System.currentTimeMillis() - pulseStarted) / (float) ROW_PULSE_MILLIS
            );
            if (pulse > 0.0F) {
                UiAssets.SETTING_ROW_HOVER.drawScaled(
                    graphics, row.x(), row.y(), row.width(), row.height(),
                    UiMotion.multiplyAlpha(0xFFFFFFFF, pulse * 0.72F * rowOpacity)
                );
            } else {
                settingPulseStartedAt.remove(setting);
            }
        }

        UI.drawLeftText(graphics, setting.displayName(), row.x() + 18, row.y() + 27,
            UiTextStyle.SETTING_LABEL, UiTheme.TEXT);
        if (setting instanceof BooleanSetting booleanSetting) {
            UiDraw.toggleSwitch(
                graphics,
                new UiRect(row.right() - 80, row.y() + 30, 62, 32),
                booleanSetting.get(),
                booleanToggleAmount(booleanSetting),
                0xFFFFFFFF
            );
            drawDescription(graphics, setting, row, row.y() + 55, row.right() - 120, 2);
            return;
        }
        if (setting instanceof NumberSetting numberSetting) {
            double min = numberSetting.minimum();
            double max = numberSetting.maximum();
            double progress = max > min ? Mth.clamp((numberSetting.get() - min) / (max - min), 0.0D, 1.0D) : 0.0D;
            String value = formatNumber(numberSetting.get(), numberSetting.step());
            UI.drawRightText(graphics, value, row.right() - 20, row.y() + 27,
                UiTextStyle.AUXILIARY_MEDIUM, UiTheme.TEXT);
            drawDescription(graphics, setting, row, row.y() + 55, row.right() - 120, 1);
            UiRect slider = new UiRect(row.x() + 18, row.y() + 72, row.width() - 36, 12);
            UiDraw.slider(graphics, slider, (float) progress, 0xFFFFFFFF);
        }
    }

    private void drawDescription(
        GuiGraphicsExtractor graphics,
        Setting<?> setting,
        UiRect row,
        int firstLineCenterY,
        int rightLimit,
        int maxLines
    ) {
        String description = setting.description();
        if (description == null || description.isBlank()) return;
        float maxWidth = Math.max(0.0F, rightLimit - row.x() - 18);
        List<String> lines = wrapDescription(description, maxWidth, maxLines);
        for (int line = 0; line < lines.size(); line++) {
            UI.drawLeftText(
                graphics,
                lines.get(line),
                row.x() + 18,
                firstLineCenterY + line * DESCRIPTION_LINE_HEIGHT,
                UiTextStyle.AUXILIARY,
                UiTheme.MUTED
            );
        }
    }

    /** Wraps a description into at most {@code maxLines} lines; overflow gets an ellipsis. */
    private static List<String> wrapDescription(String text, float maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String[] words = text.split("\\s+");
        for (int index = 0; index < words.length; index++) {
            String candidate = line.length() == 0 ? words[index] : line + " " + words[index];
            if (line.length() > 0 && UI.width(candidate, UiTextStyle.AUXILIARY) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(words[index]);
                if (lines.size() == maxLines) {
                    StringBuilder rest = new StringBuilder(line);
                    for (int restIndex = index + 1; restIndex < words.length; restIndex++) {
                        rest.append(' ').append(words[restIndex]);
                    }
                    lines.set(lines.size() - 1, UI.trim(rest.toString(), maxWidth, UiTextStyle.AUXILIARY));
                    return lines;
                }
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private SettingRow toRow(Setting<?> setting, UiRect row) {
        if (setting instanceof NumberSetting) {
            return new SettingRow(setting, row, new UiRect(row.x() + 18, row.y() + 72, row.width() - 36, 12));
        }
        return new SettingRow(setting, row, UiRect.EMPTY);
    }

    private static String formatNumber(double value, double step) {
        if (step >= 1.0D) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private void updateMotion() {
        long now = System.nanoTime();
        frameDeltaSeconds = Math.min(0.05F, Math.max(0.0F, (now - lastFrameNanos) / 1_000_000_000.0F));
        lastFrameNanos = now;
    }

    private float hoverAmount(String key, boolean hovered) {
        float target = hovered ? 1.0F : 0.0F;
        float current = hoverAmounts.getOrDefault(key, 0.0F);
        float value = REDUCE_MOTION
            ? target
            : UiMotion.damp(current, target, frameDeltaSeconds, HOVER_RESPONSE_SECONDS);
        if (value < 0.002F && !hovered) {
            hoverAmounts.remove(key);
        } else {
            hoverAmounts.put(key, value);
        }
        return value;
    }

    private float booleanToggleAmount(BooleanSetting setting) {
        float target = setting.get() ? 1.0F : 0.0F;
        float current = booleanToggleAmounts.getOrDefault(setting, target);
        float value = REDUCE_MOTION
            ? target
            : UiMotion.damp(current, target, frameDeltaSeconds, HOVER_RESPONSE_SECONDS);
        booleanToggleAmounts.put(setting, value);
        return value;
    }

    private void pulseSetting(Setting<?> setting) {
        if (!REDUCE_MOTION) {
            settingPulseStartedAt.put(setting, System.currentTimeMillis());
        }
    }

    private float openingProgress() {
        if (REDUCE_MOTION) return 1.0F;
        return UiMotion.easeOutQuint((System.currentTimeMillis() - openedAt) / (float) OPEN_DURATION_MILLIS);
    }

    private float closingProgress() {
        if (!closing) return 1.0F;
        if (REDUCE_MOTION) return 0.0F;
        float progress = UiMotion.clamp01((System.currentTimeMillis() - closeStartedAt) / (float) CLOSE_DURATION_MILLIS);
        return 1.0F - UiMotion.easeInOutCubic(progress);
    }

    private float presentationProgress() {
        return openingProgress() * closingProgress();
    }

    /** Progress of one row of a staggered entrance, clamped and eased. */
    private static float staggeredEase(long startedAt, int row, long stepMillis, long durationMillis) {
        if (startedAt == 0L) return 1.0F;
        long elapsed = System.currentTimeMillis() - startedAt - row * stepMillis;
        return UiMotion.easeOutQuint(elapsed / (float) durationMillis);
    }

    /** Soft cursor pulse shared with the Astrail search/number editors. */
    private static float cursorPulse() {
        double phase = (System.currentTimeMillis() % 1_100L) / 1_100.0D;
        return 0.25F + 0.75F * (0.5F + 0.5F * (float) Math.cos(phase * Math.PI * 2.0D));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        UiScaleSystem uiScale = UiScaleSystem.fit(width, height);
        double mx = uiScale.designX(event.x());
        double my = uiScale.designY(event.y());
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (closeHit.contains(mx, my)) {
            onClose();
            return true;
        }
        if (capturingKeybind) return true;
        if (statusHit.contains(mx, my)) {
            toggleModule();
            return true;
        }
        if (keybindHit.contains(mx, my)) {
            capturingKeybind = true;
            return true;
        }
        for (SettingRow row : rows) {
            if (row.row().contains(mx, my)) {
                if (row.setting() instanceof BooleanSetting booleanSetting) {
                    booleanSetting.set(!booleanSetting.get());
                    runtime.modules().configurationChanged();
                    pulseSetting(booleanSetting);
                    return true;
                }
                if (row.setting() instanceof NumberSetting numberSetting) {
                    dragNumber(uiScale, row, mx);
                    draggingSetting = numberSetting;
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingSetting instanceof NumberSetting) {
            UiScaleSystem uiScale = UiScaleSystem.fit(width, height);
            for (SettingRow row : rows) {
                if (row.setting() == draggingSetting) {
                    dragNumber(uiScale, row, uiScale.designX(event.x()));
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingSetting = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        UiScaleSystem uiScale = UiScaleSystem.fit(width, height);
        double mx = uiScale.designX(mouseX);
        double my = uiScale.designY(mouseY);
        if (vertical == 0.0D) return true;
        for (SettingRow row : rows) {
            if (row.setting() instanceof NumberSetting numberSetting && row.row().contains(mx, my)) {
                numberSetting.set(numberSetting.get() + Math.copySign(numberSetting.step(), vertical));
                runtime.modules().configurationChanged();
                pulseSetting(numberSetting);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturingKeybind) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                capturingKeybind = false;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT || event.key() == GLFW.GLFW_KEY_ENTER) {
                capturingKeybind = false;
                return true;
            }
            ClientModule module = module();
            if (module == null) {
                capturingKeybind = false;
                return true;
            }
            KeybindSetting keybind = module.keybind();
            keybind.set(event.key());
            capturingKeybind = false;
            runtime.modules().configurationChanged();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (closing) return;
        draggingSetting = null;
        runtime.config().save();
        if (REDUCE_MOTION) {
            finishClose();
            return;
        }
        closing = true;
        closeStartedAt = System.currentTimeMillis();
    }

    /** Triggers the closing transition; used by the vanilla menu key binding. */
    public void requestClose() {
        onClose();
    }

    /** True while the keybind capture field is waiting for a key press. */
    public boolean isCapturingKeybind() {
        return capturingKeybind;
    }

    private void finishClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void dragNumber(UiScaleSystem uiScale, SettingRow row, double designX) {
        if (row == null || !(row.setting() instanceof NumberSetting numberSetting)) return;
        UiRect slider = row.slider();
        double local = Mth.clamp((designX - slider.x()) / slider.width(), 0.0D, 1.0D);
        numberSetting.set(numberSetting.minimum() + (numberSetting.maximum() - numberSetting.minimum()) * local);
        runtime.modules().configurationChanged();
        pulseSetting(numberSetting);
    }

    private void toggleModule() {
        ClientModule module = module();
        if (module == null) return;
        previousStatus = module.state() == ModuleState.ENABLED;
        runtime.modules().toggle(module.metadata().id());
        statusToggleStartedAt = REDUCE_MOTION ? 0L : System.currentTimeMillis();
    }

    private ClientModule module() {
        return runtime.modules().find(MODULE_ID).orElse(null);
    }
}