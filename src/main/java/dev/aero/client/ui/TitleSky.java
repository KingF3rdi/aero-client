package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Title screen / out-of-world menu backgrounds picked with the Sky button: Polar (aurora night),
 * Glacier (bright ice), Dusk (sunset hills) and Mono (soft greyscale). Drawn procedurally with plain
 * fills, so they cost next to nothing and need no image files.
 */
public final class TitleSky {
    public static final String[] NAMES = {"Polar", "Glacier", "Dusk", "Mono", "Panorama"};

    private TitleSky() {}

    public static String current() {
        var c = AeroClient.CONFIG;
        String s = c == null || c.titleSky == null ? "Polar" : c.titleSky;
        for (String n : NAMES) {
            if (n.equalsIgnoreCase(s)) {
                return n;
            }
        }
        return "Polar";
    }

    public static boolean active() {
        return !"Panorama".equals(current());
    }

    public static void cycle() {
        var c = AeroClient.CONFIG;
        if (c == null) {
            return;
        }
        String cur = current();
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(cur)) {
                c.titleSky = NAMES[(i + 1) % NAMES.length];
                break;
            }
        }
        c.save();
    }

    public static void draw(DrawContext c, int w, int h) {
        float t = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        switch (current()) {
            case "Glacier" -> glacier(c, w, h, t);
            case "Dusk" -> dusk(c, w, h, t);
            case "Mono" -> mono(c, w, h, t);
            default -> polar(c, w, h, t);
        }
    }

    private static double noise(double x, int seed) {
        return Math.sin(x * 0.011 + seed) * 0.55 + Math.sin(x * 0.027 + seed * 1.7) * 0.3 + Math.sin(x * 0.071 + seed * 2.3) * 0.15;
    }

    /** Filled ridge line: for each 2px column the ground starts at base - amp * noise. */
    private static void ridge(DrawContext c, int w, int h, int base, int amp, int seed, int top, int bottom) {
        for (int x = 0; x < w; x += 2) {
            int y = (int) (base - amp * (0.5 + 0.5 * noise(x, seed)));
            c.fillGradient(x, y, x + 2, h, top, bottom);
        }
    }

    private static void stars(DrawContext c, int w, int h, float t, int count, int alphaMax) {
        java.util.Random r = new java.util.Random(42);
        for (int i = 0; i < count; i++) {
            int x = r.nextInt(Math.max(1, w));
            int y = r.nextInt(Math.max(1, h));
            float tw = 0.55f + 0.45f * (float) Math.sin(t * (0.6 + r.nextFloat()) + i);
            int a = (int) (alphaMax * tw);
            int s = r.nextFloat() < 0.12f ? 2 : 1;
            c.fill(x, y, x + s, y + s, (a << 24) | 0xFFFFFF);
        }
    }

    private static void polar(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h, 0xFF070B1C, 0xFF16233F);
        stars(c, w, h * 2 / 3, t, 140, 0xD0);
        // Aurora: two drifting ribbons, each column a vertical gradient that fades at both ends.
        int[][] bands = {{0x2BE8A5, 0x3FA9F5}, {0x7C5CFF, 0x2BE8A5}};
        for (int b = 0; b < bands.length; b++) {
            double phase = t * (0.18 + b * 0.07);
            for (int x = 0; x < w; x += 2) {
                double k = x / (double) Math.max(1, w);
                int yc = (int) (h * (0.22 + b * 0.1) + Math.sin(k * 5.0 + phase) * h * 0.06 + Math.sin(k * 11.0 - phase * 1.3) * h * 0.02);
                int len = (int) (h * (0.16 + 0.08 * Math.sin(k * 7.0 + phase * 2)));
                float glow = (float) (0.55 + 0.45 * Math.sin(k * 9.0 + phase * 3 + b));
                int rgb = lerp(bands[b][0], bands[b][1], (float) (0.5 + 0.5 * Math.sin(k * 3 + phase)));
                int a = (int) (0x58 * glow);
                c.fillGradient(x, yc - len, x + 2, yc, rgb & 0xFFFFFF, (a << 24) | rgb);
                c.fillGradient(x, yc, x + 2, yc + len / 3, (a << 24) | rgb, rgb & 0xFFFFFF);
            }
        }
        ridge(c, w, h, h * 82 / 100, h / 5, 3, 0xFF2A3A5C, 0xFF1A2440);
        ridge(c, w, h, h * 92 / 100, h / 8, 11, 0xFFDCE6F5, 0xFF9FB2CF);
    }

    private static void glacier(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h * 2 / 3, 0xFF7FB8E6, 0xFFE3F1FB);
        c.fillGradient(0, h * 2 / 3, w, h, 0xFFE3F1FB, 0xFFF6FBFE);
        // soft sun
        int sx = w * 3 / 4;
        int sy = h / 4;
        for (int r = 64; r > 0; r -= 8) {
            int a = (int) (0x14 + (64 - r) * 0.9);
            disc(c, sx, sy, r, (a << 24) | 0xFFFBEA);
        }
        // drifting haze
        for (int i = 0; i < 4; i++) {
            int y = h / 6 + i * h / 12;
            int x = (int) ((t * (6 + i * 3) + i * 170) % (w + 300)) - 300;
            UiDraw.roundRect(c, x, y, 260, 18, 9, 0x22FFFFFF);
        }
        ridge(c, w, h, h * 72 / 100, h / 4, 5, 0xFFB9D6EE, 0xFF8DB7DB);
        ridge(c, w, h, h * 84 / 100, h / 6, 13, 0xFFEAF4FB, 0xFFC9E0F2);
        ridge(c, w, h, h * 95 / 100, h / 12, 21, 0xFFFFFFFF, 0xFFE4EFF8);
    }

    private static void dusk(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h / 2, 0xFF241A45, 0xFFB5476A);
        c.fillGradient(0, h / 2, w, h, 0xFFB5476A, 0xFFF4A259);
        stars(c, w, h / 3, t, 60, 0x90);
        int sx = w / 2;
        int sy = h * 64 / 100;
        for (int r = 72; r > 38; r -= 8) {
            disc(c, sx, sy, r, ((0x12 + (72 - r)) << 24) | 0xFFD58A);
        }
        disc(c, sx, sy, 36, 0xFFFFE0A8);
        ridge(c, w, h, h * 74 / 100, h / 6, 7, 0xFF6B2E55, 0xFF4A1F45);
        ridge(c, w, h, h * 86 / 100, h / 7, 17, 0xFF3A1838, 0xFF2A1030);
        ridge(c, w, h, h * 96 / 100, h / 12, 29, 0xFF1D0B22, 0xFF14071A);
    }

    private static void mono(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h, 0xFF2A2B30, 0xFF121316);
        for (int i = 0; i < 5; i++) {
            int x = (int) ((w * (0.15 + i * 0.18)) + Math.sin(t * 0.2 + i) * 30);
            int y = (int) ((h * (0.3 + (i % 2) * 0.35)) + Math.cos(t * 0.17 + i) * 20);
            disc(c, x, y, 50 + i * 14, 0x0CFFFFFF);
        }
        // diagonal light sweep
        for (int x = -h; x < w; x += 2) {
            double k = (x + h) / (double) (w + h);
            int a = (int) (0x10 * Math.max(0, Math.sin(k * Math.PI * 2 + t * 0.15)));
            if (a > 0) {
                c.fill(x + h / 2, 0, x + h / 2 + 2, h, (a << 24) | 0xFFFFFF);
            }
        }
        c.fillGradient(0, h * 3 / 4, w, h, 0x00000000, 0x55000000);
    }

    /** Filled circle from 2px scanlines. */
    private static void disc(DrawContext c, int cx, int cy, int r, int color) {
        for (int dy = -r; dy < r; dy += 2) {
            int half = (int) Math.sqrt(Math.max(0, r * r - (dy + 1) * (dy + 1)));
            c.fill(cx - half, cy + dy, cx + half, cy + dy + 2, color);
        }
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
