package dev.aero.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

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

    private static final Map<UUID, Vec3d> lastSeenPos = new HashMap<>();
    private static final Map<UUID, Long> echoSpawnedAt = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3d> echoPos = new ConcurrentHashMap<>();

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

        if (cfg.damageTint && mc.player.hurtTime > 0) {
            int packed = cfg.damageTintColor;
            if (cfg.damageTintChroma) {
                float hue = (System.currentTimeMillis() % 4000L) / 4000f * Math.max(0.05f, cfg.damageTintSpeed) * 4f;
                packed = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue % 1f, 0.75f, 1f) & 0xFFFFFF);
            }
            int a = Math.min(220, 90 + mc.player.hurtTime * 12);
            int color = (a << 24) | (packed & 0xFFFFFF);
            drawBox(matrices, consumers, mc.player.getBoundingBox().expand(0.04), camPos, color);
        }

        if (cfg.shieldTweaks) {
            try {
                boolean firstPerson = mc.options != null && mc.options.getPerspective().isFirstPerson();
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player && firstPerson) {
                        continue;
                    }
                    Integer color = Visuals.shieldStateColor(cfg, p);
                    if (color != null) {
                        Box marker = new Box(p.getX() - 0.06, p.getEyeY() + 0.35, p.getZ() - 0.06,
                                p.getX() + 0.06, p.getEyeY() + 0.47, p.getZ() + 0.06);
                        drawBox(matrices, consumers, marker, camPos, 0xFF000000 | (color & 0xFFFFFF));
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (cfg.popChams) {
            tickPopChams(mc, cfg);
            drawPopChams(matrices, consumers, camPos, cfg);
        } else if (!lastSeenPos.isEmpty()) {
            lastSeenPos.clear();
            echoSpawnedAt.clear();
            echoPos.clear();
        }
    }

    private static void tickPopChams(MinecraftClient mc, dev.aero.client.config.ClientConfig cfg) {
        Set<UUID> seenNow = new HashSet<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player && !cfg.popChamsShowOwn) {
                continue;
            }
            seenNow.add(p.getUuid());
            lastSeenPos.put(p.getUuid(), p.getEntityPos());
        }
        for (UUID id : new HashSet<>(lastSeenPos.keySet())) {
            if (!seenNow.contains(id)) {
                // Player is gone this tick - spawn one echo at their last known spot, then forget
                // them so this doesn't keep re-spawning an echo every tick forever.
                echoSpawnedAt.put(id, System.currentTimeMillis());
                echoPos.put(id, lastSeenPos.get(id));
                lastSeenPos.remove(id);
            }
        }
    }

    private static void drawPopChams(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camPos,
                                      dev.aero.client.config.ClientConfig cfg) {
        long durationMs = 1400L;
        var it = echoSpawnedAt.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            long age = System.currentTimeMillis() - entry.getValue();
            if (age > durationMs) {
                it.remove();
                echoPos.remove(entry.getKey());
                continue;
            }
            Vec3d pos = echoPos.get(entry.getKey());
            if (pos == null) {
                continue;
            }
            float t = cfg.popChamsFadeOverTime ? 1f - (age / (float) durationMs) : 1f;
            int alpha = Math.max(0, Math.min(255, (int) (140 * t)));
            int color = (alpha << 24) | 0x00C4B5FD;
            Box box = new Box(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.8, pos.z + 0.3);
            if (cfg.popChamsWireframe) {
                drawBox(matrices, consumers, box, camPos, color);
            }
        }
    }

    private static void drawBox(MatrixStack matrices, VertexConsumerProvider consumers, Box worldBox, Vec3d camPos,
                                 int argb) {
        try {
            Box box = worldBox.offset(-camPos.x, -camPos.y, -camPos.z);
            float a = ((argb >>> 24) & 0xFF) / 255f;
            int rgb = argb & 0xFFFFFF;
            var buffer = consumers.getBuffer(RenderLayers.lines());
            VertexRendering.drawOutline(matrices, buffer, VoxelShapes.cuboid(box), 0, 0, 0, rgb, a == 0 ? 1f : a);
        } catch (Throwable ignored) {
        }
    }
}
