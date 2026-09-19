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
 * Pause overlay: frosted panel, primary action, labelled shortcuts.
 */
public class PauseMenuScreen extends Screen {
    private static final int TEXT = 0xFFF6F3FB;
    private static final int MUTED = 0xFFB8B0C8;
    private static final String[] LABELS = {"Resume", "Options", "FPS", "Menu", "Quit"};
    private int hover = -1;

    public PauseMenuScreen() {
        super(Text.literal("Aero Client"));
    }

    /** Esc Menu Scale (GUI Tweaks) - independent of Minecraft's own GUI Scale option. */
    private float scale() {
        var cfg = dev.aero.client.AeroClient.CONFIG;
        if (cfg == null) {
            return 1f;
        }
        return Math.max(0.5f, Math.min(2f, cfg.escHudScale));
    }

    private int vw() {
        return Math.max(1, Math.round(width / scale()));
    }

    private int vh() {
        return Math.max(1, Math.round(height / scale()));
    }

    private int panelX() {
        return vw() / 2 - 180;
    }

    private int panelY() {
        return vh() / 2 - 78;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        float scale = scale();
        int mx = Math.round(mouseX / scale);
        int my = Math.round(mouseY / scale);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        try {
            renderScaled(context, mx, my);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private void renderScaled(DrawContext context, int mouseX, int mouseY) {
        int px = panelX();
        int py = panelY();
        UiDraw.glass(context, px, py, 360, 168, 0xC414121E, 20);

        int cx = vw() / 2;
        String title = "AERO CLIENT";
        int tw = textRenderer.getWidth(title) + 20;
        UiDraw.aeroMark(context, cx - tw / 2, py + 14, 14, UiDraw.accent());
        context.drawText(textRenderer, Text.literal(title), cx - tw / 2 + 20, py + 18, TEXT, false);
        context.drawText(textRenderer, Text.literal("Right Shift opens the client menu"),
                cx - textRenderer.getWidth("Right Shift opens the client menu") / 2, py + 36, MUTED, false);

        int bw = 220;
        int bh = 24;
        int bx = cx - bw / 2;
        int by = py + 56;
        boolean menuHover = inside(mouseX, mouseY, bx, by, bw, bh);
        UiDraw.pill(context, bx, by, bw, bh, menuHover);
        String label = "Open menu";
        context.drawText(textRenderer, Text.literal(label),
                cx - textRenderer.getWidth(label) / 2, by + 8, TEXT, false);

        hover = -1;
        int iconY = py + 96;
        int start = cx - (5 * 52) / 2;
        for (int i = 0; i < 5; i++) {
            int ix = start + i * 52;
            boolean h = inside(mouseX, mouseY, ix, iconY, 44, 48);
            if (h) {
                hover = i;
            }
            UiDraw.roundRect(context, ix, iconY, 44, 36, 8, h ? UiDraw.withAlpha(UiDraw.accent(), 0x44) : 0x2214101C);
            drawIcon(context, i, ix + 13, iconY + 9, h ? 0xFFFFFFFF : 0xFFD0D0D0);
            String cap = LABELS[i];
            context.drawText(textRenderer, Text.literal(cap),
                    ix + (44 - textRenderer.getWidth(cap)) / 2, iconY + 38, h ? TEXT : MUTED, false);
        }
    }

    private static void drawBolt(DrawContext context, int cx, int cy) {
        context.fill(cx + 2, cy - 2, cx + 5, cy + 5, UiDraw.accent());
        context.fill(cx - 3, cy + 4, cx + 5, cy + 6, UiDraw.accent());
        context.fill(cx - 2, cy + 6, cx + 1, cy + 13, UiDraw.accent());
    }

    private static void drawIcon(DrawContext context, int id, int x, int y, int color) {
        switch (id) {
            case 0 -> { // Resume: play triangle
                for (int dy = -6; dy <= 6; dy++) {
                    int w = Math.max(1, (10 - Math.abs(dy)) / 2);
                    int ry = y + 8 + dy;
                    context.fill(x + 3, ry, x + 3 + w, ry + 1, color);
                }
            }
            case 1 -> { // Options: sliders
                int track = 0x66000000 | (color & 0xFFFFFF);
                int[] knobX = {12, 5, 9};
                for (int i = 0; i < 3; i++) {
                    int ly = y + 3 + i * 5;
                    context.fill(x + 2, ly, x + 16, ly + 1, track);
                    context.fill(x + knobX[i] - 1, ly - 2, x + knobX[i] + 2, ly + 3, color);
                }
            }
            case 2 -> drawBolt(context, x + 8, y + 2);
            case 3 -> { // Menu: hamburger
                for (int i = 0; i < 3; i++) {
                    context.fill(x + 2, y + 3 + i * 5, x + 16, y + 5 + i * 5, color);
                }
            }
            default -> { // Quit: power glyph
                int cx = x + 9;
                int cy = y + 9;
                int r = 6;
                for (int a = 25; a <= 335; a += 18) {
                    double rad = Math.toRadians(a - 90);
                    int px = cx + (int) Math.round(Math.cos(rad) * r);
                    int py = cy + (int) Math.round(Math.sin(rad) * r);
                    context.fill(px, py, px + 2, py + 2, color);
                }
                context.fill(cx - 1, cy - r - 1, cx + 1, cy - 1, color);
            }
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float scale = scale();
        int mx = Math.round((float) (click.x() / scale));
        int my = Math.round((float) (click.y() / scale));
        int cx = vw() / 2;
        int py = panelY();
        int bx = cx - 110;
        int by = py + 56;
        if (inside(mx, my, bx, by, 220, 24)) {
            client.setScreen(Menus.clickGui(this, false));
            return true;
        }
        int iconY = py + 96;
        int start = cx - (5 * 52) / 2;
        for (int i = 0; i < 5; i++) {
            if (inside(mx, my, start + i * 52, iconY, 44, 48)) {
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
                    mc.player.sendMessage(Text.literal("Aero Client · FPS preset applied"), true);
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
