package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Death Animation: temporarily zeroes deathTime for the duration of the transform setup so the
 * vanilla death-flop rotation (computed inline from entity.deathTime) never progresses, then
 * restores it so normal death timing / entity removal elsewhere is unaffected.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Unique
    private int aero$savedDeathTime = -1;

    @Inject(method = {"setupTransforms"}, at = @At("HEAD"), require = 0)
    private void aero$freezeDeath(LivingEntity entity, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (Visuals.skipDeathAnimation(entity)) {
            aero$savedDeathTime = entity.deathTime;
            entity.deathTime = 0;
        }
    }

    @Inject(method = {"setupTransforms"}, at = @At("RETURN"), require = 0)
    private void aero$restoreDeath(LivingEntity entity, MatrixStack matrices, float animationProgress,
                                    float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (aero$savedDeathTime >= 0) {
            entity.deathTime = aero$savedDeathTime;
            aero$savedDeathTime = -1;
        }
    }
}
