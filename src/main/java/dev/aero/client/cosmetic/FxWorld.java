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

import java.util.ArrayList;
import java.util.List;
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
    }

    private static final List<P> LIST = new ArrayList<>();
    private static final int MAX = 700;

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
                LIST.remove(0);
            }
            LIST.add(p);
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

    public static void trail(String id, int color, double x, double y, double z) {
        var r = rnd();
        switch (id) {
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

    public static void totemPop(String style, int color, double x, double y, double z) {
        var r = rnd();
        int c = color | 0xFF000000;
        switch (style) {
            case "Spiral" -> {
                for (int i = 0; i < 36; i++) {
                    P p = add(1100, x, y + 0.1 + i * 0.03, z, 0, 1.7, 0, 0, 0.13f, 0.02f, tint(c, 0.7f + (i % 3) * 0.25f));
                    swirl(p, 0.55, 0, 7, i * 0.5 + (i % 2) * Math.PI);
                }
            }
            case "Rings" -> {
                for (int ring = 0; ring < 3; ring++) {
                    for (int i = 0; i < 18; i++) {
                        P p = add(800 + ring * 120, x, y + 0.2 + ring * 0.8, z, 0, 0.3, 0, 0, 0.13f, 0.02f, tint(c, 0.8f + ring * 0.2f));
                        swirl(p, 0.2, 2.6, 0, i * 6.283 / 18);
                    }
                }
            }
            case "Hearts" -> {
                for (int i = 0; i < 16; i++) {
                    P p = add(1300, x, y + 0.3, z, 0, 0.9 + r.nextDouble() * 0.6, 0, 0, 0.17f, 0.02f, i % 2 == 0 ? c : 0xFFFF7BAA);
                    swirl(p, 0.3 + r.nextDouble() * 0.5, 0.2, 2 + r.nextDouble() * 2, r.nextDouble() * 6.28);
                }
            }
            case "Soul" -> {
                for (int i = 0; i < 14; i++) {
                    P p = add(1600, x, y + 0.2, z, 0, 0.6 + r.nextDouble() * 0.6, 0, 0, 0.2f, 0.02f, tint(c, 0.7f + r.nextFloat() * 0.5f));
                    swirl(p, 0.2, 0.15, 3, r.nextDouble() * 6.28);
                }
            }
            case "Off" -> {
            }
            default -> {
                for (int i = 0; i < 32; i++) {
                    add(900, x, y + 1, z, jitter(3.2), 0.5 + r.nextDouble() * 3, jitter(3.2), 6, 0.14f, 0.02f, tint(c, 0.6f + r.nextFloat() * 0.8f));
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
        List<P> snapshot;
        synchronized (LIST) {
            LIST.removeIf(p -> now - p.t0 > p.life);
            if (LIST.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(LIST);
        }
        Vec3d cam = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
                ? mc.gameRenderer.getCamera().getCameraPos() : Vec3d.ZERO;
        RenderLayer layer = RenderLayers.entityCutoutNoCull(CubeDraw.WHITE);
        VertexConsumer vc = consumers.getBuffer(layer);
        for (P p : snapshot) {
            double t = (now - p.t0) / 1000.0;
            float f = (now - p.t0) / (float) p.life;
            double rad = p.r0 + p.rv * t;
            double ang = p.a0 + p.w * t;
            double x = p.x0 + p.vx * t + Math.cos(ang) * rad;
            double z = p.z0 + p.vz * t + Math.sin(ang) * rad;
            double y = p.y0 + p.vy * t - 0.5 * p.g * t * t;
            float size = p.s0 + (p.s1 - p.s0) * f;
            if (size <= 0.004f) {
                continue;
            }
            matrices.push();
            matrices.translate(x - cam.x, y - cam.y, z - cam.z);
            CubeDraw.cube(matrices.peek(), vc, size / 2f, size / 2f, size / 2f, p.color, CubeDraw.FULLBRIGHT);
            matrices.pop();
        }
    }
}
