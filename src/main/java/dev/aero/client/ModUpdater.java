package dev.aero.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * In-game self update from the GitHub release (same channel and version stamp as the launcher): the new jar is
 * downloaded next to the running one and swapped in by a detached helper once the game has exited (the running
 * jar is locked on Windows).
 */
public final class ModUpdater {
    public enum State { IDLE, CHECKING, AVAILABLE, DOWNLOADING, READY, UPTODATE, ERROR, DEV }

    private static final String API = "https://api.github.com/repos/KingF3rdi/aero-client-launcher/releases/latest";
    private static final String COUNTED = "https://aero.gamekni9ht.workers.dev/download/mod";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(Duration.ofSeconds(10)).build();

    private static volatile State state = State.IDLE;
    private static String stamp;
    private static String assetUrl;

    private ModUpdater() {}

    public static State state() {
        return state;
    }

    public static String label() {
        return switch (state) {
            case AVAILABLE -> "Update available";
            case DOWNLOADING -> "Downloading...";
            case READY -> "Restart to finish";
            case UPTODATE -> "Up to date";
            case ERROR -> "Retry update";
            case DEV -> "Dev build";
            default -> "Checking...";
        };
    }

    /** Click on the update button: install when one is available, re-check after an error or when current. */
    public static void click() {
        if (state == State.AVAILABLE) {
            install();
        } else if (state == State.ERROR || state == State.UPTODATE) {
            check();
        }
    }

    private static Path jar() {
        try {
            Path p = FabricLoader.getInstance().getModContainer(AeroClient.MOD_ID).orElseThrow()
                    .getOrigin().getPaths().get(0);
            return Files.isRegularFile(p) ? p : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Starts a background check unless one is running or an update is already staged. */
    public static void check() {
        if (state == State.CHECKING || state == State.DOWNLOADING || state == State.READY) {
            return;
        }
        Path jar = jar();
        if (jar == null) {
            state = State.DEV;
            return;
        }
        state = State.CHECKING;
        Thread.ofVirtual().start(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(API)).timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "AeroClient-Mod").build();
                JsonObject rel = JsonParser.parseString(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
                stamp = null;
                for (var a : rel.getAsJsonArray("assets")) {
                    JsonObject o = a.getAsJsonObject();
                    if ("aero-client.jar".equals(o.get("name").getAsString())) {
                        assetUrl = o.get("browser_download_url").getAsString();
                        stamp = o.get("updated_at").getAsString();
                    }
                }
                if (stamp == null) {
                    throw new IllegalStateException("no release jar");
                }
                Path stampFile = jar.resolveSibling(".aero-client.version");
                String have = Files.isRegularFile(stampFile) ? Files.readString(stampFile).trim() : "";
                state = have.equals(stamp) ? State.UPTODATE : State.AVAILABLE;
            } catch (Throwable t) {
                state = State.ERROR;
            }
        });
    }

    /** Downloads the release jar next to the running one and schedules the swap for game exit. */
    public static void install() {
        Path jar = jar();
        if (state != State.AVAILABLE || jar == null) {
            return;
        }
        state = State.DOWNLOADING;
        Thread.ofVirtual().start(() -> {
            try {
                Path tmp = jar.resolveSibling(jar.getFileName() + ".new");
                byte[] data = fetch(COUNTED);
                if (data == null || data.length < 10_000) {
                    data = fetch(assetUrl);
                }
                if (data == null || data.length < 10_000) {
                    throw new IllegalStateException("download failed");
                }
                Files.write(tmp, data);
                Files.writeString(jar.resolveSibling(".aero-client.version"), stamp);
                scheduleSwap(tmp, jar);
                state = State.READY;
            } catch (Throwable t) {
                state = State.ERROR;
            }
        });
    }

    private static byte[] fetch(String url) {
        try {
            HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "AeroClient-Mod").build(), HttpResponse.BodyHandlers.ofByteArray());
            return r.statusCode() == 200 ? r.body() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static void scheduleSwap(Path from, Path to) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                String f = from.toAbsolutePath().toString();
                String t = to.toAbsolutePath().toString();
                ProcessBuilder pb;
                if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                    pb = new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
                            "for($i=0;$i -lt 90;$i++){try{Move-Item -Force -LiteralPath '" + f.replace("'", "''")
                                    + "' -Destination '" + t.replace("'", "''") + "' -ErrorAction Stop;break}catch{Start-Sleep 1}}");
                } else {
                    pb = new ProcessBuilder("sh", "-c", "sleep 2; mv -f \"$0\" \"$1\"", f, t);
                }
                pb.start();
            } catch (Throwable ignored) {
            }
        }, "aero-update-swap"));
    }
}
