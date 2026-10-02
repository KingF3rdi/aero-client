package dev.aero.client.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * A whole rounded rectangle (or its 1px outline) as ONE GUI element: the corners come from the
 * UiDraw corner atlas, the straight parts sample a fully opaque texel of it. Fewer elements matter
 * because the GUI intersection-tests every new element against the ones already added.
 */
record RoundRectState(RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2f pose,
                      int x, int y, int w, int h, int r, int color, int color2, boolean border,
                      float au, float av, float[] corner,
                      @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds) implements SimpleGuiElementRenderState {

    /** corner = {u0, v0, u1, v1} of the top-left-oriented 2r x 2r block, already normalized. */
    static RoundRectState of(RenderPipeline pipeline, TextureSetup tex, Matrix3x2f pose, int x, int y, int w, int h, int r,
                             int color, int color2, boolean border, float au, float av, float[] corner, @Nullable ScreenRect scissor) {
        ScreenRect rect = new ScreenRect(x, y, w, h).transformEachVertex(pose);
        return new RoundRectState(pipeline, tex, pose, x, y, w, h, r, color, color2, border, au, av, corner, scissor,
                scissor != null ? scissor.intersection(rect) : rect);
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        int x2 = x + w;
        int y2 = y + h;
        float u0 = corner[0];
        float v0 = corner[1];
        float um = (corner[0] + corner[2]) / 2f;
        float vm = (corner[1] + corner[3]) / 2f;
        float u1 = corner[2];
        float v1 = corner[3];
        quad(v, x, y, x + r, y + r, u0, v0, um, vm);
        quad(v, x2 - r, y, x2, y + r, um, v0, u1, vm);
        quad(v, x, y2 - r, x + r, y2, u0, vm, um, v1);
        quad(v, x2 - r, y2 - r, x2, y2, um, vm, u1, v1);
        if (border) {
            solid(v, x + r, y, x2 - r, y + 1);
            solid(v, x + r, y2 - 1, x2 - r, y2);
            solid(v, x, y + r, x + 1, y2 - r);
            solid(v, x2 - 1, y + r, x2, y2 - r);
        } else {
            solid(v, x + r, y, x2 - r, y2);
            solid(v, x, y + r, x + r, y2 - r);
            solid(v, x2 - r, y + r, x2, y2 - r);
        }
    }

    private void solid(VertexConsumer v, int x1, int y1, int x2, int y2) {
        if (x2 > x1 && y2 > y1) {
            quad(v, x1, y1, x2, y2, au, av, au, av);
        }
    }

    private void quad(VertexConsumer v, int x1, int y1, int x2, int y2, float u1, float v1, float u2, float v2) {
        int c1 = at(y1);
        int c2 = at(y2);
        v.vertex(pose, x1, y1).texture(u1, v1).color(c1);
        v.vertex(pose, x1, y2).texture(u1, v2).color(c2);
        v.vertex(pose, x2, y2).texture(u2, v2).color(c2);
        v.vertex(pose, x2, y1).texture(u2, v1).color(c1);
    }

    /** Vertical gradient: color at the top edge, color2 at the bottom. */
    private int at(int yy) {
        if (color == color2) {
            return color;
        }
        float t = (yy - y) / (float) h;
        int out = 0;
        for (int sh = 0; sh < 32; sh += 8) {
            int a = (color >>> sh) & 0xFF;
            int b = (color2 >>> sh) & 0xFF;
            out |= Math.round(a + (b - a) * t) << sh;
        }
        return out;
    }
}
