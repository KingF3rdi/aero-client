package dev.aero.client.cosmetic;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Flat-colored cube geometry shared by the cosmetic feature renderer and the world particles. */
public final class CubeDraw {
    public static final Identifier WHITE = Identifier.of("minecraft", "textures/block/white_concrete.png");
    public static final int FULLBRIGHT = 0xF000F0;

    private CubeDraw() {}

    /** Cube centered on the origin with half extents a, b, c. */
    public static void cube(MatrixStack.Entry e, VertexConsumer vc, float a, float b, float c, int col, int light) {
        col |= 0xFF000000;
        face(e, vc, col, light, 1, 0, 0, a, -b, -c, a, -b, c, a, b, c, a, b, -c);
        face(e, vc, col, light, -1, 0, 0, -a, -b, c, -a, -b, -c, -a, b, -c, -a, b, c);
        face(e, vc, col, light, 0, 1, 0, -a, b, -c, a, b, -c, a, b, c, -a, b, c);
        face(e, vc, col, light, 0, -1, 0, -a, -b, c, a, -b, c, a, -b, -c, -a, -b, -c);
        face(e, vc, col, light, 0, 0, 1, -a, -b, c, a, -b, c, a, b, c, -a, b, c);
        face(e, vc, col, light, 0, 0, -1, a, -b, -c, -a, -b, -c, -a, b, -c, a, b, -c);
    }

    /**
     * Smooth cone: apex at the origin, base ring of radius r at y = +h (model space, where +y is down),
     * so hats are round instead of stepped boxes. Shaded from `top` at the apex to `rim`; `under` closes it.
     */
    public static void cone(MatrixStack.Entry e, VertexConsumer vc, float r, float h, int segments, int top, int rim, int under, int light) {
        top |= 0xFF000000;
        rim |= 0xFF000000;
        under |= 0xFF000000;
        float len = (float) Math.sqrt(r * r + h * h);
        float nh = h / len; // horizontal part of the side normal
        float nv = -r / len; // vertical part (pointing up)
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments;
            double a1 = (i + 1) * Math.PI * 2 / segments;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float cm = (float) Math.cos((a0 + a1) / 2), sm = (float) Math.sin((a0 + a1) / 2);
            vertex(e, vc, 0, 0, 0, 0.5f, 0, top, light, cm * nh, nv, sm * nh);
            vertex(e, vc, 0, 0, 0, 0.5f, 0, top, light, cm * nh, nv, sm * nh);
            vertex(e, vc, c1 * r, h, s1 * r, 1, 1, rim, light, c1 * nh, nv, s1 * nh);
            vertex(e, vc, c0 * r, h, s0 * r, 0, 1, rim, light, c0 * nh, nv, s0 * nh);
            vertex(e, vc, 0, h, 0, 0.5f, 0.5f, under, light, 0, 1, 0);
            vertex(e, vc, 0, h, 0, 0.5f, 0.5f, under, light, 0, 1, 0);
            vertex(e, vc, c0 * r, h, s0 * r, 0, 1, under, light, 0, 1, 0);
            vertex(e, vc, c1 * r, h, s1 * r, 1, 1, under, light, 0, 1, 0);
        }
    }

    private static void face(MatrixStack.Entry e, VertexConsumer vc, int col, int light, float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        vertex(e, vc, x1, y1, z1, 0, 0, col, light, nx, ny, nz);
        vertex(e, vc, x2, y2, z2, 1, 0, col, light, nx, ny, nz);
        vertex(e, vc, x3, y3, z3, 1, 1, col, light, nx, ny, nz);
        vertex(e, vc, x4, y4, z4, 0, 1, col, light, nx, ny, nz);
    }

    private static void vertex(MatrixStack.Entry e, VertexConsumer vc, float x, float y, float z, float u, float v,
                               int col, int light, float nx, float ny, float nz) {
        vc.vertex(e, x, y, z).color(col).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
    }

    /** Cape box in the standard cape layout (outside = px (1,1)-(11,17), inside = (12,1)-(22,17)) for a texW x texH texture. */
    public static void capeBox(MatrixStack.Entry e, VertexConsumer vc, float a, float b, float c, int light, int texW, int texH) {
        int w = 0xFFFFFFFF;
        float tw = texW;
        float th = texH;
        uvFace(e, vc, w, light, 0, 0, 1, -a, -b, c, a, -b, c, a, b, c, -a, b, c, 1 / tw, 1 / th, 11 / tw, 17 / th);
        uvFace(e, vc, w, light, 0, 0, -1, a, -b, -c, -a, -b, -c, -a, b, -c, a, b, -c, 12 / tw, 1 / th, 22 / tw, 17 / th);
        uvFace(e, vc, w, light, 1, 0, 0, a, -b, -c, a, -b, c, a, b, c, a, b, -c, 0f, 1 / th, 1 / tw, 17 / th);
        uvFace(e, vc, w, light, -1, 0, 0, -a, -b, c, -a, -b, -c, -a, b, -c, -a, b, c, 11 / tw, 1 / th, 12 / tw, 17 / th);
        uvFace(e, vc, w, light, 0, 1, 0, -a, b, -c, a, b, -c, a, b, c, -a, b, c, 11 / tw, 0f, 21 / tw, 1 / th);
        uvFace(e, vc, w, light, 0, -1, 0, -a, -b, c, a, -b, c, a, -b, -c, -a, -b, -c, 1 / tw, 0f, 11 / tw, 1 / th);
    }

    private static void uvFace(MatrixStack.Entry e, VertexConsumer vc, int col, int light, float nx, float ny, float nz,
                               float x1, float y1, float z1, float x2, float y2, float z2,
                               float x3, float y3, float z3, float x4, float y4, float z4,
                               float u0, float v0, float u1, float v1) {
        vertex(e, vc, x1, y1, z1, u0, v0, col, light, nx, ny, nz);
        vertex(e, vc, x2, y2, z2, u1, v0, col, light, nx, ny, nz);
        vertex(e, vc, x3, y3, z3, u1, v1, col, light, nx, ny, nz);
        vertex(e, vc, x4, y4, z4, u0, v1, col, light, nx, ny, nz);
    }

    public static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
