package dev.aero.client.cosmetic;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Cosmetics {
    public enum Kind { CAPE, WINGS, HEAD, TRAIL, PET, EMOTE, TAG, BADGE, KILL_EFFECT, NONE }

    public record Item(String id, String name, Kind kind, int color) {}

    private static final List<Item> ALL = new ArrayList<>();

    static {
        cape("none", "None", 0xFF2A2A32);
        cape("15th", "15th Anniv.", 0xFFE8C878);
        cape("beacon", "Beacon", 0xFFB8E8F0);
        cape("cherry", "Cherry Blossom", 0xFFF4C8DC);
        cape("cobalt", "Cobalt", 0xFF3A4A9A);
        cape("copper", "Copper", 0xFFB06050);
        cape("creeper", "Creeper", 0xFF3CB04A);
        cape("glitch", "Glitch", 0xFF1A1A1A);
        cape("grass", "Grass", 0xFF2E8B4A);
        cape("migrator", "Migrator", 0xFFC04040);
        cape("minecart", "Minecart", 0xFF8B5A2B);
        cape("minecon", "Minecon 2011", 0xFFB03030);
        cape("aurora", "Aurora", 0xFF4F8EFF);
        cape("void", "Void", 0xFF221833);
        wings("none", "None", 0xFF2A2A32);
        wings("angel", "Angel", 0xFFF6F2FC);
        wings("dragon", "Dragon", 0xFF6A3048);
        wings("phantom", "Phantom", 0xFF3A4A68);
        wings("feather", "Feather", 0xFFE8E0D0);
        head("none", "None", 0xFF2A2A32);
        head("halo", "Halo", 0xFFE8C878);
        head("horns", "Horns", 0xFF6A3040);
        head("crown", "Crown", 0xFFE8C878);
        head("cat", "Cat ears", 0xFFD8A070);
        trail("none", "None", 0xFF2A2A32);
        trail("spark", "Spark", 0xFF4F8EFF);
        trail("heart", "Heart", 0xFFF4C8DC);
        trail("snow", "Snow", 0xFFE8F0F8);
        pet("none", "None", 0xFF2A2A32);
        pet("axolotl", "Axolotl", 0xFFF4A0B8);
        pet("bee", "Bee", 0xFFE8C878);
        pet("fox", "Fox", 0xFFE09050);
        emote("wave", "Wave", 0xFF4F8EFF);
        emote("clap", "Clap", 0xFFE8C878);
        tag("none", "None", 0xFF2A2A32);
        tag("og", "OG", 0xFFE8C878);
        badge("none", "None", 0xFF2A2A32);
        badge("staff", "Staff", 0xFF4F8EFF);
        badge("beta", "Beta", 0xFF88C0D0);
        killEffect("none", "None", 0xFF2A2A32);
        killEffect("spark", "Spark", 0xFF4F8EFF);
        killEffect("ember", "Ember", 0xFFFF7A45);
        killEffect("venom", "Venom", 0xFF7ED957);
    }

    private Cosmetics() {}

    public static List<Item> of(Kind kind, String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<Item> out = new ArrayList<>();
        for (Item item : ALL) {
            if (item.kind != kind) {
                continue;
            }
            if (!q.isEmpty() && !item.name.toLowerCase(Locale.ROOT).contains(q) && !item.id.contains(q)) {
                continue;
            }
            out.add(item);
        }
        return out;
    }

    public static Kind kindForTab(int tab) {
        return switch (tab) {
            case 1 -> Kind.WINGS;
            case 2 -> Kind.HEAD;
            case 3 -> Kind.TRAIL;
            case 4 -> Kind.KILL_EFFECT;
            case 5 -> Kind.NONE;
            case 6 -> Kind.PET;
            case 7 -> Kind.EMOTE;
            case 8 -> Kind.TAG;
            case 9 -> Kind.BADGE;
            default -> Kind.CAPE;
        };
    }

    public static String equipped(Kind kind) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null) {
            return "none";
        }
        return switch (kind) {
            case CAPE -> nz(c.equippedCape);
            case WINGS -> nz(c.equippedWings);
            case HEAD -> nz(c.equippedHead);
            case TRAIL -> nz(c.equippedTrail);
            case PET -> nz(c.equippedPet);
            case EMOTE -> nz(c.equippedEmote);
            case TAG -> nz(c.equippedTag);
            case BADGE -> nz(c.equippedBadge);
            case KILL_EFFECT -> nz(c.equippedKillEffect);
            case NONE -> "none";
        };
    }

    public static void equip(Kind kind, String id) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null) {
            return;
        }
        String value = id == null || id.isBlank() ? "none" : id;
        switch (kind) {
            case CAPE -> c.equippedCape = value;
            case WINGS -> c.equippedWings = value;
            case HEAD -> c.equippedHead = value;
            case TRAIL -> c.equippedTrail = value;
            case PET -> c.equippedPet = value;
            case EMOTE -> c.equippedEmote = value;
            case TAG -> c.equippedTag = value;
            case BADGE -> c.equippedBadge = value;
            case KILL_EFFECT -> c.equippedKillEffect = value;
            case NONE -> {
                return;
            }
        }
        c.save();
    }

    public static Item named(Kind kind, String id) {
        for (Item item : ALL) {
            if (item.kind == kind && item.id.equals(id)) {
                return item;
            }
        }
        return null;
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "none" : s;
    }

    private static void cape(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.CAPE, color));
    }

    private static void wings(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.WINGS, color));
    }

    private static void head(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.HEAD, color));
    }

    private static void trail(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.TRAIL, color));
    }

    private static void pet(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.PET, color));
    }

    private static void emote(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.EMOTE, color));
    }

    private static void tag(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.TAG, color));
    }

    private static void badge(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.BADGE, color));
    }

    private static void killEffect(String id, String name, int color) {
        ALL.add(new Item(id, name, Kind.KILL_EFFECT, color));
    }
}
