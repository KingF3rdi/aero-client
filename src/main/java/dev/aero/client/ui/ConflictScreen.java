package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.ModConflicts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Title-screen notice: mods that duplicate an Aero module, each with a Remove button (applied when the game closes). */
public class ConflictScreen extends Screen {
    private static final int TEXT = 0xFFF6F3FB;
    private static final int MUTED = 0xFFB8B0C8;
    private static final int ROW = 26;
    private static boolean shown;

    private final Screen parent;
    private final List<ModConflicts.Conflict> conflicts;

    private ConflictScreen(Screen parent, List<ModConflicts.Conflict> conflicts) {
        super(Text.literal("Aero Client"));
        this.parent = parent;
        this.conflicts = conflicts;
    }

    /** Call every tick: opens once per launch, on the title screen, when overlapping mods are loaded. */
    public static void tick(MinecraftClient mc) {
        if (shown || !(mc.currentScreen instanceof TitleScreen) || AeroClient.CONFIG == null) {
            return;
        }
        shown = true;
        if (AeroClient.CONFIG.conflictsIgnored) {
            return;
        }
        if (AeroClient.MODULES == null) {
            return;
        }
        List<ModConflicts.Conflict> found = ModConflicts.find(AeroClient.MODULES.all);
        if (!found.isEmpty()) {
            mc.setScreen(new ConflictScreen(mc.currentScreen, found));
        }
    }

    private int panelW() {
        return 380;
    }

    private int panelH() {
        return 96 + conflicts.size() * ROW + 34;
    }

    private int px() {
        return width / 2 - panelW() / 2;
    }

    private int py() {
        return Math.max(8, height / 2 - panelH() / 2);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int px = px();
        int py = py();
        UiDraw.glass(context, px, py, panelW(), panelH(), 0xE014121E, 20);
        UiDraw.aeroMark(context, px + 16, py + 14, 14, UiDraw.accent());
        context.drawText(textRenderer, Text.literal("Overlapping mods are active"), px + 36, py + 18, TEXT, false);
        context.drawText(textRenderer, Text.literal("These do the same job as an Aero module you have switched on."), px + 16, py + 40, MUTED, false);
        context.drawText(textRenderer, Text.literal("Disabled mods move to mods/aero-removed when you close the game."), px + 16, py + 52, MUTED, false);

        int y = py + 72;
        for (var c : conflicts) {
            UiDraw.roundRect(context, px + 12, y, panelW() - 24, ROW - 4, 8, 0x2214101C);
            context.drawText(textRenderer, Text.literal(c.name), px + 22, y + 4, TEXT, false);
            context.drawText(textRenderer, Text.literal("replaced by " + c.module), px + 22, y + 13, MUTED, false);
            String label = c.removed ? "Disabled on exit" : "Disable";
            int bw = textRenderer.getWidth(label) + 16;
            int bx = px + panelW() - 20 - bw;
            UiDraw.pill(context, bx, y + 3, bw, 16, !c.removed && inside(mouseX, mouseY, bx, y + 3, bw, 16));
            context.drawText(textRenderer, Text.literal(label), bx + 8, y + 7, c.removed ? MUTED : UiDraw.accent(), false);
            y += ROW;
        }

        int by = py + panelH() - 28;
        button(context, mouseX, mouseY, px + 12, by, 110, "Disable all", true);
        button(context, mouseX, mouseY, px + 128, by, 100, "Keep", false);
        button(context, mouseX, mouseY, px + 234, by, 134, "Don't ask again", false);
    }

    private void button(DrawContext context, int mx, int my, int x, int y, int w, String label, boolean primary) {
        UiDraw.pill(context, x, y, w, 20, inside(mx, my, x, y, w, 20));
        context.drawText(textRenderer, Text.literal(label), x + (w - textRenderer.getWidth(label)) / 2, y + 6,
                primary ? UiDraw.accent() : TEXT, false);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int px = px();
        int py = py();
        int y = py + 72;
        for (var c : conflicts) {
            String label = c.removed ? "Disabled on exit" : "Disable";
            int bw = textRenderer.getWidth(label) + 16;
            if (!c.removed && inside(mx, my, px + panelW() - 20 - bw, y + 3, bw, 16)) {
                ModConflicts.remove(c);
                return true;
            }
            y += ROW;
        }
        int by = py + panelH() - 28;
        if (inside(mx, my, px + 12, by, 110, 20)) {
            conflicts.forEach(ModConflicts::remove);
            return true;
        }
        if (inside(mx, my, px + 128, by, 100, 20)) {
            close();
            return true;
        }
        if (inside(mx, my, px + 234, by, 134, 20)) {
            AeroClient.CONFIG.conflictsIgnored = true;
            AeroClient.CONFIG.save();
            close();
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
