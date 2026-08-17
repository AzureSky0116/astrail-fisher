package dev.astrail.client.mixin;

import dev.astrail.client.AstrailClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
abstract class MinecraftAttackMixin {
    @Redirect(
        method = "continueAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V"
        )
    )
    private void astrail$preserveBackgroundMining(MultiPlayerGameMode gameMode) {
        if (AstrailClient.shouldPreserveControllerMining()) {
            return;
        }
        gameMode.stopDestroyBlock();
    }
}
