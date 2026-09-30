package dev.aero.client.ui;

import dev.aero.client.cosmetic.CubeDraw;
import dev.aero.client.cosmetic.Cosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Pixel-art style icons for the wardrobe grid and the preview effects, drawn with plain fills. */
public final class CosmeticIcons {
    private CosmeticIcons() {}

    private static void px(DrawContext c, float x, float y, int s, int col) {
        int ix = Math.round(x);
        int iy = Math.round(y);
        c.fill(ix, iy, ix + s, iy + s, col);
    }

    private static int alpha(int col, float a) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (col & 0xFFFFFF);
    }

    /** A thin line of pixels from (x,y) going at angle (deg from straight up, positive = right). */
    private static void ray(DrawContext c, float x, float y, float deg, float len, int th, int col) {
        double a = Math.toRadians(deg);
        for (float k = 0; k <= len; k += 1f) {
            px(c, x + (float) Math.sin(a) * k, y - (float) Math.cos(a) * k, th, col);
        }
    }

    public static void draw(DrawContext c, Cosmetics.Kind kind, Cosmetics.Item item, int color, int x, int y, int w, int h, float t) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        switch (kind) {
            case CAPE -> cape(c, item, color, cx, y, w, h, t);
            case WINGS -> wings(c, item.id(), color, cx, cy, t);
            case HEAD -> head(c, item.id(), color, cx, cy, t);
            case TRAIL -> trail(c, item.id(), color, x, cy, w, t);
            case KILL_EFFECT -> burst(c, item.id(), color, cx, cy, Math.min(w, h) * 0.42f, t);
            case MACE -> mace(c, item.id(), color, cx, cy, Math.min(w, h) * 0.42f, t);
            case PET -> pet(c, item.id(), color, cx, cy, t);
            case EMOTE -> label(c, item.name(), color, cx, cy, 1.5f);
            case TAG -> label(c, Cosmetics.glyph(kind, item.id()), color, cx, cy, 2f);
            case BADGE -> badge(c, item.id(), color, cx, cy);
            default -> {
            }
        }
    }

    private static void label(DrawContext c, String s, int color, int cx, int cy, float scale) {
        if (s == null || s.isEmpty()) {
            return;
        }
        var tr = MinecraftClient.getInstance().textRenderer;
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(cx, cy);
        c.getMatrices().scale(scale, scale);
        c.drawText(tr, Text.literal(s), -tr.getWidth(s) / 2, -4, color | 0xFF000000, true);
        c.getMatrices().popMatrix();
    }

    private static void badge(DrawContext c, String id, int color, int cx, int cy) {
        UiDraw.roundRect(c, cx - 16, cy - 12, 32, 24, 8, alpha(color, 0.28f));
        UiDraw.roundBorder(c, cx - 16, cy - 12, 32, 24, 8, alpha(color, 0.8f));
        label(c, Cosmetics.glyph(Cosmetics.Kind.BADGE, id), color, cx, cy, 1.5f);
    }

    private static void cape(DrawContext c, Cosmetics.Item item, int col, int cx, int y, int w, int h, float t) {
        var tex = dev.aero.client.cosmetic.CapeTextures.get(item.id());
        if (tex != null) {
            int scale = Math.max(1, Math.min(h - 6, 128) / 16);
            int cw = 10 * scale;
            int ch = 16 * scale;
            c.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, tex.id(), cx - cw / 2, y + (h - ch) / 2,
                    1f, 1f, cw, ch, 10, 16, tex.w(), tex.h());
            return;
        }
        int cw = Math.max(18, w / 3);
        int ch = Math.min(h - 20, cw * 8 / 5 + 4);
        int top = y + (h - ch) / 2;
        int dark = CubeDraw.shade(col, 0.55f);
        int sway = (int) (Math.sin(t * 1.7) * 2);
        for (int i = 0; i < ch; i += 2) {
            float f = i / (float) ch;
            int rowCol = CubeDraw.mix(col, dark, f);
            int off = (int) (sway * f);
            c.fill(cx - cw / 2 + off, top + i, cx + cw / 2 + off, top + i + 2, rowCol);
        }
        int trim = CubeDraw.shade(col, 1.4f);
        c.fill(cx - cw / 2 - 1, top - 2, cx + cw / 2 + 1, top + 2, trim);
        c.fill(cx - cw / 2 - 1, top, cx - cw / 2, top + ch, alpha(trim, 0.7f));
        c.fill(cx + cw / 2, top, cx + cw / 2 + 1, top + ch, alpha(trim, 0.7f));
        for (int k = 1; k < 4; k++) {
            c.fill(cx - cw / 2 + k * cw / 4, top + 4, cx - cw / 2 + k * cw / 4 + 1, top + ch - 2, alpha(0xFF000000, 0.18f));
        }
    }

    /** Line of pixels from (x0,y0) to (x1,y1). */
    private static void line(DrawContext c, float x0, float y0, float x1, float y1, int th, int col) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        for (float k = 0; k <= len; k += 1f) {
            float f = len == 0 ? 0 : k / len;
            px(c, x0 + (x1 - x0) * f - th / 2f, y0 + (y1 - y0) * f - th / 2f, th, col);
        }
    }

    /** Filled ellipse, darker toward the rim. */
    private static void ellipse(DrawContext c, float cx, float cy, float rx, float ry, int col, int edge) {
        for (int y = (int) -ry; y <= ry; y++) {
            float hw = rx * (float) Math.sqrt(Math.max(0, 1 - (y / ry) * (y / ry)));
            float f = Math.abs(y) / ry;
            c.fill(Math.round(cx - hw), Math.round(cy + y), Math.round(cx + hw), Math.round(cy + y + 1), CubeDraw.mix(col, edge, f * f * 0.8f));
        }
    }

    private static final float[][] NEON = {{0, 2}, {5, 8}, {12, 11}, {19, 10}, {21, 6}, {17, 3}, {20, -1}, {15, -3},
            {16, -7}, {10, -6}, {6, -8}, {3, -4}, {0, -2}};

    private static void wings(DrawContext c, String id, int col, int cx, int cy, float t) {
        float flap = (float) Math.sin(t * 2.4) * 5f;
        int ox = cx;
        int oy = cy + 6;
        for (int s = -1; s <= 1; s += 2) {
            switch (id) {
                case "dragon", "bat" -> {
                    float k = "bat".equals(id) ? 0.8f : 1.1f;
                    float ex = ox + s * 6 * k;
                    float ey = oy - 8 * k + flap * 0.4f;
                    int bone = CubeDraw.shade(col, 0.5f);
                    for (int i = 0; i <= 16; i++) {
                        float a = (float) Math.toRadians(25 + i * 8);
                        float len = (15 - 3 * (float) Math.abs(Math.sin(i * Math.PI / 4))) * k;
                        line(c, ex, ey, ex + s * (float) Math.sin(a) * len, ey - (float) Math.cos(a) * len, 2, i % 8 < 4 ? col : CubeDraw.shade(col, 0.88f));
                    }
                    for (int i = 0; i <= 4; i++) {
                        float a = (float) Math.toRadians(25 + i * 32);
                        line(c, ex, ey, ex + s * (float) Math.sin(a) * 16 * k, ey - (float) Math.cos(a) * 16 * k, 1, bone);
                    }
                    line(c, ox + s * 2, oy, ex, ey, 2, bone);
                }
                case "butterfly" -> {
                    float open = 0.7f + 0.3f * (float) Math.sin(t * 5.2);
                    int edge = CubeDraw.shade(col, 0.35f);
                    ellipse(c, ox + s * 12 * open, oy - 8, 10 * open, 9, col, edge);
                    ellipse(c, ox + s * 9 * open, oy + 6, 6 * open, 7, CubeDraw.shade(col, 0.85f), edge);
                    px(c, ox + s * 17 * open - 1, oy - 12, 3, 0xFFFFFFFF);
                }
                case "mech" -> {
                    float[][] p = {{50, 17, 4}, {82, 20, 4}, {112, 15, 3}};
                    for (int i = 0; i < p.length; i++) {
                        ray(c, ox + s * 3 - 1, oy, s * (p[i][0] + flap * 0.3f), p[i][1], (int) p[i][2], i % 2 == 0 ? 0xFF4A505C : 0xFF626A78);
                        ray(c, ox + s * 3, oy + 1, s * (p[i][0] + flap * 0.3f), p[i][1] - 3, 1, col);
                    }
                    c.fill(ox - 4, oy + 4, ox - 1, oy + 9, 0xFF3A3F4A);
                    c.fill(ox + 1, oy + 4, ox + 4, oy + 9, 0xFF3A3F4A);
                    int fl = 3 + (int) (Math.abs(Math.sin(t * 17)) * 3);
                    c.fill(ox - 3, oy + 9, ox - 2, oy + 9 + fl, 0xFFFFB040);
                    c.fill(ox + 2, oy + 9, ox + 3, oy + 9 + fl, 0xFFFFB040);
                }
                case "crystal" -> {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.toRadians(18 + i * 23);
                        float d = 12 + (i % 2) * 4;
                        float bx = ox + s * (float) Math.sin(a) * d;
                        float by = oy - 4 - (float) Math.cos(a) * d + (float) Math.sin(t * 2 + i) * 1.2f;
                        int cc = CubeDraw.mix(col, 0xFFFFFFFF, 0.15f + 0.25f * (0.5f + 0.5f * (float) Math.sin(t * 2.2 + i)));
                        for (int r = -4; r <= 4; r++) {
                            int hw = (4 - Math.abs(r)) / 2;
                            c.fill(Math.round(bx) - hw, Math.round(by) + r, Math.round(bx) + hw + 1, Math.round(by) + r + 1, cc);
                        }
                    }
                }
                case "phoenix" -> {
                    for (int i = 0; i < 7; i++) {
                        float a = (float) Math.toRadians(10 + i * 14 + flap);
                        float len = 21 - i * 1.8f + (float) Math.sin(t * 9 + i * 1.9f) * 1.5f;
                        float sx = s * (float) Math.sin(a);
                        float sy = -(float) Math.cos(a);
                        line(c, ox + s * 3, oy, ox + s * 3 + sx * len * 0.62f, oy + sy * len * 0.62f, 3, CubeDraw.mix(0xFFD8321E, col, 0.45f));
                        line(c, ox + s * 3 + sx * len * 0.58f, oy + sy * len * 0.58f, ox + s * 3 + sx * len, oy + sy * len, 2, 0xFFFFC84A);
                    }
                }
                case "seraph" -> {
                    float[][] fans = {{-4, 32, 4, 12, -5}, {58, 98, 5, 15, 0}, {118, 160, 4, 11, 5}};
                    for (float[] f : fans) {
                        for (int i = 0; i < f[2]; i++) {
                            float a = (float) Math.toRadians(f[0] + (f[1] - f[0]) * i / (f[2] - 1) + flap * 0.5f);
                            float bx = ox + s * 3;
                            float by = oy + f[4];
                            float ex = bx + s * (float) Math.sin(a) * f[3];
                            float ey = by - (float) Math.cos(a) * f[3];
                            line(c, bx, by, ex, ey, 2, col);
                            px(c, ex - 1, ey - 1, 2, 0xFFFFD86B);
                        }
                    }
                }
                case "neon" -> {
                    for (int i = 0; i < NEON.length - 1; i++) {
                        int hue = java.awt.Color.HSBtoRGB((float) ((t * 0.25 + i * 0.06) % 1.0), 0.75f, 1f);
                        line(c, ox + s * (2 + NEON[i][0] * 1.1f), oy - NEON[i][1] * 1.1f,
                                ox + s * (2 + NEON[i + 1][0] * 1.1f), oy - NEON[i + 1][1] * 1.1f, 1, CubeDraw.mix(col, hue, 0.55f));
                    }
                }
                case "phantom" -> {
                    for (int k = 0; k < 5; k++) {
                        float x = ox + s * 3;
                        float y = oy - k;
                        float a = 35 + k * 19 + flap;
                        float[] seg = {7, 6, 5};
                        for (int j = 0; j < 3; j++) {
                            a += (float) Math.sin(t * 3 + k * 0.8f + j * 1.2f) * 9f * j;
                            float r = (float) Math.toRadians(a);
                            float nx = x + s * (float) Math.sin(r) * seg[j];
                            float ny = y - (float) Math.cos(r) * seg[j];
                            line(c, x, y, nx, ny, 3 - j, CubeDraw.shade(col, 0.95f - j * 0.17f));
                            x = nx;
                            y = ny;
                        }
                        px(c, x - 1, y - 1, 2, 0xFF7FE8FF);
                    }
                }
                case "fairy" -> {
                    ring(c, ox + s * 13, oy - 6, 8, 12, col, s);
                    ring(c, ox + s * 10, oy + 8, 5, 8, CubeDraw.shade(col, 0.85f), s);
                }
                case "aurora" -> {
                    for (int i = 0; i < 7; i++) {
                        int cc = CubeDraw.mix(0xFF4FE8D8, 0xFFB060FF, ((float) Math.sin(t * 1.4 + i * 0.6) + 1f) / 2f);
                        ray(c, ox + s * 3, oy, s * (12 + i * 13 + flap), 20 - i * 1.6f, 2, cc);
                    }
                }
                case "aegis" -> {
                    for (int i = 0; i < 4; i++) {
                        float th = s * (24 + i * 20 + flap * 0.5f);
                        ray(c, ox + s * 3, oy, th, 19 - i * 2.4f, 4, CubeDraw.shade(col, 1f - i * 0.08f));
                        ray(c, ox + s * 3, oy, th, 19 - i * 2.4f, 1, 0xFFE8B84A);
                    }
                }
                default -> {
                    for (int i = 0; i < 6; i++) {
                        ray(c, ox + s * 3, oy, s * (12 + i * 14 + flap), 21 - i * 2.4f, 3, CubeDraw.shade(col, i % 2 == 0 ? 1f : 0.85f));
                    }
                }
            }
        }
        c.fill(cx - 3, oy - 3, cx + 3, oy + 8, alpha(0xFF000000, 0.35f));
    }

    private static void ring(DrawContext c, int cx, int cy, int rx, int ry, int col, int side) {
        for (int i = 0; i < 20; i++) {
            double a = i * Math.PI * 2 / 20;
            px(c, cx + (float) Math.cos(a) * rx, cy + (float) Math.sin(a) * ry, 2, col);
        }
        c.fill(cx - 1, cy - 1, cx + 1, cy + 1, alpha(col, 0.5f));
    }

    /** Filled triangle (cone seen from the side): apex at (cx, top), `half` wide at the base, shaded top to base. */
    private static void tri(DrawContext c, float cx, int top, float half, int h, int colTop, int colBase) {
        for (int r = 0; r < h; r++) {
            float f = (r + 1) / (float) h;
            c.fill(Math.round(cx - half * f), top + r, Math.round(cx + half * f) + 1, top + r + 1, CubeDraw.mix(colTop, colBase, f));
        }
    }

    private static void head(DrawContext c, String id, int col, int cx, int cy, float t) {
        switch (id) {
            case "kasa" -> {
                tri(c, cx, cy - 7, 18, 12, CubeDraw.shade(col, 1.12f), CubeDraw.shade(col, 0.9f));
                c.fill(cx - 18, cy + 5, cx + 19, cy + 7, CubeDraw.shade(col, 0.6f));
                for (int s = -1; s <= 1; s += 2) {
                    c.fill(cx + s * 14, cy + 7, cx + s * 14 + 1, cy + 12, 0xFF7FC8E8);
                    c.fill(cx + s * 14 - 1, cy + 12, cx + s * 14 + 2, cy + 15, 0xFF4FA0D0);
                }
            }
            case "tophat" -> {
                c.fill(cx - 8, cy - 11, cx + 8, cy + 6, col);
                c.fill(cx - 8, cy + 1, cx + 8, cy + 5, 0xFFB0303A);
                c.fill(cx - 14, cy + 6, cx + 14, cy + 9, col);
            }
            case "wizard" -> {
                tri(c, cx + 2, cy - 14, 9, 20, CubeDraw.shade(col, 1.25f), col);
                c.fill(cx - 15, cy + 6, cx + 15, cy + 9, CubeDraw.shade(col, 0.8f));
                c.fill(cx - 8, cy + 3, cx + 9, cy + 6, 0xFFE8B84A);
                px(c, cx + 1, cy - 4, 2, 0xFFFFE08A);
                px(c, cx - 3, cy + 0, 2, 0xFFFFE08A);
            }
            case "party" -> {
                tri(c, cx, cy - 11, 8, 20, CubeDraw.shade(col, 1.35f), col);
                c.fill(cx - 9, cy + 9, cx + 10, cy + 11, 0xFFFFE08A);
                c.fill(cx - 2, cy - 14, cx + 2, cy - 10, 0xFFFFFFFF);
            }
            case "santa" -> {
                for (int r = 0; r < 16; r++) { // cone leaning to the right
                    float f = (r + 1) / 16f;
                    float mid = cx + (1 - f) * 9;
                    c.fill(Math.round(mid - 10 * f), cy - 10 + r, Math.round(mid + 10 * f), cy - 9 + r, CubeDraw.mix(CubeDraw.shade(col, 1.15f), col, f));
                }
                c.fill(cx - 12, cy + 6, cx + 12, cy + 11, 0xFFF6F3FB);
                c.fill(cx + 8, cy - 13, cx + 12, cy - 9, 0xFFFFFFFF);
            }
            case "cowboy" -> {
                c.fill(cx - 8, cy - 7, cx + 8, cy + 4, col);
                c.fill(cx - 5, cy - 9, cx + 5, cy - 7, CubeDraw.shade(col, 0.85f));
                c.fill(cx - 8, cy + 1, cx + 8, cy + 3, CubeDraw.shade(col, 0.6f));
                c.fill(cx - 13, cy + 4, cx + 13, cy + 7, col);
                for (int s = -1; s <= 1; s += 2) {
                    c.fill(cx + s * 15 - 2, cy + 2, cx + s * 15 + 2, cy + 5, col);
                    c.fill(cx + s * 17 - 1, cy, cx + s * 17 + 1, cy + 3, col);
                }
            }
            case "beanie" -> {
                ellipse(c, cx, cy + 1, 11, 9, col, CubeDraw.shade(col, 0.85f));
                c.fill(cx - 12, cy + 5, cx + 12, cy + 11, CubeDraw.shade(col, 0.75f));
                c.fill(cx - 2, cy - 12, cx + 2, cy - 8, 0xFFF6F3FB);
            }
            case "cap" -> {
                for (int y = -9; y <= 0; y++) { // dome: upper half of an ellipse
                    int hw = Math.round(11 * (float) Math.sqrt(1 - (y / 9f) * (y / 9f)));
                    c.fill(cx - 3 - hw, cy + 3 + y, cx - 3 + hw, cy + 4 + y, col);
                }
                c.fill(cx - 14, cy + 3, cx + 8, cy + 5, CubeDraw.shade(col, 0.85f));
                c.fill(cx + 6, cy + 3, cx + 17, cy + 6, CubeDraw.shade(col, 0.7f));
                c.fill(cx - 5, cy - 3, cx - 1, cy + 1, 0xFFF6F3FB);
            }
            case "headphones" -> {
                for (int i = 0; i <= 20; i++) {
                    double a = Math.PI + i * Math.PI / 20;
                    px(c, cx + (float) Math.cos(a) * 12 - 1, cy + 2 + (float) Math.sin(a) * 12, 2, 0xFF2A2D36);
                }
                for (int s = -1; s <= 1; s += 2) {
                    c.fill(cx + s * 12 - 3, cy + 1, cx + s * 12 + 3, cy + 12, 0xFF2A2D36);
                    c.fill(cx + s * 14 - 1, cy + 4, cx + s * 14 + 1, cy + 9, CubeDraw.shade(col, 0.85f + 0.3f * (float) Math.sin(t * 3)));
                }
            }
            case "flower" -> {
                int[] petals = {0xFFFF9BC8, 0xFFFFE08A, 0xFFF6F3FB, 0xFFC8A8FF};
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI * 2 / 12;
                    float x = cx + (float) Math.cos(a) * 13 - 2;
                    float y = cy + 2 + (float) Math.sin(a) * 5 - 2;
                    px(c, x, y, i % 2 == 0 ? 5 : 3, i % 2 == 0 ? petals[(i / 2) % 4] : 0xFF4CB86A);
                }
            }
            case "bunny" -> {
                for (int s = -1; s <= 1; s += 2) {
                    ellipse(c, cx + s * 6, cy - 1, 3.5f, 12, col, CubeDraw.shade(col, 0.8f));
                    ellipse(c, cx + s * 6, cy, 1.5f, 8, 0xFFF4A0B8, 0xFFF4A0B8);
                }
            }
            case "shades" -> {
                for (int s = -1; s <= 1; s += 2) {
                    c.fill(cx + s * 8 - 6, cy - 3, cx + s * 8 + 6, cy + 5, col);
                    c.fill(cx + s * 8 - 4, cy - 2, cx + s * 8 - 1, cy - 1, 0xFFF6F3FB);
                }
                c.fill(cx - 2, cy - 2, cx + 2, cy, col);
            }
            case "halo" -> {
                int y = cy - 2 + (int) (Math.sin(t * 2) * 2);
                for (int i = 0; i < 24; i++) {
                    double a = i * Math.PI * 2 / 24 + t * 1.2;
                    px(c, cx + (float) Math.cos(a) * 16, y + (float) Math.sin(a) * 5, 2, CubeDraw.shade(col, 0.85f + 0.3f * (float) ((Math.sin(a) + 1) / 2)));
                }
            }
            case "horns" -> {
                for (int s = -1; s <= 1; s += 2) {
                    ray(c, cx + s * 8, cy + 8, s * 20, 12, 3, col);
                    ray(c, cx + s * 12, cy - 2, s * 40, 8, 2, CubeDraw.shade(col, 1.4f));
                }
                c.fill(cx - 9, cy + 8, cx + 9, cy + 16, alpha(0xFF000000, 0.3f));
            }
            case "crown" -> {
                c.fill(cx - 14, cy + 2, cx + 14, cy + 10, col);
                for (int i = 0; i < 4; i++) {
                    int sx = cx - 14 + i * 9;
                    float g = 1.1f + 0.3f * (float) Math.sin(t * 3 + i);
                    c.fill(sx, cy - 8, sx + 5, cy + 3, CubeDraw.shade(col, g));
                }
                c.fill(cx - 2, cy + 4, cx + 2, cy + 8, 0xFFE0405A);
            }
            case "cat" -> {
                for (int s = -1; s <= 1; s += 2) {
                    for (int r = 0; r < 12; r++) {
                        int half = (12 - r) / 2;
                        c.fill(cx + s * 10 - half, cy - 8 + r, cx + s * 10 + half + 1, cy - 7 + r, col);
                    }
                    for (int r = 0; r < 6; r++) {
                        int half = (6 - r) / 2;
                        c.fill(cx + s * 10 - half, cy - 4 + r, cx + s * 10 + half + 1, cy - 3 + r, 0xFFF4A0B8);
                    }
                }
                c.fill(cx - 12, cy + 8, cx + 12, cy + 16, alpha(0xFF000000, 0.3f));
            }
            default -> {
            }
        }
    }

    static void trail(DrawContext c, String id, int col, int x, int cy, int w, float t) {
        switch (id) {
            case "aura" -> {
                int mx = x + w / 2;
                for (int i = 0; i < 14; i++) {
                    double a = t * 3.2 - i * 0.22;
                    float f = 1f - i / 14f;
                    px(c, mx + (float) Math.cos(a) * 16 - 1, cy + 3 + (float) Math.sin(a) * 5 - 1, 3, alpha(col, f));
                    px(c, mx - (float) Math.cos(a) * 16 - 1, cy + 3 - (float) Math.sin(a) * 5 - 1, 3, alpha(CubeDraw.shade(col, 1.3f), f));
                }
                return;
            }
            case "rings" -> {
                int mx = x + w / 2;
                for (int ringNo = 0; ringNo < 2; ringNo++) {
                    float f = (t * 0.8f + ringNo * 0.5f) % 1f;
                    for (int i = 0; i < 20; i++) {
                        double a = i * Math.PI * 2 / 20;
                        px(c, mx + (float) Math.cos(a) * (4 + f * 16) - 1, cy + 4 + (float) Math.sin(a) * (1.5f + f * 5) - 1, 2, alpha(col, 1f - f));
                    }
                }
                return;
            }
            case "steps" -> {
                for (int k = 0; k < 6; k++) {
                    float f = ((k + t * 1.2f) % 6) / 6f;
                    int fx = Math.round(x + w - 14 - f * (w - 28));
                    int fy = cy + (k % 2 == 0 ? -4 : 2);
                    c.fill(fx, fy, fx + 5, fy + 3, alpha(col, 1f - f));
                }
                return;
            }
            case "stars" -> {
                for (int k = 0; k < 7; k++) {
                    float fx = x + 10 + ((k * 37) % Math.max(1, w - 20));
                    float fy = cy - 10 + (k * 13) % 20;
                    int r = 1 + Math.round(1.5f * (0.5f + 0.5f * (float) Math.sin(t * 6 + k * 2.1f)));
                    int cc = k % 3 == 0 ? 0xFFFFFFFF : col;
                    c.fill(Math.round(fx) - r, Math.round(fy), Math.round(fx) + r + 1, Math.round(fy) + 1, cc);
                    c.fill(Math.round(fx), Math.round(fy) - r, Math.round(fx) + 1, Math.round(fy) + r + 1, cc);
                }
                return;
            }
            case "sakura" -> {
                for (int k = 0; k < 8; k++) {
                    float f = ((t * 0.35f + k * 0.125f) % 1f);
                    float fx = x + 12 + ((k * 29) % Math.max(1, w - 24)) + (float) Math.sin(t * 2.5 + k) * 4;
                    float fy = cy - 14 + f * 28;
                    int cc = alpha(k % 2 == 0 ? col : CubeDraw.mix(col, 0xFFFFFFFF, 0.45f), 1f - f * 0.7f);
                    c.fill(Math.round(fx), Math.round(fy), Math.round(fx) + 3, Math.round(fy) + 2, cc);
                }
                return;
            }
            default -> {
            }
        }
        int n = 26;
        for (int i = 0; i < n; i++) {
            float f = i / (float) n;
            float px = x + w - 10 - f * (w - 22);
            float wave = (float) Math.sin(f * 9 + t * 4) * (2 + f * 5);
            float a = 1f - f;
            int size = a > 0.5f ? 3 : 2;
            int cc = alpha(i % 3 == 0 ? CubeDraw.shade(col, 1.3f) : col, a);
            switch (id) {
                case "heart" -> px(c, px, cy + wave - f * 8, size, cc);
                case "snow" -> px(c, px, cy - 4 + f * 10 + wave * 0.5f, size, cc);
                case "magma" -> px(c, px, cy + f * 9 + wave * 0.4f, size + 1, alpha(CubeDraw.shade(col, 0.7f + (i % 4) * 0.2f), a));
                case "spirit" -> px(c, px, cy + wave * 1.6f - f * 6, size + 1, alpha(col, a * 0.8f));
                case "plasma" -> px(c, px, cy + wave * 1.4f, size, cc);
                case "rainbow" -> {
                    for (int b = 0; b < 4; b++) {
                        px(c, px, cy - 4 + b * 2, 2, alpha(java.awt.Color.HSBtoRGB((f + b * 0.13f + t * 0.2f) % 1f, 0.7f, 1f), a));
                    }
                }
                case "helix" -> {
                    float h = (float) Math.sin(f * 14 + t * 4) * 5;
                    px(c, px, cy + h, size, cc);
                    px(c, px, cy - h, size, alpha(CubeDraw.mix(col, 0xFFFFFFFF, 0.45f), a));
                }
                default -> px(c, px, cy + wave * 0.8f, size, cc);
            }
        }
    }

    static void burst(DrawContext c, String id, int col, int cx, int cy, float r, float t) {
        float ph = (t * 0.8f) % 1f;
        int n = 16;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + (i % 2) * 0.2;
            float d = r * (0.25f + ph * 0.85f) * (0.7f + (i % 3) * 0.2f);
            float a2 = 1f - ph;
            float ex = (float) Math.cos(a) * d;
            float ey = (float) Math.sin(a) * d;
            if ("lightning".equals(id)) {
                ex *= 0.25f;
                ey = (i - n / 2f) * r * 0.13f;
            } else if ("void".equals(id)) {
                d = r * (1.1f - ph);
                ex = (float) Math.cos(a) * d;
                ey = (float) Math.sin(a) * d;
            } else if ("soul".equals(id)) {
                ey -= ph * r * 0.9f;
            }
            px(c, cx + ex, cy + ey, ph < 0.6f ? 3 : 2, alpha(i % 3 == 0 ? CubeDraw.shade(col, 1.3f) : col, a2));
        }
    }

    static void mace(DrawContext c, String id, int col, int cx, int cy, float r, float t) {
        float ph = (t * 0.7f) % 1f;
        int n = 26;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            float d = r * (0.3f + ph * 1.0f);
            float ex = (float) Math.cos(a) * d;
            float ey = (float) Math.sin(a) * d * 0.32f;
            if ("quake".equals(id) || "crater".equals(id)) {
                ey = -(float) Math.abs(Math.sin(a * 3)) * ph * r * 0.9f;
            } else if ("thunder".equals(id)) {
                ex = (float) Math.sin(i * 1.7) * 3;
                ey = (i - n / 2f) * r * 0.1f;
            }
            px(c, cx + ex, cy + 6 + ey, 2, alpha(i % 3 == 0 ? CubeDraw.shade(col, 1.3f) : col, 1f - ph));
        }
    }

    private static void pet(DrawContext c, String id, int col, int cx, int cy, float t) {
        int bob = (int) (Math.sin(t * 3) * 1.5);
        switch (id) {
            case "bee" -> {
                int y = cy - 4 + bob;
                c.fill(cx - 8, y - 6, cx + 8, y + 6, col);
                c.fill(cx - 3, y - 6, cx, y + 6, 0xFF1E1A12);
                c.fill(cx + 4, y - 6, cx + 7, y + 6, 0xFF1E1A12);
                c.fill(cx - 10, y - 2, cx - 8, y + 2, 0xFF1E1A12);
                c.fill(cx - 12, y - 1, cx - 10, y + 1, 0xFF1E1A12);
                int fl = (int) (Math.sin(t * 30) * 3);
                c.fill(cx - 4, y - 12 - fl, cx + 3, y - 6, 0xFFE8F4FF);
                c.fill(cx - 8, y - 10 + fl, cx - 3, y - 6, 0xFFCFE4F8);
            }
            case "fox" -> {
                int y = cy + 2 + bob;
                c.fill(cx - 12, y - 6, cx + 6, y + 6, col);
                c.fill(cx + 4, y - 12, cx + 14, y - 2, col);
                c.fill(cx + 12, y - 8, cx + 17, y - 3, 0xFFF4EEE4);
                c.fill(cx + 4, y - 15, cx + 7, y - 11, col);
                c.fill(cx + 10, y - 15, cx + 13, y - 11, col);
                c.fill(cx - 22, y - 10, cx - 12, y - 2, col);
                c.fill(cx - 24, y - 12, cx - 20, y - 8, 0xFFF4EEE4);
                c.fill(cx - 9, y + 6, cx - 6, y + 12, 0xFF3A2418);
                c.fill(cx + 2, y + 6, cx + 5, y + 12, 0xFF3A2418);
            }
            default -> {
                int y = cy + bob;
                int gill = CubeDraw.shade(col, 0.62f);
                c.fill(cx - 12, y - 4, cx + 6, y + 6, col);
                c.fill(cx + 4, y - 8, cx + 16, y + 4, col);
                c.fill(cx + 9, y - 4, cx + 11, y - 2, 0xFF161018);
                for (int j = 0; j < 3; j++) {
                    c.fill(cx + 15, y - 8 + j * 4, cx + 20, y - 6 + j * 4, gill);
                    c.fill(cx + 1, y - 8 + j * 4, cx + 5, y - 6 + j * 4, gill);
                }
                c.fill(cx - 20, y - 2, cx - 12, y + 4, gill);
                c.fill(cx - 8, y + 6, cx - 4, y + 10, gill);
                c.fill(cx + 1, y + 6, cx + 5, y + 10, gill);
            }
        }
    }
}
