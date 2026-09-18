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
    private static volatile long lastFetch;
    private static final Style STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x4F8EFF)).withBold(true);

    private ClientUsers() {}

    public static MutableText badge() {
        return Text.literal("A ").setStyle(STYLE);
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
                        HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(8)).build(),
                        HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() != 200) {
                        return;
                    }
                    Set<UUID> ids = ConcurrentHashMap.newKeySet();
                    Map<UUID, String> names = new ConcurrentHashMap<>();
                    for (JsonElement e : JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonArray("users")) {
                        try {
                            UUID id = UUID.fromString(e.getAsJsonObject().get("uuid").getAsString());
                            ids.add(id);
                            names.put(id, e.getAsJsonObject().get("name").getAsString());
                        } catch (Exception ignored) {
                        }
                    }
                    USERS.clear();
                    USERS.addAll(ids);
                    NAMES.clear();
                    NAMES.putAll(names);
                })
                .exceptionally(t -> null);
    }

    /** Inserts the badge right before the first occurrence of a known user's name in a chat line. */
    public static Text badgeChat(Text message) {
        refreshIfStale();
        Set<String> names = Set.copyOf(NAMES.values());
        MinecraftClient mc = MinecraftClient.getInstance();
        String self = mc.player != null ? mc.player.getName().getString() : null;
        if (names.isEmpty() && self == null) {
            return message;
        }
        MutableText out = Text.empty();
        boolean[] done = {false};
        message.visit((StringVisitable.StyledVisitor<Object>) (style, str) -> {
            String rest = str;
            while (!done[0]) {
                int best = -1;
                for (String n : names) {
                    int i = indexOfWord(rest, n);
                    if (i >= 0 && (best < 0 || i < best)) {
                        best = i;
                    }
                }
                if (self != null) {
                    int i = indexOfWord(rest, self);
                    if (i >= 0 && (best < 0 || i < best)) {
                        best = i;
                    }
                }
                if (best < 0) {
                    break;
                }
                out.append(Text.literal(rest.substring(0, best)).setStyle(style));
                out.append(badge());
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
