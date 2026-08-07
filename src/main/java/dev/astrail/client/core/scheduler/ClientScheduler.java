package dev.astrail.client.core.scheduler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.BiConsumer;

/** Main-thread tick scheduler with owner and world-generation cancellation. */
public final class ClientScheduler {
    private final LongSupplier generation;
    private final List<Task> tasks = new ArrayList<>();

    /**
     * The batch currently being dispatched by {@link #tick()}.
     *
     * <p>Due one-shots leave {@code tasks} when they are snapshotted, before they
     * run. Cancellation sweeps must still be able to reach them — a module that
     * disables itself mid-batch expects its remaining tasks in that same batch to
     * be dead — so owner-wide cancellation walks this list too.
     */
    private List<Task> dispatching = List.of();

    private long tick;
    private BiConsumer<String, Throwable> failureHandler = (owner, error) -> { };

    public ClientScheduler(LongSupplier generation) {
        this.generation = Objects.requireNonNull(generation, "generation");
    }

    public synchronized ScheduledHandle nextTick(String owner, Runnable action) {
        return delayTicks(owner, 1, action);
    }

    public synchronized ScheduledHandle delayTicks(String owner, long delayTicks, Runnable action) {
        return schedule(owner, Math.max(1L, delayTicks), 0L, action);
    }

    public synchronized ScheduledHandle delay(String owner, Duration delay, Runnable action) {
        long ticks = Math.max(1L, (delay.toMillis() + 49L) / 50L);
        return delayTicks(owner, ticks, action);
    }

    public synchronized ScheduledHandle repeatTicks(String owner, long intervalTicks, Runnable action) {
        long interval = Math.max(1L, intervalTicks);
        return schedule(owner, interval, interval, action);
    }

    public void tick() {
        List<Task> due = new ArrayList<>();
        synchronized (this) {
            tick++;
            long currentGeneration = generation.getAsLong();
            Iterator<Task> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.closed || task.generation != currentGeneration) {
                    task.closed = true;
                    iterator.remove();
                    continue;
                }
                if (task.dueTick > tick) {
                    continue;
                }
                due.add(task);
                if (task.interval > 0L) {
                    task.dueTick = tick + task.interval;
                } else {
                    task.closed = true;
                    iterator.remove();
                }
            }
            dispatching = due;
        }
        try {
            for (Task task : due) {
                // Cancellation is re-checked per task because an earlier task in
                // this same batch may have cancelled a later one (a module
                // disabling itself, cancelOwner from a failure handler). `closed`
                // cannot serve here: it is already true for every due one-shot.
                synchronized (this) {
                    if (task.cancelled) continue;
                }
                try {
                    task.action.run();
                } catch (Throwable error) {
                    failureHandler.accept(task.owner, error);
                }
            }
        } finally {
            synchronized (this) {
                dispatching = List.of();
            }
        }
    }

    public synchronized void onFailure(BiConsumer<String, Throwable> handler) {
        failureHandler = Objects.requireNonNull(handler, "handler");
    }

    public synchronized void cancelOwner(String owner) {
        tasks.removeIf(task -> {
            boolean match = task.owner.equals(owner);
            if (match) {
                task.closed = true;
                task.cancelled = true;
            }
            return match;
        });
        for (Task task : dispatching) {
            if (task.owner.equals(owner)) task.cancelled = true;
        }
    }

    public synchronized void clear() {
        tasks.forEach(task -> {
            task.closed = true;
            task.cancelled = true;
        });
        tasks.clear();
        dispatching.forEach(task -> task.cancelled = true);
    }

    public synchronized int activeTaskCount() {
        return tasks.size();
    }

    private ScheduledHandle schedule(String owner, long delayTicks, long interval, Runnable action) {
        Task task = new Task(owner, generation.getAsLong(), tick + delayTicks, interval, action);
        tasks.add(task);
        return task;
    }

    private final class Task implements ScheduledHandle {
        private final String owner;
        private final long generation;
        private long dueTick;
        private final long interval;
        private final Runnable action;
        private boolean closed;

        /**
         * True only for explicit cancellation. Distinct from {@code closed}, which
         * also marks a due one-shot as consumed before it has actually run.
         */
        private boolean cancelled;

        private Task(String owner, long generation, long dueTick, long interval, Runnable action) {
            this.owner = Objects.requireNonNull(owner, "owner");
            this.generation = generation;
            this.dueTick = dueTick;
            this.interval = interval;
            this.action = Objects.requireNonNull(action, "action");
        }

        @Override
        public boolean active() {
            synchronized (ClientScheduler.this) {
                return !closed;
            }
        }

        @Override
        public void close() {
            synchronized (ClientScheduler.this) {
                closed = true;
                cancelled = true;
                tasks.remove(this);
            }
        }
    }
}
