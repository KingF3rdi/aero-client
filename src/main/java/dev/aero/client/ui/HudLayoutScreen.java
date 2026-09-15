package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Drag-to-reposition editor for the HUD elements that already have their own X/Y config fields.
 * Only lists elements whose position is actually read by OverlayHud - dragging a chip here moves
 * the real thing, nothing decorative.
 */
public class HudLayoutScreen extends Screen {
    private static final int TEXT = 0xFFF3F0F8;
    private static final int MUTED = 0xFFB8B0C8;
    private static final int ACCENT = 0xFFC4B5FD;

    private record Elem(String label, BooleanSupplier on, IntSupplier getX, IntConsumer setX,
                         IntSupplier getY, IntConsumer setY) {}

    private final Screen parent;
    private final List<Elem> elems = new ArrayList<>();
    private Elem dragging;
    private int dragOffX;
    private int dragOffY;

    public HudLayoutScreen(Screen parent) {
        super(Text.literal("HUD layout"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        elems.clear();
        ClientConfig c = AeroClient.CONFIG;
        if (c == null) {
            return;
        }
        elems.add(new Elem("FPS", () -> c.fpsHud, () -> c.fpsX, v -> c.fpsX = v, () -> c.fpsY, v -> c.fpsY = v));
        elems.add(new Elem("Ping", () -> c.pingHud, () -> c.pingX, v -> c.pingX = v, () -> c.pingY, v -> c.pingY = v));
        elems.add(new Elem("Coordinates", () -> c.coordsHud, () -> c.coordsX, v -> c.coordsX = v, () -> c.coordsY, v -> c.coordsY = v));
        elems.add(new Elem("Potion", () -> c.potionHud, () -> c.potionX, v -> c.potionX = v, () -> c.potionY, v -> c.potionY = v));
        elems.add(new Elem("Sprint", () -> c.sprintHud, () -> c.sprintX, v -> c.sprintX = v, () -> c.sprintY, v -> c.sprintY = v));
        elems.add(new Elem("Watermark", () -> c.watermark, () -> c.watermarkX, v -> c.watermarkX = v, () -> c.watermarkY, v -> c.watermarkY = v));
        elems.add(new Elem("Music", () -> c.musicPlayer, () -> c.musicX, v -> c.musicX = v, () -> c.musicY, v -> c.musicY = v));
        elems.add(new Elem("Keystrokes", () -> c.keystrokes, () -> c.keystrokesX, v -> c.keystrokesX = v, () -> c.keystrokesY, v -> c.keystrokesY = v));
        elems.add(new Elem("Armor HUD", () -> c.armorHud, () -> c.armorHudX, v -> c.armorHudX = v, () -> c.armorHudY, v -> c.armorHudY = v));
        // totemX/Y default to 0, meaning "center it automatically" (see OverlayHud) - show that
        // computed position here too, otherwise the chip would sit at (0,0) instead of where the
        // totem counter actually renders until it's been dragged at least once.
        elems.add(new Elem("Totem Counter", () -> c.totemCounter && c.totemHud,
                () -> c.totemX > 0 ? c.totemX : width / 2 - 8, v -> c.totemX = v,
                () -> c.totemY > 0 ? c.totemY : height - 70, v -> c.totemY = v));
    }

    private int chipW(Elem e) {
        return 16 + textRenderer.getWidth(e.label());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x77000000);
        UiDraw.glass(context, 8, 6, Math.min(width - 16, 420), 28, 0xC414121E, 12);
        context.drawText(textRenderer, Text.literal("Drag chips to move HUD elements. Esc to go back."),
                18, 14, TEXT, false);

        for (Elem e : elems) {
            int x = e.getX().getAsInt();
            int y = e.getY().getAsInt();
            int w = chipW(e);
            int dotColor = e.on().getAsBoolean() ? ACCENT : MUTED;
            boolean hover = inside(mouseX, mouseY, x, y, w, 16) || dragging == e;
            UiDraw.roundRect(context, x, y, w, 16, 6, hover ? 0xE0282436 : 0xC014121C);
            UiDraw.roundBorder(context, x, y, w, 16, 6, hover ? 0x66C4B5FD : 0x22FFFFFF);
            context.fill(x + 4, y + 4, x + 7, y + 12, dotColor);
            context.drawText(textRenderer, Text.literal(e.label()), x + 12, y + 4, TEXT, false);
        }
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        for (Elem e : elems) {
            int x = e.getX().getAsInt();
            int y = e.getY().getAsInt();
            if (inside(mx, my, x, y, chipW(e), 16)) {
                dragging = e;
                dragOffX = mx - x;
                dragOffY = my - y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging == null) {
            return false;
        }
        int nx = Math.max(0, Math.min(width - chipW(dragging), (int) click.x() - dragOffX));
        int ny = Math.max(0, Math.min(height - 16, (int) click.y() - dragOffY));
        dragging.setX().accept(nx);
        dragging.setY().accept(ny);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            AeroClient.CONFIG.save();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
