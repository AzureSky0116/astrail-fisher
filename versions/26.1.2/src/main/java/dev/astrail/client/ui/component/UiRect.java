package dev.astrail.client.ui.component;

public record UiRect(int x, int y, int width, int height) {
    public static final UiRect EMPTY = new UiRect(0, 0, 0, 0);

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double px, double py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    public UiRect inset(int amount) {
        return new UiRect(x + amount, y + amount, Math.max(0, width - amount * 2), Math.max(0, height - amount * 2));
    }

    public UiRect expand(int amount) {
        return new UiRect(x - amount, y - amount, width + amount * 2, height + amount * 2);
    }

    public UiRect translate(int dx, int dy) {
        return new UiRect(x + dx, y + dy, width, height);
    }
}
