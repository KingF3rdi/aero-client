package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.ModUpdater;
import dev.aero.client.auth.AccountManager;
import dev.aero.client.auth.SavedAccount;
import dev.aero.client.auth.SkinPreview;
import dev.aero.client.cosmetic.CosmeticPreview;
import dev.aero.client.module.Category;
import dev.aero.client.module.Module;
import dev.aero.client.social.FriendStore;
import dev.aero.client.social.Shards;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Card-style client menu on frosted white glass. Every category is a card; clicking one grows it
 * into its module list with toggles and settings right there while the other cards move out of the
 * way. Typing anywhere searches modules and settings. You (Wardrobe/Store/Rewards/Badges) and
 * Friends are tabs; Esc steps back one level. The whole panel scales with the window.
 */
public class ClickGuiScreenLegacy extends Screen {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final int CARD = 0xB0FFFFFF;

    /** Logical panel size; the matrix scale maps it onto any window size. */
    private static final int PW = 620;
    private static final int PH = 350;
    private static final int TOP = 40;
    private static final int ROW_H = 30;
    private static final int ANIM_MS = 200;

    private int SIDE_W = 150;
    private int RIGHT_W = 210;
    /** Settings pane / module list geometry of the open card, set before drawing or hit-testing it. */
    private int paneX;
    private int paneY;
    private int paneBottom;
    private int listX0;
    private int listY0;
    private int listW0;
    private int listH0;

    private record CardDef(String title, Category category, int kind) {}

    private static final List<CardDef> CARDS = new ArrayList<>();

    static {
        for (Category c : Category.values()) {
            if (c.inSidebar()) {
                CARDS.add(new CardDef(c.title, c, 0));
            }
        }
        CARDS.add(new CardDef("Profiles", null, 1));
        CARDS.add(new CardDef("HUD Editor", null, 2));
    }

    private static final String[] YOU_TABS = {"Wardrobe", "Store", "Rewards", "Badges"};

    private final WardrobePanel wardrobe = new WardrobePanel();
    private final ShopPanel shop = new ShopPanel();
    private final Screen parent;
    private final boolean pauseGame;
    private String search = "";
    private boolean searchFocus;
    private int scroll;
    private Module selected;
    private int topTab;
    private int youTab;
    private int openCard = -1;
    private long cardAnimAt;
    private boolean closing;
    private int cosTab;
    private int friendSel = -1;
    private String friendDraft = "";
    private boolean friendFocus;
    private String playerLookup = "";
    private boolean playerLookupFocus;
    private int friendScroll;
    private final List<Module> visible = new ArrayList<>();

    private float s = 1f;
    private int ox;
    private int oy;
    private int pw;
    private int ph;

    public void debugSelect(int i) {
        rebuild();
        selected = i < visible.size() ? visible.get(i) : null;
    }

    public void debugSelectByName(String name) {
        topTab = 0;
        search = "";
        for (Module m : AeroClient.MODULES.all) {
            if (m.name.equals(name)) {
                for (int i = 0; i < CARDS.size(); i++) {
                    if (CARDS.get(i).category() == m.category) {
                        openCard = i;
                        cardAnimAt = 0;
                    }
                }
                rebuild();
                selected = m;
            }
        }
    }

    public void debugBuy(String capeId) {
        topTab = 1;
        youTab = 0;
        wardrobe.openBuy(dev.aero.client.cosmetic.Cosmetics.named(dev.aero.client.cosmetic.Cosmetics.Kind.CAPE, capeId));
    }

    public void debugSearch(String q) {
        topTab = 0;
        search = q;
        rebuild();
    }

    public void debugTab(int top, int cos) {
        topTab = top;
        if (top == 1) {
            youTab = cos >= 100 ? cos - 100 : 0;
            wardrobe.debugTab(cos >= 100 ? 0 : cos);
        }
    }

    public void debugPublishCape(String fileName) {
        wardrobe.publishName = fileName;
        wardrobe.submitPublish();
    }

    private Module.Setting colorOpen;
    private Module.Setting hueDrag;
    private int hueBarX;
    private int hueBarW;
    private Module.Setting keyCapture;
    private static final int[] PRESETS = {0xFF5555, 0xFF9F43, 0xFFD93D, 0x55FF55, 0x4FD8FF, 0x4F8EFF, 0xB06BFF, 0xFFFFFF};

    private static String keyLabel(int key) {
        if (key < 0) {
            return "None";
        }
        if (key >= 290 && key <= 301) {
            return "F" + (key - 289);
        }
        String n = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
        return n != null ? n.toUpperCase(java.util.Locale.ROOT) : "Key " + key;
    }
    private void applyHue(Module.Setting setting, int barX, int barW, int mx) {
        float t = Math.max(0f, Math.min(1f, (mx - barX) / (float) Math.max(1, barW)));
        int rgb = java.awt.Color.HSBtoRGB(t, 0.85f, 1f) & 0xFFFFFF;
        setting.intSet.set(0xFF000000 | rgb);
    }

    private int settingsScroll;
    private int settingsContentH;
    private Module scrollModule;
    private Module capturingKeybind;
    private Module.Setting focusedTextSetting;
    private String textDraft = "";
    private boolean accentFocus;
    private String accentDraft = "";
    private Module.Setting colorFocus;
    private String colorDraft = "";
    private final java.util.Map<String, Boolean> groupState = new java.util.HashMap<>();
    private Module.Setting draggingSetting;
    private int dragBarX;
    private int dragBarW;

    public ClickGuiScreenLegacy(Screen parent, boolean pauseGame) {
        super(Text.literal("Aero Client"));
        this.parent = parent;
        this.pauseGame = pauseGame;
    }

    public ClickGuiScreenLegacy(Screen parent) {
        this(parent, false);
    }

    public ClickGuiScreenLegacy() {
        this(null, false);
    }

    private static int lastOpenCard = -1;
    private static Module lastSelected;
    private static int lastTopTab;
    private static int lastYouTab;

    private void layoutPanel() {
        s = Math.max(0.5f, Math.min(3f, Math.min(width * 0.92f / PW, height * 0.9f / PH)));
        int vw = Math.round(width / s);
        int vh = Math.round(height / s);
        pw = PW;
        ph = PH;
        ox = (vw - pw) / 2;
        oy = (vh - ph) / 2;
    }

    private int lm(double v) {
        return (int) Math.floor(v / s);
    }

    private String groupKey(Module module, String name) {
        return module.name + "/" + name;
    }
    private boolean groupExpanded(Module.Setting setting) {
        if (selected == null || setting == null) {
            return false;
        }
        Boolean stored = groupState.get(groupKey(selected, setting.name));
        if (stored != null) {
            return stored;
        }
        return setting.kind == Module.Setting.Kind.BOOL && setting.boolGet != null && setting.boolGet.getAsBoolean();
    }
    private void toggleGroup(Module.Setting setting) {
        if (selected == null || setting == null) {
            return;
        }
        groupState.put(groupKey(selected, setting.name), !groupExpanded(setting));
    }
    private Module.Setting settingNamed(String name) {
        if (selected == null || name == null) {
            return null;
        }
        for (Module.Setting setting : selected.settings) {
            if (name.equals(setting.name)) {
                return setting;
            }
        }
        return null;
    }
    private boolean settingVisible(Module.Setting setting) {
        if (setting.tab != null && !setting.tab.equals(currentTab(selected))) {
            return false;
        }
        if (setting.nestUnder == null || setting.nestUnder.isBlank()) {
            return true;
        }
        Module.Setting parent = settingNamed(setting.nestUnder);
        if (parent == null) {
            return false;
        }
        if (parent.kind == Module.Setting.Kind.BOOL && parent.boolGet != null) {
            boolean on = parent.boolGet.getAsBoolean();
            if (setting.showWhenOff) {
                return !on;
            }
            // Options under a switch only show while it's on; the expand arrow can still fold them away.
            return on && (!parent.group || groupExpanded(parent));
        }
        return groupExpanded(parent);
    }

    private final java.util.Map<String, String> settingsTab = new java.util.HashMap<>();

    /** Distinct tab names for a module, in the order they first appear among its settings. */
    private List<String> tabsFor(Module module) {
        List<String> tabs = new ArrayList<>();
        if (module == null) {
            return tabs;
        }
        for (Module.Setting setting : module.settings) {
            if (setting.tab != null && !tabs.contains(setting.tab)) {
                tabs.add(setting.tab);
            }
        }
        return tabs;
    }
    private String currentTab(Module module) {
        List<String> tabs = tabsFor(module);
        if (tabs.isEmpty()) {
            return null;
        }
        String saved = settingsTab.get(module.name);
        return saved != null && tabs.contains(saved) ? saved : tabs.get(0);
    }

    @Override
    protected void init() {
        super.init();
        topTab = lastTopTab;
        youTab = lastYouTab;
        openCard = lastOpenCard;
        cardAnimAt = 0;
        closing = false;
        rebuild();
        selected = lastSelected != null && visible.contains(lastSelected) ? lastSelected
                : !visible.isEmpty() ? visible.get(0) : null;
        Shards.refresh(false);
    }

    @Override
    public void removed() {
        super.removed();
        lastOpenCard = closing ? -1 : openCard;
        lastSelected = selected;
        lastTopTab = topTab;
        lastYouTab = youTab;
    }

    private boolean searching() {
        return !search.isBlank();
    }

    private static boolean settingMatches(Module module, String q) {
        for (Module.Setting setting : module.settings) {
            if (setting.name != null && setting.name.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    private void rebuild() {
        visible.clear();
        if (AeroClient.MODULES == null) {
            return;
        }
        String q = search.toLowerCase(Locale.ROOT).trim();
        Category cat = openCard >= 0 && openCard < CARDS.size() ? CARDS.get(openCard).category() : null;
        for (Module module : AeroClient.MODULES.all) {
            if (!module.category.inSidebar()) {
                continue;
            }
            if (!q.isEmpty()) {
                if (!module.name.toLowerCase(Locale.ROOT).contains(q)
                        && !module.description.toLowerCase(Locale.ROOT).contains(q)
                        && !settingMatches(module, q)) {
                    continue;
                }
            } else if (cat == null || module.category != cat) {
                continue;
            }
            visible.add(module);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH() - listH0)));
        if (!visible.isEmpty() && (selected == null || !visible.contains(selected))) {
            selected = visible.get(0);
        }
    }

    private int sectionGaps() {
        if (!searching()) {
            return 0;
        }
        Category last = null;
        int extra = 0;
        for (Module module : visible) {
            if (module.category != last) {
                extra += 20;
                last = module.category;
            }
        }
        return extra;
    }

    private int contentH() {
        return visible.size() * ROW_H + sectionGaps();
    }

    private int listX() {
        return ox + SIDE_W + 8;
    }

    // ---- card animation ---------------------------------------------------------------------

    /** 0 = card grid, 1 = card fully open (eased). */
    private float cardT() {
        if (openCard < 0) {
            return 0f;
        }
        float e = cardAnimAt == 0 ? 1f : Math.min(1f, (System.currentTimeMillis() - cardAnimAt) / (float) ANIM_MS);
        if (closing && e >= 1f) {
            openCard = -1;
            closing = false;
            rebuild();
            return 0f;
        }
        float t = closing ? 1f - e : e;
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private void openCardAt(int i) {
        CardDef c = CARDS.get(i);
        if (c.kind() == 1) {
            client.setScreen(new ProfilesScreen(this));
            return;
        }
        if (c.kind() == 2) {
            client.setScreen(new HudLayoutScreen(this));
            return;
        }
        openCard = i;
        closing = false;
        cardAnimAt = System.currentTimeMillis();
        scroll = 0;
        settingsScroll = 0;
        selected = null;
        rebuild();
    }

    private void closeCard() {
        if (openCard >= 0 && !closing) {
            closing = true;
            cardAnimAt = System.currentTimeMillis();
        }
    }

    private int contentX() {
        return ox + 12;
    }

    private int contentY() {
        return oy + TOP + 6;
    }

    private int contentW() {
        return pw - 24;
    }

    private int contentH0() {
        return ph - TOP - 18;
    }

    /** Grid slot of card i: {x, y, w, h}. */
    private int[] cardRect(int i) {
        int cols = 3;
        int rows = (CARDS.size() + cols - 1) / cols;
        int gap = 8;
        int w = (contentW() - gap * (cols - 1)) / cols;
        int h = (contentH0() - gap * (rows - 1)) / rows;
        return new int[]{contentX() + (i % cols) * (w + gap), contentY() + (i / cols) * (h + gap), w, h};
    }

    // ---- render ---------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiDraw.menuOpen = true;
        try {
            layoutPanel();
            try {
                applyBlur(context);
            } catch (Throwable ignored) {
            }
            context.fillGradient(0, 0, width, height, 0x38F2F5FA, 0x5CDCE3EE);
            if (AeroClient.MODULES == null) {
                return;
            }
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(s, s);
            try {
                renderScaled(context, lm(mouseX), lm(mouseY));
            } finally {
                context.getMatrices().popMatrix();
            }
        } catch (Throwable t) {
            context.drawText(textRenderer, Text.literal("Menu draw failed: " + t), 16, 16, 0xFFFF5555, true);
        } finally {
            UiDraw.fade = 1f;
            UiDraw.menuOpen = false;
        }
    }

    private void renderScaled(DrawContext context, int mx, int my) {
        UiDraw.glass(context, ox, oy, pw, ph, 0, 22);
        drawTop(context, mx, my);
        if (topTab == 1) {
            drawYou(context, mx, my);
        } else if (topTab == 2) {
            SIDE_W = 150;
            RIGHT_W = 200;
            drawFriends(context, mx, my);
        } else {
            drawClient(context, mx, my);
        }
    }

    private int tabX(int i) {
        return ox + 118 + i * 74;
    }

    private int searchX() {
        return ox + pw - 16 - 20 - 8 - shardChipW() - 8 - 128;
    }

    private int shardChipW() {
        return textRenderer.getWidth(Shards.balanceLabel()) + 26;
    }

    private void drawTop(DrawContext context, int mx, int my) {
        int x0 = ox;
        int y0 = oy;
        int x1 = ox + pw;

        UiDraw.aeroMark(context, x0 + 16, y0 + 11, 18, UiDraw.accent());
        context.drawText(textRenderer, Text.literal("Aero"), x0 + 38, y0 + 12, TEXT, false);
        context.drawText(textRenderer, Text.literal(AeroClient.VERSION), x0 + 38, y0 + 22, MUTED, false);

        if (ModUpdater.state() == ModUpdater.State.IDLE) {
            ModUpdater.check();
        }
        if (y0 >= 24) {
            String ul = ModUpdater.label();
            int uw = textRenderer.getWidth(ul) + 16;
            int ux = x1 - uw - 6;
            boolean uh = inside(mx, my, ux, y0 - 22, uw, 18);
            UiDraw.pill(context, ux, y0 - 22, uw, 18, uh);
            context.drawText(textRenderer, Text.literal(ul), ux + 8, y0 - 17,
                    ModUpdater.state() == ModUpdater.State.AVAILABLE ? UiDraw.accent() : MUTED, false);
        }

        // Segmented tab control
        String[] tabs = {"Client", "You", "Friends"};
        UiDraw.roundRect(context, tabX(0) - 3, y0 + 9, 3 * 74 + 2, 24, 12, 0x14000000);
        for (int i = 0; i < 3; i++) {
            int tx = tabX(i);
            boolean on = topTab == i;
            boolean h = inside(mx, my, tx, y0 + 12, 70, 18);
            if (on) {
                UiDraw.roundRect(context, tx, y0 + 13, 70, 18, 9, 0x10000000);
                UiDraw.roundRect(context, tx, y0 + 12, 70, 18, 9, 0xFFFFFFFF);
            } else if (h) {
                UiDraw.roundRect(context, tx, y0 + 12, 70, 18, 9, 0x66FFFFFF);
            }
            int tw = textRenderer.getWidth(tabs[i]);
            context.drawText(textRenderer, Text.literal(tabs[i]), tx + (70 - tw) / 2, y0 + 17, on ? TEXT : MUTED, false);
        }

        if (topTab == 0) {
            int sx = searchX();
            UiDraw.field(context, sx, y0 + 10, 128, 22, searchFocus);
            drawSearchGlyph(context, sx + 8, y0 + 16, searchFocus ? UiDraw.accent() : MUTED);
            String q = search.isEmpty() && !searchFocus ? "Search" : search + (searchFocus ? "|" : "");
            context.drawText(textRenderer, Text.literal(fitLeft(q, 96)), sx + 22, y0 + 17,
                    search.isEmpty() && !searchFocus ? MUTED : TEXT, false);
        }

        int chipW = shardChipW();
        int chipX = x1 - 16 - 20 - 8 - chipW;
        boolean chipH = inside(mx, my, chipX, y0 + 10, chipW, 22);
        UiDraw.roundRect(context, chipX, y0 + 10, chipW, 22, 11, chipH ? 0xFFFFFFFF : 0xB4FFFFFF);
        UiDraw.roundBorder(context, chipX, y0 + 10, chipW, 22, 11, 0x14000000);
        Shards.drawGem(context, chipX + 7, y0 + 15, 12);
        context.drawText(textRenderer, Text.literal(Shards.balanceLabel()), chipX + 22, y0 + 17, TEXT, false);

        int closeX = x1 - 16 - 20;
        boolean closeH = inside(mx, my, closeX, y0 + 11, 20, 20);
        UiDraw.roundRect(context, closeX, y0 + 11, 20, 20, 10, closeH ? 0xFFFF5F57 : 0x12000000);
        context.drawText(textRenderer, Text.literal("×"), closeX + 7, y0 + 17, closeH ? 0xFFFFFFFF : MUTED, false);
        UiDraw.divider(context, x0 + 16, y0 + TOP, pw - 32);
    }

    private static void drawSearchGlyph(DrawContext context, int x, int y, int col) {
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            int px = x + 3 + (int) Math.round(Math.cos(rad) * 3);
            int py = y + 3 + (int) Math.round(Math.sin(rad) * 3);
            context.fill(px, py, px + 1, py + 1, col);
        }
        context.fill(x + 6, y + 6, x + 8, y + 8, col);
        context.fill(x + 7, y + 7, x + 9, y + 9, col);
    }

    /** Keeps the end of a string visible (search field while typing). */
    private String fitLeft(String text, int maxW) {
        if (textRenderer.getWidth(text) <= maxW) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && textRenderer.getWidth("…" + cut) > maxW) {
            cut = cut.substring(1);
        }
        return "…" + cut;
    }

    // ---- Client tab: cards ------------------------------------------------------------------

    static int catColor(Category category) {
        if (category == null) {
            return 0xFF3B82F6;
        }
        return switch (category) {
            case PVP -> 0xFFEF4444;
            case HUD -> 0xFF0EA5E9;
            case RENDER -> 0xFF8B5CF6;
            case PLAYER -> 0xFF10B981;
            case MISC -> 0xFF64748B;
            case PERFORMANCE -> 0xFFF59E0B;
            case PREVIEW -> 0xFFEC4899;
        };
    }

    private void drawClient(DrawContext context, int mx, int my) {
        if (searching()) {
            UiDraw.roundRect(context, contentX(), contentY(), contentW(), contentH0(), 14, 0xD8FFFFFF);
            UiDraw.roundBorder(context, contentX(), contentY(), contentW(), contentH0(), 14, 0x10000000);
            drawExpanded(context, mx, my, "Search", "\"" + search.trim() + "\"", null);
            return;
        }
        float t = cardT();
        int[] full = {contentX(), contentY(), contentW(), contentH0()};
        int[] sel = openCard >= 0 ? cardRect(openCard) : null;
        for (int i = 0; i < CARDS.size(); i++) {
            if (i == openCard) {
                continue;
            }
            int[] r = cardRect(i);
            int ddx = 0;
            int ddy = 0;
            if (sel != null) {
                float vx = (r[0] + r[2] / 2f) - (sel[0] + sel[2] / 2f);
                float vy = (r[1] + r[3] / 2f) - (sel[1] + sel[3] / 2f);
                float len = Math.max(1f, (float) Math.sqrt(vx * vx + vy * vy));
                ddx = Math.round(vx / len * 60f * t);
                ddy = Math.round(vy / len * 60f * t);
                UiDraw.fade = 1f - t;
            }
            if (UiDraw.fade > 0.02f) {
                drawCard(context, CARDS.get(i), r[0] + ddx, r[1] + ddy, r[2], r[3],
                        openCard < 0 && inside(mx, my, r[0], r[1], r[2], r[3]), false);
            }
            UiDraw.fade = 1f;
        }
        if (sel != null) {
            int x = Math.round(sel[0] + (full[0] - sel[0]) * t);
            int y = Math.round(sel[1] + (full[1] - sel[1]) * t);
            int w = Math.round(sel[2] + (full[2] - sel[2]) * t);
            int h = Math.round(sel[3] + (full[3] - sel[3]) * t);
            drawCard(context, CARDS.get(openCard), x, y, w, h, false, t > 0.98f);
            if (t > 0.98f && !closing) {
                CardDef c = CARDS.get(openCard);
                int on = 0;
                for (Module m : visible) {
                    if (m.enabled()) {
                        on++;
                    }
                }
                drawExpanded(context, mx, my, c.title(), visible.size() + " modules · " + on + " on", c.category());
            }
        }
    }

    private void drawCard(DrawContext context, CardDef c, int x, int y, int w, int h, boolean hover, boolean open) {
        int col = catColor(c.category());
        if (c.kind() == 1) {
            col = 0xFF14B8A6;
        } else if (c.kind() == 2) {
            col = 0xFF6366F1;
        }
        int yy = hover ? y - 2 : y;
        UiDraw.roundRect(context, x + 1, yy + 3, w - 2, h, 14, hover ? 0x14000000 : 0x0A000000);
        UiDraw.roundRect(context, x, yy, w, h, 14, open ? 0xE8FFFFFF : hover ? 0xFAFFFFFF : 0xC4FFFFFF);
        UiDraw.roundBorder(context, x, yy, w, h, 14, hover ? UiDraw.withAlpha(col, 0x90) : 0x12000000);
        if (open) {
            return;
        }
        UiDraw.roundRect(context, x + 12, yy + 12, 24, 24, 12, UiDraw.withAlpha(col, 0x24));
        drawCardIcon(context, c, x + 19, yy + 19, UiDraw.fa(col));
        context.drawText(textRenderer, Text.literal(c.title()), x + 44, yy + 13, UiDraw.fa(TEXT), false);
        String sub;
        String preview = "";
        if (c.kind() == 0) {
            int total = 0;
            int on = 0;
            StringBuilder names = new StringBuilder();
            for (Module m : AeroClient.MODULES.all) {
                if (m.category != c.category()) {
                    continue;
                }
                total++;
                if (m.enabled()) {
                    on++;
                    if (names.length() < 80) {
                        names.append(names.length() == 0 ? "" : " · ").append(m.name);
                    }
                }
            }
            sub = total + " modules · " + on + " on";
            preview = names.length() == 0 ? "Nothing enabled yet" : names.toString();
        } else if (c.kind() == 1) {
            sub = "Saved and public setups";
            preview = "Switch or share your settings";
        } else {
            sub = "Move, hide and lock";
            preview = "Drag every HUD element";
        }
        context.drawText(textRenderer, Text.literal(fit(sub, w - 56)), x + 44, yy + 24, UiDraw.fa(MUTED), false);
        if (h > 62) {
            context.drawText(textRenderer, Text.literal(fit(preview, w - 24)), x + 12, yy + h - 18, UiDraw.fa(0xFF8A90A0), false);
        }
        context.drawText(textRenderer, Text.literal("›"), x + w - 16, yy + 13, UiDraw.fa(hover ? col : 0xFFB0B6C2), false);
    }

    private static void drawCardIcon(DrawContext context, CardDef c, int x, int y, int col) {
        if (c.kind() == 1) {
            // sliders
            context.fill(x, y + 1, x + 10, y + 2, col);
            context.fill(x, y + 5, x + 10, y + 6, col);
            context.fill(x, y + 9, x + 10, y + 10, col);
            context.fill(x + 6, y - 1, x + 8, y + 4, col);
            context.fill(x + 2, y + 3, x + 4, y + 8, col);
            context.fill(x + 5, y + 7, x + 7, y + 12, col);
        } else if (c.kind() == 2) {
            iconMonitor(context, x, y, col);
        } else {
            iconForCategory(context, c.category(), x, y, col);
        }
    }

    /** Open card (or search results): module list on the left, settings of the picked module on the right. */
    private void drawExpanded(DrawContext context, int mx, int my, String title, String sub, Category category) {
        int x = contentX();
        int y = contentY();
        int w = contentW();
        int h = contentH0();
        boolean backH = inside(mx, my, x + 10, y + 9, 22, 22);
        UiDraw.roundRect(context, x + 10, y + 9, 22, 22, 11, backH ? 0xFFFFFFFF : 0x10000000);
        context.drawText(textRenderer, Text.literal("‹"), x + 18, y + 16, backH ? UiDraw.accent() : TEXT, false);
        if (category != null) {
            UiDraw.roundRect(context, x + 38, y + 9, 22, 22, 11, UiDraw.withAlpha(catColor(category), 0x24));
            iconForCategory(context, category, x + 44, y + 15, catColor(category));
        }
        int tx = x + (category != null ? 66 : 40);
        context.drawText(textRenderer, Text.literal(title), tx, y + 10, TEXT, false);
        context.drawText(textRenderer, Text.literal(fit(sub, w / 2 - 80)), tx, y + 21, MUTED, false);

        listX0 = x + 8;
        listY0 = y + 38;
        listW0 = w * 46 / 100;
        listH0 = h - 44;
        paneX = listX0 + listW0 + 10;
        paneY = y + 4;
        RIGHT_W = x + w - 6 - paneX;
        paneBottom = y + h - 2;
        context.fill(paneX - 5, y + 12, paneX - 4, y + h - 12, 0x12000000);
        drawList(context, mx, my);
        drawRight(context, mx, my);
    }

    private void drawList(DrawContext context, int mx, int my) {
        int x = listX0;
        int y0 = listY0;
        int w = listW0;
        int view = listH0;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH() - view)));
        context.enableScissor(x, y0, x + w, y0 + view);
        try {
            if (visible.isEmpty()) {
                context.drawText(textRenderer, Text.literal("No modules match."), x + 12, y0 + 16, MUTED, false);
            }
            int y = y0 - scroll;
            Category last = null;
            int bottom = y0 + view;
            for (Module module : visible) {
                if (searching() && module.category != last) {
                    last = module.category;
                    if (y + 20 >= y0 && y <= bottom) {
                        context.drawText(textRenderer, Text.literal(last.title.toUpperCase(Locale.ROOT)),
                                x + 10, y + 7, catColor(last), false);
                    }
                    y += 20;
                }
                if (y + ROW_H < y0) {
                    y += ROW_H;
                    continue;
                }
                if (y > bottom) {
                    break;
                }
                boolean h = inside(mx, my, x, y, w, ROW_H - 2) && my >= y0 && my < bottom;
                boolean sel = module == selected;
                int rh = ROW_H - 4;
                if (sel) {
                    UiDraw.roundRect(context, x + 2, y, w - 8, rh, 10, UiDraw.withAlpha(UiDraw.accent(), 0x1C));
                    UiDraw.roundBorder(context, x + 2, y, w - 8, rh, 10, UiDraw.withAlpha(UiDraw.accent(), 0x55));
                } else if (h) {
                    UiDraw.roundRect(context, x + 2, y, w - 8, rh, 10, UiDraw.HOVER);
                }
                drawModIcon(context, module, x + 10, y + 8);
                int nameMax = w - 80;
                context.drawText(textRenderer, Text.literal(fit(module.name, nameMax)), x + 28, y + 4, TEXT, false);
                String second;
                int key = module.style().toggleKey;
                if (key >= 0) {
                    second = "Key " + keyLabel(key);
                } else {
                    second = module.description;
                }
                context.drawText(textRenderer, Text.literal(fit(second, nameMax)), x + 28, y + 14, MUTED, false);
                drawSwitch(context, x + w - 42, y + 5, module.enabled());
                y += ROW_H;
            }
        } finally {
            context.disableScissor();
        }
        UiDraw.scrollbar(context, x + w - 4, y0, view, scroll, contentH(), view);
    }

    private void drawRight(DrawContext context, int mx, int my) {
        int x = paneX;
        int y0 = paneY;
        int y1 = paneBottom;
        context.fill(x, y0, (paneX + RIGHT_W), y1, 0);
        context.fill(x, y0, x + 1, y1, 0);

        if (selected == null) {
            context.drawText(textRenderer, Text.literal("Settings"), x + 16, y0 + 16, TEXT, false);
            context.drawText(textRenderer, Text.literal("Pick a module on the left."), x + 16, y0 + 32, MUTED, false);
            return;
        }

        context.drawText(textRenderer, Text.literal(fit(selected.name, RIGHT_W - 110)), x + 14, y0 + 12, TEXT, false);
        drawSwitch(context, (paneX + RIGHT_W) - 42, y0 + 10, selected.enabled());
        boolean resetHover = inside(mx, my, (paneX + RIGHT_W) - 42 - 46, y0 + 9, 40, 16);
        UiDraw.pill(context, (paneX + RIGHT_W) - 42 - 46, y0 + 9, 40, 16, resetHover);
        context.drawText(textRenderer, Text.literal("Reset"), (paneX + RIGHT_W) - 42 - 46 + 6, y0 + 13,
                resetHover ? TEXT : MUTED, false);
        wrap(context, selected.description, x + 14, y0 + 30, RIGHT_W - 28, MUTED, 3);
        UiDraw.divider(context, x + 14, settingsStartY() - 8, RIGHT_W - 28);

        if (scrollModule != selected) {
            scrollModule = selected;
            settingsScroll = 0;
        }
        int viewTop = settingsStartY() - 6;
        int viewBottom = settingsViewBottom();
        settingsScroll = Math.max(0, Math.min(settingsScroll, Math.max(0, settingsContentH - (viewBottom - viewTop))));
        int contentStart = settingsStartY() - settingsScroll;
        int y = contentStart;
        context.enableScissor(x + 1, viewTop, (paneX + RIGHT_W), viewBottom);
        try {
        Module.ModuleStyle style = selected.style();
        String keyLabel = "Emotes".equals(selected.name) ? "Hold Key" : "Toggle Key";
        context.drawText(textRenderer, Text.literal(keyLabel), x + 14, y + 4, MUTED, false);
        String keyName = capturingKeybind == selected ? "Press a key..." : keyLabel(style.toggleKey);
        int kpw = pillW(keyLabel);
        int kpx = x + RIGHT_W - 14 - kpw;
        UiDraw.pill(context, kpx, y - 3, kpw, 20, capturingKeybind == selected);
        keyName = fit(keyName, kpw - 8);
        int kw = textRenderer.getWidth(keyName);
        context.drawText(textRenderer, Text.literal(keyName), kpx + (kpw - kw) / 2, y + 3, TEXT, false);
        y += 28;

        if ("Emotes".equals(selected.name)) {
            context.drawText(textRenderer, Text.literal("Edit pose"), x + 14, y + 4, TEXT, false);
            int ppw = pillW("Edit pose");
            int ppx = x + RIGHT_W - 14 - ppw;
            boolean poseH = inside(mx, my, ppx, y - 3, ppw, 20);
            UiDraw.pill(context, ppx, y - 3, ppw, 20, poseH);
            int ow = textRenderer.getWidth("Open");
            context.drawText(textRenderer, Text.literal("Open"), ppx + (ppw - ow) / 2, y + 3, TEXT, false);
            y += 28;
        }

        List<String> tabs = tabsFor(selected);
        if (!tabs.isEmpty()) {
            String active = currentTab(selected);
            int tabX = x + 14;
            int tabW = (RIGHT_W - 28) / tabs.size();
            for (String tabName : tabs) {
                boolean on = tabName.equals(active);
                UiDraw.pill(context, tabX, y - 3, tabW - 4, 20, on);
                int tw = textRenderer.getWidth(tabName);
                context.drawText(textRenderer, Text.literal(tabName), tabX + (tabW - 4 - tw) / 2, y + 3,
                        on ? TEXT : MUTED, false);
                tabX += tabW;
            }
            y += 28;
        }

        if (selected.settings.isEmpty() && !"Emotes".equals(selected.name)) {
            context.drawText(textRenderer, Text.literal("No extra options."), x + 14, y, MUTED, false);
        }
        for (Module.Setting setting : selected.settings) {
            if (!settingVisible(setting)) {
                continue;
            }
            int indent = setting.nestUnder == null ? 0 : 12;
            int tx = x + 14 + indent;
            if (setting.numeric) {
                context.drawText(textRenderer, Text.literal(setting.name), tx, y, TEXT, false);
                String val = setting.kind == Module.Setting.Kind.FLOAT
                        ? (Math.abs(setting.floatGet.getAsDouble() - Math.round(setting.floatGet.getAsDouble())) < 0.05
                        ? String.valueOf(Math.round(setting.floatGet.getAsDouble()))
                        : String.format(java.util.Locale.ROOT, "%.2f", setting.floatGet.getAsDouble()))
                        : String.valueOf(setting.intGet.get());
                context.drawText(textRenderer, Text.literal(val),
                        x + RIGHT_W - 28 - textRenderer.getWidth(val), y, MUTED, false);
                int barX = tx;
                int barW = RIGHT_W - 28 - indent;
                float t;
                if (setting.kind == Module.Setting.Kind.FLOAT) {
                    t = (float) ((setting.floatGet.getAsDouble() - setting.fMin) / Math.max(0.0001, setting.fMax - setting.fMin));
                } else {
                    t = (setting.intGet.get() - setting.min) / (float) Math.max(1, setting.max - setting.min);
                }
                UiDraw.slider(context, barX, y + 14, barW, t);
                y += 32;
            } else if (setting.kind == Module.Setting.Kind.CHOICE) {
                context.drawText(textRenderer, Text.literal(setting.name), tx, y + 4, TEXT, false);
                String cur = setting.choiceGet.get() == null ? "" : setting.choiceGet.get();
                int cw = Math.max(78, textRenderer.getWidth(cur) + 16);
                UiDraw.pill(context, x + RIGHT_W - 14 - cw, y - 3, cw, 20, false);
                context.drawText(textRenderer, Text.literal(cur),
                        x + RIGHT_W - 14 - cw + (cw - textRenderer.getWidth(cur)) / 2, y + 3, TEXT, false);
                y += 26;
            } else if (setting.kind == Module.Setting.Kind.TEXT) {
                context.drawText(textRenderer, Text.literal(setting.name), tx, y, MUTED, false);
                boolean focused = setting == focusedTextSetting;
                UiDraw.inset(context, tx, y + 12, RIGHT_W - 28 - indent, 18, focused ? 0xFFFFFFFF : CARD);
                String shown = focused ? textDraft + "|" : setting.choiceGet.get();
                if (shown == null || shown.isBlank()) {
                    shown = "Empty";
                }
                context.drawText(textRenderer, Text.literal(shown), tx + 6, y + 17, focused ? TEXT : MUTED, false);
                y += 34;
            } else if (setting.kind == Module.Setting.Kind.KEY) {
                context.drawText(textRenderer, Text.literal(setting.name), tx, y + 4, TEXT, false);
                String kl = keyCapture == setting ? "Press a key..." : keyLabel(setting.intGet.get());
                int kw2 = Math.max(52, Math.min(RIGHT_W - 28 - textRenderer.getWidth(setting.name) - 6, textRenderer.getWidth(kl) + 16));
                int kx2 = x + RIGHT_W - 14 - kw2;
                UiDraw.pill(context, kx2, y - 3, kw2, 20, keyCapture == setting);
                String shown2 = fit(kl, kw2 - 8);
                context.drawText(textRenderer, Text.literal(shown2), kx2 + (kw2 - textRenderer.getWidth(shown2)) / 2, y + 3, TEXT, false);
                y += 26;
            } else if (setting.kind == Module.Setting.Kind.COLOR) {
                context.drawText(textRenderer, Text.literal(setting.name), tx, y + 4, TEXT, false);
                int rgb = setting.intGet.get() & 0xFFFFFF;
                String hex = colorFocus == setting ? colorDraft : String.format("#%06X", rgb);
                if (tx - x + textRenderer.getWidth(setting.name) + textRenderer.getWidth(hex) + 52 < RIGHT_W) {
                    context.drawText(textRenderer, Text.literal(hex),
                            x + RIGHT_W - 44 - textRenderer.getWidth(hex), y + 4, colorFocus == setting ? TEXT : MUTED, false);
                }
                UiDraw.raised(context, x + RIGHT_W - 36, y, 18, 12, 0xFF000000 | rgb);
                y += 22;
                if (colorOpen == setting) {
                    int barX = tx;
                    int barW = RIGHT_W - 28 - indent;
                    for (int i = 0; i < barW; i++) {
                        int hc = java.awt.Color.HSBtoRGB(i / (float) barW, 0.85f, 1f) & 0xFFFFFF;
                        context.fill(barX + i, y, barX + i + 1, y + 10, 0xFF000000 | hc);
                    }
                    UiDraw.roundBorder(context, barX - 1, y - 1, barW + 2, 12, 3, 0x22000000);
                    int sw2 = Math.min(16, (barW - 4) / PRESETS.length - 3);
                    for (int i = 0; i < PRESETS.length; i++) {
                        int px2 = barX + i * (sw2 + 3);
                        UiDraw.roundRect(context, px2, y + 15, sw2, sw2, 3, 0xFF000000 | PRESETS[i]);
                        if ((rgb & 0xFFFFFF) == PRESETS[i]) {
                            UiDraw.roundBorder(context, px2 - 1, y + 14, sw2 + 2, sw2 + 2, 3, UiDraw.accent());
                        }
                    }
                    y += 20 + sw2 + 6;
                }
            } else if (setting.kind == Module.Setting.Kind.ACTION) {
                if (setting.group) {
                    context.drawText(textRenderer, Text.literal(groupExpanded(setting) ? "v" : ">"), tx, y + 4, MUTED, false);
                }
                context.drawText(textRenderer, Text.literal(setting.name), tx + (setting.group ? 10 : 0), y + 4, TEXT, false);
                String lab = setting.actionLabel == null ? "Open" : setting.actionLabel;
                int aw = Math.max(52, textRenderer.getWidth(lab) + 16);
                boolean ah = inside(mx, my, x + RIGHT_W - 14 - aw, y - 3, aw, 20);
                UiDraw.pill(context, x + RIGHT_W - 14 - aw, y - 3, aw, 20, ah);
                context.drawText(textRenderer, Text.literal(lab),
                        x + RIGHT_W - 14 - aw + (aw - textRenderer.getWidth(lab)) / 2, y + 3, TEXT, false);
                y += 26;
            } else {
                if (setting.group) {
                    context.drawText(textRenderer, Text.literal(groupExpanded(setting) ? "v" : ">"), tx, y + 4, MUTED, false);
                }
                context.drawText(textRenderer, Text.literal(setting.name), tx + (setting.group ? 10 : 0), y + 4, TEXT, false);
                drawSwitch(context, x + RIGHT_W - 48, y + 2, setting.boolGet.getAsBoolean());
                y += 22;
            }
        }
        } finally {
            context.disableScissor();
        }
        settingsContentH = y - contentStart + 12;
        UiDraw.scrollbar(context, (paneX + RIGHT_W) - 16, viewTop, viewBottom - viewTop, settingsScroll, settingsContentH, viewBottom - viewTop);
    }
    private int pillW(String label) {
        return Math.max(40, Math.min(78, RIGHT_W - 28 - textRenderer.getWidth(label) - 6));
    }
    private int settingsStartY() {
        int lines = selected == null ? 1 : descLines(selected.description, RIGHT_W - 28);
        return paneY + 30 + lines * 10 + 14;
    }
    private int settingsViewBottom() {
        return paneBottom - 12;
    }

    private int youLeft() {
        return ox + 8;
    }

    private int youMid() {
        return listX();
    }

    private int youRight() {
        return ox + pw - RIGHT_W;
    }

    private String ownSkinKey() {
        SavedAccount acc = AccountManager.account;
        if (acc != null && acc.uuid != null) {
            return acc.uuid.toString().replace("-", "").toLowerCase();
        }
        return AccountManager.currentName().toLowerCase();
    }
    private String wardrobeSkinKey() {
        if (!playerLookup.isBlank()) {
            return playerLookup.trim().toLowerCase();
        }
        return ownSkinKey();
    }
    /** The friend's live PlayerEntity if they're currently in this world/server, else null. */
    private static net.minecraft.entity.player.PlayerEntity onlinePlayerEntity(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || name == null || name.isBlank()) {
            return null;
        }
        for (var p : mc.world.getPlayers()) {
            try {
                if (name.equalsIgnoreCase(p.getName().getString())) {
                    return p;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
    private void drawFriends(DrawContext context, int mx, int my) {
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = youLeft();
        int mid = youMid();
        int right = youRight();
        int leftW = mid - left - 8;
        int midW = right - mid - 8;

        UiDraw.innerCard(context, left, y0 + 6, leftW, ph - TOP - 14);
        UiDraw.innerCard(context, mid, y0 + 6, midW, ph - TOP - 14);
        UiDraw.innerCard(context, right + 2, y0 + 6, RIGHT_W - 10, ph - TOP - 14);

        int lx = left + 10;
        int lw = leftW - 20;
        UiDraw.field(context, lx, y0 + 16, lw, 22, friendFocus);
        String draft = friendDraft.isEmpty() && !friendFocus ? "Add a friend…" : friendDraft + (friendFocus ? "|" : "");
        context.drawText(textRenderer, Text.literal(fit(draft, lw - 16)), lx + 8, y0 + 22,
                friendDraft.isEmpty() && !friendFocus ? MUTED : TEXT, false);

        UiDraw.pill(context, lx, y0 + 44, lw, 20, true);
        String count = "Friends  " + FriendStore.size();
        context.drawText(textRenderer, Text.literal(count),
                lx + (lw - textRenderer.getWidth(count)) / 2, y0 + 50, TEXT, false);

        int listTop = y0 + 72;
        context.enableScissor(left + 6, listTop, left + leftW - 6, y1 - 16);
        try {
            int y = listTop - friendScroll;
            if (FriendStore.size() == 0) {
                context.drawText(textRenderer, Text.literal("No friends yet."), lx, listTop + 8, MUTED, false);
            }
            for (int i = 0; i < FriendStore.size(); i++) {
                if (y + ROW_H < listTop) {
                    y += ROW_H;
                    continue;
                }
                if (y > y1 - 16) {
                    break;
                }
                String n = FriendStore.get(i);
                boolean on = FriendStore.online(n);
                boolean sel = friendSel == i;
                SkinPreview.requestLookup(n);
                if (sel) {
                    UiDraw.roundRect(context, lx - 2, y, lw + 4, ROW_H - 4, 10, UiDraw.withAlpha(UiDraw.accent(), 0x44));
                    UiDraw.roundRect(context, lx - 2, y + 6, 3, ROW_H - 16, 1, UiDraw.accent());
                } else if (inside(mx, my, lx - 2, y, lw + 4, ROW_H - 4)) {
                    UiDraw.roundRect(context, lx - 2, y, lw + 4, ROW_H - 4, 10, UiDraw.HOVER);
                }
                if (SkinPreview.ready(n.toLowerCase())) {
                    SkinPreview.drawHead(context, n.toLowerCase(), lx + 4, y + 6, 16);
                } else {
                    UiDraw.roundRect(context, lx + 6, y + 8, 12, 12, 6, on ? 0xFF22C55E : 0xFFC4C9D4);
                }
                context.drawText(textRenderer, Text.literal(fit(n, lw - 70)), lx + 24, y + 6, TEXT, false);
                context.drawText(textRenderer, Text.literal(on ? "online" : "offline"), lx + 24, y + 16, MUTED, false);
                y += ROW_H;
            }
        } finally {
            context.disableScissor();
        }

        int mx0 = mid + 12;
        context.drawText(textRenderer, Text.literal("You"), mx0, y0 + 16, MUTED, false);
        String own = FriendStore.ownName();
        SkinPreview.requestOwn();
        String ownKey = ownSkinKey();
        if (SkinPreview.ready(ownKey)) {
            SkinPreview.drawHead(context, ownKey, mx0 + 4, y0 + 34, 28);
        } else {
            UiDraw.roundRect(context, mx0 + 4, y0 + 34, 28, 28, 8, 0xFFC8A0E8);
        }
        context.drawText(textRenderer, Text.literal(fit(own, midW - 60)), mx0 + 46, y0 + 42, TEXT, false);
        context.drawText(textRenderer, Text.literal("Your list"), mx0 + 46, y0 + 54, MUTED, false);

        context.drawText(textRenderer, Text.literal("Pick a friend on the left."), mx0, y0 + 88, MUTED, false);
        context.drawText(textRenderer, Text.literal("Enter adds. Delete removes."), mx0, y0 + 102, MUTED, false);

        int rx = right + 14;
        if (friendSel >= 0 && friendSel < FriendStore.size()) {
            String picked = FriendStore.get(friendSel);
            SkinPreview.requestLookup(picked);
            context.drawText(textRenderer, Text.literal(fit(picked, RIGHT_W - 36)), rx, y0 + 16, TEXT, false);
            context.drawText(textRenderer, Text.literal(FriendStore.online(picked) ? "Online now" : "Offline"), rx, y0 + 30, MUTED, false);
            var liveFriend = onlinePlayerEntity(picked);
            if (liveFriend != null) {
                // Online: render the real live entity in 3D (auto-spinning) instead of the
                // network-fetched flat skin, since we already have their model loaded anyway.
                CosmeticPreview.spin(context, rx + 40, y0 + 132, 30, liveFriend);
            } else if (SkinPreview.ready(picked.toLowerCase())) {
                SkinPreview.drawBody(context, picked.toLowerCase(), rx + 20, y0 + 50, 3);
            } else {
                UiDraw.roundRect(context, rx + 28, y0 + 54, 36, 90, 8, 0x14000000);
            }
            boolean remH = inside(mx, my, rx, y1 - 38, RIGHT_W - 28, 22);
            UiDraw.pill(context, rx, y1 - 38, RIGHT_W - 28, 22, remH);
            context.drawText(textRenderer, Text.literal("Remove"),
                    rx + (RIGHT_W - 28 - textRenderer.getWidth("Remove")) / 2, y1 - 32, remH ? TEXT : MUTED, false);
        } else {
            context.drawText(textRenderer, Text.literal("Nobody picked"), rx, y0 + 16, TEXT, false);
            wrap(context, "Select a friend to preview their skin and remove them.", rx, y0 + 34, RIGHT_W - 32, MUTED);
        }
    }

    // ---- You tab -------------------------------------------------------------------------

    private int youTabX(int i) {
        return ox + 16 + i * 80;
    }

    private void drawYou(DrawContext context, int mx, int my) {
        int y = oy + TOP + 8;
        for (int i = 0; i < YOU_TABS.length; i++) {
            int x = youTabX(i);
            boolean on = youTab == i;
            boolean h = inside(mx, my, x, y, 74, 20);
            UiDraw.pill(context, x, y, 74, 20, on || h);
            int tw = textRenderer.getWidth(YOU_TABS[i]);
            context.drawText(textRenderer, Text.literal(YOU_TABS[i]), x + (74 - tw) / 2, y + 6, on ? TEXT : MUTED, false);
        }
        String streak = Shards.streakLabel();
        if (!streak.isEmpty()) {
            context.drawText(textRenderer, Text.literal(streak), ox + pw - 16 - textRenderer.getWidth(streak), y + 6, MUTED, false);
        }
        int top = y + 22;
        if (youTab == 0) {
            wardrobe.render(context, mx, my, ox, top, pw, oy + ph - top);
        } else {
            shop.render(context, mx, my, ox + 12, top + 4, pw - 24, oy + ph - top - 16, youTab);
        }
    }

    // ---- icons ---------------------------------------------------------------------------

    /**
     * One deliberate glyph per module: a handful of the most-used modules get a bespoke icon by
     * name, everything else falls back to a fixed glyph for its category. Previously this picked
     * one of six meaningless blob shapes from module.name.hashCode(), which read as random noise
     * rather than actual icons.
     */
    private void drawModIcon(DrawContext context, Module module, int x, int y) {
        int col = catColor(module.category);
        switch (module.name) {
            case "FPS" -> iconBolt(context, x, y, col);
            case "Ping" -> iconBars(context, x, y, col);
            case "Keystrokes" -> iconKey(context, x, y, col);
            case "Potion HUD" -> iconFlask(context, x, y, col);
            case "Crosshair", "Crosshair Addons" -> iconTarget(context, x, y, col);
            case "Nametags" -> iconTag(context, x, y, col);
            case "Zoom" -> iconMagnifier(context, x, y, col);
            case "Watermark" -> UiDraw.aeroMark(context, x, y, 10, col);
            default -> iconForCategory(context, module.category, x, y, col);
        }
    }
    private static void iconForCategory(DrawContext context, Category category, int x, int y, int col) {
        switch (category) {
            case PVP -> iconCross(context, x, y, col);
            case HUD -> iconMonitor(context, x, y, col);
            case RENDER -> iconSparkle(context, x, y, col);
            case PLAYER -> iconPerson(context, x, y, col);
            case MISC -> iconDots(context, x, y, col);
            case PERFORMANCE -> iconBars(context, x, y, col);
            case PREVIEW -> iconSparkle(context, x, y, col);
        }
    }
    /** Crossed blades - PvP. */
    private static void iconCross(DrawContext context, int x, int y, int col) {
        for (int i = 0; i < 9; i++) {
            context.fill(x + i, y + i, x + i + 2, y + i + 2, col);
            context.fill(x + 8 - i, y + i, x + 10 - i, y + i + 2, col);
        }
    }
    /** Screen + stand - HUD. */
    private static void iconMonitor(DrawContext context, int x, int y, int col) {
        context.fill(x + 1, y + 1, x + 9, y + 2, col);
        context.fill(x + 1, y + 6, x + 9, y + 7, col);
        context.fill(x + 1, y + 1, x + 2, y + 7, col);
        context.fill(x + 8, y + 1, x + 9, y + 7, col);
        context.fill(x + 4, y + 7, x + 6, y + 9, col);
    }
    /** Four-point sparkle - Visuals. */
    private static void iconSparkle(DrawContext context, int x, int y, int col) {
        context.fill(x + 4, y, x + 6, y + 10, col);
        context.fill(x, y + 4, x + 10, y + 6, col);
    }
    /** Head + shoulders - Player. */
    private static void iconPerson(DrawContext context, int x, int y, int col) {
        context.fill(x + 3, y, x + 7, y + 4, col);
        context.fill(x + 2, y + 5, x + 8, y + 10, col);
    }
    /** Vertical ellipsis - Misc. */
    private static void iconDots(DrawContext context, int x, int y, int col) {
        context.fill(x + 3, y + 1, x + 6, y + 3, col);
        context.fill(x + 3, y + 4, x + 6, y + 6, col);
        context.fill(x + 3, y + 7, x + 6, y + 9, col);
    }
    /** Ascending bars - Performance / Ping / signal strength. */
    private static void iconBars(DrawContext context, int x, int y, int col) {
        context.fill(x + 1, y + 7, x + 3, y + 10, col);
        context.fill(x + 4, y + 4, x + 6, y + 10, col);
        context.fill(x + 7, y + 1, x + 9, y + 10, col);
    }
    /** Lightning bolt - FPS. */
    private static void iconBolt(DrawContext context, int x, int y, int col) {
        context.fill(x + 5, y, x + 8, y + 4, col);
        context.fill(x + 2, y + 4, x + 8, y + 6, col);
        context.fill(x + 2, y + 6, x + 5, y + 10, col);
    }
    /** Single keycap - Keystrokes. */
    private static void iconKey(DrawContext context, int x, int y, int col) {
        context.fill(x + 1, y + 1, x + 9, y + 9, col);
        context.fill(x + 3, y + 3, x + 7, y + 7, 0xFFFFFFFF);
    }
    /** Flask - Potion HUD. */
    private static void iconFlask(DrawContext context, int x, int y, int col) {
        context.fill(x + 4, y, x + 6, y + 3, col);
        context.fill(x + 2, y + 3, x + 8, y + 4, col);
        context.fill(x + 1, y + 4, x + 9, y + 10, col);
    }
    /** Target ring + center dot - Crosshair. */
    private static void iconTarget(DrawContext context, int x, int y, int col) {
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            int px = x + 5 + (int) Math.round(Math.cos(rad) * 4);
            int py = y + 5 + (int) Math.round(Math.sin(rad) * 4);
            context.fill(px, py, px + 2, py + 2, col);
        }
        context.fill(x + 4, y + 4, x + 6, y + 6, col);
    }
    /** Luggage tag - Nametags. */
    private static void iconTag(DrawContext context, int x, int y, int col) {
        context.fill(x + 1, y + 2, x + 7, y + 8, col);
        context.fill(x + 7, y + 3, x + 9, y + 7, col);
        context.fill(x + 3, y + 4, x + 5, y + 6, 0xFFFFFFFF);
    }
    /** Magnifying glass - Zoom. */
    private static void iconMagnifier(DrawContext context, int x, int y, int col) {
        for (int a = 0; a < 360; a += 30) {
            double rad = Math.toRadians(a);
            int px = x + 4 + (int) Math.round(Math.cos(rad) * 3);
            int py = y + 4 + (int) Math.round(Math.sin(rad) * 3);
            context.fill(px, py, px + 2, py + 2, col);
        }
        context.fill(x + 6, y + 6, x + 9, y + 9, col);
    }
    private void wrap(DrawContext context, String text, int x, int y, int max, int color) {
        wrap(context, text, x, y, max, color, Integer.MAX_VALUE);
    }
    private void wrap(DrawContext context, String text, int x, int y, int max, int color, int maxLines) {
        int lines = 0;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(next) > max && !line.isEmpty()) {
                if (++lines > maxLines) {
                    return;
                }
                context.drawText(textRenderer, Text.literal(line.toString()), x, y, color, false);
                y += 10;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty() && lines < maxLines) {
            context.drawText(textRenderer, Text.literal(line.toString()), x, y, color, false);
        }
    }
    private int descLines(String text, int max) {
        int lines = 1;
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(next) > max && !line.isEmpty()) {
                lines++;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        return Math.min(3, lines);
    }
    private void drawSwitch(DrawContext context, int x, int y, boolean on) {
        UiDraw.toggle(context, x, y, on);
    }
    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
    private String fit(String text, int maxW) {
        if (text == null) {
            return "";
        }
        if (textRenderer.getWidth(text) <= maxW) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && textRenderer.getWidth(cut + "…") > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    // ---- input ---------------------------------------------------------------------------

    public boolean handleClick(double rawX, double rawY, int button) {
        layoutPanel();
        int mx = lm(rawX);
        int my = lm(rawY);
        if (button != 0) {
            return false;
        }
        if (inside(mx, my, ox + pw - 16 - 20, oy + 11, 20, 20)) {
            closeMenu();
            return true;
        }
        int updW = textRenderer.getWidth(ModUpdater.label()) + 16;
        if (oy >= 24 && inside(mx, my, ox + pw - updW - 6, oy - 22, updW, 18)) {
            ModUpdater.click();
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (inside(mx, my, tabX(i), oy + 10, 70, 22)) {
                topTab = i;
                searchFocus = false;
                return true;
            }
        }
        int chipW = shardChipW();
        if (inside(mx, my, ox + pw - 16 - 20 - 8 - chipW, oy + 10, chipW, 22)) {
            topTab = 1;
            youTab = 2;
            Shards.refresh(true);
            return true;
        }
        if (topTab == 0 && inside(mx, my, searchX(), oy + 10, 128, 22)) {
            searchFocus = true;
            return true;
        }
        searchFocus = false;
        if (topTab == 1) {
            friendFocus = false;
            playerLookupFocus = false;
            int y = oy + TOP + 8;
            for (int i = 0; i < YOU_TABS.length; i++) {
                if (inside(mx, my, youTabX(i), y, 74, 20)) {
                    youTab = i;
                    if (i > 0) {
                        Shards.refresh(true);
                    }
                    return true;
                }
            }
            int top = y + 22;
            if (youTab == 0) {
                return wardrobe.click(mx, my, ox, top, pw, oy + ph - top);
            }
            boolean used = shop.click(mx, my, ox + 12, top + 4, pw - 24, oy + ph - top - 16, youTab);
            dev.aero.client.cosmetic.Cosmetics.Item buy = shop.takeBuyRequest();
            if (buy != null) {
                youTab = 0;
                wardrobe.openBuy(buy);
            }
            return used;
        }
        if (topTab == 2) {
            wardrobe.searchFocus = false;
            SIDE_W = 150;
            RIGHT_W = 200;
            return clickFriends(mx, my);
        }
        wardrobe.searchFocus = false;
        friendFocus = false;
        return clickClient(mx, my);
    }

    private boolean clickFriends(int mx, int my) {
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = youLeft();
        int mid = youMid();
        int right = youRight();
        int leftW = mid - left - 8;
        int lx = left + 10;
        int lw = leftW - 20;
        friendFocus = inside(mx, my, lx, y0 + 16, lw, 22);
        int listTop = y0 + 72;
        int y = listTop - friendScroll;
        for (int i = 0; i < FriendStore.size(); i++) {
            if (inside(mx, my, lx - 2, y, lw + 4, ROW_H - 4) && y >= listTop && y <= y1 - 16) {
                friendSel = i;
                return true;
            }
            y += ROW_H;
        }
        if (friendSel >= 0 && friendSel < FriendStore.size()
                && inside(mx, my, right + 14, y1 - 38, RIGHT_W - 28, 22)) {
            FriendStore.remove(friendSel);
            friendSel = Math.min(friendSel, FriendStore.size() - 1);
            return true;
        }
        return true;
    }

    private boolean clickClient(int mx, int my) {
        boolean expanded = searching() || (openCard >= 0 && !closing && cardT() > 0.98f);
        if (!expanded) {
            if (openCard >= 0) {
                return true; // mid-animation
            }
            for (int i = 0; i < CARDS.size(); i++) {
                int[] r = cardRect(i);
                if (inside(mx, my, r[0], r[1], r[2], r[3])) {
                    openCardAt(i);
                    return true;
                }
            }
            return false;
        }
        int x = contentX();
        int y = contentY();
        if (inside(mx, my, x + 10, y + 9, 22, 22)) {
            if (searching()) {
                search = "";
                rebuild();
            } else {
                closeCard();
            }
            return true;
        }
        if (selected != null && inside(mx, my, paneX + RIGHT_W - 42, paneY + 10, 28, 16)) {
            selected.toggle();
            AeroClient.CONFIG.save();
            return true;
        }
        if (selected != null && inside(mx, my, paneX + RIGHT_W - 42 - 46, paneY + 9, 40, 16)) {
            resetModule(selected);
            AeroClient.CONFIG.save();
            return true;
        }
        if (selected != null && clickSelectedSettings(mx, my)) {
            AeroClient.CONFIG.save();
            return true;
        }
        int lx = listX0;
        int rowY = listY0 - scroll;
        int w = listW0;
        Category last = null;
        for (Module module : visible) {
            if (searching() && module.category != last) {
                last = module.category;
                rowY += 20;
            }
            if (inside(mx, my, lx, rowY, w, ROW_H - 2) && my >= listY0 && my <= listY0 + listH0) {
                if (selected != module) {
                    focusedTextSetting = null;
                    accentFocus = false;
                    capturingKeybind = null;
                    settingsScroll = 0;
                }
                selected = module;
                if (mx >= lx + w - 48) {
                    module.toggle();
                    AeroClient.CONFIG.save();
                    Notifications.toggled(module);
                }
                return true;
            }
            rowY += ROW_H;
        }
        return false;
    }

    /**
     * Generic reset: since Settings are plain getter/setter closures with no stored "original
     * default" to reach back to, this resets to a sensible neutral state (off / minimum / first
     * choice / empty) rather than literally replaying the shipped default value.
     */
    private void resetModule(Module module) {
        module.setEnabled(false);
        for (Module.Setting setting : module.settings) {
            switch (setting.kind) {
                case BOOL -> setting.boolSet.accept(false);
                case INT -> setting.intSet.set(setting.min);
                case FLOAT -> setting.floatSet.accept(setting.fMin);
                case CHOICE -> {
                    if (setting.choices != null && setting.choices.length > 0) {
                        setting.choiceSet.accept(setting.choices[0]);
                    }
                }
                case TEXT -> setting.choiceSet.accept("");
                case COLOR, ACTION -> {
                }
            }
        }
        Module.ModuleStyle style = module.style();
        style.toggleKey = -1;
        style.panelStyle = "Glass";
        style.shadow = true;
        style.accent = 0xFF4F8EFF;
    }
    private void applySliderDrag(Module.Setting setting, int barX, int barW, int mx) {
        float t = Math.max(0f, Math.min(1f, (mx - barX) / (float) barW));
        if (setting.kind == Module.Setting.Kind.FLOAT) {
            double value = setting.fMin + t * (setting.fMax - setting.fMin);
            setting.floatSet.accept(value);
        } else {
            int value = setting.min + Math.round(t * (setting.max - setting.min));
            setting.intSet.set(Math.max(setting.min, Math.min(setting.max, value)));
        }
    }
    private boolean clickSelectedSettings(int mx, int my) {
        if (selected == null) {
            return false;
        }
        int x = paneX;
        if (my < settingsStartY() - 6 || my >= settingsViewBottom()) {
            return false;
        }
        int y = settingsStartY() - settingsScroll;
        Module.ModuleStyle style = selected.style();
        String clickKeyLabel = "Emotes".equals(selected.name) ? "Hold Key" : "Toggle Key";
        if (inside(mx, my, x + RIGHT_W - 14 - pillW(clickKeyLabel), y - 3, pillW(clickKeyLabel), 20)) {
            capturingKeybind = selected;
            focusedTextSetting = null;
            accentFocus = false;
            return true;
        }
        y += 28;
        if ("Emotes".equals(selected.name)) {
            if (inside(mx, my, x + RIGHT_W - 14 - pillW("Edit pose"), y - 3, pillW("Edit pose"), 20)) {
                topTab = 1;
                youTab = 0;
                wardrobe.debugTab(7);
                return true;
            }
            y += 28;
        }
        List<String> tabs = tabsFor(selected);
        if (!tabs.isEmpty()) {
            int tabX = x + 14;
            int tabW = (RIGHT_W - 28) / tabs.size();
            for (String tabName : tabs) {
                if (inside(mx, my, tabX, y - 3, tabW - 4, 20)) {
                    settingsTab.put(selected.name, tabName);
                    return true;
                }
                tabX += tabW;
            }
            y += 28;
        }
        for (Module.Setting setting : selected.settings) {
            if (!settingVisible(setting)) {
                continue;
            }
            int indent = setting.nestUnder == null ? 0 : 12;
            int tx = x + 14 + indent;
            if (setting.numeric) {
                int barX = tx;
                int barW = RIGHT_W - 28 - indent;
                if (inside(mx, my, barX, y + 10, barW, 12)) {
                    draggingSetting = setting;
                    dragBarX = barX;
                    dragBarW = barW;
                    applySliderDrag(setting, barX, barW, mx);
                    return true;
                }
                y += 32;
            } else if (setting.kind == Module.Setting.Kind.CHOICE) {
                if (inside(mx, my, tx, y - 2, RIGHT_W - 28 - indent, 20)) {
                    setting.cycle();
                    // Cycling the Crosshair style straight to "Drawn" opens the pixel editor right
                    // away, since otherwise it's easy to miss the separate "Draw own" action row.
                    if ("Crosshair".equals(selected.name) && "Style".equals(setting.name)
                            && "Drawn".equalsIgnoreCase(setting.choiceGet.get()) && client != null) {
                        client.setScreen(new DrawCrosshairScreen(this));
                    }
                    return true;
                }
                y += 26;
            } else if (setting.kind == Module.Setting.Kind.TEXT) {
                if (inside(mx, my, tx, y + 12, RIGHT_W - 28 - indent, 18)) {
                    focusedTextSetting = setting;
                    accentFocus = false;
                    colorFocus = null;
                    String cur = setting.choiceGet.get();
                    textDraft = cur == null ? "" : cur;
                    return true;
                }
                y += 34;
            } else if (setting.kind == Module.Setting.Kind.KEY) {
                if (inside(mx, my, tx, y - 3, RIGHT_W - 28 - indent, 20)) {
                    keyCapture = setting;
                    focusedTextSetting = null;
                    return true;
                }
                y += 26;
            } else if (setting.kind == Module.Setting.Kind.COLOR) {
                if (inside(mx, my, x + 14, y - 2, RIGHT_W - 28, 20)) {
                    colorOpen = colorOpen == setting ? null : setting;
                    colorFocus = null;
                    focusedTextSetting = null;
                    accentFocus = false;
                    return true;
                }
                y += 22;
                if (colorOpen == setting) {
                    int barX = tx;
                    int barW = RIGHT_W - 28 - indent;
                    if (inside(mx, my, barX, y - 1, barW, 12)) {
                        hueDrag = setting;
                        hueBarX = barX;
                        hueBarW = barW;
                        applyHue(setting, barX, barW, mx);
                        return true;
                    }
                    int sw2 = Math.min(16, (barW - 4) / PRESETS.length - 3);
                    for (int i = 0; i < PRESETS.length; i++) {
                        if (inside(mx, my, barX + i * (sw2 + 3), y + 15, sw2, sw2)) {
                            setting.intSet.set(0xFF000000 | PRESETS[i]);
                            AeroClient.CONFIG.save();
                            return true;
                        }
                    }
                    y += 20 + sw2 + 6;
                }
            } else if (setting.kind == Module.Setting.Kind.ACTION) {
                String lab = setting.actionLabel == null ? "Open" : setting.actionLabel;
                int aw = Math.max(52, textRenderer.getWidth(lab) + 16);
                int ax = x + RIGHT_W - 14 - aw;
                if (setting.group && inside(mx, my, tx, y - 2, 16, 20)) {
                    toggleGroup(setting);
                    return true;
                }
                if (inside(mx, my, ax, y - 3, aw, 20)) {
                    if (setting.action != null) {
                        setting.action.run();
                    }
                    return true;
                }
                y += 26;
            } else if (setting.group && inside(mx, my, tx, y - 2, 16, 20)) {
                toggleGroup(setting);
                return true;
            } else if (inside(mx, my, x + RIGHT_W - 52, y - 2, 36, 20)) {
                setting.boolSet.accept(!setting.boolGet.getAsBoolean());
                return true;
            } else {
                y += 22;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return handleClick(click.x(), click.y(), click.button());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return handleClick(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        layoutPanel();
        if (topTab == 1 && youTab == 0 && wardrobe.drag(deltaX / s)) {
            return true;
        }
        if (hueDrag != null) {
            applyHue(hueDrag, hueBarX, hueBarW, lm(click.x()));
            return true;
        }
        if (draggingSetting != null) {
            applySliderDrag(draggingSetting, dragBarX, dragBarW, lm(click.x()));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        wardrobe.release();
        if (hueDrag != null) {
            AeroClient.CONFIG.save();
            hueDrag = null;
            return true;
        }
        if (draggingSetting != null) {
            AeroClient.CONFIG.save();
            draggingSetting = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double rawX, double rawY, double horizontalAmount, double verticalAmount) {
        layoutPanel();
        int mouseX = lm(rawX);
        int mouseY = lm(rawY);
        if (topTab == 1) {
            if (youTab == 0) {
                wardrobe.scroll(mouseX, mouseY, verticalAmount);
            } else {
                shop.scroll(verticalAmount);
            }
            return true;
        }
        if (topTab == 2) {
            friendScroll = (int) Math.max(0, friendScroll - verticalAmount * 16);
            return true;
        }
        if (selected != null && mouseX >= paneX - 4) {
            int view = settingsViewBottom() - (settingsStartY() - 6);
            settingsScroll = (int) Math.max(0, Math.min(Math.max(0, settingsContentH - view), settingsScroll - verticalAmount * 16));
            return true;
        }
        scroll = (int) Math.max(0, Math.min(Math.max(0, contentH() - listH0), scroll - verticalAmount * 16));
        return true;
    }

    public boolean handleKey(int key) {
        if (keyCapture != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                keyCapture.intSet.set(key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key);
                AeroClient.CONFIG.save();
            }
            keyCapture = null;
            return true;
        }
        if (capturingKeybind != null) {
            capturingKeybind.style().toggleKey = key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key;
            AeroClient.CONFIG.save();
            capturingKeybind = null;
            return true;
        }
        if (focusedTextSetting != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !textDraft.isEmpty()) {
                textDraft = textDraft.substring(0, textDraft.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                focusedTextSetting.choiceSet.accept(textDraft);
                AeroClient.CONFIG.save();
                focusedTextSetting = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                focusedTextSetting = null;
                return true;
            }
            return true;
        }
        if (accentFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !accentDraft.isEmpty()) {
                accentDraft = accentDraft.substring(0, accentDraft.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                applyAccentDraft();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                accentFocus = false;
                return true;
            }
            return true;
        }
        if (topTab == 2 && (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) && !friendFocus && friendSel >= 0) {
            FriendStore.remove(friendSel);
            friendSel = Math.min(friendSel, FriendStore.size() - 1);
            return true;
        }
        if (wardrobe.searchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !wardrobe.search.isEmpty()) {
                wardrobe.search = wardrobe.search.substring(0, wardrobe.search.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                wardrobe.searchFocus = false;
                return true;
            }
        }
        if (wardrobe.publishFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !wardrobe.publishName.isEmpty()) {
                wardrobe.publishName = wardrobe.publishName.substring(0, wardrobe.publishName.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                wardrobe.submitPublish();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                wardrobe.publishFocus = false;
                return true;
            }
        }
        if (friendFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !friendDraft.isEmpty()) {
                friendDraft = friendDraft.substring(0, friendDraft.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (FriendStore.add(friendDraft)) {
                    friendDraft = "";
                    friendSel = FriendStore.size() - 1;
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                friendFocus = false;
                return true;
            }
        }
        if (searchFocus || (topTab == 0 && searching())) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                searchFocus = true;
                rebuild();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocus = false;
                search = "";
                rebuild();
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_F && (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS)) {
            topTab = 0;
            searchFocus = true;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (topTab == 0 && openCard >= 0) {
                closeCard();
            } else if (topTab != 0) {
                topTab = 0;
            } else {
                closeMenu();
            }
            return true;
        }
        return false;
    }

    public boolean handleChar(int cp) {
        boolean printable = cp >= 32 && cp != 127;
        if (focusedTextSetting != null && printable) {
            textDraft += Character.toString(cp);
            return true;
        }
        if (colorFocus != null) {
            char ch = Character.toUpperCase((char) cp);
            if (ch == '#' || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')) {
                if (colorDraft.length() < 7) {
                    colorDraft += ch;
                }
            }
            return true;
        }
        if (accentFocus) {
            char ch = Character.toUpperCase((char) cp);
            if (ch == '#' || (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')) {
                if (accentDraft.length() < 7) {
                    accentDraft += ch;
                }
            }
            return true;
        }
        if (playerLookupFocus && printable) {
            playerLookup += Character.toString(cp);
            return true;
        }
        if (wardrobe.publishFocus && printable) {
            wardrobe.publishName += Character.toString(cp);
            return true;
        }
        if (friendFocus && printable) {
            friendDraft += Character.toString(cp);
            return true;
        }
        if (topTab == 1 && youTab == 0 && printable) {
            // Typing in the wardrobe goes straight into its search.
            wardrobe.searchFocus = true;
            wardrobe.search += Character.toString(cp);
            return true;
        }
        if (topTab == 0 && printable && keyCapture == null && capturingKeybind == null) {
            // Start typing anywhere in the client tab to search modules and settings.
            if (!searchFocus && search.isEmpty() && cp == ' ') {
                return false;
            }
            searchFocus = true;
            search += Character.toString(cp);
            scroll = 0;
            rebuild();
            return true;
        }
        return false;
    }

    private void applyAccentDraft() {
        if (selected == null) {
            accentFocus = false;
            return;
        }
        String hex = accentDraft.startsWith("#") ? accentDraft.substring(1) : accentDraft;
        try {
            selected.style().accent = 0xFF000000 | (Integer.parseInt(hex, 16) & 0xFFFFFF);
            AeroClient.CONFIG.save();
        } catch (NumberFormatException ignored) {
        }
        accentFocus = false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return handleKey(input.key());
    }

    @Override
    public boolean charTyped(CharInput input) {
        return handleChar(input.codepoint());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return handleKey(keyCode);
    }

    public boolean charTyped(char chr, int modifiers) {
        return handleChar(chr);
    }

    private void closeMenu() {
        if (parent != null) {
            client.setScreen(parent);
        } else {
            client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return pauseGame;
    }
}
