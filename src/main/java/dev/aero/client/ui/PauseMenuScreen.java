package dev.aero.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * NoRisk-style pause overlay: brand, Mod Menu button, icon row.
 */
public class PauseMenuScreen extends Screen {
    private static final int ACCENT = 0xFFFFFFFF;
    private int hover = -1;

    public PauseMenuScreen() {
        super(Text.literal("Larp Launcher"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = width / 2;
        int cy = height / 2 - 28;

        String left = "LARP";
        String right = "LAUNCHER";
        int gap = 28;
        int lw = textRenderer.getWidth(left);
        int rw = textRenderer.getWidth(right);
        context.drawText(textRenderer, Text.literal(left), cx - gap - lw, cy, ACCENT, false);
        drawBolt(context, cx, cy + 4);
        context.drawText(textRenderer, Text.literal(right), cx + gap, cy, ACCENT, false);

        int bw = 220;
        int bh = 22;
        int bx = cx - bw / 2;
        int by = cy + 36;
        boolean menuHover = inside(mouseX, mouseY, bx, by, bw, bh);
        context.fill(bx, by, bx + bw, by + bh, menuHover ? 0x33111111 : 0x22111111);
        context.fill(bx, by, bx + bw, by + 1, 0x33FFFFFF);
        context.fill(bx, by + bh - 1, bx + bw, by + bh, 0x33FFFFFF);
        String label = "MOD MENU";
        context.drawText(textRenderer, Text.literal(label),
                cx - textRenderer.getWidth(label) / 2, by + 7, ACCENT, false);

        int[] icons = {0, 1, 2, 3, 4};
        hover = -1;
        int iconY = by + 36;
        int start = cx - (icons.length * 28) / 2;
        for (int i = 0; i < icons.length; i++) {
            int ix = start + i * 28;
            boolean h = inside(mouseX, mouseY, ix, iconY, 18, 18);
            if (h) {
                hover = i;
            }
            drawIcon(context, i, ix, iconY, h ? 0xFFFFFFFF : 0xFFD0D0D0);
        }
    }

    private static void drawBolt(DrawContext context, int cx, int cy) {
        context.fill(cx + 2, cy - 2, cx + 5, cy + 5, 0xFFFFFFFF);
        context.fill(cx - 3, cy + 4, cx + 5, cy + 6, 0xFFFFFFFF);
        context.fill(cx - 2, cy + 6, cx + 1, cy + 13, 0xFFFFFFFF);
    }

    private static void drawIcon(DrawContext context, int id, int x, int y, int color) {
        switch (id) {
            case 0 -> { // camera
                context.fill(x + 2, y + 5, x + 16, y + 14, color);
                context.fill(x + 6, y + 3, x + 12, y + 6, color);
            }
            case 1 -> { // shirt / cosmetics
                context.fill(x + 4, y + 3, x + 14, y + 6, color);
                context.fill(x + 5, y + 6, x + 13, y + 15, color);
            }
            case 2 -> drawBolt(context, x + 8, y + 2);
            case 3 -> { // person / settings
                context.fill(x + 7, y + 3, x + 11, y + 7, color);
                context.fill(x + 5, y + 8, x + 13, y + 15, color);
            }
            default -> { // leave
                context.fill(x + 5, y + 3, x + 13, y + 15, color);
                context.fill(x + 8, y + 6, x + 15, y + 12, color);
            }
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int cx = width / 2;
        int cy = height / 2 - 28;
        int bx = cx - 110;
        int by = cy + 36;
        if (inside(mx, my, bx, by, 220, 22)) {
            client.setScreen(Menus.clickGui(this, false));
            return true;
        }
        int iconY = by + 36;
        int start = cx - (5 * 28) / 2;
        for (int i = 0; i < 5; i++) {
            if (inside(mx, my, start + i * 28, iconY, 18, 18)) {
                onIcon(i);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void onIcon(int id) {
        MinecraftClient mc = client;
        if (mc == null) {
            return;
        }
        switch (id) {
            case 0 -> mc.setScreen(null);
            case 1 -> mc.setScreen(new OptionsScreen(this, mc.options));
            case 2 -> {
                if (dev.aero.client.AeroClient.MODULES != null) {
                    dev.aero.client.AeroClient.MODULES.applyFpsPreset();
                }
                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("Larp Launcher · FPS preset applied"), true);
                }
                mc.setScreen(null);
            }
            case 3 -> mc.setScreen(Menus.clickGui(this, false));
            default -> {
                try {
                    mc.disconnect(new TitleScreen(), false);
                } catch (Throwable t) {
                    mc.setScreen(new TitleScreen());
                }
            }
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(null);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
