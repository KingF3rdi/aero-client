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
 * Renders a live 3D entity model into a GUI panel, slowly auto-spinning instead of the vanilla
 * inventory preview's cursor-follow (which only tracks the mouse across a ~60 degree window, not a
 * full rotation). Built directly on the same render-state + DrawContext.addEntity primitives that
 * InventoryScreen.drawEntity itself uses internally, so it works for any live LivingEntity - our
 * own player, or another player found in the world (see WardrobeScreen usage) - without needing
 * that method's mouse-driven rotation math or the reflection this used to go through.
 */
public final class CosmeticPreview {
    public static volatile boolean drawing;

    private CosmeticPreview() {}

    /** Own player preview - kept for compatibility with existing call sites (mouseX/Y unused now). */
    public static void player(DrawContext context, int x, int y, int size, float mouseX, float mouseY) {
        spin(context, x, y, size, MinecraftClient.getInstance().player);
    }

    /** Preview for any live LivingEntity, e.g. an online friend found via world.getPlayers(). */
    public static void spin(DrawContext context, int x, int y, int size, LivingEntity entity) {
        if (entity == null || context == null) {
            return;
        }
        int s = Math.max(20, Math.min(size, 64));
        int x1 = x - s;
        int y1 = y - s * 2;
        int x2 = x + s;
        int y2 = y + 8;
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        drawing = true;
        try {
            var dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
            EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
            EntityRenderState state = renderer.getAndUpdateRenderState(entity, 1.0F);
            state.light = 15728880;
            state.outlineColor = 0;
            // One full turn every 9s - slow enough to read as idle rather than a distracting spin.
            float spinYaw = (System.currentTimeMillis() % 9000L) / 9000f * 360f;
            if (state instanceof LivingEntityRenderState living) {
                living.bodyYaw = spinYaw;
                living.relativeHeadYaw = 0f;
                living.pitch = 0f;
                living.width = living.width / living.baseScale;
                living.height = living.height / living.baseScale;
                living.baseScale = 1.0F;
            }
            Vector3f offset = new Vector3f(0f, state.height / 2f + 0.0625f, 0f);
            Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf tilt = new Quaternionf();
            context.addEntity(state, s, offset, rot, tilt, x1, y1, x2, y2);
        } catch (Throwable ignored) {
        } finally {
            drawing = false;
        }
    }
}
