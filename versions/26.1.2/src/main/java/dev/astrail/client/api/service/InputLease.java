package dev.astrail.client.api.service;

public interface InputLease extends AutoCloseable {
    void setPressed(boolean pressed);

    @Override
    void close();
}
