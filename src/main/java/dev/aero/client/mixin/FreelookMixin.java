package dev.aero.client.mixin;

import dev.aero.client.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: the mouse turns the freelook camera instead of the player while it's active. */
@Mixin(Entity.class)
public class FreelookMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$freelook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (Freelook.active() && (Object) this == MinecraftClient.getInstance().player) {
            Freelook.look(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
