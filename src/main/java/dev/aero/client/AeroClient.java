package dev.aero.client;

import dev.aero.client.auth.AccountManager;
import dev.aero.client.config.ClientConfig;
import dev.aero.client.social.FriendStore;
import dev.aero.client.hud.HudStats;
import dev.aero.client.hud.OverlayHud;
import dev.aero.client.module.Modules;
import dev.aero.client.ui.Menus;
import dev.aero.client.ui.PauseMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class AeroClient implements ClientModInitializer {
    public static final String MOD_ID = "aero";
    public static final String VERSION = "1.0";
    public static ClientConfig CONFIG = new ClientConfig();
    public static Modules MODULES;

    /**
     * Swaps in a whole different config (e.g. loading a profile). Modules close over the
     * ClientConfig instance they were built with, so MODULES must be rebuilt too - otherwise the
     * GUI would keep toggling the old, now-orphaned config while everything else reads the new one.
     */
    public static void switchConfig(ClientConfig config) {
        CONFIG = config;
        MODULES = new Modules();
        CONFIG.save();
    }

    private static KeyBinding guiKey;
    private static boolean wasAttackDown;
    private static final java.util.Map<String, Boolean> moduleKeyWasDown = new java.util.HashMap<>();

    @Override
    public void onInitializeClient() {
        CONFIG = ClientConfig.load();
        MODULES = new Modules();
        AccountManager.load();
        FriendStore.load();
        guiKey = registerMenuKey();
        registerHud();
        registerCobwebTweaks();
        registerOptimizers();
        WorldOverlayRenderer.register();
        ClientTickEvents.END_CLIENT_TICK.register(AeroClient::tick);
    }

    /** Cobweb Tweaks: recolor/see-through cobwebs via Fabric's own block color + render-layer APIs, no mixin needed. */
    private static void registerCobwebTweaks() {
        try {
            net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                    (state, world, pos, tintIndex) -> {
                        if (CONFIG == null || !CONFIG.cobwebTweaks) {
                            return -1;
                        }
                        return CONFIG.cobwebColor;
                    },
                    net.minecraft.block.Blocks.COBWEB);
        } catch (Throwable ignored) {
        }
    }

    private static void registerOptimizers() {
        try {
            net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
                Optimizer.onUseBlock(player, world, hand, hit);
                return net.minecraft.util.ActionResult.PASS;
            });
        } catch (Throwable ignored) {
        }
    }

    private static KeyBinding registerMenuKey() {
        try {
            Class<?> cat = Class.forName("net.minecraft.client.option.KeyBinding$Category");
            Class<?> id = Class.forName("net.minecraft.util.Identifier");
            Object ident = id.getMethod("of", String.class, String.class).invoke(null, MOD_ID, "main");
            Object category = cat.getMethod("create", id).invoke(null, ident);
            Constructor<?> ctor = KeyBinding.class.getConstructor(
                    String.class, InputUtil.Type.class, int.class, cat);
            KeyBinding binding = (KeyBinding) ctor.newInstance(
                    "key.aero.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category);
            return KeyBindingHelper.registerKeyBinding(binding);
        } catch (Throwable ignored) {
        }
        try {
            Constructor<?> ctor = KeyBinding.class.getConstructor(
                    String.class, InputUtil.Type.class, int.class, String.class);
            KeyBinding binding = (KeyBinding) ctor.newInstance(
                    "key.aero.clickgui",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    "key.category.aero");
            return KeyBindingHelper.registerKeyBinding(binding);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void registerHud() {
        try {
            HudElementRegistry.addLast(Identifier.of(MOD_ID, "overlay"), OverlayHud::render);
            return;
        } catch (Throwable ignored) {
        }
        try {
            Class<?> cb = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback");
            Object event = cb.getField("EVENT").get(null);
            Method register = null;
            for (Method m : event.getClass().getMethods()) {
                if ("register".equals(m.getName()) && m.getParameterCount() == 1) {
                    register = m;
                    break;
                }
            }
            if (register != null) {
                Class<?> fn = register.getParameterTypes()[0];
                Object proxy = Proxy.newProxyInstance(fn.getClassLoader(), new Class<?>[]{fn},
                        (p, method, args) -> {
                            if (args != null && args.length >= 1) {
                                OverlayHud.renderAny(args[0], args.length > 1 ? args[1] : null);
                            }
                            return null;
                        });
                register.invoke(event, proxy);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void tick(MinecraftClient client) {
        try {
            Screen screen = client.currentScreen;
            if (isVanillaPause(screen) && !Menus.isClickGui(screen) && !Menus.isPauseOverlay(screen)) {
                client.setScreen(new PauseMenuScreen());
            }
            if (client.currentScreen == null && client.options != null) {
                boolean down = client.options.attackKey.isPressed();
                if (down && !wasAttackDown) {
                    HudStats.click();
                    if (CONFIG != null && CONFIG.crosshairAddons && CONFIG.addonHitmarker
                            && CONFIG.addonHitmarkerToggleOnAttack) {
                        HudStats.onAttackPress();
                    }
                }
                wasAttackDown = down;
            } else {
                wasAttackDown = false;
            }
            HudStats.tick(client);
            tickModuleToggleKeys(client);
            tickRenderDistanceOverride(client);
            tickUnfocusedCpu(client);
            tickEmoteWheel(client);
            dev.aero.client.Visuals.tickZoom();
            dev.aero.client.Visuals.tickAutoText(client);
            dev.aero.client.cosmetic.CosmeticEffects.tick(client);
            DiscordRpc.tick();
        } catch (Throwable ignored) {
        }
    }

    /** Generic Toggle Key support: works for every module, present or future, with no per-module wiring. */
    private static void tickModuleToggleKeys(MinecraftClient client) {
        if (MODULES == null || client.currentScreen != null || client.getWindow() == null) {
            return;
        }
        long handle = client.getWindow().getHandle();
        for (dev.aero.client.module.Module module : MODULES.all) {
            int key = module.style().toggleKey;
            if (key < 0) {
                continue;
            }
            boolean down;
            try {
                down = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
            } catch (Throwable ignored) {
                continue;
            }
            boolean was = Boolean.TRUE.equals(moduleKeyWasDown.get(module.name));
            if (down && !was) {
                if ("Screenshot".equals(module.name)) {
                    if (module.enabled()) {
                        Screenshots.capture(client);
                    }
                } else if (!"Emotes".equals(module.name)) {
                    module.toggle();
                    CONFIG.save();
                }
            }
            moduleKeyWasDown.put(module.name, down);
        }
    }

    private static final boolean[] emoteKeyWasDown = new boolean[9];

    private static void tickEmoteWheel(MinecraftClient client) {
        if (!dev.aero.client.Visuals.emoteWheelOpen() || client.getWindow() == null) {
            java.util.Arrays.fill(emoteKeyWasDown, false);
            return;
        }
        long handle = client.getWindow().getHandle();
        String[] presets = dev.aero.client.Visuals.EMOTE_PRESETS;
        for (int i = 0; i < presets.length && i < 9; i++) {
            boolean down;
            try {
                down = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS;
            } catch (Throwable ignored) {
                continue;
            }
            if (down && !emoteKeyWasDown[i] && client.getNetworkHandler() != null) {
                try {
                    client.getNetworkHandler().sendChatMessage(presets[i]);
                } catch (Throwable ignored) {
                }
            }
            emoteKeyWasDown[i] = down;
        }
    }

    private static int unfocusedFpsPrev = -1;

    private static void tickUnfocusedCpu(MinecraftClient client) {
        if (CONFIG == null || client.options == null) {
            return;
        }
        try {
            var maxFps = client.options.getMaxFps();
            boolean unfocused = !client.isWindowFocused();
            if (CONFIG.unfocusedCpu && unfocused) {
                if (unfocusedFpsPrev < 0) {
                    unfocusedFpsPrev = maxFps.getValue();
                }
                int wanted = Math.max(5, Math.min(60, CONFIG.unfocusedFps));
                if (maxFps.getValue() != wanted) {
                    maxFps.setValue(wanted);
                }
            } else if (unfocusedFpsPrev >= 0) {
                maxFps.setValue(unfocusedFpsPrev);
                unfocusedFpsPrev = -1;
            }
        } catch (Throwable ignored) {
        }
    }

    private static void tickRenderDistanceOverride(MinecraftClient client) {
        if (CONFIG == null || client.options == null) {
            return;
        }
        try {
            var viewDistance = client.options.getViewDistance();
            if (CONFIG.renderDistanceOverride) {
                if (CONFIG.renderDistancePrev < 0) {
                    CONFIG.renderDistancePrev = viewDistance.getValue();
                }
                int wanted = Math.max(2, Math.min(32, CONFIG.renderDistanceValue));
                if (viewDistance.getValue() != wanted) {
                    viewDistance.setValue(wanted);
                }
            } else if (CONFIG.renderDistancePrev >= 0) {
                viewDistance.setValue(CONFIG.renderDistancePrev);
                CONFIG.renderDistancePrev = -1;
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isVanillaPause(Screen screen) {
        if (screen == null) {
            return false;
        }
        String n = screen.getClass().getName();
        return n.endsWith("GameMenuScreen") || n.endsWith("PauseScreen");
    }

    private static void applyFpsCap(Object window, int fps) {
        try {
            window.getClass().getMethod("setFramerateLimit", int.class).invoke(window, fps);
        } catch (ReflectiveOperationException ignored) {
            try {
                window.getClass().getMethod("setMaxFps", int.class).invoke(window, fps);
            } catch (ReflectiveOperationException ignored2) {
            }
        }
    }
}
