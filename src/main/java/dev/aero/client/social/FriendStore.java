package dev.aero.client.social;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FriendStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> NAMES = new ArrayList<>();
    private static Set<String> onlineLower = Set.of();
    private static long onlineAt;

    private FriendStore() {}

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("aero-friends.json");
    }

    public static void load() {
        NAMES.clear();
        Path file = path();
        if (Files.isRegularFile(file)) {
            try {
                List<String> loaded = GSON.fromJson(Files.readString(file), new TypeToken<List<String>>() {}.getType());
                if (loaded != null) {
                    for (String n : loaded) {
                        addSilent(n);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(NAMES));
        } catch (Exception ignored) {
        }
    }

    public static List<String> all() {
        return List.copyOf(NAMES);
    }

    public static int size() {
        return NAMES.size();
    }

    public static String get(int i) {
        return i >= 0 && i < NAMES.size() ? NAMES.get(i) : "";
    }

    public static boolean add(String raw) {
        if (!addSilent(raw)) {
            return false;
        }
        save();
        return true;
    }

    public static void remove(int i) {
        if (i < 0 || i >= NAMES.size()) {
            return;
        }
        NAMES.remove(i);
        save();
    }

    public static boolean isFriend(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (String n : NAMES) {
            if (n.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The Friends panel calls this once per friend per frame, so it used to redo a reflective
     * getMethod()/invoke() over the whole server player list for every single friend, every
     * frame - O(friends x players) reflection calls, 60+ times a second while the panel is open.
     * PlayerListEntry.getProfile() is a plain GameProfile with a real getName(), no reflection
     * needed, and the whole online set only needs rebuilding a few times a second since tab-list
     * membership doesn't change every frame.
     */
    public static boolean online(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        refreshOnlineIfStale();
        return onlineLower.contains(name.toLowerCase(Locale.ROOT));
    }

    private static void refreshOnlineIfStale() {
        long now = System.currentTimeMillis();
        if (now - onlineAt < 500) {
            return;
        }
        onlineAt = now;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) {
            onlineLower = Set.of();
            return;
        }
        Set<String> names = new HashSet<>();
        for (var entry : mc.getNetworkHandler().getPlayerList()) {
            com.mojang.authlib.GameProfile profile = entry.getProfile();
            if (profile != null && profile.name() != null) {
                names.add(profile.name().toLowerCase(Locale.ROOT));
            }
        }
        onlineLower = names;
    }

    public static String ownName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            if (mc.getSession() != null && mc.getSession().getUsername() != null) {
                return mc.getSession().getUsername();
            }
        } catch (Throwable ignored) {
        }
        return "You";
    }

    private static boolean addSilent(String raw) {
        if (raw == null) {
            return false;
        }
        String name = raw.trim().replaceAll("[^A-Za-z0-9_]", "");
        if (name.length() < 3 || name.length() > 16) {
            return false;
        }
        for (String n : NAMES) {
            if (n.equalsIgnoreCase(name)) {
                return false;
            }
        }
        NAMES.add(name);
        return true;
    }
}
