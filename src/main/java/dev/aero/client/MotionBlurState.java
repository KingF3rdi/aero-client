package dev.aero.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

/**
 * Camera-speed driven radius for the real motion blur (vanilla blur pass run over the world each frame).
 * Driven by angular speed in degrees per second, so it looks the same at 60 and 700 FPS, and it eases in and
 * out over real time instead of per frame.
 */
public final class MotionBlurState {
    private static float lastYaw;
    private static float lastPitch;
    private static long lastNanos;
    private static float speed; // smoothed deg/s
    private static float radius;

    private MotionBlurState() {}

    /** Blur radius to use this frame; 0 = off. */
    public static int radius() {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.motionBlur ? Math.round(radius) : 0;
    }

    public static void update(MinecraftClient mc) {
        var cfg = AeroClient.CONFIG;
        if (cfg == null || !cfg.motionBlur || mc.player == null || "Simple".equalsIgnoreCase(cfg.motionBlurStyle)) {
            radius = 0;
            speed = 0;
            lastNanos = 0;
            return;
        }
        long now = System.nanoTime();
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        if (lastNanos == 0) {
            lastNanos = now;
            lastYaw = yaw;
            lastPitch = pitch;
            return;
        }
        float dt = Math.max(0.0005f, Math.min(0.1f, (now - lastNanos) / 1_000_000_000f));
        lastNanos = now;
        float d = (float) Math.hypot(MathHelper.wrapDegrees(yaw - lastYaw), pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;

        // rise fast, fall over ~80 ms so a quick flick leaves a short trail instead of flickering
        float inst = d / dt;
        float rise = 1f - (float) Math.exp(-dt / 0.02f);
        float fall = 1f - (float) Math.exp(-dt / 0.08f);
        speed += (inst - speed) * (inst > speed ? rise : fall);

        float preset = "High".equalsIgnoreCase(cfg.motionBlurStrength) ? 1.5f : "Low".equalsIgnoreCase(cfg.motionBlurStrength) ? 0.6f : 1f;
        float amount = MathHelper.clamp(cfg.motionBlurAmount, 10, 100) / 50f * preset;
        // ~0 up to 40 deg/s (slow aiming stays sharp), full blur around 900 deg/s
        float t = MathHelper.clamp((speed - 40f) / 860f, 0f, 1f);
        float target = t * t * (3f - 2f * t) * 14f * amount;
        radius = Math.min(24f, target);
        if (radius < 0.6f) {
            radius = 0;
        }
    }

    public static void apply(MinecraftClient mc) {
        if (radius() > 0) {
            mc.gameRenderer.renderBlur();
        }
    }
}
