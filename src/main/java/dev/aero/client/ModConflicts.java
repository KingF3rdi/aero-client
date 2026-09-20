package dev.aero.client;

import dev.aero.client.module.Module;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Other mods that do the same job as an enabled Aero module. Their jars are never touched: on the
 * first world join the player gets one chat line naming them so they can be removed.
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

    private static boolean told;

    private ModConflicts() {}

    /** Names of loaded mods that duplicate a currently enabled Aero module ("mod (Module)"). */
    public static List<String> found(List<Module> modules) {
        List<String> out = new ArrayList<>();
        FabricLoader loader = FabricLoader.getInstance();
        for (Module m : modules) {
            List<String> ids = TABLE.get(m.name);
            if (ids == null || !m.enabled()) {
                continue;
            }
            for (String id : ids) {
                if (loader.isModLoaded(id)) {
                    out.add(id + " (" + m.name + ")");
                }
            }
        }
        return out;
    }

    /** Call every tick; says something once, when a world is loaded. */
    public static void tell(MinecraftClient mc, List<Module> modules) {
        if (told || mc.player == null) {
            return;
        }
        told = true;
        List<String> f = found(modules);
        if (!f.isEmpty()) {
            mc.player.sendMessage(Text.literal("[Aero] Overlapping mods found: " + String.join(", ", f)
                    + ". Remove them from your mods folder to avoid double rendering."), false);
        }
    }
}
