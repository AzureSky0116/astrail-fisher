package dev.astrail.client.core.event;

@FunctionalInterface
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
