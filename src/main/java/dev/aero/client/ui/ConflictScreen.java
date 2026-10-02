package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.ModConflicts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Title-screen card when mods are loaded that do the same job as an Aero module you have on: lists them
 * (name + mod id), with Open mods folder / Quit game / Play anyway, and an offer to disable them
 * (renamed to .jar.disabled, the way launchers switch a mod off).
 */
public class ConflictScreen extends Screen {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final int W = 340;
    private static final int ROW = 22;
    private static boolean shown;

    private final Screen parent;
    private final List<ModConflicts.Conflict> conflicts;
    private float s = 1f;

    private ConflictScreen(Screen parent, List<ModConflicts.Conflict> conflicts) {
        super(Text.literal("Aero Client"));
        this.parent = parent;
        this.conflicts = conflicts;
    }

    /** Dev harness (DevShot): show the card for a given list. */
    public static Screen preview(Screen parent, List<ModConflicts.Conflict> list) {
        return new ConflictScreen(parent, list);
    }

    /** Call every tick: opens once per launch, on the title screen, when overlapping mods are loaded. */
    public static void tick(MinecraftClient mc) {
        if (shown || !(mc.currentScreen instanceof TitleScreen) || AeroClient.CONFIG == null) {
            return;
        }
        shown = true;
        // Normally the pre-launch window already asked before Minecraft started (not on macOS).
        if (AeroClient.CONFIG.conflictsIgnored || ModConflicts.handledBeforeStart) {
            return;
        }
        List<ModConflicts.Conflict> found = ModConflicts.find();
        if (!found.isEmpty()) {
            mc.setScreen(new ConflictScreen(mc.currentScreen, found));
        }
    }

    private int h() {
        return 50 + conflicts.size() * ROW + 70;
    }

    private float scale() {
        return Math.max(0.6f, Math.min(2.5f, Math.min(width * 0.42f / W, height * 0.86f / h())));
    }

    private int px() {
        return Math.round(width / s) / 2 - W / 2;
    }

    private int py() {
        return Math.max(4, Math.round(height / s) / 2 - h() / 2);
    }

    private boolean allDisabled() {
        return conflicts.stream().allMatch(c -> c.disabled);
    }

    private int btnW() {
        return (W - 24 - 12) / 3;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        s = scale();
        int mx = (int) (mouseX / s);
        int my = (int) (mouseY / s);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(s, s);
        try {
            renderScaled(context, mx, my);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private void renderScaled(DrawContext context, int mx, int my) {
        int x = px();
        int y = py();
        UiDraw.glass(context, x, y, W, h(), 0, 16);
        UiDraw.roundRect(context, x + 12, y + 12, 24, 24, 12, UiDraw.withAlpha(UiDraw.accent(), 0x22));
        UiDraw.aeroMark(context, x + 16, y + 16, 16, UiDraw.accent());
        context.drawText(textRenderer, Text.literal("Aero can't run alongside these mods"), x + 44, y + 14, TEXT, false);
        context.drawText(textRenderer, Text.literal("Each one duplicates an Aero feature you have on"), x + 44, y + 25, MUTED, false);

        int ry = y + 46;
        for (var c : conflicts) {
            UiDraw.roundRect(context, x + 12, ry, W - 24, ROW - 4, 7, UiDraw.LIFT);
            int idW = textRenderer.getWidth(c.id);
            context.drawText(textRenderer, Text.literal(fit(c.name, W - 48 - idW)), x + 20, ry + 5, c.disabled ? MUTED : TEXT, false);
            context.drawText(textRenderer, Text.literal(c.id), x + W - 20 - idW, ry + 5, MUTED, false);
            if (c.disabled) {
                context.fill(x + 20, ry + 9, x + 20 + textRenderer.getWidth(fit(c.name, W - 48 - idW)), ry + 10, MUTED);
            }
            ry += ROW;
        }

        int fy = ry + 4;
        context.drawText(textRenderer, Text.literal("Disable or remove them, then restart."), x + 14, fy, MUTED, false);
        String move = allDisabled() ? "✓ Disabled, takes effect after a restart" : "Disable them for me";
        boolean moveH = !allDisabled() && inside(mx, my, x + 14, fy + 11, textRenderer.getWidth(move), 10);
        context.drawText(textRenderer, Text.literal(move), x + 14, fy + 12, allDisabled() ? 0xFF16A34A : UiDraw.accent(), false);
        if (moveH) {
            context.fill(x + 14, fy + 21, x + 14 + textRenderer.getWidth(move), fy + 22, UiDraw.accent());
        }

        int by = y + h() - 30;
        String[] labels = {"Open mods folder", "Quit game", "Play anyway"};
        for (int i = 0; i < 3; i++) {
            int bx = x + 12 + i * (btnW() + 6);
            boolean hv = inside(mx, my, bx, by, btnW(), 20);
            if (i == 2) {
                UiDraw.roundRect(context, bx, by, btnW(), 20, 10, hv ? UiDraw.accent() : UiDraw.withAlpha(UiDraw.accent(), 0xDD));
            } else {
                UiDraw.roundRect(context, bx, by, btnW(), 20, 10, hv ? UiDraw.HOVER_FILL : UiDraw.LIFT);
                UiDraw.roundBorder(context, bx, by, btnW(), 20, 10, UiDraw.BORDER);
            }
            String l = fit(labels[i], btnW() - 8);
            context.drawText(textRenderer, Text.literal(l), bx + (btnW() - textRenderer.getWidth(l)) / 2, by + 6,
                    i == 2 ? 0xFFFFFFFF : TEXT, false);
        }
    }

    private String fit(String text, int maxW) {
        if (textRenderer.getWidth(text) <= maxW) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && textRenderer.getWidth(cut + "…") > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        s = scale();
        int mx = (int) (click.x() / s);
        int my = (int) (click.y() / s);
        int x = px();
        int y = py();
        int fy = y + 46 + conflicts.size() * ROW + 4;
        String move = "Disable them for me";
        if (!allDisabled() && inside(mx, my, x + 14, fy + 11, textRenderer.getWidth(move), 10)) {
            conflicts.forEach(ModConflicts::disable);
            return true;
        }
        int by = y + h() - 30;
        for (int i = 0; i < 3; i++) {
            if (!inside(mx, my, x + 12 + i * (btnW() + 6), by, btnW(), 20)) {
                continue;
            }
            switch (i) {
                case 0 -> Util.getOperatingSystem().open(FabricLoader.getInstance().getGameDir().resolve("mods"));
                case 1 -> client.scheduleStop();
                default -> close();
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
