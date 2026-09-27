package dev.aero.client.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aero.client.cosmetic.Cosmetics;
import dev.aero.client.social.Shards;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

/** Store, Rewards and Badges pages of the You tab. Every number comes from the Aero server (Shards). */
public final class ShopPanel {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final int GREEN = 0xFF16A34A;

    private int scroll;
    private boolean busy;
    /** Item the player clicked to buy; the menu opens the wardrobe's buy dialog for it. */
    private Cosmetics.Item requestBuy;

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public Cosmetics.Item takeBuyRequest() {
        Cosmetics.Item r = requestBuy;
        requestBuy = null;
        return r;
    }

    public void scroll(double amount) {
        scroll = (int) Math.max(0, scroll - amount * 16);
    }

    private static Cosmetics.Item itemFor(String catalogId) {
        if (catalogId.startsWith("cape:")) {
            return Cosmetics.named(Cosmetics.Kind.CAPE, catalogId.substring(5));
        }
        if (catalogId.startsWith("badge:")) {
            return Cosmetics.named(Cosmetics.Kind.BADGE, catalogId.substring(6));
        }
        return null;
    }

    private static String until(long at) {
        long ms = Math.max(0, at - System.currentTimeMillis());
        long h = ms / 3_600_000L;
        long d = h / 24;
        long m = (ms / 60_000L) % 60;
        return d > 0 ? d + "d " + (h % 24) + "h" : h > 0 ? h + "h " + m + "m" : m + "m";
    }

    private static String ago(long at) {
        long s = Math.max(0, (System.currentTimeMillis() - at) / 1000);
        if (s < 60) {
            return "just now";
        }
        if (s < 3600) {
            return (s / 60) + "m ago";
        }
        if (s < 86400) {
            return (s / 3600) + "h ago";
        }
        return (s / 86400) + "d ago";
    }

    public void render(DrawContext ctx, int mx, int my, int x, int y, int w, int h, int page) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (page == 1) {
            drawStore(ctx, tr, mx, my, x, y, w, h);
        } else if (page == 2) {
            drawRewards(ctx, tr, mx, my, x, y, w, h);
        } else {
            drawBadges(ctx, tr, mx, my, x, y, w, h);
        }
        if (!Shards.status.isEmpty()) {
            ctx.drawText(tr, Text.literal(Shards.status), x + w - 8 - tr.getWidth(Shards.status), y + h + 2, MUTED, false);
        }
    }

    public boolean click(int mx, int my, int x, int y, int w, int h, int page) {
        if (page == 1) {
            return clickStore(mx, my, x, y, w, h);
        }
        if (page == 2) {
            return clickRewards(mx, my, x, y, w, h);
        }
        return clickBadges(mx, my, x, y, w, h);
    }

    // ---- Store ------------------------------------------------------------------------------

    private void priceTag(DrawContext ctx, TextRenderer tr, Shards.Offer o, int rightX, int y, boolean owned) {
        if (owned) {
            ctx.drawText(tr, Text.literal("Owned"), rightX - tr.getWidth("Owned"), y, GREEN, false);
            return;
        }
        if (Shards.freeBetaCape(o.id())) {
            ctx.drawText(tr, Text.literal("Free"), rightX - tr.getWidth("Free"), y, GREEN, false);
            return;
        }
        String p = "◆ " + o.price();
        ctx.drawText(tr, Text.literal(p), rightX - tr.getWidth(p), y, TEXT, false);
        if (o.price() < o.base()) {
            String b = String.valueOf(o.base());
            int bx = rightX - tr.getWidth(p) - 6 - tr.getWidth(b);
            ctx.drawText(tr, Text.literal(b), bx, y, MUTED, false);
            ctx.fill(bx, y + 4, bx + tr.getWidth(b), y + 5, MUTED);
        }
    }

    private static int rarityColor(String r) {
        return switch (r) {
            case "uncommon" -> Cosmetics.Rarity.UNCOMMON.color;
            case "rare" -> Cosmetics.Rarity.RARE.color;
            case "legendary" -> Cosmetics.Rarity.LEGENDARY.color;
            default -> Cosmetics.Rarity.COMMON.color;
        };
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void offBadge(DrawContext ctx, TextRenderer tr, int x, int y, int off) {
        String s = "-" + off + "%";
        int w = tr.getWidth(s) + 10;
        UiDraw.roundRect(ctx, x, y, w, 14, 7, 0xFFEF4444);
        ctx.drawText(tr, Text.literal(s), x + 5, y + 3, 0xFFFFFFFF, false);
    }

    private void drawStore(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, int h) {
        Shards.Offer f = Shards.featured();
        if (f == null) {
            ctx.drawText(tr, Text.literal(Shards.status.isEmpty() ? "Loading the store…" : Shards.status), x + 8, y + 8, MUTED, false);
            return;
        }
        float t = (System.currentTimeMillis() % 100000L) / 1000f;
        JsonObject store = Shards.store();
        // Featured
        int fw = w * 40 / 100;
        boolean fh = in(mx, my, x, y, fw, h);
        UiDraw.roundRect(ctx, x, y, fw, h, 16, fh ? 0xFFFFFFFF : 0xD0FFFFFF);
        UiDraw.roundBorder(ctx, x, y, fw, h, 16, UiDraw.withAlpha(rarityColor(f.rarity()), fh ? 0xAA : 0x55));
        ctx.fillGradient(x + 8, y + 6, x + fw - 8, y + h / 2, UiDraw.withAlpha(rarityColor(f.rarity()), 0x22), 0x00FFFFFF);
        ctx.drawText(tr, Text.literal("FEATURED THIS WEEK"), x + 12, y + 10, rarityColor(f.rarity()), false);
        String ends = "Ends in " + until(store.get("weekEndsAt").getAsLong());
        ctx.drawText(tr, Text.literal(ends), x + fw - 12 - tr.getWidth(ends), y + 10, MUTED, false);
        Cosmetics.Item fi = itemFor(f.id());
        if (fi != null) {
            CosmeticIcons.draw(ctx, fi.kind(), fi, fi.color(), x + 10, y + 24, fw - 20, h - 90, t);
        }
        ctx.drawText(tr, Text.literal(f.name()), x + 12, y + h - 58, TEXT, false);
        ctx.drawText(tr, Text.literal(cap(f.rarity()) + " cape"), x + 12, y + h - 46, rarityColor(f.rarity()), false);
        offBadge(ctx, tr, x + 12 + tr.getWidth(f.name()) + 6, y + h - 61, f.off());
        priceTag(ctx, tr, f, x + fw - 12, y + h - 52, Shards.owns(f.id()));
        boolean owned = Shards.owns(f.id());
        boolean bh = !owned && in(mx, my, x + 12, y + h - 30, fw - 24, 20);
        UiDraw.roundRect(ctx, x + 12, y + h - 30, fw - 24, 20, 10, owned ? 0xFFE3F5E9 : bh ? UiDraw.accent() : UiDraw.withAlpha(UiDraw.accent(), 0xDD));
        String bl = owned ? "In your wardrobe" : "Preview & buy";
        ctx.drawText(tr, Text.literal(bl), x + 12 + (fw - 24 - tr.getWidth(bl)) / 2, y + h - 24, owned ? GREEN : 0xFFFFFFFF, false);

        // Daily picks
        int rx = x + fw + 10;
        int rw = w - fw - 10;
        ctx.drawText(tr, Text.literal("Daily picks"), rx + 4, y + 2, TEXT, false);
        String refresh = "New picks in " + until(store.get("dayEndsAt").getAsLong());
        ctx.drawText(tr, Text.literal(refresh), rx + rw - 4 - tr.getWidth(refresh), y + 2, MUTED, false);
        List<Shards.Offer> daily = Shards.list("daily");
        int ry = y + 14;
        for (Shards.Offer o : daily) {
            drawOfferRow(ctx, tr, mx, my, o, rx, ry, rw, 40, t, true);
            ry += 44;
        }
        ctx.drawText(tr, Text.literal("New"), rx + 4, ry + 4, TEXT, false);
        ry += 16;
        List<Shards.Offer> newest = Shards.list("newest");
        int tiles = Math.max(1, newest.size());
        int tw = (rw - (tiles - 1) * 6) / tiles;
        int th = y + h - ry;
        for (int i = 0; i < newest.size(); i++) {
            Shards.Offer o = newest.get(i);
            int tx = rx + i * (tw + 6);
            boolean hv = in(mx, my, tx, ry, tw, th);
            UiDraw.roundRect(ctx, tx, ry, tw, th, 10, hv ? 0xFFFFFFFF : 0xB4FFFFFF);
            UiDraw.roundBorder(ctx, tx, ry, tw, th, 10, hv ? UiDraw.withAlpha(rarityColor(o.rarity()), 0x99) : 0x10000000);
            Cosmetics.Item it = itemFor(o.id());
            if (it != null && th > 30) {
                CosmeticIcons.draw(ctx, it.kind(), it, it.color(), tx, ry + 2, tw, th - 24, t);
            }
            String n = fit(tr, o.name(), tw - 6);
            ctx.drawText(tr, Text.literal(n), tx + (tw - tr.getWidth(n)) / 2, ry + th - 20, TEXT, false);
            String p = Shards.owns(o.id()) ? "Owned" : "◆ " + o.price();
            ctx.drawText(tr, Text.literal(p), tx + (tw - tr.getWidth(p)) / 2, ry + th - 10, Shards.owns(o.id()) ? GREEN : MUTED, false);
        }
    }

    private void drawOfferRow(DrawContext ctx, TextRenderer tr, int mx, int my, Shards.Offer o, int x, int y, int w, int h, float t, boolean sale) {
        boolean hv = in(mx, my, x, y, w, h);
        UiDraw.roundRect(ctx, x, y, w, h, 12, hv ? 0xFFFFFFFF : 0xB4FFFFFF);
        UiDraw.roundBorder(ctx, x, y, w, h, 12, hv ? UiDraw.withAlpha(rarityColor(o.rarity()), 0x99) : 0x10000000);
        Cosmetics.Item it = itemFor(o.id());
        if (it != null) {
            ctx.enableScissor(x + 4, y + 2, x + 50, y + h - 2);
            try {
                CosmeticIcons.draw(ctx, it.kind(), it, it.color(), x + 2, y + 1, 48, h - 2, t);
            } finally {
                ctx.disableScissor();
            }
        }
        ctx.drawText(tr, Text.literal(o.name()), x + 56, y + 9, TEXT, false);
        String kind = o.id().startsWith("badge:") ? " badge" : " cape";
        ctx.drawText(tr, Text.literal(cap(o.rarity()) + kind), x + 56, y + 21, rarityColor(o.rarity()), false);
        if (sale && o.off() > 0) {
            offBadge(ctx, tr, x + 56 + tr.getWidth(o.name()) + 6, y + 6, o.off());
        }
        priceTag(ctx, tr, o, x + w - 10, y + 16, Shards.owns(o.id()));
    }

    private boolean clickStore(int mx, int my, int x, int y, int w, int h) {
        Shards.Offer f = Shards.featured();
        if (f == null) {
            return true;
        }
        int fw = w * 40 / 100;
        if (in(mx, my, x, y, fw, h)) {
            buy(f.id());
            return true;
        }
        int rx = x + fw + 10;
        int rw = w - fw - 10;
        int ry = y + 14;
        for (Shards.Offer o : Shards.list("daily")) {
            if (in(mx, my, rx, ry, rw, 40)) {
                buy(o.id());
                return true;
            }
            ry += 44;
        }
        ry += 16;
        List<Shards.Offer> newest = Shards.list("newest");
        int tiles = Math.max(1, newest.size());
        int tw = (rw - (tiles - 1) * 6) / tiles;
        for (int i = 0; i < newest.size(); i++) {
            if (in(mx, my, rx + i * (tw + 6), ry, tw, y + h - ry)) {
                buy(newest.get(i).id());
                return true;
            }
        }
        return true;
    }

    private void buy(String catalogId) {
        Cosmetics.Item it = itemFor(catalogId);
        if (it == null) {
            return;
        }
        if (Shards.owns(catalogId)) {
            Cosmetics.equip(it.kind(), it.id());
            Shards.status = "Equipped " + it.name();
            return;
        }
        requestBuy = it;
    }

    // ---- Rewards ----------------------------------------------------------------------------

    private void drawRewards(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, int h) {
        JsonObject wlt = Shards.wallet();
        if (wlt == null) {
            ctx.drawText(tr, Text.literal(Shards.status.isEmpty() ? "Connecting to the Aero server…" : Shards.status), x + 8, y + 8, MUTED, false);
            return;
        }
        int colW = (w - 16) / 3;
        // Streak
        UiDraw.roundRect(ctx, x, y, colW, h, 14, 0xD0FFFFFF);
        UiDraw.roundBorder(ctx, x, y, colW, h, 14, 0x10000000);
        ctx.drawText(tr, Text.literal("Login streak"), x + 12, y + 10, TEXT, false);
        int streak = wlt.get("streak").getAsInt();
        boolean canClaim = wlt.get("canClaim").getAsBoolean();
        String st = streak + (streak == 1 ? " day" : " days");
        ctx.drawText(tr, Text.literal(st), x + colW - 12 - tr.getWidth(st), y + 10, 0xFFEA580C, false);
        int[] rewards = new int[7];
        for (int i = 0; i < 7; i++) {
            rewards[i] = wlt.getAsJsonArray("streakRewards").get(i).getAsInt();
        }
        int nextDay = wlt.has("nextStreakDay") ? wlt.get("nextStreakDay").getAsInt() : streak + 1;
        int doneDays = canClaim ? nextDay - 1 : Math.min(7, streak);
        int dw = (colW - 24 - 6 * 3) / 7;
        for (int i = 0; i < 7; i++) {
            int dx = x + 12 + i * (dw + 3);
            int dy = y + 30;
            boolean done = i < Math.min(7, doneDays);
            boolean today = canClaim && i == Math.min(7, nextDay) - 1;
            int bg = done ? 0xFFFED7AA : today ? UiDraw.withAlpha(UiDraw.accent(), 0x30) : 0x0C000000;
            UiDraw.roundRect(ctx, dx, dy, dw, 40, 8, bg);
            if (today) {
                UiDraw.roundBorder(ctx, dx, dy, dw, 40, 8, UiDraw.accent());
            }
            String dl = "D" + (i + 1);
            ctx.drawText(tr, Text.literal(dl), dx + (dw - tr.getWidth(dl)) / 2, dy + 5, MUTED, false);
            Shards.drawGem(ctx, dx + (dw - 8) / 2, dy + 16, 8);
            String rl = String.valueOf(rewards[i]);
            ctx.drawText(tr, Text.literal(rl), dx + (dw - tr.getWidth(rl)) / 2, dy + 28, done ? 0xFF9A3412 : TEXT, false);
        }
        String cl = canClaim ? "Claim ◆ " + wlt.get("nextClaimReward").getAsInt() : "Come back tomorrow";
        boolean chH = canClaim && !busy && in(mx, my, x + 12, y + 80, colW - 24, 22);
        UiDraw.roundRect(ctx, x + 12, y + 80, colW - 24, 22, 11, canClaim ? (chH ? UiDraw.accent() : UiDraw.withAlpha(UiDraw.accent(), 0xDD)) : 0xFFE5E8EE);
        ctx.drawText(tr, Text.literal(busy ? "…" : cl), x + 12 + (colW - 24 - tr.getWidth(busy ? "…" : cl)) / 2, y + 87, canClaim ? 0xFFFFFFFF : MUTED, false);
        wrapText(ctx, tr, "Log in every day: 10 shards on day one, up to 50 a day after a week. Missing a day starts over.",
                x + 12, y + 112, colW - 24, MUTED);

        // Playtime + milestones
        int px = x + colW + 8;
        UiDraw.roundRect(ctx, px, y, colW, h, 14, 0xD0FFFFFF);
        UiDraw.roundBorder(ctx, px, y, colW, h, 14, 0x10000000);
        ctx.drawText(tr, Text.literal("Playtime"), px + 12, y + 10, TEXT, false);
        long playMs = wlt.get("playMs").getAsLong();
        String hours = String.format(java.util.Locale.ROOT, "%.1f h", playMs / 3_600_000.0);
        ctx.drawText(tr, Text.literal(hours), px + colW - 12 - tr.getWidth(hours), y + 10, TEXT, false);
        long next = wlt.get("nextPayoutMs").getAsLong();
        float prog = 1f - next / 600_000f;
        UiDraw.roundRect(ctx, px + 12, y + 28, colW - 24, 8, 4, 0xFFE3E7EE);
        UiDraw.roundRect(ctx, px + 12, y + 28, Math.max(8, (int) ((colW - 24) * prog)), 8, 4, 0xFF38BDF8);
        String nx = "◆ 10 in " + (next / 60000) + "m " + ((next / 1000) % 60) + "s of play";
        ctx.drawText(tr, Text.literal(nx), px + 12, y + 40, MUTED, false);
        ctx.drawText(tr, Text.literal("Milestones"), px + 12, y + 58, TEXT, false);
        int my0 = y + 72;
        for (JsonElement e : wlt.getAsJsonArray("milestones")) {
            JsonObject m = e.getAsJsonObject();
            if (my0 + 18 > y + h - 4) {
                break;
            }
            int hrs = m.get("hours").getAsInt();
            boolean claimed = m.get("claimed").getAsBoolean();
            boolean reached = m.get("reached").getAsBoolean();
            ctx.drawText(tr, Text.literal(hrs + (hrs == 1 ? " hour" : " hours")), px + 12, my0 + 5, reached ? TEXT : MUTED, false);
            String r = "◆ " + m.get("reward").getAsInt();
            int bw = 62;
            int bx = px + colW - 12 - bw;
            if (claimed) {
                ctx.drawText(tr, Text.literal("Claimed"), bx + bw - tr.getWidth("Claimed"), my0 + 5, GREEN, false);
            } else if (reached) {
                boolean hv = in(mx, my, bx, my0, bw, 18);
                UiDraw.roundRect(ctx, bx, my0, bw, 18, 9, hv ? UiDraw.accent() : UiDraw.withAlpha(UiDraw.accent(), 0xDD));
                ctx.drawText(tr, Text.literal(r), bx + (bw - tr.getWidth(r)) / 2, my0 + 5, 0xFFFFFFFF, false);
            } else {
                ctx.drawText(tr, Text.literal(r), bx + bw - tr.getWidth(r), my0 + 5, MUTED, false);
            }
            my0 += 21;
        }

        // Ledger
        int lx = px + colW + 8;
        int lw = x + w - lx;
        UiDraw.roundRect(ctx, lx, y, lw, h, 14, 0xD0FFFFFF);
        UiDraw.roundBorder(ctx, lx, y, lw, h, 14, 0x10000000);
        ctx.drawText(tr, Text.literal("Where your shards went"), lx + 12, y + 10, TEXT, false);
        var ledger = wlt.getAsJsonArray("ledger");
        int maxScroll = Math.max(0, ledger.size() * 22 - (h - 36));
        scroll = Math.min(scroll, maxScroll);
        ctx.enableScissor(lx + 4, y + 26, lx + lw - 4, y + h - 6);
        try {
            int ly = y + 28 - scroll;
            if (ledger.isEmpty()) {
                ctx.drawText(tr, Text.literal("Nothing yet – play or claim your streak."), lx + 12, ly + 4, MUTED, false);
            }
            for (JsonElement e : ledger) {
                JsonObject row = e.getAsJsonObject();
                int delta = row.get("delta").getAsInt();
                String d = (delta >= 0 ? "+" : "−") + Math.abs(delta);
                ctx.drawText(tr, Text.literal(fit(tr, row.get("reason").getAsString(), lw - 70)), lx + 12, ly, TEXT, false);
                ctx.drawText(tr, Text.literal(ago(row.get("at").getAsLong())), lx + 12, ly + 10, MUTED, false);
                ctx.drawText(tr, Text.literal(d), lx + lw - 12 - tr.getWidth(d), ly + 4, delta >= 0 ? GREEN : 0xFFDC2626, false);
                ly += 22;
            }
        } finally {
            ctx.disableScissor();
        }
    }

    private boolean clickRewards(int mx, int my, int x, int y, int w, int h) {
        JsonObject wlt = Shards.wallet();
        if (wlt == null || busy) {
            return true;
        }
        int colW = (w - 16) / 3;
        if (wlt.get("canClaim").getAsBoolean() && in(mx, my, x + 12, y + 80, colW - 24, 22)) {
            busy = true;
            Shards.claimStreak(ok -> busy = false);
            return true;
        }
        int px = x + colW + 8;
        int my0 = y + 72;
        for (JsonElement e : wlt.getAsJsonArray("milestones")) {
            JsonObject m = e.getAsJsonObject();
            int bx = px + colW - 12 - 62;
            if (!m.get("claimed").getAsBoolean() && m.get("reached").getAsBoolean() && in(mx, my, bx, my0, 62, 18)) {
                busy = true;
                Shards.claimMilestone(m.get("id").getAsString(), ok -> busy = false);
                return true;
            }
            my0 += 21;
        }
        return true;
    }

    // ---- Badges -----------------------------------------------------------------------------

    private static final String[][] BADGE_HOW = {
            {"beta", "Played during the beta"},
            {"streak", "Reach a 7 day login streak"},
            {"veteran", "Play 10 hours in game"},
            {"collector", "Own 5 capes"},
            {"supporter", "Buy it in the store"},
            {"partner", "Aero partners"},
            {"media", "Aero media team"},
            {"staff", "Aero staff"},
            {"dev", "Aero developers"},
            {"owner", "Aero owner"},
    };

    private static String how(String id) {
        for (String[] r : BADGE_HOW) {
            if (r[0].equals(id)) {
                return r[1];
            }
        }
        return "";
    }

    private int[] badgeCell(int i, int x, int y, int w) {
        int cols = 2;
        int cw = (w - 8) / cols;
        return new int[]{x + (i % cols) * (cw + 8), y + 18 + (i / cols) * 50 - scroll, cw, 44};
    }

    private void drawBadges(DrawContext ctx, TextRenderer tr, int mx, int my, int x, int y, int w, int h) {
        List<Cosmetics.Item> badges = Cosmetics.all(Cosmetics.Kind.BADGE);
        int earned = 0;
        for (Cosmetics.Item b : badges) {
            if (Shards.earnedBadge(b.id())) {
                earned++;
            }
        }
        ctx.drawText(tr, Text.literal("Badges"), x + 4, y + 2, TEXT, false);
        String cnt = earned + " of " + badges.size() + " earned · click an earned badge to wear it";
        ctx.drawText(tr, Text.literal(cnt), x + w - 4 - tr.getWidth(cnt), y + 2, MUTED, false);
        int rows = (badges.size() + 1) / 2;
        scroll = Math.min(scroll, Math.max(0, rows * 50 - (h - 18)));
        String eq = Cosmetics.equipped(Cosmetics.Kind.BADGE);
        ctx.enableScissor(x, y + 14, x + w, y + h);
        try {
            for (int i = 0; i < badges.size(); i++) {
                Cosmetics.Item b = badges.get(i);
                int[] c = badgeCell(i, x, y, w);
                boolean has = Shards.earnedBadge(b.id());
                boolean on = eq.equals(b.id());
                boolean hv = in(mx, my, c[0], c[1], c[2], c[3]) && my >= y + 14;
                UiDraw.roundRect(ctx, c[0], c[1], c[2], c[3], 12, on ? UiDraw.withAlpha(UiDraw.accent(), 0x1E) : hv ? 0xFFFFFFFF : 0xC0FFFFFF);
                UiDraw.roundBorder(ctx, c[0], c[1], c[2], c[3], 12, on ? UiDraw.withAlpha(UiDraw.accent(), 0xAA) : 0x10000000);
                int col = has ? b.color() : 0xFFB8BEC9;
                UiDraw.roundRect(ctx, c[0] + 8, c[1] + 8, 28, 28, 14, UiDraw.withAlpha(col, has ? 0x30 : 0x20));
                String g = Cosmetics.glyph(Cosmetics.Kind.BADGE, b.id());
                ctx.drawText(tr, Text.literal(g), c[0] + 22 - tr.getWidth(g) / 2, c[1] + 18, col, false);
                if (!has) {
                    WardrobePanel.drawLock(ctx, c[0] + 30, c[1] + 28, 0xFF9CA3AF);
                }
                ctx.drawText(tr, Text.literal(b.name()), c[0] + 44, c[1] + 8, has ? TEXT : MUTED, false);
                ctx.drawText(tr, Text.literal(b.rarity().label), c[0] + 44 + tr.getWidth(b.name()) + 6, c[1] + 8, b.rarity().color, false);
                String state = on ? "Equipped" : has ? "Earned" : "Locked";
                int sc = on ? UiDraw.accent() : has ? GREEN : 0xFF9CA3AF;
                ctx.drawText(tr, Text.literal(state), c[0] + c[2] - 10 - tr.getWidth(state), c[1] + 8, sc, false);
                int[] p = Shards.badgeProgress(b.id());
                if (p != null && !has) {
                    int bw = c[2] - 44 - 50;
                    UiDraw.roundRect(ctx, c[0] + 44, c[1] + 24, bw, 6, 3, 0xFFE3E7EE);
                    UiDraw.roundRect(ctx, c[0] + 44, c[1] + 24, Math.max(6, bw * p[0] / Math.max(1, p[1])), 6, 3, b.color());
                    String pl = p[0] + "/" + p[1];
                    ctx.drawText(tr, Text.literal(pl), c[0] + c[2] - 10 - tr.getWidth(pl), c[1] + 23, MUTED, false);
                    ctx.drawText(tr, Text.literal(fit(tr, how(b.id()), c[2] - 54)), c[0] + 44, c[1] + 33, MUTED, false);
                } else if ("supporter".equals(b.id()) && !has) {
                    int price = Shards.priceOf("badge:supporter");
                    ctx.drawText(tr, Text.literal("Buy in the store · ◆ " + (price < 0 ? b.rarity().price : price)), c[0] + 44, c[1] + 24, MUTED, false);
                } else {
                    ctx.drawText(tr, Text.literal(fit(tr, how(b.id()), c[2] - 54)), c[0] + 44, c[1] + 24, MUTED, false);
                }
            }
        } finally {
            ctx.disableScissor();
        }
    }

    private boolean clickBadges(int mx, int my, int x, int y, int w, int h) {
        if (my < y + 14) {
            return true;
        }
        List<Cosmetics.Item> badges = Cosmetics.all(Cosmetics.Kind.BADGE);
        String eq = Cosmetics.equipped(Cosmetics.Kind.BADGE);
        for (int i = 0; i < badges.size(); i++) {
            Cosmetics.Item b = badges.get(i);
            int[] c = badgeCell(i, x, y, w);
            if (!in(mx, my, c[0], c[1], c[2], c[3])) {
                continue;
            }
            if (Shards.earnedBadge(b.id())) {
                Cosmetics.equip(Cosmetics.Kind.BADGE, eq.equals(b.id()) ? "none" : b.id());
            } else if ("supporter".equals(b.id())) {
                requestBuy = b;
            } else {
                Shards.status = "Not earned yet: " + how(b.id());
            }
            return true;
        }
        return true;
    }

    private static void wrapText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int max, int color) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (tr.getWidth(next) > max && !line.isEmpty()) {
                ctx.drawText(tr, Text.literal(line.toString()), x, y, color, false);
                y += 10;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (!line.isEmpty()) {
            ctx.drawText(tr, Text.literal(line.toString()), x, y, color, false);
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
}
