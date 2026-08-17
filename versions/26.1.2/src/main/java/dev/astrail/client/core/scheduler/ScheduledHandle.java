package dev.astrail.client.core.scheduler;

public interface ScheduledHandle extends AutoCloseable {
    boolean active();

    @Override
    void close();
}
