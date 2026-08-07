package dev.astrail.client.core.module;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ModuleScope implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private boolean closed;

    public synchronized <T extends AutoCloseable> T own(T resource) {
        if (closed) {
            closeImmediately(resource);
            throw new IllegalStateException("Module scope is already closed");
        }
        resources.push(resource);
        return resource;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        RuntimeException combined = null;
        while (!resources.isEmpty()) {
            try {
                resources.pop().close();
            } catch (Exception error) {
                if (combined == null) {
                    combined = new RuntimeException("Failed to close module resources");
                }
                combined.addSuppressed(error);
            }
        }
        if (combined != null) {
            throw combined;
        }
    }

    private static void closeImmediately(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception error) {
            throw new RuntimeException("Failed to close late module resource", error);
        }
    }
}
