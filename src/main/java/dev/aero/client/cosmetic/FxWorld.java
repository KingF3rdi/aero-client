package dev.aero.client.cosmetic;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Our own world-space particles: small glowing cubes drawn in the normal world render pass, so
 * trails, kill/mace effects and custom totem pops work in first person too and don't depend on the
 * vanilla particle engine or the player's particle setting. Positions are computed analytically from
 * the spawn time, so no per-tick simulation is needed.
 */
public final class FxWorld {
    private static final class P {
        long t0;
        int life;
        double x0, y0, z0, vx, vy, vz, g;
        float s0, s1;
        int color;
        double r0, rv, w, a0;
        /** Shape: size multipliers per axis, fixed yaw, spin (rad/s), star = 3D cross that twinkles. */
        float ax = 1, ay = 1, az = 1, yaw, spin;
        boolean star;
    }

    /** Per-player trail state (footstep rhythm, helix/rainbow phase). */
    public static final class Walker {
        double x, z, acc;
        boolean have, left, ground;
        float phase;
    }

    private static final java.util.ArrayDeque<P> LIST = new java.util.ArrayDeque<>();
    private static final int MAX = 900;
    private static final double MAX_DIST_SQ = 72 * 72;

    private FxWorld() {}

    public static void register() {
        try {
            WorldRenderEvents.AFTER_ENTITIES.register(FxWorld::render);
        } catch (Throwable ignored) {
        }
    }

    private static ThreadLocalRandom rnd() {
        return ThreadLocalRandom.current();
    }

    private static P add(int life, double x, double y, double z, double vx, double vy, double vz, double g,
                         float s0, float s1, int color) {
        P p = new P();
        p.t0 = System.currentTimeMillis();
        p.life = life;
        p.x0 = x;
        p.y0 = y;
        p.z0 = z;
        p.vx = vx;
        p.vy = vy;
        p.vz = vz;
        p.g = g;
        p.s0 = s0;
        p.s1 = s1;
        p.color = color;
        synchronized (LIST) {
            if (LIST.size() >= MAX) {
                LIST.pollFirst();
            }
            LIST.addLast(p);
        }
        return p;
    }

    private static void swirl(P p, double r0, double rv, double w, double a0) {
        p.r0 = r0;
        p.rv = rv;
        p.w = w;
        p.a0 = a0;
    }

    private static int tint(int base, float f) {
        return CubeDraw.shade(base, f);
    }

    // ---- emitters -------------------------------------------------------------------------

    /** One trail step for a player who moved (dx, dz) since the last tick and now stands at (x, y, z). */
    /** Aura: two lights circling the feet, also while standing still (called every tick). */
    public static void aura(int color, Walker wk, double x, double y, double z) {
        for (int i = 0; i < 2; i++) {
            P p = add(650, x, y + 0.06, z, 0, 0.12, 0, 0, 0.1f, 0.02f, i == 0 ? color : CubeDraw.shade(color, 1.3f));
            swirl(p, 0.62, 0, 3.2, wk.phase + i * Math.PI);
        }
        wk.phase += 0.42f;
    }

    /** Jump rings: a ring that spreads out over the ground where the player took off. */
    public static void ring(int color, double x, double y, double z) {
        for (int i = 0; i < 26; i++) {
            P p = add(620, x, y + 0.05, z, 0, 0, 0, 0, 0.11f, 0.01f, i % 2 == 0 ? color : CubeDraw.shade(color, 1.3f));
            swirl(p, 0.25, 2.3, 0, i * Math.PI * 2 / 26);
        }
    }

    public static void trail(String id, int color, Walker wk, double x, double y, double z, double dx, double dz) {
        var r = rnd();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double px = -dz / dist; // unit vector to the player's side
        double pz = dx / dist;
        switch (id) {
            case "rainbow" -> { // a rainbow band painted on the ground behind the player
                int n = Math.max(1, (int) Math.ceil(dist / 0.11));
                for (int k = 1; k <= n; k++) {
                    double f = k / (double) n;
                    double bx = x - dx * (1 - f);
                    double bz = z - dz * (1 - f);
                    wk.phase += 0.012f;
                    for (int band = 0; band < 4; band++) {
                        double off = (band - 1.5) * 0.085;
                        int col = java.awt.Color.HSBtoRGB((wk.phase + band * 0.13f) % 1f, 0.7f, 1f);
                        P p = add(1300, bx + px * off, y + 0.03, bz + pz * off, 0, 0, 0, 0, 0.1f, 0.03f, col);
                        p.ay = 0.25f;
                    }
                }
            }
            case "steps" -> { // glowing footprints, left and right
                wk.acc += dist;
                if (wk.acc >= 0.55) {
                    wk.acc = 0;
                    wk.left = !wk.left;
                    double off = wk.left ? 0.13 : -0.13;
                    P p = add(2600, x + px * off, y + 0.02, z + pz * off, 0, 0, 0, 0, 0.16f, 0.08f, color);
                    p.ax = 0.6f;
                    p.ay = 0.15f;
                    p.az = 1.1f;
                    p.yaw = (float) Math.atan2(dx, dz);
                }
            }
            case "helix" -> { // two strands coiling along the path
                int n = Math.max(1, (int) Math.ceil(dist / 0.12));
                for (int k = 1; k <= n; k++) {
                    double f = k / (double) n;
                    double bx = x - dx * (1 - f);
                    double bz = z - dz * (1 - f);
                    wk.phase += 0.12f / n * (float) (dist / 0.12);
                    for (int strand = 0; strand < 2; strand++) {
                        double a = wk.phase * 6 + strand * Math.PI;
                        double side = Math.cos(a) * 0.35;
                        double up = Math.sin(a) * 0.35;
                        add(1000, bx + px * side, y + 0.9 + up, bz + pz * side, 0, 0, 0, 0, 0.09f, 0.02f,
                                strand == 0 ? color : CubeDraw.mix(color, 0xFFFFFFFF, 0.45f));
                    }
                }
            }
            case "sakura" -> { // petals tumbling down around the player
                if (r.nextFloat() < 0.8f) {
                    P p = add(2200, x + jitter(0.45), y + 1.6 + r.nextDouble() * 0.4, z + jitter(0.45), jitter(0.15), -0.4, jitter(0.15), 0,
                            0.13f, 0.08f, r.nextBoolean() ? color : CubeDraw.mix(color, 0xFFFFFFFF, 0.45f));
                    swirl(p, 0.12, 0.05, 2.5, r.nextDouble() * 6.28);
                    p.ax = 1.1f;
                    p.ay = 0.2f;
                    p.az = 0.75f;
                    p.spin = 2.5f + r.nextFloat() * 3f;
                    p.yaw = r.nextFloat() * 6.28f;
                }
            }
            case "stars" -> { // twinkling stars left hanging in the air
                P p = add(1300, x + jitter(0.5), y + 0.3 + r.nextDouble() * 1.4, z + jitter(0.5), 0, 0.05, 0, 0, 0.16f, 0.01f,
                        r.nextInt(3) == 0 ? 0xFFFFFFFF : color);
                p.star = true;
                p.yaw = r.nextFloat() * 6.28f;
            }
            case "heart" -> add(1100, x + jitter(0.25), y + 0.2, z + jitter(0.25), 0, 0.7, 0, 0, 0.16f, 0.02f, color);
            case "snow" -> add(1400, x + jitter(0.35), y + 0.9, z + jitter(0.35), jitter(0.2), -0.5, jitter(0.2), 0, 0.09f, 0.03f, color);
            case "void" -> {
                P p = add(1000, x, y + 0.15, z, 0, 0.4, 0, 0, 0.14f, 0.02f, color);
                swirl(p, 0.25, 0, 4, r.nextDouble() * 6.28);
            }
            case "gold" -> add(900, x + jitter(0.3), y + 0.1 + r.nextDouble() * 0.3, z + jitter(0.3), 0, 0.3, 0, 0, 0.1f, 0f, r.nextBoolean() ? color : CubeDraw.shade(color, 1.3f));
            case "magma" -> add(700, x + jitter(0.15), y + 0.1, z + jitter(0.15), jitter(0.4), 0.5, jitter(0.4), 4, 0.17f, 0.03f, tint(color, 0.7f + r.nextFloat() * 0.5f));
            case "plasma" -> {
                P p = add(800, x, y + 0.5, z, 0, 0.2, 0, 0, 0.13f, 0.02f, color);
                swirl(p, 0.35, 0.3, 7, r.nextDouble() * 6.28);
            }
            case "spirit" -> add(1500, x + jitter(0.2), y + 0.4, z + jitter(0.2), jitter(0.1), 0.35, jitter(0.1), 0, 0.22f, 0.02f, color);
            default -> {
                add(800, x + jitter(0.2), y + 0.1, z + jitter(0.2), jitter(0.6), 0.6, jitter(0.6), 2.2, 0.11f, 0.02f, color);
                add(800, x + jitter(0.2), y + 0.1, z + jitter(0.2), jitter(0.6), 0.6, jitter(0.6), 2.2, 0.08f, 0.02f, CubeDraw.shade(color, 1.35f));
            }
        }
    }

    private static double jitter(double amp) {
        return (rnd().nextDouble() - 0.5) * 2 * amp;
    }

    public static void kill(String id, int color, double x, double y, double z) {
        var r = rnd();
        switch (id) {
            case "ember" -> {
                for (int i = 0; i < 18; i++) {
                    add(1100, x + jitter(0.4), y + 0.2 + r.nextDouble(), z + jitter(0.4), jitter(0.5), 0.9 + r.nextDouble(), jitter(0.5), -0.6,
                            0.16f, 0.02f, tint(color, 0.7f + r.nextFloat() * 0.6f));
                }
            }
            case "venom" -> {
                for (int i = 0; i < 16; i++) {
                    add(1200, x + jitter(0.3), y + 0.3, z + jitter(0.3), jitter(1.2), 0.8 + r.nextDouble(), jitter(1.2), 1.5, 0.14f, 0.03f, tint(color, 0.7f + r.nextFloat() * 0.6f));
                }
            }
            case "lightning" -> {
                double cx = x, cz = z;
                for (int i = 0; i < 16; i++) {
                    cx += jitter(0.18);
                    cz += jitter(0.18);
                    add(420, cx, y + i * 0.45, cz, 0, 0, 0, 0, 0.2f, 0.05f, i % 3 == 0 ? 0xFFFFFFFF : color);
                }
                for (int i = 0; i < 10; i++) {
                    add(500, x, y + 0.2, z, jitter(3), 1 + r.nextDouble() * 2, jitter(3), 6, 0.1f, 0.02f, color);
                }
            }
            case "soul" -> {
                for (int i = 0; i < 12; i++) {
                    P p = add(1500, x, y + 0.2, z, 0, 0.8 + r.nextDouble() * 0.6, 0, 0, 0.2f, 0.02f, tint(color, 0.8f + r.nextFloat() * 0.5f));
                    swirl(p, 0.2 + r.nextDouble() * 0.4, 0.1, 3 + r.nextDouble() * 2, r.nextDouble() * 6.28);
                }
            }
            case "void" -> {
                for (int i = 0; i < 20; i++) {
                    P p = add(700, x, y + 0.9, z, 0, 0, 0, 0, 0.16f, 0.02f, tint(color, 0.6f + r.nextFloat() * 0.7f));
                    swirl(p, 1.6, -2.2, 6, r.nextDouble() * 6.28);
                }
            }
            case "totem" -> {
                for (int i = 0; i < 22; i++) {
                    add(1000, x, y + 1, z, jitter(2.5), 1 + r.nextDouble() * 2.5, jitter(2.5), 5, 0.15f, 0.02f,
                            r.nextBoolean() ? 0xFF8CE060 : 0xFFFFD040);
                }
            }
            case "nova" -> {
                for (int i = 0; i < 28; i++) {
                    P p = add(700, x, y + 1, z, 0, jitter(0.4), 0, 0, 0.17f, 0.02f, tint(color, 0.7f + r.nextFloat() * 0.6f));
                    swirl(p, 0.2, 3.4, 0, i * 6.283 / 28);
                }
            }
            default -> {
                for (int i = 0; i < 20; i++) {
                    add(800, x, y + 1, z, jitter(3), 0.5 + r.nextDouble() * 2.5, jitter(3), 4, 0.13f, 0.02f, tint(color, 0.7f + r.nextFloat() * 0.6f));
                }
            }
        }
    }

    public static void mace(String id, int color, double x, double y, double z) {
        var r = rnd();
        switch (id) {
            case "quake" -> {
                for (int i = 0; i < 34; i++) {
                    add(900, x + jitter(0.3), y + 0.1, z + jitter(0.3), jitter(2.2), 1.5 + r.nextDouble() * 3, jitter(2.2), 12, 0.15f, 0.04f, tint(color, 0.6f + r.nextFloat() * 0.6f));
                }
            }
            case "thunder" -> kill("lightning", color, x, y, z);
            case "crater" -> {
                for (int i = 0; i < 26; i++) {
                    add(1100, x + jitter(0.4), y + 0.1, z + jitter(0.4), jitter(1.6), 3 + r.nextDouble() * 4, jitter(1.6), 16, 0.2f, 0.08f, tint(color, 0.5f + r.nextFloat() * 0.6f));
                }
            }
            case "nova" -> {
                for (int i = 0; i < 40; i++) {
                    P p = add(900, x, y + 0.6, z, 0, 0.7 + jitter(0.4), 0, 0, 0.16f, 0.02f, tint(color, 0.7f + r.nextFloat() * 0.6f));
                    swirl(p, 0.2, 3.8, 3, i * 6.283 / 40);
                }
            }
            default -> {
                for (int i = 0; i < 40; i++) {
                    P p = add(800, x, y + 0.1, z, 0, 0.1, 0, 0, 0.16f, 0.03f, tint(color, 0.7f + r.nextFloat() * 0.6f));
                    swirl(p, 0.3, 4.2, 0, i * 6.283 / 40);
                }
            }
        }
    }

    /** Totem pop particles; count/size/speed/life are multipliers from the Totem Counter module. */
    public static void totemPop(String style, int color, double x, double y, double z) {
        var cfg = dev.aero.client.AeroClient.CONFIG;
        float cnt = cfg == null ? 1f : Math.max(0.1f, cfg.totemPopCount);
        float sz = cfg == null ? 1f : Math.max(0.2f, cfg.totemPopFxSize);
        float sp = cfg == null ? 1f : Math.max(0.1f, cfg.totemPopSpeed);
        float lf = cfg == null ? 1f : Math.max(0.2f, cfg.totemPopLife);
        int c2 = cfg == null ? color : cfg.totemPopColor2;
        boolean two = cfg != null && cfg.totemPopTwoColors;
        var r = rnd();
        int c = color | 0xFF000000;
        switch (style) {
            case "Spiral" -> {
                int n = Math.max(1, Math.round(36 * cnt));
                for (int i = 0; i < n; i++) {
                    P p = add(Math.round(1100 * lf), x, y + 0.1 + i * 0.03 * (36f / n), z, 0, 1.7 * sp, 0, 0, 0.13f * sz, 0.02f, tint(two && i % 2 == 1 ? c2 | 0xFF000000 : c, 0.7f + (i % 3) * 0.25f));
                    swirl(p, 0.55, 0, 7 * sp, i * 0.5 + (i % 2) * Math.PI);
                }
            }
            case "Rings" -> {
                int n = Math.max(3, Math.round(18 * cnt));
                for (int ring = 0; ring < 3; ring++) {
                    for (int i = 0; i < n; i++) {
                        P p = add(Math.round((800 + ring * 120) * lf), x, y + 0.2 + ring * 0.8, z, 0, 0.3 * sp, 0, 0, 0.13f * sz, 0.02f, tint(two && ring == 1 ? c2 | 0xFF000000 : c, 0.8f + ring * 0.2f));
                        swirl(p, 0.2, 2.6 * sp, 0, i * 6.283 / n);
                    }
                }
            }
            case "Hearts" -> {
                int n = Math.max(1, Math.round(16 * cnt));
                for (int i = 0; i < n; i++) {
                    P p = add(Math.round(1300 * lf), x, y + 0.3, z, 0, (0.9 + r.nextDouble() * 0.6) * sp, 0, 0, 0.17f * sz, 0.02f, i % 2 == 0 ? c : (two ? c2 | 0xFF000000 : 0xFFFF7BAA));
                    swirl(p, 0.3 + r.nextDouble() * 0.5, 0.2, 2 + r.nextDouble() * 2, r.nextDouble() * 6.28);
                }
            }
            case "Soul" -> {
                int n = Math.max(1, Math.round(14 * cnt));
                for (int i = 0; i < n; i++) {
                    P p = add(Math.round(1600 * lf), x, y + 0.2, z, 0, (0.6 + r.nextDouble() * 0.6) * sp, 0, 0, 0.2f * sz, 0.02f, tint(two && i % 2 == 1 ? c2 | 0xFF000000 : c, 0.7f + r.nextFloat() * 0.5f));
                    swirl(p, 0.2, 0.15, 3, r.nextDouble() * 6.28);
                }
            }
            case "Off" -> {
            }
            default -> {
                int n = Math.max(1, Math.round(32 * cnt));
                for (int i = 0; i < n; i++) {
                    add(Math.round(900 * lf), x, y + 1, z, jitter(3.2 * sp), (0.5 + r.nextDouble() * 3) * sp, jitter(3.2 * sp), 6, 0.14f * sz, 0.02f, tint(two && i % 2 == 1 ? c2 | 0xFF000000 : c, 0.6f + r.nextFloat() * 0.8f));
                }
            }
        }
    }

    // ---- rendering ------------------------------------------------------------------------

    private static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (mc.world == null || matrices == null || consumers == null) {
            synchronized (LIST) {
                LIST.clear();
            }
            return;
        }
        long now = System.currentTimeMillis();
        Vec3d cam = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
                ? mc.gameRenderer.getCamera().getCameraPos() : Vec3d.ZERO;
        synchronized (LIST) {
            LIST.removeIf(p -> now - p.t0 > p.life);
            if (LIST.isEmpty()) {
                return;
            }
            // Everything lives on the render thread, so draw straight from the list (no per-frame copy).
            draw(matrices, consumers.getBuffer(RenderLayers.entityCutoutNoCull(CubeDraw.WHITE)), cam, now);
        }
    }

    private static void draw(MatrixStack matrices, VertexConsumer vc, Vec3d cam, long now) {
        for (P p : LIST) {
            double t = (now - p.t0) / 1000.0;
            float f = (now - p.t0) / (float) p.life;
            double rad = p.r0 + p.rv * t;
            double ang = p.a0 + p.w * t;
            double x = p.x0 + p.vx * t + Math.cos(ang) * rad;
            double z = p.z0 + p.vz * t + Math.sin(ang) * rad;
            double y = p.y0 + p.vy * t - 0.5 * p.g * t * t;
            float size = p.s0 + (p.s1 - p.s0) * f;
            if (p.star) {
                size *= 0.6f + 0.4f * (float) Math.sin(t * 12 + p.yaw * 3);
            }
            double ox = x - cam.x;
            double oy = y - cam.y;
            double oz = z - cam.z;
            if (size <= 0.004f || ox * ox + oy * oy + oz * oz > MAX_DIST_SQ) {
                continue;
            }
            matrices.push();
            matrices.translate(ox, oy, oz);
            if (p.yaw != 0 || p.spin != 0) {
                float sp = (float) (p.spin * t);
                matrices.multiply(new org.joml.Quaternionf().rotateY(p.yaw + sp).rotateX(sp * 0.7f));
            }
            float h = size / 2f;
            if (p.star) {
                float thin = h * 0.22f;
                CubeDraw.cube(matrices.peek(), vc, h, thin, thin, p.color, CubeDraw.FULLBRIGHT);
                CubeDraw.cube(matrices.peek(), vc, thin, h, thin, p.color, CubeDraw.FULLBRIGHT);
                CubeDraw.cube(matrices.peek(), vc, thin, thin, h, p.color, CubeDraw.FULLBRIGHT);
            } else {
                CubeDraw.cube(matrices.peek(), vc, h * p.ax, h * p.ay, h * p.az, p.color, CubeDraw.FULLBRIGHT);
            }
            matrices.pop();
        }
    }
}
