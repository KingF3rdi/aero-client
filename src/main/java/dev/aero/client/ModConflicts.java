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
 * removing one moves its jar into mods/aero-removed once the game has exited, so it can always be put back.
 */
public final class ModConflicts {
    /** Aero module name -> Fabric mod ids that duplicate it. Unknown ids are simply not loaded, so wrong guesses are harmless. */
    private static final Map<String, List<String>> TABLE = Map.ofEntries(
            Map.entry("TierTagger", List.of("tiertagger", "mctiers", "pvptiers", "tiers")),
            Map.entry("Motion Blur", List.of("motionblur", "motion_blur")),
            Map.entry("Zoom", List.of("zoomify", "ok_zoomer", "logical_zoom", "wi_zoom")),
            Map.entry("Toggle Sprint", List.of("togglesprint", "toggle-sprint", "toggle_sprint")),
            Map.entry("Fullbright", List.of("fullbright", "gammautils")),
            Map.entry("Keystrokes", List.of("keystrokes")),
            Map.entry("CPS", List.of("cps", "cpsmod")),
            Map.entry("Armor HUD", List.of("armorhud", "armor-hud")),
            Map.entry("Potion HUD", List.of("potionhud", "potion-hud", "effecttimerplus")),
            Map.entry("Nametags", List.of("nametagtweaks")),
            Map.entry("Totem Counter", List.of("totemcounter")),
            Map.entry("Shield Tweaks", List.of("shieldstatus", "shieldfixes")),
            Map.entry("Time Changer", List.of("timechanger")),
            Map.entry("Saturation Overlay", List.of("appleskin")),
            Map.entry("Damage Tint", List.of("contts-hitcolorx")),
            Map.entry("Death Animation", List.of("nodeathanimation"))
    );

    /** A loaded mod that duplicates an Aero module. {@code removed} flips once its removal is scheduled. */
    public static final class Conflict {
        public final String name;
        public final String module;
        public final Path jar;
        public boolean removed;

        Conflict(String name, String module, Path jar) {
            this.name = name;
            this.module = module;
            this.jar = jar;
        }
    }

    private ModConflicts() {}

    /** Loaded mods that overlap an Aero module and whose jar can be moved (a plain .jar file). */
    public static List<Conflict> find() {
        List<Conflict> out = new ArrayList<>();
        FabricLoader loader = FabricLoader.getInstance();
        for (var e : TABLE.entrySet()) {
            for (String id : e.getValue()) {
                ModContainer mc = loader.getModContainer(id).orElse(null);
                if (mc == null) {
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
                    out.add(new Conflict(mc.getMetadata().getName(), e.getKey(), found));
                }
            }
        }
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
            ModUpdater.scheduleSwap(c.jar, dir.resolve(c.jar.getFileName()));
            c.removed = true;
        } catch (Throwable ignored) {
        }
    }
}
