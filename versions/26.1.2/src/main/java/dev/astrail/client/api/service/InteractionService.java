package dev.astrail.client.api.service;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.Entity;

public interface InteractionService {
    boolean useMainHand();

    boolean interact(Entity target);

    boolean interactAtCrosshair();

    boolean attack(Entity target);

    boolean clickContainerSlot(int containerId, int slot, ContainerInput input);

    boolean closeScreen();
}
