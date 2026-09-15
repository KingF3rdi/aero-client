package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem Counter "pops next to name": every client sees EntityStatuses.USE_TOTEM_OF_UNDYING for
 * any entity that pops a totem, so this works for other players too, not just yourself. */
@Mixin(LivingEntity.class)
public class LivingEntityStatusMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void aero$totemPop(byte status, CallbackInfo ci) {
        if (status == EntityStatuses.USE_TOTEM_OF_UNDYING) {
            Visuals.onTotemPop((LivingEntity) (Object) this);
        }
    }
}
