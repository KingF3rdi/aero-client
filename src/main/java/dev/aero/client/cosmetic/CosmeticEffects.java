package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.random.Random;

/**
 * Renders equipped Trail/Wings/Kill-effect cosmetics as small flat-colored dust particles - matches
 * the "few pixels, good colors" look asked for and needs no custom textures or 3D models, just a
 * handful of particles per tick/event so it reads as a clean accent rather than clutter.
 *
 * Self-visible only for now: nothing here broadcasts a cosmetic choice to other players, since a
 * pure client mod has no way to show a chosen cosmetic on someone else's client without server
 * cooperation or a shared backend, and neither exists yet - see AeroClient's cosmetic package notes.
 */
public final class CosmeticEffects {
    private CosmeticEffects() {}

    private static double lastX, lastZ;
    private static boolean havePos;
    private static int trailPhase;
    private static int capePhase;
    private static int headPhase;
    private static int petPhase;

    public static void tick(MinecraftClient client) {
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

        tickTrail(client, player, moved);
        tickWings(client, player);
        tickCape(client, player, moved);
        tickHead(client, player);
        tickPet(client, player, moved);
    }

    private static void tickTrail(MinecraftClient client, ClientPlayerEntity player, double moved) {
        if (moved < 0.02) {
            return;
        }
        Cosmetics.Item item = equippedItem(Cosmetics.Kind.TRAIL);
        if (item == null) {
            return;
        }
        // One puff every 2 ticks while moving - a light accent, not a firehose.
        if (++trailPhase % 2 != 0) {
            return;
        }
        spawnPuff(client, player.getX(), player.getY() + 0.1, player.getZ(), item.color(), 2);
    }

    private static void tickWings(MinecraftClient client, ClientPlayerEntity player) {
        // Only while airborne, so it reads as wings catching air rather than a permanent back-glow.
        if (player.isOnGround()) {
            return;
        }
        Cosmetics.Item item = equippedItem(Cosmetics.Kind.WINGS);
        if (item == null) {
            return;
        }
        double yaw = Math.toRadians(player.getYaw());
        double backX = -Math.sin(yaw) * 0.3;
        double backZ = Math.cos(yaw) * 0.3;
        double sideX = Math.cos(yaw) * 0.35;
        double sideZ = Math.sin(yaw) * 0.35;
        double baseX = player.getX() - backX;
        double baseY = player.getY() + 1.1;
        double baseZ = player.getZ() - backZ;
        spawnPuff(client, baseX + sideX, baseY, baseZ + sideZ, item.color(), 1);
        spawnPuff(client, baseX - sideX, baseY, baseZ - sideZ, item.color(), 1);
    }

    private static void tickCape(MinecraftClient client, ClientPlayerEntity player, double moved) {
        if (moved < 0.02) {
            return;
        }
        Cosmetics.Item item = equippedItem(Cosmetics.Kind.CAPE);
        if (item == null) {
            return;
        }
        // Every 3 ticks at shoulder height, behind the player - a cloak catching the air as they walk.
        if (++capePhase % 3 != 0) {
            return;
        }
        double yaw = Math.toRadians(player.getYaw());
        double backX = -Math.sin(yaw) * 0.25;
        double backZ = Math.cos(yaw) * 0.25;
        spawnPuff(client, player.getX() - backX, player.getY() + 1.2, player.getZ() - backZ, item.color(), 1);
    }

    private static void tickHead(MinecraftClient client, ClientPlayerEntity player) {
        Cosmetics.Item item = equippedItem(Cosmetics.Kind.HEAD);
        if (item == null) {
            return;
        }
        // A slow, constant shimmer above the head - visible even standing still, unlike trail/wings.
        if (++headPhase % 8 != 0) {
            return;
        }
        spawnPuff(client, player.getX(), player.getY() + player.getHeight() + 0.15, player.getZ(), item.color(), 1);
    }

    private static void tickPet(MinecraftClient client, ClientPlayerEntity player, double moved) {
        if (moved < 0.02) {
            return;
        }
        Cosmetics.Item item = equippedItem(Cosmetics.Kind.PET);
        if (item == null) {
            return;
        }
        // A little companion puff trailing at ankle height, off to the side rather than underfoot.
        if (++petPhase % 4 != 0) {
            return;
        }
        double yaw = Math.toRadians(player.getYaw());
        double sideX = Math.cos(yaw) * 0.5;
        double sideZ = Math.sin(yaw) * 0.5;
        spawnPuff(client, player.getX() + sideX, player.getY() + 0.1, player.getZ() + sideZ, item.color(), 1);
    }

    /** Called from the chat-based own-kill detector (see Visuals.isOwnKillLine) - a pure client mod
     * has no direct kill-event hook, only the death message that already gets parsed there. */
    public static void onKill() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        Cosmetics.Item item = equippedItem(Cosmetics.Kind.KILL_EFFECT);
        if (item == null) {
            return;
        }
        spawnPuff(client, player.getX(), player.getY() + 1, player.getZ(), item.color(), 14);
    }

    private static Cosmetics.Item equippedItem(Cosmetics.Kind kind) {
        String id = Cosmetics.equipped(kind);
        if ("none".equals(id)) {
            return null;
        }
        return Cosmetics.named(kind, id);
    }

    private static void spawnPuff(MinecraftClient client, double x, double y, double z, int argb, int count) {
        if (client.world == null) {
            return;
        }
        DustParticleEffect effect = new DustParticleEffect(argb & 0xFFFFFF, 1.0f);
        Random rnd = client.world.random;
        for (int i = 0; i < count; i++) {
            double ox = (rnd.nextDouble() - 0.5) * 0.25;
            double oy = (rnd.nextDouble() - 0.5) * 0.25;
            double oz = (rnd.nextDouble() - 0.5) * 0.25;
            client.particleManager.addParticle(effect, x + ox, y + oy, z + oz, 0.0, 0.01, 0.0);
        }
    }
}
