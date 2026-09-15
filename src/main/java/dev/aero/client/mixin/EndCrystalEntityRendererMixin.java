package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EndCrystalEntityRenderer.class, priority = 2500)
public class EndCrystalEntityRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"),
            require = 0
    )
    private void aero$style(EndCrystalEntityRenderState state, MatrixStack matrices,
                            OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.customEndCrystals || matrices == null) {
            return;
        }
        float scale = Math.max(0.15f, c.crystalScale);
        float bounce = (float) Math.sin((state.age * 0.2f) * Math.max(0.05f, c.crystalBounceSpeed))
                * 0.2f * c.crystalBounceHeight;
        matrices.translate(0, c.crystalHeightOffset + bounce, 0);
        if ("Flat".equalsIgnoreCase(c.crystalMode)) {
            matrices.scale(scale, scale * 0.35f, scale);
        } else {
            matrices.scale(scale, scale, scale);
        }
        float spin = state.age * 8f * Math.max(0f, c.crystalRotationSpeed);
        try {
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(spin));
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"), require = 0)
    private void aero$base(net.minecraft.entity.decoration.EndCrystalEntity entity,
                           EndCrystalEntityRenderState state, float tickDelta, CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c != null && c.customEndCrystals && "No Frame".equalsIgnoreCase(c.crystalMode)) {
            state.baseVisible = false;
        }
    }

    @Inject(method = "getYOffset", at = @At("RETURN"), cancellable = true, require = 0)
    private static void aero$bounce(float tickProgress, CallbackInfoReturnable<Float> cir) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.customEndCrystals || cir.getReturnValue() == null) {
            return;
        }
        float extra = (float) Math.sin(tickProgress * Math.max(0.05f, c.crystalBounceSpeed))
                * 0.15f * c.crystalBounceHeight;
        cir.setReturnValue(cir.getReturnValue() + extra + c.crystalHeightOffset * 0.15f);
    }
}
