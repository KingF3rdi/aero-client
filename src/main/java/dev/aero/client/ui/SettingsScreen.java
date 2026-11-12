package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.ModUpdater;
import dev.aero.client.config.ClientConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings page: General, Appearance (themes, glass, accents), Notifications and About on one scrolling page,
 * with a sidebar that jumps between them and the menu's icon bar on top.
 * Drawing and clicking go through the same walk over the page, so a control is hit exactly where it is drawn.
 */
public class SettingsScreen extends Screen {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final String[] SECTIONS = {"General", "Appearance", "Notifications", "About"};
    private static final String[] ICONS = {"search", "grid", "friends", "wardrobe", "layers", "gear"};
    private static final String[] SIZES = {"Small", "Normal", "Large"};

    private final Screen parent;
    private final int[] anchors = new int[SECTIONS.length];
    private float scroll;
    private float scrollTarget;
    private int contentH;
    private boolean waitingKey;
    private boolean draggingGlass;
    private long lastFrame;

    // walk state: g == null means "hit test the click at (cx, cy)" instead of drawing
    private DrawContext g;
    private int mx;
    private int my;
    private int cx;
    private int cy;
    private boolean handled;
    private int glassX;
    private int glassW;

    public SettingsScreen(Screen parent) {
        super(Text.literal("Settings"));
        this.parent = parent;
    }

    private ClientConfig cfg() {
        return AeroClient.CONFIG;
    }

    private float scale() {
        return Math.max(0.5f, Math.min(width / 640f, height / 360f));
    }

    private int vw() {
        return Math.round(width / scale());
    }

    private int vh() {
        return Math.round(height / scale());
    }

    // panel geometry (design space)
    private int px() {
        return Math.max(8, (vw() - 600) / 2);
    }

    private int py() {
        return 12;
    }

    private int pw() {
        return Math.min(600, vw() - 16);
    }

    private int ph() {
        return vh() - 24;
    }

    private int contentX() {
        return px() + 150;
    }

    private int contentW() {
        return pw() - 150 - 22;
    }

    private int contentTop() {
        return py() + 40;
    }

    private int viewH() {
        return py() + ph() - 8 - contentTop();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        float s = scale();
        long now = System.nanoTime();
        float k = lastFrame == 0 ? 1f : 1f - (float) Math.exp(-(now - lastFrame) / 1e9 * 16);
        lastFrame = now;
        scroll += (scrollTarget - scroll) * k;
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(s, s);
        try {
            int x = Math.round(mouseX / s);
            int y = Math.round(mouseY / s);
            drawFrame(context, x, y);
            g = context;
            this.mx = x;
            this.my = y;
            context.enableScissor(contentX() - 8, contentTop(), px() + pw() - 4, contentTop() + viewH());
            try {
                walk();
            } finally {
                context.disableScissor();
            }
        } finally {
            context.getMatrices().popMatrix();
            g = null;
        }
    }

    // ---- frame: panel, header icons, sidebar --------------------------------------------------------

    private void drawFrame(DrawContext c, int x, int y) {
        int px = px();
        int py = py();
        int pw = pw();
        int ph = ph();
        UiDraw.glass(c, px, py, pw, ph, 0, 14);
        UiDraw.aeroMark(c, px + 14, py + 10, 14, UiDraw.accent());
        c.drawText(textRenderer, Text.literal("Aero"), px + 32, py + 13, TEXT, false);
        for (int i = 0; i < ICONS.length; i++) {
            int ix = iconX(i);
            int iy = py + 7;
            boolean active = i == ICONS.length - 1;
            boolean hover = inside(x, y, ix, iy, 20, 20);
            if (active || hover) {
                UiDraw.roundRect(c, ix, iy, 20, 20, 6, active ? 0x22FFFFFF : 0x14FFFFFF);
            }
            LineIcons.draw(c, ICONS[i], ix + 4, iy + 4, 12, active || hover ? TEXT : UiDraw.SOFT);
        }
        c.fill(iconX(0) + 25, py + 11, iconX(0) + 26, py + 23, 0x26FFFFFF);
        c.fill(px + 1, py + 33, px + pw - 1, py + 34, 0x1AFFFFFF);
        c.fill(px + 140, py + 34, px + 141, py + ph - 1, 0x1AFFFFFF);
        int active = activeSection();
        for (int i = 0; i < SECTIONS.length; i++) {
            int sy = py + 42 + i * 26;
            boolean hover = inside(x, y, px + 8, sy, 124, 22);
            if (i == active || hover) {
                UiDraw.roundRect(c, px + 8, sy, 124, 22, 7, i == active ? 0x22FFFFFF : 0x10FFFFFF);
            }
            c.drawText(textRenderer, Text.literal(SECTIONS[i]), px + 18, sy + 7, i == active ? TEXT : UiDraw.SOFT, false);
        }
    }

    private int iconX(int i) {
        int right = px() + pw() - 10;
        return right - (ICONS.length - i) * 24 - (i == 0 ? 8 : 0);
    }

    private int activeSection() {
        int a = 0;
        for (int i = 0; i < anchors.length; i++) {
            if (anchors[i] - 12 <= scrollTarget) {
                a = i;
            }
        }
        if (scrollTarget >= maxScroll() - 2) {
            a = anchors.length - 1; // the last section may be too short to reach the top
        }
        return a;
    }

    private float maxScroll() {
        return Math.max(0, contentH - viewH());
    }

    // ---- the page --------------------------------------------------------------------------------------

    /** Lays out (and draws, or hit-tests) the whole page. y runs in page space; shown at contentTop - scroll. */
    private void walk() {
        int x = contentX();
        int w = contentW();
        int top = contentTop() - Math.round(scroll);
        int y = top + 6;
        var c = cfg();
        if (c == null) {
            return;
        }

        anchors[0] = y - top - 6;
        y = title(x, y, "General");
        y = row(x, y, w, "Menu key", (rx, ry) -> {
            KeyBinding key = AeroClient.menuKey();
            String label = waitingKey ? "Press a key" : key == null ? "-" : key.getBoundKeyLocalizedText().getString();
            int bw = textRenderer.getWidth(label) + 16;
            if (button(rx - bw, ry - 2, bw, 16, label, waitingKey)) {
                waitingKey = true;
            }
        });
        y = row(x, y, w, "Menu size", (rx, ry) -> {
            int segW = 46;
            int sx = rx - segW * SIZES.length - 4;
            draw(() -> UiDraw.roundRect(g, sx, ry - 4, segW * SIZES.length + 4, 20, 8, 0x40000000));
            for (int i = 0; i < SIZES.length; i++) {
                boolean on = SIZES[i].equalsIgnoreCase(c.menuSize);
                int bx = sx + 2 + i * segW;
                if (segment(bx, ry - 2, segW, 16, SIZES[i], on)) {
                    c.menuSize = SIZES[i];
                    c.save();
                }
            }
        });
        y = toggleRow(x, y, w, "Other players' cosmetics", () -> c.showOthersCosmetics, v -> c.showOthersCosmetics = v, false);
        y = toggleRow(x, y, w, "Aero title screen", () -> c.aeroTitleScreen, v -> c.aeroTitleScreen = v, false);

        y += 18;
        anchors[1] = y - top - 6;
        y = title(x, y, "Appearance");
        y = label(x, y, "Theme") + 4;
        int gap = 10;
        int cardW = (w - gap * 3) / 4;
        for (int i = 0; i < Themes.THEMES.length; i++) {
            Themes.Theme t = Themes.THEMES[i];
            int col = i % 4;
            int rowY = y + (i / 4) * 66;
            int tx = x + col * (cardW + gap);
            boolean on = t == Themes.current();
            draw(() -> g.drawText(textRenderer, Text.literal(t.name()), tx, rowY, on ? TEXT : UiDraw.SOFT, false));
            if (themeCard(tx, rowY + 12, cardW, 42, t, on)) {
                c.uiTheme = t.name();
                c.uiAccent = t.accent();
                c.uiAccent2 = 0;
                c.save();
            }
        }
        y += ((Themes.THEMES.length + 3) / 4) * 66 + 4;

        int gy = y;
        draw(() -> g.drawText(textRenderer, Text.literal("Glass"), x, gy + 2, TEXT, false));
        glassX = x + 56;
        glassW = w - 56 - 40;
        float glass = Math.max(0f, Math.min(1f, c.menuGlass / 100f));
        draw(() -> {
            UiDraw.slider(g, glassX, gy + 3, glassW, glass);
            String pct = Math.round(c.menuGlass) + "%";
            g.drawText(textRenderer, Text.literal(pct), x + w - textRenderer.getWidth(pct), gy + 2, MUTED, false);
        });
        if (hit(glassX - 4, gy - 4, glassW + 8, 16)) {
            draggingGlass = true;
            setGlass(cx);
        }
        y += 24;

        y = label(x, y, "Accent");
        y = swatches(x, y, Themes.SOLID, c);
        y = label(x, y + 4, "Gradient accents");
        y = swatches(x, y, Themes.GRADIENT, c);

        y += 18;
        anchors[2] = y - top - 6;
        y = title(x, y, "Notifications");
        y = toggleRow(x, y, w, "A friend comes online", () -> c.notifyFriendOnline, v -> c.notifyFriendOnline = v, false);
        int nestTop = y;
        y = toggleRow(x, y, w, "Play a sound", () -> c.notifySound, v -> c.notifySound = v, true);
        int nestBottom = y;
        draw(() -> g.fill(x + 4, nestTop + 2, x + 5, nestBottom - 4, 0x33FFFFFF));

        y += 18;
        anchors[3] = y - top - 6;
        y = title(x, y, "About");
        y = row(x, y, w, "Version", (rx, ry) -> {
            String lab = "Aero Client · " + ModUpdater.label();
            int bw = textRenderer.getWidth(lab) + 16;
            if (button(rx - bw, ry - 2, bw, 16, lab, false)) {
                ModUpdater.click();
            }
        });
        y += 6;
        y = label(x, y, "What's new");
        for (String line : AeroTitleScreen.changelog()) {
            String body = line.startsWith("•") ? line.substring(1).trim() : line;
            var parts = textRenderer.wrapLines(Text.literal(body), w - 14);
            for (int pi = 0; pi < parts.size(); pi++) {
                var part = parts.get(pi);
                int ly = y;
                boolean first = pi == 0;
                draw(() -> {
                    if (first) {
                        UiDraw.roundRect(g, x + 2, ly + 3, 3, 3, 1, UiDraw.accent());
                    }
                    g.drawText(textRenderer, part, x + 12, ly, UiDraw.SOFT, false);
                });
                y += 11;
            }
            y += 3;
        }
        contentH = y - top + 10;
    }

    // ---- page pieces (each draws when g != null, otherwise tests the click) ---------------------------

    private interface Right {
        void at(int rightX, int rowY);
    }

    private void draw(Runnable r) {
        if (g != null) {
            r.run();
        }
    }

    private boolean hit(int x, int y, int w, int h) {
        if (g == null && !handled && inside(cx, cy, x, y, w, h)) {
            handled = true;
            return true;
        }
        return false;
    }

    private int title(int x, int y, String text) {
        draw(() -> {
            g.getMatrices().pushMatrix();
            g.getMatrices().translate(x, y);
            g.getMatrices().scale(1.6f, 1.6f);
            g.drawText(textRenderer, Text.literal(text), 0, 0, TEXT, false);
            g.getMatrices().popMatrix();
        });
        return y + 26;
    }

    private int label(int x, int y, String text) {
        draw(() -> g.drawText(textRenderer, Text.literal(text), x, y, TEXT, false));
        return y + 14;
    }

    /** A settings row: label left, control right (given the row's right edge and text line), divider below. */
    private int row(int x, int y, int w, String text, Right right) {
        int ty = y + 8;
        draw(() -> {
            g.drawText(textRenderer, Text.literal(text), x, ty, TEXT, false);
            g.fill(x, y + 25, x + w, y + 26, 0x14FFFFFF);
        });
        right.at(x + w, ty);
        return y + 26;
    }

    private int toggleRow(int x, int y, int w, String text, Supplier<Boolean> get, Consumer<Boolean> set, boolean nested) {
        int ix = nested ? x + 14 : x;
        return row(ix, y, w - (ix - x), text, (rx, ry) -> {
            boolean on = get.get();
            draw(() -> UiDraw.toggle(g, rx - 28, ry - 4, on));
            if (hit(ix, y, w, 26)) {
                set.accept(!on);
                cfg().save();
            }
        });
    }

    private boolean button(int x, int y, int w, int h, String text, boolean on) {
        draw(() -> {
            boolean hover = inside(mx, my, x, y, w, h);
            UiDraw.roundRect(g, x, y, w, h, 6, on ? 0x30FFFFFF : hover ? 0x22FFFFFF : 0x14FFFFFF);
            UiDraw.roundBorder(g, x, y, w, h, 6, on ? 0xB3FFFFFF : 0x33FFFFFF);
            g.drawText(textRenderer, Text.literal(text), x + (w - textRenderer.getWidth(text)) / 2, y + (h - 8) / 2, TEXT, false);
        });
        return hit(x, y, w, h);
    }

    private boolean segment(int x, int y, int w, int h, String text, boolean on) {
        draw(() -> {
            if (on) {
                UiDraw.roundRect(g, x, y, w, h, 6, 0x2EFFFFFF);
            }
            g.drawText(textRenderer, Text.literal(text), x + (w - textRenderer.getWidth(text)) / 2, y + (h - 8) / 2, on ? TEXT : MUTED, false);
        });
        return hit(x, y, w, h);
    }

    /** Mini preview of a theme: its base, a sidebar with the accent, a panel with a little accent toggle. */
    private boolean themeCard(int x, int y, int w, int h, Themes.Theme t, boolean on) {
        draw(() -> {
            int acc = t.accent() | 0xFF000000;
            boolean hover = inside(mx, my, x, y, w, h);
            UiDraw.roundRect(g, x, y, w, h, 7, t.base() | 0xFF000000);
            UiDraw.roundGradient(g, x, y, w, h, 7, 0x14FFFFFF, 0x04FFFFFF, false);
            UiDraw.roundRect(g, x + 6, y + 5, 3, 3, 1, acc);
            g.fill(x + 12, y + 6, x + 26, y + 7, 0x55FFFFFF);
            g.fill(x + 1, y + 11, x + w - 1, y + 12, 0x1AFFFFFF);
            int side = Math.round(w * 0.3f);
            UiDraw.roundRect(g, x + 5, y + 15, side, 4, 2, 0x22FFFFFF);
            g.fill(x + 7, y + 16, x + 5 + side * 2 / 3, y + 17, acc);
            g.fill(x + 6, y + 24, x + 5 + side - 4, y + 25, 0x33FFFFFF);
            g.fill(x + 6, y + 29, x + 5 + side - 8, y + 30, 0x22FFFFFF);
            int pX = x + side + 10;
            int pW = w - side - 15;
            UiDraw.roundRect(g, pX, y + 15, pW, h - 20, 5, 0x12FFFFFF);
            g.fill(pX + 5, y + 21, pX + pW / 2, y + 22, 0x40FFFFFF);
            g.fill(pX + 5, y + 27, pX + pW / 2 + 6, y + 28, 0x26FFFFFF);
            UiDraw.roundRect(g, pX + pW - 15, y + 20, 10, 5, 2, acc);
            UiDraw.roundBorder(g, x, y, w, h, 7, on ? acc : hover ? 0x66FFFFFF : 0x26FFFFFF);
            if (on) {
                UiDraw.roundBorder(g, x + 1, y + 1, w - 2, h - 2, 6, UiDraw.withAlpha(acc & 0xFFFFFF, 0x80));
            }
        });
        return hit(x, y, w, h);
    }

    private int swatches(int x, int y, Themes.Accent[] list, ClientConfig c) {
        int size = 20;
        int step = 27;
        String tip = null;
        int tipX = 0;
        for (int i = 0; i < list.length; i++) {
            Themes.Accent a = list[i];
            int sx = x + i * step;
            boolean on = (c.uiAccent & 0xFFFFFF) == a.a() && (c.uiAccent2 & 0xFFFFFF) == a.b();
            boolean hover = inside(mx, my, sx, y, size, size);
            if (hover) {
                tip = a.name();
                tipX = sx + size / 2;
            }
            draw(() -> {
                if (on) {
                    UiDraw.roundBorder(g, sx - 3, y - 3, size + 6, size + 6, 9, 0xFFFFFFFF);
                }
                if (a.b() == 0) {
                    UiDraw.roundRect(g, sx, y, size, size, 6, a.a() | 0xFF000000);
                } else {
                    UiDraw.roundGradient(g, sx, y, size, size, 6, a.a() | 0xFF000000, a.b() | 0xFF000000, false);
                }
                UiDraw.roundBorder(g, sx, y, size, size, 6, hover ? 0x80FFFFFF : 0x26FFFFFF);
            });
            if (hit(sx, y, size, size)) {
                c.uiAccent = a.a();
                c.uiAccent2 = a.b();
                c.save();
            }
        }
        if (tip != null) {
            String t = tip;
            int tw = textRenderer.getWidth(t) + 12;
            int tx = tipX - tw / 2;
            draw(() -> {
                UiDraw.roundRect(g, tx, y - 18, tw, 14, 7, 0xF0101116);
                UiDraw.roundBorder(g, tx, y - 18, tw, 14, 7, 0x40FFFFFF);
                g.drawText(textRenderer, Text.literal(t), tx + 6, y - 15, TEXT, false);
            });
        }
        return y + size + 10;
    }

    private void setGlass(int x) {
        var c = cfg();
        if (c != null && glassW > 0) {
            c.menuGlass = Math.round(Math.max(0f, Math.min(1f, (x - glassX) / (float) glassW)) * 100f);
        }
    }

    private static boolean inside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && y >= ry && x < rx + rw && y < ry + rh;
    }

    // ---- input -------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float s = scale();
        int x = Math.round((float) (click.x() / s));
        int y = Math.round((float) (click.y() / s));
        waitingKey = false;
        for (int i = 0; i < ICONS.length; i++) {
            if (inside(x, y, iconX(i), py() + 7, 20, 20)) {
                openIcon(i);
                return true;
            }
        }
        for (int i = 0; i < SECTIONS.length; i++) {
            if (inside(x, y, px() + 8, py() + 42 + i * 26, 124, 22)) {
                scrollTarget = Math.min(maxScroll(), anchors[i]);
                return true;
            }
        }
        if (inside(x, y, contentX() - 8, contentTop(), contentW() + 16, viewH())) {
            g = null;
            cx = x;
            cy = y;
            handled = false;
            walk();
            if (handled) {
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingGlass) {
            setGlass(Math.round((float) (click.x() / scale())));
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingGlass) {
            draggingGlass = false;
            if (cfg() != null) {
                cfg().save();
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget = (float) Math.max(0, Math.min(maxScroll(), scrollTarget - verticalAmount * 28));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (waitingKey) {
            waitingKey = false;
            KeyBinding key = AeroClient.menuKey();
            if (key != null && input.key() != GLFW.GLFW_KEY_ESCAPE) {
                key.setBoundKey(InputUtil.fromKeyCode(input));
                KeyBinding.updateKeysByCode();
                client.options.write();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    /** Screenshot harness: jump to a section. */
    public void debugSection(int i) {
        scrollTarget = scroll = Math.min(maxScroll(), anchors[i]);
    }

    private void openIcon(int i) {
        if (i == ICONS.length - 1) {
            return; // this page
        }
        if (i == 4) {
            client.setScreen(new ProfilesScreen(this));
            return;
        }
        Screen gui = Menus.clickGui(null, false);
        if (gui instanceof ClickGuiScreenLegacy l) {
            switch (i) {
                case 0 -> l.debugSearch("");
                case 1 -> l.debugHome();
                case 2 -> l.debugTab(2, 0);
                default -> l.debugTab(1, 0);
            }
        }
        client.setScreen(gui);
    }
}
