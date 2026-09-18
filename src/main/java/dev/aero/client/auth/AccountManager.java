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
            Object session = sessionOf(mc);
            if (session != null) {
                for (String m : new String[]{"getUsername", "getName", "username"}) {
                    try {
                        return String.valueOf(session.getClass().getMethod(m).invoke(session));
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
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
            Object session = createSession(logged);
            if (session == null) {
                status.set("Angemeldet als " + logged.name + " (Session-API nicht gesetzt, nach Relog gilt der Token)");
                return;
            }
            boolean set = false;
            for (Field field : MinecraftClient.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String type = field.getType().getName();
                if (type.endsWith("Session") || type.endsWith(".User")) {
                    field.setAccessible(true);
                    field.set(mc, session);
                    set = true;
                }
                if (type.contains("CompletableFuture") && field.getName().toLowerCase().contains("gameprofile")) {
                    field.setAccessible(true);
                    field.set(mc, java.util.concurrent.CompletableFuture.completedFuture(null));
                }
            }
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

    private static Object createSession(SavedAccount logged) {
        String[] classes = {
                "net.minecraft.client.session.Session",
                "net.minecraft.client.util.Session"
        };
        for (String name : classes) {
            try {
                Class<?> session = Class.forName(name);
                Object type = msaType(session);
                UUID uuid = logged.uuid;
                java.util.Optional<String> xuid = Optional.ofNullable(logged.xuid);
                java.util.Optional<String> empty = Optional.empty();
                for (Constructor<?> ctor : session.getDeclaredConstructors()) {
                    Class<?>[] p = ctor.getParameterTypes();
                    ctor.setAccessible(true);
                    try {
                        if (p.length == 5 && p[0] == String.class && p[1] == UUID.class && p[2] == String.class) {
                            if (p[3] == Optional.class) {
                                return ctor.newInstance(logged.name, uuid, logged.mcToken, xuid, empty);
                            }
                            return ctor.newInstance(logged.name, uuid, logged.mcToken, empty, type);
                        }
                        if (p.length == 6 && p[0] == String.class && p[2] == String.class) {
                            return ctor.newInstance(logged.name, uuid, logged.mcToken,
                                    Optional.ofNullable(logged.xuid), Optional.empty(), type);
                        }
                        if (p.length == 4 && p[0] == String.class && p[1] == String.class) {
                            return ctor.newInstance(logged.name, uuid.toString(), logged.mcToken, type);
                        }
                        if (p.length == 4 && p[0] == String.class) {
                            return ctor.newInstance(logged.name, uuid, logged.mcToken, type);
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object msaType(Class<?> session) {
        for (Class<?> inner : session.getDeclaredClasses()) {
            if (!inner.isEnum()) {
                continue;
            }
            for (Object c : inner.getEnumConstants()) {
                String n = ((Enum) c).name();
                if (n.contains("MSA") || n.contains("MICROSOFT")) {
                    return c;
                }
            }
            Object[] all = inner.getEnumConstants();
            if (all.length > 0) {
                return all[0];
            }
        }
        return null;
    }

    private static Object sessionOf(MinecraftClient mc) {
        try {
            return mc.getSession();
        } catch (Throwable ignored) {
            return null;
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
