package dev.aero.client;

import net.minecraft.client.MinecraftClient;

/** Camera-speed driven radius for the real motion blur (vanilla post-effect blur run over the world each frame). */
public final class MotionBlurState {
    private static float lastYaw;
    private static float lastPitch;
    private static float radius;

    private MotionBlurState() {}

    /** Menu-blur radius to use this frame; 0 = off. */
    public static int radius() {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.motionBlur ? Math.round(radius) : 0;
    }

    public static void update(MinecraftClient mc) {
        var cfg = AeroClient.CONFIG;
        if (cfg == null || !cfg.motionBlur || mc.player == null || "Simple".equalsIgnoreCase(cfg.motionBlurStyle)) {
            radius = 0;
            return;
        }
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        float d = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(yaw - lastYaw)) + Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;
        float max = "High".equalsIgnoreCase(cfg.motionBlurStrength) ? 10f : "Low".equalsIgnoreCase(cfg.motionBlurStrength) ? 3f : 6f;
        float target = Math.min(max, d * 0.6f);
        radius = Math.max(target, radius * 0.8f);
        if (radius < 0.5f) {
            radius = 0;
        }
    }

    public static void apply(MinecraftClient mc) {
        if (radius() > 0) {
            mc.gameRenderer.renderBlur();
        }
    }
}
