package dev.aero.client.cosmetic;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Shield cosmetic model: a heater-shaped shield (wide top, tapering to a point) built from a few dozen
 * cubes. "Spiked" adds a metal boss and spikes. Local space: x = width, y = up (tip at -0.5), the
 * front face looks toward -Z. About 60 quads for the plain shield, ~130 with spikes.
 */
public final class SpikedShield {
    private static final int PLATE = 0xFF14151A;
    private static final int RIM = 0xFF5A626E;
    private static final int BOSS = 0xFF7A838F;
    private static final int SPIKE = 0xFFAEB7C4;
    private static final int TIP = 0xFF9AA4B0;

    /** {centerY, halfHeight, halfWidth}: top block first, then the taper down to the point. */
    private static final float[][] ROWS = {
            {0.30f, 0.20f, 0.34f},
            {0.03f, 0.07f, 0.34f},
            {-0.11f, 0.07f, 0.30f},
            {-0.25f, 0.07f, 0.24f},
            {-0.37f, 0.05f, 0.17f},
            {-0.46f, 0.04f, 0.09f},
    };

    /** Render-state entity id -> arms holding a shield (bit 1 = left, 2 = right), set while updating the player render state. */
    public static final java.util.Map<Integer, Integer> ARMS = new java.util.concurrent.ConcurrentHashMap<>();

    private SpikedShield() {}

    private static void box(MatrixStack m, OrderedRenderCommandQueue q, RenderLayer layer, int light,
                            float x, float y, float z, float hx, float hy, float hz, int col) {
        m.push();
        m.translate(x, y, z);
        q.submitCustom(m, layer, (e, vc) -> CubeDraw.cube(e, vc, hx, hy, hz, col, light));
        m.pop();
    }

    /** Shield model for a cosmetic id: "spiked" = dark plate with spikes, any other id = plain plate in its color. */
    public static void drawFor(MatrixStack m, OrderedRenderCommandQueue q, RenderLayer layer, int light, String id, int color) {
        if ("spiked".equals(id)) {
            draw(m, q, layer, light, PLATE, true);
        } else {
            draw(m, q, layer, light, color | 0xFF000000, false);
        }
    }

    public static void draw(MatrixStack m, OrderedRenderCommandQueue q, RenderLayer layer, int light, int plate, boolean spikes) {
        int rim = spikes ? RIM : CubeDraw.shade(plate, 0.6f);
        int trim = CubeDraw.shade(rim, 1.5f);
        int boss = spikes ? BOSS : CubeDraw.shade(plate, 1.45f);
        int panel = CubeDraw.shade(plate, spikes ? 1.7f : 1.22f);
        int emblem = spikes ? CubeDraw.shade(BOSS, 0.85f) : CubeDraw.shade(plate, 1.75f);
        for (float[] r : ROWS) {
            float y = r[0];
            float hh = r[1];
            float hw = r[2];
            // dark rim behind and around, plate proud of it, inset panel and a bright trim line on top
            box(m, q, layer, light, 0, y, 0.005f, hw + 0.035f, hh + 0.004f, 0.045f, rim);
            box(m, q, layer, light, 0, y, -0.012f, hw, hh, 0.05f, plate);
            if (hw > 0.12f) {
                box(m, q, layer, light, 0, y, -0.058f, hw - 0.07f, Math.max(0.01f, hh - 0.012f), 0.012f, panel);
            }
            box(m, q, layer, light, -(hw + 0.02f), y, -0.03f, 0.014f, hh, 0.04f, trim);
            box(m, q, layer, light, hw + 0.02f, y, -0.03f, 0.014f, hh, 0.04f, trim);
        }
        // cross emblem on the upper block
        box(m, q, layer, light, 0, 0.16f, -0.072f, 0.03f, 0.27f, 0.012f, emblem);
        box(m, q, layer, light, 0, 0.26f, -0.072f, 0.19f, 0.03f, 0.012f, emblem);
        // boss (dark ring + lighter core)
        box(m, q, layer, light, 0, 0.14f, -0.08f, 0.12f, 0.12f, 0.02f, CubeDraw.shade(boss, 0.7f));
        box(m, q, layer, light, 0, 0.14f, -0.105f, 0.085f, 0.085f, 0.03f, boss);
        // rivets
        for (int sx = -1; sx <= 1; sx += 2) {
            box(m, q, layer, light, sx * 0.27f, 0.42f, -0.075f, 0.022f, 0.022f, 0.014f, boss);
            box(m, q, layer, light, sx * 0.27f, 0.10f, -0.075f, 0.022f, 0.022f, 0.014f, boss);
        }
        if (!spikes) {
            return;
        }
        float[] half = {0.07f, 0.05f, 0.032f, 0.018f};
        float[] zs = {-0.15f, -0.21f, -0.265f, -0.31f};
        for (int i = 0; i < 4; i++) {
            box(m, q, layer, light, 0, 0.14f, zs[i], half[i], half[i], 0.03f, SPIKE);
        }
        float[][] corners = {{-0.27f, 0.42f}, {0.27f, 0.42f}, {-0.27f, 0.10f}, {0.27f, 0.10f}};
        for (float[] c : corners) {
            for (int i = 0; i < 3; i++) {
                float h = 0.04f - i * 0.012f;
                box(m, q, layer, light, c[0] + Math.signum(c[0]) * i * 0.02f, c[1], -0.09f - i * 0.05f, h, h, 0.03f, TIP);
            }
        }
    }
}
