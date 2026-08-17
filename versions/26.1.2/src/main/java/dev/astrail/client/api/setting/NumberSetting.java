package dev.astrail.client.api.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {
    private final double minimum;
    private final double maximum;
    private final double step;

    public NumberSetting(String id, String displayName, double defaultValue, double minimum, double maximum, double step) {
        super(id, displayName, defaultValue);
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("Invalid number range");
        }
        if (!Double.isFinite(step) || step <= 0.0) {
            throw new IllegalArgumentException("Step must be finite and positive");
        }
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        set(defaultValue);
    }

    @Override
    protected Double normalize(Double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Value must be finite");
        }
        if (minimum == 0.0 && maximum == 0.0 && step == 0.0) {
            return value;
        }
        double clamped = Math.max(minimum, Math.min(maximum, value));
        double quantized = minimum + Math.round((clamped - minimum) / step) * step;
        return Math.max(minimum, Math.min(maximum, quantized));
    }

    public int intValue() {
        return get().intValue();
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }

    public double step() {
        return step;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement value) {
        set(value.getAsDouble());
    }

    @Override
    public void fromString(String value) {
        set(Double.parseDouble(value));
    }
}
