package dev.astrail.client.api.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class KeybindSetting extends Setting<Integer> {
    public static final int UNBOUND = GLFW.GLFW_KEY_UNKNOWN;

    public KeybindSetting(int defaultKey) {
        super("keybind", "Keybind", defaultKey);
    }

    @Override
    protected Integer normalize(Integer value) {
        if (value < UNBOUND || value > GLFW.GLFW_KEY_LAST) {
            throw new IllegalArgumentException("Invalid key code: " + value);
        }
        return value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement value) {
        set(value.getAsInt());
    }

    @Override
    public void fromString(String value) {
        set(Integer.parseInt(value));
    }

    @Override
    public String displayValue() {
        if (get() == UNBOUND) {
            return "UNBOUND";
        }
        return InputConstants.Type.KEYSYM.getOrCreate(get()).getDisplayName().getString().toUpperCase();
    }
}
