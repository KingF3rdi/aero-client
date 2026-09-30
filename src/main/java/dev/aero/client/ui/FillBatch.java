package dev.aero.client.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * Many plain rectangles as ONE GUI element, for pixel icons: collect with fill(), then flush(). A
 * small icon drawn as 10-20 separate DrawContext.fill() calls costs 10-20 GUI elements, and every
 * element is intersection-tested against the ones already added (see RoundRectState).
 */
final class FillBatch {
    private final DrawContext c;
    private int[] d = new int[5 * 20];
    private int n;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;

    FillBatch(DrawContext c) {
        this.c = c;
    }

    void fill(int x1, int y1, int x2, int y2, int color) {
        if ((color >>> 24) == 0 || x1 == x2 || y1 == y2) {
            return;
        }
        if (n + 5 > d.length) {
            d = java.util.Arrays.copyOf(d, d.length * 2);
        }
        d[n++] = Math.min(x1, x2);
        d[n++] = Math.min(y1, y2);
        d[n++] = Math.max(x1, x2);
        d[n++] = Math.max(y1, y2);
        d[n++] = color;
        minX = Math.min(minX, Math.min(x1, x2));
        minY = Math.min(minY, Math.min(y1, y2));
        maxX = Math.max(maxX, Math.max(x1, x2));
        maxY = Math.max(maxY, Math.max(y1, y2));
    }

    void flush() {
        if (n == 0) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(c.getMatrices());
        ScreenRect scissor = c.scissorStack.peekLast();
        ScreenRect rect = new ScreenRect(minX, minY, maxX - minX, maxY - minY).transformEachVertex(pose);
        c.state.addSimpleElement(new State(RenderPipelines.GUI, TextureSetup.empty(), pose, d, n, scissor,
                scissor != null ? scissor.intersection(rect) : rect));
        n = 0;
        d = new int[5 * 20]; // the element keeps the old array until the frame is drawn
    }

    private record State(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose, int[] rects, int count,
                         @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds) implements SimpleGuiElementRenderState {
        @Override
        public void setupVertices(VertexConsumer v) {
            for (int i = 0; i < count; i += 5) {
                int color = rects[i + 4];
                v.vertex(pose, rects[i], rects[i + 1]).color(color);
                v.vertex(pose, rects[i], rects[i + 3]).color(color);
                v.vertex(pose, rects[i + 2], rects[i + 3]).color(color);
                v.vertex(pose, rects[i + 2], rects[i + 1]).color(color);
            }
        }
    }
}
