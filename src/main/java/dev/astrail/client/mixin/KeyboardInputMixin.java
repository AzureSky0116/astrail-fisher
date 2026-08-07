package dev.astrail.client.mixin;

import dev.astrail.client.AstrailClient;
import dev.astrail.client.api.service.InputAction;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void astrail$mergeSyntheticInput(CallbackInfo callback) {
        boolean forward = keyPresses.forward();
        boolean backward = keyPresses.backward();
        boolean left = keyPresses.left();
        boolean right = keyPresses.right();
        if (!forward && !backward) {
            forward = AstrailClient.isSyntheticInputPressed(InputAction.FORWARD);
            backward = AstrailClient.isSyntheticInputPressed(InputAction.BACKWARD);
        }
        if (!left && !right) {
            left = AstrailClient.isSyntheticInputPressed(InputAction.LEFT);
            right = AstrailClient.isSyntheticInputPressed(InputAction.RIGHT);
        }
        boolean shift = keyPresses.shift() || AstrailClient.isSyntheticInputPressed(InputAction.SNEAK);
        boolean jump = keyPresses.jump() || AstrailClient.isSyntheticInputPressed(InputAction.JUMP);
        boolean sprint = keyPresses.sprint() || AstrailClient.isSyntheticInputPressed(InputAction.SPRINT);

        keyPresses = new Input(forward, backward, left, right, jump, shift, sprint);
        moveVector = new Vec2(impulse(left, right), impulse(forward, backward)).normalized();
    }

    private static float impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
