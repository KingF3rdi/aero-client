package dev.aero.client.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * Glassy/liquid style: rounded corners everywhere, soft multi-step shadows, a light top-sheen
 * gradient on panels instead of hard 1px bevels, thin translucent borders instead of a solid dark
 * frame. Fast HUD skips the extra layers (shadow/sheen/scan) to keep FPS up.
 */
public final class UiDraw {
    public static final int SHADOW = 0x50000000;
    public static final int SHADOW_SOFT = 0x28000000;
    public static final int SHADOW_WIDE = 0x14000000;
    public static final int BORDER = 0x30FFFFFF;
    public static final int BORDER_SOFT = 0x16FFFFFF;
    public static final int HI = 0x38FFFFFF;
    public static final int HI_SOFT = 0x18FFFFFF;
    public static final int LO = 0x30000000;
    public static final int LO_SOFT = 0x18000000;
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

    /** Core rounded-rectangle fill: a cross of flat fills plus per-row circular corners. */
    public static void roundRect(DrawContext c, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x + r, y, x + w - r, y + h, color);
        c.fill(x, y + r, x + r, y + h - r, color);
        c.fill(x + w - r, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int dx = (int) Math.round(Math.sqrt(Math.max(0, (double) r * r - (double) dy * dy)));
            if (dx <= 0) {
                continue;
            }
            c.fill(x + r - dx, y + i, x + r, y + i + 1, color);
            c.fill(x + w - r, y + i, x + w - r + dx, y + i + 1, color);
            c.fill(x + r - dx, y + h - 1 - i, x + r, y + h - i, color);
            c.fill(x + w - r, y + h - 1 - i, x + w - r + dx, y + h - i, color);
        }
    }

    /** A thin rounded outline, built the same way as roundRect but only the outer ring. */
    public static void roundBorder(DrawContext c, int x, int y, int w, int h, int radius, int color) {
        roundRect(c, x, y, w, 1, radius, color);
        roundRect(c, x, y + h - 1, w, 1, radius, color);
        roundRect(c, x, y, 1, h, radius, color);
        roundRect(c, x + w - 1, y, 1, h, radius, color);
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int dx = (int) Math.round(Math.sqrt(Math.max(0, (double) r * r - (double) dy * dy)));
            if (dx <= 0) {
                continue;
            }
            int px = r - dx;
            c.fill(x + px, y + i, x + px + 1, y + i + 1, color);
            c.fill(x + w - r - 1 + (r - px), y + i, x + w - r + dx, y + i + 1, color);
            c.fill(x + px, y + h - 1 - i, x + px + 1, y + h - i, color);
            c.fill(x + w - r - 1 + (r - px), y + h - 1 - i, x + w - r + dx, y + h - i, color);
        }
    }

    /** Soft multi-step glow shadow, wider and fainter than a hard drop shadow. */
    public static void shadow(DrawContext c, int x, int y, int w, int h) {
        if (lite()) {
            return;
        }
        int r = Math.min(10, Math.min(w, h) / 2);
        roundRect(c, x - 1, y + 4, w + 2, h + 2, r, SHADOW_WIDE);
        roundRect(c, x, y + 3, w, h + 1, r, SHADOW_SOFT);
        roundRect(c, x, y + 2, w, h, r, SHADOW);
    }

    /** Light gradient sheen across the top of a panel, like light catching curved glass. */
    public static void sheen(DrawContext c, int x, int y, int w, int h, int radius) {
        if (lite() || h < 6) {
            return;
        }
        int inset = Math.max(1, radius / 2);
        int sheenH = Math.max(2, h / 2);
        c.fillGradient(x + inset, y + 1, x + w - inset, y + 1 + sheenH, 0x2AFFFFFF, 0x00FFFFFF);
    }

    public static void frame(DrawContext c, int x, int y, int w, int h, int color) {
        roundBorder(c, x, y, w, h, 6, color);
    }

    /** Rounded, glassy raised surface: shadow, translucent fill, top sheen, thin light border. */
    public static void raised(DrawContext c, int x, int y, int w, int h, int fill) {
        raised(c, x, y, w, h, fill, Math.min(8, Math.min(w, h) / 2));
    }

    public static void raised(DrawContext c, int x, int y, int w, int h, int fill, int radius) {
        if (lite()) {
            c.fill(x, y, x + w, y + h, fill);
            return;
        }
        shadow(c, x, y, w, h);
        roundRect(c, x, y, w, h, radius, fill);
        sheen(c, x, y, w, h, radius);
        roundBorder(c, x, y, w, h, radius, BORDER);
    }

    /** Rounded inset (pressed-in) surface for inputs and tracks. */
    public static void inset(DrawContext c, int x, int y, int w, int h, int fill) {
        inset(c, x, y, w, h, fill, Math.min(8, Math.min(w, h) / 2));
    }

    public static void inset(DrawContext c, int x, int y, int w, int h, int fill, int radius) {
        if (lite()) {
            c.fill(x, y, x + w, y + h, fill);
            return;
        }
        roundRect(c, x, y, w, h, radius, fill);
        if (h > 4) {
            c.fillGradient(x + Math.max(1, radius / 2), y + 1, x + w - Math.max(1, radius / 2),
                    y + Math.min(h / 2, 6), 0x2A000000, 0x00000000);
        }
        roundBorder(c, x, y, w, h, radius, BORDER_SOFT);
    }

    /** Frosted rounded panel used by the Aurora-style click GUI. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int fill) {
        glass(c, x, y, w, h, fill, 12);
    }

    public static void glass(DrawContext c, int x, int y, int w, int h, int fill, int radius) {
        roundRect(c, x, y, w, h, radius, fill);
        sheen(c, x, y, w, h, radius);
        roundBorder(c, x, y, w, h, radius, BORDER);
    }

    public static void innerCard(DrawContext c, int x, int y, int w, int h) {
        glass(c, x, y, w, h, 0xC212111C, 10);
    }

    public static void card(DrawContext c, int x, int y, int w, int h, int fill, boolean accentBar) {
        if (lite()) {
            c.fill(x, y, x + w, y + h, fill);
            if (accentBar) {
                c.fill(x, y, x + 3, y + h, ACCENT);
            }
            return;
        }
        int radius = Math.min(10, Math.min(w, h) / 2);
        raised(c, x, y, w, h, fill, radius);
        if (accentBar) {
            roundRect(c, x + 3, y + 3, 3, h - 6, 2, ACCENT);
        }
    }

    public static void pill(DrawContext c, int x, int y, int w, int h, boolean on) {
        int fill = on ? 0xDA342C4A : 0xB01C1A26;
        raised(c, x, y, w, h, fill, h / 2);
        if (on) {
            roundRect(c, x + 2, y + 2, w - 4, Math.max(1, h / 3), (h - 4) / 2, 0x40C4B5FD);
        }
    }

    public static void toggle(DrawContext c, int x, int y, boolean on) {
        int track = on ? 0xFFB9A8EE : 0xFF2A2836;
        inset(c, x, y + 3, 28, 10, track, 5);
        if (on) {
            c.fillGradient(x + 2, y + 4, x + 26, y + 6, 0x50FFFFFF, 0x00FFFFFF);
        }
        int knobX = on ? x + 15 : x + 1;
        raised(c, knobX, y, 13, 16, on ? 0xFFF8F5FF : 0xFFAAA5B8, 7);
    }

    public static void slider(DrawContext c, int x, int y, int w, float t) {
        inset(c, x, y, w, 6, 0xFF221F2E, 3);
        int filled = Math.max(4, (int) (w * Math.max(0f, Math.min(1f, t))));
        roundRect(c, x, y, filled, 6, 3, ACCENT);
        c.fillGradient(x + 1, y + 1, x + Math.max(2, filled - 1), y + 3, 0x66FFFFFF, 0x00FFFFFF);
        raised(c, x + Math.max(0, filled - 6), y - 3, 12, 12, 0xFFF6F1FF, 6);
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
        c.fill(0, 0, w, 10, 0x22000000);
        c.fill(0, h - 12, w, h, 0x28000000);
    }

    public static void grid(DrawContext c, int x, int y, int w, int h) {
        if (lite()) {
            return;
        }
        c.fill(x, y, x + w, y + h, 0x06FFFFFF);
    }

    /** Soft, rounded-friendly divider - a faint gradient line rather than a hard 2-tone edge. */
    public static void divider(DrawContext c, int x, int y, int w) {
        c.fillGradient(x, y, x + w, y + 1, 0x00FFFFFF, 0x1AFFFFFF);
        c.fillGradient(x, y + 1, x + w, y + 2, 0x1AFFFFFF, 0x00FFFFFF);
    }

    private static int mix(int base, int tint, int amount) {
        int a = (base >>> 24) & 0xFF;
        int r = Math.min(255, ((base >>> 16) & 0xFF) + amount);
        int g = Math.min(255, ((base >>> 8) & 0xFF) + amount);
        int b = Math.min(255, (base & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
