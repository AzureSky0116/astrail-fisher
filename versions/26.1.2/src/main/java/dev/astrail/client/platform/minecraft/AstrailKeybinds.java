package dev.astrail.client.platform.minecraft;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Vanilla key bindings for the Astrail Fisher client. */
public final class AstrailKeybinds {
    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("astrail_fisher", "controls"));

    /** Translation key for the GUI opening binding's label. */
    public static final String OPEN_GUI_KEY = "key.astrail_fisher.open_gui";

    private static final KeyMapping OPEN_FISHING_GUI = createOpenFishingGui();

    private AstrailKeybinds() {
    }

    /** Opens the Auto Fishing settings page; rebindable in Options -> Controls. */
    public static KeyMapping openFishingGui() {
        return OPEN_FISHING_GUI;
    }

    /** The key code currently bound to the GUI opening binding. */
    public static int boundGuiKey() {
        return KeyMappingHelper.getBoundKeyOf(OPEN_FISHING_GUI).getValue();
    }

    private static KeyMapping createOpenFishingGui() {
        return KeyMappingHelper.registerKeyMapping(
            new KeyMapping(OPEN_GUI_KEY, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY)
        );
    }
}