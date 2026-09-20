package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Non-player entities only (players go through PlayerEntityRendererMixin, which overrides these
 * methods): nametag scale, and No Shadows by emptying the shadow pieces of the render state.
 */
@Mixin(value = EntityRenderer.class, priority = 2000)
public class EntityRendererMixin {
    @Inject(method = "updateShadow(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;)V",
            at = @At("TAIL"), require = 0)
    private void aero$noShadow(Entity entity, EntityRenderState state, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noShadows && state != null) {
            state.shadowPieces.clear();
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), require = 0)
    private void aero$scale(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                             CameraRenderState cameraState, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null && !(state instanceof PlayerEntityRenderState)) {
            float s = Math.max(0.5F, AeroClient.CONFIG.nametagScale);
            matrices.push();
            matrices.scale(s, s, s);
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("RETURN"), require = 0)
    private void aero$unscale(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                               CameraRenderState cameraState, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null && !(state instanceof PlayerEntityRenderState)) {
            try {
                matrices.pop();
            } catch (Throwable ignored) {
            }
        }
    }
}
