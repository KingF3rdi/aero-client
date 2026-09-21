package dev.aero.client;

import net.minecraft.util.math.MathHelper;

/** Sky Changer colors: presets or a custom color for the overworld sky, horizon glow and fog. */
public final class SkyFx {
    /** {sky, horizon} RGB per preset. */
    private static int[] preset(String name) {
        return switch (name) {
            case "Sunset" -> new int[]{0xE0663A, 0xFFB15A};
            case "Dusk" -> new int[]{0x3B2A6B, 0xE0668A};
            case "Cotton Candy" -> new int[]{0xF7A8D8, 0xA9D8FF};
            case "Toxic" -> new int[]{0x5BD62B, 0xC9FF4D};
            case "Blood" -> new int[]{0x8E1212, 0xFF4B2B};
            case "Ocean" -> new int[]{0x0E7C9B, 0x6FE3D6};
            case "Midnight" -> new int[]{0x0A0B24, 0x2B2F6B};
            default -> null;
        };
    }

    private SkyFx() {}

    public static boolean on() {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.skyChanger;
    }

    /** RGB of the sky for the current settings. */
    public static int sky() {
        var c = AeroClient.CONFIG;
        if ("Rainbow".equals(c.skyPreset)) {
            return java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 12000L) / 12000f, 0.65f, 1f) & 0xFFFFFF;
        }
        int[] p = preset(c.skyPreset);
        return (p != null ? p[0] : c.skyChangerColor) & 0xFFFFFF;
    }

    /** RGB of the sunrise/sunset glow for the current settings. */
    public static int horizon() {
        var c = AeroClient.CONFIG;
        if ("Rainbow".equals(c.skyPreset)) {
            return java.awt.Color.HSBtoRGB(((System.currentTimeMillis() + 4000L) % 12000L) / 12000f, 0.6f, 1f) & 0xFFFFFF;
        }
        int[] p = preset(c.skyPreset);
        return (p != null ? p[1] : c.skyChangerHorizon) & 0xFFFFFF;
    }

    /** Sky color scaled by the brightness setting (0-100 %). */
    public static int skyScaled() {
        return scale(sky(), AeroClient.CONFIG.skyBrightness / 100f);
    }

    public static int scale(int rgb, float f) {
        f = MathHelper.clamp(f, 0f, 1.5f);
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }
}
