package dev.astrail.client.core.event;

import dev.astrail.client.api.event.ClientEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger("Astrail/EventBus");

    private final Map<Class<? extends ClientEvent>, CopyOnWriteArrayList<Listener<?>>> listeners =
        new ConcurrentHashMap<>();
    private volatile BiConsumer<String, Throwable> failureHandler = (owner, error) -> { };
    private volatile Consumer<String> successHandler = owner -> { };

    public <E extends ClientEvent> Subscription subscribe(Class<E> eventType, String owner, Consumer<E> handler) {
        Listener<E> listener = new Listener<>(owner, handler);
        listeners.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> {
            CopyOnWriteArrayList<Listener<?>> registered = listeners.get(eventType);
            if (registered != null) registered.remove(listener);
        };
    }

    public void onFailure(BiConsumer<String, Throwable> handler) {
        this.failureHandler = handler;
    }

    public void onSuccess(Consumer<String> handler) {
        this.successHandler = handler;
    }

    public <E extends ClientEvent> void post(E event) {
        // getOrDefault would build and discard its default list on every post —
        // the argument is evaluated eagerly — and this runs for every tick event.
        CopyOnWriteArrayList<Listener<?>> registered = listeners.get(event.getClass());
        if (registered == null) return;
        for (Listener<?> rawListener : registered) {
            dispatch(rawListener, event);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends ClientEvent> void dispatch(Listener<?> rawListener, E event) {
        Listener<E> listener = (Listener<E>) rawListener;
        try {
            listener.handler().accept(event);
            successHandler.accept(listener.owner());
        } catch (Throwable error) {
            LOGGER.error("Event handler failed: owner={}, event={}", listener.owner(), event.getClass().getName(), error);
            failureHandler.accept(listener.owner(), error);
        }
    }

    private record Listener<E extends ClientEvent>(String owner, Consumer<E> handler) {
    }
}
