package dev.aero.client.mixin;

import dev.aero.client.AlphaBuffers;
import dev.aero.client.Visuals;
import dev.aero.client.hud.HudStats;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderManager.class, priority = 2000)
public class EntityRenderDispatcherMixin {
    @Unique
    private static final ThreadLocal<Entity> AERO$RENDERING = new ThreadLocal<>();

    // Transparent Players used to also skip shouldRender() once alpha dropped near 0, meant as a
    // cheap optimization - but that turns "barely visible" into "fully culled", so dragging the
    // opacity slider low made every player disappear outright instead of fading. AlphaBuffers
    // already handles the actual fade at render time, so there's no need to cull here at all.
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$cull(Entity entity, Frustum frustum, double x, double y, double z,
                           CallbackInfoReturnable<Boolean> cir) {
        if (HudStats.skipEntity(entity) || Visuals.hideOtherPlayer(entity)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, require = 0)
    private Entity aero$ent(Entity entity) {
        AERO$RENDERING.set(entity);
        return entity;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, require = 0)
    private VertexConsumerProvider aero$alpha(VertexConsumerProvider vertices) {
        return AlphaBuffers.wrap(AERO$RENDERING.get(), vertices);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void aero$clear(CallbackInfo ci) {
        AERO$RENDERING.remove();
    }

    @Inject(method = {"renderShadow", "renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/entity/Entity;FFLnet/minecraft/world/WorldView;F)V"},
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void aero$noShadow(CallbackInfo ci) {
        if (dev.aero.client.AeroClient.CONFIG != null && dev.aero.client.AeroClient.CONFIG.noShadows) {
            ci.cancel();
        }
    }
}
