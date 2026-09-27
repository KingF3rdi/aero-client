package dev.aero.client.ui;

import dev.aero.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Module toggle notification: a white glass pill that drops in at the top center, then fades. */
public final class Notifications {
    private static final long LIFE_MS = 1600;
    private static String name;
    private static boolean on;
    private static long at;

    private Notifications() {}

    public static void toggled(Module module) {
        name = module.name;
        on = module.enabled();
        at = System.currentTimeMillis();
    }

    public static void render(DrawContext ctx, int screenW) {
        if (name == null) {
            return;
        }
        long age = System.currentTimeMillis() - at;
        if (age > LIFE_MS) {
            name = null;
            return;
        }
        var tr = MinecraftClient.getInstance().textRenderer;
        float in = Math.min(1f, age / 160f);
        float out = age > LIFE_MS - 250 ? (LIFE_MS - age) / 250f : 1f;
        float k = 1f - (1f - in) * (1f - in);
        String state = on ? "On" : "Off";
        int stateW = tr.getWidth(state) + 12;
        int w = 30 + tr.getWidth(name) + 8 + stateW + 8;
        int h = 22;
        int x = screenW / 2 - w / 2;
        int y = Math.round(-h + (8 + h) * k);
        UiDraw.fade = out;
        try {
            UiDraw.roundRect(ctx, x, y + 2, w, h, 11, 0x14000000);
            UiDraw.roundRect(ctx, x, y, w, h, 11, 0xF2FFFFFF);
            UiDraw.roundBorder(ctx, x, y, w, h, 11, 0x14000000);
            int dot = on ? 0xFF22C55E : 0xFF9CA3AF;
            UiDraw.roundRect(ctx, x + 8, y + 6, 10, 10, 5, UiDraw.withAlpha(dot, 0x40));
            UiDraw.roundRect(ctx, x + 10, y + 8, 6, 6, 3, dot);
            ctx.drawText(tr, Text.literal(name), x + 24, y + 7, UiDraw.fa(UiDraw.TEXT), false);
            int sx = x + w - 8 - stateW;
            UiDraw.roundRect(ctx, sx, y + 4, stateW, 14, 7, on ? 0x2622C55E : 0x149CA3AF);
            ctx.drawText(tr, Text.literal(state), sx + 6, y + 7, UiDraw.fa(on ? 0xFF15803D : 0xFF6B7280), false);
            int bar = Math.round((w - 22) * (1f - age / (float) LIFE_MS));
            UiDraw.roundRect(ctx, x + 11, y + h - 3, bar, 2, 1, UiDraw.withAlpha(UiDraw.accent(), 0x99));
        } finally {
            UiDraw.fade = 1f;
        }
    }
}
