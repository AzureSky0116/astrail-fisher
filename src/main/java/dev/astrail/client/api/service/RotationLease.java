package dev.astrail.client.api.service;

import net.minecraft.world.phys.Vec3;

/**
 * Ownership handle for smooth client-side rotation.
 *
 * <p>Leases are owned by the module's {@code ModuleScope} and closed
 * automatically on disable; every call on a closed lease is a no-op. Only the
 * highest-priority lease with a pending target actually steers the camera each
 * tick.
 */
public interface RotationLease extends AutoCloseable {
    /**
     * Requests with the defaults the Auto Mining sliders start from
     * ({@code rotation_speed} 18, {@code rotation_smoothing} 0.72).
     */
    default void request(Vec3 target) {
        request(target, 18.0F, 0.72F);
    }

    /**
     * @param maxDegreesPerTick turn-rate cap in degrees per client tick; pitch is
     *     additionally limited to 80% of this
     * @param smoothing damping factor in 0..1 — higher values approach the target
     *     more gradually
     */
    void request(Vec3 target, float maxDegreesPerTick, float smoothing);

    void clear();

    @Override
    void close();
}
