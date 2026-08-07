package dev.astrail.client.platform.minecraft;

import dev.astrail.client.api.service.RotationLease;
import dev.astrail.client.api.service.RotationService;
import dev.astrail.client.core.diagnostic.DiagnosticService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class MinecraftRotationService implements RotationService {
    private final List<Lease> leases = new ArrayList<>();
    private final DiagnosticService diagnostics;

    public MinecraftRotationService() {
        this(null);
    }

    public MinecraftRotationService(DiagnosticService diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public synchronized RotationLease acquire(String owner, int priority) {
        Lease lease = new Lease(owner, priority);
        leases.add(lease);
        return lease;
    }

    @Override
    public synchronized void tick() {
        Lease selected = leases.stream()
            .filter(lease -> !lease.closed && lease.target != null)
            .max(Comparator.comparingInt(lease -> lease.priority))
            .orElse(null);
        if (selected != null) {
            if (diagnostics != null) diagnostics.setCapabilityOwner("rotation", selected.owner);
            apply(selected);
        } else {
            if (diagnostics != null) diagnostics.setCapabilityOwner("rotation", null);
            for (Lease lease : leases) {
                lease.yawVelocity *= 0.45F;
                lease.pitchVelocity *= 0.45F;
            }
        }
        for (Lease lease : leases) {
            lease.target = null;
        }
    }

    @Override
    public synchronized void clear() {
        for (Lease lease : leases) {
            lease.closed = true;
            lease.target = null;
        }
        leases.clear();
        if (diagnostics != null) diagnostics.setCapabilityOwner("rotation", null);
    }

    private static void apply(Lease lease) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        Vec3 eyes = client.player.getEyePosition();
        double dx = lease.target.x - eyes.x;
        double dy = lease.target.y - eyes.y;
        double dz = lease.target.z - eyes.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        float yawError = Mth.wrapDegrees(targetYaw - client.player.getYRot());
        float pitchError = Mth.wrapDegrees(targetPitch - client.player.getXRot());

        float response = Mth.clamp(1.0F - lease.smoothing, 0.08F, 0.85F);
        float trackingGain = 0.18F + response * 0.45F;
        float acceleration = 0.22F + response * 0.38F;
        float desiredYawVelocity = Mth.clamp(yawError * trackingGain, -lease.maxDegreesPerTick, lease.maxDegreesPerTick);
        float pitchLimit = lease.maxDegreesPerTick * 0.8F;
        float desiredPitchVelocity = Mth.clamp(pitchError * trackingGain, -pitchLimit, pitchLimit);

        lease.yawVelocity = brakeReversal(lease.yawVelocity, yawError);
        lease.pitchVelocity = brakeReversal(lease.pitchVelocity, pitchError);
        lease.yawVelocity = Mth.lerp(acceleration, lease.yawVelocity, desiredYawVelocity);
        lease.pitchVelocity = Mth.lerp(acceleration, lease.pitchVelocity, desiredPitchVelocity);
        float yawStep = clampStepToError(lease.yawVelocity, yawError);
        float pitchStep = clampStepToError(lease.pitchVelocity, pitchError);
        if (Math.abs(yawError) < 0.025F) {
            yawStep = yawError;
            lease.yawVelocity = 0.0F;
        }
        if (Math.abs(pitchError) < 0.025F) {
            pitchStep = pitchError;
            lease.pitchVelocity = 0.0F;
        }

        client.player.setYRot(client.player.getYRot() + yawStep);
        client.player.setXRot(Mth.clamp(client.player.getXRot() + pitchStep, -90.0F, 90.0F));
    }

    private static float clampStepToError(float step, float error) {
        if (Math.signum(step) == Math.signum(error) && Math.abs(step) > Math.abs(error)) {
            return error;
        }
        return step;
    }

    private static float brakeReversal(float velocity, float error) {
        if (velocity != 0.0F && error != 0.0F && Math.signum(velocity) != Math.signum(error)) {
            return velocity * 0.28F;
        }
        return velocity;
    }

    private final class Lease implements RotationLease {
        private final String owner;
        private final int priority;
        private Vec3 target;
        private float maxDegreesPerTick = 18.0F;
        private float smoothing = 0.72F;
        private float yawVelocity;
        private float pitchVelocity;
        private boolean closed;

        private Lease(String owner, int priority) {
            this.owner = owner;
            this.priority = priority;
        }

        @Override
        public void request(Vec3 target, float maxDegreesPerTick, float smoothing) {
            synchronized (MinecraftRotationService.this) {
                if (!closed) {
                    this.target = target;
                    this.maxDegreesPerTick = Mth.clamp(maxDegreesPerTick, 0.5F, 180.0F);
                    this.smoothing = Mth.clamp(smoothing, 0.0F, 0.98F);
                }
            }
        }

        @Override
        public void clear() {
            synchronized (MinecraftRotationService.this) {
                target = null;
                yawVelocity = 0.0F;
                pitchVelocity = 0.0F;
            }
        }

        @Override
        public void close() {
            synchronized (MinecraftRotationService.this) {
                if (closed) {
                    return;
                }
                closed = true;
                target = null;
                yawVelocity = 0.0F;
                pitchVelocity = 0.0F;
                leases.remove(this);
            }
        }

        @Override
        public String toString() {
            return owner + ":" + priority;
        }
    }
}
