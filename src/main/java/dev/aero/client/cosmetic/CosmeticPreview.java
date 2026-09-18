package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
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

    /** yaw in degrees: 180 faces the camera. scale = pixels per block. */
    public static void show(DrawContext context, int x1, int y1, int x2, int y2, float scale, float yaw, LivingEntity entity) {
        if (entity == null || context == null || x2 <= x1 || y2 <= y1) {
            return;
        }
        drawing = true;
        try {
            var dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
            EntityRenderState state = renderer.getAndUpdateRenderState(entity, 1.0F);
            state.light = 15728880;
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
            context.addEntity(state, scale, offset, rot, new Quaternionf(), x1, y1, x2, y2);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            drawing = false;
        }
    }

    /** Gentle sway preview for small panels (friends list). */
    public static void spin(DrawContext context, int x, int y, int size, LivingEntity entity) {
        int s = Math.max(20, Math.min(size, 64));
        float yaw = 180f + (float) Math.sin(System.currentTimeMillis() / 1500.0) * 30f;
        show(context, x - s, y - s * 2, x + s, y + 8, s, yaw, entity);
    }
}
