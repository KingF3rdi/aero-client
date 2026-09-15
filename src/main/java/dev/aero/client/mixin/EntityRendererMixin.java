package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = {"renderLabelIfPresent", "renderLabel"}, at = @At("HEAD"), require = 0)
    private void aero$scale(Entity entity, Text text, MatrixStack matrices, Object vertices, int light,
                            CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null) {
            float s = Math.max(0.5F, AeroClient.CONFIG.nametagScale);
            matrices.push();
            matrices.scale(s, s, s);
        }
    }

    @Inject(method = {"renderLabelIfPresent", "renderLabel"}, at = @At("RETURN"), require = 0)
    private void aero$unscale(Entity entity, Text text, MatrixStack matrices, Object vertices, int light,
                              CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && matrices != null) {
            try {
                matrices.pop();
            } catch (Throwable ignored) {
            }
        }
    }

    @ModifyVariable(method = {"renderLabelIfPresent", "renderLabel"}, at = @At("HEAD"), argsOnly = true, require = 0)
    private Text aero$ping(Text text, Entity entity) {
        Text result = text;
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.nametags && AeroClient.CONFIG.nametagPing
                && entity instanceof PlayerEntity player) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getNetworkHandler() != null) {
                try {
                    var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                    if (entry != null && result != null) {
                        String ping = entry.getLatency() + "ms";
                        String div = AeroClient.CONFIG.pingDivider ? " | " : " ";
                        result = "Left".equalsIgnoreCase(AeroClient.CONFIG.pingSide)
                                ? Text.literal("§7" + ping + "§r" + div).append(result)
                                : result.copy().append(Text.literal(div + "§7" + ping));
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.tierTagger && entity instanceof PlayerEntity player
                && result != null) {
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
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.totemCounter && AeroClient.CONFIG.totemPopsOnNametag
                && result != null) {
            try {
                int pops = dev.aero.client.Visuals.totemPopsFor(entity.getUuid());
                if (pops > 0) {
                    result = result.copy().append(Text.literal(" §7(§d" + pops + " pop" + (pops == 1 ? "" : "s") + "§7)"));
                }
            } catch (Throwable ignored) {
            }
        }
        return result;
    }
}
