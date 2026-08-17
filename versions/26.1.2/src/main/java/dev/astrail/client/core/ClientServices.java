package dev.astrail.client.core;

import dev.astrail.client.api.service.InputService;
import dev.astrail.client.api.service.InteractionService;
import dev.astrail.client.api.service.RotationService;
import dev.astrail.client.core.event.EventBus;

public record ClientServices(
    EventBus events,
    InputService inputs,
    RotationService rotations,
    InteractionService interactions
) {
}
