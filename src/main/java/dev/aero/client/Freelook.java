package dev.aero.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Freelook (like the Perspective mod): while the key is held (or toggled) the camera orbits your character
 * in third person and the mouse moves only the camera - where you walk and aim stays put.
 */
public final class Freelook {
    private static boolean active;
    private static boolean keyWasDown;
    private static float yaw;
    private static float pitch;
    private static Perspective before;

    private Freelook() {}

    public static boolean active() {
        return active;
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }

    public static void tick(MinecraftClient mc) {
        var c = AeroClient.CONFIG;
        boolean enabled = c != null && c.freelook && mc.player != null && mc.getWindow() != null;
        boolean down = enabled && mc.currentScreen == null && c.freelookKey >= 0
                && GLFW.glfwGetKey(mc.getWindow().getHandle(), c.freelookKey) == GLFW.GLFW_PRESS;
        boolean want;
        if (!enabled) {
            want = false;
        } else if (c.freelookToggle) {
            want = down && !keyWasDown ? !active : active;
        } else {
            want = down;
        }
        keyWasDown = down;
        if (want && !active) {
            active = true;
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
            before = mc.options.getPerspective();
            if (before.isFirstPerson()) {
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
        } else if (!want && active) {
            active = false;
            if (before != null) {
                mc.options.setPerspective(before);
            }
        }
    }

    /** Mouse movement while active: same sensitivity as turning (0.15 degrees per unit, like Entity#changeLookDirection). */
    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }
}
