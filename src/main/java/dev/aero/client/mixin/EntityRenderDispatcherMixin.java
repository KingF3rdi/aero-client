package dev.aero.client.mixin;

import dev.aero.client.hud.HudStats;
import dev.aero.client.Visuals;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hide Other Players and the entity-hiding filters: entities that should not draw are culled before rendering. */
@Mixin(value = EntityRenderManager.class, priority = 2000)
public class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$cull(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (HudStats.skipEntity(entity) || Visuals.hideOtherPlayer(entity)) {
            cir.setReturnValue(false);
        }
    }
}
