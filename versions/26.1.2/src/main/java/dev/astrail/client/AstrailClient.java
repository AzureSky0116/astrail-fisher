package dev.astrail.client;

import dev.astrail.client.core.ClientRuntime;
import dev.astrail.client.api.service.InputAction;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;

public final class AstrailClient implements ClientModInitializer {
    private static ClientRuntime runtime;

    @Override
    public void onInitializeClient() {
        runtime = new ClientRuntime();
        runtime.initialize();
    }

    public static ClientRuntime runtime() {
        if (runtime == null) {
            throw new IllegalStateException("Astrail is not initialized");
        }
        return runtime;
    }

    public static boolean shouldKeepWorldRunning() {
        return runtime != null && runtime.shouldKeepWorldRunning();
    }

    public static boolean shouldPreserveControllerMining() {
        return runtime != null && runtime.shouldPreserveControllerMining();
    }

    public static boolean isSyntheticInputPressed(InputAction action) {
        return runtime != null && runtime.inputs().isPressed(action);
    }

    public static void onContainerSlotUpdate(ClientboundContainerSetSlotPacket packet) {
        if (runtime != null) runtime.handleContainerSlotUpdate(packet);
    }
}
