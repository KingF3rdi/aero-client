package dev.aero.client;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Aggressive Culling: entities and block entities fully hidden behind solid blocks are not drawn at all.
 * Visibility is a few straight rays from the camera to the object's box (center first, then its corners)
 * walked block by block; the object counts as hidden only when every ray hits an opaque full cube.
 * Results are cached per object and refreshed every few ticks, so the cost stays small with many entities.
 */
public final class Culling {
    private Culling() {}

    /** Beyond this the ray walk gets long; far objects are left to vanilla and Entity Distance. */
    private static final double MAX_DIST = 96;
    private static final int REFRESH_TICKS = 3;

    // key -> (world tick << 1) | visible. Entities and block entities in separate maps (ids and positions overlap).
    private static final Long2LongOpenHashMap ENTITIES = new Long2LongOpenHashMap();
    private static final Long2LongOpenHashMap BLOCKS = new Long2LongOpenHashMap();
    private static final BlockPos.Mutable CELL = new BlockPos.Mutable();
    private static long lastClear;

    public static boolean hideEntity(Entity e) {
        var c = AeroClient.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.aggressiveCulling || mc.world == null || mc.gameRenderer == null) {
            return false;
        }
        if (e == mc.getCameraEntity() || e == mc.player || e.isGlowing() || (c.cullKeepPlayers && e instanceof PlayerEntity)) {
            return false;
        }
        Box b = e.getBoundingBox();
        return !visible(ENTITIES, e.getId(), b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, mc);
    }

    public static boolean hideBlockEntity(BlockPos pos) {
        var c = AeroClient.CONFIG;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.aggressiveCulling || !c.cullBlockEntities || mc.world == null || mc.gameRenderer == null) {
            return false;
        }
        return !visible(BLOCKS, pos.asLong(), pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, mc);
    }

    private static boolean visible(Long2LongOpenHashMap cache, long key, double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ, MinecraftClient mc) {
        World world = mc.world;
        Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
        double cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, cz = (minZ + maxZ) / 2;
        double dx = cx - cam.x, dy = cy - cam.y, dz = cz - cam.z;
        double d2 = dx * dx + dy * dy + dz * dz;
        int near = AeroClient.CONFIG.cullMinDistance;
        if (d2 < near * near || d2 > MAX_DIST * MAX_DIST) {
            return true;
        }
        long tick = world.getTime();
        if (tick - lastClear > 200) { // drop objects that are gone; everything re-checks once
            ENTITIES.clear();
            BLOCKS.clear();
            lastClear = tick;
        }
        long cached = cache.getOrDefault(key, Long.MIN_VALUE);
        if (cached != Long.MIN_VALUE && tick - (cached >> 1) < REFRESH_TICKS) {
            return (cached & 1) == 1;
        }
        boolean seen = rayClear(world, cam, cx, cy, cz) || corners(world, cam, minX, minY, minZ, maxX, maxY, maxZ);
        cache.put(key, (tick << 1) | (seen ? 1 : 0));
        return seen;
    }

    private static boolean corners(World world, Vec3d cam, double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        double in = 0.05; // just inside the box, so a corner sitting in a wall block isn't blocked by it
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? minX + in : maxX - in;
            double y = (i & 2) == 0 ? minY + in : maxY - in;
            double z = (i & 4) == 0 ? minZ + in : maxZ - in;
            if (rayClear(world, cam, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /** Walks the blocks between the camera and the point (grid traversal); false when an opaque full cube is in the way. */
    static boolean rayClear(World world, Vec3d from, double tx, double ty, double tz) {
        double dx = tx - from.x, dy = ty - from.y, dz = tz - from.z;
        int x = MathHelper.floor(from.x), y = MathHelper.floor(from.y), z = MathHelper.floor(from.z);
        int ex = MathHelper.floor(tx), ey = MathHelper.floor(ty), ez = MathHelper.floor(tz);
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
        double ddx = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx);
        double ddy = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double ddz = dz == 0 ? Double.MAX_VALUE : Math.abs(1 / dz);
        double mx = dx == 0 ? Double.MAX_VALUE : ((sx > 0 ? x + 1 - from.x : from.x - x) * ddx);
        double my = dy == 0 ? Double.MAX_VALUE : ((sy > 0 ? y + 1 - from.y : from.y - y) * ddy);
        double mz = dz == 0 ? Double.MAX_VALUE : ((sz > 0 ? z + 1 - from.z : from.z - z) * ddz);
        for (int steps = 0; steps < 400; steps++) {
            if (mx < my && mx < mz) {
                x += sx;
                mx += ddx;
            } else if (my < mz) {
                y += sy;
                my += ddy;
            } else {
                z += sz;
                mz += ddz;
            }
            if (x == ex && y == ey && z == ez) {
                return true; // reached the target's own block
            }
            if (world.getBlockState(CELL.set(x, y, z)).isOpaqueFullCube()) {
                return false;
            }
        }
        return true;
    }
}
