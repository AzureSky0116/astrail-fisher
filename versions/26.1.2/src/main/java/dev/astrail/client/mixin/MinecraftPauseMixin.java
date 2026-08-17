package dev.astrail.client.mixin;

import dev.astrail.client.AstrailClient;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.1.2 has no {@code Gui.isPausing()} (introduced in 26.2): the pause
 * decision lives on the open screen/overlay, whose {@code isPauseScreen()}
 * the client tick loop consults to freeze the world. Forcing it to false
 * while an enabled background module wants the world to keep running
 * reproduces the 26.2 behavior.
 */
@Mixin(value = {Screen.class, Overlay.class})
abstract class MinecraftPauseMixin {
    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void astrail$keepAutomationRunning(CallbackInfoReturnable<Boolean> result) {
        if (AstrailClient.shouldKeepWorldRunning()) {
            result.setReturnValue(false);
        }
    }
}
