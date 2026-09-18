package dev.aero.client.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the armor pass of a hurt player so EquipmentRenderer draws it with the red hurt overlay. */
@Mixin(value = ArmorFeatureRenderer.class, priority = 2000)
public class DamageTintArmorMixin {
    private static final String RENDER = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V";

    @Inject(method = RENDER, at = @At("HEAD"), require = 0)
    private void aero$begin(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state,
                            float yaw, float pitch, CallbackInfo ci) {
        dev.aero.client.DamageTintState.armorBegin++;
        dev.aero.client.DamageTintState.ARMOR_HURT.set(dev.aero.client.DamageTintState.on(state) && state.hurt);
    }

    @Inject(method = RENDER, at = @At("RETURN"), require = 0)
    private void aero$end(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state,
                          float yaw, float pitch, CallbackInfo ci) {
        dev.aero.client.DamageTintState.ARMOR_HURT.set(false);
    }
}
