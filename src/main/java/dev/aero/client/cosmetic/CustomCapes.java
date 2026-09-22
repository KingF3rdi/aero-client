package dev.aero.client.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.client.social.AeroApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Community capes: browse what other Aero players published, download and cache their textures on
 * demand, and publish/delete your own. A cape id elsewhere in the cosmetic system is "custom_&lt;n&gt;",
 * n being the server's row id; its PNG is cached at &lt;gameDir&gt;/aero-capes/custom_&lt;n&gt;.png once fetched.
 * To publish, a player drops their own 64x32 PNG into aero-capes/ and enters its file name.
 */
public final class CustomCapes {
    public record Entry(int id, String name, String ownerName, String ownerUuid) {
        public String capeId() {
            return "custom_" + id;
        }
    }

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private static final long REFRESH_MS = 2 * 60 * 1000L;
    private static final Set<String> DOWNLOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> ATTEMPTED = ConcurrentHashMap.newKeySet();

    private static volatile List<Entry> list = List.of();
    private static volatile long lastFetch;
    private static volatile boolean refreshing;
    private static volatile boolean publishing;
    /** Last publish/delete outcome, shown in the Wardrobe until the next attempt. */
    public static volatile String status = "";

    private CustomCapes() {}

    public static List<Entry> list() {
        refreshIfStale();
        return list;
    }

    public static Entry find(String capeId) {
        for (Entry e : list) {
            if (e.capeId().equals(capeId)) {
                return e;
            }
        }
        return null;
    }

    public static boolean isPublishing() {
        return publishing;
    }

    public static boolean isMine(Entry e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return e != null && mc.player != null && e.ownerUuid().replace("-", "")
                .equalsIgnoreCase(mc.player.getUuidAsString().replace("-", ""));
    }

    public static void refreshNow() {
        lastFetch = 0;
        refreshIfStale();
    }

    private static void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (refreshing || now - lastFetch < REFRESH_MS) {
            return;
        }
        String base = AeroApi.base();
        if (base.isEmpty()) {
            return;
        }
        refreshing = true;
        lastFetch = now;
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> res = HTTP.send(
                        HttpRequest.newBuilder(URI.create(base + "/api/capes")).timeout(Duration.ofSeconds(8)).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    List<Entry> out = new ArrayList<>();
                    JsonArray arr = JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonArray("capes");
                    for (var el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        out.add(new Entry(o.get("id").getAsInt(), o.get("name").getAsString(),
                                o.get("owner").getAsString(), o.get("ownerUuid").getAsString()));
                    }
                    list = out;
                }
            } catch (Exception ignored) {
            } finally {
                refreshing = false;
            }
        }, "aero-capes-list");
        t.setDaemon(true);
        t.start();
    }

    /** Downloads a custom cape's texture into aero-capes/ the first time it is drawn; never retried after that within a session. */
    public static void ensureDownloaded(String capeId) {
        if (!capeId.startsWith("custom_") || DOWNLOADING.contains(capeId) || ATTEMPTED.contains(capeId)) {
            return;
        }
        String base = AeroApi.base();
        if (base.isEmpty()) {
            return;
        }
        DOWNLOADING.add(capeId);
        String num = capeId.substring("custom_".length());
        Thread t = new Thread(() -> {
            try {
                HttpResponse<byte[]> res = HTTP.send(
                        HttpRequest.newBuilder(URI.create(base + "/api/capes/" + num + ".png")).timeout(Duration.ofSeconds(10)).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() == 200 && res.body().length > 16) {
                    Files.createDirectories(CapeTextures.folder());
                    Files.write(CapeTextures.folder().resolve(capeId + ".png"), res.body());
                }
            } catch (Exception ignored) {
            } finally {
                DOWNLOADING.remove(capeId);
                ATTEMPTED.add(capeId);
            }
        }, "aero-cape-dl-" + capeId);
        t.setDaemon(true);
        t.start();
    }

    /** Reads a 64x32 PNG from aero-capes/&lt;fileName&gt;.png and uploads it as a new community cape. */
    public static void publish(String fileName, Consumer<Boolean> done) {
        if (publishing) {
            return;
        }
        String token = AeroApi.authToken();
        String base = AeroApi.base();
        if (token == null || base.isEmpty()) {
            status = "Not connected to the Aero server yet.";
            done.accept(false);
            return;
        }
        String trimmed = fileName == null ? "" : fileName.trim();
        if (trimmed.isEmpty()) {
            status = "Type a file name from aero-capes/ first.";
            done.accept(false);
            return;
        }
        String file = trimmed.toLowerCase(java.util.Locale.ROOT).endsWith(".png") ? trimmed : trimmed + ".png";
        Path path = CapeTextures.folder().resolve(file);
        if (!Files.isRegularFile(path)) {
            status = "No " + file + " in aero-capes/.";
            done.accept(false);
            return;
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            status = "Couldn't read that file.";
            done.accept(false);
            return;
        }
        try (var in = Files.newInputStream(path)) {
            NativeImage img = NativeImage.read(in);
            int w = img.getWidth();
            int h = img.getHeight();
            img.close();
            if (w != 64 || h != 32) {
                status = "Cape must be 64x32 pixels (this one is " + w + "x" + h + ").";
                done.accept(false);
                return;
            }
        } catch (IOException e) {
            status = "That's not a readable PNG.";
            done.accept(false);
            return;
        }
        String name = file.substring(0, file.length() - 4);
        publishing = true;
        status = "Publishing...";
        Thread t = new Thread(() -> {
            try {
                String url = base + "/api/capes?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
                HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                                .header("content-type", "image/png").header("authorization", "Bearer " + token)
                                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    int id = JsonParser.parseString(res.body()).getAsJsonObject().get("id").getAsInt();
                    try {
                        Files.copy(path, CapeTextures.folder().resolve("custom_" + id + ".png"), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignored) {
                    }
                    status = "Published!";
                    refreshNow();
                    done.accept(true);
                } else {
                    status = errorFrom(res.body(), res.statusCode());
                    done.accept(false);
                }
            } catch (Exception e) {
                status = "Upload failed: " + e.getClass().getSimpleName();
                done.accept(false);
            } finally {
                publishing = false;
            }
        }, "aero-cape-publish");
        t.setDaemon(true);
        t.start();
    }

    /** Deletes one of the player's own published capes. */
    public static void delete(int id, Runnable done) {
        String token = AeroApi.authToken();
        String base = AeroApi.base();
        if (token == null || base.isEmpty()) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/capes/" + id)).timeout(Duration.ofSeconds(8))
                        .header("authorization", "Bearer " + token).DELETE().build(), HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {
            } finally {
                refreshNow();
                done.run();
            }
        }, "aero-cape-delete");
        t.setDaemon(true);
        t.start();
    }

    private static String errorFrom(String body, int status) {
        try {
            return JsonParser.parseString(body).getAsJsonObject().get("error").getAsString();
        } catch (Exception e) {
            return "Server rejected it (" + status + ")";
        }
    }
}
