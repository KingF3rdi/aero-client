package dev.aero.client;

import dev.aero.client.module.Module;

/**
 * Rows shown under Optimizers / Shield Tweaks. These are built-in client features inspired by the
 * listed public mods; toggles always work, whether or not the original jar is in the mods folder.
 */
public final class OptimizerMods {
    private OptimizerMods() {}

    public record Entry(String id, String label) {}

    public static final Entry[] SUPPORTED = {
            new Entry("marlowcrystal", "Crystal"),
            new Entry("client_side_anchors", "Anchor"),
            new Entry("herosanchoroptimizer", "Heros Anchor"),
            new Entry("heroselytraoptimizer", "Heros Elytra"),
            new Entry("maceoptimizer", "Mace"),
            new Entry("pearloptimizer", "Pearl"),
            new Entry("totemoptimizer", "Totem"),
            new Entry("shieldoptimizer", "Shield"),
            new Entry("crossbowoptimizer", "Crossbow"),
    };

    public static final Entry[] SHIELD = {
            new Entry("shieldstatus", "Shield Status"),
            new Entry("shieldfixes", "Shield Fixes"),
    };

    public static final Entry[] TOTEM_COUNTER = {
            new Entry("totemcounter", "TotemCounter"),
    };

    public static final Entry[] POP_CHAMS = {
            new Entry("walksypopchams", "Pop Chams (Walksy)"),
    };

    public static boolean isLoaded(String modId) {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void attach(Module module, Entry[] entries) {
        for (Entry entry : entries) {
            module.setting(entry.label(), () -> enabled(entry.id()), v -> setEnabled(entry.id(), v));
        }
    }

    public static boolean enabled(String modId) {
        var cfg = AeroClient.CONFIG;
        if (cfg == null || cfg.optimizerEnabled == null) {
            return true;
        }
        return cfg.optimizerEnabled.getOrDefault(modId, true);
    }

    public static void setEnabled(String modId, boolean value) {
        var cfg = AeroClient.CONFIG;
        if (cfg == null) {
            return;
        }
        if (cfg.optimizerEnabled == null) {
            cfg.optimizerEnabled = new java.util.HashMap<>();
        }
        cfg.optimizerEnabled.put(modId, value);
    }

    public static boolean shieldStatus() {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.shieldTweaks && enabled("shieldstatus");
    }

    public static boolean shieldFixes() {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.shieldTweaks && enabled("shieldfixes");
    }
}
