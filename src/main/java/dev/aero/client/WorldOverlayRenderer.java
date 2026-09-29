package dev.aero.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws Hitboxes, Block Overlay and Pop Chams inside the normal world render pass (via Fabric's
 * WorldRenderEvents), so everything here is depth-tested exactly like any other world geometry -
 * never visible through walls/terrain. Uses the 1.21.11 rendering API (RenderLayers.lines() /
 * VertexRendering.drawOutline()), which replaced the older RenderLayer.getLines()-style calls.
 */
public final class WorldOverlayRenderer {
    private WorldOverlayRenderer() {}

    public static void register() {
        try {
            WorldRenderEvents.AFTER_ENTITIES.register(WorldOverlayRenderer::afterEntities);
        } catch (Throwable ignored) {
        }
    }

    private static void afterEntities(WorldRenderContext context) {
        var cfg = AeroClient.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || mc.world == null || mc.player == null) {
            return;
        }
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }
        Vec3d camPos = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
                ? mc.gameRenderer.getCamera().getCameraPos() : Vec3d.ZERO;

        if (cfg.hitboxes) {
            for (Entity e : mc.world.getEntities()) {
                if (Visuals.showHitbox(e)) {
                    drawBox(matrices, consumers, e.getBoundingBox(), camPos, cfg.hitboxColor);
                }
            }
        }

        if (cfg.blockOverlay && cfg.blockOverlayOutline
                && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            Box box = new Box(hit.getBlockPos());
            drawBox(matrices, consumers, box.expand(0.002), camPos, cfg.blockOverlayColor);
        }

        if (cfg.popChams) {
            drawPopChams(matrices, consumers, camPos, cfg);
        } else if (!POPS.isEmpty()) {
            POPS.clear();
        }
    }

    private record Pop(Vec3d pos, float yaw, long at) {}

    private static final java.util.List<Pop> POPS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Records where a player's totem popped so Pop Chams can outline them there. */
    public static void onPop(net.minecraft.entity.LivingEntity e) {
        var cfg = AeroClient.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || !cfg.popChams || !(e instanceof PlayerEntity) || (e == mc.player && !cfg.popChamsShowOwn)) {
            return;
        }
        POPS.add(new Pop(e.getEntityPos(), e.bodyYaw, System.currentTimeMillis()));
    }

    /** Player-shaped outline (head, body, arms, legs) at each recent pop, fading out over the set duration. */
    private static void drawPopChams(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camPos,
                                      dev.aero.client.config.ClientConfig cfg) {
        long durationMs = (long) (Math.max(0.2f, cfg.popChamsSeconds) * 1000f);
        long now = System.currentTimeMillis();
        POPS.removeIf(p -> now - p.at() > durationMs);
        for (Pop p : POPS) {
            float t = cfg.popChamsFadeOverTime ? 1f - ((now - p.at()) / (float) durationMs) : 1f;
            int alpha = Math.max(8, Math.min(255, (int) (255 * t)));
            int rgb = cfg.popChamsColor & 0xFFFFFF;
            try {
                matrices.push();
                matrices.translate(p.pos().x - camPos.x, p.pos().y - camPos.y, p.pos().z - camPos.z);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f - p.yaw()));
                var buffer = consumers.getBuffer(RenderLayers.lines());
                double[][] parts = {
                        {-0.25, 1.4, -0.25, 0.25, 1.9, 0.25},   // head (over 1.8 like the model's 8px head + hat)
                        {-0.25, 0.7, -0.125, 0.25, 1.4, 0.125}, // body
                        {-0.5, 0.7, -0.125, -0.25, 1.4, 0.125}, // arms
                        {0.25, 0.7, -0.125, 0.5, 1.4, 0.125},
                        {-0.25, 0.0, -0.125, 0.0, 0.7, 0.125},  // legs
                        {0.0, 0.0, -0.125, 0.25, 0.7, 0.125}};
                for (double[] q : parts) {
                    boxLines(matrices.peek(), buffer, q[0], q[1], q[2], q[3], q[4], q[5], (alpha << 24) | rgb, 3f);
                }
                matrices.pop();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void drawBox(MatrixStack matrices, VertexConsumerProvider consumers, Box worldBox, Vec3d camPos,
                                 int argb) {
        try {
            Box box = worldBox.offset(-camPos.x, -camPos.y, -camPos.z);
            // 1.21.11: the color is ARGB (alpha 0 = invisible) and the last argument is the line width.
            int alpha = (argb >>> 24) & 0xFF;
            int color = ((alpha == 0 ? 0xFF : alpha) << 24) | (argb & 0xFFFFFF);
            var buffer = consumers.getBuffer(RenderLayers.lines());
            boxLines(matrices.peek(), buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, color, 2.5f);
        } catch (Throwable t) {
            
        }
    }

    /**
     * The 12 edges of a box, the same vertices VertexRendering.drawOutline emits. drawOutline wants a
     * VoxelShape, and VoxelShapes.cuboid with off-grid (world) coordinates builds a fresh shape per box
     * per frame - that alone was the biggest cost Aero added to a frame with Hitboxes on.
     */
    private static void boxLines(MatrixStack.Entry e, net.minecraft.client.render.VertexConsumer b,
                                 double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) {
        float a = (float) x1, bb = (float) y1, c = (float) z1, d = (float) x2, f = (float) y2, g = (float) z2;
        line(e, b, a, bb, c, d, bb, c, 1, 0, 0, color, width);
        line(e, b, a, bb, g, d, bb, g, 1, 0, 0, color, width);
        line(e, b, a, f, c, d, f, c, 1, 0, 0, color, width);
        line(e, b, a, f, g, d, f, g, 1, 0, 0, color, width);
        line(e, b, a, bb, c, a, f, c, 0, 1, 0, color, width);
        line(e, b, d, bb, c, d, f, c, 0, 1, 0, color, width);
        line(e, b, a, bb, g, a, f, g, 0, 1, 0, color, width);
        line(e, b, d, bb, g, d, f, g, 0, 1, 0, color, width);
        line(e, b, a, bb, c, a, bb, g, 0, 0, 1, color, width);
        line(e, b, d, bb, c, d, bb, g, 0, 0, 1, color, width);
        line(e, b, a, f, c, a, f, g, 0, 0, 1, color, width);
        line(e, b, d, f, c, d, f, g, 0, 0, 1, color, width);
    }

    private static void line(MatrixStack.Entry e, net.minecraft.client.render.VertexConsumer b, float x1, float y1, float z1,
                             float x2, float y2, float z2, float nx, float ny, float nz, int color, float width) {
        b.vertex(e, x1, y1, z1).color(color).normal(e, nx, ny, nz).lineWidth(width);
        b.vertex(e, x2, y2, z2).color(color).normal(e, nx, ny, nz).lineWidth(width);
    }
}
