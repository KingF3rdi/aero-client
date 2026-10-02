package dev.aero.client.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * Dark glass UI: black see-through panels with a bright white rim, white text, coverage-antialiased
 * rounded corners (no stair-step pixels).
 */
public final class UiDraw {
    public static final int BORDER = 0x40FFFFFF;
    public static final int BORDER_SOFT = 0x24FFFFFF;
    /** White text on the dark glass. */
    public static final int TEXT = 0xFFF2F3F5;
    public static final int MUTED = 0xFF9AA0AB;
    /** Secondary text that still reads as content (inactive rows). */
    public static final int SOFT = 0xFFC9CDD4;
    /** Row / button hover wash on dark surfaces. */
    public static final int HOVER = 0x16FFFFFF;
    /** Hovered card or button. */
    public static final int HOVER_FILL = 0x26FFFFFF;
    /** Idle card or button lifted a little off the dark glass. */
    public static final int LIFT = 0x12FFFFFF;
    /** Rim of a selected pill / tab, as in the reference design. */
    public static final int SELECT_RIM = 0xB3FFFFFF;
    public static final int SURFACE = 0x5A0A0B0F;

    /** Multiplies the alpha of every UiDraw fill (fading cards in/out); 1 = normal. */
    public static float fade = 1f;

    /** color with the current fade applied, for text drawn next to faded UiDraw shapes. */
    public static int fa(int color) {
        if (fade >= 1f) {
            return color;
        }
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0f, fade));
        return (Math.max(a, fade > 0.02f ? 5 : 0) << 24) | (color & 0xFFFFFF);
    }
    public static int withAlpha(int rgb, int a) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Menu background opacity (Interface Color), 0..1. */
    private static float glassOpacity() {
        var c = dev.aero.client.AeroClient.CONFIG;
        return c == null ? 0.3f : Math.max(0.1f, Math.min(1f, c.menuGlass / 100f));
    }

    /**
     * Idle background of an inner panel or card: dark glass whose darkness follows the Menu background
     * opacity. The rgb argument is kept for the callers but the surface is always the dark glass tone.
     */
    public static int surface(int rgb) {
        return withAlpha(0x0A0B0F, Math.round(255 * Math.min(0.85f, 0.18f + glassOpacity())));
    }

    public static int accent() {
        var c = dev.aero.client.AeroClient.CONFIG;
        return c == null ? 0xFF4F8EFF : (c.uiAccent | 0xFF000000);
    }
    public static final int FILL = 0x18FFFFFF;
    public static final int FILL_DEEP = 0x40000000;
    public static final int FILL_LIFT = 0x22FFFFFF;

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

    /**
     * All corner shapes in one small texture: per radius r a 2r x 2r block (four mirrored quadrants),
     * fill coverage in the top half, edge coverage in the bottom half. A corner is then ONE textured
     * quad. Drawing it as per-pixel fill() calls meant thousands of GUI elements a frame, and since
     * 1.21.6 every added element is intersection-tested against the ones before it (GuiRenderState),
     * which took the menu from ~700 to ~11 FPS.
     */
    private static final net.minecraft.util.Identifier ATLAS = net.minecraft.util.Identifier.of("aero", "ui_corners");
    private static final int ATLAS_W = MAX_R * (MAX_R + 1);
    private static final int ATLAS_H = MAX_R * 4;
    private static int atlasState; // 0 not built yet, 1 ready, -1 failed (per-pixel fallback)

    private static boolean atlas() {
        if (atlasState == 0) {
            atlasState = -1;
            try {
                var img = new net.minecraft.client.texture.NativeImage(ATLAS_W, ATLAS_H, true);
                for (int r = 1; r <= MAX_R; r++) {
                    int ox = r * (r - 1);
                    for (int py = 0; py < 2 * r; py++) {
                        int iy = py < r ? py : 2 * r - 1 - py;
                        for (int px = 0; px < 2 * r; px++) {
                            int ix = px < r ? px : 2 * r - 1 - px;
                            img.setColorArgb(ox + px, py, ((FILL_COV[r][iy * r + ix] & 0xFF) << 24) | 0xFFFFFF);
                            img.setColorArgb(ox + px, 2 * MAX_R + py, ((EDGE_COV[r][iy * r + ix] & 0xFF) << 24) | 0xFFFFFF);
                        }
                    }
                }
                net.minecraft.client.MinecraftClient.getInstance().getTextureManager()
                        .registerTexture(ATLAS, new net.minecraft.client.texture.NativeImageBackedTexture(() -> "aero ui corners", img));
                atlasState = 1;
            } catch (Throwable ignored) {
            }
        }
        return atlasState == 1;
    }

    private static float[] cornerUv(int r, boolean edge) {
        float ox = r * (r - 1);
        float oy = edge ? 2 * MAX_R : 0;
        return new float[]{ox / ATLAS_W, oy / ATLAS_H, (ox + 2 * r) / ATLAS_W, (oy + 2 * r) / ATLAS_H};
    }

    /** One GUI element for the whole shape (see RoundRectState); false = atlas missing, draw it piece by piece. */
    private static boolean single(DrawContext c, int x, int y, int w, int h, int r, int color, int color2, boolean border) {
        if (!atlas()) {
            return false;
        }
        var tex = net.minecraft.client.MinecraftClient.getInstance().getTextureManager().getTexture(ATLAS);
        float au = (MAX_R * (MAX_R - 1) + MAX_R - 0.5f) / ATLAS_W;
        float av = (MAX_R - 0.5f) / ATLAS_H;
        c.state.addSimpleElement(RoundRectState.of(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                net.minecraft.client.texture.TextureSetup.of(tex.getGlTextureView(), tex.getSampler()),
                new org.joml.Matrix3x2f(c.getMatrices()), x, y, w, h, r, color, color2, border, au, av,
                border ? EDGE_UV[r] : FILL_UV[r], c.scissorStack.peekLast()));
        return true;
    }

    private static final float[][] FILL_UV = new float[MAX_R + 1][];
    private static final float[][] EDGE_UV = new float[MAX_R + 1][];

    static {
        for (int r = 1; r <= MAX_R; r++) {
            FILL_UV[r] = cornerUv(r, false);
            EDGE_UV[r] = cornerUv(r, true);
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
        if (atlas()) {
            int u = r * (r - 1) + (flipX ? r : 0);
            int v = (cov == EDGE_COV[r] ? 2 * MAX_R : 0) + (flipY ? r : 0);
            c.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, ATLAS, x, y, u, v, r, r, ATLAS_W, ATLAS_H, color);
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
        color = fa(color);
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(MAX_R, Math.min(w, h) / 2)));
        if (r <= 1) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        if (single(c, x, y, w, h, r, color, color, false)) {
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
        color = fa(color);
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
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
        if (single(c, x, y, w, h, r, color, color, true)) {
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

    /** Rounded rect (or its outline) fading from top to bottom in one element; flat top color without the atlas. */
    public static void roundGradient(DrawContext c, int x, int y, int w, int h, int radius, int top, int bottom, boolean border) {
        top = fa(top);
        bottom = fa(bottom);
        int r = Math.max(0, Math.min(radius, Math.min(MAX_R, Math.min(w, h) / 2)));
        if (w <= 0 || h <= 0 || r <= 1 || !single(c, x, y, w, h, r, top, bottom, border)) {
            if (border) {
                roundBorder(c, x, y, w, h, radius, top);
            } else {
                roundRect(c, x, y, w, h, radius, top);
            }
        }
    }

    /**
     * Frosted glass card: a light white tint over the blurred world (lighter at the top), a crisp white rim and
     * a faint shadow. lit 0..1 (hover, eased by the caller) brightens fill and rim.
     */
    public static void frost(DrawContext c, int x, int y, int w, int h, int r, float lit, boolean active) {
        float l = active ? 1f : Math.max(0f, Math.min(1f, lit));
        softShadow(c, x, y, w, h, r, Math.round(0x14 + 0x10 * l), 6);
        float o = glassOpacity();
        if (o > 0f) {
            roundRect(c, x, y, w, h, r, withAlpha(0x000000, Math.round(255 * o * 0.45f))); // readability over bright worlds
        }
        roundGradient(c, x, y, w, h, r, withAlpha(0xFFFFFF, Math.round(0x1E + 0x26 * l)),
                withAlpha(0xFFFFFF, Math.round(0x0A + 0x1A * l)), false);
        roundBorder(c, x, y, w, h, r, withAlpha(0xFFFFFF, Math.round(0x70 + 0x80 * l)));
        roundBorder(c, x + 1, y + 1, w - 2, h - 2, r - 1, withAlpha(0xFFFFFF, Math.round(0x12 + 0x12 * l)));
    }

    /** Blurred-looking drop shadow from 1px rings outside the shape, deeper below than above. */
    public static void softShadow(DrawContext c, int x, int y, int w, int h, int r, int strength, int spread) {
        for (int g = 1; g <= spread; g++) {
            float f = 1f - (g - 1f) / spread;
            int a = Math.round(strength * f * f);
            if (a > 0) {
                int up = g - g / 3;
                roundBorder(c, x - g, y - up, w + g * 2, h + up + g + g / 2, r + g, a << 24);
            }
        }
    }

    public static void shadow(DrawContext c, int x, int y, int w, int h) {
        shadow(c, x, y, w, h, 14);
    }

    /** Soft drop shadow under a light panel: a few widening low-alpha layers, offset down. */
    public static void shadow(DrawContext c, int x, int y, int w, int h, int radius) {
        // Rings outside the panel only: panels are see-through, so filled (and offset) shadow layers showed
        // through the body as extra rounded corners just inside the edge.
        for (int g = 1; g <= 6; g++) {
            roundBorder(c, x - g, y - g, w + g * 2, h + g * 2, radius + g, (16 - g * 2) << 24);
        }
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
        roundBorder(c, x, y, w, h, radius, 0x33FFFFFF);
    }

    public static void glass(DrawContext c, int x, int y, int w, int h, int fill) {
        glass(c, x, y, w, h, fill, 18);
    }

    public static void glass(DrawContext c, int x, int y, int w, int h, int fill, int radius) {
        frost(c, x, y, w, h, Math.max(12, radius), 0f, false);
    }

    public static void innerCard(DrawContext c, int x, int y, int w, int h) {
        roundRect(c, x, y, w, h, 14, surface(0xFFFFFF));
        roundBorder(c, x, y, w, h, 14, 0x40FFFFFF);
    }

    public static void field(DrawContext c, int x, int y, int w, int h, boolean focused) {
        int r = Math.min(h / 2, 10);
        roundRect(c, x, y, w, h, r, focused ? 0x55000000 : 0x33000000);
        roundBorder(c, x, y, w, h, r, focused ? 0xCCFFFFFF : 0x40FFFFFF);
    }

    public static void scrollbar(DrawContext c, int x, int y, int h, int scroll, int content, int view) {
        if (content <= view || h < 12) {
            return;
        }
        int track = Math.max(12, h - 8);
        int thumb = Math.max(18, (int) (track * (view / (float) content)));
        int max = Math.max(1, content - view);
        int ty = y + 4 + (int) ((track - thumb) * (scroll / (float) max));
        roundRect(c, x, y + 4, 4, track, 2, 0x18FFFFFF);
        roundRect(c, x, ty, 4, thumb, 2, withAlpha(accent(), 0x88));
    }

    public static void card(DrawContext c, int x, int y, int w, int h, int fill, boolean accentBar) {
        roundRect(c, x, y, w, h, Math.min(12, Math.min(w, h) / 2), fill);
        if (accentBar) {
            roundRect(c, x + 3, y + 5, 3, h - 10, 1, accent());
        }
    }

    public static void pill(DrawContext c, int x, int y, int w, int h, boolean on) {
        // Selected = light fill with a white rim; idle = a faint chip on the dark glass.
        roundRect(c, x, y, w, h, h / 2, on ? 0x2EFFFFFF : 0x0FFFFFFF);
        roundBorder(c, x, y, w, h, h / 2, on ? SELECT_RIM : 0x1FFFFFFF);
    }

    public static void toggle(DrawContext c, int x, int y, boolean on) {
        roundRect(c, x, y + 1, 28, 14, 7, on ? accent() : 0x33FFFFFF);
        roundBorder(c, x, y + 1, 28, 14, 7, on ? 0x66FFFFFF : 0x40FFFFFF);
        int knobX = on ? x + 15 : x + 2;
        roundRect(c, knobX, y + 3, 10, 10, 5, on ? 0xFFFFFFFF : 0xFF9AA0AB);
    }

    public static void slider(DrawContext c, int x, int y, int w, float t) {
        roundRect(c, x, y, w, 6, 3, 0x33FFFFFF);
        int filled = Math.max(4, (int) (w * Math.max(0f, Math.min(1f, t))));
        roundRect(c, x, y, filled, 6, 3, accent());
        roundRect(c, x + Math.max(0, filled - 6), y - 3, 12, 12, 6, 0xFFFFFFFF);
        roundBorder(c, x + Math.max(0, filled - 6), y - 3, 12, 12, 6, 0x40000000);
    }

    public static void scan(DrawContext c, int x, int y, int w, int h) {
    }

    public static void vignette(DrawContext c, int w, int h) {
    }

    public static void grid(DrawContext c, int x, int y, int w, int h) {
    }

    public static void divider(DrawContext c, int x, int y, int w) {
        c.fill(x, y, x + w, y + 1, fa(0x1FFFFFFF));
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
