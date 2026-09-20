package dev.aero.client.cosmetic;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Cosmetics {
    public enum Kind { CAPE, WINGS, HEAD, TRAIL, PET, EMOTE, TAG, BADGE, KILL_EFFECT, MACE, SHIELD, NONE }

    public record Item(String id, String name, Kind kind, int color) {}

    /** Alternative colors offered as the dots under each item; index 0 is the item's own color. */
    private static final int[] ALT = {0xFFFF6B9B, 0xFF6BE8FF, 0xFF8CFF6B, 0xFFFFC94D};

    private static final List<Item> ALL = new ArrayList<>();

    static {
        cape("none", "None", 0xFF2A2A32);
        cape("migrator", "Migrator", 0xFFC04040);
        cape("vanilla", "Vanilla", 0xFF4A90C8);
        cape("minecon2011", "Minecon 2011", 0xFFB03030);
        cape("minecon2012", "Minecon 2012", 0xFF6A4A8A);
        cape("minecon2013", "Minecon 2013", 0xFF3A7A4A);
        cape("minecon2015", "Minecon 2015", 0xFFC8A040);
        cape("minecon2016", "Minecon 2016", 0xFF5A6A8A);
        cape("mojang", "Mojang", 0xFFB02020);
        cape("mojang_studios", "Mojang Studios", 0xFFD84A3A);
        cape("cherry", "Cherry Blossom", 0xFFF4C8DC);
        cape("15th", "15th Anniversary", 0xFFE8C878);
        cape("copper", "Copper", 0xFFB06050);
        cape("founders", "Founder's", 0xFF8A6A3A);
        cape("home", "Home", 0xFF5AA0D8);
        cape("menace", "Menace", 0xFF3A3A48);
        cape("purple_heart", "Purple Heart", 0xFF9A50C8);
        cape("yearn", "Yearn", 0xFFC85A8A);
        cape("zombie_horse", "Zombie Horse", 0xFF6A8A4A);
        wings("none", "None", 0xFF2A2A32);
        wings("angel", "Angel", 0xFFF6F2FC);
        wings("dragon", "Dragon", 0xFFB03828);
        wings("fairy", "Fairy", 0xFFFF9BD0);
        wings("aurora", "Aurora", 0xFF4FC8D8);
        wings("aegis", "Aegis", 0xFFB8BCC8);
        wings("phantom", "Phantom", 0xFF5A6A9A);
        wings("feather", "Feather", 0xFFE8E0D0);
        head("none", "None", 0xFF2A2A32);
        head("halo", "Halo", 0xFFFFD86B);
        head("horns", "Horns", 0xFFB04050);
        head("crown", "Crown", 0xFFFFC94D);
        head("cat", "Cat ears", 0xFFD8A070);
        trail("none", "None", 0xFF2A2A32);
        trail("spark", "Spark", 0xFF4F8EFF);
        trail("heart", "Heart", 0xFFFF7BAA);
        trail("snow", "Snow", 0xFFE8F0F8);
        trail("void", "Void", 0xFF9B5BFF);
        trail("gold", "Gold", 0xFFFFC94D);
        trail("magma", "Magma", 0xFFFF6A2A);
        trail("plasma", "Plasma", 0xFFD060FF);
        trail("spirit", "Spirit", 0xFFB8E8F0);
        pet("none", "None", 0xFF2A2A32);
        pet("axolotl", "Axolotl", 0xFFF4A0B8);
        pet("bee", "Bee", 0xFFFFD040);
        pet("fox", "Fox", 0xFFE8802A);
        emote("wave", "Wave", 0xFF4F8EFF);
        emote("clap", "Clap", 0xFFE8C878);
        emote("gg", "GG", 0xFF8CFF6B);
        emote("o7", "o7", 0xFFFF6B9B);
        tag("none", "None", 0xFF2A2A32);
        tag("og", "OG", 0xFFFFC94D);
        tag("star", "Star", 0xFFFFD86B);
        tag("heart", "Heart", 0xFFFF6B9B);
        tag("skull", "Skull", 0xFFC8C8D8);
        tag("bolt", "Bolt", 0xFF6BE8FF);
        badge("none", "None", 0xFF2A2A32);
        badge("staff", "Staff", 0xFF4F8EFF);
        badge("beta", "Beta", 0xFF88C0D0);
        badge("supporter", "Supporter", 0xFFFF6B9B);
        badge("dev", "Dev", 0xFF8CFF6B);
        killEffect("none", "None", 0xFF2A2A32);
        killEffect("spark", "Spark", 0xFF4F8EFF);
        killEffect("ember", "Ember", 0xFFFF7A45);
        killEffect("venom", "Venom", 0xFF7ED957);
        killEffect("lightning", "Lightning", 0xFFE8F0FF);
        killEffect("soul", "Soul", 0xFF6BE8FF);
        killEffect("void", "Void", 0xFF9B5BFF);
        killEffect("totem", "Totem", 0xFF8CE060);
        killEffect("nova", "Nova", 0xFFFF9B4D);
        shield("none", "None", 0xFF2A2A32);
        shield("crimson", "Crimson", 0xFFE04A4A);
        shield("azure", "Azure", 0xFF4F8EFF);
        shield("emerald", "Emerald", 0xFF3CD07A);
        shield("gold", "Gold", 0xFFFFC94D);
        shield("obsidian", "Obsidian", 0xFF5A4A8A);
        shield("ice", "Ice", 0xFF9FE8F8);
        shield("rainbow", "Rainbow", 0xFFFF6BD0);
        shield("spiked", "Spiked", 0xFF26262C);
        mace("none", "None", 0xFF2A2A32);
        mace("slam", "Slam", 0xFFFF9B4D);
        mace("quake", "Quake", 0xFFB8946A);
        mace("thunder", "Thunder", 0xFFE8F0FF);
        mace("crater", "Crater", 0xFF8A6A50);
        mace("nova", "Nova", 0xFFD060FF);
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
            case 5 -> Kind.MACE;
            case 6 -> Kind.PET;
            case 7 -> Kind.EMOTE;
            case 8 -> Kind.TAG;
            case 9 -> Kind.BADGE;
            case 10 -> Kind.SHIELD;
            default -> Kind.CAPE;
        };
    }

    public static boolean hasVariants(Kind kind) {
        return switch (kind) {
            case WINGS, HEAD, TRAIL, KILL_EFFECT, MACE, PET, SHIELD -> true;
            default -> false;
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
            case MACE -> nz(c.equippedMace);
            case SHIELD -> nz(c.equippedShield);
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
            case MACE -> c.equippedMace = value;
            case SHIELD -> c.equippedShield = value;
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

    /** The 5 selectable colors of an item: its own, then the shared alternates. */
    public static int variantColor(Item item, int index) {
        return index <= 0 || index > ALT.length ? item.color : ALT[index - 1];
    }

    public static int variantCount() {
        return ALT.length + 1;
    }

    public static int variantIndex(Kind kind, String id) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null || c.cosmeticVariant == null) {
            return 0;
        }
        return c.cosmeticVariant.getOrDefault(kind.name() + ":" + id, 0);
    }

    public static void setVariant(Kind kind, String id, int index) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null) {
            return;
        }
        if (c.cosmeticVariant == null) {
            c.cosmeticVariant = new java.util.HashMap<>();
        }
        c.cosmeticVariant.put(kind.name() + ":" + id, index);
        c.save();
    }

    /** Color of the equipped item of a kind with its chosen variant applied, or 0 when nothing is equipped. */
    public static int equippedColor(Kind kind) {
        String id = equipped(kind);
        if ("none".equals(id)) {
            return 0;
        }
        Item item = named(kind, id);
        if (kind == Kind.SHIELD && "rainbow".equals(id)) {
            return 0xFF000000 | (java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000L) / 3000f, 0.7f, 1f) & 0xFFFFFF);
        }
        return item == null ? 0 : variantColor(item, variantIndex(kind, id));
    }

    /** Text glyph shown for chat tags and name badges. */
    public static String glyph(Kind kind, String id) {
        if (kind == Kind.BADGE) {
            return switch (id) {
                case "staff" -> "✦";
                case "beta" -> "β";
                case "supporter" -> "♥";
                case "dev" -> "</>";
                default -> "A";
            };
        }
        return switch (id) {
            case "og" -> "OG";
            case "star" -> "★";
            case "heart" -> "♥";
            case "skull" -> "☠";
            case "bolt" -> "⚡";
            default -> "";
        };
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "none" : s;
    }

    private static void cape(String id, String name, int color) { ALL.add(new Item(id, name, Kind.CAPE, color)); }
    private static void wings(String id, String name, int color) { ALL.add(new Item(id, name, Kind.WINGS, color)); }
    private static void head(String id, String name, int color) { ALL.add(new Item(id, name, Kind.HEAD, color)); }
    private static void trail(String id, String name, int color) { ALL.add(new Item(id, name, Kind.TRAIL, color)); }
    private static void pet(String id, String name, int color) { ALL.add(new Item(id, name, Kind.PET, color)); }
    private static void emote(String id, String name, int color) { ALL.add(new Item(id, name, Kind.EMOTE, color)); }
    private static void tag(String id, String name, int color) { ALL.add(new Item(id, name, Kind.TAG, color)); }
    private static void badge(String id, String name, int color) { ALL.add(new Item(id, name, Kind.BADGE, color)); }
    private static void killEffect(String id, String name, int color) { ALL.add(new Item(id, name, Kind.KILL_EFFECT, color)); }
    private static void shield(String id, String name, int color) { ALL.add(new Item(id, name, Kind.SHIELD, color)); }
    private static void mace(String id, String name, int color) { ALL.add(new Item(id, name, Kind.MACE, color)); }
}
