package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Death Animation: the death-flop rotation reads deathTime from the render state, so capping it at
 * the "Death time" setting right after the state is built stops the flop at that point. The fade-out
 * reads the entity's real deathTime separately (Visuals.deathFadeAlpha()).
 */
@Mixin(value = LivingEntityRenderer.class, priority = 2000)
public class LivingEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void aero$freezeDeath(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state != null && Visuals.skipDeathAnimation(entity)) {
            state.deathTime = Math.min(state.deathTime, (float) Visuals.deathTimeCap());
        }
    }
}
