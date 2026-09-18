package dev.aero.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.aero.client.module.Module;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Bump this whenever defaults change in a way that should override an old saved config on disk. */
    private static final int CURRENT_VERSION = 3;

    public int configVersion = CURRENT_VERSION;

    public boolean fpsHud = false;
    public boolean pingHud = false;
    public boolean cpsHud = false;
    public boolean coordsHud = false;
    public boolean potionHud = false;
    public boolean comboHud = false;
    public boolean keystrokes = false;
    public boolean armorHud = false;
    public boolean watermark = false;
    public boolean arraylist = false;

    public boolean particleLimiter = false;
    public int maxParticles = 80;
    public boolean entityDistance = false;
    public int entityRange = 48;
    public boolean noWeather = false;
    public boolean noFog = false;
    public boolean unfocusedCpu = false;
    public int unfocusedFps = 15;
    public boolean hideArmorStands = false;
    public boolean itemLimiter = false;

    public boolean fullbright = false;
    public boolean noHurtcam = false;
    public boolean lowFire = false;
    public boolean noVignette = false;
    public boolean uiBoost = true;
    public boolean uiBoostOverlays = true;
    public boolean uiBoostToasts = true;
    public boolean hideScoreboard = false;
    public boolean cleanWater = false;

    public boolean toggleSprint = false;
    public boolean customCrosshair = false;
    public boolean crosshairAddons = false;
    public boolean damageTint = false;
    public boolean shieldTweaks = false;
    public boolean crossbowTweaks = false;
    public boolean guiTweaks = false;
    public boolean musicPlayer = false;
    public boolean saturationOverlay = false;
    public boolean skyGradient = false;
    public boolean netherSky = false;
    public int netherColor = -10551296;
    public boolean endSky = false;
    public int endColor = -15198172;
    public boolean cloudsOff = false;

    public boolean hideArmor = false;
    public boolean hideArmorSelf = true;
    public boolean hideArmorOthers = true;
    public boolean hideHelmet = false;
    public boolean hideChestplate = false;
    public boolean hideLeggings = false;
    public boolean hideBoots = false;
    public boolean cleanView = false;
    public boolean cleanArrows = true;
    public boolean hideFireOverlay = true;
    public boolean cleanPotionSwirls = false;
    public boolean cleanSprintDust = false;
    public boolean hideHands = false;
    public boolean noSwing = false;
    public boolean handTweaks = false;
    public String handStyle = "Normal";
    public float handSpeed = 1.0f;
    public boolean handRestart = false;
    public float customX = 0.56f;
    public float customY = -0.32f;
    public float customZ = -0.72f;
    public float customPitch = 0f;
    public float customYaw = 0f;
    public float customRoll = 0f;
    public float swingPitch = -60f;
    public float swingYaw = 0f;
    public float swingRoll = 0f;
    public float mainX = 0f;
    public float mainY = 0f;
    public float mainZ = 0f;
    public float mainPitch = 0f;
    public float mainYaw = 0f;
    public float mainRoll = 0f;
    public float mainScale = 1.0f;
    public float offX = 0f;
    public float offY = 0f;
    public float offZ = 0f;
    public float offPitch = 0f;
    public float offYaw = 0f;
    public float offRoll = 0f;
    public float offScale = 1.0f;
    public int handX = 0;
    public int handY = 0;
    public int handZ = 0;
    public int handScale = 100;
    public boolean nametags = false;
    public float nametagScale = 1.0f;
    public boolean nametagPing = true;
    public String pingSide = "Right";
    public boolean pingDivider = false;
    public boolean ownNametag = false;
    public boolean nametagBadge = true;
    public boolean timeChanger = false;
    public String timePreset = "Day";
    public int timeHour = 12;
    public float customTicks = 6000f;
    public boolean weatherChanger = false;
    public String weatherMode = "Clear";
    public boolean weatherClear = true;
    public boolean zoom = false;
    public int zoomKey = 67;
    public boolean zoomToggle = false;
    public float zoomInitial = 4.0f;
    public float zoomInTime = 1.0f;
    public float zoomOutTime = 0.5f;
    public boolean zoomScroll = true;
    public float zoomSteps = 10f;
    public float zoomPerStep = 150f;
    public float zoomSmooth = 70f;
    public boolean zoomHands = true;
    public int zoomFov = 30;
    public boolean chatTimestamps = false;
    public boolean chatMentionPing = true;
    public boolean hideInvModel = false;
    public boolean hideInvTransparent = false;
    public boolean totemCounter = false;
    public boolean totemHud = true;
    public boolean totemOwnOnly = true;
    public boolean totemUseColor = true;
    public int totemColor = -6267649;
    public String totemStyle = "Boxed";
    public boolean totemPopsOnNametag = true;
    public boolean totemTweaks = false;
    public float totemSize = 1.0f;
    public float totemOpacity = 100f;
    public boolean totemNoEquip = false;
    public float totemPopSize = 1.0f;
    public float totemPopDuration = 40f;
    public boolean totemPopCentre = false;
    public float totemPopOpacity = 100f;
    public boolean totemPopNoParticles = false;
    public float totemPopParticleCount = 1.0f;
    public boolean sprintHud = false;
    public String sprintStyle = "Short";
    public boolean sprintShowSprint = true;
    public boolean sprintShowSneak = false;
    public boolean sprintShowSwim = true;
    public boolean alwaysSprint = false;
    public boolean itemHighlighter = false;
    public boolean highlightInventories = true;
    public boolean highlightHotbar = true;
    public boolean highlightTotem = true;
    public boolean highlightCrystal = true;
    public boolean highlightGapple = true;
    public boolean highlightPearl = true;
    public boolean highlightObsidian = true;
    public boolean highlightXp = true;
    public boolean highlightShield = false;
    public boolean highlightSword = true;
    public boolean highlightAxe = true;
    public boolean highlightMace = true;
    public boolean highlightAnchor = true;
    public boolean highlightGlowstone = false;
    public boolean highlightWeb = false;
    public boolean highlightPotion = false;
    public boolean highlightEnchanted = false;
    public String itemHighlighterFilter = "";
    public boolean ambience = false;
    public boolean overworldSky = false;
    public int skyColor = -8869889;
    public int gradientColor = -4138753;
    public boolean ambienceClouds = false;
    public int cloudColor = -1;
    public boolean ambienceWater = false;
    public int waterColor = -12618012;
    public boolean ambienceLava = false;
    public int lavaColor = -42496;
    public boolean ambienceFire = false;
    public int fireColor = -33024;
    public float brightness = 10.0f;
    public boolean damageTintChroma = false;
    public float damageTintSpeed = 0.25f;
    public int damageTintColor = 1308557312;
    public boolean crosshairHover = true;
    public int crosshairHoverColor = -44976;
    public float crosshairHoverRange = 6.0f;
    public int crosshairArm = 5;
    public int crosshairGap = 2;
    public int crosshairColor = 0xFFF6F2FF;
    public boolean crosshairUseDrawing = false;
    public String crosshairPixels = "";
    public String crosshairStyle = "Cross";
    public float crosshairThickness = 1f;
    public boolean crosshairOutline = true;
    public int crosshairOutlineColor = 0xCC120E1A;
    public boolean crosshairDot = false;
    public int crosshairDotColor = 0xFFF6F2FF;
    public boolean crosshairRainbow = false;
    public float crosshairRainbowSpeed = 1f;
    public boolean crosshairDynamicAttack = false;
    public boolean crosshairThirdPerson = false;
    public boolean crosshairWhenHidden = false;
    public boolean crosshairHighlightHostiles = false;
    public int crosshairHostileColor = 0xFFE05555;
    public boolean crosshairHighlightPassives = false;
    public int crosshairPassiveColor = 0xFF4CD964;
    public boolean crosshairCooldown = false;
    public int crosshairCooldownColor = 0x88FFFFFF;
    public boolean addonElytra = true;
    public boolean addonEntity = false;
    public boolean addonEntityShowAll = false;
    public String addonEntityList = "";
    public boolean addonShield = false;
    public boolean addonShieldFactorDelay = true;
    public boolean addonEnvBlend = true;
    public boolean addonThirdPerson = false;
    public int addonGap = 3;
    public boolean addonHitmarkerToggleOnAttack = true;
    public boolean addonHitmarkerStopOnAnimEnd = true;
    public int addonHitmarkerDuration = 6;
    public boolean addonShieldBreakStopOnAnimEnd = true;
    public int addonShieldBreakDuration = 6;
    public boolean crossbowInInv = true;
    public boolean crossbowInHotbar = false;
    public float crossbowOpacity = 10f;
    public int crossbowArrowColor = -1183753;
    public boolean shieldReady = true;
    public int shieldReadyColor = 0xFF4CD964;
    public boolean shieldBlocking = false;
    public int shieldBlockingColor = -4745994;
    public boolean shieldDisabled = true;
    public int shieldDisabledColor = 0xFFE05555;
    public float shieldOpacity = 100f;
    public boolean shieldOwnOnly = false;
    public boolean shieldFixAnim = true;
    public boolean potionIcons = true;
    public boolean potionName = true;
    public boolean potionLevel = true;
    public boolean potionTimer = true;
    public boolean potionHideVanilla = true;
    public boolean armorDurabilityColor = true;
    public String armorLayout = "Horizontal";
    public String armorDurabilityStyle = "Bars";
    public boolean armorBreakWarning = true;
    public float satOpacity = 85f;
    public boolean satHideFull = false;
    public int satColor = -469926;
    public boolean guiHudTweaks = true;
    public boolean guiNoItemName = false;
    public boolean guiHideSelector = false;
    public boolean guiHideActionBar = false;
    public boolean guiInventoryTweaks = true;
    public float guiInventoryScale = 1.0f;
    public float escHudScale = 1.0f;
    public boolean guiHotbar = true;
    public boolean guiHealth = true;
    public boolean guiArmor = true;
    public boolean guiHunger = true;
    public boolean guiScoreboard = true;
    public boolean guiChat = true;
    public boolean guiBossBar = true;
    public boolean guiStatus = true;
    public boolean guiXp = true;
    public boolean guiAir = true;
    public boolean guiMount = true;
    public String watermarkText = "Aero";
    public String watermarkSubtitle = "";
    public boolean watermarkRainbow = false;
    public boolean watermarkBg = true;
    public boolean watermarkShadow = true;
    public boolean keyShowKeys = true;
    public boolean keyShowMouse = true;
    public boolean keyCps = true;
    public float keySize = 20f;
    public float keyGap = 0f;
    public float keyScale = 0.9f;
    public int fpsX = 4;
    public int fpsY = 4;
    public int pingX = 4;
    public int pingY = 28;
    public int coordsX = 4;
    public int coordsY = 16;
    public int potionX = 4;
    public int potionY = 44;
    public int sprintX = 4;
    public int sprintY = 40;
    public int watermarkX = 4;
    public int watermarkY = 30;
    public int totemX = 0;
    public int totemY = 0;
    public int armorHudX = 600;
    public int armorHudY = 649;
    public int keystrokesX = 8;
    public int keystrokesY = 8;
    public int musicX = 4;
    public int musicY = 40;
    public boolean musicAlbum = true;
    public boolean musicBg = true;
    public boolean musicBar = true;
    public int panelAccent = -4745994;
    public boolean panelShadow = true;
    public String panelStyle = "Glass";
    // Coordinates/FPS/Ping each show their own "Shadow" checkbox in the ClickGUI -
    // used to all read/write panelShadow, so toggling one silently flipped the other two.
    public boolean coordsShadow = true;
    public boolean fpsShadow = true;
    public boolean pingShadow = true;

    public String equippedCape = "none";
    public String equippedWings = "none";
    public String equippedHead = "none";
    public String equippedTrail = "none";
    public String equippedPet = "none";
    public String equippedEmote = "wave";
    public String equippedTag = "none";
    public String equippedBadge = "none";
    public String equippedKillEffect = "none";
    public String equippedMace = "none";
    public java.util.Map<String, Integer> cosmeticVariant = new java.util.HashMap<>();
    public String totemPopFx = "Burst";
    public int totemPopFxColor = 0xFF4F8EFF;

    public boolean fpsBoost = false;
    public boolean noShadows = false;
    public boolean noGlint = false;
    public boolean noBeacons = false;
    public boolean hideFrames = false;
    public boolean hideFalling = false;
    public boolean fastHud = false;
    public boolean noBobbing = false;
    public boolean noChatBg = false;
    public boolean hideBossBar = false;
    public boolean noLightning = false;
    public boolean noExplosions = false;
    public boolean noFireworks = false;
    public boolean hideXpOrbs = false;
    public boolean soundCut = false;
    public boolean hideDroppedItems = false;
    public boolean hideTnt = false;
    public boolean hideProjectiles = false;
    public boolean hidePassiveMobs = false;
    public boolean hideTileEntities = false;
    public boolean hideSky = false;
    public boolean hideStars = false;
    public boolean noBreakParticles = false;
    public boolean noPotionParticles = false;
    public boolean hideEnchantParticles = false;
    public boolean fastGraphics = false;

    public Map<String, Module.ModuleStyle> moduleStyles = new HashMap<>();

    public boolean blockOverlay = false;
    public boolean blockOverlayOutline = true;
    public boolean blockOverlayFaces = true;
    public boolean blockOverlayThroughWalls = false;
    public int blockOverlayColor = -6740657;

    public boolean cobwebTweaks = false;
    public float cobwebOpacity = 35f;
    public int cobwebColor = -1;

    public boolean customEndCrystals = false;
    public String crystalMode = "Default";
    public float crystalScale = 1.0f;
    public float crystalRotationSpeed = 1.0f;
    public float crystalBounceHeight = 1.0f;
    public float crystalBounceSpeed = 1.0f;
    public float crystalHeightOffset = 0f;
    public float crystalOpacity = 100f;

    public boolean deathAnimation = false;
    public String deathAnimationEntities = "player";
    public float deathTime = 22f;
    public float deathOverlayTime = 22f;
    public float deathOpacity = 100f;

    public boolean renderDistanceOverride = false;
    public int renderDistanceValue = 8;
    public int renderDistancePrev = -1;

    public boolean hideOtherPlayers = false;

    public boolean crystalOptimizer = false;
    public boolean crystalOptimizerParticles = true;
    public boolean crystalOptimizerSound = true;
    public boolean crystalOptimizerRange = false;
    public int crystalOptimizerRangeValue = 24;

    public boolean anchorOptimizer = false;
    public boolean anchorOptimizerSwing = true;
    public boolean anchorOptimizerParticles = true;
    public boolean anchorOptimizerSound = true;

    public boolean pearlOptimizer = false;
    public boolean pearlOptimizerSwing = true;
    public boolean pearlOptimizerParticles = true;

    public boolean shieldOptimizer = false;
    public boolean shieldOptimizerInstant = true;
    public boolean shieldOptimizerSound = true;

    public boolean crossbowOptimizer = false;
    public boolean crossbowOptimizerSwing = true;
    public boolean crossbowOptimizerParticles = true;

    public boolean hitboxes = false;
    public boolean hitboxPlayers = false;
    public boolean hitboxEndCrystals = true;
    public boolean hitboxPearls = true;
    public boolean hitboxArrows = false;
    public boolean hitboxEntities = false;
    public boolean hitboxTeams = false;
    public int hitboxColor = -1;

    public boolean motionBlur = false;
    public String motionBlurStyle = "Trail";
    public String motionBlurStrength = "Medium";

    public boolean nostalgia = false;
    public boolean nostalgiaOldCrystals = false;
    public boolean nostalgiaOldLighting = false;
    public boolean nostalgiaOldGlint = false;
    public boolean nostalgiaOldHurtCamera = false;
    public boolean nostalgiaPotionGlint = false;
    public boolean nostalgiaOldPotions = false;
    public boolean nostalgiaOldWalk = false;

    public boolean popChams = false;
    public boolean popChamsShowOwn = false;
    public boolean popChamsFadeOverTime = true;
    public boolean popChamsDisperse = false;
    public boolean popChamsFilledModel = true;
    public boolean popChamsWireframe = true;

    public boolean renders = false;
    public String rendersHandShader = "None";
    public boolean rendersPlayersGlow = false;
    public boolean rendersEndCrystalsGlow = false;

    public boolean tierTagger = false;
    public String tierList = "Mctiers";
    public String tierGamemode = "vanilla";
    public String tierSide = "Left";
    public boolean tierGamemodeIcon = true;
    public boolean tierShowOwn = true;
    public boolean tierShowChat = true;

    public boolean transparentPlayers = false;
    public float transparentBodyOpacity = 55f;
    public float transparentArmorOpacity = 100f;
    public boolean transparentSelf = false;
    public boolean transparentOnlyDamaged = false;

    public boolean autoText = false;
    public String autoTextMessage = "gg";
    public float autoTextDelayTicks = 5f;
    public float autoTextCooldown = 3f;
    public float autoTextKillRange = 24f;
    public String autoTextChatTrigger = "";

    public boolean emotes = false;

    public boolean discordRpc = false;
    public String discordClientId = "";

    public boolean optimizersModule = false;

    public boolean addonShieldBreak = false;
    public boolean addonHitmarker = false;

    /** Per-entry preference for each detected companion mod row shown in the Optimizers module. */
    public Map<String, Boolean> optimizerEnabled = new HashMap<>();

    public boolean screenshotModule = false;
    public boolean screenshotAutoCopy = false;

    public boolean soundController = false;
    public String soundControllerFilter = "";

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("aero-client.json");
    }

    public static Path profilesDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("aero-client-profiles");
    }

    public static java.util.List<String> listProfiles() {
        java.util.List<String> names = new java.util.ArrayList<>();
        try {
            Path dir = profilesDir();
            if (Files.isDirectory(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.filter(p -> p.toString().endsWith(".json"))
                            .forEach(p -> {
                                String n = p.getFileName().toString();
                                names.add(n.substring(0, n.length() - 5));
                            });
                }
            }
        } catch (IOException ignored) {
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void saveAsProfile(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String safe = name.trim().replaceAll("[^A-Za-z0-9 _-]", "");
        if (safe.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(profilesDir());
            Files.writeString(profilesDir().resolve(safe + ".json"), GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }

    public static ClientConfig loadProfile(String name) {
        try {
            Path file = profilesDir().resolve(name + ".json");
            if (Files.isRegularFile(file)) {
                ClientConfig cfg = GSON.fromJson(Files.readString(file), ClientConfig.class);
                if (cfg != null) {
                    if (cfg.moduleStyles == null) {
                        cfg.moduleStyles = new HashMap<>();
                    }
                    if (cfg.optimizerEnabled == null) {
                        cfg.optimizerEnabled = new HashMap<>();
                    }
                    cfg.configVersion = CURRENT_VERSION;
                    return cfg;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void deleteProfile(String name) {
        try {
            Files.deleteIfExists(profilesDir().resolve(name + ".json"));
        } catch (IOException ignored) {
        }
    }

    public static ClientConfig load() {
        Path file = path();
        if (Files.isRegularFile(file)) {
            try {
                String text = Files.readString(file);
                // Check the raw JSON (not the deserialized object) for the version marker - a
                // missing/older configVersion means this file predates a defaults change and must
                // be discarded, since Gson would otherwise silently keep old saved "true" values
                // that a Java-side default change can no longer override.
                com.google.gson.JsonObject raw = com.google.gson.JsonParser.parseString(text).getAsJsonObject();
                int savedVersion = raw.has("configVersion") ? raw.get("configVersion").getAsInt() : 0;
                if (savedVersion >= CURRENT_VERSION) {
                    ClientConfig cfg = GSON.fromJson(text, ClientConfig.class);
                    if (cfg != null) {
                        if (cfg.moduleStyles == null) {
                            cfg.moduleStyles = new HashMap<>();
                        }
                        if (cfg.optimizerEnabled == null) {
                            cfg.optimizerEnabled = new HashMap<>();
                        }
                        return cfg;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        ClientConfig cfg = new ClientConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
