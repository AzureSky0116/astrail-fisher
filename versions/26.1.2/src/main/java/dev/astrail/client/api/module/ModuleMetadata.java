package dev.astrail.client.api.module;

import java.util.Objects;

/**
 * Immutable identity and lifecycle policy of a module.
 *
 * <p>The two trailing booleans are adjacent and same-typed, so swapping them at a
 * call site compiles cleanly while silently changing both world-change teardown
 * and enable persistence — name them at construction.
 *
 * @param disableOnWorldChange whether the module is force-disabled when the
 *     client changes worlds or servers
 * @param persistEnabled whether an enabled state is saved to the config and
 *     restored on the next launch
 */
public record ModuleMetadata(
    String id,
    String displayName,
    String description,
    ModuleCategory category,
    boolean disableOnWorldChange,
    boolean persistEnabled
) {
    public ModuleMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(category, "category");
        if (!id.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Module id must be stable and lowercase: " + id);
        }
    }
}
