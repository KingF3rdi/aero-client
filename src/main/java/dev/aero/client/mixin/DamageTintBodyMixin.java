package dev.aero.client.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Damage Tint: in 1.21.11 only the body gets the red hurt flash; with the module on, the skin no longer flashes. */
@Mixin(value = LivingEntityRenderer.class, priority = 2000)
public class DamageTintBodyMixin {
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getOverlay(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)I"),
            require = 0)
    private int aero$noSkinFlash(LivingEntityRenderState state, float whiteProgress) {
        if (dev.aero.client.DamageTintState.on(state)) {
            return OverlayTexture.packUv(OverlayTexture.getU(whiteProgress), OverlayTexture.getV(state.deathTime > 0f));
        }
        return LivingEntityRenderer.getOverlay(state, whiteProgress);
    }
}
