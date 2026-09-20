package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.Icons;
import dev.aero.client.Visuals;
import dev.aero.client.social.ClientUsers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
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
 * (badge, ping, TierTagger, totem pop count, own-nametag, scale/position) has to hook here instead.
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
        // Only displayName is drawn as the nametag; playerName is a separate second label, so
        // writing our edits to both made the name show up twice.
        Text result = state.displayName;
        if (result == null) {
            return;
        }
        var cfg = AeroClient.CONFIG;
        if (cfg == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean changed = false;

        if (ClientUsers.showBadge(player.getUuid(), player.getName().getString(), "nametag")) {
            result = Text.empty().append(ClientUsers.badge(player.getUuid())).append(result);
            changed = true;
        }

        if (cfg.nametagPing && mc.getNetworkHandler() != null) {
            try {
                var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null) {
                    int ms = entry.getLatency();
                    MutableText ping = Text.literal(ms + "ms").setStyle(Style.EMPTY.withColor(Visuals.pingColor(ms) & 0xFFFFFF));
                    String div = cfg.pingDivider ? " | " : " ";
                    result = "Left".equalsIgnoreCase(cfg.pingSide)
                            ? Text.empty().append(ping).append(Text.literal(div)).append(result)
                            : result.copy().append(Text.literal(div)).append(ping);
                    changed = true;
                }
            } catch (Throwable ignored) {
            }
        }

        if (cfg.tierTagger) {
            boolean self = mc.player != null && player == mc.player;
            if (!self || cfg.tierShowOwn) {
                try {
                    String tier = Visuals.tierFor(player.getUuid(), cfg.tierGamemode);
                    if (tier != null && !tier.isBlank()) {
                        MutableText tag = Text.empty();
                        if (cfg.tierGamemodeIcon) {
                            tag.append(Icons.mode(cfg.tierGamemode)).append(Text.literal(" "));
                        }
                        tag.append(Text.literal(tier).setStyle(Style.EMPTY.withColor(Icons.tierColor(tier)).withBold(true)));
                        result = "Right".equalsIgnoreCase(cfg.tierSide)
                                ? result.copy().append(Text.literal(" ")).append(tag)
                                : Text.empty().append(tag).append(Text.literal(" ")).append(result);
                        changed = true;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        // No way for a pure client mod to tell who else runs this client except the public list, so
        // this star only marks players already on your own friends list.
        if (cfg.nametagBadge && dev.aero.client.social.FriendStore.isFriend(player.getName().getString())) {
            result = Text.literal("★ ").setStyle(Style.EMPTY.withColor(0xFFC94D)).append(result);
            changed = true;
        }

        if (cfg.totemPopsOnNametag) {
            try {
                int pops = Visuals.totemPopsFor(player.getUuid());
                if (pops > 0) {
                    int col = pops >= 3 ? cfg.totemColBad : pops == 2 ? cfg.totemColWarn : cfg.totemColGood;
                    result = result.copy().append(Text.literal(" ")).append(Icons.totem())
                            .append(Text.literal(" " + pops).setStyle(Style.EMPTY.withColor(col & 0xFFFFFF).withBold(true)));
                    changed = true;
                }
            } catch (Throwable ignored) {
            }
        }

        if (changed) {
            state.displayName = result;
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), require = 0)
    private void aero$scale(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                             CameraRenderState cameraState, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null) {
            float s = Math.max(0.5F, AeroClient.CONFIG.nametagScale);
            matrices.push();
            matrices.translate(0.0, AeroClient.CONFIG.nametagYOffset, 0.0);
            net.minecraft.util.math.Vec3d p = state == null ? null : state.nameLabelPos;
            if (p != null && s != 1f) {
                // Text hangs 0.2 blocks below its anchor, 0.5 above the head: scale around the anchor so
                // the tag stays put, and only lift it once the bigger text would reach into the head.
                float lift = Math.max(0f, 0.2f * s - 0.4f);
                matrices.translate(0.0, lift, 0.0);
                matrices.translate(p.x, p.y, p.z);
                matrices.scale(s, s, s);
                matrices.translate(-p.x, -p.y, -p.z);
            }
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
