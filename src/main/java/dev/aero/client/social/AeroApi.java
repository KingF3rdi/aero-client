package dev.aero.client.social;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.client.AeroClient;
import dev.aero.client.cosmetic.Cosmetics;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Talks to the Aero server (Cloudflare Worker): proves the player's identity through Mojang's session
 * server, then sends a heartbeat with the equipped cosmetics every minute. Off while apiBase is empty.
 */
public final class AeroApi {
    private static final String DEFAULT_BASE = "https://aero.gamekni9ht.workers.dev";
    private static final long BEAT_MS = 60_000L;
    private static final long TOKEN_MS = 20L * 60 * 60 * 1000;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static volatile String token;
    private static volatile long tokenAt;
    private static volatile long lastBeat;
    private static volatile boolean busy;

    private AeroApi() {}

    /** Bearer token for authenticated calls (custom capes, delete-my-data), or null before the server login finishes. */
    public static String authToken() {
        return token;
    }

    /** Server base URL without a trailing slash, or "" when no server is configured. */
    public static String base() {
        var c = AeroClient.CONFIG;
        String b = c == null || c.apiBase == null ? "" : c.apiBase.trim();
        if (b.isEmpty()) {
            b = DEFAULT_BASE; // older configs saved an empty value
        }
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b;
    }

    /** Call every client tick. Runs from the title screen on, so a brand-new account registers on its very first start. */
    public static void tick(MinecraftClient mc) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.shareProfile || busy || base().isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBeat < BEAT_MS) {
            return;
        }
        lastBeat = now;
        busy = true;
        String b = base();
        var session = mc.getSession();
        String name = session.getUsername();
        java.util.UUID uuid = session.getUuidOrNull();
        String access = session.getAccessToken();
        String body = profileBody();
        Thread t = new Thread(() -> {
            try {
                if (token == null || System.currentTimeMillis() - tokenAt > TOKEN_MS) {
                    login(mc, b, name, uuid, access);
                }
                if (token != null) {
                    int code = post(b + "/api/heartbeat", body, token).statusCode();
                    if (code != 200) {
                        log("heartbeat status " + code);
                    }
                    if (code == 401) {
                        token = null;
                    }
                }
            } catch (Exception e) {
                log("heartbeat failed: " + e);
                status("Server not reachable: " + e.getClass().getSimpleName());
            } finally {
                busy = false;
            }
        }, "aero-api");
        t.setDaemon(true);
        t.start();
    }

    /** Removes this player from the server (the "Delete my data" button). */
    public static void deleteMe() {
        String b = base();
        String tk = token;
        if (b.isEmpty() || tk == null) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                HTTP.send(HttpRequest.newBuilder(URI.create(b + "/api/me")).timeout(Duration.ofSeconds(8))
                        .header("authorization", "Bearer " + tk).DELETE().build(), HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            }
        }, "aero-api-delete");
        t.setDaemon(true);
        t.start();
    }

    /** Appends to aero-api.log in the game folder so connection problems can be diagnosed. */
    private static void log(String line) {
        try {
            java.nio.file.Files.writeString(MinecraftClient.getInstance().runDirectory.toPath().resolve("aero-api.log"),
                    java.time.LocalTime.now().withNano(0) + " " + line + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static volatile String lastStatus = "";

    /** One chat line per status change, so the player can see whether the server connection works. */
    private static void status(String msg) {
        if (msg.equals(lastStatus)) {
            return;
        }
        lastStatus = msg;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal("[Aero] " + msg), false);
            }
        });
    }

    private static void login(MinecraftClient mc, String b, String name, java.util.UUID uuid, String access) throws Exception {
        if (uuid != null && access != null && !access.isBlank()) {
            try {
                JsonObject start = JsonParser.parseString(post(b + "/api/auth/start", "{}", null).body()).getAsJsonObject();
                String serverId = start.get("serverId").getAsString();
                mc.getApiServices().sessionService().joinServer(uuid, access, serverId);
                JsonObject req = new JsonObject();
                req.addProperty("name", name);
                req.addProperty("serverId", serverId);
                HttpResponse<String> res = post(b + "/api/auth/finish", req.toString(), null);
                if (res.statusCode() == 200) {
                    token = JsonParser.parseString(res.body()).getAsJsonObject().get("token").getAsString();
                    tokenAt = System.currentTimeMillis();
                    log("logged in as " + name);
                    status("Connected to the Aero server");
                    return;
                }
                log("login rejected: " + res.statusCode() + " " + res.body());
            } catch (Exception e) {
                log("verified login failed: " + e);
            }
        } else {
            log("no online session (offline account?)");
        }
        // No valid Mojang session: register as a guest, so the player still counts (no cosmetics for guests).
        if (uuid == null) {
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty("name", name);
        req.addProperty("uuid", uuid.toString());
        HttpResponse<String> res = post(b + "/api/auth/guest", req.toString(), null);
        if (res.statusCode() == 200) {
            token = JsonParser.parseString(res.body()).getAsJsonObject().get("token").getAsString();
            tokenAt = System.currentTimeMillis();
            log("registered as guest " + name);
            status("Connected to the Aero server as guest (no verified Microsoft session)");
        } else {
            log("guest registration rejected: " + res.statusCode() + " " + res.body());
            status("Server login rejected (" + res.statusCode() + ")");
        }
    }

    private static HttpResponse<String> post(String url, String body, String bearer) throws Exception {
        HttpRequest.Builder r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null) {
            r.header("authorization", "Bearer " + bearer);
        }
        return HTTP.send(r.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String profileBody() {
        JsonObject o = new JsonObject();
        o.addProperty("badge", Cosmetics.equipped(Cosmetics.Kind.BADGE));
        JsonObject cos = new JsonObject();
        cos.addProperty("cape", Cosmetics.equipped(Cosmetics.Kind.CAPE));
        cos.addProperty("wings", Cosmetics.equipped(Cosmetics.Kind.WINGS));
        cos.addProperty("head", Cosmetics.equipped(Cosmetics.Kind.HEAD));
        cos.addProperty("pet", Cosmetics.equipped(Cosmetics.Kind.PET));
        o.add("cosmetics", cos);
        return o.toString();
    }
}
