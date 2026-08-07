package dev.astrail.client.ui.component;

/**
 * A repeating vertical color ramp used to fill text.
 *
 * <p>Colors are sampled in the caller's own coordinate space, so a multi-line
 * list can share one ramp instead of restarting it per line. The ramp repeats
 * every {@code cycleLength} units and is traversed by {@code phase}, which lets
 * the palette travel through the text over time.
 *
 * <p>The palette is treated as a closed loop: the segment from the last stop
 * back to the first is interpolated like any other. That removes the constraint
 * a two-color ramp has, where the ends must match to avoid a seam and the fill
 * is therefore limited to one hue fading in and out. Here any number of stops
 * can be traversed as long as the list closes.
 */
public final class UiTextGradient {
    private final int[] stops;
    private final float originY;
    private final float cycleLength;
    private final float phase;

    /**
     * @param stops       at least two ARGB colors, traversed top to bottom and wrapping
     * @param originY     coordinate the ramp is measured from
     * @param cycleLength distance covered by one full palette pass
     * @param phase       ramp offset in cycles; increasing it moves colors downward
     */
    public UiTextGradient(int[] stops, float originY, float cycleLength, float phase) {
        if (stops == null || stops.length < 2) {
            throw new IllegalArgumentException("A gradient needs at least two stops");
        }
        this.stops = stops.clone();
        this.originY = originY;
        this.cycleLength = cycleLength;
        this.phase = phase;
    }

    /** Resolves the fill color at {@code y}. */
    public int colorAt(float y) {
        float span = Math.max(0.001F, cycleLength);
        float position = (y - originY) / span - phase;
        float wrapped = position - (float) Math.floor(position);
        float scaled = wrapped * stops.length;
        int index = (int) scaled;
        if (index >= stops.length) index = stops.length - 1;
        int next = (index + 1) % stops.length;
        return UiDraw.lerpColor(stops[index], stops[next], scaled - index);
    }

    public int stopCount() {
        return stops.length;
    }
}
