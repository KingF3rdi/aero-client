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
    private static final int ACCENT = 0xFF4F8EFF;
    private static final int LINE = 0x14FFFFFF;

    private static final int TOP = 36;
    private static final int ROW_H = 32;
    private int SIDE_W = 168;
    private int RIGHT_W = 248;

    private static final String[] COS_TABS = {
            "Capes", "Wings", "Headwear", "Trails", "Kill", "Mace", "Pets"
    };
    private static final String[] COS_EXTRA = {
            "Emotes", "Chat tags", "Badges"
    };

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
    private String cosSearch = "";
    private boolean cosSearchFocus;
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
        pw = Math.min(maxW, 760);
        ph = Math.min(maxH, 440);
        if (pw < 612) {
            SIDE_W = Math.max(96, pw * 22 / 100);
            RIGHT_W = Math.max(120, pw * 30 / 100);
        } else {
            SIDE_W = 168;
            RIGHT_W = 248;
        }
        ox = Math.max(0, (width - pw) / 2);
        oy = Math.max(0, (height - ph) / 2);
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
        return ph - TOP - 12;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiDraw.menuOpen = true;
        try {
            layoutPanel();
            context.fill(0, 0, width, height, BG);
            if (AeroClient.MODULES == null) {
                return;
            }
            UiDraw.glass(context, ox, oy, pw, ph, 0xC414121E, 20);
            drawTop(context, mouseX, mouseY);
            if (topTab == 1) {
                drawYou(context, mouseX, mouseY);
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

        UiDraw.roundRect(context, x0 + 12, y0 + 8, 18, 18, 6, ACCENT);
        context.drawText(textRenderer, Text.literal("L"), x0 + 17, y0 + 13, 0xFF1A1024, false);
        context.drawText(textRenderer, Text.literal("Aero"), x0 + 34, y0 + 10, TEXT, false);
        context.drawText(textRenderer, Text.literal(AeroClient.VERSION), x0 + 34, y0 + 20, MUTED, false);

        int tabX = x0 + 86;
        tabPill(context, tabX, y0 + 8, 64, topTab == 0, inside(mx, my, tabX, y0 + 8, 64, 20), "Client");
        tabPill(context, tabX + 68, y0 + 8, 76, topTab == 1, inside(mx, my, tabX + 68, y0 + 8, 76, 20), "Wardrobe");
        tabPill(context, tabX + 148, y0 + 8, 64, topTab == 2, inside(mx, my, tabX + 148, y0 + 8, 64, 20), "Friends");

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

        String name = fit(AccountManager.currentName(), 90);
        int nw = textRenderer.getWidth(name);
        int chipW = nw + 26;
        int nameX = profilesX - 10 - chipW;
        UiDraw.pill(context, nameX, y0 + 8, chipW, 20, false);
        UiDraw.roundRect(context, nameX + 4, y0 + 12, 12, 12, 6, 0xFFC8A0E8);
        context.drawText(textRenderer, Text.literal(name), nameX + 20, y0 + 14, TEXT, false);
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

        sy += 34;
        nav(context, mx, my, sy, null, "All", AeroClient.MODULES.menuCount(), !boundKeysOnly && filter == null);
        sy += 28;
        for (Category category : Category.values()) {
            if (!category.inSidebar()) {
                continue;
            }
            nav(context, mx, my, sy, category, category.title, AeroClient.MODULES.count(category),
                    !boundKeysOnly && filter == category);
            sy += 28;
        }
        sy += 10;
        boolean boundH = inside(mx, my, x0 + 8, sy, SIDE_W - 16, 24);
        if (boundKeysOnly) {
            UiDraw.roundRect(context, x0 + 8, sy, SIDE_W - 16, 24, 8, PILL_ON);
            UiDraw.roundRect(context, x0 + 8, sy + 5, 3, 14, 1, ACCENT);
        } else if (boundH) {
            UiDraw.roundRect(context, x0 + 8, sy, SIDE_W - 16, 24, 8, 0x8014121C);
        }
        context.drawText(textRenderer, Text.literal("Bound keys"), x0 + 16, sy + 8,
                boundKeysOnly ? TEXT : MUTED, false);

        int hy = y1 - 34;
        boolean hudH = inside(mx, my, x0 + 10, hy, SIDE_W - 20, 22);
        UiDraw.pill(context, x0 + 10, hy, SIDE_W - 20, 22, hudH);
        String hudLabel = "HUD layout";
        context.drawText(textRenderer, Text.literal(hudLabel),
                x0 + 10 + (SIDE_W - 20 - textRenderer.getWidth(hudLabel)) / 2, hy + 7, hudH ? TEXT : MUTED, false);
    }

    private void nav(DrawContext context, int mx, int my, int y, Category category, String name, int count, boolean on) {
        boolean h = inside(mx, my, ox + 8, y, SIDE_W - 16, 24);
        if (on) {
            UiDraw.roundRect(context, ox + 8, y, SIDE_W - 16, 24, 8, PILL_ON);
            UiDraw.roundRect(context, ox + 8, y + 5, 3, 14, 1, ACCENT);
        } else if (h) {
            UiDraw.roundRect(context, ox + 8, y, SIDE_W - 16, 24, 8, 0x8014121C);
        }
        context.drawText(textRenderer, Text.literal(name), ox + 18, y + 8, on ? TEXT : MUTED, false);
        String n = String.valueOf(count);
        context.drawText(textRenderer, Text.literal(n), ox + SIDE_W - 16 - textRenderer.getWidth(n), y + 8, MUTED, false);
    }

    private void drawList(DrawContext context, int mx, int my) {
        int x = listX();
        int y0 = oy + TOP + 8;
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
                    UiDraw.roundRect(context, x + 6, y, w - 12, rowBottom - y, 8, 0x444F8EFF);
                    UiDraw.roundRect(context, x + 6, y + 6, 3, rowBottom - y - 12, 1, ACCENT);
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
        wrap(context, selected.description, x + 14, y0 + 30, RIGHT_W - 28, MUTED);
        UiDraw.divider(context, x + 14, settingsStartY() - 8, RIGHT_W - 28);

        int y = settingsStartY();
        Module.ModuleStyle style = selected.style();
        String keyLabel = "Emotes".equals(selected.name) ? "Hold Key" : "Toggle Key";
        context.drawText(textRenderer, Text.literal(keyLabel), x + 14, y + 4, MUTED, false);
        String keyName = capturingKeybind == selected ? "Press a key..."
                : style.toggleKey < 0 ? "None" : org.lwjgl.glfw.GLFW.glfwGetKeyName(style.toggleKey, 0) != null
                ? org.lwjgl.glfw.GLFW.glfwGetKeyName(style.toggleKey, 0).toUpperCase(java.util.Locale.ROOT) : "Key " + style.toggleKey;
        UiDraw.pill(context, x + RIGHT_W - 92, y - 3, 78, 20, capturingKeybind == selected);
        int kw = textRenderer.getWidth(keyName);
        context.drawText(textRenderer, Text.literal(keyName), x + RIGHT_W - 92 + (78 - kw) / 2, y + 3, TEXT, false);
        y += 28;

        if ("Emotes".equals(selected.name)) {
            context.drawText(textRenderer, Text.literal("Edit pose"), x + 14, y + 4, TEXT, false);
            boolean poseH = inside(mx, my, x + RIGHT_W - 92, y - 3, 78, 20);
            UiDraw.pill(context, x + RIGHT_W - 92, y - 3, 78, 20, poseH);
            int ow = textRenderer.getWidth("Open");
            context.drawText(textRenderer, Text.literal("Open"), x + RIGHT_W - 92 + (78 - ow) / 2, y + 3, TEXT, false);
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
            if (y > y1 - 28) {
                break;
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
            } else if (setting.kind == Module.Setting.Kind.COLOR) {
                context.drawText(textRenderer, Text.literal(setting.name), tx, y + 4, TEXT, false);
                int rgb = setting.intGet.get() & 0xFFFFFF;
                String hex = colorFocus == setting ? colorDraft : String.format("#%06X", rgb);
                context.drawText(textRenderer, Text.literal(hex),
                        x + RIGHT_W - 44 - textRenderer.getWidth(hex), y + 4, colorFocus == setting ? TEXT : MUTED, false);
                UiDraw.raised(context, x + RIGHT_W - 36, y, 18, 12, 0xFF000000 | rgb);
                y += 22;
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
    }

    private int settingsStartY() {
        return oy + TOP + 58;
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

    private void drawYou(DrawContext context, int mx, int my) {
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = youLeft();
        int mid = youMid();
        int right = youRight();
        int leftW = mid - left - 8;
        int midW = right - mid - 8;
        Cosmetics.Kind kind = Cosmetics.kindForTab(cosTab);
        java.util.List<Cosmetics.Item> items = Cosmetics.of(kind, cosSearch);
        String equipped = Cosmetics.equipped(kind);

        UiDraw.innerCard(context, left, y0 + 6, leftW, ph - TOP - 14);
        UiDraw.innerCard(context, mid, y0 + 6, midW, ph - TOP - 14);
        UiDraw.innerCard(context, right + 2, y0 + 6, RIGHT_W - 10, ph - TOP - 14);

        int lx = left + 10;
        int lw = leftW - 20;
        UiDraw.field(context, lx, y0 + 16, lw, 22, playerLookupFocus);
        String look = playerLookup.isEmpty() && !playerLookupFocus ? "Look up a player…" : playerLookup + (playerLookupFocus ? "|" : "");
        context.drawText(textRenderer, Text.literal(fit(look, lw - 16)), lx + 8, y0 + 22,
                playerLookup.isEmpty() && !playerLookupFocus ? MUTED : TEXT, false);

        String name = AccountManager.currentName();
        SkinPreview.requestOwn();
        if (!playerLookup.isBlank()) {
            SkinPreview.requestLookup(playerLookup);
        }
        String key = wardrobeSkinKey();
        if (SkinPreview.ready(key)) {
            SkinPreview.drawHead(context, key, lx, y0 + 48, 28);
        } else {
            UiDraw.roundRect(context, lx, y0 + 48, 28, 28, 8, 0xFFC8A0E8);
        }
        context.drawText(textRenderer, Text.literal(fit(name, lw - 40)), lx + 36, y0 + 52, TEXT, false);
        String status = AccountManager.status.get();
        if (status == null || status.isBlank()) {
            status = MinecraftClient.getInstance().player != null ? "In world" : "Menu";
        }
        context.drawText(textRenderer, Text.literal(fit(status, lw - 40)), lx + 36, y0 + 64, MUTED, false);

        int rowY = y0 + 92;
        context.drawText(textRenderer, Text.literal("Friends"), lx, rowY, MUTED, false);
        context.drawText(textRenderer, Text.literal(String.valueOf(FriendStore.size())), lx + lw - textRenderer.getWidth(String.valueOf(FriendStore.size())), rowY, TEXT, false);
        UiDraw.divider(context, lx, rowY + 16, lw);
        context.drawText(textRenderer, Text.literal("Account"), lx, rowY + 28, MUTED, false);
        context.drawText(textRenderer, Text.literal(AccountManager.account != null ? "Microsoft" : "Offline"),
                lx + lw - textRenderer.getWidth(AccountManager.account != null ? "Microsoft" : "Offline"), rowY + 28, TEXT, false);

        boolean signed = AccountManager.account != null;
        String sign = signed ? "Sign out" : "Sign in with Microsoft";
        boolean signH = inside(mx, my, lx, y1 - 38, lw, 22);
        UiDraw.pill(context, lx, y1 - 38, lw, 22, signH);
        context.drawText(textRenderer, Text.literal(sign),
                lx + Math.max(8, (lw - textRenderer.getWidth(sign)) / 2), y1 - 32, TEXT, false);

        int tabY = y0 + 16;
        int tx = mid + 10;
        String[] tabs = new String[COS_TABS.length + COS_EXTRA.length];
        System.arraycopy(COS_TABS, 0, tabs, 0, COS_TABS.length);
        System.arraycopy(COS_EXTRA, 0, tabs, COS_TABS.length, COS_EXTRA.length);
        for (int i = 0; i < tabs.length; i++) {
            boolean on = cosTab == i;
            int tw = Math.max(48, textRenderer.getWidth(tabs[i]) + 16);
            if (tx + tw > mid + midW - 8) {
                tx = mid + 10;
                tabY += 22;
            }
            boolean h = inside(mx, my, tx, tabY, tw, 20);
            UiDraw.pill(context, tx, tabY, tw - 4, 20, on || h);
            context.drawText(textRenderer, Text.literal(tabs[i]),
                    tx + (tw - 4 - textRenderer.getWidth(tabs[i])) / 2, tabY + 6, on || h ? TEXT : MUTED, false);
            tx += tw;
        }

        int searchY = tabY + 26;
        UiDraw.field(context, mid + 10, searchY, midW - 20, 22, cosSearchFocus);
        String hint = cosSearch.isEmpty() && !cosSearchFocus ? "Search cosmetics…" : cosSearch + (cosSearchFocus ? "|" : "");
        context.drawText(textRenderer, Text.literal(fit(hint, midW - 36)), mid + 18, searchY + 6,
                cosSearch.isEmpty() && !cosSearchFocus ? MUTED : TEXT, false);

        int gx = mid + 10;
        int gy = searchY + 32;
        int cell = 56;
        int gap = 8;
        int cols = Math.max(1, (midW - 20) / (cell + gap));
        context.enableScissor(mid + 8, gy, mid + midW - 8, y1 - 16);
        try {
            for (int i = 0; i < items.size(); i++) {
                Cosmetics.Item item = items.get(i);
                int col = i % cols;
                int row = i / cols;
                int cx = gx + col * (cell + gap);
                int cy = gy + row * (cell + 20);
                if (cy + cell > y1 - 16) {
                    break;
                }
                boolean sel = equipped.equals(item.id());
                UiDraw.roundRect(context, cx, cy, cell, cell, 8, item.color());
                if (sel) {
                    UiDraw.roundBorder(context, cx - 1, cy - 1, cell + 2, cell + 2, 8, ACCENT);
                }
                context.drawText(textRenderer, Text.literal(fit(item.name(), cell)), cx + 4, cy + cell + 4, MUTED, false);
            }
            if (items.isEmpty()) {
                context.drawText(textRenderer, Text.literal("Nothing in this tab."), gx, gy + 8, MUTED, false);
            }
        } finally {
            context.disableScissor();
        }

        int rx = right + 14;
        context.drawText(textRenderer, Text.literal("Preview"), rx, y0 + 16, MUTED, false);
        int px = rx + 18;
        int py = y0 + 36;
        int belowPreviewY = Math.min(y1 - 72, py + 140);
        context.enableScissor(right + 8, py - 2, ox + pw - 12, belowPreviewY);
        try {
            // Prefer the live, slowly-spinning 3D preview (own player, or the looked-up player if
            // they're online in this world) over the flat static body render - only falls back to
            // the flat skin when there's no live entity to spin (an offline looked-up player).
            var liveEntity = playerLookup.isBlank() ? MinecraftClient.getInstance().player : onlinePlayerEntity(playerLookup);
            if (liveEntity != null) {
                CosmeticPreview.spin(context, right + 70, py + 110, 38, liveEntity);
            } else if (SkinPreview.ready(key)) {
                SkinPreview.drawBody(context, key, px, py, 4);
            } else {
                UiDraw.roundRect(context, px + 12, py, 48, 120, 12, 0x6614101C);
                context.drawText(textRenderer, Text.literal("Loading skin…"), px, py + 52, MUTED, false);
            }
        } catch (Throwable ignored) {
        } finally {
            context.disableScissor();
        }
        String eqName = equipped == null || equipped.isBlank() ? "None" : equipped;
        context.drawText(textRenderer, Text.literal("Equipped"), rx, belowPreviewY + 8, MUTED, false);
        context.drawText(textRenderer, Text.literal(fit(eqName, RIGHT_W - 36)), rx, belowPreviewY + 20, TEXT, false);
        context.drawText(textRenderer, Text.literal(kind.name()), rx, belowPreviewY + 34, MUTED, false);
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
                    UiDraw.roundRect(context, lx - 2, y, lw + 4, ROW_H - 4, 10, 0x444F8EFF);
                    UiDraw.roundRect(context, lx - 2, y + 6, 3, ROW_H - 16, 1, ACCENT);
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
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.getWidth(next) > max && !line.isEmpty()) {
                context.drawText(textRenderer, Text.literal(line.toString()), x, y, color, false);
                y += 10;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) {
            context.drawText(textRenderer, Text.literal(line.toString()), x, y, color, false);
        }
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
        if (inside(mx, my, ox + 86, oy + 8, 64, 20)) {
            topTab = 0;
            return true;
        }
        if (inside(mx, my, ox + 154, oy + 8, 76, 20)) {
            topTab = 1;
            return true;
        }
        if (inside(mx, my, ox + 234, oy + 8, 64, 20)) {
            topTab = 2;
            return true;
        }
        if (topTab == 1) {
            searchFocus = false;
            friendFocus = false;
            playerLookupFocus = false;
            return clickYou(mx, my);
        }
        if (topTab == 2) {
            searchFocus = false;
            cosSearchFocus = false;
            return clickFriends(mx, my);
        }
        cosSearchFocus = false;
        friendFocus = false;
        return clickClient(mx, my);
    }

    private boolean clickYou(int mx, int my) {
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = youLeft();
        int mid = youMid();
        int right = youRight();
        int leftW = mid - left - 8;
        int midW = right - mid - 8;
        int lx = left + 10;
        int lw = leftW - 20;

        if (inside(mx, my, lx, y0 + 16, lw, 22)) {
            playerLookupFocus = true;
            cosSearchFocus = false;
            return true;
        }
        playerLookupFocus = false;
        if (inside(mx, my, lx, y1 - 38, lw, 22)) {
            if (AccountManager.account != null) {
                AccountManager.logout();
            } else {
                AccountManager.startLogin();
            }
            return true;
        }

        int tabY = y0 + 16;
        int tx = mid + 10;
        String[] tabs = new String[COS_TABS.length + COS_EXTRA.length];
        System.arraycopy(COS_TABS, 0, tabs, 0, COS_TABS.length);
        System.arraycopy(COS_EXTRA, 0, tabs, COS_TABS.length, COS_EXTRA.length);
        for (int i = 0; i < tabs.length; i++) {
            int tw = Math.max(48, textRenderer.getWidth(tabs[i]) + 16);
            if (tx + tw > mid + midW - 8) {
                tx = mid + 10;
                tabY += 22;
            }
            if (inside(mx, my, tx, tabY, tw - 4, 20)) {
                cosTab = i;
                return true;
            }
            tx += tw;
        }
        int searchY = tabY + 26;
        cosSearchFocus = inside(mx, my, mid + 10, searchY, midW - 20, 22);
        Cosmetics.Kind kind = Cosmetics.kindForTab(cosTab);
        java.util.List<Cosmetics.Item> items = Cosmetics.of(kind, cosSearch);
        int gx = mid + 10;
        int gy = searchY + 32;
        int cell = 56;
        int gap = 8;
        int cols = Math.max(1, (midW - 20) / (cell + gap));
        for (int i = 0; i < items.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = gx + col * (cell + gap);
            int cy = gy + row * (cell + 20);
            if (cy + cell > y1 - 16) {
                break;
            }
            if (inside(mx, my, cx, cy, cell, cell)) {
                Cosmetics.equip(kind, items.get(i).id());
                cosSel = i;
                return true;
            }
        }
        return true;
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

        if (inside(mx, my, ox + 10, oy + ph - 34, SIDE_W - 20, 22)) {
            MinecraftClient.getInstance().setScreen(new HudLayoutScreen(this));
            return true;
        }

        int sy = oy + TOP + 42;
        if (inside(mx, my, ox + 8, sy, SIDE_W - 16, 24)) {
            filter = null;
            boundKeysOnly = false;
            rebuild();
            return true;
        }
        sy += 28;
        for (Category category : Category.values()) {
            if (!category.inSidebar()) {
                continue;
            }
            if (inside(mx, my, ox + 8, sy, SIDE_W - 16, 24)) {
                filter = category;
                boundKeysOnly = false;
                rebuild();
                return true;
            }
            sy += 28;
        }
        sy += 10;
        if (inside(mx, my, ox + 8, sy, SIDE_W - 16, 24)) {
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
        int rowY = oy + TOP + 8 - scroll;
        int w = listW();
        Category last = null;
        for (Module module : visible) {
            if (module.category != last) {
                last = module.category;
                rowY += 22;
            }
            if (inside(mx, my, lx, rowY, w, ROW_H - 2) && my >= oy + TOP + 8 && my <= oy + TOP + 8 + listH()) {
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
        int y = settingsStartY();
        Module.ModuleStyle style = selected.style();
        if (inside(mx, my, x + RIGHT_W - 92, y - 3, 78, 20)) {
            capturingKeybind = selected;
            focusedTextSetting = null;
            accentFocus = false;
            return true;
        }
        y += 28;
        if ("Emotes".equals(selected.name)) {
            if (inside(mx, my, x + RIGHT_W - 92, y - 3, 78, 20)) {
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
            } else if (setting.kind == Module.Setting.Kind.COLOR) {
                if (inside(mx, my, x + 14, y - 2, RIGHT_W - 28, 20)) {
                    colorFocus = setting;
                    focusedTextSetting = null;
                    accentFocus = false;
                    colorDraft = String.format("#%06X", setting.intGet.get() & 0xFFFFFF);
                    return true;
                }
                y += 22;
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
        if (draggingSetting != null) {
            applySliderDrag(draggingSetting, dragBarX, dragBarW, (int) click.x());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSetting != null) {
            AeroClient.CONFIG.save();
            draggingSetting = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (topTab == 2) {
            friendScroll = (int) Math.max(0, friendScroll - verticalAmount * 16);
            return true;
        }
        int max = Math.max(0, visible.size() * ROW_H + sectionGaps() - listH());
        scroll = (int) Math.max(0, Math.min(max, scroll - verticalAmount * 16));
        return true;
    }

    public boolean handleKey(int key) {
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
        if (cosSearchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !cosSearch.isEmpty()) {
                cosSearch = cosSearch.substring(0, cosSearch.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                cosSearchFocus = false;
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
        if (cosSearchFocus && cp >= 32 && cp != 127) {
            cosSearch += Character.toString(cp);
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
