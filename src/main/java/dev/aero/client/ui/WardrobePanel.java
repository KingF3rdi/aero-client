package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.auth.AccountManager;
import dev.aero.client.auth.SavedAccount;
import dev.aero.client.auth.SkinPreview;
import dev.aero.client.cosmetic.CosmeticPreview;
import dev.aero.client.cosmetic.Cosmetics;
import dev.aero.client.cosmetic.CubeDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/** Wardrobe tab laid out like a full-screen cosmetics page: categories left, live preview center, item grid right. */
public final class WardrobePanel {
    private static final int TEXT = 0xFFF3F0F8;
    private static final int MUTED = 0xFF8E889C;
    private static final int ACCENT = 0xFF4F8EFF;
    private static final int PAD = 8;

    private static final String[] TAB_NAMES = {"Capes", "Wings", "Headwear", "Trails", "Kill", "Mace", "Pets", "Emotes", "Chat tags", "Badges"};
    private static final int[] TAB_ORDER = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    public String search = "";
    public boolean searchFocus;
    private int tab;
    private int scroll;
    private float yaw = 200f;
    private float zoom = 1f;
    private boolean dragging;

    // layout, recomputed by layout()
    private int lx, lw, cx, cw, rx, rw, top, height;

    private void layout(int x, int y, int w, int h) {
        top = y + PAD;
        height = h - PAD * 2;
        lx = x + PAD;
        lw = Math.min(150, w * 19 / 100);
        rw = Math.min(310, w * 36 / 100);
        rx = x + w - PAD - rw;
        cx = lx + lw + PAD;
        cw = rx - PAD - cx;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private Cosmetics.Kind kind() {
        return Cosmetics.kindForTab(tab);
    }

    private int previewTop() {
        return top + 28;
    }

    private int previewBottom() {
        return top + height - 56;
    }

    private boolean modelKind(Cosmetics.Kind k) {
        return k != Cosmetics.Kind.TAG && k != Cosmetics.Kind.BADGE && k != Cosmetics.Kind.EMOTE;
    }

    private int cardH(Cosmetics.Kind k) {
        return Cosmetics.hasVariants(k) ? 98 : 78;
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

        drawLeft(ctx, tr, mx, my);

        UiDraw.innerCard(ctx, cx, top, cw, height);
        ctx.drawText(tr, Text.literal(TAB_NAMES[tab]), cx + 12, top + 10, TEXT, false);
        drawPreview(ctx, tr, mx, my, kind, t);
        drawInfoBar(ctx, tr, mx, my, kind);

        UiDraw.innerCard(ctx, rx, top, rw, height);
        drawGrid(ctx, tr, mx, my, kind, t);
    }

    private void drawLeft(DrawContext ctx, TextRenderer tr, int mx, int my) {
        UiDraw.innerCard(ctx, lx, top, lw, height);
        SkinPreview.requestOwn();
        String key = ownSkinKey();
        int hx = lx + 8;
        int hy = top + 8;
        if (SkinPreview.ready(key)) {
            SkinPreview.drawHead(ctx, key, hx, hy, 24);
        } else {
            UiDraw.roundRect(ctx, hx, hy, 24, 24, 6, 0xFFC8A0E8);
        }
        String name = AccountManager.currentName();
        ctx.drawText(tr, Text.literal(fit(tr, name, lw - 44)), hx + 30, hy + 3, TEXT, false);
        ctx.drawText(tr, Text.literal("Profile"), hx + 30, hy + 14, MUTED, false);

        int y = top + 44;
        for (int i = 0; i < TAB_ORDER.length; i++) {
            if (i == 8) {
                ctx.fill(lx + 12, y + 2, lx + lw - 12, y + 3, 0x22FFFFFF);
                y += 8;
            }
            boolean on = tab == TAB_ORDER[i];
            boolean hover = in(mx, my, lx + 6, y, lw - 12, 22);
            if (on) {
                UiDraw.pill(ctx, lx + 6, y, lw - 12, 22, true);
            } else if (hover) {
                UiDraw.roundRect(ctx, lx + 6, y, lw - 12, 22, 11, 0x14FFFFFF);
            }
            ctx.drawText(tr, Text.literal(TAB_NAMES[TAB_ORDER[i]]), lx + 16, y + 7, on || hover ? TEXT : MUTED, false);
            y += 26;
        }
    }

    private void drawPreview(DrawContext ctx, TextRenderer tr, int mx, int my, Cosmetics.Kind kind, float t) {
        int px1 = cx + 8;
        int px2 = cx + cw - 8;
        int py1 = previewTop();
        int py2 = previewBottom();
        String equipped = Cosmetics.equipped(kind);
        Cosmetics.Item item = "none".equals(equipped) ? null : Cosmetics.named(kind, equipped);
        int color = Cosmetics.equippedColor(kind);
        int midX = (px1 + px2) / 2;

        if (modelKind(kind)) {
            var player = MinecraftClient.getInstance().player;
            ctx.enableScissor(px1, py1, px2, py2);
            try {
                float scale = (py2 - py1) * 0.34f * zoom;
                CosmeticPreview.show(ctx, px1, py1, px2, py2, scale, yaw, player);
                if (item != null) {
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
            if (player == null) {
                ctx.drawText(tr, Text.literal("Join a world to see the preview"), midX - 70, (py1 + py2) / 2, MUTED, false);
            }
            arrow(ctx, tr, mx, my, px1 + 4, (py1 + py2) / 2 - 11, "<");
            arrow(ctx, tr, mx, my, px2 - 26, (py1 + py2) / 2 - 11, ">");
            ctx.drawCenteredTextWithShadow(tr, Text.literal("Drag to rotate, scroll to zoom"), midX, top + height - 14, MUTED);
        } else {
            drawChatPreview(ctx, tr, kind, item, color, midX, (py1 + py2) / 2);
        }
    }

    private void drawChatPreview(DrawContext ctx, TextRenderer tr, Cosmetics.Kind kind, Cosmetics.Item item, int color, int midX, int midY) {
        String name = AccountManager.currentName();
        String head = kind == Cosmetics.Kind.BADGE ? "On your name" : kind == Cosmetics.Kind.TAG ? "In chat" : "Emote wheel";
        ctx.drawCenteredTextWithShadow(tr, Text.literal(head), midX, previewTop() + 8, MUTED);
        if (kind == Cosmetics.Kind.EMOTE) {
            String line = item == null ? "None" : item.name();
            ctx.drawCenteredTextWithShadow(tr, Text.literal(line), midX, midY - 4, TEXT);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("Use it from the emote wheel in game"), midX, midY + 10, MUTED);
            return;
        }
        String glyph = item == null ? "" : Cosmetics.glyph(kind, item.id());
        if (kind == Cosmetics.Kind.BADGE && item == null) {
            glyph = "A";
            color = ACCENT;
        }
        int w = tr.getWidth(glyph) + tr.getWidth(name + ": gg") + 24;
        int x = midX - w / 2;
        UiDraw.roundRect(ctx, x - 6, midY - 14, w + 12, 28, 10, 0x99060810);
        int tx = x + 4;
        if (!glyph.isEmpty()) {
            ctx.drawText(tr, Text.literal(glyph), tx, midY - 4, color | 0xFF000000, true);
            tx += tr.getWidth(glyph) + 6;
        }
        ctx.drawText(tr, Text.literal(name + ": gg"), tx, midY - 4, TEXT, false);
    }

    private void arrow(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, String s) {
        boolean h = in(mx, my, x, y, 22, 22);
        UiDraw.roundRect(ctx, x, y, 22, 22, 11, h ? 0x66FFFFFF : 0x33FFFFFF);
        ctx.drawText(tr, Text.literal(s), x + 8, y + 7, TEXT, false);
    }

    private void drawInfoBar(DrawContext ctx, TextRenderer tr, int mx, int my, Cosmetics.Kind kind) {
        int y = top + height - 52;
        int x = cx + 8;
        int w = cw - 16;
        UiDraw.roundRect(ctx, x, y, w, 34, 10, 0xAA05060C);
        String eq = Cosmetics.equipped(kind);
        Cosmetics.Item item = "none".equals(eq) ? null : Cosmetics.named(kind, eq);
        ctx.drawText(tr, Text.literal(TAB_NAMES[tab]), x + 10, y + 7, MUTED, false);
        ctx.drawText(tr, Text.literal(item == null ? "None" : item.name()), x + 10, y + 19, TEXT, false);
        if (item != null && kind != Cosmetics.Kind.EMOTE) {
            boolean h = in(mx, my, x + w - 80, y + 7, 70, 20);
            UiDraw.pill(ctx, x + w - 80, y + 7, 70, 20, h);
            ctx.drawText(tr, Text.literal("Unequip"), x + w - 80 + (70 - tr.getWidth("Unequip")) / 2, y + 13, TEXT, false);
        }
    }

    private int gridTop() {
        return top + 36;
    }

    private int cardW() {
        return (rw - 16 - 8) / 2;
    }

    private void drawGrid(DrawContext ctx, TextRenderer tr, int mx, int my, Cosmetics.Kind kind, float t) {
        UiDraw.field(ctx, rx + 8, top + 8, rw - 16, 22, searchFocus);
        String hint = search.isEmpty() && !searchFocus ? "Search " + TAB_NAMES[tab].toLowerCase() : search + (searchFocus ? "|" : "");
        ctx.drawText(tr, Text.literal(fit(tr, hint, rw - 36)), rx + 16, top + 15, search.isEmpty() && !searchFocus ? MUTED : TEXT, false);

        List<Cosmetics.Item> items = Cosmetics.of(kind, search);
        int gy = gridTop();
        int ch = cardH(kind);
        int cwd = cardW();
        int gh = top + height - 8 - gy;
        int rows = (items.size() + 1) / 2;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows * (ch + 8) - gh)));
        String eq = Cosmetics.equipped(kind);
        ctx.enableScissor(rx + 4, gy, rx + rw - 4, gy + gh);
        try {
            for (int i = 0; i < items.size(); i++) {
                Cosmetics.Item item = items.get(i);
                int x = rx + 8 + (i % 2) * (cwd + 8);
                int y = gy + (i / 2) * (ch + 8) - scroll;
                if (y + ch < gy || y > gy + gh) {
                    continue;
                }
                boolean sel = eq.equals(item.id());
                boolean hover = in(mx, my, x, y, cwd, ch) && my >= gy && my < gy + gh;
                UiDraw.roundRect(ctx, x, y, cwd, ch, 12, sel ? 0x664F8EFF : hover ? 0x40FFFFFF : 0x30FFFFFF);
                if (sel) {
                    UiDraw.roundBorder(ctx, x, y, cwd, ch, 12, 0xAA4F8EFF);
                }
                int vi = Cosmetics.variantIndex(kind, item.id());
                int color = Cosmetics.variantColor(item, vi);
                boolean none = "none".equals(item.id());
                int iconH = ch - (Cosmetics.hasVariants(kind) ? 38 : 22);
                if (!none) {
                    ctx.enableScissor(Math.max(x, rx + 4), Math.max(y + 2, gy), Math.min(x + cwd, rx + rw - 4), Math.min(y + iconH, gy + gh));
                    try {
                        CosmeticIcons.draw(ctx, kind, item, color, x, y, cwd, iconH, t);
                    } finally {
                        ctx.disableScissor();
                    }
                }
                if (Cosmetics.hasVariants(kind) && !none) {
                    int dots = Cosmetics.variantCount();
                    int dx = x + (cwd - (dots * 12 - 4)) / 2;
                    for (int d = 0; d < dots; d++) {
                        int dc = Cosmetics.variantColor(item, d);
                        UiDraw.roundRect(ctx, dx + d * 12, y + ch - 30, 8, 8, 4, dc);
                        if (d == vi) {
                            UiDraw.roundBorder(ctx, dx + d * 12 - 1, y + ch - 31, 10, 10, 5, 0xFFFFFFFF);
                        }
                    }
                }
                ctx.drawCenteredTextWithShadow(tr, Text.literal(fit(tr, item.name(), cwd - 8)), x + cwd / 2, y + ch - 14, sel || hover ? TEXT : 0xFFD0CCDA);
            }
            if (items.isEmpty()) {
                ctx.drawText(tr, Text.literal("Nothing found."), rx + 12, gy + 8, MUTED, false);
            }
        } finally {
            ctx.disableScissor();
        }
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

        int ty = top + 44;
        for (int i = 0; i < TAB_ORDER.length; i++) {
            if (i == 8) {
                ty += 8;
            }
            if (in(mx, my, lx + 6, ty, lw - 12, 22)) {
                tab = TAB_ORDER[i];
                scroll = 0;
                search = "";
                return true;
            }
            ty += 26;
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
        int iy = top + height - 52;
        if (!"none".equals(eq) && kind != Cosmetics.Kind.EMOTE && in(mx, my, ix + (cw - 16) - 80, iy + 7, 70, 20)) {
            Cosmetics.equip(kind, "none");
            return true;
        }

        if (in(mx, my, rx + 8, top + 8, rw - 16, 22)) {
            searchFocus = true;
            return true;
        }

        List<Cosmetics.Item> items = Cosmetics.of(kind, search);
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
            if (Cosmetics.hasVariants(kind) && !"none".equals(item.id())) {
                int dots = Cosmetics.variantCount();
                int dx = cxp + (cwd - (dots * 12 - 4)) / 2;
                for (int d = 0; d < dots; d++) {
                    if (in(mx, my, dx + d * 12 - 2, cyp + ch - 32, 12, 12)) {
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
