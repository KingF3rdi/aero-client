package dev.aero.client;

import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Built-in copies of the public PvP optimizer mods (crystal / anchor / elytra / mace / pearl / totem).
 * Each row in the Optimizers module maps to a real client-side lag cut; the server stays authoritative.
 */
public final class Optimizer {
    private Optimizer() {}

    public static boolean master() {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.optimizersModule;
    }

    public static boolean on(String id) {
        return master() && OptimizerMods.enabled(id);
    }

    public static boolean crystal() {
        return on("marlowcrystal") || (AeroClient.CONFIG != null && AeroClient.CONFIG.crystalOptimizer);
    }

    public static boolean cutebowAnchor() {
        return on("client_side_anchors") || (AeroClient.CONFIG != null && AeroClient.CONFIG.anchorOptimizer);
    }

    public static boolean heroAnchor() {
        return on("herosanchoroptimizer");
    }

    public static boolean elytra() {
        return on("heroselytraoptimizer");
    }

    public static boolean mace() {
        return on("maceoptimizer");
    }

    public static boolean pearl() {
        return on("pearloptimizer") || (AeroClient.CONFIG != null && AeroClient.CONFIG.pearlOptimizer);
    }

    public static boolean shield() {
        return on("shieldoptimizer") || (AeroClient.CONFIG != null && AeroClient.CONFIG.shieldOptimizer);
    }

    public static boolean crossbow() {
        return on("crossbowoptimizer") || (AeroClient.CONFIG != null && AeroClient.CONFIG.crossbowOptimizer);
    }

    public static boolean totem() {
        return on("totemoptimizer");
    }

    public static void onAttackEntity(Entity target) {
        if (target == null || !crystal()) {
            return;
        }
        String n = target.getClass().getName().toLowerCase();
        if (!n.contains("endcrystal") && !n.contains("end_crystal")) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        removeLocal(target);
        try {
            if (mc.targetedEntity == target) {
                mc.targetedEntity = null;
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (player == null || world == null || hit == null || (!heroAnchor() && !cutebowAnchor())) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isIntegratedServerRunning()) {
            return;
        }
        if (!world.isClient()) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        var state = world.getBlockState(pos);
        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
            return;
        }
        if (player.isSneaking()) {
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        String held = stack.getItem().toString().toLowerCase();
        if (held.contains("glowstone")) {
            return;
        }
        int charge = 0;
        try {
            charge = state.get(RespawnAnchorBlock.CHARGES);
        } catch (Throwable ignored) {
        }
        boolean explodeDim = true;
        try {
            Object dim = world.getDimension();
            try {
                Object v = dim.getClass().getMethod("respawnAnchorWorks").invoke(dim);
                explodeDim = !Boolean.TRUE.equals(v);
            } catch (NoSuchMethodException e) {
                for (String m : new String[]{"hasRespawnAnchorWorks", "isRespawnAnchorWorks"}) {
                    try {
                        Object v = dim.getClass().getMethod(m).invoke(dim);
                        explodeDim = !Boolean.TRUE.equals(v);
                        break;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (charge <= 0 || !explodeDim) {
            return;
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
    }

    public static void tickElytra(PlayerEntity player) {
        if (!elytra() || player == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (player != mc.player) {
            return;
        }
        if (!gliding(player)) {
            return;
        }
        ItemStack chest;
        try {
            chest = player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        } catch (Throwable t) {
            return;
        }
        if (!chest.isEmpty() && chest.getItem().toString().toLowerCase().contains("elytra")) {
            return;
        }
        stopGliding(player);
    }

    public static boolean heldIs(PlayerEntity player, String needle) {
        if (player == null) {
            return false;
        }
        String main = player.getMainHandStack().getItem().toString().toLowerCase();
        String off = player.getOffHandStack().getItem().toString().toLowerCase();
        return main.contains(needle) || off.contains(needle);
    }

    private static boolean gliding(PlayerEntity player) {
        try {
            Object v = player.getClass().getMethod("isGliding").invoke(player);
            if (Boolean.TRUE.equals(v)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object v = player.getClass().getMethod("isFallFlying").invoke(player);
            return Boolean.TRUE.equals(v);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void stopGliding(PlayerEntity player) {
        for (String m : new String[]{"stopGliding", "stopFallFlying"}) {
            try {
                player.getClass().getMethod(m).invoke(player);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void removeLocal(Entity entity) {
        try {
            entity.discard();
            return;
        } catch (Throwable ignored) {
        }
        try {
            entity.remove(Entity.RemovalReason.KILLED);
        } catch (Throwable ignored) {
        }
    }
}
