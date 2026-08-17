package dev.astrail.client.platform.minecraft;

import dev.astrail.client.api.service.InteractionService;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class MinecraftInteractionService implements InteractionService {
    @Override
    public boolean useMainHand() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            return false;
        }
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public boolean interact(Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || target == null
                || target == client.player || !target.isAlive()) {
            return false;
        }
        client.gameMode.interact(
            client.player,
            target,
            new EntityHitResult(target, target.position()),
            InteractionHand.MAIN_HAND
        );
        client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public boolean interactAtCrosshair() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return false;
        if (client.hitResult instanceof EntityHitResult hit
                && hit.getType() == HitResult.Type.ENTITY
                && hit.getEntity() != client.player
                && hit.getEntity().isAlive()) {
            client.gameMode.interact(client.player, hit.getEntity(), hit, InteractionHand.MAIN_HAND);
            client.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return useMainHand();
    }

    @Override
    public boolean attack(Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null || target == null
                || target == client.player || !target.isAlive()) {
            return false;
        }
        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public boolean clickContainerSlot(int containerId, int slot, ContainerInput input) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) {
            return false;
        }
        client.gameMode.handleContainerInput(containerId, slot, 0, input, client.player);
        return true;
    }

    @Override
    public boolean closeScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) {
            return false;
        }
        client.setScreen(null);
        return true;
    }
}
