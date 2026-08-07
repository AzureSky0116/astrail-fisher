package dev.astrail.client.api.event;

import net.minecraft.client.Minecraft;

public record ClientTickEvent(Minecraft client, long worldGeneration) implements ClientEvent {
}
