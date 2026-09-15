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
    private static final int ACCENT = 0xFFC4B5FD;
    private static final int TEXT = 0xFFF4F1FA;
    private static final int MUTED = 0xFFB8B0C8;

    private static final List<Module> ENABLED = new ArrayList<>();
    private static int cachedFps;
    private static long fpsAt;

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

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        try {
            renderInner(context, tickCounter);
        } catch (Throwable ignored) {
        }
    }

    private static void renderInner(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        if (mc.getDebugHud() != null && mc.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        ClientConfig cfg = AeroClient.CONFIG;
        if (cfg == null) {
            return;
        }
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        if (cfg.watermark) {
            String brand = (cfg.watermarkText == null || cfg.watermarkText.isBlank() ? "LARP" : cfg.watermarkText)
                    + "  " + AeroClient.VERSION;
            if (cfg.watermarkSubtitle != null && !cfg.watermarkSubtitle.isBlank()) {
                brand = brand + "  " + cfg.watermarkSubtitle;
            }
            int wx = cfg.watermarkX;
            int wy = cfg.watermarkY;
            int color = cfg.watermarkRainbow
                    ? java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000L) / 4000f, 0.45f, 1f) | 0xFF000000
                    : TEXT;
            if (cfg.fastHud || !cfg.watermarkBg) {
                context.drawText(mc.textRenderer, Text.literal(brand), wx, wy + 4, color, cfg.watermarkShadow);
            } else {
                int w = 26 + mc.textRenderer.getWidth(brand);
                UiDraw.card(context, wx, wy, w, 18, 0xE014121C, true);
                context.drawTextWithShadow(mc.textRenderer, Text.literal(brand), wx + 8, wy + 5, color);
            }
        }

        if (cfg.fpsHud) {
            hudLine(context, mc, cfg, cfg.fpsX, cfg.fpsY, fps(mc) + " FPS");
        }
        if (cfg.pingHud) {
            hudLine(context, mc, cfg, cfg.pingX, cfg.pingY, ping(mc) + " ms");
        }
        if (cfg.cpsHud) {
            hudLine(context, mc, cfg, cfg.fpsX, cfg.fpsY + 12, HudStats.cps() + " CPS");
        }
        if (cfg.comboHud && HudStats.combo() > 0) {
            hudLine(context, mc, cfg, cfg.fpsX, cfg.fpsY + 24, HudStats.combo() + " Combo");
        }
        if (cfg.coordsHud) {
            hudLine(context, mc, cfg, cfg.coordsX, cfg.coordsY,
                    String.format("%.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
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
            if (totems > 0) {
                int iconX = sw / 2 - 8;
                int iconY = sh - 40;
                context.drawItem(new ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING), iconX, iconY);
                String count = String.valueOf(totems);
                int cw = mc.textRenderer.getWidth(count);
                context.drawText(mc.textRenderer, Text.literal(count), sw / 2 - cw / 2, iconY + 18, TEXT, true);
            }
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
                    String text = "";
                    if (cfg.potionName) {
                        text = effectName(effect);
                    }
                    if (cfg.potionLevel) {
                        text += (text.isEmpty() ? "" : " ") + (effect.getAmplifier() + 1);
                    }
                    if (cfg.potionTimer) {
                        int sec = effect.getDuration() / 20;
                        text += (text.isEmpty() ? "" : "  ") + (sec / 60) + ":" + String.format("%02d", sec % 60);
                    }
                    if (text.isBlank()) {
                        continue;
                    }
                    hudLine(context, mc, cfg, cfg.potionX, y, text);
                    y += cfg.fastHud ? 11 : 17;
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
        if ((cfg.customCrosshair || cfg.crosshairAddons) && (first || (cfg.crosshairAddons && cfg.addonThirdPerson))) {
            int cx = sw / 2;
            int cy = sh / 2;
            int gap = cfg.customCrosshair ? Math.max(0, cfg.crosshairGap) : (cfg.crosshairAddons ? Math.max(0, cfg.addonGap) : 2);
            int arm = cfg.customCrosshair ? Math.max(2, cfg.crosshairArm) : (cfg.crosshairAddons ? 6 : 5);
            int c = cfg.customCrosshair ? (cfg.crosshairColor | 0xFF000000) : 0xFFF6F2FF;
            if (cfg.crosshairHover && dev.aero.client.Visuals.hoveredPlayer(mc, Math.max(1.0, cfg.crosshairHoverRange)) != null) {
                c = cfg.crosshairHoverColor | 0xFF000000;
            }
            int o = 0xCC120E1A;
            context.fill(cx - arm - 1, cy, cx - gap + 1, cy + 2, o);
            context.fill(cx + gap, cy, cx + arm + 2, cy + 2, o);
            context.fill(cx, cy - arm - 1, cx + 2, cy - gap + 1, o);
            context.fill(cx, cy + gap, cx + 2, cy + arm + 2, o);
            context.fill(cx - arm, cy, cx - gap, cy + 1, c);
            context.fill(cx + gap + 1, cy, cx + arm + 1, cy + 1, c);
            context.fill(cx, cy - arm, cx + 1, cy - gap, c);
            context.fill(cx, cy + gap + 1, cx + 1, cy + arm + 1, c);
            if (cfg.crosshairAddons) {
                if (cfg.addonElytra && flying(mc)) {
                    context.fill(cx - 2, cy + 8, cx + 3, cy + 10, ACCENT);
                }
                if (cfg.addonShield && shieldIndicatorReady(mc, cfg)) {
                    context.fill(cx - 6, cy - 1, cx - 4, cy + 2, ACCENT);
                }
                if (cfg.addonEntity && matchesEntityAddon(mc, cfg)) {
                    context.fill(cx - 1, cy - arm - 5, cx + 2, cy - arm - 2, ACCENT);
                }
                if (cfg.addonShieldBreak && HudStats.shieldBreakActive(
                        Math.max(1, cfg.addonShieldBreakDuration) * 50L, cfg.addonShieldBreakStopOnAnimEnd)) {
                    context.fill(cx - 6, cy + gap + 4, cx + 6, cy + gap + 6, 0xFFE05555);
                }
                if (cfg.addonHitmarker && HudStats.hitmarkerActive(
                        Math.max(1, cfg.addonHitmarkerDuration) * 50L, cfg.addonHitmarkerStopOnAnimEnd)) {
                    context.fill(cx - arm - 4, cy - 1, cx - arm - 1, cy + 2, 0xFFF6F2FF);
                    context.fill(cx + arm + 1, cy - 1, cx + arm + 4, cy + 2, 0xFFF6F2FF);
                }
            }
        }

        if (cfg.shieldTweaks) {
            renderShieldStatus(context, mc, cfg, sw, sh);
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

    private static ItemStack findShield(net.minecraft.client.network.AbstractClientPlayerEntity player) {
        for (ItemStack stack : new ItemStack[]{player.getMainHandStack(), player.getOffHandStack()}) {
            if (!stack.isEmpty() && stack.getItem().toString().toLowerCase().contains("shield")) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void renderShieldStatus(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh) {
        if (mc.player == null) {
            return;
        }
        ItemStack shield = findShield(mc.player);
        if (shield.isEmpty()) {
            return;
        }
        boolean blocking;
        boolean disabled;
        try {
            disabled = mc.player.getItemCooldownManager().isCoolingDown(shield);
        } catch (Throwable t) {
            disabled = false;
        }
        blocking = mc.player.isBlocking();

        String label;
        int color;
        if (disabled && cfg.shieldDisabled) {
            label = "Disabled";
            color = cfg.shieldDisabledColor;
        } else if (blocking && cfg.shieldBlocking) {
            label = "Blocking";
            color = cfg.shieldBlockingColor;
        } else if (!disabled && cfg.shieldReady) {
            label = "Shield Ready";
            color = cfg.shieldReadyColor;
        } else {
            return;
        }

        int a = Math.max(10, Math.min(255, (int) (cfg.shieldOpacity / 100f * 255)));
        int barW = 74;
        int x = sw / 2 - barW / 2;
        int y = sh / 2 + 34;
        int rgb = color & 0x00FFFFFF;
        context.fill(x, y, x + barW, y + 14, (Math.min(255, a) << 24) | 0x00101018);
        context.fill(x, y, x + barW, y + 1, ((a) << 24) | rgb);
        context.fill(x + 2, y + 2, x + 6, y + 12, (a << 24) | rgb);
        context.drawText(mc.textRenderer, Text.literal(label), x + 11, y + 4, (a << 24) | 0x00FFFFFF, cfg.panelShadow);
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

    private static boolean matchesHighlight(ClientConfig cfg, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String filter = cfg.itemHighlighterFilter == null ? "" : cfg.itemHighlighterFilter.trim();
        if (filter.isEmpty()) {
            return enchanted(stack) || isTotem(stack);
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

    private static void highlightHotbar(DrawContext context, MinecraftClient mc, ClientConfig cfg, int sw, int sh) {
        try {
            int selected = selectedHotbar(mc);
            int x0 = sw / 2 - 91;
            int y = sh - 22;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!matchesHighlight(cfg, stack)) {
                    continue;
                }
                int x = x0 + i * 20;
                int color = i == selected ? 0x88C4B5FD : 0x55C4B5FD;
                context.fill(x, y, x + 20, y + 1, color);
                context.fill(x, y + 19, x + 20, y + 20, color);
                context.fill(x, y, x + 1, y + 20, color);
                context.fill(x + 19, y, x + 20, y + 20, color);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void hudLine(DrawContext context, MinecraftClient mc, ClientConfig cfg, int x, int y, String line) {
        if (cfg.fastHud) {
            context.fill(x, y + 2, x + 2, y + 12, ACCENT);
            context.drawText(mc.textRenderer, Text.literal(line), x + 5, y + 3, TEXT, cfg.panelShadow);
            return;
        }
        int w = 12 + mc.textRenderer.getWidth(line);
        context.fill(x, y, x + w, y + 14, 0xCC12101A);
        context.fill(x, y + 2, x + 2, y + 12, cfg.panelAccent | 0xFF000000);
        context.drawText(mc.textRenderer, Text.literal(line), x + 6, y + 3, TEXT, true);
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
        context.fill(x, y, x + w, y + h, down ? 0xE8C4B5FD : 0xD214121C);
        context.drawText(mc.textRenderer, Text.literal(label), x + 4, y + Math.max(2, h / 2 - 4), down ? 0xFF1A1024 : TEXT, !down);
    }

    private static String effectName(StatusEffectInstance effect) {
        try {
            Object type = effect.getClass().getMethod("getEffectType").invoke(effect);
            if (type == null) {
                type = effect.getClass().getMethod("getEffect").invoke(effect);
            }
            String text = String.valueOf(type);
            try {
                Object id = type.getClass().getMethod("getIdAsString").invoke(type);
                text = String.valueOf(id);
            } catch (Throwable ignored) {
                try {
                    Object key = type.getClass().getMethod("getKey").invoke(type);
                    text = String.valueOf(key);
                } catch (Throwable ignored2) {
                }
            }
            int colon = text.indexOf(':');
            return colon >= 0 ? text.substring(colon + 1) : text;
        } catch (Throwable t) {
            return "effect";
        }
    }

    private static String currentMusic(MinecraftClient mc) {
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

    private static float splashSaturation(MinecraftClient mc) {
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
        if (now - fpsAt < 200) {
            return cachedFps;
        }
        fpsAt = now;
        try {
            cachedFps = (int) MinecraftClient.class.getMethod("getCurrentFps").invoke(mc);
        } catch (Throwable t) {
            try {
                cachedFps = (int) MinecraftClient.class.getMethod("getFps").invoke(mc);
            } catch (Throwable ignored) {
                cachedFps = 0;
            }
        }
        return cachedFps;
    }

    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }
}
