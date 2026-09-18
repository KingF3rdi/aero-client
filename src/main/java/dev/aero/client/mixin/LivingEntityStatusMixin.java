package dev.aero.client.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem pop counting and the shield-break direct hook moved to EntityStatusMixin - handleStatus
 * is declared on Entity itself, not overridden here, so a mixin on LivingEntity.class never had
 * anything to attach to (see that file for the full explanation). */
@Mixin(value = LivingEntity.class, priority = 2000)
public class LivingEntityStatusMixin {
    /** Hand Tweaks "Speed": scales how long the local player's swing animation lasts. */
    @Inject(method = "getHandSwingDuration()I", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$swingSpeed(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        var c = dev.aero.client.AeroClient.CONFIG;
        if (c == null || !c.handTweaks || c.handSpeed <= 0f || Math.abs(c.handSpeed - 1f) < 0.01f) {
            return;
        }
        if ((Object) this == net.minecraft.client.MinecraftClient.getInstance().player) {
            cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValueI() / c.handSpeed)));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void aero$elytra(CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.entity.player.PlayerEntity player) {
            dev.aero.client.Optimizer.tickElytra(player);
        }
    }
}
