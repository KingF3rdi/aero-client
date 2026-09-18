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
        int cw = Math.max(18, w / 3);
        int ch = h - 20;
        int top = y + 8;
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

    private static void wings(DrawContext c, String id, int col, int cx, int cy, float t) {
        float flap = (float) Math.sin(t * 2.4) * 5f;
        int ox = cx;
        int oy = cy + 6;
        for (int s = -1; s <= 1; s += 2) {
            switch (id) {
                case "dragon" -> {
                    for (int i = 0; i < 9; i++) {
                        float th = s * (14 + i * 9.5f + flap);
                        ray(c, ox + s * 3, oy, th, 13 + (float) Math.sin(i * 0.55) * 6, 2, CubeDraw.shade(col, i % 2 == 0 ? 1f : 0.8f));
                    }
                    for (int i = 0; i < 3; i++) {
                        ray(c, ox + s * 3, oy, s * (20 + i * 32 + flap), 22 - i * 2, 2, 0xFF4A2A22);
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

    private static void head(DrawContext c, String id, int col, int cx, int cy, float t) {
        switch (id) {
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
