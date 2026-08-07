package dev.astrail.client.ui.state;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UiPreferences {
    private static final int RECENT_LIMIT = 8;
    private final Set<String> favorites = new LinkedHashSet<>();
    private final Deque<String> recent = new ArrayDeque<>();

    public synchronized boolean toggleFavorite(String moduleId) {
        if (!favorites.remove(moduleId)) {
            favorites.add(moduleId);
            return true;
        }
        return false;
    }

    public synchronized boolean isFavorite(String moduleId) {
        return favorites.contains(moduleId);
    }

    public synchronized Set<String> favorites() {
        return Set.copyOf(favorites);
    }

    public synchronized void replaceFavorites(Iterable<String> moduleIds) {
        favorites.clear();
        moduleIds.forEach(favorites::add);
    }

    public synchronized void markRecent(String moduleId) {
        recent.remove(moduleId);
        recent.addFirst(moduleId);
        while (recent.size() > RECENT_LIMIT) recent.removeLast();
    }

    public synchronized List<String> recent() {
        return List.copyOf(recent);
    }

    public synchronized void replaceRecent(Iterable<String> moduleIds) {
        recent.clear();
        moduleIds.forEach(id -> {
            if (!recent.contains(id) && recent.size() < RECENT_LIMIT) recent.addLast(id);
        });
    }
}
