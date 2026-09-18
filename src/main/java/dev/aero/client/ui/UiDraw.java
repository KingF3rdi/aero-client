package dev.aero.client.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * Frosted-glass UI with coverage-antialiased rounded corners (no stair-step pixels).
 */
public final class UiDraw {
    public static final int BORDER = 0x38FFFFFF;
    public static final int BORDER_SOFT = 0x18FFFFFF;
    public static final int ACCENT = 0xFF4F8EFF;
    public static final int ACCENT_DIM = 0x552E5A8A;
    public static final int FILL = 0xA812101A;
    public static final int FILL_DEEP = 0xA00A0910;
    public static final int FILL_LIFT = 0xB01A1826;

    private static final int MAX_R = 28;
    /** Per-radius top-left coverage, 0–255, row-major r*r. */
    private static final byte[][] FILL_COV = new byte[MAX_R + 1][];
    private static final byte[][] EDGE_COV = new byte[MAX_R + 1][];

    static {
        for (int r = 1; r <= MAX_R; r++) {
            FILL_COV[r] = new byte[r * r];
            EDGE_COV[r] = new byte[r * r];
            float cr = r - 0.45f;
            for (int iy = 0; iy < r; iy++) {
                for (int ix = 0; ix < r; ix++) {
                    float dx = r - (ix + 0.5f);
                    float dy = r - (iy + 0.5f);
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float fill = clamp01(cr - dist + 0.75f);
                    float edge = clamp01(1.15f - Math.abs(dist - cr));
                    FILL_COV[r][iy * r + ix] = (byte) Math.round(fill * 255f);
                    EDGE_COV[r][iy * r + ix] = (byte) Math.round(edge * 255f);
                }
            }
        }
    }

    private UiDraw() {}

    public static boolean menuOpen;

    public static boolean lite() {
        return true;
    }

    private static float clamp01(float v) {
        if (v <= 0f) {
            return 0f;
        }
        if (v >= 1f) {
            return 1f;
        }
        return v;
    }

    private static int withCoverage(int color, int cov) {
        if (cov <= 0) {
            return 0;
        }
        if (cov >= 255) {
            return color;
        }
        int a = ((color >>> 24) & 0xFF) * cov / 255;
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * A rounded corner is mostly fully-opaque interior with only a 1-2px anti-aliased ring at the
     * actual curve, but the original version called context.fill() once per pixel regardless -
     * for a single radius-20 corner that's up to 400 draw calls, times 4 corners, times every
     * rounded element the ClickGUI draws (rows, pills, toggles, sliders, panels) every frame,
     * which is what made the whole menu feel slow. This walks each row and merges consecutive
     * fully-opaque pixels into one fill() call, only falling back to per-pixel fills for the
     * actual blended edge - same output, far fewer draw calls.
     */
    private static void corner(DrawContext c, int x, int y, int r, int color, byte[] cov,
                               boolean flipX, boolean flipY) {
        if (r <= 0 || cov == null) {
            return;
        }
        for (int iy = 0; iy < r; iy++) {
            int row = iy * r;
            int ix = 0;
            while (ix < r) {
                int rawCov = cov[row + ix] & 0xFF;
                if (rawCov == 0) {
                    ix++;
                    continue;
                }
                int py = flipY ? y + r - 1 - iy : y + iy;
                if (rawCov == 255) {
                    int runStart = ix;
                    do {
                        ix++;
                    } while (ix < r && (cov[row + ix] & 0xFF) == 255);
                    int runLen = ix - runStart;
                    int px0 = flipX ? x + r - runStart - runLen : x + runStart;
                    c.fill(px0, py, px0 + runLen, py + 1, color);
                    continue;
                }
                int packed = withCoverage(color, rawCov);
                int px = flipX ? x + r - 1 - ix : x + ix;
                c.fill(px, py, px + 1, py + 1, packed);
                ix++;
            }
        }
    }

    public static void roundRect(DrawContext c, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(MAX_R, Math.min(w, h) / 2)));
        if (r <= 1) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x + r, y, x + w - r, y + h, color);
        c.fill(x, y + r, x + r, y + h - r, color);
        c.fill(x + w - r, y + r, x + w, y + h - r, color);
        byte[] cov = FILL_COV[r];
        corner(c, x, y, r, color, cov, false, false);
        corner(c, x + w - r, y, r, color, cov, true, false);
        corner(c, x, y + h - r, r, color, cov, false, true);
        corner(c, x + w - r, y + h - r, r, color, cov, true, true);
    }

    public static void roundBorder(DrawContext c, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(MAX_R, Math.min(w, h) / 2)));
        if (r <= 1) {
            c.fill(x, y, x + w, y + 1, color);
            c.fill(x, y + h - 1, x + w, y + h, color);
            c.fill(x, y, x + 1, y + h, color);
            c.fill(x + w - 1, y, x + w, y + h, color);
            return;
        }
        c.fill(x + r, y, x + w - r, y + 1, color);
        c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        byte[] cov = EDGE_COV[r];
        corner(c, x, y, r, color, cov, false, false);
        corner(c, x + w - r, y, r, color, cov, true, false);
        corner(c, x, y + h - r, r, color, cov, false, true);
        corner(c, x + w - r, y + h - r, r, color, cov, true, true);
    }

    public static void shadow(DrawContext c, int x, int y, int w, int h) {
    }

    public static void sheen(DrawContext c, int x, int y, int w, int h, int radius) {
        if (h < 8 || w < 8) {
            return;
        }
        int inset = Math.max(2, Math.min(radius / 2, 8));
        c.fillGradient(x + inset, y + 1, x + w - inset, y + Math.min(22, h / 3), 0x28FFFFFF, 0x00FFFFFF);
    }

    public static void frame(DrawContext c, int x, int y, int w, int h, int color) {
        roundBorder(c, x, y, w, h, 10, color);
    }

    public static void raised(DrawContext c, int x, int y, int w, int h, int fill) {
        raised(c, x, y, w, h, fill, Math.min(10, Math.min(w, h) / 2));
    }

    public static void raised(DrawContext c, int x, int y, int w, int h, int fill, int radius) {
        roundRect(c, x, y, w, h, radius, fill);
        roundBorder(c, x, y, w, h, radius, BORDER_SOFT);
    }

    public static void inset(DrawContext c, int x, int y, int w, int h, int fill) {
        inset(c, x, y, w, h, fill, Math.min(10, Math.min(w, h) / 2));
    }

    public static void inset(DrawContext c, int x, int y, int w, int h, int fill, int radius) {
        roundRect(c, x, y, w, h, radius, fill);
        roundBorder(c, x, y, w, h, radius, 0x22000000);
    }

    public static void glass(DrawContext c, int x, int y, int w, int h, int fill) {
        glass(c, x, y, w, h, fill, 18);
    }

    public static void glass(DrawContext c, int x, int y, int w, int h, int fill, int radius) {
        int r = Math.max(12, radius);
        // Soft accent glow just outside the panel, then a see-through body so the world shows through.
        roundRect(c, x - 2, y - 2, w + 4, h + 4, r + 2, 0x0C4F8EFF);
        int alpha = Math.min(0xA8, (fill >>> 24));
        roundRect(c, x, y, w, h, r, (alpha << 24) | (fill & 0xFFFFFF));
        int in = (int) Math.ceil(r * 0.4);
        if (w > in * 2 + 4 && h > in * 2 + 4) {
            // Frost: bright at the top fading out, faint blue pooling at the bottom.
            int mid = y + h / 2;
            c.fillGradient(x + in, y + 1, x + w - in, mid, 0x2CFFFFFF, 0x06FFFFFF);
            c.fillGradient(x + in, mid, x + w - in, y + h - 1, 0x06FFFFFF, 0x184F8EFF);
        }
        roundBorder(c, x, y, w, h, r, 0x46FFFFFF);
        if (w > 20) {
            roundRect(c, x + 12, y + 1, w - 24, 2, 1, 0x55FFFFFF);
        }
    }

    public static void innerCard(DrawContext c, int x, int y, int w, int h) {
        roundRect(c, x, y, w, h, 14, 0x5E0E0E18);
        if (w > 20 && h > 20) {
            c.fillGradient(x + 6, y + 1, x + w - 6, y + Math.min(h / 2, 30), 0x16FFFFFF, 0x00FFFFFF);
        }
        roundBorder(c, x, y, w, h, 14, 0x26FFFFFF);
    }

    public static void field(DrawContext c, int x, int y, int w, int h, boolean focused) {
        int r = Math.min(h / 2, 10);
        roundRect(c, x, y, w, h, r, focused ? 0xE0181622 : 0x9912111A);
        roundBorder(c, x, y, w, h, r, focused ? 0x664F8EFF : 0x22FFFFFF);
    }

    public static void scrollbar(DrawContext c, int x, int y, int h, int scroll, int content, int view) {
        if (content <= view || h < 12) {
            return;
        }
        int track = Math.max(12, h - 8);
        int thumb = Math.max(18, (int) (track * (view / (float) content)));
        int max = Math.max(1, content - view);
        int ty = y + 4 + (int) ((track - thumb) * (scroll / (float) max));
        roundRect(c, x, y + 4, 4, track, 2, 0x22FFFFFF);
        roundRect(c, x, ty, 4, thumb, 2, 0x884F8EFF);
    }

    public static void card(DrawContext c, int x, int y, int w, int h, int fill, boolean accentBar) {
        roundRect(c, x, y, w, h, Math.min(12, Math.min(w, h) / 2), fill);
        if (accentBar) {
            roundRect(c, x + 3, y + 5, 3, h - 10, 1, ACCENT);
        }
    }

    public static void pill(DrawContext c, int x, int y, int w, int h, boolean on) {
        roundRect(c, x, y, w, h, h / 2, on ? 0x554F8EFF : 0x2814101C);
        roundBorder(c, x, y, w, h, h / 2, on ? 0x444F8EFF : 0x14FFFFFF);
    }

    public static void toggle(DrawContext c, int x, int y, boolean on) {
        roundRect(c, x, y + 3, 28, 10, 0, on ? ACCENT : 0x66302C3C);
        int knobX = on ? x + 15 : x + 1;
        roundRect(c, knobX, y, 13, 16, 0, on ? 0xFFF8F5FF : 0xD0AAA5B8);
        roundBorder(c, knobX, y, 13, 16, 0, on ? 0x66FFFFFF : 0x22000000);
    }

    public static void slider(DrawContext c, int x, int y, int w, float t) {
        roundRect(c, x, y, w, 6, 3, 0x66221F2E);
        int filled = Math.max(4, (int) (w * Math.max(0f, Math.min(1f, t))));
        roundRect(c, x, y, filled, 6, 3, ACCENT);
        roundRect(c, x + Math.max(0, filled - 6), y - 3, 12, 12, 6, 0xFFF6F1FF);
        roundBorder(c, x + Math.max(0, filled - 6), y - 3, 12, 12, 6, 0x66FFFFFF);
    }

    public static void scan(DrawContext c, int x, int y, int w, int h) {
    }

    public static void vignette(DrawContext c, int w, int h) {
    }

    public static void grid(DrawContext c, int x, int y, int w, int h) {
    }

    public static void divider(DrawContext c, int x, int y, int w) {
        c.fill(x, y, x + w, y + 1, 0x14FFFFFF);
    }

    /** The brand mark: a plain bold blue "A". */
    public static void aeroMark(DrawContext c, int x, int y, int s, int color) {
        int m = Math.max(8, s);
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        float scale = m / 8f;
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(x + (m - 6 * scale) / 2f, y + (m - 8 * scale) / 2f);
        c.getMatrices().scale(scale, scale);
        c.drawText(tr, net.minecraft.text.Text.literal("A").setStyle(net.minecraft.text.Style.EMPTY.withBold(true)
                .withColor(color & 0xFFFFFF)), 0, 0, color | 0xFF000000, false);
        c.getMatrices().popMatrix();
    }
}
