package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

/**
 * Real 3D cosmetics (cape, wings, headwear, pet) drawn as small flat-colored boxes on the local
 * player's model. Being a normal feature renderer, the same code shows up in third person / F5 and
 * in the wardrobe's live preview, so what you preview is exactly what you get in game. Client-only:
 * other players can't see your cosmetics (there is no shared backend), and first person hides your
 * own model, so use F5 to see them.
 */
public final class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final Identifier WHITE = Identifier.of("minecraft", "textures/block/white_concrete.png");
    private static final int FULLBRIGHT = 0xF000F0;

    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack m, OrderedRenderCommandQueue q, int light, PlayerEntityRenderState state,
                       float yaw, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || state.id != mc.player.getId() || state.invisible) {
            return;
        }
        PlayerEntityModel model = getContextModel();
        RenderLayer layer = RenderLayers.entityCutoutNoCull(WHITE);
        float t = (System.currentTimeMillis() % 100000L) / 1000f;

        Cosmetics.Item cape = item(Cosmetics.Kind.CAPE);
        Cosmetics.Item wings = item(Cosmetics.Kind.WINGS);
        Cosmetics.Item pet = item(Cosmetics.Kind.PET);
        if (cape != null || wings != null || pet != null) {
            m.push();
            model.body.applyTransform(m);
            if (cape != null) {
                box(m, q, layer, 0, 0, 2.6f, 0, 0, 12, 10, 24, 1, cape.color(), light);
                box(m, q, layer, 0, 0, 2.7f, 0, 0, 1, 10, 2, 1.2f, shade(cape.color(), 1.35f), light);
            }
            if (wings != null) {
                wings(m, q, layer, wings, t, light);
            }
            if (pet != null) {
                float bob = (float) Math.sin(t * 2.2) * 1.2f;
                box(m, q, layer, 10, -4 + bob, 0, 0, 0, 0, 3.5f, 3.5f, 3.5f, pet.color(), FULLBRIGHT);
                box(m, q, layer, 10, -6.5f + bob, 0, 0, 0, 0, 1.8f, 1.8f, 1.8f, shade(pet.color(), 1.4f), FULLBRIGHT);
            }
            m.pop();
        }

        Cosmetics.Item head = item(Cosmetics.Kind.HEAD);
        if (head != null) {
            m.push();
            model.head.applyTransform(m);
            headwear(m, q, layer, head, light);
            m.pop();
        }
    }

    private void wings(MatrixStack m, OrderedRenderCommandQueue q, RenderLayer layer, Cosmetics.Item item, float t, int light) {
        boolean dragon = "dragon".equals(item.id());
        int feathers = dragon ? 3 : 5;
        float flap = (float) Math.sin(t * 2.4) * 7f;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < feathers; i++) {
                float theta = 22 + i * (dragon ? 26 : 16) + flap;
                float len = dragon ? 20 - i * 3 : 17 - i * 2.5f;
                float width = dragon ? 2.2f : 3.2f;
                int col = shade(item.color(), i % 2 == 0 ? 1.0f : 0.85f);
                box(m, q, layer, side * 2.5f, 2, 3f + i * 0.35f, side * theta, 0, -len / 2f, width, len, 0.8f, col, light);
            }
        }
    }

    private void headwear(MatrixStack m, OrderedRenderCommandQueue q, RenderLayer layer, Cosmetics.Item item, int light) {
        int c = item.color();
        switch (item.id()) {
            case "halo" -> {
                for (int k = 0; k < 10; k++) {
                    double a = k * Math.PI * 2 / 10;
                    box(m, q, layer, (float) Math.cos(a) * 5.2f, -11.5f, (float) Math.sin(a) * 5.2f, 0, 0, 0, 2.2f, 0.9f, 2.2f, c, FULLBRIGHT);
                }
            }
            case "horns" -> {
                for (int s = -1; s <= 1; s += 2) {
                    box(m, q, layer, s * 3.6f, -8.2f, 0, s * -18, 0, -2, 1.8f, 4, 1.8f, c, light);
                    box(m, q, layer, s * 5f, -12.4f, 0, s * -32, 0, -1, 1.2f, 3, 1.2f, shade(c, 1.4f), light);
                }
            }
            case "crown" -> {
                box(m, q, layer, 0, -9f, 4.3f, 0, 0, 0, 8.6f, 2, 1, c, FULLBRIGHT);
                box(m, q, layer, 0, -9f, -4.3f, 0, 0, 0, 8.6f, 2, 1, c, FULLBRIGHT);
                box(m, q, layer, 4.3f, -9f, 0, 0, 0, 0, 1, 2, 8.6f, c, FULLBRIGHT);
                box(m, q, layer, -4.3f, -9f, 0, 0, 0, 0, 1, 2, 8.6f, c, FULLBRIGHT);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        box(m, q, layer, sx * 4.3f, -11.2f, sz * 4.3f, 0, 0, 0, 1.2f, 2.6f, 1.2f, shade(c, 1.25f), FULLBRIGHT);
                    }
                }
            }
            case "cat" -> {
                for (int s = -1; s <= 1; s += 2) {
                    box(m, q, layer, s * 3f, -9.4f, 0, s * -10, 0, 0, 2.8f, 3, 1.2f, c, light);
                    box(m, q, layer, s * 3f, -9.2f, -0.2f, s * -10, 0, 0, 1.4f, 1.6f, 1.2f, 0xFFF4A0B8, light);
                }
            }
            default -> box(m, q, layer, 0, -10, 0, 0, 0, 0, 4, 2, 4, c, light);
        }
    }

    private static Cosmetics.Item item(Cosmetics.Kind kind) {
        String id = Cosmetics.equipped(kind);
        return "none".equals(id) ? null : Cosmetics.named(kind, id);
    }

    private static int shade(int argb, float f) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Box in model-space pixels: pivot (px,py,pz), rotated rotZ degrees about Z, drawn centered at offset (ox,oy). */
    private static void box(MatrixStack m, OrderedRenderCommandQueue q, RenderLayer layer,
                            float px, float py, float pz, float rotZ, float ox, float oy,
                            float w, float h, float d, int argb, int light) {
        m.push();
        m.translate(px / 16f, py / 16f, pz / 16f);
        if (rotZ != 0) {
            m.multiply(new Quaternionf().rotateZ((float) Math.toRadians(rotZ)));
        }
        m.translate(ox / 16f, oy / 16f, 0);
        float a = w / 32f;
        float b = h / 32f;
        float c = d / 32f;
        int col = argb | 0xFF000000;
        q.submitCustom(m, layer, (e, vc) -> cube(e, vc, a, b, c, col, light));
        m.pop();
    }

    private static void cube(MatrixStack.Entry e, VertexConsumer vc, float a, float b, float c, int col, int light) {
        face(e, vc, col, light, 1, 0, 0, a, -b, -c, a, -b, c, a, b, c, a, b, -c);
        face(e, vc, col, light, -1, 0, 0, -a, -b, c, -a, -b, -c, -a, b, -c, -a, b, c);
        face(e, vc, col, light, 0, 1, 0, -a, b, -c, a, b, -c, a, b, c, -a, b, c);
        face(e, vc, col, light, 0, -1, 0, -a, -b, c, a, -b, c, a, -b, -c, -a, -b, -c);
        face(e, vc, col, light, 0, 0, 1, -a, -b, c, a, -b, c, a, b, c, -a, b, c);
        face(e, vc, col, light, 0, 0, -1, a, -b, -c, -a, -b, -c, -a, b, -c, a, b, -c);
    }

    private static void face(MatrixStack.Entry e, VertexConsumer vc, int col, int light, float nx, float ny, float nz,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        vertex(e, vc, x1, y1, z1, 0, 0, col, light, nx, ny, nz);
        vertex(e, vc, x2, y2, z2, 1, 0, col, light, nx, ny, nz);
        vertex(e, vc, x3, y3, z3, 1, 1, col, light, nx, ny, nz);
        vertex(e, vc, x4, y4, z4, 0, 1, col, light, nx, ny, nz);
    }

    private static void vertex(MatrixStack.Entry e, VertexConsumer vc, float x, float y, float z, float u, float v,
                               int col, int light, float nx, float ny, float nz) {
        vc.vertex(e, x, y, z).color(col).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, nx, ny, nz);
    }
}
