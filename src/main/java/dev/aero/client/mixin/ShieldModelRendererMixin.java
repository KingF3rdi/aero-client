package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.client.render.model.ModelBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShieldModelRenderer.class, priority = 2000)
public class ShieldModelRendererMixin {
    /** Spiked/Studded shield cosmetics: a handful of metal cubes on the plate (a few dozen quads, no extra model). */
    @Inject(method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",
            at = @At("RETURN"), require = 0)
    private void aero$spikes(net.minecraft.component.ComponentMap components, net.minecraft.item.ItemDisplayContext ctx,
                             net.minecraft.client.util.math.MatrixStack matrices,
                             net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
                             int light, int overlay, boolean glint, int color, CallbackInfo ci) {
        String style = Visuals.shieldSpikeStyle();
        if (style == null) {
            return;
        }
        System.out.println("[SPIKE] drawing " + style);
        boolean spiked = style.equals("spiked");
        int metal = 0xFFB4BCC8;
        matrices.push();
        matrices.scale(1f, -1f, -1f);
        var layer = RenderLayers.entityCutoutNoCull(dev.aero.client.cosmetic.CubeDraw.WHITE);
        float[][] pts = spiked
                ? new float[][]{{0f, 0f, 0.09f}, {-0.2f, 0.42f, 0.06f}, {0.2f, 0.42f, 0.06f}, {-0.2f, -0.42f, 0.06f}, {0.2f, -0.42f, 0.06f}, {-0.2f, 0f, 0.05f}, {0.2f, 0f, 0.05f}}
                : new float[][]{{-0.22f, 0.45f, 0.02f}, {0.22f, 0.45f, 0.02f}, {-0.22f, -0.45f, 0.02f}, {0.22f, -0.45f, 0.02f}, {0f, 0f, 0.03f}};
        for (float[] p : pts) {
            float len = spiked ? p[2] : 0.03f;
            float half = spiked ? 0.035f : 0.05f;
            matrices.push();
            matrices.translate(p[0], p[1], 0.16f + len * 0.5f);
            queue.submitCustom(matrices, layer, (e, vc) ->
                    dev.aero.client.cosmetic.CubeDraw.cube(e, vc, half, half, len * 0.5f + 0.02f, metal, light));
            matrices.pop();
        }
        matrices.pop();
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"
            ),
            index = 8,
            require = 0
    )
    private int aero$tintShield(int tintedColor) {
        Integer tint = Visuals.shieldModelTint();
        return tint == null ? tintedColor : tint;
    }

    @ModifyArg(
            method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"
            ),
            index = 2,
            require = 0
    )
    private RenderLayer aero$shieldOpacityLayer(RenderLayer original) {
        Integer tint = Visuals.shieldModelTint();
        if (tint == null) {
            return original;
        }
        int a = (tint >>> 24) & 0xFF;
        if (a >= 250) {
            return original;
        }
        try {
            return RenderLayers.entityTranslucent(ModelBaker.SHIELD_BASE.getAtlasId());
        } catch (Throwable ignored) {
            return original;
        }
    }
}
