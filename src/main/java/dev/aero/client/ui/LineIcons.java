package dev.aero.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Smooth outline icons for the menu. Each one is stroked once with anti-aliasing into a white 64x64
 * texture (linear filtering, so it stays smooth at any size) and tinted when drawn.
 */
public final class LineIcons {
    private static final int S = 64;
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    private LineIcons() {}

    /** Draws icon {@code name} in a size x size box at (x, y), tinted with color (ARGB). */
    public static void draw(DrawContext c, String name, int x, int y, int size, int color) {
        Identifier id = texture(name);
        if (id != null && (color >>> 24) > 0) {
            c.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, size, size, S, S, S, S, color);
        }
    }

    private static Identifier texture(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            try {
                BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                paint(g, n);
                g.dispose();
                NativeImage ni = new NativeImage(S, S, true);
                for (int py = 0; py < S; py++) {
                    for (int px = 0; px < S; px++) {
                        ni.setColorArgb(px, py, img.getRGB(px, py));
                    }
                }
                Identifier id = Identifier.of("aero", "lineicon/" + n);
                NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "aero icon " + n, ni) {
                    {
                        sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                    }
                };
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
                return id;
            } catch (Throwable t) {
                return null;
            }
        });
    }

    private static void paint(Graphics2D g, String name) {
        switch (name) {
            case "pvp" -> { // shield
                Path2D p = new Path2D.Double();
                p.moveTo(32, 7);
                p.lineTo(52, 14);
                p.lineTo(52, 30);
                p.curveTo(52, 44, 43, 53, 32, 57);
                p.curveTo(21, 53, 12, 44, 12, 30);
                p.lineTo(12, 14);
                p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(23, 31, 30, 38));
                g.draw(new Line2D.Double(30, 38, 42, 25));
            }
            case "hud" -> { // screen on a stand
                g.draw(new RoundRectangle2D.Double(9, 11, 46, 32, 10, 10));
                g.draw(new Line2D.Double(32, 43, 32, 51));
                g.draw(new Line2D.Double(21, 53, 43, 53));
            }
            case "visuals" -> { // wand with sparkles
                g.draw(new Line2D.Double(12, 52, 38, 26));
                sparkle(g, 45, 17, 8);
                sparkle(g, 51, 36, 5);
                sparkle(g, 27, 12, 5);
            }
            case "player" -> {
                g.draw(new Ellipse2D.Double(22, 9, 20, 20));
                Path2D p = new Path2D.Double();
                p.moveTo(12, 55);
                p.curveTo(12, 40, 52, 40, 52, 55);
                g.draw(p);
            }
            case "friends" -> {
                g.draw(new Ellipse2D.Double(12, 13, 16, 16));
                g.draw(new Ellipse2D.Double(36, 13, 16, 16));
                Path2D p = new Path2D.Double();
                p.moveTo(5, 52);
                p.curveTo(5, 37, 35, 37, 35, 52);
                p.moveTo(29, 52);
                p.curveTo(29, 37, 59, 37, 59, 52);
                g.draw(p);
            }
            case "misc" -> { // menu lines
                g.draw(new Line2D.Double(12, 18, 52, 18));
                g.draw(new Line2D.Double(12, 32, 52, 32));
                g.draw(new Line2D.Double(12, 46, 52, 46));
            }
            case "performance" -> { // gauge
                g.draw(new Arc2D.Double(9, 13, 46, 46, -20, 220, Arc2D.OPEN));
                g.draw(new Line2D.Double(32, 36, 44, 24));
                g.fill(new Ellipse2D.Double(28, 32, 8, 8));
            }
            case "preview" -> { // eye
                Path2D p = new Path2D.Double();
                p.moveTo(6, 32);
                p.curveTo(18, 14, 46, 14, 58, 32);
                p.curveTo(46, 50, 18, 50, 6, 32);
                p.closePath();
                g.draw(p);
                g.draw(new Ellipse2D.Double(24, 24, 16, 16));
            }
            case "sliders" -> {
                g.draw(new Line2D.Double(10, 18, 54, 18));
                g.draw(new Line2D.Double(10, 32, 54, 32));
                g.draw(new Line2D.Double(10, 46, 54, 46));
                knob(g, 40, 18);
                knob(g, 22, 32);
                knob(g, 34, 46);
            }
            case "search" -> {
                g.draw(new Ellipse2D.Double(10, 10, 32, 32));
                g.draw(new Line2D.Double(38, 38, 53, 53));
            }
            case "wardrobe" -> { // hanger
                Path2D p = new Path2D.Double();
                p.moveTo(26, 16);
                p.curveTo(26, 8, 38, 8, 38, 15);
                p.curveTo(38, 20, 32, 20, 32, 26);
                p.lineTo(7, 46);
                p.lineTo(57, 46);
                p.lineTo(32, 26);
                g.draw(p);
            }
            case "bolt" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(36, 6);
                p.lineTo(14, 36);
                p.lineTo(30, 36);
                p.lineTo(26, 58);
                p.lineTo(50, 26);
                p.lineTo(34, 26);
                p.closePath();
                g.draw(p);
            }
            case "signal" -> {
                g.draw(new Line2D.Double(14, 50, 14, 42));
                g.draw(new Line2D.Double(26, 50, 26, 34));
                g.draw(new Line2D.Double(38, 50, 38, 24));
                g.draw(new Line2D.Double(50, 50, 50, 14));
            }
            case "key" -> { // keyboard
                g.draw(new RoundRectangle2D.Double(6, 16, 52, 32, 10, 10));
                for (int i = 0; i < 4; i++) {
                    g.fill(new Ellipse2D.Double(14 + i * 11, 24, 5, 5));
                }
                g.draw(new Line2D.Double(22, 39, 42, 39));
            }
            case "flask" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(25, 8);
                p.lineTo(39, 8);
                p.moveTo(27, 8);
                p.lineTo(27, 24);
                p.lineTo(12, 50);
                p.curveTo(10, 54, 12, 57, 17, 57);
                p.lineTo(47, 57);
                p.curveTo(52, 57, 54, 54, 52, 50);
                p.lineTo(37, 24);
                p.lineTo(37, 8);
                g.draw(p);
                g.draw(new Line2D.Double(19, 42, 45, 42));
            }
            case "target" -> {
                g.draw(new Ellipse2D.Double(12, 12, 40, 40));
                g.draw(new Line2D.Double(32, 4, 32, 18));
                g.draw(new Line2D.Double(32, 46, 32, 60));
                g.draw(new Line2D.Double(4, 32, 18, 32));
                g.draw(new Line2D.Double(46, 32, 60, 32));
            }
            case "tag" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(8, 10);
                p.lineTo(30, 10);
                p.lineTo(55, 35);
                p.lineTo(35, 55);
                p.lineTo(10, 30);
                p.closePath();
                g.draw(p);
                g.fill(new Ellipse2D.Double(19, 19, 7, 7));
            }
            default -> g.draw(new Ellipse2D.Double(12, 12, 40, 40));
        }
    }

    /** Four-point star. */
    private static void sparkle(Graphics2D g, double cx, double cy, double r) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx, cy - r);
        p.quadTo(cx, cy, cx + r, cy);
        p.quadTo(cx, cy, cx, cy + r);
        p.quadTo(cx, cy, cx - r, cy);
        p.quadTo(cx, cy, cx, cy - r);
        g.fill(p);
    }

    private static void knob(Graphics2D g, double cx, double cy) {
        Graphics2D k = (Graphics2D) g.create();
        k.setComposite(java.awt.AlphaComposite.Clear);
        k.fill(new Ellipse2D.Double(cx - 6, cy - 6, 12, 12));
        k.dispose();
        g.draw(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
    }
}
