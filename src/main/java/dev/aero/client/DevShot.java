package dev.aero.client;

import dev.aero.client.cosmetic.Cosmetics;
import dev.aero.client.ui.ClickGuiModern;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only test harness. -Daero.shot=1 opens the ClickGUI on several tabs; -Daero.world=1 creates a
 * throwaway demo world and screenshots first/third person views. Saves PNGs to run/shots, then quits.
 */
public final class DevShot {
    private record Step(String name, Runnable setup, int waitTicks) {}

    private static int ticks;
    private static boolean worldStarted;
    private static int worldReady;
    private static int index;
    private static int wait;
    private static boolean primed;
    private static boolean HURT;
    private static final List<Step> STEPS = new ArrayList<>();

    private DevShot() {}

    public static void register() {
        if (System.getProperty("aero.world") != null) {
            buildWorldSteps();
            ClientTickEvents.END_CLIENT_TICK.register(DevShot::worldTick);
        } else if (System.getProperty("aero.shot") != null) {
            ClientTickEvents.END_CLIENT_TICK.register(DevShot::guiTick);
        }
    }

    private static void log(MinecraftClient mc, String msg) {
        try {
            Path f = mc.runDirectory.toPath().resolve("devshot.log");
            Files.writeString(f, msg + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static void shot(MinecraftClient mc, String name) {
        try {
            Path dir = mc.runDirectory.toPath().resolve("shots");
            Files.createDirectories(dir);
            Path out = dir.resolve(name + ".png");
            ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), img -> {
                try {
                    img.writeTo(out);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---- world mode -------------------------------------------------------------------------

    private static void buildWorldSteps() {
        STEPS.add(new Step("fp_default", () -> {
            var mc = MinecraftClient.getInstance();
            mc.player.getInventory().setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_SWORD));
            mc.player.getInventory().setSelectedSlot(0);
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }, 40));
        STEPS.add(new Step("fp_hand_custom", () -> {
            var c = AeroClient.CONFIG;
            c.handTweaks = true;
            c.handStyle = "Custom";
            c.customX = 0.8f;
            c.customY = -0.45f;
            c.mainScale = 1.0f;
        }, 40));
        STEPS.add(new Step("fp_hand_mini", () -> AeroClient.CONFIG.handStyle = "Mini", 40));
        STEPS.add(new Step("tp_damage_on", () -> {
            var mc = MinecraftClient.getInstance();
            AeroClient.CONFIG.handTweaks = false;
            AeroClient.CONFIG.damageTint = true;
            var p = mc.player;
            p.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_HELMET));
            p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_CHESTPLATE));
            p.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_LEGGINGS));
            p.equipStack(net.minecraft.entity.EquipmentSlot.FEET, new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND_BOOTS));
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            HURT = true;
        }, 30));
        STEPS.add(new Step("tp_damage_off", () -> AeroClient.CONFIG.damageTint = false, 30));
        STEPS.add(new Step("tp_hurt_clear", () -> HURT = false, 5));
        STEPS.add(new Step("tp_nametag", () -> {
            var c = AeroClient.CONFIG;
            var mc = MinecraftClient.getInstance();
            c.nametags = true;
            c.ownNametag = true;
            c.nametagPing = true;
            c.nametagYOffset = 0.15f;
            c.totemPopsOnNametag = true;
            Visuals.onTotemPop(mc.player);
            Visuals.onTotemPop(mc.player);
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
        }, 40));
        STEPS.add(new Step("fp_shield", () -> {
            var mc = MinecraftClient.getInstance();
            mc.player.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHIELD));
            mc.options.setPerspective(Perspective.FIRST_PERSON);
        }, 40));
        STEPS.add(new Step("tp_back_cosmetics", () -> {
            var c = AeroClient.CONFIG;
            c.handTweaks = false;
            Cosmetics.equip(Cosmetics.Kind.CAPE, "migrator");
            Cosmetics.equip(Cosmetics.Kind.WINGS, "dragon");
            Cosmetics.equip(Cosmetics.Kind.HEAD, "crown");
            Cosmetics.equip(Cosmetics.Kind.PET, "fox");
            MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }, 60));
        STEPS.add(new Step("tp_front", () -> MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_FRONT), 40));
        STEPS.add(new Step("tp_back_wings_angel_cape", () -> {
            Cosmetics.equip(Cosmetics.Kind.WINGS, "angel");
            Cosmetics.equip(Cosmetics.Kind.CAPE, "none");
            Cosmetics.equip(Cosmetics.Kind.HEAD, "halo");
            Cosmetics.equip(Cosmetics.Kind.PET, "axolotl");
            MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }, 40));
    }

    private static void worldTick(MinecraftClient mc) {
        ticks++;
        if (ticks % 100 == 0) {
            log(mc, "tick " + ticks + " screen=" + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName())
                    + " player=" + (mc.player != null) + " started=" + worldStarted + " ready=" + worldReady + " idx=" + index);
        }
        if (!worldStarted) {
            if (ticks >= 300 && mc.currentScreen instanceof TitleScreen) {
                worldStarted = true;
                var info = new net.minecraft.world.level.LevelInfo("AeroDev" + (System.currentTimeMillis() % 100000),
                        net.minecraft.world.GameMode.CREATIVE, false, net.minecraft.world.Difficulty.PEACEFUL, true,
                        new net.minecraft.world.rule.GameRules(net.minecraft.resource.featuretoggle.FeatureFlags.DEFAULT_ENABLED_FEATURES),
                        net.minecraft.resource.DataConfiguration.SAFE_MODE);
                try {
                    mc.createIntegratedServerLoader().createAndStart("AeroDev" + (System.currentTimeMillis() % 100000), info,
                            net.minecraft.world.gen.GeneratorOptions.DEMO_OPTIONS,
                            net.minecraft.world.gen.WorldPresets::createDemoOptions, new TitleScreen());
                    log(mc, "creating world");
                } catch (Throwable t) {
                    log(mc, "create failed: " + t);
                }
            }
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (worldReady++ < 260) {
            return;
        }
        if (!primed) {
            primed = true;
            STEPS.get(0).setup().run();
            wait = STEPS.get(0).waitTicks();
            return;
        }
        if (HURT && mc.player != null) {
            mc.player.hurtTime = 8;
            mc.player.maxHurtTime = 10;
        }
        if (--wait > 0) {
            return;
        }
        Step cur = STEPS.get(index);
        log(mc, "shot " + cur.name() + " armorBegin=" + DamageTintState.armorBegin + " equipHits=" + DamageTintState.equipHits + " equipHurt=" + DamageTintState.equipHurtHits);
        shot(mc, cur.name());
        index++;
        if (index >= STEPS.size()) {
            mc.scheduleStop();
            return;
        }
        Step next = STEPS.get(index);
        next.setup().run();
        wait = next.waitTicks();
    }

    // ---- gui mode ---------------------------------------------------------------------------

    private static int stage = -1;
    private static ClickGuiModern gui;
    private static final String[] NAMES = {"client_none", "client_sel0", "client_sel_totem", "wardrobe_capes", "wardrobe_wings", "wardrobe_trails", "wardrobe_badges", "friends"};

    private static void guiTick(MinecraftClient mc) {
        if (++ticks < 300) {
            return;
        }
        if (stage == -1) {
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
        shot(mc, NAMES[stage]);
        stage++;
        wait = 40;
        if (stage >= NAMES.length) {
            mc.scheduleStop();
            return;
        }
        switch (stage) {
            case 1 -> gui.debugSelect(0);
            case 2 -> gui.debugSelectByName("Totem Counter");
            case 3 -> {
                Cosmetics.equip(Cosmetics.Kind.CAPE, "migrator");
                gui.debugTab(1, 0);
            }
            case 4 -> gui.debugTab(1, 1);
            case 5 -> gui.debugTab(1, 3);
            case 6 -> gui.debugTab(1, 9);
            case 7 -> gui.debugTab(2, 0);
            default -> {
            }
        }
    }
}
