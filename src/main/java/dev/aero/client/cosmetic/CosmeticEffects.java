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

    private static final java.util.Map<Integer, FxWorld.Walker> WALKERS = new java.util.HashMap<>();
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
                WALKERS.clear();
                return;
            }
            if (WALKERS.size() > 128) {
                WALKERS.clear();
            }
            boolean others = AeroClient.CONFIG == null || AeroClient.CONFIG.showOthersCosmetics;
            for (var p : client.world.getPlayers()) {
                String id;
                int color;
                if (p == player) {
                    id = Cosmetics.equipped(Cosmetics.Kind.TRAIL);
                    color = Cosmetics.equippedColor(Cosmetics.Kind.TRAIL);
                } else {
                    // Other Aero players' trails, only nearby ones.
                    if (!others || p.squaredDistanceTo(player) > 48 * 48 || !dev.aero.client.social.ClientUsers.hasCosmetics(p.getUuid())) {
                        continue;
                    }
                    id = dev.aero.client.social.ClientUsers.cosmeticOf(p.getUuid(), "trail");
                    Cosmetics.Item item = Cosmetics.named(Cosmetics.Kind.TRAIL, id);
                    if (item == null) {
                        continue;
                    }
                    color = item.color();
                }
                if ("none".equals(id) || p.isInvisible() || p.isSpectator()) {
                    continue;
                }
                FxWorld.Walker w = WALKERS.computeIfAbsent(p.getId(), k -> new FxWorld.Walker());
                double dx = p.getX() - w.x;
                double dz = p.getZ() - w.z;
                boolean had = w.have;
                w.x = p.getX();
                w.z = p.getZ();
                w.have = true;
                double moved = Math.sqrt(dx * dx + dz * dz);
                if (had && moved >= 0.02 && moved < 4) { // not standing still, not a teleport
                    FxWorld.trail(id, color, w, p.getX(), p.getY(), p.getZ(), dx, dz);
                }
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
