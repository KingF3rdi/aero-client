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

    /** One cube of the frame's batch; all of a player's cubes go to the GPU as a single command. */
    private record Cube(MatrixStack.Entry e, float a, float b, float c, int col, int light) {}

    private java.util.List<Cube> batch;

    /** A smooth cone (hats) in the same batch; see CubeDraw.cone. */
    private record Cone(MatrixStack.Entry e, float r, float h, int top, int rim, int under, int light) {}

    private java.util.List<Cone> cones;
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
            return Cosmetics.shown(kind);
        }
        String key = switch (kind) {
            case CAPE -> "cape";
            case WINGS -> "wings";
            case HEAD -> "head";
            case PET -> "pet";
            default -> "none";
        };
        return dev.aero.client.social.ClientUsers.cosmeticOf(who, key);
    }

    private int colorOf(Cosmetics.Kind kind) {
        if (self) {
            return Cosmetics.shownColor(kind);
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
        boolean menu = state.id == CosmeticPreview.MENU_ID; // wardrobe preview on the title screen
        if (state.invisible || (!menu && (mc.player == null || mc.world == null))) {
            return;
        }
        boolean self = menu || state.id == mc.player.getId();
        java.util.UUID who = null;
        if (menu) {
            who = mc.getGameProfile().id();
        } else if (self) {
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
        this.speed = menu ? 0f : (float) Math.min(1.0, mc.player.getVelocity().horizontalLength() * 5.0);
        this.batch = new java.util.ArrayList<>(96);
        this.cones = new java.util.ArrayList<>(2);
        PlayerEntityModel model = getContextModel();

        int cape = colorOf(Cosmetics.Kind.CAPE);
        int wings = colorOf(Cosmetics.Kind.WINGS);
        if (cape != 0 || wings != 0) {
            m.push();
            model.body.applyTransform(m);
            if (cape != 0) {
                // Like vanilla: no cape while an elytra is worn (it clipped through the wings as dark bars).
                if (!state.equippedChestStack.isOf(net.minecraft.item.Items.ELYTRA)) {
                    cape(cape, idOf(Cosmetics.Kind.CAPE));
                }
            }
            if (wings != 0) {
                wings(wings, idOf(Cosmetics.Kind.WINGS));
            }
            m.pop();
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

        if (!batch.isEmpty() || !cones.isEmpty()) {
            java.util.List<Cube> cubes = batch;
            java.util.List<Cone> round = cones;
            q.submitCustom(m, layer, (e, vc) -> {
                for (Cube c : cubes) {
                    CubeDraw.cube(c.e(), vc, c.a(), c.b(), c.c(), c.col(), c.light());
                }
                for (Cone c : round) {
                    CubeDraw.cone(c.e(), vc, c.r(), c.h(), 24, c.top(), c.rim(), c.under(), c.light());
                }
            });
        }
        batch = null;
        cones = null;
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
        if (shapedWings(c, id)) {
            return;
        }
        float flap = (float) Math.sin(t * 2.4) * (7f + speed * 10f);
        for (int side = -1; side <= 1; side += 2) {
            switch (id) {
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

    // ---- shaped wings: drawn flat in a "wing plane" (u = outward, v = up, in pixels) hinged at the shoulder
    // ---- blade and swept back around the vertical axis, so flapping is a real back-and-forth beat.

    private int side;
    /** Per-wing depth step: bars that overlap in one plane z-fought (flickered) while the wing moved. */
    private int zStep;

    /** Draws the wing styles that have their own silhouette; false for the classic feather styles. */
    private boolean shapedWings(int c, String id) {
        switch (id) {
            case "dragon", "bat", "butterfly", "mech", "crystal", "phoenix", "seraph", "neon", "phantom" -> {
            }
            default -> {
                return false;
            }
        }
        float beat = (float) Math.sin(t * 2.4);
        if ("mech".equals(id)) {
            thrusters(c);
        }
        for (side = -1; side <= 1; side += 2) {
            zStep = 0;
            float sweep = switch (id) {
                case "butterfly" -> 12 + (0.5f + 0.5f * (float) Math.sin(t * 3.2)) * 30;
                case "bat" -> 20 + (float) Math.sin(t * 4.2) * (12 + speed * 10);
                case "mech" -> 30 + beat * 3 + speed * 12;
                case "crystal" -> 26 + beat * 4;
                default -> 22 + beat * (8 + speed * 12);
            };
            m.push();
            m.translate(side * 1.5f / 16f, 2.5f / 16f, 3f / 16f);
            m.multiply(new Quaternionf().rotateY((float) Math.toRadians(-side * sweep)));
            switch (id) {
                case "dragon" -> membrane(c, 5, 6, new float[][]{{19, 8}, {21, 0}, {17, -7}, {10, -11}, {1, -4}}, 1.8f, 0.45f);
                case "bat" -> membrane(c, 4, 4, new float[][]{{13, 5}, {15, -1}, {12, -6}, {7, -8}, {1, -3}}, 1.3f, 0.55f);
                case "butterfly" -> butterfly(c);
                case "mech" -> mech(c);
                case "crystal" -> crystal(c);
                case "phoenix" -> phoenix(c);
                case "seraph" -> seraph(c);
                case "neon" -> neon(c);
                default -> phantom(c);
            }
            m.pop();
        }
        return true;
    }

    /** A straight bar from (u0,v0) to (u1,v1) in the wing plane. */
    private void bar(float u0, float v0, float u1, float v1, float thick, float depth, int col, int lt) {
        float x0 = side * u0;
        float y0 = -v0;
        float x1 = side * u1;
        float y1 = -v1;
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        if (len < 0.05f) {
            return;
        }
        float ang = (float) Math.toDegrees(Math.atan2(y1 - y0, x1 - x0));
        box((x0 + x1) / 2f, (y0 + y1) / 2f, zStep++ * 0.04f, 0, 0, ang, 0, 0, 0, len + thick * 0.5f, thick, depth, col, lt);
    }

    /** Bat/dragon wing: arm to an elbow, fingers fanning out, scalloped membrane between the fingers. */
    private void membrane(int c, float eu, float ev, float[][] tips, float boneW, float boneShade) {
        int bone = CubeDraw.shade(c, boneShade);
        bar(0, 0, eu, ev, boneW * 1.3f, boneW * 1.3f, bone, light);
        bar(eu, ev, eu + 0.8f, ev + 2.4f, 0.9f, 0.9f, 0xFFE8E0D0, light); // claw
        int n = 4;
        for (int i = 0; i < tips.length - 1; i++) {
            float[] a = tips[i];
            float[] b = tips[i + 1];
            int col = CubeDraw.shade(c, i % 2 == 0 ? 1f : 0.88f);
            float gap = (float) Math.hypot(a[0] - b[0], a[1] - b[1]) / (n + 1);
            for (int k = 1; k <= n; k++) {
                float f = k / (float) (n + 1);
                float scallop = 1f - 0.2f * (float) Math.sin(Math.PI * f);
                float pu = eu + (a[0] + (b[0] - a[0]) * f - eu) * scallop;
                float pv = ev + (a[1] + (b[1] - a[1]) * f - ev) * scallop;
                bar(eu, ev, pu, pv, Math.max(1.3f, gap * 1.2f), 0.4f, col, light);
            }
            bar(eu, ev, a[0], a[1], boneW * 0.7f, boneW * 0.7f, bone, light); // finger
        }
        float[] last = tips[tips.length - 1];
        for (int k = 1; k <= 2; k++) { // fill between the body and the arm
            float f = k / 3f;
            bar(eu, ev, last[0] * f, last[1] * f, 2.4f, 0.4f, c, light);
        }
    }

    private void butterfly(int c) {
        int edge = CubeDraw.shade(c, 0.35f);
        lobe(9, 5, 8.5f, 7f, c, edge);
        lobe(7, -5, 5.2f, 6f, CubeDraw.shade(c, 0.85f), edge);
        bar(0, 0, 15, 9, 0.6f, 0.9f, edge, light); // veins
        bar(0, 0, 10, -9, 0.6f, 0.9f, edge, light);
        box(side * 13.5f, -8f, 0, 0, 0, 0, 0, 0, 0, 1.8f, 1.8f, 0.9f, 0xFFFFFFFF, FB); // spots
        box(side * 10.5f, -10.5f, 0, 0, 0, 0, 0, 0, 0, 1.2f, 1.2f, 0.9f, 0xFFFFFFFF, FB);
    }

    /** Filled ellipse made of horizontal rows, lighter at the center, dark at the rim. */
    private void lobe(float cu, float cv, float rx, float ry, int c, int edge) {
        for (float y = -ry + 0.8f; y <= ry - 0.4f; y += 1.5f) {
            float hw = rx * (float) Math.sqrt(Math.max(0, 1 - (y / ry) * (y / ry)));
            float f = Math.abs(y) / ry;
            bar(cu - hw, cv + y, cu + hw, cv + y, 1.7f, 0.6f, CubeDraw.mix(c, edge, f * f * 0.8f), light);
        }
    }

    private void mech(int c) {
        int plate = 0xFF4A505C;
        int plate2 = 0xFF626A78;
        float[][] p = {{0.5f, 2, 11, 7, 3.2f}, {0.5f, 0, 13, 1.5f, 2.8f}, {0.5f, -2, 10, -5, 2.4f}};
        for (int i = 0; i < p.length; i++) {
            float[] r = p[i];
            bar(r[0], r[1], r[2], r[3], r[4], 1.2f, i % 2 == 0 ? plate : plate2, light);
            float glow = 0.95f + 0.25f * (float) Math.sin(t * 3 + i);
            bar(r[0] + 1.5f, r[1], r[0] + (r[2] - r[0]) * 0.92f, r[1] + (r[3] - r[1]) * 0.92f, 0.5f, 1.5f,
                    CubeDraw.shade(c, glow), FB);
        }
        box(0, 0, 0, 0, 0, 0, 0, 0, 0, 2.4f, 2.4f, 2.4f, plate2, light); // hinge
    }

    /** Twin thrusters on the back with a flickering flame (body space, drawn once). */
    private void thrusters(int c) {
        for (int s = -1; s <= 1; s += 2) {
            box(s * 1.8f, 6f, 2.9f, 0, 0, 0, 0, 0, 0, 2f, 4f, 1.8f, 0xFF3A3F4A, light);
            float len = 2f + (float) Math.abs(Math.sin(t * 6 + s)) * 1.2f + speed * 2f;
            box(s * 1.8f, 8f + len / 2f, 2.9f, 0, 0, 0, 0, 0, 0, 1.2f, len, 1.2f, CubeDraw.mix(c, 0xFFFFB040, 0.6f), FB);
            box(s * 1.8f, 8.3f, 2.9f, 0, 0, 0, 0, 0, 0, 1.5f, 0.8f, 1.5f, 0xFFFFF4D0, FB);
        }
    }

    private void crystal(int c) {
        for (int i = 0; i < 6; i++) {
            float a = (float) Math.toRadians(22 + i * 20);
            float d = 6.5f + i * 1.5f;
            float bob = (float) Math.sin(t * 2 + i * 1.1f) * 0.8f;
            float len = 5.2f - Math.abs(i - 2.5f) * 0.55f;
            m.push();
            m.translate(side * (float) Math.sin(a) * d / 16f, (-(float) Math.cos(a) * d - bob) / 16f, 0);
            m.multiply(new Quaternionf().rotateZ(side * a).rotateY((float) Math.toRadians(45 + t * 40 + i * 20)));
            int col = CubeDraw.mix(c, 0xFFFFFFFF, 0.3f + 0.15f * (0.5f + 0.5f * (float) Math.sin(t * 1.2 + i)));
            cubeHere(1.7f, len, 1.7f, col, FB);
            m.pop();
        }
    }

    private void phoenix(int c) {
        int deep = CubeDraw.mix(0xFFD8321E, c, 0.45f);
        for (int i = 0; i < 7; i++) {
            float a = (float) Math.toRadians(10 + i * 14);
            float len = 17 - i * 1.6f + (float) Math.sin(t * 3 + i * 1.9f) * 0.8f;
            float su = (float) Math.sin(a);
            float sv = (float) Math.cos(a);
            bar(0, 0, su * len * 0.62f, sv * len * 0.62f, 2.4f, 0.8f, i % 2 == 0 ? deep : CubeDraw.shade(deep, 1.15f), FB);
            bar(su * len * 0.58f, sv * len * 0.58f, su * len, sv * len, 1.8f, 0.8f, 0xFFFFC84A, FB);
        }
        for (int k = 0; k < 3; k++) { // embers drifting up off the wing
            float ph = (float) ((t * 0.7 + k * 0.33 + (side + 1) * 0.15) % 1.0);
            float a = (float) Math.toRadians(20 + k * 30);
            float u = (float) Math.sin(a) * 14f;
            float v = (float) Math.cos(a) * 14f + ph * 7f;
            float sz = 1.3f * (1f - ph);
            box(side * u, -v, 0, 0, 0, 0, 0, 0, 0, sz, sz, sz, 0xFFFFE08A, FB);
        }
    }

    private void seraph(int c) {
        fan(0, 3, -4, 32, 4, 11, c);
        fan(0, 0, 58, 98, 5, 13, c);
        fan(0, -3, 118, 160, 4, 10, c);
    }

    /** A small fan of feathers with glowing gold tips (seraph). */
    private void fan(float ru, float rv, float from, float to, int n, float len, int c) {
        for (int i = 0; i < n; i++) {
            float a = (float) Math.toRadians(from + (to - from) * i / (n - 1f));
            float l = len - Math.abs(i - (n - 1) / 2f) * 1.2f;
            float su = (float) Math.sin(a);
            float sv = (float) Math.cos(a);
            bar(ru, rv, ru + su * l * 0.8f, rv + sv * l * 0.8f, 2.2f, 0.7f, CubeDraw.shade(c, i % 2 == 0 ? 1f : 0.9f), light);
            bar(ru + su * l * 0.75f, rv + sv * l * 0.75f, ru + su * l, rv + sv * l, 1.4f, 0.8f, 0xFFFFD86B, FB);
        }
    }

    private static final float[][] NEON = {{0, 2}, {5, 8}, {12, 11}, {19, 10}, {21, 6}, {17, 3}, {20, -1}, {15, -3},
            {16, -7}, {10, -6}, {6, -8}, {3, -4}, {0, -2}};

    /** Glowing outline of a wing, colors running along it. */
    private void neon(int c) {
        for (int i = 0; i < NEON.length - 1; i++) {
            int hue = java.awt.Color.HSBtoRGB((float) ((t * 0.25 + i * 0.06) % 1.0), 0.75f, 1f);
            bar(NEON[i][0], NEON[i][1], NEON[i + 1][0], NEON[i + 1][1], 0.8f, 0.8f, CubeDraw.mix(c, hue, 0.55f), FB);
        }
        int dim = CubeDraw.shade(c, 0.8f);
        bar(0, 0, 17, 3, 0.5f, 0.5f, dim, FB);
        bar(0, 0, 15, -3, 0.5f, 0.5f, dim, FB);
    }

    /** Tattered strips that ripple like cloth, with glowing tips. */
    private void phantom(int c) {
        float[] seg = {5.5f, 4.5f, 3.8f};
        float[] w = {2.6f, 1.9f, 1.2f};
        for (int k = 0; k < 5; k++) {
            float u = 0;
            float v = 1 - k * 0.6f;
            float a = 35 + k * 19;
            for (int j = 0; j < seg.length; j++) {
                a += (float) Math.sin(t * 3 + k * 0.8f + j * 1.2f) * 9f * j;
                float r = (float) Math.toRadians(a);
                float nu = u + (float) Math.sin(r) * seg[j];
                float nv = v + (float) Math.cos(r) * seg[j];
                bar(u, v, nu, nv, w[j], 0.6f, CubeDraw.shade(c, 0.95f - j * 0.17f), light);
                u = nu;
                v = nv;
            }
            box(side * u, -v, 0, 0, 0, 0, 0, 0, 0, 0.9f, 0.9f, 0.9f, 0xFF7FE8FF, FB);
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
            case "kasa" -> { // smooth conical straw hat with two tassels
                cone(0, -12.6f, 0, 0, 0, 8.6f, 4.8f, CubeDraw.shade(c, 1.12f), c, CubeDraw.shade(c, 0.55f), light);
                box(0, -12.7f, 0, 0, 0, 0, 0, 0, 0, 1.1f, 0.7f, 1.1f, CubeDraw.shade(c, 0.7f), light);
                for (int s = -1; s <= 1; s += 2) {
                    float sway = (float) Math.sin(t * 2.2 + s) * (5f + speed * 14f);
                    box(s * 6.4f, -8.4f, 1.5f, sway, 0, s * sway * 0.4f, 0, 1.8f, 0, 0.4f, 3.6f, 0.4f, 0xFF7FC8E8, light);
                    box(s * 6.4f, -8.4f, 1.5f, sway, 0, s * sway * 0.4f, 0, 3.9f, 0, 0.9f, 1.3f, 0.9f, 0xFF4FA0D0, light);
                }
            }
            case "tophat" -> {
                box(0, -8.5f, 0, 0, 0, 0, 0, 0, 0, 11.4f, 0.9f, 11.4f, c, light);
                box(0, -12.2f, 0, 0, 0, 0, 0, 0, 0, 7.2f, 6.6f, 7.2f, c, light);
                box(0, -9.7f, 0, 0, 0, 0, 0, 0, 0, 7.5f, 1.4f, 7.5f, 0xFFB0303A, light);
            }
            case "wizard" -> {
                cone(0, -9.6f, 0, 0, 0, 8f, 1.2f, c, CubeDraw.shade(c, 0.85f), CubeDraw.shade(c, 0.5f), light);
                cone(0, -19.4f, 1.6f, 9, 0, 4.4f, 10.4f, CubeDraw.shade(c, 1.25f), c, CubeDraw.shade(c, 0.5f), light);
                box(0, -9.7f, 0, 0, 0, 0, 0, 0, 0, 8.6f, 1.1f, 8.6f, 0xFFE8B84A, light);
                for (int i = 0; i < 3; i++) {
                    float tw = 0.7f + 0.3f * (float) Math.sin(t * 3 + i * 2.1f);
                    box(-1.6f + i * 1.6f, -12.4f - i * 2.1f, -3.4f + i * 0.9f, 0, 0, 45, 0, 0, 0, 0.9f * tw, 0.9f * tw, 0.4f, 0xFFFFE08A, FB);
                }
            }
            case "party" -> {
                cone(1.2f, -15.6f, 0, 0, 9, 3.3f, 7.8f, CubeDraw.shade(c, 1.35f), c, CubeDraw.shade(c, 0.6f), light);
                box(1.2f, -15.9f, 0, 0, 0, 9, 0, 0, 0, 1.6f, 1.6f, 1.6f, 0xFFFFFFFF, FB);
                box(0, -8.2f, 0, 0, 0, 0, 0, 0, 0, 6.8f, 0.7f, 6.8f, 0xFFFFE08A, light);
            }
            case "santa" -> {
                box(0, -8.6f, 0, 0, 0, 0, 0, 0, 0, 9.2f, 2f, 9.2f, 0xFFF6F3FB, light);
                float flop = 24f + (float) Math.sin(t * 2) * 3f + speed * 8f;
                m.push();
                m.translate(0, -9.4f / 16f, 0);
                m.multiply(new Quaternionf().rotateZ((float) Math.toRadians(flop)));
                m.translate(0, -7.4f / 16f, 0);
                coneHere(4.4f, 7.6f, CubeDraw.shade(c, 1.15f), c, CubeDraw.shade(c, 0.6f), light);
                cubeHere(2.2f, 2.2f, 2.2f, 0xFFFFFFFF, light);
                m.pop();
            }
            case "cowboy" -> {
                int dark = CubeDraw.shade(c, 0.6f);
                box(0, -8.5f, 0, 0, 0, 0, 0, 0, 0, 8.2f, 0.8f, 12f, c, light);
                for (int s = -1; s <= 1; s += 2) {
                    box(s * 4f, -8.5f, 0, 0, 0, s * -26, s * 1.9f, 0, 0, 4f, 0.8f, 12f, c, light);
                }
                box(0, -10.6f, 0, 0, 0, 0, 0, 0, 0, 7.2f, 3.6f, 7.8f, c, light);
                box(0, -12.2f, 0, 0, 0, 0, 0, 0, 0, 5f, 0.9f, 7.8f, CubeDraw.shade(c, 0.85f), light);
                box(0, -9.4f, 0, 0, 0, 0, 0, 0, 0, 7.5f, 1f, 8.1f, dark, light);
            }
            case "beanie" -> {
                box(0, -8.3f, 0, 0, 0, 0, 0, 0, 0, 8.8f, 3.2f, 8.8f, c, light);
                box(0, -6.5f, 0, 0, 0, 0, 0, 0, 0, 9.3f, 1.8f, 9.3f, CubeDraw.shade(c, 0.75f), light);
                box(0, -10.9f, 0, 0, 0, 0, 0, 0, 0, 2.4f, 2.4f, 2.4f, 0xFFF6F3FB, light);
            }
            case "cap" -> {
                box(0, -8.4f, 0, 0, 0, 0, 0, 0, 0, 8.8f, 2.6f, 8.8f, c, light);
                box(0, -7.4f, -6.2f, 8, 0, 0, 0, 0, 0, 7.6f, 0.7f, 4.4f, CubeDraw.shade(c, 0.75f), light);
                box(0, -9.9f, 0, 0, 0, 0, 0, 0, 0, 1.1f, 0.6f, 1.1f, CubeDraw.shade(c, 0.75f), light);
                box(0, -8.5f, -4.45f, 0, 0, 0, 0, 0, 0, 2.2f, 1.4f, 0.3f, 0xFFF6F3FB, FB);
            }
            case "headphones" -> {
                int shell = 0xFF2A2D36;
                box(0, -8.8f, 0, 0, 0, 0, 0, 0, 0, 9.8f, 1.1f, 1.9f, shell, light);
                float pulse = 0.85f + 0.3f * (float) Math.sin(t * 3);
                for (int s = -1; s <= 1; s += 2) {
                    box(s * 4.75f, -6.6f, 0, 0, 0, 0, 0, 0, 0, 1.1f, 3.6f, 1.9f, shell, light);
                    box(s * 5.2f, -3.9f, 0, 0, 0, 0, 0, 0, 0, 1.9f, 4.4f, 4.4f, shell, light);
                    box(s * 6.25f, -3.9f, 0, 0, 0, 0, 0, 0, 0, 0.4f, 2.6f, 2.6f, CubeDraw.shade(c, pulse), FB);
                }
            }
            case "flower" -> {
                int[] petals = {0xFFFF9BC8, 0xFFFFE08A, 0xFFF6F3FB, 0xFFC8A8FF};
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI * 2 / 12;
                    float x = (float) Math.cos(a) * 4.7f;
                    float z = (float) Math.sin(a) * 4.7f;
                    if (k % 2 == 0) {
                        box(x, -8.5f, z, 0, (float) -Math.toDegrees(a), 0, 0, 0, 0, 1.9f, 1.9f, 1.9f, petals[(k / 2) % 4], light);
                        box(x * 1.06f, -8.5f, z * 1.06f, 0, (float) -Math.toDegrees(a), 0, 0, 0, 0, 0.8f, 0.8f, 1f, 0xFFFFC94D, light);
                    } else {
                        box(x, -8.2f, z, 0, (float) -Math.toDegrees(a), 0, 0, 0, 0, 1.4f, 0.9f, 2.4f, 0xFF4CB86A, light);
                    }
                }
            }
            case "bunny" -> {
                for (int s = -1; s <= 1; s += 2) {
                    float rot = s * (9f + (float) Math.sin(t * 1.8 + s) * 4f + speed * 10f);
                    box(s * 2.2f, -8f, 0.5f, -speed * 14f, 0, rot, 0, -3.4f, 0, 2f, 6.8f, 1.1f, c, light);
                    box(s * 2.2f, -8f, 0.2f, -speed * 14f, 0, rot, 0, -3.2f, 0, 1f, 5.2f, 1.05f, 0xFFF4A0B8, light);
                }
            }
            case "shades" -> {
                for (int s = -1; s <= 1; s += 2) {
                    box(s * 2.1f, -4f, -4.35f, 0, 0, 0, 0, 0, 0, 3.4f, 2.1f, 0.5f, c, light);
                    box(s * 2.7f, -4.5f, -4.62f, 0, 0, 0, 0, 0, 0, 0.9f, 0.5f, 0.1f, 0xFFF6F3FB, FB);
                    box(s * 4.2f, -4.4f, -2.1f, 0, 0, 0, 0, 0, 0, 0.5f, 0.6f, 4.6f, c, light);
                }
                box(0, -4.5f, -4.35f, 0, 0, 0, 0, 0, 0, 1.2f, 0.6f, 0.5f, c, light);
            }
            default -> box(0, -10, 0, 0, 0, 0, 0, 0, 0, 4, 2, 4, c, light);
        }
    }

    /** Cone with its apex at (px, py, pz) pixels, tilted by rotX/rotZ degrees; r and h in pixels. */
    private void cone(float px, float py, float pz, float rotX, float rotZ, float r, float h, int top, int rim, int under, int lt) {
        m.push();
        m.translate(px / 16f, py / 16f, pz / 16f);
        if (rotX != 0 || rotZ != 0) {
            m.multiply(new Quaternionf().rotateX((float) Math.toRadians(rotX)).rotateZ((float) Math.toRadians(rotZ)));
        }
        coneHere(r, h, top, rim, under, lt);
        m.pop();
    }

    private void coneHere(float r, float h, int top, int rim, int under, int lt) {
        cones.add(new Cone(m.peek().copy(), r / 16f, h / 16f, top, rim, under, lt));
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
        cubeHere(w, h, d, argb, lt);
        m.pop();
    }

    /** Box of w x h x d pixels centered on the current matrix origin. */
    private void cubeHere(float w, float h, float d, int argb, int lt) {
        batch.add(new Cube(m.peek().copy(), w / 32f, h / 32f, d / 32f, argb, lt));
    }
}
