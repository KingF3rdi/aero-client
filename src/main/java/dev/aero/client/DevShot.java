package dev.aero.client;

import dev.aero.client.ui.ClickGuiModern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dev-only: with -Daero.shot=1 opens the ClickGUI on several tabs, saves screenshots, then quits. */
public final class DevShot {
    private static int ticks;
    private static int stage = -1;
    private static int wait;
    private static ClickGuiModern gui;

    private DevShot() {}

    public static void register() {
        if (System.getProperty("aero.shot") != null) {
            ClientTickEvents.END_CLIENT_TICK.register(DevShot::tick);
        }
    }

    private static final String[] NAMES = {"client_none", "client_sel0", "client_sel_totem", "wardrobe_capes", "wardrobe_wings", "wardrobe_trails", "wardrobe_badges", "friends"};

    private static void tick(MinecraftClient mc) {
        if (++ticks < 300) {
            return;
        }
        if (stage == -1) {
            System.out.println("[DevShot] opening gui at tick " + ticks + " screen=" + mc.currentScreen);
            gui = new ClickGuiModern(null, false);
            mc.setScreen(gui);
            stage = 0;
            wait = 40;
            return;
        }
        if (--wait > 0) {
            return;
        }
        System.out.println("[DevShot] shot " + NAMES[stage]);
        try {
            Path dir = mc.runDirectory.toPath().resolve("shots");
            Files.createDirectories(dir);
            Path out = dir.resolve(NAMES[stage] + ".png");
            ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), img -> {
                try {
                    img.writeTo(out);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        stage++;
        wait = 40;
        if (stage >= NAMES.length) {
            mc.scheduleStop();
            return;
        }
        switch (stage) {
            case 1 -> gui.debugSelect(0);
            case 2 -> gui.debugSelectByName("Totem Counter");
            case 3 -> gui.debugTab(1, 0);
            case 4 -> gui.debugTab(1, 1);
            case 5 -> gui.debugTab(1, 3);
            case 6 -> gui.debugTab(1, 9);
            case 7 -> gui.debugTab(2, 0);
            default -> {
            }
        }
    }
}
