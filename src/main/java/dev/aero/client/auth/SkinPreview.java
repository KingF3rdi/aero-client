package dev.aero.client.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resolves skins from the vanilla skin provider first, then Mojang / public skin CDNs.
 */
public final class SkinPreview {
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Minecraft/1.21";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "larp-skin");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, Identifier> READY = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TEX_SIZE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> PENDING = new ConcurrentHashMap<>();
    private static final Map<String, Long> RETRY_AT = new ConcurrentHashMap<>();

    private SkinPreview() {}

    public static void requestOwn() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = AccountManager.currentName();
        String uuid = "";
        String token = "";
        SavedAccount acc = AccountManager.account;
        if (acc != null) {
            if (acc.uuid != null) {
                uuid = acc.uuid.toString();
            }
            token = acc.mcToken == null ? "" : acc.mcToken;
            if (acc.name != null && !acc.name.isBlank()) {
                name = acc.name;
            }
        }
        if (uuid.isBlank()) {
            try {
                Object profile = mc.getGameProfile();
                uuid = profileId(profile);
                String pn = profileName(profile);
                if (!pn.isBlank()) {
                    name = pn;
                }
            } catch (Throwable ignored) {
            }
        }
        cacheVanilla(name, uuid);
        request(keyOf(name, uuid), name, uuid.replace("-", ""), token);
        if (name != null && !name.isBlank()) {
            request(name.toLowerCase(), name, uuid.replace("-", ""), token);
        }
    }

    public static void requestLookup(String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        String q = query.trim();
        request(q.toLowerCase(), q, "", "");
    }

    public static boolean ready(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return texture(key) != null;
    }

    public static void drawBody(DrawContext context, String key, int x, int y, int scale) {
        Identifier id = texture(key);
        if (id == null) {
            return;
        }
        int s = Math.max(2, scale);
        blit(context, id, key, x + 4 * s, y, 8 * s, 8 * s, 8, 8, 8, 8);
        blit(context, id, key, x + 4 * s, y, 8 * s, 8 * s, 40, 8, 8, 8);
        blit(context, id, key, x + 4 * s, y + 8 * s, 8 * s, 12 * s, 20, 20, 8, 12);
        blit(context, id, key, x + 4 * s, y + 8 * s, 8 * s, 12 * s, 20, 36, 8, 12);
        blit(context, id, key, x, y + 8 * s, 4 * s, 12 * s, 44, 20, 4, 12);
        blit(context, id, key, x + 12 * s, y + 8 * s, 4 * s, 12 * s, 36, 52, 4, 12);
        blit(context, id, key, x + 4 * s, y + 20 * s, 4 * s, 12 * s, 4, 20, 4, 12);
        blit(context, id, key, x + 8 * s, y + 20 * s, 4 * s, 12 * s, 20, 52, 4, 12);
    }

    public static void drawHead(DrawContext context, String key, int x, int y, int size) {
        Identifier id = texture(key);
        if (id == null) {
            return;
        }
        blit(context, id, key, x, y, size, size, 8, 8, 8, 8);
        blit(context, id, key, x, y, size, size, 40, 8, 8, 8);
    }

    private static Identifier texture(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String k = key.toLowerCase().trim();
        Identifier id = READY.get(k);
        if (id != null) {
            return id;
        }
        id = READY.get(k.replace("-", ""));
        if (id != null) {
            return id;
        }
        if (isOwnKey(k)) {
            return READY.getOrDefault("own", null);
        }
        return null;
    }

    private static boolean isOwnKey(String key) {
        String k = key.toLowerCase().replace("-", "");
        SavedAccount acc = AccountManager.account;
        if (acc != null) {
            if (acc.uuid != null && k.equals(acc.uuid.toString().replace("-", "").toLowerCase())) {
                return true;
            }
            if (acc.name != null && k.equals(acc.name.toLowerCase())) {
                return true;
            }
        }
        try {
            Object profile = MinecraftClient.getInstance().getGameProfile();
            String id = profileId(profile).replace("-", "").toLowerCase();
            if (!id.isBlank() && k.equals(id)) {
                return true;
            }
            String pn = profileName(profile).toLowerCase();
            if (!pn.isBlank() && k.equals(pn)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        String current = AccountManager.currentName();
        return current != null && k.equals(current.toLowerCase());
    }

    private static Identifier sessionTexture() {
        return READY.get("own");
    }

    private static void cacheVanilla(String name, String uuid) {
        // Vanilla skin identifiers are atlas-backed and look wrong as 64×64 GUI blits.
        // Wardrobe/friends always use downloaded PNG skins via request().
    }

    private static void blit(DrawContext context, Identifier id, String key, int x, int y, int w, int h,
                             int u, int v, int uw, int vh) {
        int tex = TEX_SIZE.getOrDefault(id.toString(), 64);
        if (tex < 32) {
            tex = 64;
        }
        try {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, (float) u, (float) v,
                    w, h, uw, vh, tex, tex);
        } catch (Throwable t) {
            try {
                context.drawTexturedQuad(id, x, y, x + w, y + h,
                        u / (float) tex, v / (float) tex, (u + uw) / (float) tex, (v + vh) / (float) tex);
            } catch (Throwable ignored) {
            }
        }
    }

    private static String keyOf(String name, String uuid) {
        if (uuid != null && !uuid.isBlank()) {
            return uuid.replace("-", "").toLowerCase();
        }
        return name == null ? "" : name.toLowerCase();
    }

    private static String profileId(Object profile) {
        if (profile == null) {
            return "";
        }
        for (String m : new String[]{"id", "getId"}) {
            try {
                Object v = profile.getClass().getMethod(m).invoke(profile);
                return v == null ? "" : v.toString();
            } catch (Throwable ignored) {
            }
        }
        return "";
    }

    private static String profileName(Object profile) {
        if (profile == null) {
            return "";
        }
        for (String m : new String[]{"name", "getName"}) {
            try {
                Object v = profile.getClass().getMethod(m).invoke(profile);
                return v == null ? "" : v.toString();
            } catch (Throwable ignored) {
            }
        }
        return "";
    }

    private static void alias(String key, Identifier id) {
        if (key == null || key.isBlank() || id == null) {
            return;
        }
        READY.put(key.toLowerCase().replace("-", ""), id);
        READY.put(key.toLowerCase(), id);
    }

    private static void request(String key, String name, String uuidHex, String token) {
        String k = key == null ? "" : key.toLowerCase();
        if (k.isBlank() || READY.containsKey(k)) {
            return;
        }
        Long retry = RETRY_AT.get(k);
        if (retry != null && retry > System.currentTimeMillis()) {
            return;
        }
        if (PENDING.putIfAbsent(k, true) != null) {
            return;
        }
        IO.execute(() -> {
            try {
                byte[] png = downloadPng(name, uuidHex, token);
                if (png == null || png.length < 64 || png[0] != (byte) 0x89) {
                    RETRY_AT.put(k, System.currentTimeMillis() + 8000L);
                    return;
                }
                MinecraftClient.getInstance().execute(() -> register(k, name, uuidHex, png));
            } catch (Exception e) {
                RETRY_AT.put(k, System.currentTimeMillis() + 8000L);
            } finally {
                PENDING.remove(k);
            }
        });
    }

    private static void register(String key, String name, String uuidHex, byte[] png) {
        try {
            NativeImage image = NativeImage.read(png);
            if (image.getWidth() < 64 || image.getHeight() < 32) {
                image.close();
                return;
            }
            String safe = key.replaceAll("[^a-z0-9]", "");
            if (safe.isBlank()) {
                safe = "player";
            }
            final String label = safe;
            Identifier id = Identifier.of("larp", "skin/" + label);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "larp-skin-" + label, image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            tex.upload();
            TEX_SIZE.put(id.toString(), 64);
            alias(key, id);
            if (isOwnKey(key) || (name != null && isOwnKey(name))) {
                alias("own", id);
            }
            if (name != null && !name.isBlank()) {
                alias(name.toLowerCase(), id);
            }
            if (uuidHex != null && uuidHex.replace("-", "").length() == 32) {
                alias(uuidHex.replace("-", "").toLowerCase(), id);
            }
        } catch (Throwable ignored) {
        }
    }

    private static byte[] downloadPng(String name, String uuidHex, String token) throws Exception {
        String uuid = uuidHex == null ? "" : uuidHex.replace("-", "").toLowerCase();
        if (uuid.length() != 32 && name != null && !name.isBlank() && !"nicht angemeldet".equalsIgnoreCase(name)) {
            uuid = fetchUuid(name);
        }
        String url = "";
        if (token != null && !token.isBlank()) {
            url = skinUrlFromServices(token);
        }
        if (url.isBlank() && uuid.length() == 32) {
            url = skinUrlFromSession(uuid);
        }
        if (!url.isBlank()) {
            byte[] direct = getBytes(url);
            if (direct != null && direct.length > 64 && direct[0] == (byte) 0x89) {
                return direct;
            }
        }
        if (uuid.length() == 32) {
            for (String u : new String[]{
                    "https://crafatar.com/skins/" + uuid,
                    "https://mc-heads.net/skin/" + uuid,
                    "https://minotar.net/skin/" + uuid
            }) {
                byte[] b = getBytes(u);
                if (b != null && b.length > 64 && b[0] == (byte) 0x89) {
                    return b;
                }
            }
        }
        if (name != null && !name.isBlank() && !"nicht angemeldet".equalsIgnoreCase(name)) {
            String enc = java.net.URLEncoder.encode(name, StandardCharsets.UTF_8);
            for (String u : new String[]{
                    "https://mc-heads.net/skin/" + enc,
                    "https://minotar.net/skin/" + enc,
                    "https://mineskin.eu/skin/" + enc
            }) {
                byte[] b = getBytes(u);
                if (b != null && b.length > 64 && b[0] == (byte) 0x89) {
                    return b;
                }
            }
        }
        return null;
    }

    private static String fetchUuid(String name) throws Exception {
        JsonObject json = getJson("https://api.mojang.com/users/profiles/minecraft/"
                + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8), null);
        if (json == null || !json.has("id")) {
            json = getJson("https://api.minecraftservices.com/minecraft/profile/lookup/name/"
                    + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8), null);
        }
        if (json == null || !json.has("id")) {
            return "";
        }
        return json.get("id").getAsString().replace("-", "").toLowerCase();
    }

    public static String skinUrlFromServices(String token) {
        try {
            JsonObject profile = getJson("https://api.minecraftservices.com/minecraft/profile", token);
            if (profile == null || !profile.has("skins")) {
                return "";
            }
            JsonArray skins = profile.getAsJsonArray("skins");
            String fallback = "";
            for (JsonElement el : skins) {
                JsonObject s = el.getAsJsonObject();
                String u = s.has("url") ? s.get("url").getAsString() : "";
                if (u.isBlank()) {
                    continue;
                }
                if ("ACTIVE".equalsIgnoreCase(s.has("state") ? s.get("state").getAsString() : "")) {
                    return u.replace("http://", "https://");
                }
                fallback = u;
            }
            return fallback.replace("http://", "https://");
        } catch (Exception e) {
            return "";
        }
    }

    private static String skinUrlFromSession(String uuid) {
        try {
            JsonObject json = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid, null);
            if (json == null || !json.has("properties")) {
                return "";
            }
            for (JsonElement el : json.getAsJsonArray("properties")) {
                JsonObject p = el.getAsJsonObject();
                if (!"textures".equals(p.has("name") ? p.get("name").getAsString() : "")) {
                    continue;
                }
                String raw = p.get("value").getAsString();
                String decoded = new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
                JsonObject tex = JsonParser.parseString(decoded).getAsJsonObject();
                String u = tex.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
                return u.replace("http://", "https://");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static JsonObject getJson(String url, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", UA)
                .GET();
        if (bearer != null && !bearer.isBlank()) {
            b.header("Authorization", "Bearer " + bearer);
        }
        HttpResponse<String> res = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 400 || res.body() == null || res.body().isBlank()) {
            return null;
        }
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static byte[] getBytes(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url.replace("http://", "https://")))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", UA)
                .GET()
                .build();
        HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() >= 400) {
            return null;
        }
        return res.body();
    }
}
