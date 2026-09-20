package dev.aero.client.social;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Players known to use Aero Client, from a public list (a client mod can't see this on other players itself). */
public final class ClientUsers {
    private static final String URL = "https://raw.githubusercontent.com/KingF3rdi/aero-client-users/main/users.json";
    private static final long REFRESH_MS = 10 * 60 * 1000L;
    private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> NAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, String> BADGES = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, String>> COSMETICS = new ConcurrentHashMap<>();

    /** The cosmetic id another listed user has for a kind ("cape", "wings", "head", "pet"), or "none". */
    public static String cosmeticOf(UUID id, String kind) {
        Map<String, String> m = id == null ? null : COSMETICS.get(id);
        return m == null ? "none" : m.getOrDefault(kind, "none");
    }

    public static boolean hasCosmetics(UUID id) {
        return id != null && COSMETICS.containsKey(id);
    }
    private static volatile long lastFetch;
    private static final Style STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x4F8EFF)).withBold(true);

    private ClientUsers() {}

    private static String listUrl() {
        String b = AeroApi.base();
        return b.isEmpty() ? URL : b + "/api/users";
    }

    /** The user's chosen badge (own: equipped one; others: from the public list), else the plain blue A. */
    public static MutableText badge(UUID id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String badgeId = mc.player != null && id != null && id.equals(mc.player.getUuid())
                ? dev.aero.client.cosmetic.Cosmetics.equipped(dev.aero.client.cosmetic.Cosmetics.Kind.BADGE)
                : BADGES.getOrDefault(id, "none");
        var item = "none".equals(badgeId) ? null : dev.aero.client.cosmetic.Cosmetics.named(dev.aero.client.cosmetic.Cosmetics.Kind.BADGE, badgeId);
        if (item == null) {
            return Text.literal("A ").setStyle(STYLE);
        }
        String glyph = dev.aero.client.cosmetic.Cosmetics.glyph(dev.aero.client.cosmetic.Cosmetics.Kind.BADGE, item.id());
        return Text.literal(glyph + " ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(item.color() & 0xFFFFFF)).withBold(true));
    }

    /** Whether to draw the client badge for a listed user at a place ("nametag", "tab", "chat"), per the Client Badge module. */
    public static boolean showBadge(UUID id, String name, String place) {
        var c = dev.aero.client.AeroClient.CONFIG;
        if (c == null || !c.badgeEnabled) {
            return false;
        }
        boolean on = switch (place) {
            case "nametag" -> c.badgeNametag;
            case "tab" -> c.badgeTab;
            default -> c.badgeChat;
        };
        if (!on || !isUser(id)) {
            return false;
        }
        if ("Friends only".equals(c.badgeMode)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            boolean self = mc.player != null && id.equals(mc.player.getUuid());
            return self || (name != null && FriendStore.isFriend(name));
        }
        return true;
    }

    public static boolean isUser(UUID id) {
        if (id == null) {
            return false;
        }
        refreshIfStale();
        MinecraftClient mc = MinecraftClient.getInstance();
        return (mc.player != null && id.equals(mc.player.getUuid())) || USERS.contains(id);
    }

    public static boolean isUserName(String name) {
        return NAMES.containsValue(name);
    }

    public static Set<String> knownNames() {
        return Set.copyOf(NAMES.values());
    }

    private static void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - lastFetch < REFRESH_MS) {
            return;
        }
        lastFetch = now;
        HttpClient.newHttpClient().sendAsync(
                        HttpRequest.newBuilder(URI.create(listUrl())).timeout(Duration.ofSeconds(8)).build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() != 200) {
                        return;
                    }
                    Set<UUID> ids = ConcurrentHashMap.newKeySet();
                    Map<UUID, String> names = new ConcurrentHashMap<>();
                    Map<UUID, String> badges = new ConcurrentHashMap<>();
                    Map<UUID, Map<String, String>> cosmetics = new ConcurrentHashMap<>();
                    for (JsonElement e : JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonArray("users")) {
                        try {
                            UUID id = UUID.fromString(e.getAsJsonObject().get("uuid").getAsString());
                            ids.add(id);
                            names.put(id, e.getAsJsonObject().get("name").getAsString());
                            if (e.getAsJsonObject().has("badge")) {
                                badges.put(id, e.getAsJsonObject().get("badge").getAsString());
                            }
                            if (e.getAsJsonObject().has("cosmetics")) {
                                Map<String, String> cm = new java.util.HashMap<>();
                                for (var en : e.getAsJsonObject().getAsJsonObject("cosmetics").entrySet()) {
                                    cm.put(en.getKey().toLowerCase(java.util.Locale.ROOT), en.getValue().getAsString());
                                }
                                cosmetics.put(id, cm);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    USERS.clear();
                    USERS.addAll(ids);
                    NAMES.clear();
                    NAMES.putAll(names);
                    BADGES.clear();
                    BADGES.putAll(badges);
                    COSMETICS.clear();
                    COSMETICS.putAll(cosmetics);
                })
                .exceptionally(t -> null);
    }

    /** Inserts the user's badge right before the first occurrence of a known user's name in a chat line. */
    public static Text badgeChat(Text message) {
        refreshIfStale();
        Map<String, UUID> byName = new java.util.HashMap<>();
        for (Map.Entry<UUID, String> e : NAMES.entrySet()) {
            byName.put(e.getValue(), e.getKey());
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            byName.put(mc.player.getName().getString(), mc.player.getUuid());
        }
        byName.entrySet().removeIf(e -> !showBadge(e.getValue(), e.getKey(), "chat"));
        if (byName.isEmpty()) {
            return message;
        }
        MutableText out = Text.empty();
        boolean[] done = {false};
        message.visit((StringVisitable.StyledVisitor<Object>) (style, str) -> {
            String rest = str;
            while (!done[0]) {
                int best = -1;
                UUID who = null;
                for (Map.Entry<String, UUID> n : byName.entrySet()) {
                    int i = indexOfWord(rest, n.getKey());
                    if (i >= 0 && (best < 0 || i < best)) {
                        best = i;
                        who = n.getValue();
                    }
                }
                if (best < 0) {
                    break;
                }
                out.append(Text.literal(rest.substring(0, best)).setStyle(style));
                out.append(badge(who));
                rest = rest.substring(best);
                done[0] = true;
            }
            out.append(Text.literal(rest).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return done[0] ? out : message;
    }

    private static int indexOfWord(String s, String word) {
        int from = 0;
        while (true) {
            int i = s.indexOf(word, from);
            if (i < 0) {
                return -1;
            }
            boolean before = i == 0 || !Character.isLetterOrDigit(s.charAt(i - 1)) && s.charAt(i - 1) != '_';
            int end = i + word.length();
            boolean after = end >= s.length() || !Character.isLetterOrDigit(s.charAt(end)) && s.charAt(end) != '_';
            if (before && after) {
                return i;
            }
            from = i + 1;
        }
    }
}
