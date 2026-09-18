package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.random.Random;

/**
 * Trail and kill-effect cosmetics as small flat-colored dust particles. Cape, wings, headwear and
 * pet are real models now (see CosmeticFeatureRenderer). Self-visible only: a pure client mod can't
 * show a chosen cosmetic on someone else's client without a shared backend.
 */
public final class CosmeticEffects {
    private CosmeticEffects() {}

    private static double lastX, lastZ;
    private static boolean havePos;
    private static int trailPhase;

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
    }

    private static void tickTrail(MinecraftClient client, ClientPlayerEntity player, double moved) {
        if (moved < 0.02) {
            return;
        }
        Cosmetics.Item item = equippedItem(Cosmetics.Kind.TRAIL);
        if (item == null) {
            return;
        }
        if (++trailPhase % 2 != 0) {
            return;
        }
        spawnPuff(client, player.getX(), player.getY() + 0.1, player.getZ(), item.color(), 3);
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
