package dev.aero.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Other mods that do the same job as an Aero module. They are listed on the title screen (see ConflictScreen);
 * disabling one moves its jar into mods/aero-removed once the game has exited, so it can always be put back.
 */
public final class ModConflicts {
    /**
     * Aero module name -> Fabric mod ids of public mods that do the same job. Ids are compared without
     * "-" / "_" and case, so spelling variants match; unknown ids are simply not loaded, so guesses are harmless.
     */
    private static final Map<String, List<String>> TABLE = Map.ofEntries(
            Map.entry("TierTagger", List.of("tiertagger", "mctiers", "pvptiers", "tiers")),
            Map.entry("Optimizer", List.of("marlowcrystal", "marlowscrystaloptimizer", "crystaloptimizer", "anchoroptimizer",
                    "clientsideanchors", "herosanchoroptimizer", "heroselytraoptimizer", "maceoptimizer", "pearloptimizer",
                    "totemoptimizer", "shieldoptimizer", "crossbowoptimizer")),
            Map.entry("Motion Blur", List.of("motionblur")),
            Map.entry("Zoom", List.of("zoomify", "okzoomer", "logicalzoom", "wizoom", "zoom", "simplezoom", "zume")),
            Map.entry("Freelook", List.of("freelook", "perspective", "perspectivemod", "perspectivemodredux", "pmr", "freecam3p")),
            Map.entry("Toggle Sprint", List.of("togglesprint", "togglesneak", "sprinttoggle", "autosprint", "bettersprinting")),
            Map.entry("Fullbright", List.of("fullbright", "gammautils", "brightnessutils", "fullbrightness")),
            Map.entry("Keystrokes", List.of("keystrokes", "keystrokesmod", "keystrokeshud")),
            Map.entry("CPS", List.of("cps", "cpsmod", "cpsdisplay", "cpscounter")),
            Map.entry("FPS", List.of("fpsdisplay", "fpshud", "fpscounter")),
            Map.entry("Coordinates", List.of("coordinates", "coordinateshud", "coordsdisplay", "coordshud")),
            Map.entry("Armor HUD", List.of("armorhud", "armorhudmod", "durabilityviewer", "itemdurability", "durabilitytooltip")),
            Map.entry("Potion HUD", List.of("potionhud", "effecttimerplus", "statuseffectbars", "statuseffecttimer", "potiontimer")),
            Map.entry("Ping", List.of("betterpingdisplay", "pingdisplay", "numericping")),
            Map.entry("Nametags", List.of("nametagtweaks", "betternametags")),
            Map.entry("Totem Counter", List.of("totemcounter", "totempopcounter", "totemcount")),
            Map.entry("Shield Tweaks", List.of("shieldstatus", "shieldfixes", "shieldindicator")),
            Map.entry("Time Changer", List.of("timechanger", "clienttime")),
            Map.entry("Weather Changer", List.of("weatherchanger", "clientweather")),
            Map.entry("Saturation Overlay", List.of("appleskin")),
            Map.entry("Damage Tint", List.of("conttshitcolorx", "hitcolor", "hitcolorx", "damagetint")),
            Map.entry("Death Animation", List.of("nodeathanimation")),
            Map.entry("No Hurtcam", List.of("nohurtcam", "nohurtcamera")),
            Map.entry("Shulker Tooltips", List.of("shulkerboxtooltip", "shulkertooltip")),
            Map.entry("Hitboxes", List.of("hitboxes", "betterhitboxes")),
            Map.entry("Crosshair", List.of("customcrosshair", "crosshairtweaks")),
            Map.entry("Unfocused CPU", List.of("dynamicfps")),
            Map.entry("Discord RPC", List.of("discordrpc", "simplerpc", "customdiscordrpc")),
            Map.entry("Pop Chams", List.of("walksypopchams", "popchams")),
            Map.entry("Transparent Players", List.of("transparentplayers")),
            Map.entry("Sky Changer", List.of("skychanger", "customsky"))
    );

    private static String norm(String id) {
        return id.toLowerCase(java.util.Locale.ROOT).replace("-", "").replace("_", "");
    }

    /** Set once the pre-launch window has dealt with conflicts, so the in-game card doesn't ask again. */
    public static volatile boolean handledBeforeStart;

    /** A loaded mod that duplicates an Aero module. {@code removed} flips once its removal is scheduled. */
    public static final class Conflict {
        public final String name;
        public final String id;
        public final String module;
        public final Path jar;
        public boolean removed;

        public Conflict(String name, String id, String module, Path jar) {
            this.name = name;
            this.id = id;
            this.module = module;
            this.jar = jar;
        }
    }

    private ModConflicts() {}

    /** Every loaded mod that duplicates an Aero module and whose jar can be moved (a plain .jar file). */
    public static List<Conflict> find() {
        java.util.Map<String, String> byId = new java.util.HashMap<>();
        for (var e : TABLE.entrySet()) {
            for (String id : e.getValue()) {
                byId.putIfAbsent(norm(id), e.getKey());
            }
        }
        List<Conflict> out = new ArrayList<>();
        for (ModContainer mc : FabricLoader.getInstance().getAllMods()) {
            String id = mc.getMetadata().getId();
            String module = byId.get(norm(id));
            if (module == null || "aero".equals(id)) {
                continue;
            }
            Path jar = null;
            try {
                Path p = mc.getOrigin().getPaths().get(0);
                if (Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar")) {
                    jar = p;
                }
            } catch (Throwable ignored) {
            }
            final Path found = jar;
            if (found != null && out.stream().noneMatch(o -> o.jar.equals(found))) {
                out.add(new Conflict(mc.getMetadata().getName(), id, module, found));
            }
        }
        out.sort(java.util.Comparator.comparing(c -> c.name.toLowerCase(java.util.Locale.ROOT)));
        return out;
    }

    /** Moves the jar to mods/aero-removed as soon as the game has exited (the running jar is locked until then). */
    public static void remove(Conflict c) {
        if (c.removed) {
            return;
        }
        try {
            Path dir = c.jar.toAbsolutePath().getParent().resolve("aero-removed");
            Files.createDirectories(dir);
            Path target = dir.resolve(c.jar.getFileName());
            try {
                Files.move(c.jar, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Throwable locked) {
                ModUpdater.scheduleSwap(c.jar, target); // still locked: move it once the game has exited
            }
            c.removed = true;
        } catch (Throwable ignored) {
        }
    }
}
