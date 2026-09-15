package dev.aero.client.ui;

import net.minecraft.client.gui.screen.Screen;

/** Picks the Screen API that exists on this Minecraft build (1.21–1.21.11). */
public final class Menus {
    private Menus() {}

    public static boolean modernInput() {
        return exists("net.minecraft.client.gui.Click")
                && exists("net.minecraft.client.input.KeyInput");
    }

    public static Screen clickGui(Screen parent, boolean pause) {
        if (modernInput()) {
            try {
                return (Screen) Class.forName("dev.aero.client.ui.ClickGuiModern")
                        .getConstructor(Screen.class, boolean.class)
                        .newInstance(parent, pause);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return new ClickGuiScreenLegacy(parent, pause);
    }

    public static boolean isClickGui(Screen screen) {
        return screen instanceof ClickGuiScreenLegacy
                || (screen != null && screen.getClass().getName().endsWith("ClickGuiScreen"));
    }

    public static boolean isPauseOverlay(Screen screen) {
        return screen != null && screen.getClass().getName().contains("PauseMenu");
    }

    private static boolean exists(String name) {
        try {
            Class.forName(name, false, Screen.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
