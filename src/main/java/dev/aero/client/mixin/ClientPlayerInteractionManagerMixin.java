package dev.aero.client.mixin;

import dev.aero.client.Optimizer;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = {"attackEntity", "attack"}, at = @At("TAIL"), require = 0)
    private void aero$crystal(PlayerEntity player, Entity target, CallbackInfo ci) {
        Optimizer.onAttackEntity(target);
        if (target instanceof PlayerEntity victim && player != null) {
            String held = player.getMainHandStack().getItem().toString().toLowerCase();
            boolean axe = (held.contains("_axe") || held.endsWith(":axe")) && !held.contains("pickaxe");
            if (axe && (victim.isBlocking() || victim.isUsingItem())) {
                dev.aero.client.Visuals.markShieldDisabled(victim);
            }
        }
    }
}
