package dev.aero.client;

import dev.aero.client.module.Module;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Detects public, MIT-licensed companion mods this client can complement. These are separate
 * Fabric mods with their own logic and settings - this class only reports whether each is present
 * in the user's mods folder, it does not reimplement or alter their behavior.
 */
public final class OptimizerMods {
    private OptimizerMods() {}

    public record Entry(String id, String label) {}

    public static final Entry[] SUPPORTED = {
            new Entry("marlowcrystal", "Marlow's Crystal Optimizer"),
            new Entry("herosanchoroptimizer", "HerosAnchorOptimizer"),
            new Entry("consumableoptimizer", "Consumable Optimizer"),
            new Entry("heroselytraoptimizer", "HerosElytraOptimizer"),
            new Entry("oneauras-cart-optimizer", "oneaura's Cart Optimizer"),
            new Entry("client_side_anchors", "Anchor Optimizer (cutebow)"),
            new Entry("pearloptimizer", "Pearl Optimizer (cutebow)"),
            new Entry("totemoptimizer", "Totem Optimizer (cutebow)"),
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
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Adds one toggle per entry, labelled with its detected install state. The switch itself is a
     * real, persisted preference (default on) - not fake control over the other mod's internals -
     * it just lets this row remember whether you want it treated as active.
     */
    public static void attach(Module module, Entry[] entries) {
        for (Entry entry : entries) {
            module.setting(
                    entry.label() + (isLoaded(entry.id()) ? " (installed)" : " (not installed)"),
                    () -> enabled(entry.id()), v -> setEnabled(entry.id(), v));
        }
    }

    public static boolean enabled(String modId) {
        var cfg = dev.aero.client.AeroClient.CONFIG;
        if (cfg == null || cfg.optimizerEnabled == null) {
            return true;
        }
        return cfg.optimizerEnabled.getOrDefault(modId, true);
    }

    public static void setEnabled(String modId, boolean value) {
        var cfg = dev.aero.client.AeroClient.CONFIG;
        if (cfg == null) {
            return;
        }
        if (cfg.optimizerEnabled == null) {
            cfg.optimizerEnabled = new java.util.HashMap<>();
        }
        cfg.optimizerEnabled.put(modId, value);
    }
}
