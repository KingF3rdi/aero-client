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
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void aero$elytra(CallbackInfo ci) {
        if ((Object) this instanceof net.minecraft.entity.player.PlayerEntity player) {
            dev.aero.client.Optimizer.tickElytra(player);
        }
    }
}
