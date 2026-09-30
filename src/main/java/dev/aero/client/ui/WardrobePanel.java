package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.auth.AccountManager;
import dev.aero.client.auth.SavedAccount;
import dev.aero.client.auth.SkinPreview;
import dev.aero.client.cosmetic.CosmeticPreview;
import dev.aero.client.cosmetic.Cosmetics;
import dev.aero.client.cosmetic.CustomCapes;
import dev.aero.client.social.Shards;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wardrobe: categories left, live preview center, item grid right. Search, sort (newest, oldest,
 * rarity, price, name) and filters (owned, rarity); hovering an item tries it on in the preview, and
 * clicking something you don't own opens the buy dialog (price, balance, you wearing it).
 */
public final class WardrobePanel {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final int PAD = 8;

    private static final String[] TAB_NAMES =
            {"Capes", "Wings", "Headwear", "Trails", "Kill", "Mace", "Pets", "Emotes", "Chat tags", "Badges", "Custom Capes"};
    private static final int TAB_CUSTOM_CAPES = 10;
    private static final int[] TAB_ORDER = {0, TAB_CUSTOM_CAPES, 2, 6, 7, 1, 3, 4, 5};
    private static final String[] SORTS = {"Newest", "Oldest", "Rarity", "Price", "Name"};
    private static final String[] RARITY_FILTER = {"All", "Common", "Uncommon", "Rare", "Legendary"};

    public String search = "";
    public boolean searchFocus;
    /** File name (in aero-capes/) the player is about to publish as a community cape - Custom Capes tab only. */
    public String publishName = "";
    public boolean publishFocus;
    private int tab;
    private int scroll;
    /** Preview rotation: 20 shows the back (capes), 200 the front. */
    private float yaw = 20f;
    private float zoom = 1f;
    private boolean dragging;
    private int sort;
    private boolean ownedOnly;
    private int rarityFilter;
    /** Item waiting in the buy dialog, or null. */
    private Cosmetics.Item pendingBuy;
    private boolean buying;

    // layout, recomputed by layout()
    private int lx, lw, cx, cw, rx, rw, top, height, step = 21;

    private void layout(int x, int y, int w, int h) {
        top = y + 4;
        height = h - 12;
        step = Math.max(12, Math.min(21, (height - 42) / TAB_ORDER.length));
        lx = x + 12;
        lw = Math.min(128, w * 20 / 100);
        rw = Math.min(300, w * 40 / 100);
        rx = x + w - 12 - rw;
        cx = lx + lw + PAD;
        cw = rx - PAD - cx;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public void debugTab(int t) {
        tab = t;
        yaw = t <= 1 ? 35f : 200f;
    }

    /** Store / Badges page asked to buy something: show its wardrobe tab with the buy dialog open. */
    public void openBuy(Cosmetics.Item item) {
        tab = item.kind() == Cosmetics.Kind.BADGE ? 9 : 0;
        yaw = tab == 0 ? 20f : 200f;
        scroll = 0;
        search = "";
        pendingBuy = item;
        Shards.status = "";
    }

    private Cosmetics.Kind kind() {
        return Cosmetics.kindForTab(tab);
    }

    private boolean customCapesTab() {
        return tab == TAB_CUSTOM_CAPES;
    }

    private static int priceOf(Cosmetics.Item item) {
        String cid = item.catalogId();
        if (cid == null) {
            return -1;
        }
        int p = Shards.priceOf(cid);
        return p >= 0 ? p : item.rarity().price;
    }

    private List<Cosmetics.Item> gridItems(Cosmetics.Kind kind) {
        List<Cosmetics.Item> raw = customCapesTab() ? Cosmetics.customCapeItems(search) : Cosmetics.of(kind, search);
        List<Cosmetics.Item> out = new ArrayList<>();
        Cosmetics.Item none = null;
        for (Cosmetics.Item item : raw) {
            if ("none".equals(item.id())) {
                none = item;
                continue;
            }
            if (ownedOnly && !Cosmetics.owns(item)) {
                continue;
            }
            if (rarityFilter > 0 && item.rarity().ordinal() != rarityFilter - 1) {
                continue;
            }
            out.add(item);
        }
        Comparator<Cosmetics.Item> byName = Comparator.comparing(i -> i.name().toLowerCase(java.util.Locale.ROOT));
        switch (sort) {
            case 0 -> out.sort(Comparator.comparingInt((Cosmetics.Item i) -> -i.added()).thenComparing(byName));
            case 1 -> out.sort(Comparator.comparingInt(Cosmetics.Item::added).thenComparing(byName));
            case 2 -> out.sort(Comparator.comparingInt((Cosmetics.Item i) -> -i.rarity().ordinal()).thenComparing(byName));
            case 3 -> out.sort(Comparator.comparingInt((Cosmetics.Item i) -> -Math.max(0, priceOf(i))).thenComparing(byName));
            default -> out.sort(byName);
        }
        if (none != null && !ownedOnly && rarityFilter == 0) {
            out.add(0, none);
        }
        return out;
    }

    private int previewTop() {
        return top + 24;
    }

    private int previewBottom() {
        return top + height - 46;
    }

    private boolean modelKind(Cosmetics.Kind k) {
        return k != Cosmetics.Kind.TAG && k != Cosmetics.Kind.BADGE && k != Cosmetics.Kind.EMOTE;
    }

    private int cardH(Cosmetics.Kind k) {
        return Cosmetics.hasVariants(k) ? 88 : 74;
    }

    private static String ownSkinKey() {
        SavedAccount acc = AccountManager.account;
        if (acc != null && acc.uuid != null) {
            return acc.uuid.toString().replace("-", "").toLowerCase();
        }
        return AccountManager.currentName().toLowerCase();
    }

    // ---- render -------------------------------------------------------------------------------

    public void render(DrawContext ctx, int mx, int my, int x, int y, int w, int h) {
        layout(x, y, w, h);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        Cosmetics.Kind kind = kind();

        if (pendingBuy != null && !buying && Cosmetics.owns(pendingBuy)) {
            pendingBuy = null; // bought (here or elsewhere) - nothing left to confirm
        }
        Cosmetics.Item hovered = pendingBuy == null ? hoveredItem(kind, mx, my) : null;
        if (pendingBuy != null) {
            Cosmetics.preview(pendingBuy.kind(), pendingBuy.id());
        } else if (hovered != null && modelKind(kind) && !Cosmetics.locked(kind)) {
            Cosmetics.preview(kind, hovered.id());
        } else {
            Cosmetics.clearPreview();
        }

        drawLeft(ctx, tr, mx, my);
        UiDraw.innerCard(ctx, cx, top, cw, height);
        ctx.drawText(tr, Text.literal(TAB_NAMES[tab]), cx + 12, top + 10, TEXT, false);
        if (Cosmetics.locked(kind)) {
            String l = "Locked for now";
            ctx.drawText(tr, Text.literal(l), cx + cw - 12 - tr.getWidth(l), top + 10, 0xFFB45309, false);
        }
        drawPreview(ctx, tr, mx, my, kind, t, hovered);
        drawInfoBar(ctx, tr, mx, my, kind);
        UiDraw.innerCard(ctx, rx, top, rw, height);
        drawGrid(ctx, tr, mx, my, kind, t);
        if (pendingBuy != null) {
            drawBuyDialog(ctx, tr, mx, my, x, y, w, h);
        }
    }

    private void drawLeft(DrawContext ctx, TextRenderer tr, int mx, int my) {
        UiDraw.innerCard(ctx, lx, top, lw, height);
        SkinPreview.requestOwn();
        String key = ownSkinKey();
        int hx = lx + 8;
        int hy = top + 8;
        int hs = 22;
        if (SkinPreview.ready(key)) {
            SkinPreview.drawHead(ctx, key, hx, hy, hs);
        } else {
            UiDraw.roundRect(ctx, hx, hy, hs, hs, 6, 0xFFD6DBE6);
        }
        String name = AccountManager.currentName();
        ctx.drawText(tr, Text.literal(fit(tr, name, lw - 44)), hx + hs + 6, hy + 2, TEXT, false);
        String rank = Shards.rank();
        ctx.drawText(tr, Text.literal(rank.isEmpty() ? "Profile" : Character.toUpperCase(rank.charAt(0)) + rank.substring(1)),
                hx + hs + 6, hy + 12, MUTED, false);

        int y = top + 38;
        for (int value : TAB_ORDER) {
            if (value == 1) {
                context(ctx, lx + 12, y + 1, lw - 24);
                y += 5;
            }
            boolean on = tab == value;
            boolean hover = in(mx, my, lx + 6, y, lw - 12, step - 2);
            if (on) {
                UiDraw.pill(ctx, lx + 6, y, lw - 12, step - 2, true);
            } else if (hover) {
                UiDraw.roundRect(ctx, lx + 6, y, lw - 12, step - 2, (step - 2) / 2, UiDraw.HOVER);
            }
            ctx.drawText(tr, Text.literal(TAB_NAMES[value]), lx + 14, y + (step - 2 - 8) / 2 + 1, on || hover ? TEXT : MUTED, false);
            if (Cosmetics.locked(Cosmetics.kindForTab(value))) {
                drawLock(ctx, lx + lw - 20, y + (step - 2 - 8) / 2, 0xFFA0A6B4);
            }
            y += step;
        }
    }

    private static void context(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, 0x12000000);
    }

    /** Small padlock glyph (8x8). */
    static void drawLock(DrawContext ctx, int x, int y, int col) {
        ctx.fill(x + 2, y, x + 6, y + 1, col);
        ctx.fill(x + 1, y + 1, x + 2, y + 4, col);
        ctx.fill(x + 6, y + 1, x + 7, y + 4, col);
        ctx.fill(x, y + 4, x + 8, y + 9, col);
        ctx.fill(x + 3, y + 5, x + 5, y + 7, 0xFFFFFFFF);
    }

    private static final int BTN_W = 56;
    private static final int BTN_H = 14;

    private int btnX(int i) {
        return cx + cw - 8 - (2 - i) * (BTN_W + 4) + 4;
    }

    private int btnY() {
        return top + 26;
    }

    private void drawPreview(DrawContext ctx, TextRenderer tr, int mx, int my, Cosmetics.Kind kind, float t, Cosmetics.Item hovered) {
        int px1 = cx + 8;
        int px2 = cx + cw - 8;
        int py1 = previewTop();
        int py2 = previewBottom();
        String equipped = Cosmetics.equipped(kind);
        Cosmetics.Item item = hovered != null && !Cosmetics.locked(kind) ? hovered
                : "none".equals(equipped) ? null : Cosmetics.named(kind, equipped);
        int color = item == null ? 0 : Cosmetics.variantColor(item, Cosmetics.variantIndex(kind, item.id()));
        int midX = (px1 + px2) / 2;

        boolean others = AeroClient.CONFIG == null || AeroClient.CONFIG.showOthersCosmetics;
        String lab = others ? "Others on" : "Others off";
        boolean oh = in(mx, my, btnX(1), btnY(), BTN_W, BTN_H);
        UiDraw.pill(ctx, btnX(1), btnY(), BTN_W, BTN_H, others || oh);
        String l2 = fit(tr, lab, BTN_W - 6);
        ctx.drawText(tr, Text.literal(l2), btnX(1) + (BTN_W - tr.getWidth(l2)) / 2, btnY() + 3, others ? TEXT : MUTED, false);

        if (modelKind(kind)) {
            ctx.enableScissor(px1, py1, px2, py2);
            try {
                float scale = (py2 - py1) * 0.34f * zoom;
                CosmeticPreview.showSelf(ctx, px1, py1, px2, py2, scale, yaw);
                // Trails, kill and mace effects happen in the world, so the preview shows them as an animation.
                if (item != null && (Cosmetics.locked(kind) || kind == Cosmetics.Kind.TRAIL)) {
                    int feet = py1 + (py2 - py1) * 82 / 100;
                    switch (kind) {
                        case TRAIL -> CosmeticIcons.trail(ctx, item.id(), color, px1, feet, midX - px1 - 14, t);
                        case KILL_EFFECT -> CosmeticIcons.burst(ctx, item.id(), color, midX + 36, (py1 + py2) / 2, (py2 - py1) * 0.3f, t);
                        case MACE -> CosmeticIcons.mace(ctx, item.id(), color, midX, feet - 4, (py2 - py1) * 0.34f, t);
                        default -> {
                        }
                    }
                }
            } finally {
                ctx.disableScissor();
            }
            arrow(ctx, tr, mx, my, px1 + 4, (py1 + py2) / 2 - 11, "‹");
            arrow(ctx, tr, mx, my, px2 - 26, (py1 + py2) / 2 - 11, "›");
            String hint = hovered != null && !Cosmetics.locked(kind) ? "Trying on " + hovered.name() : "Drag to rotate, scroll to zoom";
            ctx.drawText(tr, Text.literal(hint), midX - tr.getWidth(hint) / 2, previewBottom() - 10, MUTED, false);
        } else {
            drawChatPreview(ctx, tr, kind, item, color, midX, (py1 + py2) / 2);
        }
    }

    private void drawChatPreview(DrawContext ctx, TextRenderer tr, Cosmetics.Kind kind, Cosmetics.Item item, int color, int midX, int midY) {
        String name = AccountManager.currentName();
        String head = kind == Cosmetics.Kind.BADGE ? "On your name" : "Emote wheel";
        ctx.drawText(tr, Text.literal(head), midX - tr.getWidth(head) / 2, previewTop() + 8, MUTED, false);
        if (kind == Cosmetics.Kind.EMOTE) {
            String line = item == null ? "None" : item.name();
            ctx.drawText(tr, Text.literal(line), midX - tr.getWidth(line) / 2, midY - 4, TEXT, false);
            String s = "Use it from the emote wheel in game";
            ctx.drawText(tr, Text.literal(s), midX - tr.getWidth(s) / 2, midY + 10, MUTED, false);
            return;
        }
        String glyph = item == null ? "" : Cosmetics.glyph(kind, item.id());
        int w = tr.getWidth(glyph) + tr.getWidth(name + ": gg") + 24;
        int x = midX - w / 2;
        UiDraw.roundRect(ctx, x - 6, midY - 14, w + 12, 28, 10, 0xCC1C1F27);
        int tx = x + 4;
        if (!glyph.isEmpty()) {
            ctx.drawText(tr, Text.literal(glyph), tx, midY - 4, color | 0xFF000000, true);
            tx += tr.getWidth(glyph) + 6;
        }
        ctx.drawText(tr, Text.literal(name + ": gg"), tx, midY - 4, 0xFFF3F4F6, false);
    }

    private void arrow(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, String s) {
        boolean h = in(mx, my, x, y, 22, 22);
        UiDraw.roundRect(ctx, x, y, 22, 22, 11, h ? 0xFFFFFFFF : 0x99FFFFFF);
        UiDraw.roundBorder(ctx, x, y, 22, 22, 11, 0x14000000);
        ctx.drawText(tr, Text.literal(s), x + 9, y + 7, TEXT, false);
    }

    private void drawInfoBar(DrawContext ctx, TextRenderer tr, int mx, int my, Cosmetics.Kind kind) {
        int y = top + height - 40;
        int x = cx + 8;
        int w = cw - 16;
        UiDraw.roundRect(ctx, x, y, w, 32, 10, 0xE6FFFFFF);
        UiDraw.roundBorder(ctx, x, y, w, 32, 10, 0x12000000);
        String eq = Cosmetics.equipped(kind);
        Cosmetics.Item item = "none".equals(eq) ? null : Cosmetics.named(kind, eq);
        ctx.drawText(tr, Text.literal("Equipped"), x + 10, y + 7, MUTED, false);
        ctx.drawText(tr, Text.literal(item == null ? "None" : item.name()), x + 10, y + 19, TEXT, false);
        if (item != null && kind != Cosmetics.Kind.EMOTE) {
            int uw = 64;
            boolean h = in(mx, my, x + w - uw - 6, y + 6, uw, 20);
            UiDraw.pill(ctx, x + w - uw - 6, y + 6, uw, 20, h);
            ctx.drawText(tr, Text.literal("Unequip"), x + w - uw - 6 + (uw - tr.getWidth("Unequip")) / 2, y + 12, TEXT, false);
            if (kind == Cosmetics.Kind.CAPE && CustomCapes.isMine(CustomCapes.find(eq))) {
                int dw = 52;
                int dx = x + w - uw - dw - 12;
                boolean dh = in(mx, my, dx, y + 6, dw, 20);
                UiDraw.pill(ctx, dx, y + 6, dw, 20, dh);
                ctx.drawText(tr, Text.literal("Delete"), dx + (dw - tr.getWidth("Delete")) / 2, y + 12, dh ? 0xFFDC2626 : 0xFFB91C1C, false);
            }
        }
    }

    private int gridTop() {
        return top + (customCapesTab() ? 86 : 60);
    }

    private int cardW() {
        return (rw - 16 - 8) / 2;
    }

    private static final int PUB_BTN_W = 60;

    private int publishFieldW() {
        return rw - 16 - PUB_BTN_W - 6;
    }

    private void drawPublishRow(DrawContext ctx, TextRenderer tr, int mx, int my) {
        int fx = rx + 8;
        int fy = top + 58;
        int fw = publishFieldW();
        UiDraw.field(ctx, fx, fy, fw, 20, publishFocus);
        String hint = publishName.isEmpty() && !publishFocus ? "aero-capes/ file name" : publishName + (publishFocus ? "|" : "");
        ctx.drawText(tr, Text.literal(fit(tr, hint, fw - 12)), fx + 6, fy + 6, publishName.isEmpty() && !publishFocus ? MUTED : TEXT, false);
        int bx = fx + fw + 6;
        boolean busy = CustomCapes.isPublishing();
        boolean hover = !busy && in(mx, my, bx, fy, PUB_BTN_W, 20);
        UiDraw.pill(ctx, bx, fy, PUB_BTN_W, 20, hover);
        String label = busy ? "…" : "Publish";
        ctx.drawText(tr, Text.literal(label), bx + (PUB_BTN_W - tr.getWidth(label)) / 2, fy + 6, hover ? TEXT : MUTED, false);
        if (!CustomCapes.status.isEmpty()) {
            ctx.drawText(tr, Text.literal(fit(tr, CustomCapes.status, rw - 16)), rx + 8, top + height - 12, MUTED, false);
        }
    }

    private void doPublish() {
        CustomCapes.publish(publishName, ok -> {
            if (ok) {
                publishName = "";
            }
        });
    }

    public void submitPublish() {
        if (!CustomCapes.isPublishing()) {
            doPublish();
        }
    }

    /** Filter row: sort, owned only, rarity. {x, w} per pill. */
    private int[][] filterPills() {
        int y0 = rx + 8;
        int w0 = 78;
        int w1 = 52;
        int w2 = rw - 16 - w0 - w1 - 8;
        return new int[][]{{y0, w0}, {y0 + w0 + 4, w1}, {y0 + w0 + w1 + 8, w2}};
    }

    private void drawGrid(DrawContext ctx, TextRenderer tr, int mx, int my, Cosmetics.Kind kind, float t) {
        UiDraw.field(ctx, rx + 8, top + 8, rw - 16, 22, searchFocus);
        String hint = search.isEmpty() && !searchFocus ? "Search " + TAB_NAMES[tab].toLowerCase() : search + (searchFocus ? "|" : "");
        ctx.drawText(tr, Text.literal(fit(tr, hint, rw - 36)), rx + 16, top + 15, search.isEmpty() && !searchFocus ? MUTED : TEXT, false);

        int[][] fp = filterPills();
        String[] labels = {"↕ " + SORTS[sort], "Owned", RARITY_FILTER[rarityFilter]};
        boolean[] on = {false, ownedOnly, rarityFilter > 0};
        for (int i = 0; i < 3; i++) {
            boolean h = in(mx, my, fp[i][0], top + 34, fp[i][1], 18);
            UiDraw.pill(ctx, fp[i][0], top + 34, fp[i][1], 18, on[i] || h);
            String l = fit(tr, labels[i], fp[i][1] - 8);
            int col = i == 2 && rarityFilter > 0 ? Cosmetics.Rarity.values()[rarityFilter - 1].color : on[i] ? TEXT : MUTED;
            ctx.drawText(tr, Text.literal(l), fp[i][0] + (fp[i][1] - tr.getWidth(l)) / 2, top + 39, col, false);
        }
        if (customCapesTab()) {
            drawPublishRow(ctx, tr, mx, my);
        }

        List<Cosmetics.Item> items = gridItems(kind);
        int gy = gridTop();
        int ch = cardH(kind);
        int cwd = cardW();
        int gh = top + height - 8 - gy;
        int rows = (items.size() + 1) / 2;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows * (ch + 8) - gh)));
        String eq = Cosmetics.equipped(kind);
        boolean locked = Cosmetics.locked(kind);
        ctx.enableScissor(rx + 4, gy, rx + rw - 4, gy + gh);
        try {
            for (int i = 0; i < items.size(); i++) {
                Cosmetics.Item item = items.get(i);
                int x = rx + 8 + (i % 2) * (cwd + 8);
                int y = gy + (i / 2) * (ch + 8) - scroll;
                if (y + ch < gy || y > gy + gh) {
                    continue;
                }
                boolean none = "none".equals(item.id());
                boolean sel = eq.equals(item.id());
                boolean owned = Cosmetics.owns(item);
                boolean hover = in(mx, my, x, y, cwd, ch) && my >= gy && my < gy + gh && pendingBuy == null;
                UiDraw.roundRect(ctx, x, y, cwd, ch, 12, sel ? UiDraw.withAlpha(UiDraw.accent(), 0x30) : hover ? 0xFFFFFFFF : UiDraw.surface(0xFFFFFF));
                UiDraw.roundBorder(ctx, x, y, cwd, ch, 12, !sel && hover ? UiDraw.withAlpha(item.rarity().color, 0x99) : 0x10000000);
                int vi = Cosmetics.variantIndex(kind, item.id());
                int color = Cosmetics.variantColor(item, vi);
                int iconH = ch - (Cosmetics.hasVariants(kind) ? 44 : 30);
                if (!none) {
                    ctx.enableScissor(Math.max(x, rx + 4), Math.max(y + 2, gy), Math.min(x + cwd, rx + rw - 4), Math.min(y + iconH, gy + gh));
                    try {
                        CosmeticIcons.draw(ctx, kind, item, color, x, y, cwd, iconH, t);
                    } finally {
                        ctx.disableScissor();
                    }
                }
                if (Cosmetics.hasVariants(kind) && !none && !locked) {
                    int dots = Cosmetics.variantCount();
                    int dx = x + (cwd - (dots * 12 - 4)) / 2;
                    for (int d = 0; d < dots; d++) {
                        int dc = Cosmetics.variantColor(item, d);
                        UiDraw.roundRect(ctx, dx + d * 12, y + ch - 38, 8, 8, 4, dc);
                        if (d == vi) {
                            UiDraw.roundBorder(ctx, dx + d * 12 - 1, y + ch - 39, 10, 10, 5, UiDraw.accent());
                        }
                    }
                }
                String nm = fit(tr, item.name(), cwd - 10);
                ctx.drawText(tr, Text.literal(nm), x + (cwd - tr.getWidth(nm)) / 2, y + ch - 24, TEXT, false);
                // Bottom line: rarity, and price / owned / lock
                if (!none) {
                    String status;
                    int scol;
                    if (locked) {
                        status = "Locked";
                        scol = 0xFFA0A6B4;
                    } else if (sel) {
                        status = "Equipped";
                        scol = UiDraw.accent();
                    } else if (owned && Shards.shopMissing()) {
                        status = "Free";
                        scol = 0xFF16A34A;
                    } else if (owned) {
                        status = "Owned";
                        scol = 0xFF16A34A;
                    } else if (item.catalogId() != null && Shards.freeBetaCape(item.catalogId())) {
                        status = "Free";
                        scol = 0xFF16A34A;
                    } else {
                        int p = priceOf(item);
                        status = p >= 0 ? "◆ " + p : "Not sold";
                        scol = TEXT;
                    }
                    String rl = item.rarity().label;
                    boolean showRarity = item.kind() == Cosmetics.Kind.CAPE || item.kind() == Cosmetics.Kind.BADGE;
                    if (showRarity) {
                        ctx.drawText(tr, Text.literal(rl), x + 6, y + ch - 12, item.rarity().color, false);
                        ctx.drawText(tr, Text.literal(status), x + cwd - 6 - tr.getWidth(status), y + ch - 12, scol, false);
                    } else {
                        ctx.drawText(tr, Text.literal(status), x + (cwd - tr.getWidth(status)) / 2, y + ch - 12, scol, false);
                    }
                    if (locked) {
                        drawLock(ctx, x + cwd - 14, y + 6, 0xFFA0A6B4);
                    }
                }
            }
            if (items.isEmpty()) {
                ctx.drawText(tr, Text.literal(ownedOnly ? "You don't own anything here yet." : "Nothing found."), rx + 12, gy + 8, MUTED, false);
            }
        } finally {
            ctx.disableScissor();
        }
        if (!Shards.status.isEmpty() && !customCapesTab() && pendingBuy == null) {
            ctx.drawText(tr, Text.literal(fit(tr, Shards.status, rw - 16)), rx + 8, top + height - 12, MUTED, false);
        }
    }

    private Cosmetics.Item hoveredItem(Cosmetics.Kind kind, int mx, int my) {
        List<Cosmetics.Item> items = gridItems(kind);
        int gy = gridTop();
        int ch = cardH(kind);
        int cwd = cardW();
        int gh = top + height - 8 - gy;
        if (my < gy || my >= gy + gh) {
            return null;
        }
        for (int i = 0; i < items.size(); i++) {
            int x = rx + 8 + (i % 2) * (cwd + 8);
            int y = gy + (i / 2) * (ch + 8) - scroll;
            if (in(mx, my, x, y, cwd, ch)) {
                return items.get(i);
            }
        }
        return null;
    }

    // ---- buy dialog -------------------------------------------------------------------------

    private int[] dialogRect(int x, int y, int w, int h) {
        int dw = Math.min(340, w - 40);
        int dh = Math.min(190, h - 20);
        return new int[]{x + (w - dw) / 2, y + (h - dh) / 2, dw, dh};
    }

    private void drawBuyDialog(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, int h) {
        ctx.fill(x + 4, y, x + w - 4, y + h - 4, 0x55E9EDF4);
        int[] r = dialogRect(x, y, w, h);
        UiDraw.glass(ctx, r[0], r[1], r[2], r[3], 0, 16);
        Cosmetics.Item item = pendingBuy;
        int pvW = 120;
        UiDraw.roundRect(ctx, r[0] + 10, r[1] + 10, pvW, r[3] - 20, 12, 0x99FFFFFF);
        ctx.enableScissor(r[0] + 10, r[1] + 10, r[0] + 10 + pvW, r[1] + r[3] - 10);
        try {
            CosmeticPreview.showSelf(ctx, r[0] + 10, r[1] + 10, r[0] + 10 + pvW, r[1] + r[3] - 10, (r[3] - 20) * 0.34f,
                    (item.kind() == Cosmetics.Kind.CAPE ? 20f : 200f) + (float) Math.sin(System.currentTimeMillis() / 900.0) * 25f);
        } finally {
            ctx.disableScissor();
        }
        int tx = r[0] + pvW + 22;
        int tw = r[2] - pvW - 32;
        ctx.drawText(tr, Text.literal(item.name()), tx, r[1] + 16, TEXT, false);
        Cosmetics.Rarity rar = item.rarity();
        int rw2 = tr.getWidth(rar.label) + 12;
        UiDraw.roundRect(ctx, tx, r[1] + 28, rw2, 14, 7, UiDraw.withAlpha(rar.color, 0x26));
        ctx.drawText(tr, Text.literal(rar.label), tx + 6, r[1] + 31, rar.color, false);

        String cid = item.catalogId();
        boolean free = cid != null && Shards.freeBetaCape(cid);
        int price = free ? 0 : priceOf(item);
        int base = item.rarity().price;
        int bal = Shards.balance();
        int ly = r[1] + 54;
        ctx.drawText(tr, Text.literal("Price"), tx, ly, MUTED, false);
        String ps = free ? "Free beta cape" : "◆ " + price;
        ctx.drawText(tr, Text.literal(ps), tx + tw - tr.getWidth(ps), ly, TEXT, false);
        if (!free && price >= 0 && price < base) {
            String bs = "◆ " + base;
            int bx = tx + tw - tr.getWidth(ps) - 8 - tr.getWidth(bs);
            ctx.drawText(tr, Text.literal(bs), bx, ly, MUTED, false);
            ctx.fill(bx, ly + 4, bx + tr.getWidth(bs), ly + 5, MUTED);
        }
        ctx.drawText(tr, Text.literal("Your balance"), tx, ly + 14, MUTED, false);
        String bls = Shards.ready() ? "◆ " + bal : "…";
        ctx.drawText(tr, Text.literal(bls), tx + tw - tr.getWidth(bls), ly + 14, TEXT, false);
        UiDraw.divider(ctx, tx, ly + 27, tw);
        ctx.drawText(tr, Text.literal("After"), tx, ly + 33, MUTED, false);
        int after = bal - Math.max(0, price);
        String as = Shards.ready() ? "◆ " + after : "…";
        ctx.drawText(tr, Text.literal(as), tx + tw - tr.getWidth(as), ly + 33, after < 0 ? 0xFFDC2626 : TEXT, false);
        if (free) {
            String fl = "You can pick one of Blossom, Samurai or Dragon for free.";
            ctx.drawText(tr, Text.literal(fit(tr, fl, tw)), tx, ly + 48, 0xFF16A34A, false);
        }

        boolean afford = Shards.ready() && (free || (price >= 0 && after >= 0));
        int by = r[1] + r[3] - 30;
        int bw = (tw - 6) / 2;
        boolean ch = in(mx, my, tx, by, bw, 20);
        UiDraw.pill(ctx, tx, by, bw, 20, ch);
        ctx.drawText(tr, Text.literal("Cancel"), tx + (bw - tr.getWidth("Cancel")) / 2, by + 6, TEXT, false);
        int bx2 = tx + bw + 6;
        boolean bh = afford && !buying && in(mx, my, bx2, by, bw, 20);
        UiDraw.roundRect(ctx, bx2, by, bw, 20, 10, afford ? (bh ? UiDraw.accent() : UiDraw.withAlpha(UiDraw.accent(), 0xDD)) : 0xFFD5DAE4);
        String bl = buying ? "…" : !Shards.ready() ? "Not connected" : afford ? (free ? "Claim free" : "Buy") : "Not enough";
        ctx.drawText(tr, Text.literal(bl), bx2 + (bw - tr.getWidth(bl)) / 2, by + 6, afford ? 0xFFFFFFFF : MUTED, false);
    }

    private boolean clickBuyDialog(int mx, int my, int x, int y, int w, int h) {
        int[] r = dialogRect(x, y, w, h);
        int tx = r[0] + 120 + 22;
        int tw = r[2] - 120 - 32;
        int by = r[1] + r[3] - 30;
        int bw = (tw - 6) / 2;
        if (in(mx, my, tx, by, bw, 20) || !in(mx, my, r[0], r[1], r[2], r[3])) {
            pendingBuy = null;
            return true;
        }
        if (in(mx, my, tx + bw + 6, by, bw, 20) && !buying) {
            Cosmetics.Item item = pendingBuy;
            String cid = item.catalogId();
            boolean free = Shards.freeBetaCape(cid);
            int price = free ? 0 : priceOf(item);
            if (!Shards.ready() || (!free && Shards.balance() < price)) {
                return true;
            }
            buying = true;
            Shards.buy(cid, free ? -1 : price, ok -> {
                buying = false;
                if (ok) {
                    Cosmetics.equip(item.kind(), item.id());
                    pendingBuy = null;
                }
            });
        }
        return true;
    }

    private static String fit(TextRenderer tr, String s, int maxW) {
        if (tr.getWidth(s) <= maxW) {
            return s;
        }
        String cut = s;
        while (!cut.isEmpty() && tr.getWidth(cut + "…") > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    // ---- input --------------------------------------------------------------------------------

    public boolean click(int mx, int my, int x, int y, int w, int h) {
        layout(x, y, w, h);
        Cosmetics.Kind kind = kind();
        searchFocus = false;
        publishFocus = false;
        if (pendingBuy != null) {
            return clickBuyDialog(mx, my, x, y, w, h);
        }

        if (customCapesTab()) {
            int fx = rx + 8;
            int fy = top + 58;
            int fw = publishFieldW();
            if (in(mx, my, fx, fy, fw, 20)) {
                publishFocus = true;
                return true;
            }
            if (!CustomCapes.isPublishing() && in(mx, my, fx + fw + 6, fy, PUB_BTN_W, 20)) {
                doPublish();
                return true;
            }
        }

        if (in(mx, my, btnX(1), btnY(), BTN_W, BTN_H)) {
            if (AeroClient.CONFIG != null) {
                AeroClient.CONFIG.showOthersCosmetics = !AeroClient.CONFIG.showOthersCosmetics;
                AeroClient.CONFIG.save();
            }
            return true;
        }
        int ty = top + 38;
        for (int value : TAB_ORDER) {
            if (value == 1) {
                ty += 5;
            }
            if (in(mx, my, lx + 6, ty, lw - 12, step - 2)) {
                tab = value;
                yaw = value == 0 || value == TAB_CUSTOM_CAPES ? 20f : 200f;
                scroll = 0;
                search = "";
                publishName = "";
                return true;
            }
            ty += step;
        }

        int[][] fp = filterPills();
        if (in(mx, my, fp[0][0], top + 34, fp[0][1], 18)) {
            sort = (sort + 1) % SORTS.length;
            return true;
        }
        if (in(mx, my, fp[1][0], top + 34, fp[1][1], 18)) {
            ownedOnly = !ownedOnly;
            scroll = 0;
            return true;
        }
        if (in(mx, my, fp[2][0], top + 34, fp[2][1], 18)) {
            rarityFilter = (rarityFilter + 1) % RARITY_FILTER.length;
            scroll = 0;
            return true;
        }

        int py1 = previewTop();
        int py2 = previewBottom();
        if (modelKind(kind)) {
            int midY = (py1 + py2) / 2;
            if (in(mx, my, cx + 12, midY - 11, 22, 22)) {
                yaw -= 45f;
                return true;
            }
            if (in(mx, my, cx + cw - 34, midY - 11, 22, 22)) {
                yaw += 45f;
                return true;
            }
            if (in(mx, my, cx + 8, py1, cw - 16, py2 - py1)) {
                dragging = true;
                return true;
            }
        }

        String eq = Cosmetics.equipped(kind);
        int ix = cx + 8;
        int iy = top + height - 40;
        int uw = 64;
        if (!"none".equals(eq) && kind != Cosmetics.Kind.EMOTE) {
            if (in(mx, my, ix + (cw - 16) - uw - 6, iy + 6, uw, 20)) {
                Cosmetics.equip(kind, "none");
                return true;
            }
            CustomCapes.Entry mine = kind == Cosmetics.Kind.CAPE ? CustomCapes.find(eq) : null;
            if (CustomCapes.isMine(mine)) {
                int dw = 52;
                int dx = ix + (cw - 16) - uw - dw - 12;
                if (in(mx, my, dx, iy + 6, dw, 20)) {
                    CustomCapes.delete(mine.id(), () -> {});
                    Cosmetics.equip(kind, "none");
                    return true;
                }
            }
        }

        if (in(mx, my, rx + 8, top + 8, rw - 16, 22)) {
            searchFocus = true;
            return true;
        }

        List<Cosmetics.Item> items = gridItems(kind);
        int gy = gridTop();
        int ch = cardH(kind);
        int cwd = cardW();
        int gh = top + height - 8 - gy;
        if (my < gy || my >= gy + gh) {
            return true;
        }
        for (int i = 0; i < items.size(); i++) {
            int cxp = rx + 8 + (i % 2) * (cwd + 8);
            int cyp = gy + (i / 2) * (ch + 8) - scroll;
            if (!in(mx, my, cxp, cyp, cwd, ch)) {
                continue;
            }
            Cosmetics.Item item = items.get(i);
            if (Cosmetics.locked(kind) && !"none".equals(item.id())) {
                Shards.status = TAB_NAMES[tab] + " are locked for now";
                return true;
            }
            if (!Cosmetics.owns(item)) {
                if (item.catalogId() != null) {
                    pendingBuy = item;
                    Shards.status = "";
                    Shards.refresh(true);
                } else {
                    Shards.status = item.id().startsWith("rank_") ? "Only for that rank" : "Not available";
                }
                return true;
            }
            if (Cosmetics.hasVariants(kind) && !"none".equals(item.id())) {
                int dots = Cosmetics.variantCount();
                int dx = cxp + (cwd - (dots * 12 - 4)) / 2;
                for (int d = 0; d < dots; d++) {
                    if (in(mx, my, dx + d * 12 - 2, cyp + ch - 40, 12, 12)) {
                        Cosmetics.setVariant(kind, item.id(), d);
                        Cosmetics.equip(kind, item.id());
                        return true;
                    }
                }
            }
            Cosmetics.equip(kind, item.id());
            return true;
        }
        return true;
    }

    public boolean drag(double dx) {
        if (!dragging) {
            return false;
        }
        yaw += (float) dx * 1.1f;
        return true;
    }

    public void release() {
        dragging = false;
    }

    public boolean scroll(int mx, int my, double amount) {
        if (pendingBuy != null) {
            return true;
        }
        if (in(mx, my, rx, top, rw, height)) {
            scroll = (int) Math.max(0, scroll - amount * 24);
            return true;
        }
        if (in(mx, my, cx, top, cw, height)) {
            zoom = Math.max(0.6f, Math.min(2.2f, zoom + (float) amount * 0.1f));
            return true;
        }
        return false;
    }
}
