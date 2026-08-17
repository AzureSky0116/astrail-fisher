package dev.astrail.client.api.setting;

import com.google.gson.JsonElement;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String id;
    private final String displayName;
    private final T defaultValue;
    private T value;
    private String group = "General";
    private String description = "";
    private BooleanSupplier visibleWhen = () -> true;

    protected Setting(String id, String displayName, T defaultValue) {
        if (!id.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Setting id must be stable and lowercase: " + id);
        }
        this.id = id;
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.value = normalize(defaultValue);
    }

    public final String id() {
        return id;
    }

    public final String displayName() {
        return displayName;
    }

    public final T get() {
        return value;
    }

    public final T defaultValue() {
        return defaultValue;
    }

    public final String group() {
        return group;
    }

    public final String description() {
        return description;
    }

    public final Setting<T> presentation(String group, String description) {
        this.group = requirePresentationText(group, "group");
        this.description = Objects.requireNonNull(description, "description").trim();
        return this;
    }

    /** Controls presentation only; hidden settings remain persisted in profiles. */
    public final Setting<T> visibleWhen(BooleanSupplier condition) {
        visibleWhen = Objects.requireNonNull(condition, "condition");
        return this;
    }

    public final boolean isVisible() {
        return visibleWhen.getAsBoolean();
    }

    public final void set(T value) {
        this.value = normalize(Objects.requireNonNull(value, "value"));
    }

    public final void reset() {
        set(defaultValue);
    }

    protected T normalize(T value) {
        return value;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement value);

    public abstract void fromString(String value);

    public String displayValue() {
        return String.valueOf(value);
    }

    private static String requirePresentationText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
