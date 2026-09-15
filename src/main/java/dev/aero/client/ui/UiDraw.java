package dev.aero.client.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * Pixel-style depth: drop shadow, 1px bevel, inset/raised faces.
 * Fast HUD skips extra fills (grid/scan/shadows) to keep FPS up.
 */
public final class UiDraw {
    public static final int SHADOW = 0x6A000000;
    public static final int SHADOW_SOFT = 0x33000000;
    public static final int FRAME = 0xFF07060D;
    public static final int HI = 0x38FFFFFF;
    public static final int HI_SOFT = 0x18FFFFFF;
    public static final int LO = 0x5A000000;
    public static final int LO_SOFT = 0x28000000;
    public static final int ACCENT = 0xFFC4B5FD;
    public static final int ACCENT_DIM = 0x55A78BFA;
    public static final int FILL = 0xFF12101A;
    public static final int FILL_DEEP = 0xFF0A0910;
    public static final int FILL_LIFT = 0xFF1A1826;

    private UiDraw() {}

    public static boolean menuOpen;

    public static boolean lite() {
        var c = dev.aero.client.AeroClient.CONFIG;
        return !menuOpen && c != null && c.fastHud;
    }

    /** Frosted rounded panel used by the Aurora-style click GUI. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int fill) {
        int r = 7;
        c.fill(x + r, y, x + w - r, y + h, fill);
        c.fill(x, y + r, x + w, y + h - r, fill);
        c.fill(x + 3, y + 2, x + w - 3, y + h - 2, fill);
        c.fill(x + 2, y + 3, x + w - 2, y + h - 3, fill);
        c.fill(x + 1, y + 4, x + w - 1, y + h - 4, fill);
        c.fill(x + r, y, x + w - r, y + 1, 0x33FFFFFF);
        c.fill(x, y + r, x + 1, y + h - r, 0x22FFFFFF);
        c.fill(x + r, y + h - 1, x + w - r, y + h, 0x33000000);
        c.fill(x + w - 1, y + r, x + w, y + h - r, 0x22000000);
        frame(c, x + 1, y + 1, w - 2, h - 2, 0x18FFFFFF);
    }

    public static void innerCard(DrawContext c, int x, int y, int w, int h) {
        glass(c, x, y, w, h, 0xE812111C);
    }

    public static void shadow(DrawContext c, int x, int y, int w, int h) {
        if (lite()) {
            return;
        }
        c.fill(x + 2, y + 3, x + w + 2, y + h + 3, SHADOW);
        c.fill(x + 1, y + 2, x + w + 1, y + h + 2, SHADOW_SOFT);
    }

    public static void frame(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y, x + 1, y + h, color);
        c.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void pixelCorners(DrawContext c, int x, int y, int w, int h, int bg) {
        c.fill(x, y, x + 1, y + 1, bg);
        c.fill(x + w - 1, y, x + w, y + 1, bg);
        c.fill(x, y + h - 1, x + 1, y + h, bg);
        c.fill(x + w - 1, y + h - 1, x + w, y + h, bg);
    }

    public static void raised(DrawContext c, int x, int y, int w, int h, int fill) {
        if (lite()) {
            c.fill(x, y, x + w, y + h, fill);
            return;
        }
        shadow(c, x, y, w, h);
        c.fill(x, y, x + w, y + h, fill);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, mix(fill, 0xFFFFFF, 18));
        c.fill(x, y, x + w, y + 1, HI);
        c.fill(x, y, x + 1, y + h, HI_SOFT);
        c.fill(x, y + h - 1, x + w, y + h, LO);
        c.fill(x + w - 1, y, x + w, y + h, LO_SOFT);
        frame(c, x, y, w, h, FRAME);
        pixelCorners(c, x, y, w, h, 0xFF0A0910);
    }

    public static void inset(DrawContext c, int x, int y, int w, int h, int fill) {
        if (lite()) {
            c.fill(x, y, x + w, y + h, fill);
            return;
        }
        c.fill(x, y, x + w, y + h, fill);
        c.fill(x, y, x + w, y + 1, 0x66000000);
        c.fill(x, y, x + 1, y + h, 0x44000000);
        c.fill(x, y + h - 1, x + w, y + h, HI_SOFT);
        c.fill(x + w - 1, y, x + w, y + h, HI_SOFT);
        frame(c, x, y, w, h, FRAME);
        c.fill(x + 1, y + 1, x + w - 1, y + 2, 0x22000000);
    }

    public static void card(DrawContext c, int x, int y, int w, int h, int fill, boolean accentBar) {
        if (lite()) {
            c.fill(x, y, x + w, y + h, fill);
            if (accentBar) {
                c.fill(x, y, x + 2, y + h, ACCENT);
            }
            return;
        }
        raised(c, x, y, w, h, fill);
        if (accentBar) {
            c.fill(x, y + 1, x + 2, y + h - 1, ACCENT);
            c.fill(x + 2, y + 1, x + 3, y + h - 1, ACCENT_DIM);
        }
    }

    public static void pill(DrawContext c, int x, int y, int w, int h, boolean on) {
        int fill = on ? 0xFF2E2840 : 0xFF1A1824;
        raised(c, x, y, w, h, fill);
        if (on) {
            c.fill(x + 1, y + 1, x + w - 1, y + 2, 0x55C4B5FD);
        }
    }

    public static void toggle(DrawContext c, int x, int y, boolean on) {
        int track = on ? 0xFFD8D0EC : 0xFF22202C;
        inset(c, x, y + 3, 28, 10, track);
        if (on) {
            c.fill(x + 2, y + 5, x + 26, y + 6, 0x44FFFFFF);
        }
        int knobX = on ? x + 16 : x + 1;
        raised(c, knobX, y, 12, 16, on ? 0xFFF6F2FC : 0xFF9A96A8);
        if (on) {
            c.fill(knobX + 3, y + 4, knobX + 9, y + 5, 0x66C4B5FD);
        }
    }

    public static void slider(DrawContext c, int x, int y, int w, float t) {
        inset(c, x, y, w, 5, 0xFF1A1822);
        int filled = Math.max(2, (int) (w * Math.max(0f, Math.min(1f, t))));
        c.fill(x + 1, y + 1, x + filled, y + 4, ACCENT);
        c.fill(x + 1, y + 1, x + filled, y + 2, 0x66FFFFFF);
        raised(c, x + Math.max(0, filled - 4), y - 3, 6, 11, 0xFFF0EAF8);
    }

    public static void scan(DrawContext c, int x, int y, int w, int h) {
        if (lite() || w < 2 || h < 2) {
            return;
        }
        c.fill(x, y, x + w, y + 1, 0x10FFFFFF);
        c.fill(x, y + h - 1, x + w, y + h, 0x12000000);
    }

    public static void vignette(DrawContext c, int w, int h) {
        if (lite()) {
            return;
        }
        c.fill(0, 0, w, 10, 0x28000000);
        c.fill(0, h - 12, w, h, 0x30000000);
    }

    public static void grid(DrawContext c, int x, int y, int w, int h) {
        if (lite()) {
            return;
        }
        c.fill(x, y, x + w, y + h, 0x06FFFFFF);
    }

    public static void divider(DrawContext c, int x, int y, int w) {
        c.fill(x, y, x + w, y + 1, 0x22000000);
        c.fill(x, y + 1, x + w, y + 2, 0x14FFFFFF);
    }

    private static int mix(int base, int tint, int amount) {
        int a = (base >>> 24) & 0xFF;
        int r = Math.min(255, ((base >>> 16) & 0xFF) + amount);
        int g = Math.min(255, ((base >>> 8) & 0xFF) + amount);
        int b = Math.min(255, (base & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
