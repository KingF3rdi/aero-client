package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/**
 * Pixel-grid crosshair editor, modeled after Custom Crosshair Mod's "Draw your own" screen (CC0):
 * a small square canvas you click cells on/off in, stored as offsets from the crosshair center.
 */
public class DrawCrosshairScreen extends Screen {
    private static final int BG = 0xCC09080F;
    private static final int PANEL = 0xF00E0C16;
    private static final int TEXT = 0xFFF3F0F8;
    private static final int MUTED = 0xFF8E889C;
    private static final int ACCENT = 0xFF4F8EFF;

    private static final int RADIUS = 8;
    private static final int GRID = RADIUS * 2 + 1;
    private static final int CELL = 14;

    private final Screen parent;
    private final Set<Long> pixels = new HashSet<>();
    private boolean dragErasing;
    private boolean dragging;
    private boolean colorFocus;
    private String colorDraft = "";

    public DrawCrosshairScreen(Screen parent) {
        super(Text.literal("Draw Crosshair"));
        this.parent = parent;
    }

    private static long key(int dx, int dy) {
        return ((long) (dx + 128) << 32) | (dy + 128);
    }

    @Override
    protected void init() {
        super.init();
        pixels.clear();
        String raw = AeroClient.CONFIG == null ? "" : AeroClient.CONFIG.crosshairPixels;
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.split(";")) {
                String[] xy = part.split(",");
                if (xy.length == 2) {
                    try {
                        pixels.add(key(Integer.parseInt(xy[0].trim()), Integer.parseInt(xy[1].trim())));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        if (pixels.isEmpty()) {
            pixels.add(key(0, 0));
            pixels.add(key(-3, 0));
            pixels.add(key(-2, 0));
            pixels.add(key(2, 0));
            pixels.add(key(3, 0));
            pixels.add(key(0, -3));
            pixels.add(key(0, -2));
            pixels.add(key(0, 2));
            pixels.add(key(0, 3));
            save();
        }
    }

    private int gridX() {
        return width / 2 - (GRID * CELL) / 2;
    }

    private int gridY() {
        return height / 2 - (GRID * CELL) / 2 - 10;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x66000000);
        int gx = gridX();
        int gy = gridY();
        int panelW = GRID * CELL + 28;
        int panelH = GRID * CELL + 112;
        UiDraw.glass(context, gx - 14, gy - 36, panelW, panelH, 0xD414121E, 18);
        context.drawText(textRenderer, Text.literal("Draw your crosshair"),
                gx, gy - 22, TEXT, false);

        context.fill(gx - 2, gy - 2, gx + GRID * CELL + 2, gy + GRID * CELL + 2, PANEL);
        for (int cy = 0; cy < GRID; cy++) {
            for (int cx = 0; cx < GRID; cx++) {
                int dx = cx - RADIUS;
                int dy = cy - RADIUS;
                int x = gx + cx * CELL;
                int y = gy + cy * CELL;
                boolean on = pixels.contains(key(dx, dy));
                boolean center = dx == 0 && dy == 0;
                int fill = on ? (0xFF000000 | (AeroClient.CONFIG.crosshairColor & 0xFFFFFF)) : 0xFF14121C;
                context.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, fill);
                if (center) {
                    context.fill(x, y, x + CELL, y + 1, ACCENT);
                    context.fill(x, y + CELL - 1, x + CELL, y + CELL, ACCENT);
                    context.fill(x, y, x + 1, y + CELL, ACCENT);
                    context.fill(x + CELL - 1, y, x + CELL, y + CELL, ACCENT);
                }
            }
        }

        int row1Y = gy + GRID * CELL + 14;
        boolean useH = inside(mouseX, mouseY, gx, row1Y, 150, 20);
        UiDraw.pill(context, gx, row1Y, 150, 20, AeroClient.CONFIG.crosshairUseDrawing);
        String useLabel = "Use drawing: " + (AeroClient.CONFIG.crosshairUseDrawing ? "On" : "Off");
        context.drawText(textRenderer, Text.literal(useLabel), gx + 8, row1Y + 6,
                useH ? TEXT : MUTED, false);

        int colorX = gx + 160;
        int colorW = GRID * CELL - 160;
        String hex = colorFocus ? colorDraft : String.format("#%06X", AeroClient.CONFIG.crosshairColor & 0xFFFFFF);
        UiDraw.field(context, colorX, row1Y, colorW, 20, colorFocus);
        UiDraw.roundRect(context, colorX + 6, row1Y + 5, 10, 10, 3, 0xFF000000 | AeroClient.CONFIG.crosshairColor);
        context.drawText(textRenderer, Text.literal(hex + (colorFocus ? "|" : "")), colorX + 22, row1Y + 6,
                colorFocus ? TEXT : MUTED, false);

        int row2Y = row1Y + 26;
        boolean clearH = inside(mouseX, mouseY, gx, row2Y, 110, 20);
        UiDraw.pill(context, gx, row2Y, 110, 20, clearH);
        context.drawText(textRenderer, Text.literal("Clear"), gx + 45, row2Y + 6,
                clearH ? TEXT : MUTED, false);

        int doneX = gx + GRID * CELL - 110;
        boolean doneH = inside(mouseX, mouseY, doneX, row2Y, 110, 20);
        UiDraw.pill(context, doneX, row2Y, 110, 20, doneH);
        context.drawText(textRenderer, Text.literal("Done"), doneX + 45, row2Y + 6,
                doneH ? TEXT : MUTED, false);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private void save() {
        if (AeroClient.CONFIG == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (long k : pixels) {
            int dx = (int) (k >> 32) - 128;
            int dy = (int) (k & 0xFFFFFFFFL) - 128;
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(dx).append(',').append(dy);
        }
        AeroClient.CONFIG.crosshairPixels = sb.toString();
        AeroClient.CONFIG.save();
    }

    private boolean toggleCellAt(int mx, int my, boolean startDrag) {
        int gx = gridX();
        int gy = gridY();
        if (mx < gx || my < gy || mx >= gx + GRID * CELL || my >= gy + GRID * CELL) {
            return false;
        }
        int cx = (mx - gx) / CELL;
        int cy = (my - gy) / CELL;
        int dx = cx - RADIUS;
        int dy = cy - RADIUS;
        long k = key(dx, dy);
        if (startDrag) {
            dragErasing = pixels.contains(k);
        }
        if (dragErasing) {
            pixels.remove(k);
        } else {
            pixels.add(k);
        }
        save();
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int gx = gridX();
        int gy = gridY();

        int row1Y = gy + GRID * CELL + 14;
        int row2Y = row1Y + 26;
        if (inside(mx, my, gx, row1Y, 150, 20)) {
            AeroClient.CONFIG.crosshairUseDrawing = !AeroClient.CONFIG.crosshairUseDrawing;
            if (AeroClient.CONFIG.crosshairUseDrawing) {
                AeroClient.CONFIG.customCrosshair = true;
                AeroClient.CONFIG.crosshairStyle = "Drawn";
            }
            AeroClient.CONFIG.save();
            return true;
        }
        int colorX = gx + 160;
        int colorW = GRID * CELL - 160;
        if (inside(mx, my, colorX, row1Y, colorW, 20)) {
            colorFocus = true;
            colorDraft = String.format("#%06X", AeroClient.CONFIG.crosshairColor & 0xFFFFFF);
            return true;
        }
        colorFocus = false;
        if (inside(mx, my, gx, row2Y, 110, 20)) {
            pixels.clear();
            save();
            return true;
        }
        int doneX = gx + GRID * CELL - 110;
        if (inside(mx, my, doneX, row2Y, 110, 20)) {
            AeroClient.CONFIG.customCrosshair = true;
            AeroClient.CONFIG.crosshairStyle = "Drawn";
            AeroClient.CONFIG.crosshairUseDrawing = true;
            save();
            client.setScreen(parent);
            return true;
        }
        if (toggleCellAt(mx, my, true)) {
            dragging = true;
            return true;
        }
        return false;
    }

    private void applyColorDraft() {
        String hex = colorDraft.startsWith("#") ? colorDraft.substring(1) : colorDraft;
        try {
            AeroClient.CONFIG.crosshairColor = 0xFF000000 | (Integer.parseInt(hex, 16) & 0xFFFFFF);
            AeroClient.CONFIG.save();
        } catch (NumberFormatException ignored) {
        }
        colorFocus = false;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (!colorFocus) {
            return false;
        }
        char ch = Character.toUpperCase((char) input.codepoint());
        if ((ch == '#' || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')) && colorDraft.length() < 7) {
            colorDraft += ch;
        }
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClicked(new Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(button, 0)), false);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (dragging) {
            toggleCellAt((int) click.x(), (int) click.y(), false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (colorFocus) {
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !colorDraft.isEmpty()) {
                colorDraft = colorDraft.substring(0, colorDraft.length() - 1);
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                applyColorDraft();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                colorFocus = false;
                return true;
            }
            return true;
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
