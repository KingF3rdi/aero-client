package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.render.entity.feature.ArmorFeatureRenderer")
public class ArmorFeatureRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideEntity(net.minecraft.client.util.math.MatrixStack matrices, Object vertices, int light,
                                 LivingEntity entity, float limbAngle, float limbDistance, float tickDelta,
                                 float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (Visuals.hideArmor(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideAny(net.minecraft.client.util.math.MatrixStack matrices, Object vertices, int light,
                              Entity entity, CallbackInfo ci) {
        if (Visuals.hideArmor(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideSlotEntity(Object matrices, Object vertices, Object entity, Object slot, CallbackInfo ci) {
        if (entity instanceof Entity e && Visuals.hideArmorSlot(e, slot)) {
            ci.cancel();
        }
    }
}
