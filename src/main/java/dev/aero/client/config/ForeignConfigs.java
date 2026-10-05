package dev.aero.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * First start only: takes over the settings of mods that do an Aero module's job (TierTagger, Shield Status,
 * Crosshair Addons, Simple Block Overlay, Totem Counter, Hit Color, Zoomify, AppleSkin, Dynamic FPS), read from
 * their files in config/, so someone coming from a modpack finds Aero set up the same way. Those mods themselves
 * get disabled by ModConflicts.
 */
public final class ForeignConfigs {
    /** Names of the mods whose settings were taken over on this start, for the title screen. */
    public static final List<String> IMPORTED = new ArrayList<>();

    private ForeignConfigs() {}

    public static void apply(ClientConfig c) {
        Path dir = FabricLoader.getInstance().getConfigDir();
        run("TierTagger", () -> tierTagger(c, dir.resolve("tiertagger.toml")));
        run("Shield Status", () -> shieldStatus(c, dir.resolve("shieldstatus.json")));
        run("Crosshair Addons", () -> crosshairAddons(c, dir.resolve("crosshairaddons.json")));
        run("Block Overlay", () -> blockOverlay(c, dir.resolve("simpleblockoverlay.json")));
        run("Totem Counter", () -> totemCounter(c, dir.resolve("totemcounter.toml")));
        run("Hit Color", () -> hitColor(c, dir.resolve("hitcolorplus.json")));
        run("Zoomify", () -> zoomify(c, dir.resolve("zoomify.json")));
        run("AppleSkin", () -> {
            if (!Files.isRegularFile(dir.resolve("appleskin.json5"))) {
                return false;
            }
            c.saturationOverlay = true;
            return true;
        });
        run("Dynamic FPS", () -> {
            if (!Files.isRegularFile(dir.resolve("dynamic_fps.json"))) {
                return false;
            }
            c.unfocusedCpu = true;
            return true;
        });
    }

    private interface Step {
        boolean run() throws Exception;
    }

    private static void run(String name, Step step) {
        try {
            if (step.run()) {
                IMPORTED.add(name);
            }
        } catch (Exception e) {
            System.out.println("[Aero] could not read " + name + " settings: " + e);
        }
    }

    // ---- per mod ----------------------------------------------------------------------------------------

    private static boolean tierTagger(ClientConfig c, Path file) throws Exception {
        Map<String, String> t = toml(file);
        if (t == null) {
            return false;
        }
        c.tierTagger = bool(t, "enabled", c.tierTagger);
        String mode = t.getOrDefault("gameMode", "").toLowerCase(java.util.Locale.ROOT);
        if (List.of("vanilla", "uhc", "pot", "nethop", "smp", "sword", "axe", "mace").contains(mode)) {
            c.tierGamemode = mode;
        }
        c.tierGamemodeIcon = bool(t, "showIcons", c.tierGamemodeIcon);
        c.tierShowTab = bool(t, "playerList", c.tierShowTab);
        return true;
    }

    private static boolean shieldStatus(ClientConfig c, Path file) throws Exception {
        Map<String, JsonElement> o = options(file);
        if (o == null) {
            return false;
        }
        c.shieldTweaks = bool(o, "Mod Enabled", c.shieldTweaks);
        c.shieldOwnOnly = bool(o, "Self State Only", c.shieldOwnOnly);
        c.shieldReady = bool(o, "Custom Enabled Shield Color", c.shieldReady);
        c.shieldBlocking = bool(o, "Custom Using Shield Color", c.shieldBlocking);
        c.shieldDisabled = bool(o, "Custom Disabled Shield Color", c.shieldDisabled);
        Integer ready = color(o.get("Enabled Color"));
        if (ready != null) {
            c.shieldReadyColor = ready | 0xFF000000;
            c.shieldOpacity = Math.round((ready >>> 24) * 100f / 255f);
        }
        Integer using = color(o.get("Using Color"));
        if (using != null) {
            c.shieldBlockingColor = using | 0xFF000000;
        }
        Integer disabled = color(o.get("Disabled Color"));
        if (disabled != null) {
            c.shieldDisabledColor = disabled | 0xFF000000;
        }
        return true;
    }

    private static boolean crosshairAddons(ClientConfig c, Path file) throws Exception {
        Map<String, JsonElement> o = options(file);
        if (o == null) {
            return false;
        }
        c.crosshairAddons = bool(o, "Mod Enabled", c.crosshairAddons);
        c.addonEnvBlend = bool(o, "Environment Blend", c.addonEnvBlend);
        c.addonThirdPerson = bool(o, "Show In Third Person", c.addonThirdPerson);
        c.addonEntity = bool(o, "Entity Indicator Addon Enabled", c.addonEntity);
        c.addonEntityShowAll = bool(o, "Entity Indicator Show All Entities", c.addonEntityShowAll);
        c.addonElytra = bool(o, "Elytra Addon Enabled", c.addonElytra);
        c.addonHitmarker = bool(o, "Hitmarker Addon Enabled", c.addonHitmarker);
        c.addonHitmarkerToggleOnAttack = bool(o, "Toggle Hitmarker On Attack", c.addonHitmarkerToggleOnAttack);
        c.addonHitmarkerStopOnAnimEnd = bool(o, "Hitmarker Stop On Animation End", c.addonHitmarkerStopOnAnimEnd);
        c.addonHitmarkerDuration = integer(o, "Hitmarker Duration", c.addonHitmarkerDuration);
        c.addonShield = bool(o, "Shield Indicator Addon Enabled", c.addonShield);
        c.addonShieldFactorDelay = bool(o, "Shield Indicator Factor Delay", c.addonShieldFactorDelay);
        c.addonShieldBreak = bool(o, "Shield Break Addon Enabled", c.addonShieldBreak);
        c.addonShieldBreakStopOnAnimEnd = bool(o, "Shield Break Stop On Animation End", c.addonShieldBreakStopOnAnimEnd);
        c.addonShieldBreakDuration = integer(o, "Shield Break Duration", c.addonShieldBreakDuration);
        return true;
    }

    private static boolean blockOverlay(ClientConfig c, Path file) throws Exception {
        JsonObject j = json(file);
        if (j == null) {
            return false;
        }
        c.blockOverlay = !bool(j, "disableMod", false);
        c.blockOverlayOutline = !bool(j, "disableOutline", false);
        c.blockOverlayFaces = !bool(j, "disableOverlay", false);
        c.blockOverlayThroughWalls = bool(j, "disableDepth", c.blockOverlayThroughWalls);
        String key = c.blockOverlayOutline ? "solidColor" : "solidColorOverlay";
        if (j.has(key)) {
            c.blockOverlayColor = j.get(key).getAsInt();
        }
        return true;
    }

    private static boolean totemCounter(ClientConfig c, Path file) throws Exception {
        Map<String, String> t = toml(file);
        if (t == null) {
            return false;
        }
        c.totemCounter = bool(t, "displayEnabled", false) || bool(t, "counterEnabled", false);
        c.totemAutoColor = bool(t, "displayColors", c.totemAutoColor);
        return true;
    }

    private static boolean hitColor(ClientConfig c, Path file) throws Exception {
        JsonObject j = json(file);
        if (j == null) {
            return false;
        }
        c.damageTint = bool(j, "isEnabled", c.damageTint);
        Integer col = hex(j.has("entityHitColor") ? j.get("entityHitColor").getAsString() : null);
        if (col != null) {
            c.damageTintColor = col;
        }
        return true;
    }

    private static boolean zoomify(ClientConfig c, Path file) throws Exception {
        JsonObject j = json(file);
        if (j == null) {
            return false;
        }
        c.zoom = true;
        c.zoomInitial = clamp(num(j, "initialZoom", c.zoomInitial), 1, 10);
        c.zoomInTime = clamp(num(j, "zoomInTime", c.zoomInTime), 0, 3);
        c.zoomOutTime = clamp(num(j, "zoomOutTime", c.zoomOutTime), 0, 3);
        c.zoomScroll = bool(j, "scrollZoom", c.zoomScroll);
        c.zoomSteps = num(j, "scrollStepCount", c.zoomSteps);
        c.zoomPerStep = num(j, "zoomPerStep", c.zoomPerStep);
        c.zoomSmooth = num(j, "scrollZoomSmoothness", c.zoomSmooth);
        c.zoomHands = bool(j, "affectHandFov", c.zoomHands);
        c.zoomToggle = j.has("zoomKeyBehaviour") && "toggle".equalsIgnoreCase(j.get("zoomKeyBehaviour").getAsString());
        return true;
    }

    // ---- readers --------------------------------------------------------------------------------------

    private static JsonObject json(Path file) throws Exception {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        JsonElement e = JsonParser.parseString(Files.readString(file));
        return e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    /** Top-level "key = value" lines of a TOML file (sections are not needed here). */
    static Map<String, String> toml(Path file) throws Exception {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        Map<String, String> out = new HashMap<>();
        for (String line : Files.readAllLines(file)) {
            line = line.trim();
            if (line.startsWith("[")) {
                break;
            }
            int eq = line.indexOf('=');
            if (eq > 0 && !line.startsWith("#")) {
                out.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim().replaceAll("^\"|\"$", ""));
            }
        }
        return out;
    }

    /** The "[ {groups: [ {options: [ {name, value} ]} ]} ]" layout Shield Status and Crosshair Addons share. */
    private static Map<String, JsonElement> options(Path file) throws Exception {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        JsonElement root = JsonParser.parseString(Files.readString(file));
        if (!root.isJsonArray()) {
            return null;
        }
        Map<String, JsonElement> out = new HashMap<>();
        for (JsonElement cat : root.getAsJsonArray()) {
            JsonObject co = cat.getAsJsonObject();
            collect(co.getAsJsonArray("options"), out);
            if (co.has("groups")) {
                for (JsonElement g : co.getAsJsonArray("groups")) {
                    collect(g.getAsJsonObject().getAsJsonArray("options"), out);
                }
            }
        }
        return out;
    }

    private static void collect(JsonArray opts, Map<String, JsonElement> out) {
        if (opts == null) {
            return;
        }
        for (JsonElement o : opts) {
            JsonObject oo = o.getAsJsonObject();
            if (oo.has("name") && oo.has("value")) {
                out.putIfAbsent(oo.get("name").getAsString(), oo.get("value"));
            }
        }
    }

    private static boolean bool(Map<String, ?> m, String k, boolean def) {
        Object v = m.get(k);
        if (v instanceof JsonElement e && e.isJsonPrimitive()) {
            return e.getAsBoolean();
        }
        return v instanceof String s ? Boolean.parseBoolean(s) : def;
    }

    private static boolean bool(JsonObject j, String k, boolean def) {
        return j.has(k) && j.get(k).isJsonPrimitive() ? j.get(k).getAsBoolean() : def;
    }

    private static int integer(Map<String, JsonElement> m, String k, int def) {
        JsonElement e = m.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsInt() : def;
    }

    private static float num(JsonObject j, String k, float def) {
        return j.has(k) && j.get(k).isJsonPrimitive() ? j.get(k).getAsFloat() : def;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** {"value": argb, ...} color objects. */
    private static Integer color(JsonElement e) {
        return e != null && e.isJsonObject() && e.getAsJsonObject().has("value") ? e.getAsJsonObject().get("value").getAsInt() : null;
    }

    /** "#AARRGGBB" or "#RRGGBB". */
    static Integer hex(String s) {
        if (s == null || !s.startsWith("#")) {
            return null;
        }
        try {
            long v = Long.parseLong(s.substring(1), 16);
            return (int) (s.length() == 7 ? v | 0xFF000000L : v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
