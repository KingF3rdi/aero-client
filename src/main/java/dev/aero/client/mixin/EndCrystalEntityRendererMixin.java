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
        // Vanilla's own render() does matrices.push()/pop() around its scale+submitModel, so without
        // pushing here first, our transform never gets popped and leaks into whatever runs after this
        // method returns (the super.render() call at the end, i.e. the nametag/label pass).
        matrices.push();
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

    /** End crystal glow: raises the model's light toward full brightness by the Glow strength. */
    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"),
            index = 4, require = 0)
    private int aero$glowLight(int light) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.renders || !c.rendersEndCrystalsGlow) {
            return light;
        }
        int lvl = Math.round(15f * Math.max(0f, Math.min(100f, c.crystalGlowStrength)) / 100f);
        int block = Math.max(light >> 4 & 15, lvl);
        int sky = Math.max(light >> 20 & 15, lvl);
        return (sky << 20) | (block << 4);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("RETURN"),
            require = 0
    )
    private void aero$unstyle(EndCrystalEntityRenderState state, MatrixStack matrices,
                              OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c != null && c.renders && c.rendersEndCrystalsGlow && matrices != null) {
            try {
                // Glowing core inside the glass, sized by the Glow strength.
                float s = Math.max(0f, Math.min(100f, c.crystalGlowStrength)) / 100f;
                float half = 0.06f + 0.16f * s;
                int col = c.crystalGlowColor | 0xFF000000;
                float bob = (float) Math.sin(state.age * 0.1f) * 0.1f;
                matrices.push();
                matrices.translate(0, 1.0f + bob, 0);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(state.age * 3f));
                queue.submitCustom(matrices, net.minecraft.client.render.RenderLayers.entityCutoutNoCull(dev.aero.client.cosmetic.CubeDraw.WHITE),
                        (e, vc) -> dev.aero.client.cosmetic.CubeDraw.cube(e, vc, half, half, half, col, dev.aero.client.cosmetic.CubeDraw.FULLBRIGHT));
                matrices.pop();
            } catch (Throwable ignored) {
            }
        }
        if (c == null || !c.customEndCrystals || matrices == null) {
            return;
        }
        try {
            matrices.pop();
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
