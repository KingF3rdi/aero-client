package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.9+ refactored entity rendering onto per-frame "render state" objects: renderLabelIfPresent
 * no longer takes the entity/text/light directly (state.displayName / MatrixStack / a render queue
 * instead), so the old signature guess here silently never matched anything (require=0 hid it).
 * Text edits now happen in updateRenderState, which still hands us the live Entity plus the state
 * object whose displayName field actually gets drawn.
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$ownNameDist(Entity entity, double dist, CallbackInfoReturnable<Boolean> cir) {
        aero$ownName(entity, cir);
    }

    @Inject(method = "hasLabel", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$ownName(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.nametags || !AeroClient.CONFIG.ownNametag) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entity == mc.player) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"), require = 0)
    private void aero$label(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (state == null || state.displayName == null) {
            return;
        }
        Text result = state.displayName;
        boolean changed = false;

        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && AeroClient.CONFIG.nametagPing
                && entity instanceof PlayerEntity player) {
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

        if (AeroClient.CONFIG != null && AeroClient.CONFIG.tierTagger && entity instanceof PlayerEntity player) {
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

        if (AeroClient.CONFIG != null && AeroClient.CONFIG.totemCounter && AeroClient.CONFIG.totemPopsOnNametag) {
            try {
                int pops = dev.aero.client.Visuals.totemPopsFor(entity.getUuid());
                if (pops > 0) {
                    result = result.copy().append(Text.literal(" §7(§d" + pops + " pop" + (pops == 1 ? "" : "s") + "§7)"));
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
    private void aero$scale(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                             CameraRenderState cameraState, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null) {
            float s = Math.max(0.5F, AeroClient.CONFIG.nametagScale);
            matrices.push();
            matrices.scale(s, s, s);
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("RETURN"), require = 0)
    private void aero$unscale(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                               CameraRenderState cameraState, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null) {
            try {
                matrices.pop();
            } catch (Throwable ignored) {
            }
        }
    }
}
