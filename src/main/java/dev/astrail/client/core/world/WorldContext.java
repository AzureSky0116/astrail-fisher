package dev.astrail.client.core.world;

/**
 * Monotonically increasing world generation used to invalidate cross-world state.
 *
 * <p>This deliberately no longer carries a per-tick snapshot of readiness, focus
 * or the open screen. That snapshot was refreshed at the top of the client tick,
 * before input handling could open a screen, so by the time modules ran it was
 * already stale — every consumer read the live {@code Minecraft} state instead,
 * and the snapshot had no callers. If a snapshot role is ever revived it must be
 * populated after input handling, not before.
 */
public final class WorldContext {
    private long generation;

    public synchronized long advanceGeneration() {
        return ++generation;
    }

    public synchronized long generation() {
        return generation;
    }
}
