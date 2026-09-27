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
 * Every element shows its name; hover one and press H to hide/show it (switches the element itself)
 * or L to lock it in place.
 */
public class HudLayoutScreen extends Screen {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;

    private record Elem(String label, BooleanSupplier on, java.util.function.Consumer<Boolean> setOn, IntSupplier getX, IntConsumer setX,
                         IntSupplier getY, IntConsumer setY) {}

    private Elem hoveredElem;

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
        elems.add(new Elem("FPS", () -> c.fpsHud, v -> c.fpsHud = v, () -> c.fpsX, v -> c.fpsX = v, () -> c.fpsY, v -> c.fpsY = v));
        elems.add(new Elem("Ping", () -> c.pingHud, v -> c.pingHud = v, () -> c.pingX, v -> c.pingX = v, () -> c.pingY, v -> c.pingY = v));
        elems.add(new Elem("Coordinates", () -> c.coordsHud, v -> c.coordsHud = v, () -> c.coordsX, v -> c.coordsX = v, () -> c.coordsY, v -> c.coordsY = v));
        elems.add(new Elem("Potion", () -> c.potionHud, v -> c.potionHud = v, () -> c.potionX, v -> c.potionX = v, () -> c.potionY, v -> c.potionY = v));
        elems.add(new Elem("Sprint", () -> c.sprintHud, v -> c.sprintHud = v, () -> c.sprintX, v -> c.sprintX = v, () -> c.sprintY, v -> c.sprintY = v));
        elems.add(new Elem("Watermark", () -> c.watermark, v -> c.watermark = v, () -> c.watermarkX, v -> c.watermarkX = v, () -> c.watermarkY, v -> c.watermarkY = v));
        elems.add(new Elem("Music", () -> c.musicPlayer, v -> c.musicPlayer = v, () -> c.musicX, v -> c.musicX = v, () -> c.musicY, v -> c.musicY = v));
        elems.add(new Elem("Keystrokes", () -> c.keystrokes, v -> c.keystrokes = v, () -> c.keystrokesX, v -> c.keystrokesX = v, () -> c.keystrokesY, v -> c.keystrokesY = v));
        elems.add(new Elem("Armor HUD", () -> c.armorHud, v -> c.armorHud = v, () -> c.armorHudX, v -> c.armorHudX = v, () -> c.armorHudY, v -> c.armorHudY = v));
        // totemX/Y default to 0, meaning "center it automatically" (see OverlayHud) - show that
        // computed position here too, otherwise the chip would sit at (0,0) instead of where the
        // totem counter actually renders until it's been dragged at least once.
        elems.add(new Elem("Totem Counter", () -> c.totemCounter && c.totemHud, v -> c.totemHud = v,
                () -> c.totemX > 0 ? c.totemX : width / 2 - 8, v -> c.totemX = v,
                () -> c.totemY > 0 ? c.totemY : height - 70, v -> c.totemY = v));
    }

    private int chipW(Elem e) {
        return 20 + textRenderer.getWidth(e.label()) + (locked(e) ? 12 : 0);
    }

    private boolean locked(Elem e) {
        var c = AeroClient.CONFIG;
        return c != null && c.hudLocked != null && c.hudLocked.contains(e.label());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x44000000);
        String help = "Drag to move · H hide / show · L lock · Esc back";
        int bw = textRenderer.getWidth(help) + 24;
        int by = height / 2 + 24; // just under the crosshair, where no HUD element sits
        UiDraw.glass(context, (width - bw) / 2, by, bw, 24, 0, 12);
        context.drawText(textRenderer, Text.literal(help), (width - bw) / 2 + 12, by + 8, TEXT, false);

        hoveredElem = null;
        for (Elem e : elems) {
            int x = e.getX().getAsInt();
            int y = e.getY().getAsInt();
            int w = chipW(e);
            boolean on = e.on().getAsBoolean();
            boolean lock = locked(e);
            boolean hover = inside(mouseX, mouseY, x, y, w, 16) || dragging == e;
            if (hover) {
                hoveredElem = e;
            }
            UiDraw.roundRect(context, x, y, w, 16, 8, on ? (hover ? 0xFFFFFFFF : 0xE6FFFFFF) : 0x99FFFFFF);
            UiDraw.roundBorder(context, x, y, w, 16, 8, hover ? UiDraw.withAlpha(UiDraw.accent(), 0xAA) : 0x22000000);
            UiDraw.roundRect(context, x + 5, y + 5, 6, 6, 3, on ? 0xFF22C55E : 0xFFB0B6C2);
            context.drawText(textRenderer, Text.literal(e.label()), x + 15, y + 4, on ? TEXT : MUTED, false);
            if (lock) {
                WardrobePanel.drawLock(context, x + w - 12, y + 3, MUTED);
            }
            if (hover && dragging == null) {
                String tip = (on ? "H hide" : "H show") + " · " + (lock ? "L unlock" : "L lock");
                int tw = textRenderer.getWidth(tip) + 10;
                int ty = y > 24 ? y - 16 : y + 20;
                UiDraw.roundRect(context, x, ty, tw, 14, 7, 0xE61C1F27);
                context.drawText(textRenderer, Text.literal(tip), x + 5, ty + 3, 0xFFF3F4F6, false);
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
        for (Elem e : elems) {
            int x = e.getX().getAsInt();
            int y = e.getY().getAsInt();
            if (inside(mx, my, x, y, chipW(e), 16)) {
                if (locked(e)) {
                    return true;
                }
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
        var c = AeroClient.CONFIG;
        if (hoveredElem != null && c != null) {
            if (input.key() == GLFW.GLFW_KEY_H) {
                hoveredElem.setOn().accept(!hoveredElem.on().getAsBoolean());
                c.save();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_L) {
                if (c.hudLocked == null) {
                    c.hudLocked = new java.util.HashSet<>();
                }
                if (!c.hudLocked.remove(hoveredElem.label())) {
                    c.hudLocked.add(hoveredElem.label());
                }
                c.save();
                return true;
            }
        }
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
