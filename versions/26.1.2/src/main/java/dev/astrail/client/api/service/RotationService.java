package dev.astrail.client.api.service;

public interface RotationService {
    RotationLease acquire(String owner, int priority);

    void tick();

    void clear();
}
