package dev.aero.client.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

import java.lang.reflect.Method;

public final class CosmeticPreview {
    private CosmeticPreview() {}

    public static void player(DrawContext context, int x, int y, int size, float mouseX, float mouseY) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        try {
            Class<?> inv = Class.forName("net.minecraft.client.gui.screen.ingame.InventoryScreen");
            for (Method m : inv.getMethods()) {
                if (!m.getName().equals("drawEntity") && !m.getName().equals("drawPlayer")) {
                    continue;
                }
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 10 && p[0] == DrawContext.class) {
                    m.invoke(null, context, x - size, y - size * 2, x + size, y + 10, size, 0.0625f, mouseX, mouseY, mc.player);
                    return;
                }
                if (p.length == 6 && LivingEntity.class.isAssignableFrom(p[p.length - 1])) {
                    m.invoke(null, context, x, y, size, mouseX, mouseY, mc.player);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
