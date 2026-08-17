package dev.astrail.client.api.service;

public interface NotificationService {
    void show(String owner, String title, String message, boolean positive);
}
