package dev.astrail.client.api.event;

public record WorldChangedEvent(long worldGeneration) implements ClientEvent {
}
