package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Title screen / out-of-world menu backgrounds picked with the Sky button: Polar (aurora night),
 * Glacier (bright ice), Dusk (sunset hills) and Mono (soft greyscale). Drawn procedurally with plain
 * fills, so they cost next to nothing and need no image files. Drawn at screen resolution (not in
 * blocky GUI pixels), so ridges, aurora and suns stay smooth at any GUI scale.
 */
public final class TitleSky {
    public static final String[] NAMES = {"Polar", "Glacier", "Dusk", "Mono", "Panorama"};

    /** Physical pixels per GUI pixel while drawing. */
    private static float d = 1f;
    /** Column width in physical pixels for the animated aurora and light sweep, kept to a few hundred columns. */
    private static int step = 2;

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
        d = Math.max(1f, (float) net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaleFactor());
        int pw = Math.round(w * d);
        int ph = Math.round(h * d);
        step = Math.max(2, pw / 360); // aurora and light-sweep columns: ~360 at most
        c.getMatrices().pushMatrix();
        c.getMatrices().scale(1f / d, 1f / d);
        switch (current()) {
            case "Glacier" -> glacier(c, pw, ph, t);
            case "Dusk" -> dusk(c, pw, ph, t);
            case "Mono" -> mono(c, pw, ph, t);
            default -> polar(c, pw, ph, t);
        }
        c.getMatrices().popMatrix();
    }

    /** A size given in GUI pixels, in physical pixels. */
    private static int px(double guiPixels) {
        return (int) Math.round(guiPixels * d);
    }

    private static double noise(double x, int seed) {
        return Math.sin(x * 0.011 + seed) * 0.55 + Math.sin(x * 0.027 + seed * 1.7) * 0.3 + Math.sin(x * 0.071 + seed * 2.3) * 0.15;
    }

    // Ridges never move, so each sky's ridges are painted once (smooth, anti-aliased) into a texture at half
    // screen resolution and drawn as one quad. Drawn as hundreds of thin columns they were the most expensive
    // thing on the title screen: every GUI rectangle is overlap-tested against all earlier ones.
    private static final net.minecraft.util.Identifier RIDGES = net.minecraft.util.Identifier.of("aero", "title_sky_ridges");
    private static final net.minecraft.util.Identifier DISC = net.minecraft.util.Identifier.of("aero", "title_sky_disc");
    private static String ridgesKey = "";
    private static boolean discReady;

    /** Ridge spec: {base % of height, height divisor for amplitude, noise seed, top color, bottom color}. */
    private static void ridges(DrawContext c, int w, int h, String style, int[][] specs) {
        int bw = Math.max(1, w / 2);
        int bh = Math.max(1, h / 2);
        String key = style + bw + "x" + bh + "@" + d;
        if (!key.equals(ridgesKey)) {
            ridgesKey = key;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(bw, bh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            float sx = (float) w / bw;
            for (int[] s : specs) {
                int base = bh * s[0] / 100;
                int amp = bh / s[1];
                java.awt.geom.Path2D p = new java.awt.geom.Path2D.Float();
                p.moveTo(0, bh);
                for (int x = 0; x <= bw; x++) {
                    p.lineTo(x, base - amp * (0.5 + 0.5 * noise(x * sx / d, s[2])));
                }
                p.lineTo(bw, bh);
                p.closePath();
                g.setPaint(new java.awt.GradientPaint(0, base - amp, new java.awt.Color(s[3], true), 0, bh, new java.awt.Color(s[4], true)));
                g.fill(p);
            }
            g.dispose();
            upload(RIDGES, img);
        }
        c.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, RIDGES, 0, 0, 0f, 0f, w, h, bw, bh, bw, bh);
    }

    private static void upload(net.minecraft.util.Identifier id, java.awt.image.BufferedImage img) {
        int iw = img.getWidth();
        int ih = img.getHeight();
        var ni = new net.minecraft.client.texture.NativeImage(iw, ih, false);
        for (int y = 0; y < ih; y++) {
            for (int x = 0; x < iw; x++) {
                ni.setColorArgb(x, y, img.getRGB(x, y));
            }
        }
        var tex = new net.minecraft.client.texture.NativeImageBackedTexture(() -> "aero " + id.getPath(), ni) {
            {
                sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().get(com.mojang.blaze3d.textures.FilterMode.LINEAR);
            }
        };
        net.minecraft.client.MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
    }

    private static void stars(DrawContext c, int w, int h, float t, int count, int alphaMax) {
        java.util.Random r = new java.util.Random(42);
        for (int i = 0; i < count; i++) {
            int x = r.nextInt(Math.max(1, w));
            int y = r.nextInt(Math.max(1, h));
            float tw = 0.55f + 0.45f * (float) Math.sin(t * (0.6 + r.nextFloat()) + i);
            int a = (int) (alphaMax * tw);
            int s = Math.max(1, px(r.nextFloat() < 0.12f ? 1.4 : 0.7));
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
            for (int x = 0; x < w; x += step) {
                double k = x / (double) Math.max(1, w);
                int yc = (int) (h * (0.22 + b * 0.1) + Math.sin(k * 5.0 + phase) * h * 0.06 + Math.sin(k * 11.0 - phase * 1.3) * h * 0.02);
                int len = (int) (h * (0.16 + 0.08 * Math.sin(k * 7.0 + phase * 2)));
                float glow = (float) (0.55 + 0.45 * Math.sin(k * 9.0 + phase * 3 + b));
                int rgb = lerp(bands[b][0], bands[b][1], (float) (0.5 + 0.5 * Math.sin(k * 3 + phase)));
                int a = (int) (0x58 * glow);
                c.fillGradient(x, yc - len, x + step, yc, rgb & 0xFFFFFF, (a << 24) | rgb);
                c.fillGradient(x, yc, x + step, yc + len / 3, (a << 24) | rgb, rgb & 0xFFFFFF);
            }
        }
        ridges(c, w, h, "Polar", new int[][]{{82, 5, 3, 0xFF2A3A5C, 0xFF1A2440}, {92, 8, 11, 0xFFDCE6F5, 0xFF9FB2CF}});
    }

    private static void glacier(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h * 2 / 3, 0xFF7FB8E6, 0xFFE3F1FB);
        c.fillGradient(0, h * 2 / 3, w, h, 0xFFE3F1FB, 0xFFF6FBFE);
        // soft sun
        int sx = w * 3 / 4;
        int sy = h / 4;
        for (int r = 64; r > 0; r -= 8) {
            int a = (int) (0x14 + (64 - r) * 0.9);
            disc(c, sx, sy, px(r), (a << 24) | 0xFFFBEA);
        }
        // drifting haze
        for (int i = 0; i < 4; i++) {
            int y = h / 6 + i * h / 12;
            int x = (int) ((t * (6 + i * 3) * d + px(i * 170)) % (w + px(300))) - px(300);
            UiDraw.roundRect(c, x, y, px(260), px(18), px(9), 0x22FFFFFF);
        }
        ridges(c, w, h, "Glacier", new int[][]{{72, 4, 5, 0xFFB9D6EE, 0xFF8DB7DB}, {84, 6, 13, 0xFFEAF4FB, 0xFFC9E0F2}, {95, 12, 21, 0xFFFFFFFF, 0xFFE4EFF8}});
    }

    private static void dusk(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h / 2, 0xFF241A45, 0xFFB5476A);
        c.fillGradient(0, h / 2, w, h, 0xFFB5476A, 0xFFF4A259);
        stars(c, w, h / 3, t, 60, 0x90);
        int sx = w / 2;
        int sy = h * 64 / 100;
        for (int r = 72; r > 38; r -= 8) {
            disc(c, sx, sy, px(r), ((0x12 + (72 - r)) << 24) | 0xFFD58A);
        }
        disc(c, sx, sy, px(36), 0xFFFFE0A8);
        ridges(c, w, h, "Dusk", new int[][]{{74, 6, 7, 0xFF6B2E55, 0xFF4A1F45}, {86, 7, 17, 0xFF3A1838, 0xFF2A1030}, {96, 12, 29, 0xFF1D0B22, 0xFF14071A}});
    }

    private static void mono(DrawContext c, int w, int h, float t) {
        c.fillGradient(0, 0, w, h, 0xFF2A2B30, 0xFF121316);
        for (int i = 0; i < 5; i++) {
            int x = (int) ((w * (0.15 + i * 0.18)) + Math.sin(t * 0.2 + i) * px(30));
            int y = (int) ((h * (0.3 + (i % 2) * 0.35)) + Math.cos(t * 0.17 + i) * px(20));
            disc(c, x, y, px(50 + i * 14), 0x0CFFFFFF);
        }
        // diagonal light sweep
        for (int x = -h; x < w; x += step) {
            double k = (x + h) / (double) (w + h);
            int a = (int) (0x10 * Math.max(0, Math.sin(k * Math.PI * 2 + t * 0.15)));
            if (a > 0) {
                c.fill(x + h / 2, 0, x + h / 2 + step, h, (a << 24) | 0xFFFFFF);
            }
        }
        c.fillGradient(0, h * 3 / 4, w, h, 0x00000000, 0x55000000);
    }

    /** Filled circle: one tinted quad of a smooth white disc texture. */
    private static void disc(DrawContext c, int cx, int cy, int r, int color) {
        if (!discReady) {
            discReady = true;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(256, 256, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(java.awt.Color.WHITE);
            g.fill(new java.awt.geom.Ellipse2D.Float(1, 1, 254, 254));
            g.dispose();
            upload(DISC, img);
        }
        c.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, DISC, cx - r, cy - r, 0f, 0f, r * 2, r * 2, 256, 256, 256, 256, color);
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
