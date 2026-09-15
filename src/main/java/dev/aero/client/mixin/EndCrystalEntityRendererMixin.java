package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom End Crystals: wraps the MatrixStack the vanilla renderer already receives, so this only
 * depends on the render(...) parameter types (stable, public API) rather than internal fields.
 */
@Mixin(EndCrystalEntityRenderer.class)
public class EndCrystalEntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void aero$style(Object entity, float yaw, float tickDelta, MatrixStack matrices,
                             Object vertexConsumers, int light, CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.customEndCrystals || matrices == null) {
            return;
        }
        float scale = Math.max(0.1f, c.crystalScale);
        matrices.translate(0, c.crystalHeightOffset, 0);
        matrices.scale(scale, scale, scale);
    }
}
