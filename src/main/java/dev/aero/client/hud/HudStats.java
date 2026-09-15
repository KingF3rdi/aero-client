package dev.aero.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.ItemEntity;

import java.util.ArrayDeque;

public final class HudStats {
    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static int combo;
    private static int lastHurtId = -1;
    private static int lastHurtTime;
    private static long lastHitMarkerAt;
    private static boolean wasBlockingShield;
    private static long lastShieldBreakAt;

    private HudStats() {}

    public static void click() {
        clicks.addLast(System.currentTimeMillis());
        trim();
    }

    /** Immediate-feedback mode for the Hitmarker addon: fires on the swing itself, not on confirmed damage. */
    public static void onAttackPress() {
        lastHitMarkerAt = System.currentTimeMillis();
    }

    public static int cps() {
        trim();
        return clicks.size();
    }

    private static void trim() {
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.removeFirst();
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            combo = 0;
            return;
        }
        LivingEntity target = null;
        Entity looked = client.targetedEntity;
        if (looked instanceof LivingEntity living && living.hurtTime > 0) {
            target = living;
        }
        if (target != null) {
            if (target.getId() == lastHurtId && target.hurtTime >= lastHurtTime) {
                // same swing window
            } else if (target.getId() == lastHurtId) {
                combo += 1;
                lastHitMarkerAt = System.currentTimeMillis();
            } else {
                combo = 1;
                lastHurtId = target.getId();
                lastHitMarkerAt = System.currentTimeMillis();
            }
            lastHurtTime = target.hurtTime;
        } else if (client.player.hurtTime > 8) {
            combo = 0;
            lastHurtId = -1;
        }

        boolean blocking = client.player.isBlocking();
        if (wasBlockingShield && !blocking) {
            try {
                var cooldowns = client.player.getItemCooldownManager();
                var main = client.player.getMainHandStack();
                var off = client.player.getOffHandStack();
                boolean brokenMain = !main.isEmpty() && main.getItem().toString().toLowerCase().contains("shield")
                        && cooldowns.isCoolingDown(main);
                boolean brokenOff = !off.isEmpty() && off.getItem().toString().toLowerCase().contains("shield")
                        && cooldowns.isCoolingDown(off);
                if (brokenMain || brokenOff) {
                    lastShieldBreakAt = System.currentTimeMillis();
                }
            } catch (Throwable ignored) {
            }
        }
        wasBlockingShield = blocking;
    }

    /**
     * @param durationMs      how long the shield-break flash stays visible
     * @param stopOnCooldown  if true, also hide it once the shield's cooldown has finished, even if
     *                        the duration hasn't run out yet
     */
    public static boolean shieldBreakActive(long durationMs, boolean stopOnCooldown) {
        boolean inWindow = System.currentTimeMillis() - lastShieldBreakAt < durationMs;
        if (!inWindow) {
            return false;
        }
        if (!stopOnCooldown) {
            return true;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (mc.player == null) {
                return false;
            }
            var cooldowns = mc.player.getItemCooldownManager();
            var main = mc.player.getMainHandStack();
            var off = mc.player.getOffHandStack();
            return (!main.isEmpty() && cooldowns.isCoolingDown(main))
                    || (!off.isEmpty() && cooldowns.isCoolingDown(off));
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static int combo() {
        return combo;
    }

    /**
     * @param durationMs   how long the marker stays visible
     * @param stopOnAnimEnd if true, also hide it as soon as the attack swing animation finishes,
     *                      even if the duration hasn't run out yet (matches CrosshairAddons'
     *                      "stop on animation end" option)
     */
    public static boolean hitmarkerActive(long durationMs, boolean stopOnAnimEnd) {
        boolean inWindow = System.currentTimeMillis() - lastHitMarkerAt < durationMs;
        if (!inWindow) {
            return false;
        }
        if (stopOnAnimEnd) {
            MinecraftClient mc = MinecraftClient.getInstance();
            return mc.player != null && mc.player.handSwinging;
        }
        return true;
    }

    public static boolean skipEntity(Entity entity) {
        var cfg = dev.aero.client.AeroClient.CONFIG;
        var client = MinecraftClient.getInstance();
        if (cfg == null || client.player == null || entity == null) {
            return false;
        }
        double dist = client.player.squaredDistanceTo(entity);
        if (cfg.hideArmorStands && entity instanceof ArmorStandEntity) {
            return true;
        }
        String n = entity.getClass().getName();
        if (cfg.noLightning && n.contains("Lightning")) {
            return true;
        }
        if (cfg.hideFrames && (n.contains("ItemFrame") || n.contains("Painting") || n.contains("GlowItemFrame"))) {
            return true;
        }
        if (cfg.hideFalling && n.contains("FallingBlock")) {
            return true;
        }
        if (cfg.hideXpOrbs && n.contains("ExperienceOrb") && dist > 64) {
            return true;
        }
        if ((cfg.itemLimiter && entity instanceof ItemEntity && dist > 32 * 32)
                || (cfg.hideDroppedItems && entity instanceof ItemEntity)) {
            return true;
        }
        String low = n.toLowerCase();
        if (cfg.hideTnt && (low.contains("tnt") || low.contains("primedtnt"))) {
            return true;
        }
        if (cfg.hideProjectiles && (low.contains("arrow") || low.contains("trident") || low.contains("snowball")
                || low.contains("egg") || low.contains("fireworkrocket") || low.contains("potionentity")
                || low.contains("enderpearl") || low.contains("shulkerbullet") || low.contains("llama"))) {
            return entity != client.player;
        }
        if (cfg.hidePassiveMobs && (low.contains("cow") || low.contains("pig") || low.contains("sheep")
                || low.contains("chicken") || low.contains("rabbit") || low.contains("bat")
                || low.contains("bee") || low.contains("villager") || low.contains("cat")
                || low.contains("wolf") || low.contains("horse") || low.contains("squid")
                || low.contains("fish") || low.contains("axolotl"))) {
            return true;
        }
        if (cfg.entityDistance && dist > (double) cfg.entityRange * cfg.entityRange) {
            return entity != client.player;
        }
        if (dev.aero.client.Optimizer.crystal() && cfg.crystalOptimizerRange
                && low.contains("endcrystal")
                && dist > (double) cfg.crystalOptimizerRangeValue * cfg.crystalOptimizerRangeValue) {
            return true;
        }
        return false;
    }
}
