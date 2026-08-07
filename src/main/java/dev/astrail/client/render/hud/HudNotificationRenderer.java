package dev.astrail.client.render.hud;

import dev.astrail.client.api.module.ClientModule;
import dev.astrail.client.api.module.ModuleState;
import dev.astrail.client.api.service.NotificationService;
import dev.astrail.client.ui.component.TextRenderer;
import dev.astrail.client.ui.component.UiAssets;
import dev.astrail.client.ui.component.UiMotion;
import dev.astrail.client.ui.component.UiTextStyle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Compact bottom-right adaptation of Uiverse's rude-tiger-29 notification.
 * The shell is an 8x supersampled texture, while the complete toast is scaled
 * from a fixed 1920x1080 design viewport and then mapped through GUI scale.
 */
public final class HudNotificationRenderer implements NotificationService {
    private static final TextRenderer UI_TEXT = TextRenderer.instance();
    private static final int CARD_WIDTH = HudGeometry.TOAST_CARD_WIDTH;
    private static final int CARD_HEIGHT = HudGeometry.TOAST_CARD_HEIGHT;
    private static final int CARD_GAP = HudGeometry.TOAST_CARD_GAP;
    private static final int TEXT_X = 15;
    private static final int TEXT_WIDTH = CARD_WIDTH - TEXT_X - 12;
    private static final long ENTER_MILLIS = 150L;
    private static final long HOLD_MILLIS = 1_350L;
    private static final long EXIT_MILLIS = 180L;
    private static final long STATE_MILLIS = 140L;
    private static final long TOTAL_MILLIS = ENTER_MILLIS + HOLD_MILLIS + EXIT_MILLIS;
    private static final float RESTACK_RESPONSE_SECONDS = 0.085F;
    private static final int TITLE = 0xFF32A6FF;
    private static final int BODY = 0xFF99999D;
    private static final long REVEAL_MILLIS = 180L;
    private static final Notice PREVIEW_NOTICE = new Notice(
        "editor.preview", "Auto Mining", "Enabled", true, true, "Enabled", 0L, 0L, 0L
    );

    private final Deque<Notice> notices = new ArrayDeque<>();
    private final HudLayout layout;

    /** True while the element is not being drawn, so the next draw fades in. */
    private boolean suppressed = true;
    private long revealStartedAt;

    public HudNotificationRenderer(HudLayout layout) {
        this.layout = layout;
    }

    public void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.OVERLAY_MESSAGE,
            Identifier.fromNamespaceAndPath("astrail", "module_notifications"),
            this::extractRenderState
        );
    }

    public synchronized void showModuleState(ClientModule module) {
        boolean enabled = module.state() == ModuleState.ENABLED;
        show(module.metadata().id(), module.metadata().displayName(), enabled ? "Enabled" : "Disabled", enabled);
    }

    @Override
    public synchronized void show(String owner, String title, String message, boolean positive) {
        Notice previous = notices.stream().filter(notice -> notice.owner().equals(owner)).findFirst().orElse(null);
        notices.removeIf(notice -> notice.owner().equals(owner));
        long now = System.currentTimeMillis();
        Notice notice = new Notice(
            owner,
            title,
            message,
            positive,
            previous == null ? positive : previous.enabled(),
            previous == null ? message : previous.message(),
            now,
            previous == null ? now : now - ENTER_MILLIS,
            now
        );
        if (previous != null) {
            notice.animY = previous.animY;
            notice.lastFrameMillis = previous.lastFrameMillis;
        }
        notices.addFirst(notice);
        while (notices.size() > 3) {
            notices.removeLast();
        }
    }

    private void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        // Snapshot before the visibility gates so hidden or editor-suspended
        // toasts keep expiring instead of piling up for a burst replay later.
        List<Notice> visible = snapshot(now);
        if (layout.editorActive() || !layout.toastVisible()) {
            suppressed = true;
            return;
        }
        if (suppressed) {
            suppressed = false;
            revealStartedAt = now;
        }
        // Coming back from hidden or from the editor eases in rather than pops.
        float reveal = UiMotion.easeOutQuint((now - revealStartedAt) / (float) REVEAL_MILLIS);
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        float physicalToScaled = HudGeometry.physicalToScaled(
            client.getWindow().getWidth(),
            client.getWindow().getHeight(),
            (float) client.getWindow().getGuiScale()
        );
        float displayScale = HudGeometry.displayScale(physicalToScaled);
        HudGeometry.Anchor anchor = HudGeometry.toastAnchor(layout, screenWidth, screenHeight, physicalToScaled);
        float anchorX = anchor.x();
        float anchorY = anchor.y();
        float previousReveal = UiMotion.pushFrameOpacity(reveal);
        try {
            int slot = 0;
            for (Notice notice : visible) {
                float target = slot * (CARD_HEIGHT + CARD_GAP);
                float deltaSeconds = notice.lastFrameMillis() == 0L
                    ? 0.016F
                    : Math.min(0.05F, (now - notice.lastFrameMillis()) / 1_000.0F);
                notice.lastFrameMillis(now);
                float animY = notice.animY();
                notice.animY(Float.isNaN(animY)
                    ? target
                    : UiMotion.damp(animY, target, deltaSeconds, RESTACK_RESPONSE_SECONDS));

                long age = now - notice.createdAt();
                float visibility = visibility(notice, now, age);
                drawNotice(graphics, notice, anchorX, anchorY, notice.animY(), displayScale, now, visibility);
                slot++;
            }
        } finally {
            UiMotion.popFrameOpacity(previousReveal);
        }
    }

    private synchronized List<Notice> snapshot(long now) {
        notices.removeIf(notice -> now - notice.createdAt() >= TOTAL_MILLIS);
        return new ArrayList<>(notices);
    }

    /** Draws one settled sample toast for the HUD editor's draggable preview. */
    public static void drawPreview(
        GuiGraphicsExtractor graphics,
        float anchorX,
        float anchorY,
        float displayScale,
        long now
    ) {
        drawNotice(graphics, PREVIEW_NOTICE, anchorX, anchorY, 0.0F, displayScale, now, 1.0F, 1.0F);
    }

    private static float visibility(Notice notice, long now, long age) {
        long enterAge = now - notice.enterStartedAt();
        if (enterAge < ENTER_MILLIS) {
            return UiMotion.easeOutQuint(enterAge / (float) ENTER_MILLIS);
        }
        if (age < ENTER_MILLIS + HOLD_MILLIS) {
            return 1.0F;
        }
        float exit = UiMotion.clamp01((age - ENTER_MILLIS - HOLD_MILLIS) / (float) EXIT_MILLIS);
        return 1.0F - UiMotion.easeInOutCubic(exit);
    }


    private static void drawNotice(
        GuiGraphicsExtractor graphics,
        Notice notice,
        float anchorX,
        float anchorY,
        float localY,
        float displayScale,
        long now,
        float visibility
    ) {
        float stateProgress = notice.previousEnabled() == notice.enabled()
            ? 1.0F
            : UiMotion.easeOutQuint((now - notice.stateChangedAt()) / (float) STATE_MILLIS);
        drawNotice(graphics, notice, anchorX, anchorY, localY, displayScale, now, visibility, stateProgress);
    }

    private static void drawNotice(
        GuiGraphicsExtractor graphics,
        Notice notice,
        float anchorX,
        float anchorY,
        float localY,
        float displayScale,
        long now,
        float visibility,
        float stateProgress
    ) {

        // The entire toast shares one eased transform so its shell and text settle together.
        float scale = 0.96F + 0.04F * visibility;
        float slideX = (1.0F - visibility) * 24.0F;
        float slideY = (1.0F - visibility) * 5.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(
            anchorX + slideX * displayScale,
            anchorY - (localY + CARD_HEIGHT) * displayScale + slideY * displayScale
        );
        graphics.pose().scale(displayScale, displayScale);
        graphics.pose().translate(-CARD_WIDTH, 0.0F);
        graphics.pose().translate(CARD_WIDTH * 0.5F, CARD_HEIGHT * 0.5F);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-CARD_WIDTH * 0.5F, -CARD_HEIGHT * 0.5F);

        float previousOpacity = UiMotion.pushFrameOpacity(visibility);
        UiAssets.NOTIFICATION_TOAST.draw(graphics, 0, 0);

        UI_TEXT.drawLeftText(
            graphics, UI_TEXT.trim(notice.title(), TEXT_WIDTH, UiTextStyle.AUXILIARY_MEDIUM),
            TEXT_X, 20, UiTextStyle.AUXILIARY_MEDIUM, TITLE
        );

        if (stateProgress < 1.0F) {
            drawStatusText(graphics, notice.previousMessage(), 1.0F - stateProgress);
        }
        drawStatusText(graphics, notice.message(), stateProgress);
        UiMotion.popFrameOpacity(previousOpacity);
        graphics.pose().popMatrix();
    }

    private static void drawStatusText(
        GuiGraphicsExtractor graphics,
        String message,
        float opacity
    ) {
        UI_TEXT.drawLeftText(
            graphics,
            UI_TEXT.trim(message, TEXT_WIDTH, UiTextStyle.AUXILIARY),
            TEXT_X,
            42,
            UiTextStyle.AUXILIARY,
            UiMotion.multiplyAlpha(BODY, opacity)
        );
    }

    private static final class Notice {
        private final String owner;
        private final String title;
        private final String message;
        private final boolean enabled;
        private final boolean previousEnabled;
        private final String previousMessage;
        private final long createdAt;
        private final long enterStartedAt;
        private final long stateChangedAt;
        private float animY = Float.NaN;
        private long lastFrameMillis;

        private Notice(
            String owner,
            String title,
            String message,
            boolean enabled,
            boolean previousEnabled,
            String previousMessage,
            long createdAt,
            long enterStartedAt,
            long stateChangedAt
        ) {
            this.owner = owner;
            this.title = title;
            this.message = message;
            this.enabled = enabled;
            this.previousEnabled = previousEnabled;
            this.previousMessage = previousMessage;
            this.createdAt = createdAt;
            this.enterStartedAt = enterStartedAt;
            this.stateChangedAt = stateChangedAt;
        }

        private String owner() {
            return owner;
        }

        private String title() {
            return title;
        }

        private String message() {
            return message;
        }

        private boolean enabled() {
            return enabled;
        }

        private boolean previousEnabled() {
            return previousEnabled;
        }

        private String previousMessage() {
            return previousMessage;
        }

        private long createdAt() {
            return createdAt;
        }

        private long enterStartedAt() {
            return enterStartedAt;
        }

        private long stateChangedAt() {
            return stateChangedAt;
        }

        private float animY() {
            return animY;
        }

        private void animY(float value) {
            animY = value;
        }

        private long lastFrameMillis() {
            return lastFrameMillis;
        }

        private void lastFrameMillis(long value) {
            lastFrameMillis = value;
        }
    }

}
