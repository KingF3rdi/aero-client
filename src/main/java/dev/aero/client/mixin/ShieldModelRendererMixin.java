package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.client.render.model.ModelBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ShieldModelRenderer.class, priority = 2000)
public class ShieldModelRendererMixin {
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
