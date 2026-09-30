package dev.aero.client.cosmetic;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;
import dev.aero.client.social.Shards;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Cosmetics {
    public enum Kind { CAPE, WINGS, HEAD, TRAIL, PET, EMOTE, TAG, BADGE, KILL_EFFECT, MACE, NONE }

    public enum Rarity {
        COMMON("Common", 0xFF8A94A6, 150),
        UNCOMMON("Uncommon", 0xFF22C55E, 300),
        RARE("Rare", 0xFF3B82F6, 600),
        LEGENDARY("Legendary", 0xFFF59E0B, 1200);

        public final String label;
        public final int color;
        public final int price;

        Rarity(String label, int color, int price) {
            this.label = label;
            this.color = color;
            this.price = price;
        }
    }

    /**
     * added = release wave, higher is newer (sorting "newest" and the store's New shelf).
     * Capes and the Supporter badge are bought with shards; keep ids in sync with the server CATALOG.
     */
    public record Item(String id, String name, Kind kind, int color, Rarity rarity, int added) {
        public Item(String id, String name, Kind kind, int color) {
            this(id, name, kind, color, Rarity.COMMON, 0);
        }

        /** Server catalog id ("cape:frost"), or null for items that aren't sold. */
        public String catalogId() {
            if ("none".equals(id)) {
                return null;
            }
            if (kind == Kind.CAPE && !id.startsWith("custom_") && !id.startsWith("rank_")) {
                return "cape:" + id;
            }
            if (kind == Kind.BADGE && "supporter".equals(id)) {
                return "badge:supporter";
            }
            return null;
        }
    }

    /** Alternative colors offered as the dots under each item; index 0 is the item's own color. */
    private static final int[] ALT = {0xFFFF6B9B, 0xFF6BE8FF, 0xFF8CFF6B, 0xFFFFC94D};

    private static final List<Item> ALL = new ArrayList<>();

    /** Capes whose texture has several frames (textures/cape/<id>_<n>.png). */
    public static final java.util.Map<String, Integer> ANIMATED = java.util.Map.of("blossom", 8, "samurai", 8, "galaxy", 8);

    static {
        cape("none", "None", 0xFF2A2A32, Rarity.COMMON, 0);
        cape("frost", "Frost", 0xFF9FD8F5, Rarity.COMMON, 1);
        cape("ember", "Ember", 0xFFE8633A, Rarity.COMMON, 1);
        cape("tide", "Tide", 0xFF2E7FD1, Rarity.COMMON, 1);
        cape("checker", "Checker", 0xFF2B2F3A, Rarity.COMMON, 1);
        cape("verdant", "Verdant", 0xFF3FA66B, Rarity.UNCOMMON, 1);
        cape("nightfall", "Nightfall", 0xFF2A2F5E, Rarity.UNCOMMON, 1);
        cape("sunset", "Sunset", 0xFFF08A4B, Rarity.UNCOMMON, 1);
        cape("circuit", "Circuit", 0xFF14B8A6, Rarity.RARE, 1);
        cape("aero", "Aero", 0xFF3B82F6, Rarity.RARE, 1);
        cape("galaxy", "Galaxy", 0xFF6D28D9, Rarity.LEGENDARY, 1);
        cape("katana", "Katana", 0xFF1F2937, Rarity.UNCOMMON, 2);
        cape("kitsune", "Kitsune", 0xFFF97316, Rarity.RARE, 2);
        cape("dragon", "Dragon", 0xFFB91C1C, Rarity.RARE, 2);
        cape("samurai", "Samurai", 0xFFDC2626, Rarity.LEGENDARY, 2);
        cape("blossom", "Blossom", 0xFFF9A8D4, Rarity.LEGENDARY, 2);
        // Rank capes: owned automatically by Owner / Staff / Media / Partner, in the rank colour.
        cape("rank_owner", "Owner Aura", 0xFFF59E0B, Rarity.LEGENDARY, 2);
        cape("rank_staff", "Staff Aura", 0xFF3B82F6, Rarity.LEGENDARY, 2);
        cape("rank_media", "Media Aura", 0xFFEC4899, Rarity.LEGENDARY, 2);
        cape("rank_partner", "Partner Aura", 0xFF10B981, Rarity.LEGENDARY, 2);
        wings("none", "None", 0xFF2A2A32, Rarity.COMMON, 0);
        wings("feather", "Feather", 0xFFE8E0D0, Rarity.COMMON, 2);
        wings("angel", "Angel", 0xFFF6F2FC, Rarity.UNCOMMON, 2);
        wings("fairy", "Fairy", 0xFFFF9BD0, Rarity.UNCOMMON, 2);
        wings("aurora", "Aurora", 0xFF4FC8D8, Rarity.RARE, 2);
        wings("aegis", "Aegis", 0xFFB8BCC8, Rarity.RARE, 2);
        wings("bat", "Bat", 0xFF3A2E40, Rarity.COMMON, 3);
        wings("butterfly", "Butterfly", 0xFF4FA8FF, Rarity.UNCOMMON, 3);
        wings("phantom", "Phantom", 0xFF5A6A9A, Rarity.RARE, 3);
        wings("mech", "Mech", 0xFF4FE8FF, Rarity.RARE, 3);
        wings("crystal", "Crystal", 0xFF9FD8F5, Rarity.RARE, 3);
        wings("neon", "Neon", 0xFFB060FF, Rarity.RARE, 3);
        wings("dragon", "Dragon", 0xFFB03828, Rarity.LEGENDARY, 3);
        wings("phoenix", "Phoenix", 0xFFFF7A2A, Rarity.LEGENDARY, 3);
        wings("seraph", "Seraph", 0xFFF8F6FF, Rarity.LEGENDARY, 3);
        head("none", "None", 0xFF2A2A32);
        head("halo", "Halo", 0xFFFFD86B);
        head("horns", "Horns", 0xFFB04050);
        head("crown", "Crown", 0xFFFFC94D);
        head("cat", "Cat ears", 0xFFD8A070);
        head("kasa", "Rice hat", 0xFFE6DCC2, Rarity.RARE, 4);
        head("tophat", "Top hat", 0xFF24242C, Rarity.UNCOMMON, 4);
        head("wizard", "Wizard hat", 0xFF5B4BC8, Rarity.RARE, 4);
        head("cowboy", "Cowboy hat", 0xFF9A6A3C, Rarity.UNCOMMON, 4);
        head("santa", "Santa hat", 0xFFD8323C, Rarity.UNCOMMON, 4);
        head("party", "Party hat", 0xFFFF6B9B, Rarity.COMMON, 4);
        head("beanie", "Beanie", 0xFF3E7BD6, Rarity.COMMON, 4);
        head("cap", "Cap", 0xFFE0453A, Rarity.COMMON, 4);
        head("headphones", "Headphones", 0xFF4F8EFF, Rarity.RARE, 4);
        head("flower", "Flower crown", 0xFFFF9BC8, Rarity.UNCOMMON, 4);
        head("bunny", "Bunny ears", 0xFFF6F3FB, Rarity.COMMON, 4);
        head("shades", "Shades", 0xFF16181E, Rarity.COMMON, 4);
        trail("none", "None", 0xFF2A2A32, Rarity.COMMON, 0);
        trail("spark", "Spark", 0xFF4F8EFF, Rarity.COMMON, 2);
        trail("heart", "Heart", 0xFFFF7BAA, Rarity.COMMON, 2);
        trail("snow", "Snow", 0xFFE8F0F8, Rarity.COMMON, 2);
        trail("gold", "Gold", 0xFFFFC94D, Rarity.UNCOMMON, 2);
        trail("magma", "Magma", 0xFFFF6A2A, Rarity.UNCOMMON, 2);
        trail("void", "Void", 0xFF9B5BFF, Rarity.RARE, 2);
        trail("plasma", "Plasma", 0xFFD060FF, Rarity.RARE, 2);
        trail("spirit", "Spirit", 0xFFB8E8F0, Rarity.RARE, 2);
        trail("steps", "Footsteps", 0xFF8CE0FF, Rarity.COMMON, 3);
        trail("sakura", "Sakura", 0xFFF9A8D4, Rarity.UNCOMMON, 3);
        trail("helix", "Helix", 0xFF4F8EFF, Rarity.RARE, 3);
        trail("stars", "Stars", 0xFFFFE08A, Rarity.RARE, 3);
        trail("rainbow", "Rainbow", 0xFFFF6B9B, Rarity.LEGENDARY, 3);
        trail("aura", "Aura", 0xFF7FE8FF, Rarity.RARE, 4);
        trail("rings", "Jump rings", 0xFF4F8EFF, Rarity.UNCOMMON, 4);
        pet("none", "None", 0xFF2A2A32);
        pet("axolotl", "Axolotl", 0xFFF4A0B8);
        pet("bee", "Bee", 0xFFFFD040);
        pet("fox", "Fox", 0xFFE8802A);
        emote("wave", "Wave", 0xFF4F8EFF);
        emote("clap", "Clap", 0xFFE8C878);
        emote("gg", "GG", 0xFF8CFF6B);
        emote("o7", "o7", 0xFFFF6B9B);
        badge("none", "None", 0xFF2A2A32, Rarity.COMMON);
        badge("beta", "Beta", 0xFF38BDF8, Rarity.COMMON);
        badge("streak", "On Fire", 0xFFF97316, Rarity.UNCOMMON);
        badge("veteran", "Veteran", 0xFF22C55E, Rarity.RARE);
        badge("collector", "Collector", 0xFFA855F7, Rarity.RARE);
        badge("supporter", "Supporter", 0xFFFF6B9B, Rarity.RARE);
        badge("partner", "Partner", 0xFF10B981, Rarity.LEGENDARY);
        badge("media", "Media", 0xFFEC4899, Rarity.LEGENDARY);
        badge("staff", "Staff", 0xFF3B82F6, Rarity.LEGENDARY);
        badge("dev", "Dev", 0xFF8CFF6B, Rarity.LEGENDARY);
        badge("owner", "Owner", 0xFFF59E0B, Rarity.LEGENDARY);
        killEffect("none", "None", 0xFF2A2A32);
        killEffect("spark", "Spark", 0xFF4F8EFF);
        killEffect("ember", "Ember", 0xFFFF7A45);
        killEffect("venom", "Venom", 0xFF7ED957);
        killEffect("lightning", "Lightning", 0xFFE8F0FF);
        killEffect("soul", "Soul", 0xFF6BE8FF);
        killEffect("void", "Void", 0xFF9B5BFF);
        killEffect("totem", "Totem", 0xFF8CE060);
        killEffect("nova", "Nova", 0xFFFF9B4D);
        mace("none", "None", 0xFF2A2A32);
        mace("slam", "Slam", 0xFFFF9B4D);
        mace("quake", "Quake", 0xFFB8946A);
        mace("thunder", "Thunder", 0xFFE8F0FF);
        mace("crater", "Crater", 0xFF8A6A50);
        mace("nova", "Nova", 0xFFD060FF);
    }

    private Cosmetics() {}

    /** Kill and mace effects are locked for now: shown, but not equippable. */
    public static boolean locked(Kind kind) {
        return kind == Kind.KILL_EFFECT || kind == Kind.MACE;
    }

    public static List<Item> of(Kind kind, String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<Item> out = new ArrayList<>();
        for (Item item : ALL) {
            if (item.kind != kind) {
                continue;
            }
            if (kind == Kind.CAPE && item.id.startsWith("rank_") && !owns(item)) {
                continue; // rank capes only show up for the rank that has them
            }
            if (!q.isEmpty() && !item.name.toLowerCase(Locale.ROOT).contains(q) && !item.id.contains(q)
                    && !item.rarity.label.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(item);
        }
        return out;
    }

    /** Every item of a kind, rank capes included (badges page). */
    public static List<Item> all(Kind kind) {
        List<Item> out = new ArrayList<>();
        for (Item item : ALL) {
            if (item.kind == kind && !"none".equals(item.id)) {
                out.add(item);
            }
        }
        return out;
    }

    /** Published community capes as wardrobe items, for the separate Custom Capes tab. */
    public static List<Item> customCapeItems(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<Item> out = new ArrayList<>();
        for (CustomCapes.Entry e : CustomCapes.list()) {
            Item item = customCapeItem(e);
            if (!q.isEmpty() && !item.name.toLowerCase(Locale.ROOT).contains(q)
                    && !e.ownerName().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(item);
        }
        return out;
    }

    /** A published community cape shown as a normal wardrobe item; label includes the owner unless it's the player's own. */
    private static Item customCapeItem(CustomCapes.Entry e) {
        String label = CustomCapes.isMine(e) ? e.name() : e.name() + " (" + e.ownerName() + ")";
        return new Item(e.capeId(), label, Kind.CAPE, 0xFF3A3A46); // neutral placeholder while the real texture downloads
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
            case 9 -> Kind.BADGE;
            default -> Kind.CAPE;
        };
    }

    public static boolean hasVariants(Kind kind) {
        return switch (kind) {
            case WINGS, HEAD, TRAIL, KILL_EFFECT, MACE, PET -> true;
            default -> false;
        };
    }

    /** Whether the player may equip this item: bought capes, earned badges, rank capes of their rank. */
    public static boolean owns(Item item) {
        if (item == null || "none".equals(item.id)) {
            return true;
        }
        if (locked(item.kind)) {
            return false;
        }
        if (item.kind == Kind.CAPE) {
            if (item.id.startsWith("custom_")) {
                return true;
            }
            if (item.id.startsWith("rank_")) {
                return item.id.equals("rank_" + Shards.rank());
            }
            // Until the server has the shop, every cape is free to wear (nothing could be bought anyway).
            return Shards.shopMissing() || Shards.owns("cape:" + item.id);
        }
        if (item.kind == Kind.BADGE) {
            return Shards.earnedBadge(item.id);
        }
        return true;
    }

    // ---- try-on preview ------------------------------------------------------------------------

    private static Kind previewKind;
    private static String previewId;
    private static long previewAt;

    /**
     * Try-on (hover / buy dialog): the local player shows this item instead of the equipped one. GUI entity
     * previews render after the screen's render call, so the override is kept alive by the wardrobe calling
     * this every frame and simply expires a moment after it stops.
     */
    public static void preview(Kind kind, String id) {
        previewKind = kind;
        previewId = id;
        previewAt = System.currentTimeMillis();
    }

    public static void clearPreview() {
        previewKind = null;
        previewId = null;
    }

    /** What the local player shows for a kind: the try-on item while a GUI preview draws, else the equipped one. */
    public static String shown(Kind kind) {
        if (previewKind == kind && previewId != null && System.currentTimeMillis() - previewAt < 250) {
            return previewId;
        }
        return equipped(kind);
    }

    public static int shownColor(Kind kind) {
        String id = shown(kind);
        if ("none".equals(id)) {
            return 0;
        }
        Item item = named(kind, id);
        return item == null ? 0 : variantColor(item, variantIndex(kind, id));
    }

    public static String equipped(Kind kind) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null) {
            return "none";
        }
        String id = switch (kind) {
            case CAPE -> nz(c.equippedCape);
            case WINGS -> nz(c.equippedWings);
            case HEAD -> nz(c.equippedHead);
            case TRAIL -> nz(c.equippedTrail);
            case PET -> nz(c.equippedPet);
            case EMOTE -> nz(c.equippedEmote);
            case TAG -> "none";
            case BADGE -> nz(c.equippedBadge);
            case KILL_EFFECT -> nz(c.equippedKillEffect);
            case MACE -> nz(c.equippedMace);
            case NONE -> "none";
        };
        if (locked(kind)) {
            return "none";
        }
        // Capes that no longer exist (the old Mojang ones) fall back to none.
        if (kind == Kind.CAPE && !"none".equals(id) && !id.startsWith("custom_") && named(kind, id) == null) {
            return "none";
        }
        return id;
    }

    /** Equips an item; refused (false) for locked kinds and for capes/badges the player doesn't own. */
    public static boolean equip(Kind kind, String id) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null) {
            return false;
        }
        String value = id == null || id.isBlank() ? "none" : id;
        if (!"none".equals(value)) {
            Item item = named(kind, value);
            if (locked(kind) || (item != null && !owns(item))) {
                return false;
            }
        }
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
            case NONE -> {
                return false;
            }
        }
        c.save();
        return true;
    }

    public static Item named(Kind kind, String id) {
        for (Item item : ALL) {
            if (item.kind == kind && item.id.equals(id)) {
                return item;
            }
        }
        if (kind == Kind.CAPE && id != null && id.startsWith("custom_")) {
            CustomCapes.Entry e = CustomCapes.find(id);
            if (e != null) {
                return customCapeItem(e);
            }
            return new Item(id, "Community cape", Kind.CAPE, 0xFF3A3A46);
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
        return item == null ? 0 : variantColor(item, variantIndex(kind, id));
    }

    /** Text glyph shown for name badges. */
    public static String glyph(Kind kind, String id) {
        if (kind == Kind.BADGE) {
            return switch (id) {
                case "staff" -> "✦";
                case "beta" -> "β";
                case "supporter" -> "♥";
                case "dev" -> "</>";
                case "streak" -> "☀";
                case "veteran" -> "⌛";
                case "collector" -> "❖";
                case "media" -> "▶";
                case "partner" -> "✚";
                case "owner" -> "♛";
                default -> "A";
            };
        }
        return "";
    }

    private static String nz(String s) {
        return s == null || s.isBlank() ? "none" : s;
    }

    private static void cape(String id, String name, int color, Rarity rarity, int added) { ALL.add(new Item(id, name, Kind.CAPE, color, rarity, added)); }
    private static void wings(String id, String name, int color, Rarity rarity, int added) { ALL.add(new Item(id, name, Kind.WINGS, color, rarity, added)); }
    private static void head(String id, String name, int color) { ALL.add(new Item(id, name, Kind.HEAD, color)); }
    private static void head(String id, String name, int color, Rarity rarity, int added) { ALL.add(new Item(id, name, Kind.HEAD, color, rarity, added)); }
    private static void trail(String id, String name, int color, Rarity rarity, int added) { ALL.add(new Item(id, name, Kind.TRAIL, color, rarity, added)); }
    private static void pet(String id, String name, int color) { ALL.add(new Item(id, name, Kind.PET, color)); }
    private static void emote(String id, String name, int color) { ALL.add(new Item(id, name, Kind.EMOTE, color)); }
    private static void badge(String id, String name, int color, Rarity rarity) { ALL.add(new Item(id, name, Kind.BADGE, color, rarity, 1)); }
    private static void killEffect(String id, String name, int color) { ALL.add(new Item(id, name, Kind.KILL_EFFECT, color)); }
    private static void mace(String id, String name, int color) { ALL.add(new Item(id, name, Kind.MACE, color)); }
}
