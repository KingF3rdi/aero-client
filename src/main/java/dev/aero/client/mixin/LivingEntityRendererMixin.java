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
 * Death Animation: temporarily caps deathTime at the "Death time" setting for the duration of the
 * transform setup, so the vanilla death-flop rotation (computed inline from entity.deathTime) never
 * progresses past that point, then restores the real value so normal death timing / entity removal
 * elsewhere is unaffected. The actual fade-out (deathOverlayTime/deathOpacity) reads the real,
 * unrestored deathTime separately - see Visuals.deathFadeAlpha().
 */
@Mixin(value = LivingEntityRenderer.class, priority = 2000)
public class LivingEntityRendererMixin {
    @Unique
    private int aero$savedDeathTime = -1;

    @Inject(method = {"setupTransforms"}, at = @At("HEAD"), require = 0)
    private void aero$freezeDeath(LivingEntity entity, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (Visuals.skipDeathAnimation(entity)) {
            aero$savedDeathTime = entity.deathTime;
            entity.deathTime = Math.min(entity.deathTime, Visuals.deathTimeCap());
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
