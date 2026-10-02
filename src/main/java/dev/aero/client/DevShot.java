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
            if (System.getProperty("aero.rec") != null) {
                buildRecSteps();
            } else if (System.getProperty("aero.bench") != null) {
                buildBenchSteps();
            } else if (System.getProperty("aero.hats") != null) {
                buildHatSteps();
            } else if (System.getProperty("aero.trails") != null) {
                buildTrailSteps();
            } else {
                buildWorldSteps();
            }
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

    private static boolean WALK;
    private static double flyY;

    /** -Daero.trails=1: walk forward in third person with each trail (and a few wings) equipped. */
    private static void buildTrailSteps() {
        String[] trails = {"rainbow", "steps", "helix", "sakura", "stars", "spark"};
        String[] wings = {"phoenix", "mech", "crystal", "butterfly", "seraph", "neon"};
        for (int i = 0; i < trails.length; i++) {
            String tr = trails[i];
            String wi = wings[i];
            STEPS.add(new Step("trail_" + tr + "_" + wi, () -> {
                var mc = MinecraftClient.getInstance();
                WALK = true;
                mc.options.hudHidden = true;
                Cosmetics.equip(Cosmetics.Kind.CAPE, "none");
                Cosmetics.equip(Cosmetics.Kind.PET, "none");
                Cosmetics.equip(Cosmetics.Kind.TRAIL, tr);
                Cosmetics.equip(Cosmetics.Kind.WINGS, wi);
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }, 50));
        }
    }

    // ---- website preview recording (-Daero.world=1 -Daero.rec=1) ----------------------------------
    // "rec_*" steps save one frame per tick to run/rec/<step>/ (encoded to video afterwards), "still_*" one picture.

    private static int recFrame;
    private static float recYaw;

    private static void recSetup(MinecraftClient mc) {
        var c = AeroClient.CONFIG;
        var p = mc.player;
        c.timeChanger = true;
        c.timePreset = "Sunset";
        c.hitboxes = false;
        mc.options.hudHidden = false;
        mc.options.getInactivityFpsLimit().setValue(net.minecraft.client.option.InactivityFpsLimit.MINIMIZED); // no AFK frame cap
        c.ownNametag = false;
        // clear a meadow around the demo spawn (it is inside a forest); the trees further out stay as a backdrop
        int x = (int) Math.floor(p.getX());
        int z = (int) Math.floor(p.getZ());
        int top = mc.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        p.getAbilities().flying = false;
        p.setPosition(x + 0.5, top, z + 0.5);
        var net = p.networkHandler;
        net.sendChatCommand("fill " + (x - 14) + " " + top + " " + (z - 14) + " " + (x + 14) + " " + (top + 14) + " " + (z + 14) + " air");
        net.sendChatCommand("fill " + (x - 14) + " " + (top - 1) + " " + (z - 14) + " " + (x + 14) + " " + (top - 1) + " " + (z + 14) + " grass_block");
        boolean spot = true;
        log(mc, "rec spot " + spot + " at " + x + "," + top + "," + z);
    }

    private static void buildRecSteps() {
        STEPS.add(new Step("prep", () -> recSetup(MinecraftClient.getInstance()), 40));
        STEPS.add(new Step("rec_hud", () -> {
            var mc = MinecraftClient.getInstance();
            var c = AeroClient.CONFIG;
            c.fpsHud = false; // saving a frame every tick drags the counter down to a number that isn't real
            c.cpsHud = false;
            c.keystrokes = true;
            c.armorHud = true;
            c.potionHud = true;
            c.coordsHud = false;
            c.fpsX = 4;
            c.fpsY = 4;
            c.keystrokesX = 4;
            c.keystrokesY = 4;
            c.potionX = 4;
            c.potionY = 68;
            mc.inGameHud.getChatHud().clear(false); // the meadow /fill messages
            var p = mc.player;
            p.getInventory().setStack(0, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_SWORD));
            p.getInventory().setStack(1, new net.minecraft.item.ItemStack(net.minecraft.item.Items.END_CRYSTAL, 64));
            p.getInventory().setStack(2, new net.minecraft.item.ItemStack(net.minecraft.item.Items.OBSIDIAN, 64));
            p.getInventory().setStack(3, new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLDEN_APPLE, 32));
            p.getInventory().setStack(4, new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENDER_PEARL, 16));
            p.getInventory().setStack(8, new net.minecraft.item.ItemStack(net.minecraft.item.Items.MACE));
            p.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_HELMET));
            p.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_CHESTPLATE));
            p.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_LEGGINGS));
            p.equipStack(net.minecraft.entity.EquipmentSlot.FEET, new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_BOOTS));
            p.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, new net.minecraft.item.ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING));
            p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED, 20 * 90, 1));
            p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.STRENGTH, 20 * 45, 1));
            p.getInventory().setSelectedSlot(0);
            mc.options.setPerspective(Perspective.FIRST_PERSON);
            recYaw = p.getYaw();
            p.setPitch(8f);
        }, 120));
        STEPS.add(new Step("rec_world", () -> {
            var mc = MinecraftClient.getInstance();
            var c = AeroClient.CONFIG;
            c.fpsHud = false;
            c.cpsHud = false;
            c.keystrokes = false;
            c.armorHud = false;
            c.potionHud = false;
            mc.options.hudHidden = true;
            var p = mc.player;
            for (var slot : new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST,
                    net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET}) {
                p.equipStack(slot, net.minecraft.item.ItemStack.EMPTY);
            }
            Cosmetics.equip(Cosmetics.Kind.HEAD, "kasa");
            Cosmetics.equip(Cosmetics.Kind.WINGS, "seraph");
            Cosmetics.equip(Cosmetics.Kind.CAPE, "none");
            Cosmetics.equip(Cosmetics.Kind.TRAIL, "aura");
            Cosmetics.equip(Cosmetics.Kind.PET, "fox");
            mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
            recYaw = p.getYaw();
            p.setPitch(12f);
        }, 140));
        STEPS.add(new Step("rec_menu", () -> {
            var mc = MinecraftClient.getInstance();
            mc.options.hudHidden = false;
            mc.options.setPerspective(Perspective.FIRST_PERSON);
            gui = new ClickGuiModern(null, false);
            mc.setScreen(gui);
            gui.debugSelectByName("Totem Counter");
        }, 130));
        STEPS.add(new Step("rec_wardrobe", () -> {
            Cosmetics.equip(Cosmetics.Kind.WINGS, "none");
            Cosmetics.equip(Cosmetics.Kind.PET, "none");
            Cosmetics.equip(Cosmetics.Kind.HEAD, "kasa");
            gui.debugTab(1, 2);
        }, 140));
        STEPS.add(new Step("still_store", () -> gui.debugTab(1, 101), 30));
        STEPS.add(new Step("still_pause", () -> MinecraftClient.getInstance().setScreen(new dev.aero.client.ui.PauseMenuScreen()), 30));
    }

    private static final String[] REC_MENU = {"Totem Counter", "Keystrokes", "Fullbright", "Freelook", "Chat", "Optimizer"};
    private static final String[] REC_HATS = {"kasa", "wizard", "tophat", "headphones", "santa", "cowboy", "flower"};

    /** Per tick while a rec_ step runs: animate the scene, then save the frame. */
    private static void recTick(MinecraftClient mc, String step) {
        var p = mc.player;
        switch (step) {
            case "rec_hud" -> {
                recYaw += 0.45f;
                p.setYaw(recYaw);
                p.setHeadYaw(recYaw);
            }
            case "rec_world" -> {
                recYaw += 1.6f; // the player turns, so the front camera circles around the cosmetics
                p.setYaw(recYaw);
                p.setBodyYaw(recYaw);
                p.setHeadYaw(recYaw);
                if (recFrame == 70) {
                    Cosmetics.equip(Cosmetics.Kind.HEAD, "wizard");
                    Cosmetics.equip(Cosmetics.Kind.WINGS, "dragon");
                    Cosmetics.equip(Cosmetics.Kind.TRAIL, "rings");
                }
                if (recFrame >= 70 && recFrame % 18 == 0 && p.isOnGround()) {
                    p.jump();
                }
            }
            case "rec_menu" -> {
                if (recFrame > 0 && recFrame % 22 == 0) {
                    gui.debugSelectByName(REC_MENU[(recFrame / 22) % REC_MENU.length]);
                }
            }
            case "rec_wardrobe" -> {
                gui.debugWardrobeYaw(200f + recFrame * 2.6f);
                if (recFrame > 0 && recFrame % 20 == 0) {
                    Cosmetics.equip(Cosmetics.Kind.HEAD, REC_HATS[(recFrame / 20) % REC_HATS.length]);
                }
            }
            default -> {
            }
        }
        try {
            Path dir = mc.runDirectory.toPath().resolve("rec").resolve(step);
            Files.createDirectories(dir);
            Path out = dir.resolve(String.format("%04d.png", recFrame));
            ScreenshotRecorder.takeScreenshot(mc.getFramebuffer(), img -> {
                try {
                    img.writeTo(out);
                } catch (Exception ignored) {
                } finally {
                    img.close();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        recFrame++;
    }

    /** -Daero.hats=1: third-person shots of hats with the aura and jump-ring trails. */
    private static void buildHatSteps() {
        String[][] looks = {{"kasa", "aura"}, {"tophat", "rings"}, {"santa", "aura"}, {"cowboy", "rings"}};
        for (String[] look : looks) {
            STEPS.add(new Step("hat_" + look[0] + "_" + look[1], () -> {
                var mc = MinecraftClient.getInstance();
                mc.options.hudHidden = true;
                Cosmetics.equip(Cosmetics.Kind.CAPE, "none");
                Cosmetics.equip(Cosmetics.Kind.WINGS, "none");
                Cosmetics.equip(Cosmetics.Kind.PET, "none");
                Cosmetics.equip(Cosmetics.Kind.HEAD, look[0]);
                Cosmetics.equip(Cosmetics.Kind.TRAIL, look[1]);
                mc.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                }
            }, 14));
        }
    }

    /** -Daero.bench=1: FPS in a world with no menu, then with the client menu open (logged to devshot.log). */
    private static void buildBenchSteps() {
        STEPS.add(new Step("bench_world", () -> {
            var mc = MinecraftClient.getInstance();
            mc.options.getInactivityFpsLimit().setValue(net.minecraft.client.option.InactivityFpsLimit.MINIMIZED); // no AFK cap while benchmarking
            mc.setScreen(null);
        }, Integer.getInteger("aero.benchWorldTicks", 200)));
        STEPS.add(new Step("bench_menu", () -> MinecraftClient.getInstance().setScreen(dev.aero.client.ui.Menus.clickGui(null, false)),
                Integer.getInteger("aero.benchMenuTicks", 200)));
        STEPS.add(new Step("bench_world2", () -> MinecraftClient.getInstance().setScreen(null), 200));
    }

    private static void walk(MinecraftClient mc) {
        var p = mc.player;
        p.setYaw(-60f);
        p.setPitch(20f);
        double nx = p.getX() + Math.sin(Math.toRadians(60)) * 0.22;
        double nz = p.getZ() + Math.cos(Math.toRadians(60)) * 0.22;
        if (flyY == 0) { // above the trees, so the third-person camera never clips into leaves
            flyY = mc.world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(nx), (int) Math.floor(nz)) + 14;
        }
        p.getAbilities().flying = true;
        p.setVelocity(0, 0, 0);
        p.setPosition(nx, flyY, nz);
    }

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
            Cosmetics.equip(Cosmetics.Kind.CAPE, "frost");
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
        if (ticks == 20 && System.getProperty("aero.rec") != null) {
            mc.getWindow().setWindowedSize(1600, 900); // website previews
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
        if (WALK && mc.player != null) {
            walk(mc);
        }
        if (HURT && mc.player != null) {
            mc.player.hurtTime = 8;
            mc.player.maxHurtTime = 10;
        }
        if (STEPS.get(index).name().startsWith("rec_")) {
            recTick(mc, STEPS.get(index).name());
        }
        if (--wait > 0) {
            return;
        }
        recFrame = 0;
        Step cur = STEPS.get(index);
        log(mc, "shot " + cur.name() + " armorBegin=" + DamageTintState.armorBegin + " equipHits=" + DamageTintState.equipHits + " equipHurt=" + DamageTintState.equipHurtHits);
        if (cur.name().startsWith("bench")) {
            log(mc, "BENCH " + cur.name() + " fps=" + mc.getCurrentFps());
        }
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
    private static final String[] NAMES = {"a_home", "a2_morph", "b_card_pvp", "c_search", "d_wardrobe", "d2_wings", "w_bat", "w_butterfly", "w_mech", "w_crystal",
            "w_neon", "w_phoenix", "w_seraph", "w_phantom", "x_kasa", "x_wizard", "x_tophat", "x_santa", "x_headphones", "d3_trails", "e_store", "f_rewards",
            "g_badges", "h_friends", "i_profiles", "j_title", "k_conflicts"};

    private static void packsTick(MinecraftClient mc) {
        if (ticks == 300) {
            mc.setScreen(new net.minecraft.client.gui.screen.option.OptionsScreen(new TitleScreen(), mc.options));
        } else if (ticks == 330) {
            for (var child : mc.currentScreen.children()) {
                if (child instanceof net.minecraft.client.gui.widget.ButtonWidget b && b.getMessage().getString().startsWith("Resource Packs")) {
                    b.onPress(null);
                    log(mc, "pressed Resource Packs");
                }
            }
        } else if (ticks == 400) {
            log(mc, "packs screen=" + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getName()));
            shot(mc, "l_packs");
        } else if (ticks == 440) {
            mc.scheduleStop();
        }
    }

    private static void guiTick(MinecraftClient mc) {
        if (++ticks == 20 && System.getProperty("aero.big") != null) {
            mc.getWindow().setWindowedSize(1600, 900);
        }
        if (ticks < 300) {
            return;
        }
        if (System.getProperty("aero.packs") != null) { // Options -> Resource Packs, as a player clicks it
            packsTick(mc);
            return;
        }
        if (stage == -1) {
            String tk = System.getProperty("aero.token");
            if (tk != null) {
                dev.aero.client.social.AeroApi.devToken(tk);
            }
            dev.aero.client.social.Shards.refresh(true);
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
        String step = NAMES[stage];
        if (step.startsWith("x_")) { // hats
            Cosmetics.equip(Cosmetics.Kind.WINGS, "none");
            Cosmetics.equip(Cosmetics.Kind.HEAD, step.substring(2));
            gui.debugTab(1, 2);
            return;
        }
        if (step.startsWith("w_")) {
            Cosmetics.equip(Cosmetics.Kind.WINGS, step.substring(2));
            gui.debugTab(1, 1);
            return;
        }
        switch (step) {
            case "a2_morph" -> { // mid-animation: the PvP card growing into the panel
                gui.debugSelectByName("Totem Counter");
                wait = 1;
            }
            case "b_card_pvp" -> { } // the a2_morph panel, settled
            case "c_search" -> gui.debugSearch("totem");
            case "d_wardrobe" -> gui.debugTab(1, 0);
            case "d2_wings" -> {
                Cosmetics.equip(Cosmetics.Kind.WINGS, System.getProperty("aero.wings", "dragon"));
                gui.debugTab(1, 1);
            }
            case "d3_trails" -> {
                Cosmetics.equip(Cosmetics.Kind.TRAIL, "rainbow");
                gui.debugTab(1, 3);
            }
            case "e_store" -> gui.debugTab(1, 101);
            case "f_rewards" -> gui.debugTab(1, 102);
            case "g_badges" -> gui.debugTab(1, 103);
            case "h_friends" -> gui.debugTab(2, 0);
            case "i_profiles" -> mc.setScreen(new dev.aero.client.ui.ProfilesScreen(gui));
            case "j_title" -> mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
            case "k_conflicts" -> {
                java.nio.file.Path p = java.nio.file.Path.of("x.jar");
                mc.setScreen(dev.aero.client.ui.ConflictScreen.preview(mc.currentScreen, java.util.List.of(
                        new ModConflicts.Conflict("Marlow's Crystal Optimizer", "marlowcrystal", "Optimizer", p),
                        new ModConflicts.Conflict("Anchor Optimizer", "anchoroptimizer", "Optimizer", p),
                        new ModConflicts.Conflict("Shield Status", "shieldstatus", "Shield Tweaks", p),
                        new ModConflicts.Conflict("TierTagger", "tier-tagger", "TierTagger", p),
                        new ModConflicts.Conflict("AppleSkin", "appleskin", "Saturation Overlay", p))));
            }
            default -> {
            }
        }
    }
}
