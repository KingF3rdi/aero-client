package dev.aero.client.module;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;

import java.util.ArrayList;
import java.util.List;

public final class Modules {
    public final List<Module> all = new ArrayList<>();

    public Modules() {
        ClientConfig c = AeroClient.CONFIG;

        add(new Module("FPS Boost", "Master switch for extra FPS cuts", Category.PERFORMANCE,
                () -> c.fpsBoost, v -> c.fpsBoost = v));
        add(new Module("UI Boost", "Cheaper in-game UI: skips vignette/portal/nausea/pumpkin overlays and toasts", Category.PERFORMANCE,
                () -> c.uiBoost, v -> c.uiBoost = v)
                .setting("Skip screen overlays", () -> c.uiBoostOverlays, v -> c.uiBoostOverlays = v)
                .setting("Skip toasts", () -> c.uiBoostToasts, v -> c.uiBoostToasts = v));
        add(new Module("Max FPS", "Unlocks the frame cap and turns V-Sync off on every start", Category.PERFORMANCE,
                () -> c.autoMaxFps, v -> c.autoMaxFps = v));
        add(new Module("Particle Limit", "Caps world particles for more FPS", Category.PERFORMANCE,
                () -> c.particleLimiter, v -> c.particleLimiter = v)
                .setting("Max", () -> c.maxParticles, v -> c.maxParticles = v, 10, 400));
        add(new Module("No Explosions", "Skip explosion and flash particles", Category.PERFORMANCE,
                () -> c.noExplosions, v -> c.noExplosions = v));
        add(new Module("No Fireworks", "Skip firework particles", Category.PERFORMANCE,
                () -> c.noFireworks, v -> c.noFireworks = v));
        add(new Module("Entity Distance", "Stops rendering far entities", Category.PERFORMANCE,
                () -> c.entityDistance, v -> c.entityDistance = v)
                .setting("Range", () -> c.entityRange, v -> c.entityRange = v, 16, 128));
        add(new Module("No Weather", "Skips rain and thunder rendering", Category.PERFORMANCE,
                () -> c.noWeather, v -> c.noWeather = v));
        add(new Module("No Clouds", "Does not render clouds", Category.PERFORMANCE,
                () -> c.cloudsOff, v -> c.cloudsOff = v));
        add(new Module("No Fog", "Removes water and terrain fog", Category.PERFORMANCE,
                () -> c.noFog, v -> c.noFog = v));
        add(new Module("No Shadows", "Skip entity ground shadows", Category.PERFORMANCE,
                () -> c.noShadows, v -> c.noShadows = v));
        add(new Module("No Glint", "Skip enchantment glint", Category.PERFORMANCE,
                () -> c.noGlint, v -> c.noGlint = v));
        add(new Module("No Beacons", "Skip beacon beam rendering", Category.PERFORMANCE,
                () -> c.noBeacons, v -> c.noBeacons = v));
        add(new Module("Hide Frames", "Skip item frames and paintings", Category.PERFORMANCE,
                () -> c.hideFrames, v -> c.hideFrames = v));
        add(new Module("Hide Falling", "Skip falling-block entities", Category.PERFORMANCE,
                () -> c.hideFalling, v -> c.hideFalling = v));
        add(new Module("Hide XP Orbs", "Skip far experience orbs", Category.PERFORMANCE,
                () -> c.hideXpOrbs, v -> c.hideXpOrbs = v));
        add(new Module("No Lightning", "Skip lightning bolts", Category.PERFORMANCE,
                () -> c.noLightning, v -> c.noLightning = v));
        add(new Module("Unfocused CPU", "Low FPS when the window is not focused", Category.PERFORMANCE,
                () -> c.unfocusedCpu, v -> c.unfocusedCpu = v)
                .setting("FPS", () -> c.unfocusedFps, v -> c.unfocusedFps = v, 5, 60));
        add(new Module("Hide Armor Stands", "Skip armor-stand models", Category.PERFORMANCE,
                () -> c.hideArmorStands, v -> c.hideArmorStands = v));
        add(new Module("Item Limit", "Skip far dropped items", Category.PERFORMANCE,
                () -> c.itemLimiter, v -> c.itemLimiter = v));
        add(new Module("Fast HUD", "Plain HUD without cards and shadows", Category.PERFORMANCE,
                () -> c.fastHud, v -> c.fastHud = v));
        add(new Module("No Bobbing", "No view bob when walking", Category.PERFORMANCE,
                () -> c.noBobbing, v -> c.noBobbing = v));
        add(new Module("No Chat BG", "Skip chat background fill", Category.PERFORMANCE,
                () -> c.noChatBg, v -> c.noChatBg = v));
        add(new Module("Hide Boss Bar", "Hides the boss health bar", Category.PERFORMANCE,
                () -> c.hideBossBar, v -> c.hideBossBar = v));
        add(new Module("Sound Cut", "Mutes weather and ambient noise", Category.PERFORMANCE,
                () -> c.soundCut, v -> c.soundCut = v));
        add(new Module("Render Distance Override", "Forces a lower render distance for more FPS", Category.PERFORMANCE,
                () -> c.renderDistanceOverride, v -> c.renderDistanceOverride = v)
                .setting("Distance", () -> c.renderDistanceValue, v -> c.renderDistanceValue = v, 2, 32));
        add(new Module("Hide Other Players", "Skip rendering everyone except yourself", Category.PERFORMANCE,
                () -> c.hideOtherPlayers, v -> c.hideOtherPlayers = v));
        add(new Module("Hide Dropped Items", "Skip all dropped item entities", Category.PERFORMANCE,
                () -> c.hideDroppedItems, v -> c.hideDroppedItems = v));
        add(new Module("Hide TNT", "Skip primed TNT models", Category.PERFORMANCE,
                () -> c.hideTnt, v -> c.hideTnt = v));
        add(new Module("Hide Projectiles", "Skip arrows, pearls, snowballs", Category.PERFORMANCE,
                () -> c.hideProjectiles, v -> c.hideProjectiles = v));
        add(new Module("Hide Passive Mobs", "Skip cows, villagers, bees and similar", Category.PERFORMANCE,
                () -> c.hidePassiveMobs, v -> c.hidePassiveMobs = v));
        add(new Module("Hide Tile Entities", "Skip chests, signs, banners, skulls", Category.PERFORMANCE,
                () -> c.hideTileEntities, v -> c.hideTileEntities = v));
        add(new Module("Hide Sky", "Skip sky, sun and moon", Category.PERFORMANCE,
                () -> c.hideSky, v -> c.hideSky = v));
        add(new Module("Hide Stars", "Skip star rendering", Category.PERFORMANCE,
                () -> c.hideStars, v -> c.hideStars = v));
        add(new Module("No Break Particles", "Skip block-break dust", Category.PERFORMANCE,
                () -> c.noBreakParticles, v -> c.noBreakParticles = v));
        add(new Module("No Potion Particles", "Skip potion swirl particles", Category.PERFORMANCE,
                () -> c.noPotionParticles, v -> c.noPotionParticles = v));
        add(new Module("No Enchant Particles", "Skip enchanting-table motes", Category.PERFORMANCE,
                () -> c.hideEnchantParticles, v -> c.hideEnchantParticles = v));
        add(new Module("Fast Graphics", "Force Fast graphics for more FPS", Category.PERFORMANCE,
                () -> c.fastGraphics, v -> c.fastGraphics = v));

        add(new Module("Interface Color", "Accent color of the Aero Client menus and HUD", Category.RENDER,
                () -> true, v -> { })
                .settingColor("Accent", () -> c.uiAccent, v -> c.uiAccent = v)
                .setting("Style vanilla UI (hotbar, inventories, buttons)", () -> c.vanillaUi, v -> c.vanillaUi = v));
        add(new Module("Fullbright", "Maximum gamma / night vision look", Category.RENDER,
                () -> c.fullbright, v -> c.fullbright = v)
                .settingF("Brightness", () -> (double) c.brightness, v -> c.brightness = (float) v, 1, 15));
        add(new Module("No Hurtcam", "Removes screen shake when hit", Category.RENDER,
                () -> c.noHurtcam, v -> c.noHurtcam = v));
        add(new Module("Low Fire", "Smaller first-person fire overlay", Category.RENDER,
                () -> c.lowFire, v -> c.lowFire = v));
        add(new Module("No Vignette", "Hides the dark screen vignette", Category.RENDER,
                () -> c.noVignette, v -> c.noVignette = v));
        add(new Module("Hide Scoreboard", "Hides the sidebar scoreboard", Category.RENDER,
                () -> c.hideScoreboard, v -> c.hideScoreboard = v));
        add(new Module("Clear Water", "No underwater overlay tint", Category.RENDER,
                () -> c.cleanWater, v -> c.cleanWater = v));
        add(new Module("Hide Armor", "Hides worn armor models", Category.PLAYER,
                () -> c.hideArmor, v -> c.hideArmor = v)
                .setting("Hide Helmet", () -> c.hideHelmet, v -> c.hideHelmet = v)
                .setting("Hide Chestplate", () -> c.hideChestplate, v -> c.hideChestplate = v)
                .setting("Hide Leggings", () -> c.hideLeggings, v -> c.hideLeggings = v)
                .setting("Hide Boots", () -> c.hideBoots, v -> c.hideBoots = v)
                .setting("Self", () -> c.hideArmorSelf, v -> c.hideArmorSelf = v)
                .setting("Others", () -> c.hideArmorOthers, v -> c.hideArmorOthers = v));
        add(new Module("Clean View", "Hides potion swirls, arrows and sprint dust", Category.PLAYER,
                () -> c.cleanView, v -> c.cleanView = v)
                .setting("Hide Stuck Arrows", () -> c.cleanArrows, v -> c.cleanArrows = v)
                .setting("Hide Fire Overlay", () -> c.hideFireOverlay, v -> c.hideFireOverlay = v)
                .setting("Hide Potion Swirls", () -> c.cleanPotionSwirls, v -> c.cleanPotionSwirls = v)
                .setting("Hide Sprint Dust", () -> c.cleanSprintDust, v -> c.cleanSprintDust = v)
                .setting("Hide Own Hands", () -> c.hideHands, v -> c.hideHands = v)
                .setting("No Swing Animation", () -> c.noSwing, v -> c.noSwing = v));
        add(new Module("Hand Tweaks", "First-person viewmodel offset", Category.RENDER,
                () -> c.handTweaks, v -> c.handTweaks = v)
                .setting("Style", () -> c.handStyle, v -> c.handStyle = v, "Normal", "Custom", "Mini")
                .settingF("Speed", () -> (double) c.handSpeed, v -> c.handSpeed = (float) v, 0.1, 3)
                .setting("Restart instantly", () -> c.handRestart, v -> c.handRestart = v)
                .settingF("Custom X", () -> (double) c.customX, v -> c.customX = (float) v, -2, 2)
                .settingF("Custom Y", () -> (double) c.customY, v -> c.customY = (float) v, -2, 2)
                .settingF("Custom Z", () -> (double) c.customZ, v -> c.customZ = (float) v, -2, 2)
                .settingF("Custom Pitch", () -> (double) c.customPitch, v -> c.customPitch = (float) v, -180, 180)
                .settingF("Custom Yaw", () -> (double) c.customYaw, v -> c.customYaw = (float) v, -180, 180)
                .settingF("Custom Roll", () -> (double) c.customRoll, v -> c.customRoll = (float) v, -180, 180)
                .settingF("Main Scale", () -> (double) c.mainScale, v -> c.mainScale = (float) v, 0.2, 2)
                .settingF("Off Scale", () -> (double) c.offScale, v -> c.offScale = (float) v, 0.2, 2));
        add(new Module("Nametags", "Own-name and nametag scale", Category.RENDER,
                () -> c.nametags, v -> c.nametags = v)
                .settingF("Scale", () -> (double) c.nametagScale, v -> c.nametagScale = (float) v, 0.5, 3)
                .settingF("Height offset", () -> (double) c.nametagYOffset, v -> c.nametagYOffset = (float) v, -1, 2)
                .setting("Show Own Nametag", () -> c.ownNametag, v -> c.ownNametag = v)
                .setting("\"L\" badge for friends", () -> c.nametagBadge, v -> c.nametagBadge = v));
        add(new Module("Client Badge", "Blue A next to Aero Client users in nametags, tab list and chat", Category.RENDER,
                () -> c.badgeEnabled, v -> c.badgeEnabled = v)
                .setting("Who", () -> c.badgeMode, v -> c.badgeMode = v, "All clients", "Friends only")
                .setting("Nametags", () -> c.badgeNametag, v -> c.badgeNametag = v)
                .setting("Tab list", () -> c.badgeTab, v -> c.badgeTab = v)
                .setting("Chat", () -> c.badgeChat, v -> c.badgeChat = v));
        add(new Module("Time Changer", "Client-only world time", Category.RENDER,
                () -> c.timeChanger, v -> c.timeChanger = v)
                .setting("Time", () -> c.timePreset, v -> c.timePreset = v, "Day", "Noon", "Sunset", "Night", "Midnight", "Custom")
                .settingF("Custom ticks", () -> (double) c.customTicks, v -> c.customTicks = (float) v, 0, 24000));
        add(new Module("Weather Changer", "Client-only rain or clear sky", Category.RENDER,
                () -> c.weatherChanger, v -> c.weatherChanger = v)
                .setting("Weather", () -> c.weatherMode, v -> {
                    c.weatherMode = v;
                    c.weatherClear = "Clear".equalsIgnoreCase(v);
                }, "Clear", "Rain", "Thunder"));
        add(new Module("Zoom", "Hold zoom key to zoom the camera", Category.PLAYER,
                () -> c.zoom, v -> c.zoom = v)
                .setting("Zoom Key", () -> c.zoomKey, v -> c.zoomKey = v, 32, 90)
                .setting("Toggle", () -> c.zoomToggle, v -> c.zoomToggle = v)
                .settingF("Initial zoom", () -> (double) c.zoomInitial, v -> c.zoomInitial = (float) v, 1, 10)
                .settingF("Zoom in time", () -> (double) c.zoomInTime, v -> c.zoomInTime = (float) v, 0, 3)
                .settingF("Zoom out time", () -> (double) c.zoomOutTime, v -> c.zoomOutTime = (float) v, 0, 3)
                .setting("Scroll zoom", () -> c.zoomScroll, v -> c.zoomScroll = v)
                .setting("Affect hand FOV", () -> c.zoomHands, v -> c.zoomHands = v));
        add(new Module("Hide Inventory Model", "Hides the player preview in inventory", Category.PLAYER,
                () -> c.hideInvModel, v -> c.hideInvModel = v)
                .setting("Transparent", () -> c.hideInvTransparent, v -> c.hideInvTransparent = v));
        add(new Module("Totem Tweaks", "Resize the totem and its pop", Category.PVP,
                () -> c.totemTweaks, v -> c.totemTweaks = v)
                .settingF("Totem size", () -> (double) c.totemSize, v -> c.totemSize = (float) v, 0.2, 3)
                .tabLast("Totem")
                .settingF("Totem opacity", () -> (double) c.totemOpacity, v -> c.totemOpacity = (float) v, 0, 100)
                .tabLast("Totem")
                .setting("Disable equip animation", () -> c.totemNoEquip, v -> c.totemNoEquip = v)
                .tabLast("Totem")
                .settingF("Pop size", () -> (double) c.totemPopSize, v -> c.totemPopSize = (float) v, 0.2, 3)
                .tabLast("Pop")
                .settingF("Pop duration", () -> (double) c.totemPopDuration, v -> c.totemPopDuration = (float) v, 1, 80)
                .tabLast("Pop")
                .setting("Centre the pop", () -> c.totemPopCentre, v -> c.totemPopCentre = v)
                .tabLast("Pop")
                .settingF("Pop opacity", () -> (double) c.totemPopOpacity, v -> c.totemPopOpacity = (float) v, 0, 100)
                .tabLast("Pop")
                .setting("Disable particles", () -> c.totemPopNoParticles, v -> c.totemPopNoParticles = v)
                .tabLast("Particles")
                .settingF("Particle amount", () -> (double) c.totemPopParticleCount, v -> c.totemPopParticleCount = (float) v, 0, 1)
                .tabLast("Particles"));

        add(new Module("Block Overlay", "Color and fill the block outline", Category.RENDER,
                () -> c.blockOverlay, v -> c.blockOverlay = v)
                .setting("Outline", () -> c.blockOverlayOutline, v -> c.blockOverlayOutline = v)
                .setting("Faces", () -> c.blockOverlayFaces, v -> c.blockOverlayFaces = v)
                .setting("Through walls", () -> c.blockOverlayThroughWalls, v -> c.blockOverlayThroughWalls = v));
        add(new Module("Cobweb Tweaks", "See through and recolor cobwebs", Category.RENDER,
                () -> c.cobwebTweaks, v -> c.cobwebTweaks = v)
                .settingF("Opacity", () -> (double) c.cobwebOpacity, v -> c.cobwebOpacity = (float) v, 0, 100));
        add(new Module("Custom End Crystals", "Restyle the crystal, part by part", Category.RENDER,
                () -> c.customEndCrystals, v -> c.customEndCrystals = v)
                .setting("Mode", () -> c.crystalMode, v -> c.crystalMode = v, "Default", "Flat", "No Frame")
                .settingF("Scale", () -> (double) c.crystalScale, v -> c.crystalScale = (float) v, 0.2, 3)
                .settingF("Rotation speed", () -> (double) c.crystalRotationSpeed, v -> c.crystalRotationSpeed = (float) v, 0, 5)
                .settingF("Bounce height", () -> (double) c.crystalBounceHeight, v -> c.crystalBounceHeight = (float) v, 0, 3)
                .settingF("Bounce speed", () -> (double) c.crystalBounceSpeed, v -> c.crystalBounceSpeed = (float) v, 0, 5)
                .settingF("Height offset", () -> (double) c.crystalHeightOffset, v -> c.crystalHeightOffset = (float) v, -2, 2)
                .settingF("Opacity", () -> (double) c.crystalOpacity, v -> c.crystalOpacity = (float) v, 0, 100));
        add(new Module("Death Animation", "Remove the death flop", Category.RENDER,
                () -> c.deathAnimation, v -> c.deathAnimation = v)
                .settingText("Entities", () -> c.deathAnimationEntities, v -> c.deathAnimationEntities = v)
                .settingF("Death time", () -> (double) c.deathTime, v -> c.deathTime = (float) v, 0, 40)
                .settingF("Overlay time", () -> (double) c.deathOverlayTime, v -> c.deathOverlayTime = (float) v, 0, 40)
                .settingF("Opacity", () -> (double) c.deathOpacity, v -> c.deathOpacity = (float) v, 0, 100));
        add(new Module("Hitboxes", "Boxes on players, crystals and more", Category.RENDER,
                () -> c.hitboxes, v -> c.hitboxes = v)
                .setting("Players", () -> c.hitboxPlayers, v -> c.hitboxPlayers = v)
                .setting("End crystals", () -> c.hitboxEndCrystals, v -> c.hitboxEndCrystals = v)
                .setting("Pearls", () -> c.hitboxPearls, v -> c.hitboxPearls = v)
                .setting("Arrows", () -> c.hitboxArrows, v -> c.hitboxArrows = v)
                .setting("Entities", () -> c.hitboxEntities, v -> c.hitboxEntities = v)
                .setting("Teams", () -> c.hitboxTeams, v -> c.hitboxTeams = v));
        add(new Module("Motion Blur", "Smear the view as it moves", Category.RENDER,
                () -> c.motionBlur, v -> c.motionBlur = v)
                .setting("Style", () -> c.motionBlurStyle, v -> c.motionBlurStyle = v, "Blur", "Simple")
                .setting("Strength", () -> c.motionBlurStrength, v -> c.motionBlurStrength = v, "Low", "Medium", "High"));
        add(new Module("Nostalgia", "Crystals, lighting and glint as they were", Category.RENDER,
                () -> c.nostalgia, v -> c.nostalgia = v)
                .setting("Old Crystals", () -> c.nostalgiaOldCrystals, v -> c.nostalgiaOldCrystals = v)
                .setting("Old Lighting", () -> c.nostalgiaOldLighting, v -> c.nostalgiaOldLighting = v)
                .setting("Old Glint", () -> c.nostalgiaOldGlint, v -> c.nostalgiaOldGlint = v)
                .setting("Old Hurt Camera", () -> c.nostalgiaOldHurtCamera, v -> c.nostalgiaOldHurtCamera = v)
                .setting("Potion Glint", () -> c.nostalgiaPotionGlint, v -> c.nostalgiaPotionGlint = v)
                .setting("Old Potions", () -> c.nostalgiaOldPotions, v -> c.nostalgiaOldPotions = v)
                .setting("Old Walk Animation", () -> c.nostalgiaOldWalk, v -> c.nostalgiaOldWalk = v));
        add(new Module("Pop Chams", "A ghost where a player pops out of view", Category.RENDER,
                () -> c.popChams, v -> c.popChams = v)
                .setting("Show Own Pops", () -> c.popChamsShowOwn, v -> c.popChamsShowOwn = v)
                .setting("Fade Over Time", () -> c.popChamsFadeOverTime, v -> c.popChamsFadeOverTime = v)
                .setting("Disperse Enabled", () -> c.popChamsDisperse, v -> c.popChamsDisperse = v)
                .setting("Filled Model Enabled", () -> c.popChamsFilledModel, v -> c.popChamsFilledModel = v)
                .setting("Wireframe Enabled", () -> c.popChamsWireframe, v -> c.popChamsWireframe = v));
        dev.aero.client.OptimizerMods.attach(all.get(all.size() - 1), dev.aero.client.OptimizerMods.POP_CHAMS);
        add(new Module("Renders", "Hand shader and glowing players", Category.RENDER,
                () -> c.renders, v -> c.renders = v)
                .setting("Hand shader", () -> c.rendersHandShader, v -> c.rendersHandShader = v, "None", "Chrome", "Rainbow")
                .setting("Players glow", () -> c.rendersPlayersGlow, v -> c.rendersPlayersGlow = v)
                .setting("End crystals glow", () -> c.rendersEndCrystalsGlow, v -> c.rendersEndCrystalsGlow = v));
        add(new Module("TierTagger", "PvP tiers on nametags", Category.RENDER,
                () -> c.tierTagger, v -> c.tierTagger = v)
                .setting("Tier list", () -> c.tierList, v -> c.tierList = v, "Mctiers")
                .setting("Gamemode", () -> c.tierGamemode, v -> c.tierGamemode = v,
                        "vanilla", "uhc", "pot", "nethop", "smp", "sword", "axe", "mace")
                .setting("Tier Side", () -> c.tierSide, v -> c.tierSide = v, "Left", "Right")
                .setting("Show in tab", () -> c.tierShowTab, v -> c.tierShowTab = v)
                .setting("Gamemode icon", () -> c.tierGamemodeIcon, v -> c.tierGamemodeIcon = v)
                .setting("Show own", () -> c.tierShowOwn, v -> c.tierShowOwn = v)
                .setting("Show in chat", () -> c.tierShowChat, v -> c.tierShowChat = v));
        add(new Module("Transparent Players", "See-through player models", Category.RENDER,
                () -> c.transparentPlayers, v -> c.transparentPlayers = v)
                .settingF("Body opacity", () -> (double) c.transparentBodyOpacity, v -> c.transparentBodyOpacity = (float) v, 0, 100)
                .settingF("Armor opacity", () -> (double) c.transparentArmorOpacity, v -> c.transparentArmorOpacity = (float) v, 0, 100)
                .setting("Self", () -> c.transparentSelf, v -> c.transparentSelf = v)
                .setting("Only when damaged", () -> c.transparentOnlyDamaged, v -> c.transparentOnlyDamaged = v));

        add(new Module("Crossbow Tweaks", "Show loaded arrow type", Category.PVP,
                () -> c.crossbowTweaks, v -> c.crossbowTweaks = v)
                .setting("In inventory", () -> c.crossbowInInv, v -> c.crossbowInInv = v)
                .expandLast()
                .settingColor("Plain arrow color", () -> c.crossbowArrowColor, v -> c.crossbowArrowColor = v)
                .nestLast("In inventory")
                .setting("In hotbar", () -> c.crossbowInHotbar, v -> c.crossbowInHotbar = v)
                .settingF("Opacity", () -> (double) c.crossbowOpacity, v -> c.crossbowOpacity = (float) v, 0, 100));
        add(new Module("Crosshair", "Draw your own crosshair", Category.PVP,
                () -> c.customCrosshair, v -> c.customCrosshair = v)
                .setting("Style", () -> c.crosshairStyle, v -> c.crosshairStyle = v,
                        "Vanilla", "Cross", "Circle", "Square", "Triangle", "Arrow", "Drawn")
                .settingAction("Draw own", "Edit", () -> {
                    var mc = net.minecraft.client.MinecraftClient.getInstance();
                    if (AeroClient.CONFIG != null) {
                        AeroClient.CONFIG.customCrosshair = true;
                        AeroClient.CONFIG.crosshairStyle = "Drawn";
                        AeroClient.CONFIG.crosshairUseDrawing = true;
                    }
                    mc.setScreen(new dev.aero.client.ui.DrawCrosshairScreen(mc.currentScreen));
                })
                .setting("Length", () -> c.crosshairArm, v -> c.crosshairArm = v, 2, 16)
                .setting("Gap", () -> c.crosshairGap, v -> c.crosshairGap = v, 0, 12)
                .settingF("Thickness", () -> (double) c.crosshairThickness, v -> c.crosshairThickness = (float) v, 0.5, 6)
                .settingColor("Color", () -> c.crosshairColor, v -> c.crosshairColor = v)
                .setting("Outline", () -> c.crosshairOutline, v -> c.crosshairOutline = v)
                .expandLast()
                .settingColor("Outline color", () -> c.crosshairOutlineColor, v -> c.crosshairOutlineColor = v)
                .nestLast("Outline")
                .setting("Center dot", () -> c.crosshairDot, v -> c.crosshairDot = v)
                .expandLast()
                .settingColor("Dot color", () -> c.crosshairDotColor, v -> c.crosshairDotColor = v)
                .nestLast("Center dot")
                .setting("Rainbow", () -> c.crosshairRainbow, v -> c.crosshairRainbow = v)
                .setting("Dynamic attack", () -> c.crosshairDynamicAttack, v -> c.crosshairDynamicAttack = v)
                .setting("Third person", () -> c.crosshairThirdPerson, v -> c.crosshairThirdPerson = v)
                .setting("When HUD hidden", () -> c.crosshairWhenHidden, v -> c.crosshairWhenHidden = v)
                .setting("Cooldown ring", () -> c.crosshairCooldown, v -> c.crosshairCooldown = v)
                .setting("Player Hover", () -> c.crosshairHover, v -> c.crosshairHover = v)
                .expandLast()
                .settingColor("Hover color", () -> c.crosshairHoverColor, v -> c.crosshairHoverColor = v)
                .nestLast("Player Hover")
                .settingF("Hover Range", () -> (double) c.crosshairHoverRange, v -> c.crosshairHoverRange = (float) v, 1, 16)
                .nestLast("Player Hover")
                .setting("Hostile highlight", () -> c.crosshairHighlightHostiles, v -> c.crosshairHighlightHostiles = v)
                .setting("Passive highlight", () -> c.crosshairHighlightPassives, v -> c.crosshairHighlightPassives = v));
        add(new Module("Crosshair Addons", "Markers only — uses vanilla or the Crosshair module", Category.PVP,
                () -> c.crosshairAddons, v -> c.crosshairAddons = v)
                .setting("Icon gap", () -> c.addonGap, v -> c.addonGap = v, 0, 12)
                .setting("Environment blend", () -> c.addonEnvBlend, v -> c.addonEnvBlend = v)
                .setting("Show in third person", () -> c.addonThirdPerson, v -> c.addonThirdPerson = v)
                .setting("Elytra indicator", () -> c.addonElytra, v -> c.addonElytra = v)
                .setting("Entity indicator", () -> c.addonEntity, v -> c.addonEntity = v)
                .setting("Entity: show all entities", () -> c.addonEntityShowAll, v -> c.addonEntityShowAll = v)
                .settingText("Entity: match list (comma, empty = hostile)",
                        () -> c.addonEntityList, v -> c.addonEntityList = v)
                .setting("Shield indicator", () -> c.addonShield, v -> c.addonShield = v)
                .setting("Shield: factor in delay", () -> c.addonShieldFactorDelay, v -> c.addonShieldFactorDelay = v)
                .setting("Shield break", () -> c.addonShieldBreak, v -> c.addonShieldBreak = v)
                .setting("Shield break: stop on anim end", () -> c.addonShieldBreakStopOnAnimEnd, v -> c.addonShieldBreakStopOnAnimEnd = v)
                .setting("Shield break: duration", () -> c.addonShieldBreakDuration, v -> c.addonShieldBreakDuration = v, 1, 40)
                .setting("Hitmarker", () -> c.addonHitmarker, v -> c.addonHitmarker = v)
                .setting("Hitmarker: toggle on attack", () -> c.addonHitmarkerToggleOnAttack, v -> c.addonHitmarkerToggleOnAttack = v)
                .setting("Hitmarker: stop on anim end", () -> c.addonHitmarkerStopOnAnimEnd, v -> c.addonHitmarkerStopOnAnimEnd = v)
                .setting("Hitmarker: duration", () -> c.addonHitmarkerDuration, v -> c.addonHitmarkerDuration = v, 1, 40));
        add(new Module("Auto Text", "Chat message on kill", Category.PLAYER,
                () -> c.autoText, v -> c.autoText = v)
                .settingText("Text", () -> c.autoTextMessage, v -> c.autoTextMessage = v)
                .settingF("Delay (ticks)", () -> (double) c.autoTextDelayTicks, v -> c.autoTextDelayTicks = (float) v, 0, 40)
                .settingF("Cooldown (s)", () -> (double) c.autoTextCooldown, v -> c.autoTextCooldown = (float) v, 0, 30)
                .settingF("Kill Range", () -> (double) c.autoTextKillRange, v -> c.autoTextKillRange = (float) v, 4, 64)
                .settingText("Chat Trigger", () -> c.autoTextChatTrigger, v -> c.autoTextChatTrigger = v));
        add(new Module("Damage Tint", "Tint a hurt player's model (third-person self, or any other player) toward a color - skin, armor and held item all included", Category.PVP,
                () -> c.damageTint, v -> c.damageTint = v)
                .setting("Chroma", () -> c.damageTintChroma, v -> c.damageTintChroma = v)
                .settingF("Speed", () -> (double) c.damageTintSpeed, v -> c.damageTintSpeed = (float) v, 0.05, 2));
        add(new Module("Optimizer", "All PvP lag cuts in one place", Category.PVP,
                () -> c.optimizersModule, v -> c.optimizersModule = v)
                .setting("Skip crystal particles", () -> c.crystalOptimizerParticles, v -> c.crystalOptimizerParticles = v)
                .setting("Mute crystal sound", () -> c.crystalOptimizerSound, v -> c.crystalOptimizerSound = v)
                .setting("Limit crystal range", () -> c.crystalOptimizerRange, v -> c.crystalOptimizerRange = v)
                .setting("Crystal range", () -> c.crystalOptimizerRangeValue, v -> c.crystalOptimizerRangeValue = v, 8, 64)
                .setting("Anchor instant swing", () -> c.anchorOptimizerSwing, v -> c.anchorOptimizerSwing = v)
                .setting("Pearl instant swing", () -> c.pearlOptimizerSwing, v -> c.pearlOptimizerSwing = v)
                .setting("Shield instant raise", () -> c.shieldOptimizerInstant, v -> c.shieldOptimizerInstant = v)
                .setting("Crossbow instant swing", () -> c.crossbowOptimizerSwing, v -> c.crossbowOptimizerSwing = v));
        dev.aero.client.OptimizerMods.attach(all.get(all.size() - 1), dev.aero.client.OptimizerMods.SUPPORTED);
        add(new Module("Shield Tweaks", "Recolors the shield: green ready, red disabled. Opacity applies to the shield.", Category.PVP,
                () -> c.shieldTweaks, v -> c.shieldTweaks = v)
                .setting("Ready (green)", () -> c.shieldReady, v -> c.shieldReady = v)
                .settingColor("Ready color", () -> c.shieldReadyColor, v -> c.shieldReadyColor = v)
                .nestLast("Ready (green)")
                .setting("Disabled (red)", () -> c.shieldDisabled, v -> c.shieldDisabled = v)
                .settingColor("Disabled color", () -> c.shieldDisabledColor, v -> c.shieldDisabledColor = v)
                .nestLast("Disabled (red)")
                .setting("Blocking", () -> c.shieldBlocking, v -> c.shieldBlocking = v)
                .settingF("Shield opacity", () -> (double) c.shieldOpacity, v -> c.shieldOpacity = (float) v, 0, 100)
                .setting("Own shield only", () -> c.shieldOwnOnly, v -> c.shieldOwnOnly = v)
                .setting("Fix blocking animation", () -> c.shieldFixAnim, v -> c.shieldFixAnim = v));
        dev.aero.client.OptimizerMods.attach(all.get(all.size() - 1), dev.aero.client.OptimizerMods.SHIELD);
        add(new Module("Toggle Sprint", "Keeps sprint on while moving forward", Category.HUD,
                () -> c.toggleSprint, v -> c.toggleSprint = v)
                .setting("Always sprint", () -> c.alwaysSprint, v -> c.alwaysSprint = v));
        add(new Module("Combo", "Counts consecutive hits on the same target", Category.HUD,
                () -> c.comboHud, v -> c.comboHud = v));
        add(new Module("CPS", "Shows your own click rate", Category.HUD,
                () -> c.cpsHud, v -> c.cpsHud = v));

        add(new Module("Armor HUD", "Armor durability", Category.HUD,
                () -> c.armorHud, v -> c.armorHud = v)
                .setting("Durability style", () -> c.armorDurabilityStyle, v -> c.armorDurabilityStyle = v, "Bars", "Text", "None")
                .setting("Layout", () -> c.armorLayout, v -> c.armorLayout = v, "Horizontal", "Vertical")
                .setting("Color by durability", () -> c.armorDurabilityColor, v -> c.armorDurabilityColor = v)
                .setting("Break warning", () -> c.armorBreakWarning, v -> c.armorBreakWarning = v));
        add(new Module("Coordinates", "XYZ position", Category.HUD,
                () -> c.coordsHud, v -> c.coordsHud = v)
                .setting("Shadow", () -> c.coordsShadow, v -> c.coordsShadow = v));
        add(new Module("FPS", "Shows frames per second", Category.HUD,
                () -> c.fpsHud, v -> c.fpsHud = v)
                .setting("Shadow", () -> c.fpsShadow, v -> c.fpsShadow = v));
        add(new Module("GUI Tweaks", "Cleaner vanilla GUI spacing", Category.HUD,
                () -> c.guiTweaks, v -> c.guiTweaks = v)
                .setting("HUD Tweaks", () -> c.guiHudTweaks, v -> c.guiHudTweaks = v)
                .settingF("Esc Menu Scale", () -> (double) c.escHudScale, v -> c.escHudScale = (float) v, 0.5, 2.0)
                .setting("No Item Name", () -> c.guiNoItemName, v -> c.guiNoItemName = v)
                .setting("Hide Selector", () -> c.guiHideSelector, v -> c.guiHideSelector = v)
                .setting("Hide Action Bar", () -> c.guiHideActionBar, v -> c.guiHideActionBar = v)
                .setting("Inventory Tweaks", () -> c.guiInventoryTweaks, v -> c.guiInventoryTweaks = v)
                .settingF("Inventory Scale", () -> (double) c.guiInventoryScale, v -> c.guiInventoryScale = (float) v, 0.5, 2.0)
                .setting("Hotbar", () -> c.guiHotbar, v -> c.guiHotbar = v)
                .setting("Health", () -> c.guiHealth, v -> c.guiHealth = v)
                .setting("Armor", () -> c.guiArmor, v -> c.guiArmor = v)
                .setting("Hunger", () -> c.guiHunger, v -> c.guiHunger = v)
                .setting("Scoreboard", () -> c.guiScoreboard, v -> c.guiScoreboard = v)
                .setting("Chat", () -> c.guiChat, v -> c.guiChat = v)
                .setting("Boss bar", () -> c.guiBossBar, v -> c.guiBossBar = v)
                .setting("Status effects", () -> c.guiStatus, v -> c.guiStatus = v)
                .setting("Experience bar", () -> c.guiXp, v -> c.guiXp = v)
                .setting("Air bubbles", () -> c.guiAir, v -> c.guiAir = v)
                .setting("Mount health", () -> c.guiMount, v -> c.guiMount = v));
        add(new Module("Keystrokes", "WASD + mouse keys", Category.HUD,
                () -> c.keystrokes, v -> c.keystrokes = v)
                .setting("Show Keys", () -> c.keyShowKeys, v -> c.keyShowKeys = v)
                .setting("Show Mouse", () -> c.keyShowMouse, v -> c.keyShowMouse = v)
                .setting("CPS", () -> c.keyCps, v -> c.keyCps = v)
                .settingF("Key Size", () -> (double) c.keySize, v -> c.keySize = (float) v, 10, 40)
                .settingF("Gap", () -> (double) c.keyGap, v -> c.keyGap = (float) v, 0, 12)
                .settingF("Scale", () -> (double) c.keyScale, v -> c.keyScale = (float) v, 0.5, 2));
        add(new Module("Music Player", "Shows the current music track", Category.HUD,
                () -> c.musicPlayer, v -> c.musicPlayer = v)
                .setting("Album Art", () -> c.musicAlbum, v -> c.musicAlbum = v)
                .setting("Background", () -> c.musicBg, v -> c.musicBg = v)
                .setting("Progress Bar", () -> c.musicBar, v -> c.musicBar = v));
        add(new Module("Ping", "Shows your server latency", Category.HUD,
                () -> c.pingHud, v -> c.pingHud = v)
                .setting("Shadow", () -> c.pingShadow, v -> c.pingShadow = v)
                .setting("Next to names", () -> c.nametagPing, v -> c.nametagPing = v)
                .setting("Side", () -> c.pingSide, v -> c.pingSide = v, "Left", "Right")
                .setting("Divider", () -> c.pingDivider, v -> c.pingDivider = v)
                .setting("Color by latency", () -> c.pingColorByLatency, v -> c.pingColorByLatency = v)
                .settingColor("Good color", () -> c.pingColGood, v -> c.pingColGood = v)
                .settingColor("OK color", () -> c.pingColWarn, v -> c.pingColWarn = v)
                .settingColor("High color", () -> c.pingColBad, v -> c.pingColBad = v)
                .setting("OK from (ms)", () -> c.pingWarnMs, v -> c.pingWarnMs = v, 10, 300)
                .setting("High from (ms)", () -> c.pingBadMs, v -> c.pingBadMs = v, 20, 500));
        add(new Module("Potion HUD", "Active effect list", Category.HUD,
                () -> c.potionHud, v -> c.potionHud = v)
                .setting("Icons", () -> c.potionIcons, v -> c.potionIcons = v)
                .setting("Name", () -> c.potionName, v -> c.potionName = v)
                .setting("Level", () -> c.potionLevel, v -> c.potionLevel = v)
                .setting("Timer", () -> c.potionTimer, v -> c.potionTimer = v)
                .setting("Hide vanilla icons", () -> c.potionHideVanilla, v -> c.potionHideVanilla = v));
        add(new Module("Saturation Overlay", "Hunger saturation on the bar", Category.HUD,
                () -> c.saturationOverlay, v -> c.saturationOverlay = v)
                .settingF("Opacity", () -> (double) c.satOpacity, v -> c.satOpacity = (float) v, 0, 100)
                .setting("Hide when full", () -> c.satHideFull, v -> c.satHideFull = v));
        add(new Module("Watermark", "Icon + name, bottom-right when HUD or a menu is open", Category.HUD,
                () -> c.watermark, v -> c.watermark = v)
                .setting("Rainbow", () -> c.watermarkRainbow, v -> c.watermarkRainbow = v)
                .setting("Background", () -> c.watermarkBg, v -> c.watermarkBg = v)
                .setting("Shadow", () -> c.watermarkShadow, v -> c.watermarkShadow = v));
        add(new Module("Arraylist", "Lists enabled modules", Category.HUD,
                () -> c.arraylist, v -> c.arraylist = v));
        add(new Module("Sprint", "Shows when you are sprinting", Category.HUD,
                () -> c.sprintHud, v -> c.sprintHud = v)
                .setting("Style", () -> c.sprintStyle, v -> c.sprintStyle = v, "Short", "Full")
                .setting("Show sprinting", () -> c.sprintShowSprint, v -> c.sprintShowSprint = v)
                .setting("Show sneaking", () -> c.sprintShowSneak, v -> c.sprintShowSneak = v)
                .setting("Show swimming", () -> c.sprintShowSwim, v -> c.sprintShowSwim = v));
        add(new Module("Totem Counter", "Counts totems in your inventory", Category.PVP,
                () -> c.totemCounter, v -> c.totemCounter = v)
                .setting("Show HUD", () -> c.totemHud, v -> c.totemHud = v)
                .setting("Own Totems", () -> c.totemOwnOnly, v -> c.totemOwnOnly = v)
                .setting("Auto color (by amount)", () -> c.totemAutoColor, v -> c.totemAutoColor = v)
                .settingColor("Good color", () -> c.totemColGood, v -> c.totemColGood = v)
                .settingColor("Warn color", () -> c.totemColWarn, v -> c.totemColWarn = v)
                .settingColor("Bad color", () -> c.totemColBad, v -> c.totemColBad = v)
                .settingAction("Reset pops", "Reset", dev.aero.client.Visuals::resetTotemPops)
                .settingKey("Reset key", () -> c.totemResetKey, v -> c.totemResetKey = v)
                .setting("Use color", () -> c.totemUseColor, v -> c.totemUseColor = v)
                .settingColor("Count color", () -> c.totemColor, v -> c.totemColor = v)
                .nestLast("Use color")
                .setting("Style", () -> c.totemStyle, v -> c.totemStyle = v, "Boxed", "Plain")
                .setting("Pops next to name", () -> c.totemPopsOnNametag, v -> c.totemPopsOnNametag = v)
                .setting("Pop effect", () -> c.totemPopFx, v -> c.totemPopFx = v, "Off", "Burst", "Spiral", "Rings", "Hearts", "Soul")
                .settingColor("Pop effect color", () -> c.totemPopFxColor, v -> c.totemPopFxColor = v));
        dev.aero.client.OptimizerMods.attach(all.get(all.size() - 1), dev.aero.client.OptimizerMods.TOTEM_COUNTER);
        add(new Module("Item Highlighter", "Outline standard PvP items in hotbar and inv", Category.HUD,
                () -> c.itemHighlighter, v -> c.itemHighlighter = v)
                .settingColor("Background color", () -> c.highlightColor, v -> c.highlightColor = v)
                .setting("Opacity %", () -> c.highlightAlpha, v -> c.highlightAlpha = v, 5, 100)
                .setting("Every item", () -> c.highlightAllItems, v -> c.highlightAllItems = v)
                .settingText("Per-item colors (name=RRGGBB,...)", () -> c.highlightCustom, v -> c.highlightCustom = v)
                .setting("In inventories", () -> c.highlightInventories, v -> c.highlightInventories = v)
                .setting("In hotbar", () -> c.highlightHotbar, v -> c.highlightHotbar = v)
                .setting("Totem", () -> c.highlightTotem, v -> c.highlightTotem = v)
                .setting("End crystal", () -> c.highlightCrystal, v -> c.highlightCrystal = v)
                .setting("Gapple", () -> c.highlightGapple, v -> c.highlightGapple = v)
                .setting("Pearl", () -> c.highlightPearl, v -> c.highlightPearl = v)
                .setting("Obsidian", () -> c.highlightObsidian, v -> c.highlightObsidian = v)
                .setting("XP bottle", () -> c.highlightXp, v -> c.highlightXp = v)
                .setting("Sword", () -> c.highlightSword, v -> c.highlightSword = v)
                .setting("Axe", () -> c.highlightAxe, v -> c.highlightAxe = v)
                .setting("Mace", () -> c.highlightMace, v -> c.highlightMace = v)
                .setting("Anchor", () -> c.highlightAnchor, v -> c.highlightAnchor = v)
                .setting("Shield", () -> c.highlightShield, v -> c.highlightShield = v)
                .setting("Glowstone", () -> c.highlightGlowstone, v -> c.highlightGlowstone = v)
                .setting("Web", () -> c.highlightWeb, v -> c.highlightWeb = v)
                .setting("Potion", () -> c.highlightPotion, v -> c.highlightPotion = v)
                .setting("Any enchanted", () -> c.highlightEnchanted, v -> c.highlightEnchanted = v));
        add(new Module("Chat", "Timestamps and no background", Category.MISC,
                () -> c.chatTimestamps, v -> c.chatTimestamps = v)
                .setting("Mention ping", () -> c.chatMentionPing, v -> c.chatMentionPing = v)
                .setting("No BG", () -> c.noChatBg, v -> c.noChatBg = v));

        add(new Module("Ambience", "Sky, weather, lava, fire", Category.RENDER,
                () -> c.ambience, v -> c.ambience = v)
                .setting("Overworld Sky", () -> c.overworldSky, v -> c.overworldSky = v)
                .setting("Sky Gradient", () -> c.skyGradient, v -> c.skyGradient = v)
                .setting("Nether Sky", () -> c.netherSky, v -> c.netherSky = v)
                .setting("End Sky", () -> c.endSky, v -> c.endSky = v)
                .setting("Clouds", () -> c.ambienceClouds, v -> c.ambienceClouds = v)
                .setting("Water", () -> c.ambienceWater, v -> c.ambienceWater = v)
                .setting("Lava", () -> c.ambienceLava, v -> c.ambienceLava = v)
                .setting("Fire", () -> c.ambienceFire, v -> c.ambienceFire = v));
        add(new Module("Emotes", "Hold a key for the emote wheel", Category.PLAYER,
                () -> c.emotes, v -> c.emotes = v));
        add(new Module("Discord RPC", "Show Aero Client on your Discord status", Category.MISC,
                () -> c.discordRpc, v -> c.discordRpc = v)
                .settingText("Client ID", () -> c.discordClientId, v -> c.discordClientId = v));
        add(new Module("Screenshot", "Capture, copy and share", Category.MISC,
                () -> c.screenshotModule, v -> c.screenshotModule = v)
                .setting("Auto copy", () -> c.screenshotAutoCopy, v -> c.screenshotAutoCopy = v));
        add(new Module("Sound Controller", "Retune or silence any sound in the game", Category.MISC,
                () -> c.soundController, v -> c.soundController = v)
                .settingText("Sounds",
                        () -> c.soundControllerFilter, v -> c.soundControllerFilter = v));
    }

    private void add(Module module) {
        all.add(module);
    }

    public List<Module> of(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module module : all) {
            if (module.category == category) {
                out.add(module);
            }
        }
        return out;
    }

    public int menuCount() {
        int n = 0;
        for (Module module : all) {
            if (module.category.inSidebar()) {
                n++;
            }
        }
        return n;
    }

    public int count(Category category) {
        int n = 0;
        for (Module module : all) {
            if (module.category == category) {
                n++;
            }
        }
        return n;
    }

    public List<Module> enabled() {
        List<Module> out = new ArrayList<>();
        enabledInto(out);
        return out;
    }

    public void enabledInto(List<Module> out) {
        out.clear();
        for (Module module : all) {
            if (module.enabled()) {
                out.add(module);
            }
        }
    }

    public void applyFpsPreset() {
        ClientConfig c = AeroClient.CONFIG;
        c.particleLimiter = true;
        c.maxParticles = 24;
        c.entityDistance = true;
        c.entityRange = 36;
        c.noWeather = true;
        c.noFog = true;
        c.cloudsOff = true;
        c.unfocusedCpu = true;
        c.hideArmorStands = true;
        c.itemLimiter = true;
        c.fullbright = true;
        c.noHurtcam = true;
        c.lowFire = true;
        c.noVignette = true;
        c.cleanWater = true;
        c.fpsBoost = true;
        c.noShadows = true;
        c.noGlint = true;
        c.noBeacons = true;
        c.hideFrames = true;
        c.hideFalling = true;
        c.fastHud = true;
        c.noBobbing = true;
        c.noChatBg = true;
        c.noLightning = true;
        c.noExplosions = true;
        c.noFireworks = true;
        c.hideXpOrbs = true;
        c.soundCut = true;
        c.hideDroppedItems = true;
        c.hideTnt = true;
        c.hideProjectiles = true;
        c.hideTileEntities = true;
        c.noBreakParticles = true;
        c.noPotionParticles = true;
        c.fastGraphics = true;
        c.renderDistanceOverride = true;
        c.renderDistanceValue = Math.min(c.renderDistanceValue, 8);
        c.hideOtherPlayers = false;
        c.save();
    }
}
