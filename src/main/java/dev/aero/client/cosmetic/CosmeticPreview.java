package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.Method;

public final class CosmeticPreview {
    public static volatile boolean drawing;
    private static volatile Method drawEntity;
    private static volatile boolean lookedUp;

    private CosmeticPreview() {}

    public static void player(DrawContext context, int x, int y, int size, float mouseX, float mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || context == null) {
            return;
        }
        Method method = resolve();
        if (method == null) {
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
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 10) {
                method.invoke(null, context, x1, y1, x2, y2, s, 0.0625f, mouseX, mouseY, mc.player);
            } else if (p.length == 6) {
                method.invoke(null, context, x, y, s, mouseX, mouseY, mc.player);
            }
        } catch (Throwable ignored) {
        } finally {
            drawing = false;
        }
    }

    private static Method resolve() {
        Method cached = drawEntity;
        if (lookedUp) {
            return cached;
        }
        synchronized (CosmeticPreview.class) {
            if (lookedUp) {
                return drawEntity;
            }
            try {
                Class<?> inv = Class.forName("net.minecraft.client.gui.screen.ingame.InventoryScreen");
                for (Method m : inv.getMethods()) {
                    if (!"drawEntity".equals(m.getName()) && !"drawPlayer".equals(m.getName())) {
                        continue;
                    }
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 10 && p[0] == DrawContext.class && LivingEntity.class.isAssignableFrom(p[9])) {
                        drawEntity = m;
                        break;
                    }
                    if (p.length == 6 && LivingEntity.class.isAssignableFrom(p[p.length - 1])) {
                        drawEntity = m;
                    }
                }
            } catch (Throwable ignored) {
            }
            lookedUp = true;
            return drawEntity;
        }
    }
}
