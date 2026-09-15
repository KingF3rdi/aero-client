package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.auth.AccountManager;
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
    private static final int BG = 0xCC09080F;
    private static final int BAR = 0xB80C0B14;
    private static final int SIDE = 0xB00A0912;
    private static final int CARD = 0xCC12111A;
    private static final int PILL = 0xFF262232;
    private static final int PILL_ON = 0xFF322C44;
    private static final int MUTED = 0xFF8E889C;
    private static final int TEXT = 0xFFF3F0F8;
    private static final int ACCENT = 0xFFC4B5FD;
    private static final int LAV = 0xFFE4DCF6;
    private static final int TRACK_OFF = 0xFF2A2A36;
    private static final int TRACK_ON = 0xFFD8D0EC;
    private static final int KNOB_ON = 0xFFF6F2FC;
    private static final int KNOB_OFF = 0xFF9A96A8;
    private static final int LINE = 0x14FFFFFF;

    private static final int TOP = 36;
    private static final int SIDE_W = 168;
    private static final int RIGHT_W = 248;
    private static final int ROW_H = 32;

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
    private int friendScroll;
    private boolean ambienceReset;
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
        super(Text.literal("Larp Launcher"));
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
        pw = Math.min(width - 80, 820);
        ph = Math.min(height - 80, 492);
        ox = (width - pw) / 2;
        oy = (height - ph) / 2;
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
        return Math.max(180, pw - SIDE_W - RIGHT_W - 16);
    }

    private int listH() {
        return ph - TOP - 12;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        UiDraw.menuOpen = true;
        try {
            layoutPanel();
            try {
                applyBlur(context);
            } catch (Throwable ignored) {
            }
            context.fill(0, 0, width, height, 0x3A07060E);
            if (AeroClient.MODULES == null) {
                return;
            }
            UiDraw.glass(context, ox, oy, pw, ph, 0xC016141F, 16);
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
        } finally {
            UiDraw.menuOpen = false;
        }
    }

    private void drawTop(DrawContext context, int mx, int my) {
        int x0 = ox;
        int y0 = oy;
        int x1 = ox + pw;
        context.fill(x0, y0, x1, y0 + TOP, BAR);
        context.fill(x0, y0, x1, y0 + 1, 0x28FFFFFF);
        context.fill(x0, y0 + TOP - 1, x1, y0 + TOP, 0x66C4B5FD);

        UiDraw.raised(context, x0 + 10, y0 + 8, 18, 18, ACCENT);
        context.drawText(textRenderer, Text.literal("A"), x0 + 16, y0 + 13, 0xFF1A1024, false);
        context.drawText(textRenderer, Text.literal("Larp"), x0 + 34, y0 + 13, TEXT, false);

        int tabX = x0 + 78;
        tabPill(context, tabX, y0 + 8, 62, topTab == 0, "Client");
        tabPill(context, tabX + 66, y0 + 8, 72, topTab == 1, "Wardrobe");
        tabPill(context, tabX + 142, y0 + 8, 62, topTab == 2, "Friends");

        String beta = "Beta " + AeroClient.VERSION;
        int betaW = textRenderer.getWidth(beta);
        int betaX = x1 - 34 - betaW;
        context.drawText(textRenderer, Text.literal(beta), betaX, y0 + 13, MUTED, false);

        int profilesX = profilesButtonX();
        boolean profilesH = inside(mx, my, profilesX, y0 + 8, PROFILES_W, 20);
        UiDraw.pill(context, profilesX, y0 + 8, PROFILES_W, 20, profilesH);
        String profilesLabel = "Profiles";
        context.drawText(textRenderer, Text.literal(profilesLabel),
                profilesX + (PROFILES_W - textRenderer.getWidth(profilesLabel)) / 2, y0 + 14,
                profilesH ? TEXT : MUTED, false);

        String name = AccountManager.currentName();
        int nw = textRenderer.getWidth(name);
        int nameX = profilesX - 16 - nw;
        UiDraw.raised(context, nameX - 16, y0 + 10, 12, 12, 0xFFC8A0E8);
        context.drawText(textRenderer, Text.literal(name), nameX, y0 + 13, TEXT, false);

        boolean closeH = inside(mx, my, x1 - 22, y0 + 10, 14, 14);
        context.drawText(textRenderer, Text.literal("x"), x1 - 20, y0 + 13, closeH ? TEXT : MUTED, false);
    }

    private static final int PROFILES_W = 58;

    private int profilesButtonX() {
        String beta = "Beta " + AeroClient.VERSION;
        int betaX = ox + pw - 34 - textRenderer.getWidth(beta);
        return betaX - 8 - PROFILES_W;
    }

    private void tabPill(DrawContext context, int x, int y, int w, boolean on, String label) {
        UiDraw.pill(context, x, y, w, 20, on);
        int tw = textRenderer.getWidth(label);
        context.drawText(textRenderer, Text.literal(label), x + (w - tw) / 2, y + 6, on ? TEXT : MUTED, false);
    }

    private void drawSidebar(DrawContext context, int mx, int my) {
        int x0 = ox;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        context.fill(x0, y0, x0 + SIDE_W, y1, SIDE);
        context.fill(x0 + SIDE_W - 1, y0, x0 + SIDE_W, y1, 0x22FFFFFF);

        int sy = y0 + 10;
        UiDraw.inset(context, x0 + 10, sy, SIDE_W - 20, 20, searchFocus ? 0xFF16141F : CARD);
        String q = search.isEmpty() && !searchFocus ? "Search" : search + (searchFocus ? "|" : "");
        context.drawText(textRenderer, Text.literal(q), x0 + 16, sy + 6,
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
        context.enableScissor(x, y0, x + w, y0 + listH());

        int y = y0 - scroll;
        Category last = null;
        for (Module module : visible) {
            if (module.category != last) {
                last = module.category;
                context.drawText(textRenderer, Text.literal(last.title),
                        x + 8, y + 8, MUTED, false);
                y += 22;
            }
            boolean h = inside(mx, my, x, y, w, ROW_H - 2);
            boolean sel = module == selected;
            int rowBottom = y + ROW_H - 4;
            if (sel || h) {
                UiDraw.roundRect(context, x + 6, y, w - 12, rowBottom - y, 7, sel ? 0xAA3A3158 : 0x4414121C);
            }
            drawModIcon(context, module, x + 10, y + 7);
            context.drawText(textRenderer, Text.literal(module.name), x + 28, y + 10, TEXT, false);
            drawSwitch(context, x + w - 42, y + 8, module.enabled());
            y += ROW_H;
        }
        context.disableScissor();
    }

    private void drawRight(DrawContext context, int mx, int my) {
        int x = ox + pw - RIGHT_W;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        context.fill(x, y0, ox + pw, y1, 0);
        context.fill(x, y0, x + 1, y1, 0);

        if (selected == null) {
            context.drawText(textRenderer, Text.literal("Settings"), x + 14, y0 + 14, TEXT, false);
            context.drawText(textRenderer, Text.literal("Pick a module."), x + 14, y0 + 30, MUTED, false);
            return;
        }

        context.drawText(textRenderer, Text.literal(selected.name), x + 14, y0 + 12, TEXT, false);
        drawSwitch(context, ox + pw - 42, y0 + 10, selected.enabled());
        boolean resetHover = inside(mx, my, ox + pw - 42 - 46, y0 + 9, 40, 16);
        UiDraw.pill(context, ox + pw - 42 - 46, y0 + 9, 40, 16, resetHover);
        context.drawText(textRenderer, Text.literal("Reset"), ox + pw - 42 - 46 + 6, y0 + 13,
                resetHover ? TEXT : MUTED, false);
        wrap(context, selected.description, x + 14, y0 + 28, RIGHT_W - 28, MUTED);

        int y = settingsStartY();
        Module.ModuleStyle style = selected.style();
        context.drawText(textRenderer, Text.literal("Toggle Key"), x + 14, y + 4, MUTED, false);
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
        return oy + TOP + 48;
    }

    private void drawYou(DrawContext context, int mx, int my) {
        int x0 = ox;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = x0 + 16;
        int mid = x0 + Math.max(180, pw * 27 / 100);
        int right = ox + pw - Math.max(160, pw * 22 / 100);
        Cosmetics.Kind kind = Cosmetics.kindForTab(cosTab);
        java.util.List<Cosmetics.Item> items = Cosmetics.of(kind, cosSearch);
        String equipped = Cosmetics.equipped(kind);

        UiDraw.innerCard(context, x0 + 4, y0 + 4, mid - 12 - x0, y1 - y0 - 8);
        UiDraw.innerCard(context, right, y0 + 4, ox + pw - right - 4, y1 - y0 - 8);

        UiDraw.inset(context, left - 4, y0 + 10, mid - 20 - left, 20, CARD);
        context.drawText(textRenderer, Text.literal("Look up a player..."), left + 2, y0 + 16, MUTED, false);

        String name = AccountManager.currentName();
        UiDraw.raised(context, left + 4, y0 + 42, 24, 24, 0xFFC8A0E8);
        context.drawText(textRenderer, Text.literal(name), left + 36, y0 + 44, TEXT, false);
        context.drawText(textRenderer, Text.literal("Online  ·  Java Edition"), left + 36, y0 + 56, MUTED, false);

        context.drawText(textRenderer, Text.literal("Alpha   Staff   Beta"), left + 4, y0 + 80, MUTED, false);
        context.drawText(textRenderer, Text.literal("Tester"), left + 4, y0 + 94, MUTED, false);
        context.drawText(textRenderer, Text.literal("Playtime"), left + 4, y0 + 118, MUTED, false);
        context.drawText(textRenderer, Text.literal("—"), left + 104, y0 + 118, TEXT, false);
        context.drawText(textRenderer, Text.literal("First seen"), left + 4, y0 + 132, MUTED, false);
        context.drawText(textRenderer, Text.literal("—"), left + 104, y0 + 132, TEXT, false);
        context.drawText(textRenderer, Text.literal("Friends"), left + 4, y0 + 146, MUTED, false);
        context.drawText(textRenderer, Text.literal(String.valueOf(FriendStore.size())), left + 104, y0 + 146, TEXT, false);

        int signInW = mid - 24 - left;
        UiDraw.pill(context, left, y1 - 30, signInW, 20, inside(mx, my, left, y1 - 30, signInW, 20));
        context.drawText(textRenderer, Text.literal("Sign in with Microsoft"), left + 10, y1 - 25, TEXT, false);

        int tx = mid;
        int ty = y0 + 12;
        for (int i = 0; i < COS_TABS.length; i++) {
            boolean on = cosTab == i;
            int tw = textRenderer.getWidth(COS_TABS[i]) + 12;
            context.drawText(textRenderer, Text.literal(COS_TABS[i]), tx, ty, on ? TEXT : MUTED, false);
            if (on) {
                context.fill(tx, ty + 11, tx + tw - 8, ty + 12, ACCENT);
            }
            tx += tw;
        }
        tx = mid;
        ty = y0 + 28;
        for (int i = 0; i < COS_EXTRA.length; i++) {
            boolean on = cosTab == 7 + i;
            int tw = textRenderer.getWidth(COS_EXTRA[i]) + 12;
            context.drawText(textRenderer, Text.literal(COS_EXTRA[i]), tx, ty, on ? TEXT : MUTED, false);
            if (on) {
                context.fill(tx, ty + 11, tx + tw - 8, ty + 12, ACCENT);
            }
            tx += tw;
        }

        UiDraw.inset(context, mid, y0 + 46, right - mid - 12, 20, CARD);
        String hint = cosSearch.isEmpty() && !cosSearchFocus ? "Search cosmetics..." : cosSearch + (cosSearchFocus ? "_" : "");
        context.drawText(textRenderer, Text.literal(hint), mid + 8, y0 + 52, cosSearchFocus ? TEXT : MUTED, false);

        int gx = mid;
        int gy = y0 + 76;
        int cell = 70;
        int gap = 10;
        int cols = Math.max(2, (right - mid - 8) / (cell + gap));
        for (int i = 0; i < items.size(); i++) {
            Cosmetics.Item item = items.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = gx + col * (cell + gap);
            int cy = gy + row * (cell + 22);
            if (cy + cell > y1 - 24) {
                break;
            }
            boolean sel = equipped.equals(item.id());
            UiDraw.raised(context, cx, cy, cell, cell, item.color());
            if (sel) {
                UiDraw.roundBorder(context, cx - 1, cy - 1, cell + 2, cell + 2, 6, ACCENT);
            }
            context.drawText(textRenderer, Text.literal(item.name()), cx + 4, cy + cell + 4, MUTED, false);
        }

        context.drawText(textRenderer, Text.literal("Preview"), right + 16, y0 + 14, MUTED, false);
        int px = right + 50;
        int py = y0 + 40;
        int scale = Math.min(100, (y1 - py - 24) * 100 / 108);
        UiDraw.shadow(context, px + 18, py, 32, 108 * scale / 100);
        context.fill(px + 18, py, px + 50, py + 28 * scale / 100, 0xFF6A4A88);
        context.fill(px + 22, py + 28 * scale / 100, px + 46, py + 70 * scale / 100, 0xFF4A3068);
        context.fill(px + 8, py + 32 * scale / 100, px + 24, py + 68 * scale / 100, 0xFF3A2858);
        context.fill(px + 44, py + 32 * scale / 100, px + 60, py + 68 * scale / 100, 0xFF3A2858);
        context.fill(px + 22, py + 70 * scale / 100, px + 34, py + 108 * scale / 100, 0xFF2A2048);
        context.fill(px + 34, py + 70 * scale / 100, px + 46, py + 108 * scale / 100, 0xFF2A2048);
        context.fill(px + 24, py + 8, px + 44, py + 24, 0xFFE8C8F0);
        context.fill(px + 20, py + 2, px + 48, py + 4, 0x33FFFFFF);
        CosmeticPreview.player(context, right + 110, py + 108 * scale / 100, 42 * scale / 100, mx, my);

        int belowPreviewY = Math.min(y1 - 60, py + 108 * scale / 100 + 12);
        context.drawText(textRenderer, Text.literal("Cape   Wings   Trail"), right + 16, belowPreviewY, MUTED, false);
        context.drawText(textRenderer, Text.literal("Presets"), right + 16, belowPreviewY + 28, TEXT, false);
        context.drawText(textRenderer, Text.literal("None yet"), right + 16, belowPreviewY + 42, MUTED, false);
        context.drawText(textRenderer, Text.literal("Save keeps your setup."), right + 16, belowPreviewY + 54, MUTED, false);
    }

    private void drawFriends(DrawContext context, int mx, int my) {
        int x0 = ox;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = x0 + Math.max(160, pw * 25 / 100);
        int rightW = Math.max(150, pw * 20 / 100);
        int rightX = ox + pw - rightW;
        UiDraw.innerCard(context, x0 + 4, y0 + 4, left - 8 - x0, y1 - y0 - 8);
        UiDraw.innerCard(context, rightX, y0 + 4, ox + pw - rightX - 4, y1 - y0 - 8);

        UiDraw.inset(context, x0 + 10, y0 + 10, left - 30 - x0, 20, CARD);
        String draft = friendDraft.isEmpty() && !friendFocus ? "Add a friend..." : friendDraft + (friendFocus ? "_" : "");
        context.drawText(textRenderer, Text.literal(draft), x0 + 16, y0 + 16, friendFocus ? TEXT : MUTED, false);

        UiDraw.pill(context, x0 + 10, y0 + 36, 72, 16, true);
        context.drawText(textRenderer, Text.literal("Friends " + FriendStore.size()), x0 + 16, y0 + 40, TEXT, false);
        UiDraw.pill(context, x0 + 88, y0 + 36, 80, 16, false);
        context.drawText(textRenderer, Text.literal("Requests 0"), x0 + 94, y0 + 40, MUTED, false);

        int y = y0 + 62 - friendScroll;
        for (int i = 0; i < FriendStore.size(); i++) {
            if (y + 20 < y0 + 58 || y > y1 - 16) {
                y += 20;
                continue;
            }
            String n = FriendStore.get(i);
            boolean on = FriendStore.online(n);
            boolean sel = friendSel == i;
            if (sel) {
                UiDraw.raised(context, x0 + 8, y, left - 24 - x0, 20, PILL);
            }
            UiDraw.raised(context, x0 + 12, y + 4, 10, 10, on ? 0xFFC8A0E8 : 0xFF3A3A44);
            context.drawText(textRenderer, Text.literal(n), x0 + 28, y + 6, TEXT, false);
            context.drawText(textRenderer, Text.literal(on ? "online" : "offline"),
                    left - 60, y + 6, MUTED, false);
            y += 20;
        }

        String a = FriendStore.ownName();
        String b = FriendStore.size() > 0 ? FriendStore.get(0) : "HeyMake";
        String c = FriendStore.size() > 1 ? FriendStore.get(1) : "Cloudy";
        context.drawText(textRenderer, Text.literal("You"), left + 16, y0 + 16, MUTED, false);
        UiDraw.raised(context, left + 16, y0 + 32, 24, 24, 0xFFE0B0F0);
        UiDraw.raised(context, left + 52, y0 + 32, 24, 24, 0xFFE8E8F0);
        UiDraw.raised(context, left + 88, y0 + 32, 24, 24, 0xFF8A70B0);
        context.drawText(textRenderer, Text.literal(a + "    " + b + "    " + c), left + 16, y0 + 62, MUTED, false);

        UiDraw.pill(context, Math.min(left + 220, rightX - 74), y0 + 36, 64, 16, false);
        context.drawText(textRenderer, Text.literal("Public"), Math.min(left + 230, rightX - 64), y0 + 40, MUTED, false);

        context.drawText(textRenderer, Text.literal("Public stories"), left + 16, y0 + 92, TEXT, false);
        context.drawText(textRenderer, Text.literal("From people you have not added"), left + 16, y0 + 104, MUTED, false);
        UiDraw.raised(context, left + 16, y0 + 124, 24, 24, 0xFF6A5040);
        UiDraw.raised(context, left + 56, y0 + 124, 24, 24, 0xFFE0A040);
        context.drawText(textRenderer, Text.literal("Ruhop"), left + 16, y0 + 154, MUTED, false);
        context.drawText(textRenderer, Text.literal("Foolraven"), left + 56, y0 + 154, MUTED, false);

        context.drawText(textRenderer, Text.literal("Pick someone on the left to talk."),
                left + 16, y1 - 28, MUTED, false);

        if (friendSel >= 0 && friendSel < FriendStore.size()) {
            String picked = FriendStore.get(friendSel);
            context.drawText(textRenderer, Text.literal(picked), rightX + 16, y0 + 16, TEXT, false);
            wrap(context, "Pick a friend to see their tiers and badges.", rightX + 16, y0 + 32, rightW - 32, MUTED);
        } else {
            context.drawText(textRenderer, Text.literal("Nobody picked"), rightX + 16, y0 + 16, TEXT, false);
            wrap(context, "Pick a friend to see their tiers and badges.", rightX + 16, y0 + 32, rightW - 32, MUTED);
        }
    }

    private void rowToggle(DrawContext context, int panelX, int y, String name, boolean on) {
        UiDraw.inset(context, panelX + 10, y, RIGHT_W - 20, 22, 0xFF12101A);
        context.drawText(textRenderer, Text.literal(name), panelX + 16, y + 7, TEXT, false);
        drawSwitch(context, panelX + RIGHT_W - 48, y + 3, on);
    }

    private void drawModIcon(DrawContext context, Module module, int x, int y) {
        int col = switch (module.category) {
            case PVP -> 0xFFC4B5FD;
            case HUD -> 0xFFA78BFA;
            case RENDER -> 0xFFDDD6FE;
            case PLAYER -> 0xFFE9D5FF;
            default -> 0xFFC4B5FD;
        };
        int t = Math.abs(module.name.hashCode()) % 6;
        if (t == 0) {
            context.fill(x + 4, y, x + 7, y + 10, col);
            context.fill(x + 1, y + 4, x + 10, y + 7, col);
        } else if (t == 1) {
            context.fill(x + 3, y + 1, x + 8, y + 10, col);
            context.fill(x + 1, y + 3, x + 10, y + 8, col);
        } else if (t == 2) {
            context.fill(x + 1, y + 2, x + 10, y + 4, col);
            context.fill(x + 1, y + 6, x + 10, y + 8, col);
        } else if (t == 3) {
            context.fill(x + 2, y + 1, x + 9, y + 3, col);
            context.fill(x + 4, y + 3, x + 7, y + 10, col);
        } else if (t == 4) {
            context.fill(x + 1, y + 1, x + 10, y + 10, col);
            context.fill(x + 3, y + 3, x + 8, y + 8, 0xFF16141F);
        } else {
            context.fill(x + 2, y + 8, x + 5, y + 10, col);
            context.fill(x + 6, y + 4, x + 9, y + 10, col);
            context.fill(x + 3, y + 1, x + 8, y + 5, col);
        }
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

    public boolean handleClick(double x, double y, int button) {
        layoutPanel();
        int mx = (int) x;
        int my = (int) y;
        if (button != 0) {
            return false;
        }
        if (inside(mx, my, ox + pw - 22, oy + 10, 14, 14)) {
            closeMenu();
            return true;
        }
        if (inside(mx, my, profilesButtonX(), oy + 8, PROFILES_W, 20)) {
            MinecraftClient.getInstance().setScreen(new ProfilesScreen(this));
            return true;
        }
        if (inside(mx, my, ox + 78, oy + 8, 62, 20)) {
            topTab = 0;
            return true;
        }
        if (inside(mx, my, ox + 144, oy + 8, 72, 20)) {
            topTab = 1;
            return true;
        }
        if (inside(mx, my, ox + 220, oy + 8, 62, 20)) {
            topTab = 2;
            return true;
        }
        if (topTab == 1) {
            searchFocus = false;
            friendFocus = false;
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
        int x0 = ox;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = x0 + 16;
        int mid = x0 + Math.max(180, pw * 27 / 100);
        int right = ox + pw - Math.max(160, pw * 22 / 100);

        int signInW = mid - 24 - left;
        if (inside(mx, my, left, y1 - 30, signInW, 20)) {
            AccountManager.startLogin();
            return true;
        }
        int tx = mid;
        for (int i = 0; i < COS_TABS.length; i++) {
            int tw = textRenderer.getWidth(COS_TABS[i]) + 12;
            if (inside(mx, my, tx, y0 + 8, tw, 16)) {
                cosTab = i;
                return true;
            }
            tx += tw;
        }
        tx = mid;
        for (int i = 0; i < COS_EXTRA.length; i++) {
            int tw = textRenderer.getWidth(COS_EXTRA[i]) + 12;
            if (inside(mx, my, tx, y0 + 24, tw, 16)) {
                cosTab = 7 + i;
                return true;
            }
            tx += tw;
        }
        cosSearchFocus = inside(mx, my, mid, y0 + 46, right - mid - 12, 20);
        Cosmetics.Kind kind = Cosmetics.kindForTab(cosTab);
        java.util.List<Cosmetics.Item> items = Cosmetics.of(kind, cosSearch);
        int cell = 70;
        int gap = 10;
        int cols = Math.max(2, (right - mid - 8) / (cell + gap));
        for (int i = 0; i < items.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = mid + col * (cell + gap);
            int cy = y0 + 76 + row * (cell + 22);
            if (cy + cell > y1 - 24) {
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
        int x0 = ox;
        int y0 = oy + TOP;
        int y1 = oy + ph;
        int left = Math.max(160, pw * 25 / 100);
        friendFocus = inside(mx, my, x0 + 10, y0 + 10, left - 20, 20);
        int y = y0 + 62 - friendScroll;
        for (int i = 0; i < FriendStore.size(); i++) {
            if (inside(mx, my, x0 + 8, y, left - 16, 20) && y >= y0 + 58 && y <= y1 - 16) {
                friendSel = i;
                return true;
            }
            y += 20;
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
        style.accent = 0xFFC4B5FD;
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
                if (setting.group && inside(mx, my, tx, y - 2, 16, 20)) {
                    toggleGroup(setting);
                    return true;
                }
                if (inside(mx, my, x + RIGHT_W - 92, y - 3, 78, 20)) {
                    toggleGroup(setting);
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

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}
