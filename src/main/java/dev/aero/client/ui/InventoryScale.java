package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import net.minecraft.client.gui.screen.Screen;

/**
 * Shared math for the "Inventory Scale" GUI Tweak: makes the survival/creative inventory panel
 * bigger or smaller independent of Minecraft's own GUI Scale option.
 *
 * <p>Mirrors how vanilla GUI Scale itself works: the screen is laid out against a shrunk "virtual"
 * width/height (so the fixed-pixel-size background and slots take up a bigger share of it), then
 * the whole draw is magnified back up with a matrix scale. Mouse coordinates arrive in real,
 * unscaled space and have to be divided by the same factor so clicks land on the slot they appear
 * to be over - every mixin that reads x/y (render's mouseX/mouseY, Click, mouseScrolled,
 * init/resize) has to apply {@link #factor()} consistently or slot hit-testing drifts from what's
 * drawn.
 */
public final class InventoryScale {
    private InventoryScale() {}

    public static boolean applies(Screen screen) {
        var cfg = AeroClient.CONFIG;
        if (cfg == null || screen == null || !cfg.guiTweaks || !cfg.guiInventoryTweaks) {
            return false;
        }
        return Math.abs(factor() - 1f) > 0.001f;
    }

    public static float factor() {
        var cfg = AeroClient.CONFIG;
        if (cfg == null) {
            return 1f;
        }
        return Math.max(0.5f, Math.min(2f, cfg.guiInventoryScale));
    }

    public static int shrink(int real) {
        return Math.max(1, Math.round(real / factor()));
    }
}
