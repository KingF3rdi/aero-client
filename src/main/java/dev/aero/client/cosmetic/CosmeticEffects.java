package dev.aero.client.cosmetic;

import dev.aero.client.AeroClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

/**
 * Drives trail, kill, mace and custom totem-pop effects (drawn by FxWorld). Has its own tick
 * registration so an exception elsewhere in the mod's tick can never stop cosmetics from running.
 */
public final class CosmeticEffects {
    private CosmeticEffects() {}

    private static double lastX, lastZ;
    private static boolean havePos;
    private static Entity lastAttacked;
    private static long lastAttackedAt;

    public static void register() {
        FxWorld.register();
        ClientTickEvents.END_CLIENT_TICK.register(CosmeticEffects::tick);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (world.isClient() && player == mc.player) {
                lastAttacked = entity;
                lastAttackedAt = System.currentTimeMillis();
                if (player.getMainHandStack().isOf(Items.MACE) && player.fallDistance > 1.5) {
                    String id = Cosmetics.equipped(Cosmetics.Kind.MACE);
                    if (!"none".equals(id)) {
                        FxWorld.mace(id, Cosmetics.equippedColor(Cosmetics.Kind.MACE), entity.getX(), entity.getY(), entity.getZ());
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static void tick(MinecraftClient client) {
        try {
            ClientPlayerEntity player = client.player;
            if (player == null || client.world == null) {
                havePos = false;
                return;
            }
            double dx = player.getX() - lastX;
            double dz = player.getZ() - lastZ;
            double moved = havePos ? Math.sqrt(dx * dx + dz * dz) : 0;
            lastX = player.getX();
            lastZ = player.getZ();
            havePos = true;
            if (moved < 0.02) {
                return;
            }
            String id = Cosmetics.equipped(Cosmetics.Kind.TRAIL);
            if (!"none".equals(id)) {
                FxWorld.trail(id, Cosmetics.equippedColor(Cosmetics.Kind.TRAIL), player.getX(), player.getY(), player.getZ());
            }
        } catch (Throwable ignored) {
        }
    }

    /** Chat-based own-kill detection (see Visuals.isOwnKillLine) - a client mod has no direct kill event. */
    public static void onKill() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        String id = Cosmetics.equipped(Cosmetics.Kind.KILL_EFFECT);
        if ("none".equals(id)) {
            return;
        }
        Entity at = lastAttacked != null && System.currentTimeMillis() - lastAttackedAt < 4000 ? lastAttacked : player;
        FxWorld.kill(id, Cosmetics.equippedColor(Cosmetics.Kind.KILL_EFFECT), at.getX(), at.getY(), at.getZ());
    }

    public static void onTotemPop(LivingEntity entity) {
        var cfg = AeroClient.CONFIG;
        if (cfg == null || entity == null || "Off".equals(cfg.totemPopFx)) {
            return;
        }
        FxWorld.totemPop(cfg.totemPopFx, cfg.totemPopFxColor, entity.getX(), entity.getY(), entity.getZ());
    }
}
