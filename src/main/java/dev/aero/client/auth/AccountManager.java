package dev.aero.client.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "aero-msa");
        t.setDaemon(true);
        return t;
    });

    public static final AtomicReference<String> status = new AtomicReference<>("");
    public static volatile String userCode = "";
    public static volatile String verifyUrl = "";
    public static volatile boolean busy;
    public static volatile SavedAccount account;

    private AccountManager() {}

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("aero-client-account.json");
    }

    public static void load() {
        try {
            Path path = file();
            if (!Files.isRegularFile(path)) {
                return;
            }
            SavedAccount saved = GSON.fromJson(Files.readString(path), SavedAccount.class);
            if (saved != null && saved.msRefresh != null && !saved.msRefresh.isBlank()) {
                status.set("Session wird erneuert…");
                IO.execute(() -> {
                    try {
                        SavedAccount fresh = MicrosoftAuth.refresh(saved.msRefresh);
                        finish(fresh);
                    } catch (Exception e) {
                        account = saved;
                        SkinPreview.requestOwn();
                        status.set("Gespeicherter Account: " + saved.name + " (Refresh fehlgeschlagen)");
                    }
                });
            } else if (saved != null) {
                account = saved;
                SkinPreview.requestOwn();
            }
        } catch (Exception e) {
            status.set("Account-Datei unlesbar");
        }
    }

    public static String currentName() {
        if (account != null && account.name != null && !account.name.isBlank()) {
            return account.name;
        }
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getSession() != null) {
                return mc.getSession().getUsername();
            }
        } catch (Throwable ignored) {
        }
        return "Nicht angemeldet";
    }

    public static void startLogin() {
        if (busy) {
            return;
        }
        busy = true;
        userCode = "";
        verifyUrl = "";
        status.set("Microsoft Device-Code wird angefordert…");
        IO.execute(() -> {
            try {
                MicrosoftAuth.DeviceCode device = MicrosoftAuth.startDeviceCode();
                userCode = device.userCode;
                verifyUrl = device.verificationUri;
                status.set("Code " + device.userCode + " auf " + device.verificationUri + " eingeben");
                openBrowser(device.verificationUri);
                long deadline = System.currentTimeMillis() + device.expiresIn * 1000L;
                int wait = Math.max(3, device.interval);
                while (System.currentTimeMillis() < deadline) {
                    Thread.sleep(wait * 1000L);
                    var token = MicrosoftAuth.pollToken(device.deviceCode);
                    if (token.has("error")) {
                        String err = token.get("error").getAsString();
                        if ("authorization_pending".equals(err)) {
                            continue;
                        }
                        if ("slow_down".equals(err)) {
                            wait += 2;
                            continue;
                        }
                        throw new IllegalStateException(err + " " + (token.has("error_description")
                                ? token.get("error_description").getAsString() : ""));
                    }
                    SavedAccount logged = MicrosoftAuth.loginWithMsAccessToken(
                            token.get("access_token").getAsString(),
                            token.has("refresh_token") ? token.get("refresh_token").getAsString() : "");
                    finish(logged);
                    return;
                }
                throw new IllegalStateException("Anmeldung abgelaufen. Nochmal versuchen.");
            } catch (Exception e) {
                status.set(e.getMessage() == null ? e.toString() : e.getMessage());
            } finally {
                busy = false;
            }
        });
    }

    public static void logout() {
        account = null;
        userCode = "";
        verifyUrl = "";
        try {
            Files.deleteIfExists(file());
        } catch (Exception ignored) {
        }
        status.set("Abgemeldet");
    }

    private static void finish(SavedAccount logged) throws Exception {
        account = logged;
        Files.writeString(file(), GSON.toJson(logged));
        MinecraftClient.getInstance().execute(() -> applySession(logged));
        SkinPreview.requestOwn();
        status.set("Angemeldet als " + logged.name);
        busy = false;
    }

    public static void applySession(SavedAccount logged) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            // Direct constructor + accessor: class and field names are intermediary in the released jar,
            // so looking them up by name ("...Session") never matched and the switch silently did nothing.
            var session = new net.minecraft.client.session.Session(logged.name, logged.uuid, logged.mcToken,
                    Optional.ofNullable(logged.xuid), Optional.empty());
            var acc = (dev.aero.client.mixin.MinecraftClientSessionAccessor) mc;
            acc.aero$setSession(session);
            acc.aero$setGameProfileFuture(java.util.concurrent.CompletableFuture.completedFuture(null));
            boolean set = true;
            dev.aero.client.social.Shards.reset();
            SkinPreview.requestOwn();
            try {
                mc.getSkinProvider().fetchSkinTextures(mc.getGameProfile());
            } catch (Throwable ignored) {
            }
            status.set(set
                    ? "Angemeldet als " + logged.name + " — für Server neu joinen"
                    : "Account gespeichert: " + logged.name);
        } catch (Throwable t) {
            status.set("Account gespeichert: " + logged.name);
            SkinPreview.requestOwn();
        }
    }

    private static void openBrowser(String url) {
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Throwable ignored) {
            try {
                Util.getOperatingSystem().open(url);
            } catch (Throwable ignored2) {
            }
        }
    }
}
