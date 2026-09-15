package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerHeldItemFeatureRenderer.class, priority = 2000)
public class PlayerHeldItemFeatureRendererMixin {
    @Unique
    private PlayerEntity aero$prevShieldHolder;
    @Unique
    private boolean aero$pushedShieldHolder;

    @Inject(method = "renderItem", at = @At("HEAD"), require = 0)
    private void aero$begin(Object state, Object itemState, ItemStack stack, Object arm,
                            Object matrices, Object queue, int light, CallbackInfo ci) {
        if (!Visuals.isShield(stack) || !(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return;
        }
        Entity entity = mc.world.getEntityById(playerState.id);
        if (entity instanceof PlayerEntity player) {
            aero$prevShieldHolder = Visuals.currentShieldHolder();
            aero$pushedShieldHolder = true;
            Visuals.pushShieldHolder(player);
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"), require = 0)
    private void aero$end(Object state, Object itemState, ItemStack stack, Object arm,
                          Object matrices, Object queue, int light, CallbackInfo ci) {
        if (aero$pushedShieldHolder) {
            aero$pushedShieldHolder = false;
            Visuals.popShieldHolder(aero$prevShieldHolder);
            aero$prevShieldHolder = null;
        }
    }
}
