package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.auth.AccountManager;
import dev.aero.client.auth.SavedAccount;
import dev.aero.client.auth.SkinPreview;
import dev.aero.client.cosmetic.CosmeticPreview;
import dev.aero.client.cosmetic.Cosmetics;
import dev.aero.client.module.Category;
import dev.aero.client.module.Module;
import dev.aero.client.social.FriendStore;
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
 * Aurora-style in-game menu: Client modules, You cosmetics, Friends.
 */
public class ClickGuiScreenLegacy extends Screen {
    private static final int BG = 0x4406050C;
    private static final int BAR = 0xCC12101C;
    private static final int SIDE = 0x140A0912;
    private static final int CARD = 0xB012111A;
    private static final int PILL = 0x88262232;
    private static final int PILL_ON = 0xAA3A3158;
    private static final int MUTED = 0xFF8E889C;
    private static final int TEXT = 0xFFF3F0F8;
    private static final int LINE = 0x14FFFFFF;

    private static final int TOP = 36;
    private static final int ROW_H = 32;
    private boolean compactH;
    private int navH = 20;
    private int navStep = 22;
    private int SIDE_W = 168;
    private int RIGHT_W = 248;

    private static final String[] COS_TABS = {
            "Capes", "Wings", "Headwear", "Trails", "Kill", "Mace", "Pets"
    };
    private static final String[] COS_EXTRA = {
            "Emotes", "Chat tags", "Badges"
    };

    private final WardrobePanel wardrobe = new WardrobePanel();
    private final Screen parent;
    private final boolean pauseGame;
    private Category filter;
    private String search = "";
    private boolean searchFocus;
    private int scroll;
    private Module selected;
    private int rightTab;
    private int topTab;
    private int cosTab;
    private int cosSel;
    private int friendSel = -1;
    private String friendDraft = "";
    private boolean friendFocus;
    private String playerLookup = "";
    private boolean playerLookupFocus;
    private int friendScroll;
    private final List<Module> visible = new ArrayList<>();

    private boolean boundKeysOnly;
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
        filter = null;
        boundKeysOnly = false;
        rebuild();
        for (Module m : visible) {
            if (m.name.equals(name)) {
                selected = m;
            }
        }
    }

    public void debugTab(int top, int cos) {
        topTab = top;
        wardrobe.debugTab(cos);
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

    private static Category lastFilter;
    private static Module lastSelected;
    private static int lastTopTab;
    private static int lastScroll;
    private static boolean lastBoundKeys;

    private void layoutPanel() {
        int maxW = Math.max(8, width - 24);
        int maxH = Math.max(8, height - 24);
        pw = Math.min(maxW, 540);
        ph = Math.min(maxH, 310);
        if (pw < 460) {
            SIDE_W = Math.max(76, pw * 22 / 100);
            RIGHT_W = Math.max(96, pw * 30 / 100);
        } else {
            SIDE_W = 122;
            RIGHT_W = 178;
        }
        ox = Math.max(0, (width - pw) / 2);
        oy = Math.max(0, (height - ph) / 2);
        compactH = ph < 250;
        int cats = (int) java.util.Arrays.stream(Category.values()).filter(Category::inSidebar).count();
        int rows = (compactH ? 1 : 2) + (int) cats;
        int avail = ph - 114;
        navStep = Math.max(13, Math.min(22, avail / rows));
        navH = navStep - 2;
        if (pw < 470) {
            SIDE_W = compactH ? 96 : Math.max(70, pw * 20 / 100);
            RIGHT_W = Math.max(128, pw * (compactH ? 38 : 42) / 100);
        }
    }

    private boolean inMenu(Module module) {
        return module != null && module.category.inSidebar();
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
        filter = lastFilter;
        topTab = lastTopTab;
        scroll = lastScroll;
        boundKeysOnly = lastBoundKeys;
        rebuild();
        selected = lastSelected != null && visible.contains(lastSelected) ? lastSelected
                : !visible.isEmpty() ? visible.get(0) : null;
    }

    @Override
    public void removed() {
        super.removed();
        lastFilter = filter;
        lastSelected = selected;
        lastTopTab = topTab;
        lastScroll = scroll;
        lastBoundKeys = boundKeysOnly;
    }

    private void rebuild() {
        visible.clear();
        if (AeroClient.MODULES == null) {
            return;
        }
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (Module module : AeroClient.MODULES.all) {
            if (!inMenu(module)) {
                continue;
            }
            if (filter != null && module.category != filter) {
                continue;
            }
            if (!q.isEmpty() && !module.name.toLowerCase(Locale.ROOT).contains(q)
                    && !module.description.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (boundKeysOnly && module.style().toggleKey < 0) {
                continue;
            }
            visible.add(module);
        }
        int max = Math.max(0, visible.size() * ROW_H + sectionGaps() - listH());
        scroll = Math.min(scroll, max);
    }

    private int sectionGaps() {
        Category last = null;
        int extra = 0;
        for (Module module : visible) {
            if (module.category != last) {
                extra += 22;
                last = module.category;
            }
        }
        return extra;
    }

    private int listX() {
        return ox + SIDE_W + 8;
    }

    private int listW() {
        return Math.max(80, pw - SIDE_W - RIGHT_W - 16);
    }

    private int listH() {
        return ph - TOP - 22;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiDraw.menuOpen = true;
        try {
            layoutPanel();
            context.fillGradient(0, 0, width, height, 0x3A0A1224, 0x54040810);
            if (AeroClient.MODULES == null) {
                return;
            }
            UiDraw.glass(context, ox, oy, pw, ph, 0x9A10162A, 20);
            drawTop(context, mouseX, mouseY);
            if (topTab == 1) {
                wardrobe.render(context, mouseX, mouseY, ox, oy + TOP, pw, ph - TOP);
            } else if (topTab == 2) {
                drawFriends(context, mouseX, mouseY);
            } else {
                int midX = listX() - 6;
                int midW = listW() + 10;
                int rightX = ox + pw - RIGHT_W + 2;
                UiDraw.innerCard(context, midX, oy + TOP + 6, midW, ph - TOP - 14);
                UiDraw.innerCard(context, rightX, oy + TOP + 6, RIGHT_W - 10, ph - TOP - 14);
                drawSidebar(context, mouseX, mouseY);
                drawList(context, mouseX, mouseY);
                drawRight(context, mouseX, mouseY);
            }
        } catch (Throwable t) {
            context.drawText(textRenderer, Text.literal("Menu draw failed"), ox + 16, oy + 48, TEXT, false);
        } finally {
            UiDraw.menuOpen = false;
        }
    }

    private void drawTop(DrawContext context, int mx, int my) {
        int x0 = ox;
        int y0 = oy;
        int x1 = ox + pw;
        UiDraw.roundRect(context, x0 + 6, y0 + 5, pw - 12, TOP - 8, 12, BAR);
        context.fill(x0 + 14, y0 + TOP - 1, x1 - 14, y0 + TOP, 0x22FFFFFF);

        UiDraw.aeroMark(context, x0 + 12, y0 + 8, 18, UiDraw.accent());
        if (!compactTop()) {
            context.drawText(textRenderer, Text.literal("Aero"), x0 + 34, y0 + 10, TEXT, false);
            context.drawText(textRenderer, Text.literal(AeroClient.VERSION), x0 + 34, y0 + 20, MUTED, false);
        }

        int tabX = topTabX();
        tabPill(context, tabX, y0 + 8, tabW(0), topTab == 0, inside(mx, my, tabX, y0 + 8, tabW(0), 20), "Client");
        tabPill(context, tabX + tabW(0) + 4, y0 + 8, tabW(1), topTab == 1, inside(mx, my, tabX + tabW(0) + 4, y0 + 8, tabW(1), 20), "Wardrobe");
        tabPill(context, tabX + tabW(0) + tabW(1) + 8, y0 + 8, tabW(2), topTab == 2, inside(mx, my, tabX + tabW(0) + tabW(1) + 8, y0 + 8, tabW(2), 20), "Friends");

        int closeX = x1 - 28;
        boolean closeH = inside(mx, my, closeX, y0 + 9, 16, 16);
        UiDraw.roundRect(context, closeX, y0 + 9, 16, 16, 8, closeH ? 0x66E05555 : 0x22FFFFFF);
        context.drawText(textRenderer, Text.literal("x"), closeX + 5, y0 + 12, closeH ? TEXT : MUTED, false);

        int profilesX = profilesButtonX();
        boolean profilesH = inside(mx, my, profilesX, y0 + 8, PROFILES_W, 20);
        UiDraw.pill(context, profilesX, y0 + 8, PROFILES_W, 20, profilesH);
        String profilesLabel = "Profiles";
        context.drawText(textRenderer, Text.literal(profilesLabel),
                profilesX + (PROFILES_W - textRenderer.getWidth(profilesLabel)) / 2, y0 + 14,
                profilesH ? TEXT : MUTED, false);

        int tabsEnd = tabX + tabW(0) + tabW(1) + tabW(2) + 8;
        int room = profilesX - 10 - (tabsEnd + 8);
        if (room >= 60) {
            String name = fit(AccountManager.currentName(), Math.min(90, room - 26));
            int nw = textRenderer.getWidth(name);
            int chipW = nw + 26;
            int nameX = profilesX - 10 - chipW;
            UiDraw.pill(context, nameX, y0 + 8, chipW, 20, false);
            UiDraw.roundRect(context, nameX + 4, y0 + 12, 12, 12, 6, 0xFFC8A0E8);
            context.drawText(textRenderer, Text.literal(name), nameX + 20, y0 + 14, TEXT, false);
        }
    }

    private boolean compactTop() {
        return pw < 470;
    }

    private int topTabX() {
        return ox + (compactTop() ? 38 : 86);
    }

    private int tabW(int i) {
        if (compactTop()) {
            return i == 1 ? 56 : 46;
        }
        return i == 1 ? 76 : 64;
    }

    private static final int PROFILES_W = 58;

    private int profilesButtonX() {
        return ox + pw - 28 - 8 - PROFILES_W;
    }

    private void tabPill(DrawContext context, int x, int y, int w, boolean on, boolean hover, String label) {
        UiDraw.pill(context, x, y, w, 20, on || hover);
        int tw = textRenderer.getWidth(label);
        context.drawText(textRenderer, Text.literal(label), x + (w - tw) / 2, y + 6, on || hover ? TEXT : MUTED, false);
    }

    private void drawSidebar(DrawContext context, int mx, int my) {
        int x0 = ox;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        context.fill(x0 + SIDE_W - 1, y0 + 8, x0 + SIDE_W, y1 - 8, 0x14FFFFFF);

        int sy = y0 + 10;
        UiDraw.field(context, x0 + 10, sy, SIDE_W - 20, 22, searchFocus);
        String q = search.isEmpty() && !searchFocus ? "Search modules" : search + (searchFocus ? "|" : "");
        context.drawText(textRenderer, Text.literal(fit(q, SIDE_W - 36)), x0 + 16, sy + 7,
                search.isEmpty() && !searchFocus ? MUTED : TEXT, false);

        sy += 30;
        nav(context, mx, my, sy, null, "All", AeroClient.MODULES.menuCount(), !boundKeysOnly && filter == null);
        sy += navStep;
        for (Category category : Category.values()) {
            if (!category.inSidebar()) {
                continue;
            }
            nav(context, mx, my, sy, category, category.title, AeroClient.MODULES.count(category),
                    !boundKeysOnly && filter == category);
            sy += navStep;
        }
        if (compactH) {
            int by = y1 - 30;
            int half = (SIDE_W - 24) / 2;
            boolean kH = inside(mx, my, x0 + 8, by, half, 22);
            UiDraw.pill(context, x0 + 8, by, half, 22, boundKeysOnly || kH);
            context.drawText(textRenderer, Text.literal("Keys"), x0 + 8 + (half - textRenderer.getWidth("Keys")) / 2, by + 7,
                    boundKeysOnly || kH ? TEXT : MUTED, false);
            boolean hH = inside(mx, my, x0 + 12 + half, by, half, 22);
            UiDraw.pill(context, x0 + 12 + half, by, half, 22, hH);
            context.drawText(textRenderer, Text.literal("HUD"), x0 + 12 + half + (half - textRenderer.getWidth("HUD")) / 2, by + 7,
                    hH ? TEXT : MUTED, false);
            return;
        }
        sy += 4;
        UiDraw.divider(context, x0 + 14, sy - 3, SIDE_W - 28);
        boolean boundH = inside(mx, my, x0 + 8, sy, SIDE_W - 16, navH);
        if (boundKeysOnly) {
            UiDraw.roundRect(context, x0 + 8, sy, SIDE_W - 16, navH, 8, PILL_ON);
            UiDraw.roundRect(context, x0 + 8, sy + 4, 3, navH - 8, 1, UiDraw.accent());
        } else if (boundH) {
            UiDraw.roundRect(context, x0 + 8, sy, SIDE_W - 16, navH, 8, 0x8014121C);
        }
        context.drawText(textRenderer, Text.literal("Bound keys"), x0 + 18, sy + 6,
                boundKeysOnly ? TEXT : MUTED, false);

        int hy = y1 - 34;
        boolean hudH = inside(mx, my, x0 + 10, hy, SIDE_W - 20, 22);
        UiDraw.pill(context, x0 + 10, hy, SIDE_W - 20, 22, hudH);
        String hudLabel = "HUD layout";
        context.drawText(textRenderer, Text.literal(hudLabel),
                x0 + 10 + (SIDE_W - 20 - textRenderer.getWidth(hudLabel)) / 2, hy + 7, hudH ? TEXT : MUTED, false);
    }

    private void nav(DrawContext context, int mx, int my, int y, Category category, String name, int count, boolean on) {
        boolean h = inside(mx, my, ox + 8, y, SIDE_W - 16, navH);
        if (on) {
            UiDraw.roundRect(context, ox + 8, y, SIDE_W - 16, navH, 8, PILL_ON);
            UiDraw.roundRect(context, ox + 8, y + 4, 3, navH - 8, 1, UiDraw.accent());
        } else if (h) {
            UiDraw.roundRect(context, ox + 8, y, SIDE_W - 16, navH, 8, 0x8014121C);
        }
        context.drawText(textRenderer, Text.literal(name), ox + 18, y + (navH - 8) / 2 + 1, on ? TEXT : MUTED, false);
        String n = String.valueOf(count);
        if (18 + textRenderer.getWidth(name) + textRenderer.getWidth(n) + 10 < SIDE_W - 16) {
            context.drawText(textRenderer, Text.literal(n), ox + SIDE_W - 16 - textRenderer.getWidth(n), y + (navH - 8) / 2 + 1, MUTED, false);
        }
    }

    private void drawList(DrawContext context, int mx, int my) {
        int x = listX();
        int y0 = oy + TOP + 10;
        int w = listW();
        int view = listH();
        context.enableScissor(x, y0, x + w, y0 + view);
        try {
            if (visible.isEmpty()) {
                context.drawText(textRenderer, Text.literal("No modules match."), x + 12, y0 + 16, MUTED, false);
            }
            int y = y0 - scroll;
            Category last = null;
            int bottom = y0 + view;
            for (Module module : visible) {
                if (module.category != last) {
                    last = module.category;
                    if (y + 22 >= y0 && y <= bottom) {
                        context.drawText(textRenderer, Text.literal(last.title.toUpperCase(Locale.ROOT)),
                                x + 10, y + 8, MUTED, false);
                    }
                    y += 22;
                }
                if (y + ROW_H < y0) {
                    y += ROW_H;
                    continue;
                }
                if (y > bottom) {
                    break;
                }
                boolean h = inside(mx, my, x, y, w, ROW_H - 2);
                boolean sel = module == selected;
                int rowBottom = y + ROW_H - 4;
                if (sel) {
                    UiDraw.roundRect(context, x + 6, y, w - 12, rowBottom - y, 8, UiDraw.withAlpha(UiDraw.accent(), 0x44));
                    UiDraw.roundRect(context, x + 6, y + 6, 3, rowBottom - y - 12, 1, UiDraw.accent());
                } else if (h) {
                    UiDraw.roundRect(context, x + 6, y, w - 12, rowBottom - y, 8, 0x28FFFFFF);
                }
                drawModIcon(context, module, x + 12, y + 7);
                int nameMax = w - 86;
                context.drawText(textRenderer, Text.literal(fit(module.name, nameMax)), x + 30, y + 7,
                        module.enabled() ? TEXT : MUTED, false);
                int key = module.style().toggleKey;
                if (key >= 0) {
                    String kn = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
                    String hint = kn == null ? "Key" : kn.toUpperCase(Locale.ROOT);
                    context.drawText(textRenderer, Text.literal(hint), x + 30, y + 17, MUTED, false);
                }
                drawSwitch(context, x + w - 44, y + 8, module.enabled());
                y += ROW_H;
            }
        } finally {
            context.disableScissor();
        }
        int content = visible.size() * ROW_H + sectionGaps();
        UiDraw.scrollbar(context, x + w - 5, y0, view, scroll, content, view);
    }

    private void drawRight(DrawContext context, int mx, int my) {
        int x = ox + pw - RIGHT_W;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        context.fill(x, y0, ox + pw, y1, 0);
        context.fill(x, y0, x + 1, y1, 0);

        if (selected == null) {
            context.drawText(textRenderer, Text.literal("Settings"), x + 16, y0 + 16, TEXT, false);
            context.drawText(textRenderer, Text.literal("Pick a module on the left."), x + 16, y0 + 32, MUTED, false);
            return;
        }

        context.drawText(textRenderer, Text.literal(fit(selected.name, RIGHT_W - 110)), x + 14, y0 + 12, TEXT, false);
        drawSwitch(context, ox + pw - 42, y0 + 10, selected.enabled());
        boolean resetHover = inside(mx, my, ox + pw - 42 - 46, y0 + 9, 40, 16);
        UiDraw.pill(context, ox + pw - 42 - 46, y0 + 9, 40, 16, resetHover);
        context.drawText(textRenderer, Text.literal("Reset"), ox + pw - 42 - 46 + 6, y0 + 13,
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
        context.enableScissor(x + 1, viewTop, ox + pw, viewBottom);
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
                UiDraw.inset(context, tx, y + 12, RIGHT_W - 28 - indent, 18, focused ? 0xFF16141F : CARD);
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
                    UiDraw.roundBorder(context, barX - 1, y - 1, barW + 2, 12, 3, 0x33FFFFFF);
                    int sw2 = Math.min(16, (barW - 4) / PRESETS.length - 3);
                    for (int i = 0; i < PRESETS.length; i++) {
                        int px2 = barX + i * (sw2 + 3);
                        UiDraw.roundRect(context, px2, y + 15, sw2, sw2, 3, 0xFF000000 | PRESETS[i]);
                        if ((rgb & 0xFFFFFF) == PRESETS[i]) {
                            UiDraw.roundBorder(context, px2 - 1, y + 14, sw2 + 2, sw2 + 2, 3, 0xFFFFFFFF);
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
        UiDraw.scrollbar(context, ox + pw - 16, viewTop, viewBottom - viewTop, settingsScroll, settingsContentH, viewBottom - viewTop);
    }

    private int pillW(String label) {
        return Math.max(40, Math.min(78, RIGHT_W - 28 - textRenderer.getWidth(label) - 6));
    }

    private int settingsStartY() {
        int lines = selected == null ? 1 : descLines(selected.description, RIGHT_W - 28);
        return oy + TOP + 30 + lines * 10 + 14;
    }

    private int settingsViewBottom() {
        return oy + ph - 12;
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
                    UiDraw.roundRect(context, lx - 2, y, lw + 4, ROW_H - 4, 10, 0x28FFFFFF);
                }
                if (SkinPreview.ready(n.toLowerCase())) {
                    SkinPreview.drawHead(context, n.toLowerCase(), lx + 4, y + 6, 16);
                } else {
                    UiDraw.roundRect(context, lx + 6, y + 8, 12, 12, 6, on ? 0xFFC8A0E8 : 0xFF3A3A44);
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
                UiDraw.roundRect(context, rx + 28, y0 + 54, 36, 90, 8, 0x6614101C);
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

    private void rowToggle(DrawContext context, int panelX, int y, String name, boolean on) {
        UiDraw.inset(context, panelX + 10, y, RIGHT_W - 20, 22, 0xFF12101A);
        context.drawText(textRenderer, Text.literal(name), panelX + 16, y + 7, TEXT, false);
        drawSwitch(context, panelX + RIGHT_W - 48, y + 3, on);
    }

    /**
     * One deliberate glyph per module: a handful of the most-used modules get a bespoke icon by
     * name, everything else falls back to a fixed glyph for its category. Previously this picked
     * one of six meaningless blob shapes from module.name.hashCode(), which read as random noise
     * rather than actual icons.
     */
    private void drawModIcon(DrawContext context, Module module, int x, int y) {
        int col = switch (module.category) {
            case PVP -> 0xFF4F8EFF;
            case HUD -> 0xFF00B9E8;
            case RENDER -> 0xFFDDD6FE;
            case PLAYER -> 0xFFE9D5FF;
            case MISC -> 0xFFB8B0C8;
            case PERFORMANCE -> 0xFF9AE6B4;
            case PREVIEW -> 0xFFFFC94D;
        };
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
        context.fill(x + 3, y + 3, x + 7, y + 7, 0xFF16141F);
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
        context.fill(x + 3, y + 4, x + 5, y + 6, 0xFF16141F);
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

    private static void pill(DrawContext context, int x, int y, int w, int h, boolean on) {
        UiDraw.pill(context, x, y, w, h, on);
    }

    private static void fillRound(DrawContext context, int x, int y, int w, int h, int color) {
        UiDraw.raised(context, x, y, w, h, color);
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

    public boolean handleClick(double x, double y, int button) {
        layoutPanel();
        int mx = (int) x;
        int my = (int) y;
        if (button != 0) {
            return false;
        }
        if (inside(mx, my, ox + pw - 28, oy + 9, 16, 16)) {
            closeMenu();
            return true;
        }
        if (inside(mx, my, profilesButtonX(), oy + 8, PROFILES_W, 20)) {
            MinecraftClient.getInstance().setScreen(new ProfilesScreen(this));
            return true;
        }
        int tx = topTabX();
        if (inside(mx, my, tx, oy + 8, tabW(0), 20)) {
            topTab = 0;
            return true;
        }
        if (inside(mx, my, tx + tabW(0) + 4, oy + 8, tabW(1), 20)) {
            topTab = 1;
            return true;
        }
        if (inside(mx, my, tx + tabW(0) + tabW(1) + 8, oy + 8, tabW(2), 20)) {
            topTab = 2;
            return true;
        }
        if (topTab == 1) {
            searchFocus = false;
            friendFocus = false;
            playerLookupFocus = false;
            return wardrobe.click(mx, my, ox, oy + TOP, pw, ph - TOP);
        }
        if (topTab == 2) {
            searchFocus = false;
            wardrobe.searchFocus = false;
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
        searchFocus = inside(mx, my, ox + 10, oy + TOP + 10, SIDE_W - 20, 20);

        if (compactH) {
            int half = (SIDE_W - 24) / 2;
            int by = oy + ph - 30;
            if (inside(mx, my, ox + 12 + half, by, half, 22)) {
                MinecraftClient.getInstance().setScreen(new HudLayoutScreen(this));
                return true;
            }
            if (inside(mx, my, ox + 8, by, half, 22)) {
                boundKeysOnly = !boundKeysOnly;
                filter = null;
                rebuild();
                return true;
            }
        } else if (inside(mx, my, ox + 10, oy + ph - 34, SIDE_W - 20, 22)) {
            MinecraftClient.getInstance().setScreen(new HudLayoutScreen(this));
            return true;
        }

        int sy = oy + TOP + 40;
        if (inside(mx, my, ox + 8, sy, SIDE_W - 16, navH)) {
            filter = null;
            boundKeysOnly = false;
            rebuild();
            return true;
        }
        sy += navStep;
        for (Category category : Category.values()) {
            if (!category.inSidebar()) {
                continue;
            }
            if (inside(mx, my, ox + 8, sy, SIDE_W - 16, navH)) {
                filter = category;
                boundKeysOnly = false;
                rebuild();
                return true;
            }
            sy += navStep;
        }
        sy += 4;
        if (!compactH && inside(mx, my, ox + 8, sy, SIDE_W - 16, navH)) {
            boundKeysOnly = true;
            filter = null;
            rebuild();
            return true;
        }

        if (selected != null && inside(mx, my, ox + pw - 42, oy + TOP + 10, 28, 16)) {
            selected.toggle();
            AeroClient.CONFIG.save();
            return true;
        }
        if (selected != null && inside(mx, my, ox + pw - 42 - 46, oy + TOP + 9, 40, 16)) {
            resetModule(selected);
            AeroClient.CONFIG.save();
            return true;
        }
        if (selected != null && clickSelectedSettings(mx, my)) {
            AeroClient.CONFIG.save();
            return true;
        }

        int lx = listX();
        int rowY = oy + TOP + 10 - scroll;
        int w = listW();
        Category last = null;
        for (Module module : visible) {
            if (module.category != last) {
                last = module.category;
                rowY += 22;
            }
            if (inside(mx, my, lx, rowY, w, ROW_H - 2) && my >= oy + TOP + 10 && my <= oy + TOP + 10 + listH()) {
                if (selected != module) {
                    focusedTextSetting = null;
                    accentFocus = false;
                    capturingKeybind = null;
                }
                selected = module;
                if (mx >= lx + w - 48) {
                    module.toggle();
                    AeroClient.CONFIG.save();
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
        int x = ox + pw - RIGHT_W;
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
                cosTab = 7;
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
        if (topTab == 1 && wardrobe.drag(deltaX)) {
            return true;
        }
        if (hueDrag != null) {
            applyHue(hueDrag, hueBarX, hueBarW, (int) click.x());
            return true;
        }
        if (draggingSetting != null) {
            applySliderDrag(draggingSetting, dragBarX, dragBarW, (int) click.x());
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (topTab == 1) {
            layoutPanel();
            wardrobe.scroll((int) mouseX, (int) mouseY, verticalAmount);
            return true;
        }
        if (topTab == 0 && selected != null && mouseX >= ox + pw - RIGHT_W) {
            int view = settingsViewBottom() - (settingsStartY() - 6);
            settingsScroll = (int) Math.max(0, Math.min(Math.max(0, settingsContentH - view), settingsScroll - verticalAmount * 16));
            return true;
        }
        if (topTab == 2) {
            friendScroll = (int) Math.max(0, friendScroll - verticalAmount * 16);
            return true;
        }
        int max = Math.max(0, visible.size() * ROW_H + sectionGaps() - listH());
        scroll = (int) Math.max(0, Math.min(max, scroll - verticalAmount * 16));
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
        if (playerLookupFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !playerLookup.isEmpty()) {
                playerLookup = playerLookup.substring(0, playerLookup.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                SkinPreview.requestLookup(playerLookup);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                playerLookupFocus = false;
                return true;
            }
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
        if (searchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                rebuild();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocus = false;
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeMenu();
            return true;
        }
        return false;
    }

    public boolean handleChar(int cp) {
        if (focusedTextSetting != null && cp >= 32 && cp != 127) {
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
                return true;
            }
            return true;
        }
        if (playerLookupFocus && cp >= 32 && cp != 127) {
            playerLookup += Character.toString(cp);
            return true;
        }
        if (wardrobe.searchFocus && cp >= 32 && cp != 127) {
            wardrobe.search += Character.toString(cp);
            return true;
        }
        if (friendFocus && cp >= 32 && cp != 127) {
            friendDraft += Character.toString(cp);
            return true;
        }
        if (searchFocus && cp >= 32 && cp != 127) {
            search += Character.toString(cp);
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
