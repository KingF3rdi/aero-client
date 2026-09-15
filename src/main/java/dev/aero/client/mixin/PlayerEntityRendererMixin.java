package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerEntityRenderer overrides updateRenderState/renderLabelIfPresent/hasLabel from the base
 * EntityRenderer, so a mixin on the base class (see EntityRendererMixin) never actually runs for
 * players - virtual dispatch always calls this override instead. Every player-nametag feature
 * (ping, TierTagger, totem pop count, own-nametag, scale) has to hook here instead.
 */
@Mixin(value = PlayerEntityRenderer.class, priority = 2000)
public class PlayerEntityRendererMixin {
    @Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$ownName(PlayerLikeEntity entity, double dist, CallbackInfoReturnable<Boolean> cir) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.nametags || !AeroClient.CONFIG.ownNametag) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entity == mc.player) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"), require = 0)
    private void aero$label(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state == null || !(entity instanceof PlayerEntity player)) {
            return;
        }
        Text result = state.playerName != null ? state.playerName : state.displayName;
        if (result == null) {
            return;
        }
        boolean changed = false;

        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && AeroClient.CONFIG.nametagPing) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getNetworkHandler() != null) {
                try {
                    var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                    if (entry != null) {
                        String ping = entry.getLatency() + "ms";
                        String div = AeroClient.CONFIG.pingDivider ? " | " : " ";
                        result = "Left".equalsIgnoreCase(AeroClient.CONFIG.pingSide)
                                ? Text.literal("§7" + ping + "§r" + div).append(result)
                                : result.copy().append(Text.literal(div + "§7" + ping));
                        changed = true;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        if (AeroClient.CONFIG != null && AeroClient.CONFIG.tierTagger) {
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean self = mc.player != null && player == mc.player;
            if (!self || AeroClient.CONFIG.tierShowOwn) {
                try {
                    String tier = dev.aero.client.Visuals.tierFor(player.getUuid(), AeroClient.CONFIG.tierGamemode);
                    if (tier != null && !tier.isBlank()) {
                        String tag = " §7[§b" + tier + "§7]";
                        result = "Right".equalsIgnoreCase(AeroClient.CONFIG.tierSide)
                                ? result.copy().append(Text.literal(tag))
                                : Text.literal(tag.stripLeading() + " §r").append(result);
                        changed = true;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametagBadge) {
            result = Text.literal("✦ ").append(result);
            changed = true;
        }

        if (AeroClient.CONFIG != null && AeroClient.CONFIG.totemPopsOnNametag
                && (AeroClient.CONFIG.totemCounter || AeroClient.CONFIG.nametagBadge)) {
            try {
                int pops = dev.aero.client.Visuals.totemPopsFor(player.getUuid());
                if (pops > 0) {
                    result = result.copy().append(Text.literal(" §7(§d" + pops + " pop" + (pops == 1 ? "" : "s") + "§7)"));
                    changed = true;
                }
            } catch (Throwable ignored) {
            }
        }

        if (changed) {
            state.playerName = result;
            state.displayName = result;
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), require = 0)
    private void aero$scale(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                             CameraRenderState cameraState, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null) {
            float s = Math.max(0.5F, AeroClient.CONFIG.nametagScale);
            matrices.push();
            matrices.scale(s, s, s);
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("RETURN"), require = 0)
    private void aero$unscale(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                               CameraRenderState cameraState, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null) {
            try {
                matrices.pop();
            } catch (Throwable ignored) {
            }
        }
    }
}
