package dev.astrail.client.render.hud;

/**
 * Persisted placement and visibility of the movable HUD elements: the
 * notification toast stack and the active-modules list.
 *
 * <p>Positions are stored as fractions of the GUI-scaled screen in {@code
 * [0,1]}, so a layout survives window resizes and GUI-scale changes. {@code
 * NaN} means "no override": the renderer falls back to its built-in default
 * anchor, which keeps out-of-the-box placement pixel-identical to the
 * pre-editor behavior and makes "reset" a matter of clearing the override.
 *
 * <p>{@code editorActive} is transient session state, never persisted: while
 * the HUD editor screen is open the live renderers stand down so the editor's
 * draggable previews are the only copy on screen.
 */
public final class HudLayout {
    private float toastX = Float.NaN;
    private float toastY = Float.NaN;
    private float modulesX = Float.NaN;
    private float modulesY = Float.NaN;
    private float statsX = Float.NaN;
    private float statsY = Float.NaN;
    private boolean toastVisible = true;
    private boolean modulesVisible = true;
    private boolean statsVisible = true;
    private boolean editorActive;

    public synchronized boolean hasToastPosition() {
        return !Float.isNaN(toastX) && !Float.isNaN(toastY);
    }

    public synchronized float toastX() {
        return toastX;
    }

    public synchronized float toastY() {
        return toastY;
    }

    public synchronized void setToastPosition(double x, double y) {
        toastX = clamp01(x);
        toastY = clamp01(y);
    }

    public synchronized void resetToastPosition() {
        toastX = Float.NaN;
        toastY = Float.NaN;
    }

    public synchronized boolean toastVisible() {
        return toastVisible;
    }

    public synchronized void setToastVisible(boolean visible) {
        toastVisible = visible;
    }

    public synchronized boolean hasModulesPosition() {
        return !Float.isNaN(modulesX) && !Float.isNaN(modulesY);
    }

    public synchronized float modulesX() {
        return modulesX;
    }

    public synchronized float modulesY() {
        return modulesY;
    }

    public synchronized void setModulesPosition(double x, double y) {
        modulesX = clamp01(x);
        modulesY = clamp01(y);
    }

    public synchronized void resetModulesPosition() {
        modulesX = Float.NaN;
        modulesY = Float.NaN;
    }

    public synchronized boolean modulesVisible() {
        return modulesVisible;
    }

    public synchronized void setModulesVisible(boolean visible) {
        modulesVisible = visible;
    }

    public synchronized boolean hasStatsPosition() {
        return !Float.isNaN(statsX) && !Float.isNaN(statsY);
    }

    public synchronized float statsX() {
        return statsX;
    }

    public synchronized float statsY() {
        return statsY;
    }

    public synchronized void setStatsPosition(double x, double y) {
        statsX = clamp01(x);
        statsY = clamp01(y);
    }

    public synchronized void resetStatsPosition() {
        statsX = Float.NaN;
        statsY = Float.NaN;
    }

    public synchronized boolean statsVisible() {
        return statsVisible;
    }

    public synchronized void setStatsVisible(boolean visible) {
        statsVisible = visible;
    }

    public synchronized void resetAll() {
        resetToastPosition();
        resetModulesPosition();
        resetStatsPosition();
        toastVisible = true;
        modulesVisible = true;
        statsVisible = true;
    }

    public synchronized boolean editorActive() {
        return editorActive;
    }

    public synchronized void setEditorActive(boolean active) {
        editorActive = active;
    }

    private static float clamp01(double value) {
        if (Double.isNaN(value)) {
            return Float.NaN;
        }
        return (float) Math.max(0.0D, Math.min(1.0D, value));
    }
}
