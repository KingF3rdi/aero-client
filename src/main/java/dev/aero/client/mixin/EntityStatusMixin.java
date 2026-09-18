package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * handleStatus(byte) is declared on Entity itself and never overridden anywhere down the hierarchy
 * (verified against the real Yarn 1.21.11 mappings.tiny - LivingEntity's and PlayerEntity's own
 * class bodies contain no such method), so the previous LivingEntityStatusMixin targeting
 * LivingEntity.class had nothing to attach to and silently never fired (require = 0 hid it, same
 * class of bug as the earlier Shield Tweaks render fix) - totem pops were never counted and the
 * shield-break direct hook never ran, leaving only the indirect sound-based heuristic for that one.
 */
@Mixin(value = Entity.class, priority = 2000)
public class EntityStatusMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"), require = 0)
    private void aero$status(byte status, CallbackInfo ci) {
        Object self = this;
        if (self instanceof PlayerEntity player
                && (status == EntityStatuses.BREAK_OFFHAND || status == EntityStatuses.BREAK_MAINHAND)) {
            Visuals.markShieldDisabled(player);
        }
    }
}
