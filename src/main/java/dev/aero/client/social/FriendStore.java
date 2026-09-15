package dev.aero.client.social;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FriendStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<String> NAMES = new ArrayList<>();

    private FriendStore() {}

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("larp-friends.json");
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
        if (NAMES.isEmpty()) {
            addSilent(ownName());
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

    public static boolean online(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null || name == null || name.isBlank()) {
            return false;
        }
        String want = name.toLowerCase(Locale.ROOT);
        return mc.getNetworkHandler().getPlayerList().stream().anyMatch(e -> {
            try {
                Object profile = e.getProfile();
                if (profile == null) {
                    return false;
                }
                String n = profileName(profile);
                return n != null && want.equals(n.toLowerCase(Locale.ROOT));
            } catch (Throwable ignored) {
                return false;
            }
        });
    }

    private static String profileName(Object profile) {
        try {
            return (String) profile.getClass().getMethod("getName").invoke(profile);
        } catch (Throwable ignored) {
        }
        try {
            return (String) profile.getClass().getMethod("name").invoke(profile);
        } catch (Throwable ignored) {
        }
        return null;
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
