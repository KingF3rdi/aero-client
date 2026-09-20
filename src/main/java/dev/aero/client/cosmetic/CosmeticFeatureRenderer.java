package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

/**
 * Real 3D, animated cosmetics (cape, wings, headwear, pet) drawn as small flat-colored boxes on the
 * local player's model. As a normal feature renderer the same code shows up in third person and in
 * the wardrobe's live preview. Client-only: other players can't see your cosmetics, and first person
 * hides your own model, so use F5 to see them.
 */
public final class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final int FB = CubeDraw.FULLBRIGHT;

    private MatrixStack m;
    private OrderedRenderCommandQueue q;
    private RenderLayer layer;
    private int light;
    private float t;
    private float speed;
    private java.util.UUID who;
    private boolean self;

    private static boolean AeroClient_showOthers() {
        var c = dev.aero.client.AeroClient.CONFIG;
        return c == null || c.showOthersCosmetics;
    }

    private String idOf(Cosmetics.Kind kind) {
        if (self) {
            return Cosmetics.equipped(kind);
        }
        String key = switch (kind) {
            case CAPE -> "cape";
            case WINGS -> "wings";
            case HEAD -> "head";
            case PET -> "pet";
            case SHIELD -> "shield";
            default -> "none";
        };
        return dev.aero.client.social.ClientUsers.cosmeticOf(who, key);
    }

    private int colorOf(Cosmetics.Kind kind) {
        if (self) {
            return Cosmetics.equippedColor(kind);
        }
        String id = idOf(kind);
        if ("none".equals(id)) {
            return 0;
        }
        Cosmetics.Item item = Cosmetics.named(kind, id);
        return item == null ? 0 : item.color();
    }

    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state,
                       float yaw, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || state.invisible) {
            return;
        }
        boolean self = state.id == mc.player.getId();
        java.util.UUID who = null;
        if (self) {
            who = mc.player.getUuid();
        } else {
            var other = mc.world.getEntityById(state.id);
            if (other instanceof net.minecraft.entity.player.PlayerEntity p && AeroClient_showOthers()) {
                who = p.getUuid();
            }
            if (who == null || !dev.aero.client.social.ClientUsers.hasCosmetics(who)) {
                return;
            }
        }
        this.who = who;
        this.self = self;
        this.m = matrices;
        this.q = queue;
        this.light = light;
        this.layer = RenderLayers.entityCutoutNoCull(CubeDraw.WHITE);
        this.t = (System.currentTimeMillis() % 100000L) / 1000f;
        this.speed = (float) Math.min(1.0, mc.player.getVelocity().horizontalLength() * 5.0);
        PlayerEntityModel model = getContextModel();

        int cape = colorOf(Cosmetics.Kind.CAPE);
        int wings = colorOf(Cosmetics.Kind.WINGS);
        if (cape != 0 || wings != 0) {
            m.push();
            model.body.applyTransform(m);
            if (cape != 0) {
                cape(cape, idOf(Cosmetics.Kind.CAPE));
            }
            if (wings != 0) {
                wings(wings, idOf(Cosmetics.Kind.WINGS));
            }
            m.pop();
        }

        int shieldArms = SpikedShield.ARMS.getOrDefault(state.id, 0);
        if (shieldArms != 0 && !"none".equals(idOf(Cosmetics.Kind.SHIELD))) {
            for (int a = 1; a <= 2; a++) {
                if ((shieldArms & a) == 0) {
                    continue;
                }
                float outward = a == 1 ? 1f : -1f;
                m.push();
                (a == 1 ? model.leftArm : model.rightArm).applyTransform(m);
                m.translate(0.2f * outward, 0.3f, 0f);
                m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-70f * outward));
                m.scale(0.6f, 0.6f, 0.6f);
                SpikedShield.drawFor(m, q, layer, light, idOf(Cosmetics.Kind.SHIELD), colorOf(Cosmetics.Kind.SHIELD));
                m.pop();
            }
        }

        int head = colorOf(Cosmetics.Kind.HEAD);
        if (head != 0) {
            m.push();
            model.head.applyTransform(m);
            headwear(head, idOf(Cosmetics.Kind.HEAD));
            m.pop();
        }

        int pet = colorOf(Cosmetics.Kind.PET);
        if (pet != 0) {
            pet(pet, idOf(Cosmetics.Kind.PET));
        }
    }

    // ---- cape ---------------------------------------------------------------------------------

    private void cape(int c, String id) {
        float sway = 5f + speed * 22f + (float) Math.sin(t * 1.7) * 3f;
        CapeTextures.Tex tex = CapeTextures.get(id);
        if (tex != null) {
            m.push();
            m.translate(0, 0, 2.4f / 16f);
            m.multiply(new Quaternionf().rotateX((float) Math.toRadians(sway)));
            m.translate(0, 8f / 16f, 0);
            RenderLayer capeLayer = RenderLayers.entityCutoutNoCull(tex.id());
            int lt = light;
            q.submitCustom(m, capeLayer, (e, vc) -> CubeDraw.capeBox(e, vc, 5f / 16f, 8f / 16f, 0.5f / 16f, lt, tex.w(), tex.h()));
            m.pop();
            return;
        }
        int dark = CubeDraw.shade(c, 0.55f);
        int slices = 5;
        for (int i = 0; i < slices; i++) {
            int col = CubeDraw.mix(c, dark, i / (float) (slices - 1));
            if ("glitch".equals(id) && (i + (int) (t * 6)) % 2 == 0) {
                col = CubeDraw.shade(col, 1.5f);
            }
            box(0, 0, 2.4f, sway, 0, 0, 0, 1.6f + i * 3.2f, 0, 10, 3.3f, 1, col, light);
        }
        int trim = CubeDraw.shade(c, 1.4f);
        box(0, 0, 2.4f, sway, 0, 0, 0, 0.7f, -0.15f, 10.4f, 1.4f, 1.4f, trim, light);
        box(0, 0, 2.4f, sway, 0, 0, -4.9f, 8, 0, 0.8f, 16, 1.2f, trim, light);
        box(0, 0, 2.4f, sway, 0, 0, 4.9f, 8, 0, 0.8f, 16, 1.2f, trim, light);
    }

    // ---- wings --------------------------------------------------------------------------------

    private void feather(int side, float px, float py, float pz, float theta, float len, float w, int col, int lt) {
        box(side * px, py, pz, 0, 0, side * theta, 0, -len / 2f, 0, w, len, 0.8f, col, lt);
    }

    private void wings(int c, String id) {
        float flap = (float) Math.sin(t * 2.4) * (7f + speed * 10f);
        for (int side = -1; side <= 1; side += 2) {
            switch (id) {
                case "dragon" -> {
                    int bone = 0xFF4A2A22;
                    for (int i = 0; i < 9; i++) {
                        float th = 18 + i * 9.5f + flap;
                        float len = 15 + (float) Math.sin(i * 0.55) * 6f;
                        feather(side, 2.5f, 2, 3f + i * 0.12f, th, len, 3.2f, CubeDraw.shade(c, i % 2 == 0 ? 1f : 0.82f), light);
                    }
                    for (int i = 0; i < 3; i++) {
                        feather(side, 2.5f, 2, 3.9f, 24 + i * 32 + flap, 22 - i * 2, 1.4f, bone, light);
                    }
                }
                case "fairy" -> {
                    fairyWing(side, 9, -3, 6, 9.5f, 24 + flap, c);
                    fairyWing(side, 7, 8, 4, 6.5f, 62 + flap, CubeDraw.shade(c, 0.85f));
                }
                case "aurora" -> {
                    for (int i = 0; i < 7; i++) {
                        float th = 16 + i * 13f + flap;
                        int col = CubeDraw.mix(0xFF4FE8D8, 0xFFB060FF, ((float) Math.sin(t * 1.4 + i * 0.6) + 1f) / 2f);
                        feather(side, 2.5f, 2, 3f + i * 0.3f, th, 19 - i * 1.6f, 3.2f, col, FB);
                    }
                }
                case "aegis" -> {
                    for (int i = 0; i < 4; i++) {
                        float th = 28 + i * 20f + flap * 0.5f;
                        float len = 17 - i * 2.4f;
                        feather(side, 2.5f, 2, 3f + i * 0.5f, th, len, 4.2f, CubeDraw.shade(c, 1f - i * 0.08f), light);
                        feather(side, 2.5f, 2, 3.5f + i * 0.5f, th, len, 1.2f, 0xFFE8B84A, light);
                    }
                }
                default -> {
                    int n = 6;
                    for (int i = 0; i < n; i++) {
                        float th = 16 + i * 14f + flap;
                        feather(side, 2.5f, 2, 3f + i * 0.3f, th, 19 - i * 2.2f, 3.4f, CubeDraw.shade(c, i % 2 == 0 ? 1f : 0.86f), light);
                    }
                    for (int i = 0; i < 4; i++) {
                        feather(side, 2.5f, 2, 3f + i * 0.3f - 0.4f, 24 + i * 16f + flap, 9 - i, 3f, CubeDraw.shade(c, 0.7f), light);
                    }
                }
            }
        }
    }

    private void fairyWing(int side, float cx, float cy, float rx, float ry, float tilt, int c) {
        int n = 12;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            float x = (float) Math.cos(a) * rx;
            float y = (float) Math.sin(a) * ry;
            double tr = Math.toRadians(side * -tilt * 0.5);
            float rxp = (float) (x * Math.cos(tr) - y * Math.sin(tr));
            float ryp = (float) (x * Math.sin(tr) + y * Math.cos(tr));
            box(side * (cx + rxp), cy - 4 + ryp, 3.2f, 0, 0, 0, 0, 0, 0, 1.5f, 1.5f, 0.8f, c, FB);
        }
        box(side * cx, cy - 4, 3.1f, 0, 0, 0, 0, 0, 0, rx * 1.3f, ry * 1.3f, 0.5f, CubeDraw.shade(c, 0.6f), light);
    }

    // ---- headwear -----------------------------------------------------------------------------

    private void headwear(int c, String id) {
        switch (id) {
            case "halo" -> {
                float bob = (float) Math.sin(t * 2) * 0.6f;
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI * 2 / 12 + t * 1.2;
                    box((float) Math.cos(a) * 5.2f, -12f + bob, (float) Math.sin(a) * 5.2f, 0, 0, 0, 0, 0, 0, 1.9f, 0.9f, 1.9f,
                            CubeDraw.shade(c, 0.85f + 0.3f * ((k % 3) / 2f)), FB);
                }
            }
            case "horns" -> {
                for (int s = -1; s <= 1; s += 2) {
                    box(s * 3.4f, -8f, 0, 0, 0, s * -22, 0, -2, 0, 2f, 4.5f, 2f, c, light);
                    box(s * 4.6f, -11.6f, 0, 0, 0, s * -40, 0, -1.5f, 0, 1.4f, 3.2f, 1.4f, CubeDraw.shade(c, 1.4f), light);
                }
            }
            case "crown" -> {
                int g = CubeDraw.shade(c, 1f);
                box(0, -9f, 4.3f, 0, 0, 0, 0, 0, 0, 8.8f, 2, 1, g, FB);
                box(0, -9f, -4.3f, 0, 0, 0, 0, 0, 0, 8.8f, 2, 1, g, FB);
                box(4.3f, -9f, 0, 0, 0, 0, 0, 0, 0, 1, 2, 8.8f, g, FB);
                box(-4.3f, -9f, 0, 0, 0, 0, 0, 0, 0, 1, 2, 8.8f, g, FB);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sz = -1; sz <= 1; sz += 2) {
                        float glint = 1.1f + 0.3f * (float) Math.sin(t * 3 + sx + sz * 2);
                        box(sx * 4.3f, -11.4f, sz * 4.3f, 0, 0, 0, 0, 0, 0, 1.3f, 2.8f, 1.3f, CubeDraw.shade(c, glint), FB);
                    }
                }
                box(0, -10.2f, -4.4f, 0, 0, 0, 0, 0, 0, 1.4f, 1.4f, 0.8f, 0xFFE0405A, FB);
            }
            case "cat" -> {
                float twitch = (float) Math.pow(Math.max(0, Math.sin(t * 2.6)), 8) * 16f;
                for (int s = -1; s <= 1; s += 2) {
                    float rot = s * -(10 + (s > 0 ? twitch : 0));
                    box(s * 3f, -8.6f, 0, 0, 0, rot, 0, -1.4f, 0, 3f, 3.2f, 1.2f, c, light);
                    box(s * 3f, -8.6f, -0.35f, 0, 0, rot, 0, -1.2f, 0, 1.6f, 1.8f, 1.1f, 0xFFF4A0B8, light);
                }
            }
            default -> box(0, -10, 0, 0, 0, 0, 0, 0, 0, 4, 2, 4, c, light);
        }
    }

    // ---- pets ---------------------------------------------------------------------------------

    private void pet(int c, String id) {
        m.push();
        // Model root space: y = 24 px is the ground. Beside the player, slightly ahead.
        m.translate(19f / 16f, 24f / 16f, -3f / 16f);
        float walk = (float) Math.sin(t * 11) * 32f * speed;
        switch (id) {
            case "bee" -> bee(c);
            case "fox" -> fox(c, walk);
            default -> axolotl(c, walk);
        }
        m.pop();
    }

    private void axolotl(int c, float walk) {
        int belly = CubeDraw.shade(c, 1.12f);
        int gill = CubeDraw.shade(c, 0.62f);
        box(0, -3.6f, 0, 0, 0, 0, 0, 0, 0, 6, 4.6f, 9.5f, c, light);
        box(0, -3.1f, 0, 0, 0, 0, 0, 0, 0, 6.1f, 1.6f, 9.6f, belly, light);
        box(0, -4.7f, -6.4f, 0, 0, 0, 0, 0, 0, 7.4f, 5f, 5f, c, light);
        for (int s = -1; s <= 1; s += 2) {
            box(s * 1.9f, -5.4f, -8.95f, 0, 0, 0, 0, 0, 0, 1.1f, 1.1f, 0.5f, 0xFF161018, FB);
            for (int j = 0; j < 3; j++) {
                box(s * 4.8f, -6.4f + j * 1.5f, -6.4f, 0, 0, s * (12 - j * 12), 0, 0, 0, 2.6f, 0.9f, 0.9f, gill, light);
            }
        }
        float wag = (float) Math.sin(t * 3.2) * 14f;
        box(0, -3.6f, 4.5f, 0, wag, 0, 0, 0, 3.2f, 2.6f, 4.4f, 7.5f, c, light);
        box(0, -5.6f, 4.5f, 0, wag, 0, 0, 0, 3.2f, 0.9f, 2.2f, 7f, gill, light);
        for (int i = 0; i < 4; i++) {
            float x = (i % 2 == 0 ? -1 : 1) * 3.2f;
            float z = i < 2 ? -3.2f : 3f;
            box(x, -2.2f, z, (i % 3 == 0 ? walk : -walk), 0, 0, 0, 1.2f, 0, 1.8f, 2.4f, 2f, gill, light);
        }
    }

    private void bee(int c) {
        float bob = (float) Math.sin(t * 2.6) * 1.4f;
        float y = -15f + bob;
        int black = 0xFF1E1A12;
        box(0, y, 0, 0, 0, 0, 0, 0, 0, 5.4f, 5.4f, 8.4f, c, light);
        box(0, y, -1.2f, 0, 0, 0, 0, 0, 0, 5.6f, 5.6f, 1.7f, black, light);
        box(0, y, 2.2f, 0, 0, 0, 0, 0, 0, 5.6f, 5.6f, 1.7f, black, light);
        for (int s = -1; s <= 1; s += 2) {
            box(s * 1.6f, y - 0.4f, -4.3f, 0, 0, 0, 0, 0, 0, 1.4f, 1.8f, 0.6f, black, FB);
            box(s * 1.2f, y - 3.6f, -3.4f, 0, 0, s * 18, 0, 0, 0, 0.5f, 2f, 0.5f, black, light);
            float fl = 32f + (float) Math.sin(t * 38) * 24f;
            box(s * 1.2f, y - 2.9f, -0.6f, 0, 0, s * fl, s * 3.4f, 0, 0, 6.2f, 0.5f, 3.4f, 0xFFE8F4FF, FB);
        }
        box(0, y + 1.6f, 4.9f, 0, 0, 0, 0, 0, 0, 1f, 1f, 2.2f, black, light);
    }

    private void fox(int c, float walk) {
        int white = 0xFFF4EEE4;
        int dark = 0xFF3A2418;
        box(0, -8.2f, 0, 0, 0, 0, 0, 0, 0, 6, 5f, 10, c, light);
        box(0, -7.2f, -3.8f, 0, 0, 0, 0, 0, 0, 4.2f, 3.2f, 1.2f, white, light);
        box(0, -9.6f, -7.2f, 0, 0, 0, 0, 0, 0, 6.2f, 5f, 5f, c, light);
        box(0, -8.4f, -10.3f, 0, 0, 0, 0, 0, 0, 3.2f, 2.2f, 2.8f, white, light);
        box(0, -9f, -11.8f, 0, 0, 0, 0, 0, 0, 1.2f, 1.1f, 0.5f, dark, light);
        for (int s = -1; s <= 1; s += 2) {
            box(s * 2f, -13.2f, -7.2f, 0, 0, 0, 0, 0, 0, 2f, 2.6f, 1.2f, c, light);
            box(s * 2f, -14.2f, -7.2f, 0, 0, 0, 0, 0, 0, 1.4f, 1f, 1.1f, dark, light);
            box(s * 1.9f, -10.4f, -9.75f, 0, 0, 0, 0, 0, 0, 1f, 1f, 0.5f, 0xFF141010, FB);
        }
        float wag = (float) Math.sin(t * 4.2) * 16f;
        box(0, -8.6f, 5f, -30, wag, 0, 0, 0, 4.4f, 4.2f, 4.2f, 9f, c, light);
        box(0, -8.6f, 5f, -30, wag, 0, 0, 0, 8.6f, 4.3f, 4.3f, 2.6f, white, light);
        for (int i = 0; i < 4; i++) {
            float x = (i % 2 == 0 ? -1 : 1) * 2.1f;
            float z = i < 2 ? -3.4f : 3.4f;
            box(x, -5.4f, z, (i % 3 == 0 ? walk : -walk), 0, 0, 0, 2.6f, 0, 2f, 5.2f, 2f, dark, light);
        }
    }

    // ---- box helper ---------------------------------------------------------------------------

    /** Box in model-space pixels: pivot, rotation (deg) about X/Y/Z, then drawn centered at offset (ox,oy,oz). */
    private void box(float px, float py, float pz, float rotX, float rotY, float rotZ,
                     float ox, float oy, float oz, float w, float h, float d, int argb, int lt) {
        m.push();
        m.translate(px / 16f, py / 16f, pz / 16f);
        if (rotX != 0 || rotY != 0 || rotZ != 0) {
            m.multiply(new Quaternionf().rotateY((float) Math.toRadians(rotY))
                    .rotateX((float) Math.toRadians(rotX)).rotateZ((float) Math.toRadians(rotZ)));
        }
        m.translate(ox / 16f, oy / 16f, oz / 16f);
        float a = w / 32f;
        float b = h / 32f;
        float cc = d / 32f;
        q.submitCustom(m, layer, (e, vc) -> CubeDraw.cube(e, vc, a, b, cc, argb, lt));
        m.pop();
    }
}
