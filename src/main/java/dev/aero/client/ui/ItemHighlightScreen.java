package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Item Highlighter editor: search every item in the game, click one to add it, then give it its own color.
 * Stored in ClientConfig.highlightCustom as "minecraft:item_id=RRGGBB,..." (read by OverlayHud.highlightBg).
 */
public class ItemHighlightScreen extends Screen {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final int[] PRESETS = {0xFF5555, 0xFF9F43, 0xFFD93D, 0x55FF55, 0x4FD8FF, 0x4F8EFF, 0xB06BFF, 0xFF6BD5, 0xFFFFFF};
    private static final int CELL = 20;
    private static final int ROW = 20;

    private static List<Item> allItems;

    private final Screen parent;
    private final Map<String, Integer> picks = new LinkedHashMap<>();
    private String query = "";
    private List<Item> shown = List.of();
    private int gridScroll;
    private int listScroll;
    private String selected;
    private boolean hueDrag;

    public ItemHighlightScreen(Screen parent) {
        super(Text.literal("Item Highlighter"));
        this.parent = parent;
    }

    // ---- data -------------------------------------------------------------------------------

    /** "id=RRGGBB,..." -> map; entries without a namespace (older free-text names) are kept as they are. */
    public static Map<String, Integer> parse(String raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (String part : raw.split(",")) {
            String[] kv = part.trim().split("=");
            if (kv.length != 2 || kv[0].isBlank()) {
                continue;
            }
            try {
                out.put(kv[0].trim().toLowerCase(Locale.ROOT), Integer.parseInt(kv[1].trim().replace("#", ""), 16) & 0xFFFFFF);
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (var e : picks.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(e.getKey()).append('=').append(String.format("%06X", e.getValue()));
        }
        AeroClient.CONFIG.highlightCustom = sb.toString();
        AeroClient.CONFIG.save();
    }

    private static String id(Item item) {
        return Registries.ITEM.getId(item).toString();
    }

    private static ItemStack stackFor(String key) {
        var ident = net.minecraft.util.Identifier.tryParse(key.contains(":") ? key : "minecraft:" + key);
        Item item = ident == null ? Items.AIR : Registries.ITEM.get(ident);
        return item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item);
    }

    private void filter() {
        if (allItems == null) {
            allItems = new ArrayList<>();
            for (Item item : Registries.ITEM) {
                if (item != Items.AIR) {
                    allItems.add(item);
                }
            }
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<Item> out = new ArrayList<>();
        for (Item item : allItems) {
            if (q.isEmpty() || id(item).contains(q.replace(' ', '_'))
                    || item.getName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(item);
            }
        }
        shown = out;
        gridScroll = 0;
    }

    @Override
    protected void init() {
        super.init();
        picks.clear();
        picks.putAll(parse(AeroClient.CONFIG == null ? "" : AeroClient.CONFIG.highlightCustom));
        if (selected == null || !picks.containsKey(selected)) {
            selected = picks.isEmpty() ? null : picks.keySet().iterator().next();
        }
        filter();
    }

    // ---- layout -----------------------------------------------------------------------------

    private int pw() {
        return Math.min(width - 20, 470);
    }

    private int ph() {
        return Math.min(height - 20, 300);
    }

    private int px() {
        return (width - pw()) / 2;
    }

    private int py() {
        return (height - ph()) / 2;
    }

    private int gridX() {
        return px() + 14;
    }

    private int gridY() {
        return py() + 62;
    }

    private int cols() {
        return Math.max(4, (pw() / 2 - 14) / CELL);
    }

    private int gridRows() {
        return Math.max(1, (ph() - 62 - 14) / CELL);
    }

    private int listX() {
        return gridX() + cols() * CELL + 14;
    }

    private int listW() {
        return px() + pw() - 14 - listX();
    }

    private int listY() {
        return py() + 62;
    }

    /** Rows of picked items above the color editor. */
    private int listRows() {
        return Math.max(1, (ph() - 62 - 14 - 46) / ROW);
    }

    private int colorY() {
        return py() + ph() - 14 - 40;
    }

    private int doneX() {
        return px() + pw() - 14 - 60;
    }

    // ---- render -----------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mx, int my, float delta) {
        context.fill(0, 0, width, height, 0x55000000);
        UiDraw.glass(context, px(), py(), pw(), ph(), 0xE014121E, 18);
        context.drawText(textRenderer, Text.literal("Item Highlighter"), px() + 14, py() + 12, TEXT, false);
        context.drawText(textRenderer, Text.literal("Click an item to add it, then pick its color"), px() + 14, py() + 24, MUTED, false);
        boolean doneH = inside(mx, my, doneX(), py() + 10, 60, 20);
        UiDraw.pill(context, doneX(), py() + 10, 60, 20, doneH);
        context.drawText(textRenderer, Text.literal("Done"), doneX() + (60 - textRenderer.getWidth("Done")) / 2, py() + 16, doneH ? TEXT : MUTED, false);

        // search + item grid
        int gw = cols() * CELL;
        UiDraw.field(context, gridX(), py() + 38, gw, 18, true);
        String q = query.isEmpty() ? "Search items" : query;
        context.drawText(textRenderer, Text.literal(q + "|"), gridX() + 7, py() + 43, query.isEmpty() ? MUTED : TEXT, false);
        int rows = gridRows();
        int maxScroll = Math.max(0, (shown.size() + cols() - 1) / cols() - rows);
        gridScroll = Math.max(0, Math.min(gridScroll, maxScroll));
        Item hover = null;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols(); c++) {
                int i = (gridScroll + r) * cols() + c;
                if (i >= shown.size()) {
                    break;
                }
                Item item = shown.get(i);
                int x = gridX() + c * CELL;
                int y = gridY() + r * CELL;
                Integer col = picks.get(id(item));
                if (col != null) {
                    context.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x99000000 | col);
                }
                if (inside(mx, my, x, y, CELL, CELL)) {
                    context.fill(x, y, x + CELL, y + CELL, 0x33FFFFFF);
                    hover = item;
                }
                context.drawItem(new ItemStack(item), x + 2, y + 2);
            }
        }
        UiDraw.scrollbar(context, gridX() + gw + 2, gridY(), rows * CELL, gridScroll, (shown.size() + cols() - 1) / cols(), rows);
        if (shown.isEmpty()) {
            context.drawText(textRenderer, Text.literal("No item matches"), gridX() + 4, gridY() + 6, MUTED, false);
        }

        // picked items
        context.drawText(textRenderer, Text.literal("Your items (" + picks.size() + ")"), listX(), py() + 43, MUTED, false);
        List<String> keys = new ArrayList<>(picks.keySet());
        int lrows = listRows();
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, keys.size() - lrows)));
        if (keys.isEmpty()) {
            context.drawText(textRenderer, Text.literal("Nothing added yet"), listX() + 4, listY() + 6, MUTED, false);
        }
        for (int r = 0; r < lrows && listScroll + r < keys.size(); r++) {
            String key = keys.get(listScroll + r);
            int y = listY() + r * ROW;
            boolean sel = key.equals(selected);
            if (sel || inside(mx, my, listX(), y, listW(), ROW - 2)) {
                UiDraw.roundRect(context, listX(), y, listW(), ROW - 2, 6, sel ? 0x2EFFFFFF : 0x14FFFFFF);
            }
            ItemStack stack = stackFor(key);
            context.drawItem(stack, listX() + 2, y + 1);
            String name = key.contains(":") ? stack.getName().getString() : key;
            int nameW = listW() - 22 - 40;
            context.drawText(textRenderer, Text.literal(textRenderer.trimToWidth(name, nameW)), listX() + 22, y + 5, TEXT, false);
            UiDraw.roundRect(context, listX() + listW() - 34, y + 4, 12, 10, 3, 0xFF000000 | picks.get(key));
            boolean xH = inside(mx, my, listX() + listW() - 16, y, 16, ROW - 2);
            context.drawText(textRenderer, Text.literal("×"), listX() + listW() - 12, y + 5, xH ? 0xFFFF6B6B : MUTED, false);
        }
        UiDraw.scrollbar(context, listX() + listW() + 2, listY(), lrows * ROW, listScroll, keys.size(), lrows);

        // color editor for the selected item
        if (selected != null) {
            int cy = colorY();
            int bw = listW();
            for (int i = 0; i < bw; i++) {
                context.fill(listX() + i, cy, listX() + i + 1, cy + 10, 0xFF000000 | (java.awt.Color.HSBtoRGB(i / (float) bw, 0.85f, 1f) & 0xFFFFFF));
            }
            int sw = Math.min(16, (bw - (PRESETS.length - 1) * 3) / PRESETS.length);
            for (int i = 0; i < PRESETS.length; i++) {
                int x = listX() + i * (sw + 3);
                UiDraw.roundRect(context, x, cy + 16, sw, sw, 4, 0xFF000000 | PRESETS[i]);
                if (picks.get(selected) == PRESETS[i]) {
                    UiDraw.roundBorder(context, x - 1, cy + 15, sw + 2, sw + 2, 5, 0xFFFFFFFF);
                }
            }
        }
        if (hover != null) {
            context.drawTooltip(textRenderer, hover.getName(), mx, my);
        }
    }

    // ---- input ------------------------------------------------------------------------------

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private void setColor(int rgb) {
        if (selected != null) {
            picks.put(selected, rgb & 0xFFFFFF);
            save();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (inside(mx, my, doneX(), py() + 10, 60, 20)) {
            client.setScreen(parent);
            return true;
        }
        // grid: add (or select if already added)
        if (inside(mx, my, gridX(), gridY(), cols() * CELL, gridRows() * CELL)) {
            int i = (gridScroll + (my - gridY()) / CELL) * cols() + (mx - gridX()) / CELL;
            if (i < shown.size()) {
                String key = id(shown.get(i));
                if (!picks.containsKey(key)) {
                    picks.put(key, AeroClient.CONFIG.highlightColor & 0xFFFFFF);
                    save();
                    List<String> keys = new ArrayList<>(picks.keySet());
                    listScroll = Math.max(0, keys.size() - listRows());
                }
                selected = key;
            }
            return true;
        }
        // list: select or remove
        List<String> keys = new ArrayList<>(picks.keySet());
        for (int r = 0; r < listRows() && listScroll + r < keys.size(); r++) {
            int y = listY() + r * ROW;
            if (!inside(mx, my, listX(), y, listW(), ROW - 2)) {
                continue;
            }
            String key = keys.get(listScroll + r);
            if (mx >= listX() + listW() - 16) {
                picks.remove(key);
                save();
                if (key.equals(selected)) {
                    selected = picks.isEmpty() ? null : picks.keySet().iterator().next();
                }
            } else {
                selected = key;
            }
            return true;
        }
        // color editor
        if (selected != null) {
            int cy = colorY();
            int bw = listW();
            if (inside(mx, my, listX(), cy - 2, bw, 14)) {
                hueDrag = true;
                applyHue(mx);
                return true;
            }
            int sw = Math.min(16, (bw - (PRESETS.length - 1) * 3) / PRESETS.length);
            for (int i = 0; i < PRESETS.length; i++) {
                if (inside(mx, my, listX() + i * (sw + 3), cy + 16, sw, sw)) {
                    setColor(PRESETS[i]);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClicked(new Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(button, 0)), false);
    }

    private void applyHue(int mx) {
        float h = Math.max(0f, Math.min(0.999f, (mx - listX()) / (float) listW()));
        setColor(java.awt.Color.HSBtoRGB(h, 0.85f, 1f));
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (hueDrag) {
            applyHue((int) click.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        hueDrag = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int step = verticalAmount > 0 ? -1 : 1;
        if (mouseX >= listX()) {
            listScroll = Math.max(0, listScroll + step);
        } else {
            gridScroll = Math.max(0, gridScroll + step * 2);
        }
        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        char ch = (char) input.codepoint();
        if (!Character.isISOControl(ch) && query.length() < 32) {
            query += ch;
            filter();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            filter();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
