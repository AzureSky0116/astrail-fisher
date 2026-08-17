package dev.astrail.client.api.event;

import net.minecraft.world.item.ItemStack;

/** Immutable main-thread notification for a server-driven container slot update. */
public record ContainerSlotUpdateEvent(
    int containerId,
    int stateId,
    int slot,
    ItemStack item
) implements ClientEvent {
    public ContainerSlotUpdateEvent {
        item = item.copy();
    }
}
