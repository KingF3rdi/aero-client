package dev.aero.client.cosmetic;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;

/** The "Spiked" shield cosmetic: dark plate, metal rim, boss and spikes out of ~22 cubes (about 130 quads). Faces -Z. */
public final class SpikedShield {
    private static final int PLATE = 0xFF14151A;
    private static final int RIM = 0xFF5A626E;
    private static final int BOSS = 0xFF7A838F;
    private static final int SPIKE = 0xFFAEB7C4;
    private static final int TIP = 0xFF9AA4B0;

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
        int rim = spikes ? RIM : CubeDraw.shade(plate, 0.65f);
        int boss = spikes ? BOSS : CubeDraw.shade(plate, 1.35f);
        box(m, q, layer, light, 0, 0, 0, 0.30f, 0.42f, 0.04f, plate);
        box(m, q, layer, light, 0, 0.43f, 0, 0.33f, 0.03f, 0.055f, rim);
        box(m, q, layer, light, 0, -0.43f, 0, 0.33f, 0.03f, 0.055f, rim);
        box(m, q, layer, light, 0.31f, 0, 0, 0.03f, 0.42f, 0.055f, rim);
        box(m, q, layer, light, -0.31f, 0, 0, 0.03f, 0.42f, 0.055f, rim);
        box(m, q, layer, light, 0, 0, -0.07f, 0.09f, 0.09f, 0.03f, boss);
        if (!spikes) {
            return;
        }
        float[] half = {0.07f, 0.05f, 0.032f, 0.018f};
        float[] zs = {-0.13f, -0.19f, -0.245f, -0.29f};
        for (int i = 0; i < 4; i++) {
            box(m, q, layer, light, 0, 0, zs[i], half[i], half[i], 0.03f, SPIKE);
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                for (int i = 0; i < 3; i++) {
                    float h = 0.04f - i * 0.012f;
                    box(m, q, layer, light, sx * (0.24f + i * 0.03f), sy * (0.35f + i * 0.03f), -0.09f - i * 0.05f, h, h, 0.03f, TIP);
                }
            }
        }
    }
}
