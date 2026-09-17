package dev.aero.client.hud;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;
import dev.aero.client.module.Module;
import dev.aero.client.ui.UiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class OverlayHud {
    private static final int ACCENT = 0xFF4F8EFF;
    private static final int TEXT = 0xFFF4F1FA;
    private static final int MUTED = 0xFFB8B0C8;

    private static final List<Module> ENABLED = new ArrayList<>();
    private static int cachedFps;
    private static long fpsAt;
    private static String cachedMusic = "";
    private static long musicAt;
    private static float cachedSat;
    private static long satAt;

    private static boolean flying(MinecraftClient mc) {
        try {
            Object v = mc.player.getClass().getMethod("isGliding").invoke(mc.player);
            return Boolean.TRUE.equals(v);
        } catch (Throwable ignored) {
            try {
                Object v = mc.player.getClass().getMethod("isFallFlying").invoke(mc.player);
                return Boolean.TRUE.equals(v);
            } catch (Throwable ignored2) {
                return false;
            }
        }
    }

    public static void renderAny(Object context, Object tickCounter) {
        if (context instanceof DrawContext draw) {
            render(draw, tickCounter instanceof RenderTickCounter rtc ? rtc : null);
        }
    }

    public static boolean hideVanillaCrosshair() {
        ClientConfig cfg = AeroClient.CONFIG;
        if (cfg == null || !cfg.customCrosshair) {
            return false;
        }
        String style = cfg.crosshairStyle == null ? "Cross" : cfg.crosshairStyle;
        if ("Vanilla".equalsIgnoreCase(style) && !cfg.crosshairUseDrawing) {
            return false;
        }
        return true;
    }

    private static boolean usingDrawn(ClientConfig cfg) {
        if (cfg.crosshairUseDrawing && cfg.crosshairPixels != null && !cfg.crosshairPixels.isBlank()) {
            return true;
        }
        return cfg.customCrosshair && "Drawn".equalsIgnoreCase(cfg.crosshairStyle)
                && cfg.crosshairPixels != null && !cfg.crosshairPixels.isBlank();
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        try {
            renderInner(context, tickCounter);
        } catch (Throwable ignored) {
        }
    }

    private static void renderInner(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        ClientConfig cfg = AeroClient.CONFIG;
        if (cfg == null) {
            return;
        }
        boolean hudHidden = mc.options.hudHidden;
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        if (hudHidden && !(cfg.customCrosshair && cfg.crosshairWhenHidden)) {
            return;
        }
        boolean debug = mc.getDebugHud() != null && mc.getDebugHud().shouldShowDebugHud();
        if (debug) {
            drawCrosshairOverlay(context, mc, cfg, sw, sh);
            return;
        }

        if (hudHidden) {
            drawCrosshairOverlay(context, mc, cfg, sw, sh);
            return;
        }

        if (shouldWatermark(mc, cfg)) {
            drawWatermark(context, mc, cfg, sw, sh);
        }

        if (cfg.fpsHud) {
            hudLine(context, mc, cfg, cfg.fpsX, cfg.fpsY, fps(mc) + " FPS", cfg.fpsShadow);
        }
        if (cfg.pingHud) {
            hudLine(context, mc, cfg, cfg.pingX, cfg.pingY, ping(mc) + " ms", cfg.pingShadow);
        }
        if (cfg.cpsHud) {
            hudLine(context, mc, cfg, cfg.fpsX, cfg.fpsY + 12, HudStats.cps() + " CPS");
        }
        if (cfg.comboHud && HudStats.combo() > 0) {
            hudLine(context, mc, cfg, cfg.fpsX, cfg.fpsY + 24, HudStats.combo() + " Combo");
        }
        if (cfg.coordsHud) {
            hudLine(context, mc, cfg, cfg.coordsX, cfg.coordsY,
                    String.format("%.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                    cfg.coordsShadow);
        }
        if (cfg.musicPlayer) {
            String track = currentMusic(mc);
            if (!track.isBlank()) {
                hudLine(context, mc, cfg, cfg.musicX, cfg.musicY, "♪  " + track);
            }
        }
        if (cfg.sprintHud) {
            String sprint = sprintText(mc, cfg);
            if (!sprint.isBlank()) {
                hudLine(context, mc, cfg, cfg.sprintX, cfg.sprintY, sprint);
            }
        }
        if (cfg.totemCounter && cfg.totemHud) {
            int totems = countTotems(mc);
            int iconX = cfg.totemX > 0 ? cfg.totemX : sw / 2 - 8;
            int iconY = cfg.totemY > 0 ? cfg.totemY : sh - 70;
            context.drawItem(new ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING), iconX, iconY);
            String count = String.valueOf(totems);
            int cw = mc.textRenderer.getWidth(count);
            int col = cfg.totemUseColor ? (cfg.totemColor | 0xFF000000) : TEXT;
            context.drawText(mc.textRenderer, Text.literal(count), iconX + 8 - cw / 2, iconY + 18, col, true);
        }

        if (cfg.arraylist && AeroClient.MODULES != null) {
            AeroClient.MODULES.enabledInto(ENABLED);
            ENABLED.sort(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)));
            int y = 8;
            boolean lite = cfg.fastHud;
            for (Module module : ENABLED) {
                int w = mc.textRenderer.getWidth(module.name);
                int x = sw - w - 14;
                if (lite) {
                    context.fill(sw - 8, y + 2, sw - 6, y + 11, ACCENT);
                    context.drawText(mc.textRenderer, Text.literal(module.name), x, y + 2, TEXT, false);
                    y += 11;
                } else {
                    context.fill(x - 6, y, sw - 6, y + 13, 0xD214121C);
                    context.fill(sw - 8, y + 1, sw - 6, y + 12, ACCENT);
                    context.drawText(mc.textRenderer, Text.literal(module.name), x, y + 3, TEXT, true);
                    y += 14;
                }
            }
        }

        if (cfg.potionHud) {
            try {
                Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
                int y = cfg.potionY;
                for (StatusEffectInstance effect : effects) {
                    if (effect == null) {
                        continue;
                    }
                    String text = cfg.potionName ? effectName(effect) : "";
                    if (cfg.potionLevel) {
                        text += (text.isEmpty() ? "" : " ") + roman(effect.getAmplifier() + 1);
                    }
                    if (cfg.potionTimer) {
                        int sec = Math.max(0, effect.getDuration() / 20);
                        text += (text.isEmpty() ? "" : "  ") + (sec / 60) + ":" + String.format("%02d", sec % 60);
                    }
                    if (cfg.potionIcons) {
                        int col = 0xFF4F8EFF;
                        try {
                            col = 0xFF000000 | (effect.getEffectType().value().getColor() & 0xFFFFFF);
                        } catch (Throwable ignored) {
                        }
                        UiDraw.roundRect(context, cfg.potionX, y, 12, 12, 3, col);
                        hudLine(context, mc, cfg, cfg.potionX + 16, y, text);
                    } else {
                        hudLine(context, mc, cfg, cfg.potionX, y, text);
                    }
                    y += cfg.fastHud ? 13 : 17;
                }
            } catch (Throwable ignored) {
            }
        }

        if (cfg.keystrokes) {
            float scale = Math.max(0.5f, cfg.keyScale);
            int size = Math.max(12, (int) (cfg.keySize * scale));
            int gap = (int) cfg.keyGap;
            int x = cfg.keystrokesX;
            int y = cfg.keystrokesY;
            if (cfg.keyShowKeys) {
                key(context, mc, x + size + gap, y, "W", mc.options.forwardKey.isPressed(), size);
                key(context, mc, x, y + size + gap, "A", mc.options.leftKey.isPressed(), size);
                key(context, mc, x + size + gap, y + size + gap, "S", mc.options.backKey.isPressed(), size);
                key(context, mc, x + (size + gap) * 2, y + size + gap, "D", mc.options.rightKey.isPressed(), size);
            }
            if (cfg.keyShowMouse) {
                int mx = x + (size + gap) * 3 + 4;
                key(context, mc, mx, y, "LMB", mc.options.attackKey.isPressed(), size + 8);
                key(context, mc, mx, y + size + gap, "RMB", mc.options.useKey.isPressed(), size + 8);
            }
            if (cfg.keyCps) {
                context.drawText(mc.textRenderer, Text.literal(HudStats.cps() + " CPS"), x, y + (size + gap) * 2 + 4, TEXT, false);
            }
        }

        if (cfg.armorHud) {
            int x = Math.max(0, Math.min(cfg.armorHudX, sw - 24));
            int y = Math.max(0, Math.min(cfg.armorHudY, sh - 24));
            boolean horiz = "Horizontal".equalsIgnoreCase(cfg.armorLayout);
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            }) {
                ItemStack stack = mc.player.getEquippedStack(slot);
                context.fill(x - 2, y - 2, x + 18, y + 18, 0xE014121C);
                if (!stack.isEmpty()) {
                    context.drawItem(stack, x, y);
                    context.drawStackOverlay(mc.textRenderer, stack, x, y);
                }
                if (horiz) {
                    x += 22;
                } else {
                    y += 18;
                }
            }
        }

        if (cfg.saturationOverlay) {
            float sat = splashSaturation(mc);
            if (!(cfg.satHideFull && sat >= 20f)) {
                int foodX = sw / 2 + 10;
                int foodY = sh - 50;
                int a = Math.max(10, Math.min(255, (int) (cfg.satOpacity / 100f * 255)));
                int col = (cfg.satColor & 0x00FFFFFF) | (a << 24);
                for (int i = 0; i < 10; i++) {
                    int px = foodX + i * 8;
                    if (sat > i) {
                        context.fill(px, foodY, px + 6, foodY + 2, col);
                    }
                }
            }
        }

        if (cfg.itemHighlighter && cfg.highlightHotbar) {
            highlightHotbar(context, mc, cfg, sw, sh);
        }

        boolean first = true;
        try {
            first = mc.options.getPerspective().isFirstPerson();
        } catch (Throwable ignored) {
        }
        drawCrosshairOverlay(context, mc, cfg, sw, sh, first);

        if (dev.aero.client.Optimizer.elytra() && flying(mc)) {
            hudLine(context, mc, cfg, sw / 2 - 24, sh / 2 + 48, "ELYTRA");
        }
        if (cfg.crossbowTweaks) {
            renderCrossbowCharge(context, mc, cfg, sw, sh);
        }
        if (cfg.emotes && dev.aero.client.Visuals.emoteWheelOpen()) {
            renderEmoteWheel(context, mc, sw, sh);
        }
        if (cfg.motionBlur) {
            renderMotionBlur(context, mc, cfg, sw, sh);
        }
    }

    private static void drawCrosshairOverlay(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh) {
        boolean first = true;
        try {
            first = mc.options.getPerspective().isFirstPerson();
        } catch (Throwable ignored) {
        }
        drawCrosshairOverlay(context, mc, cfg, sw, sh, first);
    }

    private static void drawCrosshairOverlay(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh, boolean first) {
        boolean third = cfg.customCrosshair ? cfg.crosshairThirdPerson : (cfg.crosshairAddons && cfg.addonThirdPerson);
        if (!first && !third) {
            return;
        }
        int cx = sw / 2;
        int cy = sh / 2;
        int gap = cfg.customCrosshair ? Math.max(0, cfg.crosshairGap) : Math.max(0, cfg.addonGap);
        int arm = cfg.customCrosshair ? Math.max(2, cfg.crosshairArm) : 6;
        if (cfg.customCrosshair && cfg.crosshairDynamicAttack && mc.player.handSwinging) {
            gap += 3;
        }
        int t = 1;
        float thinAlpha = 1f;
        if (cfg.customCrosshair) {
            if (cfg.crosshairThickness < 1f) {
                // A screen pixel can't be drawn narrower than 1px, so sub-1 thickness is faked by
                // dimming the line instead - it reads as thinner even though it's still 1px wide.
                t = 1;
                thinAlpha = Math.max(0.25f, cfg.crosshairThickness);
            } else {
                t = Math.max(1, Math.round(cfg.crosshairThickness));
            }
        }
        int c = cfg.customCrosshair ? (cfg.crosshairColor | 0xFF000000) : 0xFFF6F2FF;
        if (cfg.customCrosshair && cfg.crosshairRainbow) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000f * Math.max(0.2f, cfg.crosshairRainbowSpeed);
            c = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue % 1f, 0.85f, 1f) & 0xFFFFFF);
        }
        if (cfg.customCrosshair && cfg.crosshairHover && dev.aero.client.Visuals.hoveredPlayer(mc, Math.max(1.0, cfg.crosshairHoverRange)) != null) {
            c = cfg.crosshairHoverColor | 0xFF000000;
        } else if (cfg.customCrosshair && mc.targetedEntity instanceof net.minecraft.entity.LivingEntity living) {
            if (cfg.crosshairHighlightHostiles && living instanceof net.minecraft.entity.mob.HostileEntity) {
                c = cfg.crosshairHostileColor | 0xFF000000;
            } else if (cfg.crosshairHighlightPassives && !(living instanceof net.minecraft.entity.player.PlayerEntity)
                    && !(living instanceof net.minecraft.entity.mob.HostileEntity)) {
                c = cfg.crosshairPassiveColor | 0xFF000000;
            }
        }
        if (thinAlpha < 1f) {
            int a = Math.max(40, Math.round(((c >>> 24) & 0xFF) * thinAlpha));
            c = (a << 24) | (c & 0x00FFFFFF);
        }
        if (cfg.customCrosshair && hideVanillaCrosshair()) {
            if (usingDrawn(cfg)) {
                drawCustomCrosshair(context, cx, cy, cfg.crosshairPixels, c, t);
            } else {
                drawStyledCrosshair(context, cx, cy, gap, arm, t, c, cfg);
            }
            if (cfg.crosshairCooldown) {
                drawCooldownRing(context, mc, cfg, cx, cy, arm + gap + 4);
            }
        }
        if (cfg.crosshairAddons) {
            int marker = Math.max(arm, 6) + gap;
            if (cfg.addonElytra && flying(mc)) {
                context.fill(cx - 2, cy + marker + 2, cx + 3, cy + marker + 4, ACCENT);
            }
            if (cfg.addonShield && shieldIndicatorReady(mc, cfg)) {
                context.fill(cx - marker - 4, cy - 1, cx - marker - 2, cy + 2, ACCENT);
            }
            if (cfg.addonEntity && matchesEntityAddon(mc, cfg)) {
                context.fill(cx - 1, cy - marker - 5, cx + 2, cy - marker - 2, ACCENT);
            }
            if (cfg.addonShieldBreak && HudStats.shieldBreakActive(
                    Math.max(1, cfg.addonShieldBreakDuration) * 50L, cfg.addonShieldBreakStopOnAnimEnd)) {
                context.fill(cx - 6, cy + gap + 4, cx + 6, cy + gap + 6, 0xFFE05555);
            }
            if (cfg.addonHitmarker && HudStats.hitmarkerActive(
                    Math.max(1, cfg.addonHitmarkerDuration) * 50L, cfg.addonHitmarkerStopOnAnimEnd)) {
                context.fill(cx - marker - 4, cy - 1, cx - marker - 1, cy + 2, 0xFFF6F2FF);
                context.fill(cx + marker + 1, cy - 1, cx + marker + 4, cy + 2, 0xFFF6F2FF);
            }
        }
    }

    private static void drawStyledCrosshair(DrawContext context, int cx, int cy, int gap, int arm, int t, int c, ClientConfig cfg) {
        int o = cfg.crosshairOutline ? cfg.crosshairOutlineColor : 0;
        String style = cfg.crosshairStyle == null ? "Cross" : cfg.crosshairStyle;
        if ("Circle".equalsIgnoreCase(style)) {
            int r = arm + gap;
            for (int a = 0; a < 360; a += 8) {
                double rad = Math.toRadians(a);
                int x = cx + (int) Math.round(Math.cos(rad) * r);
                int y = cy + (int) Math.round(Math.sin(rad) * r);
                context.fill(x, y, x + t, y + t, c);
            }
        } else if ("Square".equalsIgnoreCase(style)) {
            int r = arm + gap;
            fillRect(context, cx - r, cy - r, cx + r + t, cy - r + t, c, o);
            fillRect(context, cx - r, cy + r, cx + r + t, cy + r + t, c, o);
            fillRect(context, cx - r, cy - r, cx - r + t, cy + r + t, c, o);
            fillRect(context, cx + r, cy - r, cx + r + t, cy + r + t, c, o);
        } else if ("Triangle".equalsIgnoreCase(style)) {
            int r = arm + gap;
            fillRect(context, cx - r, cy + r / 2, cx + r + t, cy + r / 2 + t, c, o);
            for (int i = 0; i <= r; i++) {
                context.fill(cx - i / 2, cy - r / 2 + i, cx - i / 2 + t, cy - r / 2 + i + t, c);
                context.fill(cx + i / 2, cy - r / 2 + i, cx + i / 2 + t, cy - r / 2 + i + t, c);
            }
        } else if ("Arrow".equalsIgnoreCase(style)) {
            fillRect(context, cx, cy - arm, cx + t, cy - gap, c, o);
            fillRect(context, cx - arm / 2, cy - gap, cx, cy - gap + t, c, o);
            fillRect(context, cx + t, cy - gap, cx + arm / 2 + t, cy - gap + t, c, o);
        } else {
            fillRect(context, cx - arm, cy, cx - gap, cy + t, c, o);
            fillRect(context, cx + gap + t, cy, cx + arm + t, cy + t, c, o);
            fillRect(context, cx, cy - arm, cx + t, cy - gap, c, o);
            fillRect(context, cx, cy + gap + t, cx + t, cy + arm + t, c, o);
        }
        if (cfg.crosshairDot) {
            int d = cfg.crosshairDotColor | 0xFF000000;
            context.fill(cx, cy, cx + t, cy + t, d);
        }
    }

    private static void fillRect(DrawContext context, int x0, int y0, int x1, int y1, int c, int outline) {
        if (x1 < x0) {
            int t = x0; x0 = x1; x1 = t;
        }
        if (y1 < y0) {
            int t = y0; y0 = y1; y1 = t;
        }
        if ((outline & 0xFF000000) != 0) {
            context.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, outline);
        }
        context.fill(x0, y0, x1, y1, c);
    }

    private static void drawCooldownRing(DrawContext context, MinecraftClient mc, ClientConfig cfg, int cx, int cy, int r) {
        try {
            ItemStack stack = mc.player.getMainHandStack();
            if (stack.isEmpty()) {
                return;
            }
            float progress = mc.player.getItemCooldownManager().getCooldownProgress(stack, 0f);
            if (progress <= 0f) {
                return;
            }
            int col = cfg.crosshairCooldownColor;
            int steps = Math.max(1, (int) (360 * progress / 12));
            for (int i = 0; i < steps; i++) {
                double rad = Math.toRadians(i * 12);
                int x = cx + (int) Math.round(Math.cos(rad) * r);
                int y = cy + (int) Math.round(Math.sin(rad) * r);
                context.fill(x, y, x + 2, y + 2, col);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void drawCustomCrosshair(DrawContext context, int cx, int cy, String pixels, int color, int thickness) {
        int t = Math.max(1, thickness);
        for (String part : pixels.split(";")) {
            String[] xy = part.split(",");
            if (xy.length != 2) {
                continue;
            }
            try {
                int dx = Integer.parseInt(xy[0].trim());
                int dy = Integer.parseInt(xy[1].trim());
                context.fill(cx + dx, cy + dy, cx + dx + t, cy + dy + t, color);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /** "Factor in delay" mirrors CrosshairAddons: a shield doesn't actually raise for a few ticks. */
    private static boolean shieldIndicatorReady(MinecraftClient mc, ClientConfig cfg) {
        if (!mc.player.isUsingItem() || !mc.player.getActiveItem().getItem().toString().toLowerCase().contains("shield")) {
            return false;
        }
        if (!cfg.addonShieldFactorDelay) {
            return true;
        }
        return mc.player.getItemUseTime() >= 5;
    }

    private static boolean matchesEntityAddon(MinecraftClient mc, ClientConfig cfg) {
        if (!(mc.targetedEntity instanceof net.minecraft.entity.LivingEntity living)) {
            return false;
        }
        if (cfg.addonEntityShowAll) {
            return true;
        }
        String filter = cfg.addonEntityList == null ? "" : cfg.addonEntityList.trim();
        String id = net.minecraft.entity.EntityType.getId(living.getType()).toString().toLowerCase(java.util.Locale.ROOT);
        if (filter.isEmpty()) {
            return living instanceof net.minecraft.entity.mob.HostileEntity;
        }
        for (String part : filter.split(",")) {
            String needle = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (!needle.isEmpty() && id.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static float lastYaw;
    private static float lastPitch;

    /**
     * Best-effort approximation: Minecraft 1.21.11 removed the old shader-color/post-process hooks
     * this would normally use for a true accumulation blur, so this fakes the effect with an
     * edge-darkening vignette that grows with how fast the camera is turning.
     */
    private static void renderMotionBlur(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh) {
        if (mc.player == null) {
            return;
        }
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        float delta = Math.abs(MathHelperAngle(yaw - lastYaw)) + Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;
        float strengthMul = "High".equalsIgnoreCase(cfg.motionBlurStrength) ? 1.6f
                : "Low".equalsIgnoreCase(cfg.motionBlurStrength) ? 0.6f : 1.0f;
        int a = (int) Math.min(90, delta * 10f * strengthMul);
        if (a < 4) {
            return;
        }
        int band = Math.max(8, sw / 12);
        int color = a << 24;
        context.fill(0, 0, band, sh, color);
        context.fill(sw - band, 0, sw, sh, color);
    }

    private static float MathHelperAngle(float degrees) {
        float d = degrees % 360f;
        if (d >= 180f) {
            d -= 360f;
        } else if (d < -180f) {
            d += 360f;
        }
        return d;
    }

    private static void renderEmoteWheel(DrawContext context, MinecraftClient mc, int sw, int sh) {
        String[] presets = dev.aero.client.Visuals.EMOTE_PRESETS;
        int rowH = 16;
        int w = 140;
        int h = presets.length * rowH + 10;
        int x = sw / 2 - w / 2;
        int y = sh / 2 - h / 2;
        context.fill(x, y, x + w, y + h, 0xE014121C);
        context.fill(x, y, x + w, y + 1, ACCENT);
        for (int i = 0; i < presets.length; i++) {
            String line = (i + 1) + "  " + presets[i];
            context.drawText(mc.textRenderer, Text.literal(line), x + 8, y + 6 + i * rowH, TEXT, false);
        }
    }

    private static void renderCrossbowCharge(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh) {
        if (mc.player == null || !mc.player.isUsingItem()) {
            return;
        }
        ItemStack active = mc.player.getActiveItem();
        if (active.isEmpty() || !active.getItem().toString().toLowerCase().contains("crossbow")) {
            return;
        }
        if (!cfg.crossbowInHotbar && !cfg.crossbowInInv) {
            return;
        }
        float pullTicks = 25f;
        float progress = Math.max(0f, Math.min(1f, mc.player.getItemUseTime() / pullTicks));
        int barW = 70;
        int x = sw / 2 - barW / 2;
        int y = cfg.crossbowInHotbar ? sh - 46 : sh / 2 + 20;
        int a = Math.max(20, Math.min(255, (int) (cfg.crossbowOpacity / 100f * 255)));
        UiDraw.inset(context, x, y, barW, 5, (a << 24) | 0x1A1822);
        int filled = Math.max(1, (int) (barW * progress));
        int col = (a << 24) | (cfg.crossbowArrowColor & 0x00FFFFFF);
        context.fill(x + 1, y + 1, x + filled, y + 4, col);
    }

    private static int countTotems(MinecraftClient mc) {
        int n = 0;
        try {
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (isTotem(stack)) {
                    n += stack.getCount();
                }
            }
        } catch (Throwable ignored) {
        }
        return n;
    }

    private static boolean enchanted(ItemStack stack) {
        try {
            return stack.hasEnchantments();
        } catch (Throwable ignored) {
            try {
                Object ench = stack.getClass().getMethod("getEnchantments").invoke(stack);
                return ench != null && !ench.toString().contains("empty");
            } catch (Throwable ignored2) {
                return false;
            }
        }
    }

    private static boolean isTotem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String n = stack.getItem().toString().toLowerCase();
        return n.contains("totem");
    }

    private static int selectedHotbar(MinecraftClient mc) {
        try {
            Object inv = mc.player.getInventory();
            try {
                Object v = inv.getClass().getMethod("getSelectedSlot").invoke(inv);
                return ((Number) v).intValue();
            } catch (Throwable ignored) {
                var f = inv.getClass().getField("selectedSlot");
                return f.getInt(inv);
            }
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean matchesHighlight(ClientConfig cfg, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String filter = cfg.itemHighlighterFilter == null ? "" : cfg.itemHighlighterFilter.trim();
        if (filter.isEmpty()) {
            return pvpHighlight(cfg, stack) || (cfg.highlightEnchanted && enchanted(stack));
        }
        String name = stack.getItem().toString().toLowerCase(java.util.Locale.ROOT);
        for (String part : filter.split(",")) {
            String needle = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (!needle.isEmpty() && name.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pvpHighlight(ClientConfig cfg, ItemStack stack) {
        String n = stack.getItem().toString().toLowerCase(java.util.Locale.ROOT);
        try {
            n = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();
        } catch (Throwable ignored) {
        }
        if (cfg.highlightTotem && n.contains("totem")) {
            return true;
        }
        if (cfg.highlightCrystal && n.contains("end_crystal")) {
            return true;
        }
        if (cfg.highlightGapple && (n.contains("golden_apple") || n.contains("enchanted_golden"))) {
            return true;
        }
        if (cfg.highlightPearl && n.contains("ender_pearl")) {
            return true;
        }
        if (cfg.highlightObsidian && n.contains("obsidian")) {
            return true;
        }
        if (cfg.highlightXp && n.contains("experience_bottle")) {
            return true;
        }
        if (cfg.highlightShield && n.contains("shield")) {
            return true;
        }
        if (cfg.highlightSword && n.contains("sword")) {
            return true;
        }
        if (cfg.highlightAxe && n.contains("_axe") && !n.contains("pickaxe")) {
            return true;
        }
        if (cfg.highlightMace && n.contains("mace")) {
            return true;
        }
        if (cfg.highlightAnchor && n.contains("respawn_anchor")) {
            return true;
        }
        if (cfg.highlightGlowstone && n.contains("glowstone")) {
            return true;
        }
        if (cfg.highlightWeb && n.contains("cobweb")) {
            return true;
        }
        if (cfg.highlightPotion && n.contains("potion")) {
            return true;
        }
        return false;
    }

    private static void highlightHotbar(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh) {
        try {
            int selected = selectedHotbar(mc);
            if (selected < 0 || selected > 8) {
                selected = ((selected % 9) + 9) % 9;
            }
            int x0 = sw / 2 - 91;
            int y = sh - 23;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!matchesHighlight(cfg, stack)) {
                    continue;
                }
                int x = x0 + i * 20;
                outlineSlot(context, x, y, i == selected ? 0x884F8EFF : 0x554F8EFF);
            }
            // The offhand slot (e.g. a shield) isn't part of getInventory()'s 0-8 hotbar range, so
            // without this a spare matching item sitting in the numbered hotbar got outlined
            // instead of the one actually equipped and in use.
            ItemStack off = mc.player.getOffHandStack();
            if (matchesHighlight(cfg, off)) {
                boolean mainLeft = mc.player.getMainArm() == net.minecraft.util.Arm.LEFT;
                int ox = mainLeft ? x0 + 9 * 20 + 8 : x0 - 28;
                outlineSlot(context, ox, y, 0x884F8EFF);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void outlineSlot(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 20, y + 1, color);
        context.fill(x, y + 19, x + 20, y + 20, color);
        context.fill(x, y, x + 1, y + 20, color);
        context.fill(x + 19, y, x + 20, y + 20, color);
    }

    private static void hudLine(DrawContext context, MinecraftClient mc, ClientConfig cfg, int x, int y, String line) {
        hudLine(context, mc, cfg, x, y, line, cfg.panelShadow);
    }

    private static void hudLine(DrawContext context, MinecraftClient mc, ClientConfig cfg, int x, int y, String line, boolean shadow) {
        int w = 12 + mc.textRenderer.getWidth(line);
        UiDraw.roundRect(context, x, y, w, 14, 5, 0x9912101A);
        context.fill(x + 3, y + 3, x + 5, y + 11, cfg.panelAccent | 0xFF000000);
        context.drawText(mc.textRenderer, Text.literal(line), x + 8, y + 3, TEXT, shadow);
    }

    private static String sprintText(MinecraftClient mc, ClientConfig cfg) {
        if (cfg.sprintShowSprint && mc.player.isSprinting()) {
            return "Full".equalsIgnoreCase(cfg.sprintStyle) ? "Sprinting" : "Sprint";
        }
        if (cfg.sprintShowSneak && mc.player.isSneaking()) {
            return "Sneak";
        }
        if (cfg.sprintShowSwim && mc.player.isSwimming()) {
            return "Swim";
        }
        return "";
    }

    private static void key(DrawContext context, MinecraftClient mc, int x, int y, String label, boolean down, int size) {
        int w = Math.max(size, mc.textRenderer.getWidth(label) + 8);
        int h = Math.max(14, size);
        UiDraw.roundRect(context, x, y, w, h, 5, down ? 0xA04F8EFF : 0x6614121C);
        context.drawText(mc.textRenderer, Text.literal(label), x + 4, y + Math.max(2, h / 2 - 4), down ? 0xFF1A1024 : TEXT, false);
    }

    private static String effectName(StatusEffectInstance effect) {
        try {
            return net.minecraft.text.Text.translatable(effect.getTranslationKey()).getString();
        } catch (Throwable t) {
            try {
                var entry = effect.getEffectType();
                Object value = entry.value();
                String key = String.valueOf(value);
                int colon = key.lastIndexOf('.') + 1;
                return key.substring(Math.max(0, colon)).replace('_', ' ');
            } catch (Throwable ignored) {
                return "Effect";
            }
        }
    }

    /**
     * This resolves the current track through several layers of reflection (no stable public API
     * exposes it), including a full getMethods()/getDeclaredFields() scan in the worst case - far
     * too expensive to redo every frame. The track name doesn't change faster than once every few
     * seconds anyway, so it's only actually resolved on a timer, same idea as fps()/ping() above.
     */
    private static String currentMusic(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (now - musicAt < 1000) {
            return cachedMusic;
        }
        musicAt = now;
        cachedMusic = resolveCurrentMusic(mc);
        return cachedMusic;
    }

    private static String resolveCurrentMusic(MinecraftClient mc) {
        try {
            Object tracker = MinecraftClient.class.getMethod("getMusicTracker").invoke(mc);
            for (String field : new String[]{"current", "playing", "field_5580"}) {
                try {
                    var f = tracker.getClass().getDeclaredField(field);
                    f.setAccessible(true);
                    Object cur = f.get(tracker);
                    if (cur != null) {
                        return cur.toString().replace("minecraft:", "").replace('_', ' ');
                    }
                } catch (Throwable ignored) {
                }
            }
            for (var m : tracker.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("current")) {
                    Object cur = m.invoke(tracker);
                    if (cur != null) {
                        return cur.toString().replace("minecraft:", "").replace('_', ' ');
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Object sm = mc.getSoundManager();
            for (var f : sm.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object v = f.get(sm);
                if (v == null) {
                    continue;
                }
                String s = v.toString().toLowerCase();
                if (s.contains("music") && !s.contains("empty")) {
                    return v.toString().replace("minecraft:", "").replace('_', ' ');
                }
            }
            Object dbg = sm.getClass().getMethod("getDebugString").invoke(sm);
            if (dbg != null && dbg.toString().toLowerCase().contains("music")) {
                return dbg.toString();
            }
        } catch (Throwable ignored) {
        }
        try {
            float vol = 1f;
            for (var m : mc.options.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("sound") && m.getParameterCount() == 1) {
                    Object cat = null;
                    for (Class<?> e : m.getParameterTypes()) {
                        if (e.isEnum()) {
                            for (Object c : e.getEnumConstants()) {
                                if (c.toString().equalsIgnoreCase("MUSIC")) {
                                    cat = c;
                                }
                            }
                        }
                    }
                    if (cat != null) {
                        Object o = m.invoke(mc.options, cat);
                        if (o instanceof Number n) {
                            vol = n.floatValue();
                        }
                    }
                }
            }
            return vol > 0.01f ? "Minecraft soundtrack" : "Musik aus";
        } catch (Throwable ignored) {
        }
        return "";
    }

    /** Same reflection-avoidance idea as currentMusic(): saturation only changes on eat/regen ticks. */
    private static float splashSaturation(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (now - satAt < 250) {
            return cachedSat;
        }
        satAt = now;
        cachedSat = resolveSplashSaturation(mc);
        return cachedSat;
    }

    private static float resolveSplashSaturation(MinecraftClient mc) {
        try {
            Object food = mc.player.getClass().getMethod("getHungerManager").invoke(mc.player);
            Object v = food.getClass().getMethod("getSaturationLevel").invoke(food);
            return ((Number) v).floatValue();
        } catch (Throwable t) {
            try {
                Object food = mc.player.getClass().getMethod("getHungerManager").invoke(mc.player);
                Object v = food.getClass().getMethod("getSaturation").invoke(food);
                return ((Number) v).floatValue();
            } catch (Throwable ignored) {
                return 0;
            }
        }
    }

    private static int fps(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (now - fpsAt < 200 && cachedFps > 0) {
            return cachedFps;
        }
        fpsAt = now;
        try {
            cachedFps = mc.getCurrentFps();
        } catch (Throwable t) {
            cachedFps = 0;
        }
        if (cachedFps <= 0) {
            try {
                Object dbgObj = MinecraftClient.class.getField("fpsDebugString").get(mc);
                String dbg = dbgObj == null ? null : dbgObj.toString();
                if (dbg != null) {
                    int sp = dbg.indexOf(' ');
                    cachedFps = Integer.parseInt(sp > 0 ? dbg.substring(0, sp) : dbg.replaceAll("[^0-9]", ""));
                }
            } catch (Throwable ignored) {
            }
        }
        return cachedFps;
    }

    private static boolean shouldWatermark(MinecraftClient mc, ClientConfig cfg) {
        if (cfg == null || !cfg.watermark) {
            return false;
        }
        if (mc.currentScreen != null) {
            return true;
        }
        return mc.options == null || !mc.options.hudHidden;
    }

    private static void drawWatermark(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh) {
        String brand = (cfg.watermarkText == null || cfg.watermarkText.isBlank() ? "LARP" : cfg.watermarkText);
        int color = cfg.watermarkRainbow
                ? java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000L) / 4000f, 0.45f, 1f) | 0xFF000000
                : TEXT;
        int tw = mc.textRenderer.getWidth(brand);
        int w = 22 + tw;
        int h = 16;
        int wx = sw - w - 10;
        int wy = sh - h - 8;
        if (cfg.watermarkX > 20 || cfg.watermarkY > 40) {
            wx = cfg.watermarkX;
            wy = cfg.watermarkY;
        }
        if (cfg.watermarkBg && !cfg.fastHud) {
            UiDraw.roundRect(context, wx, wy, w, h, 8, 0xCC14121C);
            UiDraw.roundBorder(context, wx, wy, w, h, 8, 0x444F8EFF);
        }
        UiDraw.larpMark(context, wx + 3, wy + 2, 12, ACCENT);
        context.drawText(mc.textRenderer, Text.literal(brand), wx + 18, wy + 4, color, cfg.watermarkShadow);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }
}
