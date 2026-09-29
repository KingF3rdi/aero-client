package dev.aero.client.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.client.ui.UiDraw;
import net.minecraft.client.gui.DrawContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client side of the shards economy. Everything is decided by the Aero server (balance, streak,
 * store rotation, what you own); this only caches the last answers for the menus and sends the
 * claim / buy requests.
 */
public final class Shards {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static volatile JsonObject wallet;
    private static volatile JsonObject store;
    private static volatile long walletAt;
    private static volatile long storeAt;
    private static volatile boolean busy;
    /** The server answered but has no shop yet (older deploy): cosmetics stay free to wear until it does. */
    private static volatile boolean shopMissing;
    /** Last result / error line for the Store and Rewards pages. */
    public static volatile String status = "";

    private Shards() {}

    public record Offer(String id, String name, String rarity, int base, int price, int off) {}

    // ---- reads -------------------------------------------------------------------------------

    public static boolean shopMissing() {
        return shopMissing;
    }

    public static boolean ready() {
        return wallet != null;
    }

    public static int balance() {
        JsonObject w = wallet;
        return w == null ? 0 : w.get("shards").getAsInt();
    }

    public static String balanceLabel() {
        return wallet == null ? "–" : String.format(java.util.Locale.ROOT, "%,d", balance());
    }

    public static String streakLabel() {
        JsonObject w = wallet;
        if (w == null) {
            return AeroApi.authToken() == null ? "Connecting to Aero…" : "";
        }
        int s = w.get("streak").getAsInt();
        return s > 0 ? s + " day streak" : "";
    }

    public static boolean owns(String catalogId) {
        JsonObject w = wallet;
        if (w == null) {
            return false;
        }
        for (JsonElement e : w.getAsJsonArray("owned")) {
            if (catalogId.equals(e.getAsString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean freeBetaCape(String catalogId) {
        JsonObject w = wallet;
        if (w == null) {
            return false;
        }
        for (JsonElement e : w.getAsJsonArray("betaFree")) {
            if (catalogId.equals(e.getAsString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFreeBetaCape() {
        JsonObject w = wallet;
        return w != null && !w.getAsJsonArray("betaFree").isEmpty();
    }

    public static String rank() {
        JsonObject w = wallet;
        return w == null || !w.has("rank") ? "" : w.get("rank").getAsString();
    }

    public static boolean earnedBadge(String id) {
        JsonObject w = wallet;
        if ("beta".equals(id)) {
            return true;
        }
        if (w == null) {
            return false;
        }
        for (JsonElement e : w.getAsJsonArray("badges")) {
            if (id.equals(e.getAsString())) {
                return true;
            }
        }
        return false;
    }

    /** {have, need} for an achievement badge, or null when it has no progress bar. */
    public static int[] badgeProgress(String id) {
        JsonObject w = wallet;
        if (w == null || !w.has("badgeProgress") || !w.getAsJsonObject("badgeProgress").has(id)) {
            return null;
        }
        JsonObject p = w.getAsJsonObject("badgeProgress").getAsJsonObject(id);
        return new int[]{p.get("have").getAsInt(), p.get("need").getAsInt()};
    }

    public static JsonObject wallet() {
        return wallet;
    }

    public static JsonObject store() {
        return store;
    }

    /** Price shown right now for a catalog id (rotation discount applied), or -1 when unknown. */
    public static int priceOf(String catalogId) {
        JsonObject s = store;
        if (s == null) {
            return -1;
        }
        for (JsonElement e : s.getAsJsonArray("catalog")) {
            JsonObject o = e.getAsJsonObject();
            if (catalogId.equals(o.get("id").getAsString())) {
                return o.get("price").getAsInt();
            }
        }
        return -1;
    }

    public static Offer offer(JsonObject o) {
        return new Offer(o.get("id").getAsString(), o.get("name").getAsString(), o.get("rarity").getAsString(),
                o.get("base").getAsInt(), o.get("price").getAsInt(), o.has("off") ? o.get("off").getAsInt() : 0);
    }

    public static Offer featured() {
        JsonObject s = store;
        return s == null ? null : offer(s.getAsJsonObject("featured"));
    }

    public static List<Offer> list(String key) {
        List<Offer> out = new ArrayList<>();
        JsonObject s = store;
        if (s != null && s.has(key)) {
            for (JsonElement e : s.getAsJsonArray(key)) {
                out.add(offer(e.getAsJsonObject()));
            }
        }
        return out;
    }

    // ---- network -----------------------------------------------------------------------------

    /** Re-fetches the store and (when logged in) the wallet; throttled unless forced. */
    public static void refresh(boolean force) {
        long now = System.currentTimeMillis();
        if (busy || (!force && now - walletAt < 30_000 && now - storeAt < 60_000)) {
            return;
        }
        busy = true;
        String base = AeroApi.base();
        String token = AeroApi.authToken();
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> s = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/store"))
                        .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (s.statusCode() == 200) {
                    store = JsonParser.parseString(s.body()).getAsJsonObject();
                    storeAt = System.currentTimeMillis();
                    shopMissing = false;
                } else if (s.statusCode() == 404) {
                    shopMissing = true;
                    storeAt = System.currentTimeMillis();
                }
                if (token != null) {
                    HttpResponse<String> w = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/wallet"))
                            .timeout(Duration.ofSeconds(8)).header("authorization", "Bearer " + token).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
                    if (w.statusCode() == 200) {
                        wallet = JsonParser.parseString(w.body()).getAsJsonObject();
                        walletAt = System.currentTimeMillis();
                        if (status.startsWith("Shards need") || status.startsWith("Aero server")) {
                            status = "";
                        }
                    } else if (w.statusCode() == 403) {
                        status = error(w.body());
                    }
                }
            } catch (Exception e) {
                status = "Aero server not reachable";
            } finally {
                busy = false;
            }
        }, "aero-shards");
        t.setDaemon(true);
        t.start();
    }

    /** Called when the logged-in account changes, so nothing from the previous account is shown. */
    public static void reset() {
        wallet = null;
        walletAt = 0;
        status = "";
    }

    public static void claimStreak(Consumer<Boolean> done) {
        post("/api/rewards/streak", "{}", done, r -> "Claimed " + r.get("reward").getAsInt() + " shards");
    }

    public static void claimMilestone(String id, Consumer<Boolean> done) {
        JsonObject b = new JsonObject();
        b.addProperty("id", id);
        post("/api/rewards/milestone", b.toString(), done, r -> "Claimed " + r.get("reward").getAsInt() + " shards");
    }

    public static void buy(String catalogId, int shownPrice, Consumer<Boolean> done) {
        JsonObject b = new JsonObject();
        b.addProperty("item", catalogId);
        if (shownPrice >= 0) {
            b.addProperty("price", shownPrice);
        }
        post("/api/store/buy", b.toString(), done, r -> r.get("price").getAsInt() == 0 ? "Unlocked for free" : "Bought for " + r.get("price").getAsInt() + " shards");
    }

    /** Opens the website store logged in to this account (one-time code), so Shards can be spent there too. */
    public static void linkWebsite() {
        String token = AeroApi.authToken();
        if (token == null) {
            status = "Not connected to the Aero server yet";
            return;
        }
        String base = AeroApi.base();
        status = "Opening the store…";
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/link")).timeout(Duration.ofSeconds(8))
                        .header("authorization", "Bearer " + token).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() != 200) {
                    status = error(r.body());
                    return;
                }
                JsonObject res = JsonParser.parseString(r.body()).getAsJsonObject();
                URI url = URI.create(res.get("url").getAsString());
                status = "Store opened in your browser. On another device, enter code " + res.get("code").getAsString();
                net.minecraft.client.MinecraftClient.getInstance().execute(() -> net.minecraft.util.Util.getOperatingSystem().open(url));
            } catch (Exception e) {
                status = "Aero server not reachable";
            }
        }, "aero-link-website");
        t.setDaemon(true);
        t.start();
    }

    private static void post(String path, String body, Consumer<Boolean> done, java.util.function.Function<JsonObject, String> okText) {
        String token = AeroApi.authToken();
        if (token == null) {
            status = "Not connected to the Aero server yet";
            done.accept(false);
            return;
        }
        String base = AeroApi.base();
        Thread t = new Thread(() -> {
            boolean ok = false;
            try {
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8))
                        .header("authorization", "Bearer " + token).header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    JsonObject res = JsonParser.parseString(r.body()).getAsJsonObject();
                    wallet = res;
                    walletAt = System.currentTimeMillis();
                    status = okText.apply(res);
                    ok = true;
                } else {
                    status = error(r.body());
                }
            } catch (Exception e) {
                status = "Aero server not reachable";
            }
            boolean result = ok;
            net.minecraft.client.MinecraftClient.getInstance().execute(() -> done.accept(result));
        }, "aero-shards-post");
        t.setDaemon(true);
        t.start();
    }

    private static String error(String body) {
        try {
            return JsonParser.parseString(body).getAsJsonObject().get("error").getAsString();
        } catch (Exception e) {
            return "Request failed";
        }
    }

    public static JsonArray arr(String key) {
        JsonObject w = wallet;
        return w == null || !w.has(key) ? new JsonArray() : w.getAsJsonArray(key);
    }

    // ---- drawing -----------------------------------------------------------------------------

    /** The shard gem: a small faceted diamond in cyan/violet. */
    public static void drawGem(DrawContext c, int x, int y, int size) {
        int s = Math.max(6, size);
        int cx = x + s / 2;
        int top = 0xFF7DD3FC;
        int bottom = 0xFF8B5CF6;
        int crown = Math.max(1, s / 3);
        for (int row = 0; row < s; row++) {
            // Crown widens from a quarter to half width, the pavilion narrows to a point.
            float half = row < crown ? s / 4f + (s / 4f) * row / crown : (s / 2f) * (s - row) / (s - crown);
            int hw = Math.max(1, Math.round(half));
            int col = row < crown ? top : mix(top, bottom, (row - crown) / (float) (s - crown));
            c.fill(cx - hw, y + row, cx + hw, y + row + 1, UiDraw.fa(col));
        }
        c.fill(cx - 1, y + 1, cx, y + s / 3, 0xCCFFFFFF);
    }

    private static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
