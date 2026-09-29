package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders a live 3D entity model into a GUI panel at a fixed, user-controlled rotation (no
 * auto-spin). Built on the same render-state + DrawContext.addEntity primitives vanilla's
 * InventoryScreen uses, so the cosmetic feature renderer draws on it exactly like in the world.
 */
public final class CosmeticPreview {
    public static volatile boolean drawing;

    private CosmeticPreview() {}

    /** Render-state id of the menu preview: the cosmetic renderer draws the local player's cosmetics on it. */
    public static final int MENU_ID = Integer.MIN_VALUE + 17;
    private static java.util.function.Supplier<net.minecraft.entity.player.SkinTextures> skin;
    private static Object skinFor;

    /** The local player: the live entity in a world, otherwise (title screen) a model built from the account's skin. */
    public static void showSelf(DrawContext context, int x1, int y1, int x2, int y2, float scale, float yaw) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            show(context, x1, y1, x2, y2, scale, yaw, mc.player);
            return;
        }
        try {
            var profile = mc.getGameProfile();
            if (skin == null || skinFor != profile) {
                skin = mc.getSkinProvider().supplySkinTextures(profile, false);
                skinFor = profile;
            }
            PlayerEntityRenderState state = new PlayerEntityRenderState();
            state.entityType = net.minecraft.entity.EntityType.PLAYER;
            state.skinTextures = skin.get();
            state.id = MENU_ID;
            state.width = 0.6f;
            state.height = 1.8f;
            state.baseScale = 1f;
            state.age = (System.currentTimeMillis() % 1000000L) / 50f;
            draw(context, state, x1, y1, x2, y2, scale, yaw);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** yaw in degrees: 180 faces the camera. scale = pixels per block. */
    public static void show(DrawContext context, int x1, int y1, int x2, int y2, float scale, float yaw, LivingEntity entity) {
        if (entity == null || context == null || x2 <= x1 || y2 <= y1) {
            return;
        }
        drawing = true;
        try {
            var dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
            draw(context, renderer.getAndUpdateRenderState(entity, 1.0F), x1, y1, x2, y2, scale, yaw);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            drawing = false;
        }
    }

    private static void draw(DrawContext context, EntityRenderState state, int x1, int y1, int x2, int y2, float scale, float yaw) {
        if (context == null || x2 <= x1 || y2 <= y1) {
            return;
        }
        state.light = 15728880;
        state.displayName = null; // no own nametag floating over the wardrobe preview
        state.nameLabelPos = null;
        state.outlineColor = 0;
        if (state instanceof LivingEntityRenderState living && living.baseScale > 0f) {
            living.bodyYaw = yaw;
            living.relativeHeadYaw = 0f;
            living.pitch = 0f;
            living.width = living.width / living.baseScale;
            living.height = living.height / living.baseScale;
            living.baseScale = 1.0F;
        }
        Vector3f offset = new Vector3f(0f, state.height / 2f + 0.0625f, 0f);
        Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
        // Entity previews are placed in screen pixels and ignore the pose matrix, so a scaled menu
        // has to map its bounds and size through the current matrix itself.
        var m = context.getMatrices();
        org.joml.Vector2f p1 = m.transformPosition(new org.joml.Vector2f(x1, y1));
        org.joml.Vector2f p2 = m.transformPosition(new org.joml.Vector2f(x2, y2));
        float k = m.m00();
        context.addEntity(state, scale * k, offset, rot, new Quaternionf(),
                Math.round(p1.x), Math.round(p1.y), Math.round(p2.x), Math.round(p2.y));
    }

    /** Gentle sway preview for small panels (friends list). */
    public static void spin(DrawContext context, int x, int y, int size, LivingEntity entity) {
        int s = Math.max(20, Math.min(size, 64));
        float yaw = 180f + (float) Math.sin(System.currentTimeMillis() / 1500.0) * 30f;
        show(context, x - s, y - s * 2, x + s, y + 8, s, yaw, entity);
    }
}
