package dev.aero.client;

import dev.aero.client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public final class Visuals {
    private Visuals() {}

    public static ClientConfig cfg() {
        return AeroClient.CONFIG;
    }

    public static boolean hideArmor(Entity entity) {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.hideArmor || entity == null || mc.player == null) {
            return false;
        }
        if (!armorTarget(entity, c, mc)) {
            return false;
        }
        return c.hideHelmet && c.hideChestplate && c.hideLeggings && c.hideBoots;
    }

    public static boolean hideArmorSlot(Entity entity, Object slot) {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.hideArmor || entity == null || mc.player == null || slot == null) {
            return false;
        }
        if (!armorTarget(entity, c, mc)) {
            return false;
        }
        String n = slot.toString().toLowerCase();
        if (n.contains("head") || n.contains("helmet")) {
            return c.hideHelmet;
        }
        if (n.contains("chest")) {
            return c.hideChestplate;
        }
        if (n.contains("leg")) {
            return c.hideLeggings;
        }
        if (n.contains("feet") || n.contains("boot")) {
            return c.hideBoots;
        }
        return false;
    }

    private static boolean armorTarget(Entity entity, ClientConfig c, MinecraftClient mc) {
        if (entity == mc.player) {
            return c.hideArmorSelf;
        }
        return c.hideArmorOthers && entity instanceof PlayerEntity;
    }

    public static boolean skipParticle(Object particle) {
        ClientConfig c = cfg();
        if (c == null || particle == null) {
            return false;
        }
        String n = particle.getClass().getName().toLowerCase();
        boolean crystalCut = Optimizer.crystal() && (c.crystalOptimizerParticles || Optimizer.on("marlowcrystal"));
        if ((c.noExplosions || crystalCut)
                && (n.contains("explosion") || n.contains("flash") || n.contains("largeexplode"))) {
            return true;
        }
        if (crystalCut && (n.contains("crit") || n.contains("enchant"))) {
            return true;
        }
        if ((c.anchorOptimizer && c.anchorOptimizerParticles) || Optimizer.heroAnchor() || Optimizer.cutebowAnchor()) {
            if (n.contains("explosion") || n.contains("flash") || n.contains("largeexplode")) {
                return true;
            }
        }
        if (Optimizer.pearl() && (c.pearlOptimizerParticles || Optimizer.on("pearloptimizer"))
                && (n.contains("portal") || n.contains("teleport"))) {
            return true;
        }
        if ((c.crossbowOptimizer || Optimizer.crossbow()) && c.crossbowOptimizerParticles && n.contains("crit")) {
            return true;
        }
        if (Optimizer.mace() && (n.contains("smash") || n.contains("gust") || n.contains("explosion"))) {
            return true;
        }
        if (c.noFireworks && (n.contains("firework") || n.contains("fireworks"))) {
            return true;
        }
        if (c.noBreakParticles && (n.contains("blockdust") || n.contains("block_dust") || n.contains("breaking"))) {
            return true;
        }
        if (c.noPotionParticles && (n.contains("spell") || n.contains("effect") || n.contains("ambiententity"))) {
            return true;
        }
        if (c.hideEnchantParticles && n.contains("enchant")) {
            return true;
        }
        if ((c.totemTweaks && n.contains("totemparticle")) || (Optimizer.totem() && n.contains("totem"))) {
            if (c.totemPopNoParticles || Optimizer.totem()) {
                return true;
            }
            float mult = Math.max(0f, c.totemPopParticleCount);
            if (mult < 1f) {
                return Math.random() > mult;
            }
        }
        if (c.fpsBoost && (n.contains("drip") || n.contains("ash") || n.contains("spore")
                || n.contains("portal") || n.contains("bubble") || n.contains("campfire")
                || n.contains("white_ash") || n.contains("falling_dust") || n.contains("rain")
                || n.contains("splash") || n.contains("smoke") && n.contains("camp"))) {
            return true;
        }
        if (!c.cleanView) {
            return false;
        }
        if (c.cleanPotionSwirls && (n.contains("spell") || n.contains("effect") || n.contains("ambient"))) {
            return true;
        }
        if (c.cleanSprintDust && (n.contains("blockdust") || n.contains("block_dust")
                || n.contains("cloudparticle") || n.contains(".cloud")
                || n.contains("sneeze") || n.contains("sprint"))) {
            return true;
        }
        return false;
    }

    public static boolean skipSound(Object sound) {
        ClientConfig c = cfg();
        if (c == null || sound == null) {
            return false;
        }
        String n = sound.toString().toLowerCase();
        try {
            Object id = sound.getClass().getMethod("getId").invoke(sound);
            if (id != null) {
                n += id.toString().toLowerCase();
            }
        } catch (Throwable ignored) {
        }
        if (Optimizer.crystal() && (c.crystalOptimizerSound || Optimizer.on("marlowcrystal"))
                && (n.contains("explode") || n.contains("explosion"))) {
            return true;
        }
        if ((c.anchorOptimizer && c.anchorOptimizerSound) || Optimizer.heroAnchor() || Optimizer.cutebowAnchor()) {
            if (n.contains("explode") || n.contains("explosion") || n.contains("respawn_anchor")) {
                return true;
            }
        }
        if (c.shieldOptimizer && c.shieldOptimizerSound && n.contains("shield")) {
            return true;
        }
        if (c.soundController && c.soundControllerFilter != null && !c.soundControllerFilter.isBlank()) {
            for (String part : c.soundControllerFilter.split(",")) {
                String needle = part.trim().toLowerCase(java.util.Locale.ROOT);
                if (!needle.isEmpty() && n.contains(needle)) {
                    return true;
                }
            }
        }
        if (!c.soundCut) {
            return false;
        }
        return n.contains("weather") || n.contains("rain") || n.contains("thunder")
                || n.contains("ambient") || n.contains("cave") || n.contains("drip")
                || n.contains("wind") || n.contains("underwater_ambience");
    }

    /** Human-readable current connection for the Optimizers panel's "Server" row. */
    public static String currentServerLabel() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.isIntegratedServerRunning()) {
                return "Singleplayer";
            }
            var entry = mc.getCurrentServerEntry();
            if (entry != null && entry.address != null && !entry.address.isBlank()) {
                return entry.name != null && !entry.name.isBlank() ? entry.name : entry.address;
            }
        } catch (Throwable ignored) {
        }
        return "Not connected";
    }

    private static boolean zoomHeldPrev;
    private static boolean zoomLatched;

    public static void tickZoom() {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.zoom || mc == null || mc.currentScreen != null || mc.getWindow() == null) {
            zoomHeldPrev = false;
            return;
        }
        boolean held = zoomKeyDown(mc, c);
        if (c.zoomToggle) {
            if (held && !zoomHeldPrev) {
                zoomLatched = !zoomLatched;
            }
        } else {
            zoomLatched = false;
        }
        zoomHeldPrev = held;
    }

    private static boolean zoomKeyDown(MinecraftClient mc, ClientConfig c) {
        try {
            long handle = mc.getWindow().getHandle();
            int key = Math.max(32, Math.min(90, c.zoomKey));
            return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean zooming() {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.zoom || mc == null || mc.currentScreen != null || mc.getWindow() == null) {
            return false;
        }
        if (c.zoomToggle) {
            return zoomLatched;
        }
        return zoomKeyDown(mc, c);
    }

    /**
     * Crosshair hover-highlight: mc.targetedEntity is capped to attack/interaction reach, so this
     * does its own longer-range look-vector vs. bounding-box check (still stopping at blocks) so
     * the crosshair can turn red for any visible player, not just ones close enough to hit.
     */
    public static PlayerEntity hoveredPlayer(MinecraftClient mc, double maxDistance) {
        if (mc.player == null || mc.world == null) {
            return null;
        }
        Vec3d eye = mc.player.getCameraPosVec(1.0f);
        Vec3d look = mc.player.getRotationVec(1.0f);
        Vec3d end = eye.add(look.multiply(maxDistance));
        try {
            var blockHit = mc.world.raycast(new net.minecraft.world.RaycastContext(eye, end,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
            double blockDist = blockHit != null && blockHit.getType() != net.minecraft.util.hit.HitResult.Type.MISS
                    ? blockHit.getPos().distanceTo(eye) : maxDistance;
            PlayerEntity best = null;
            double bestDist = blockDist;
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) {
                    continue;
                }
                Box box = p.getBoundingBox().expand(0.1);
                var hit = box.raycast(eye, end);
                if (hit.isPresent()) {
                    double d = hit.get().distanceTo(eye);
                    if (d < bestDist) {
                        bestDist = d;
                        best = p;
                    }
                }
            }
            return best;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * True line-of-sight check (blocked by terrain, unlike the tracking-distance-only check that
     * mc.world.getPlayers() gives you) - Pop Chams needs this to know when a player actually walks
     * out of view behind something, not just when they leave render distance entirely.
     */
    public static boolean isPlayerVisible(MinecraftClient mc, PlayerEntity target) {
        if (mc.player == null || mc.world == null || target == null) {
            return false;
        }
        try {
            Vec3d eye = mc.player.getCameraPosVec(1.0f);
            Vec3d targetPos = target.getEyePos();
            var hit = mc.world.raycast(new net.minecraft.world.RaycastContext(eye, targetPos,
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE, mc.player));
            return hit == null || hit.getType() == net.minecraft.util.hit.HitResult.Type.MISS;
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static long timeOfDay() {
        ClientConfig c = cfg();
        if (c == null) {
            return 6000L;
        }
        String p = c.timePreset == null ? "Day" : c.timePreset;
        return switch (p.toLowerCase()) {
            case "noon" -> 6000L;
            case "sunset" -> 12000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            case "custom" -> (long) Math.max(0, Math.min(24000, c.customTicks));
            default -> 1000L;
        };
    }

    public static String clockPrefix() {
        java.time.LocalTime now = java.time.LocalTime.now();
        return String.format("[%02d:%02d] ", now.getHour(), now.getMinute());
    }

    public static boolean hideOtherPlayer(Entity entity) {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.hideOtherPlayers || entity == null || mc.player == null) {
            return false;
        }
        return entity != mc.player && entity instanceof PlayerEntity;
    }

    /** Death-flop animation removal: never lets the death rotation pass a tiny threshold for matching entities. */
    public static boolean skipDeathAnimation(Entity entity) {
        ClientConfig c = cfg();
        if (c == null || !c.deathAnimation || entity == null) {
            return false;
        }
        String filter = c.deathAnimationEntities == null ? "" : c.deathAnimationEntities.trim().toLowerCase();
        if (filter.isEmpty()) {
            return true;
        }
        if (filter.contains("player")) {
            return entity instanceof PlayerEntity;
        }
        return entity.getType().toString().toLowerCase().contains(filter);
    }

    /** Body/armor render alpha for the Transparent Players module; 1f = fully opaque, always occlusion-tested normally. */
    public static float transparentPlayerAlpha(Entity entity, boolean armorPiece) {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.transparentPlayers || !(entity instanceof PlayerEntity) || mc.player == null) {
            return 1f;
        }
        boolean self = entity == mc.player;
        if (self && !c.transparentSelf) {
            return 1f;
        }
        if (c.transparentOnlyDamaged && ((PlayerEntity) entity).hurtTime <= 0) {
            return 1f;
        }
        float pct = armorPiece ? c.transparentArmorOpacity : c.transparentBodyOpacity;
        return Math.max(0f, Math.min(100f, pct)) / 100f;
    }

    public static boolean showPlayerGlow(Entity entity) {
        ClientConfig c = cfg();
        return c != null && c.renders && c.rendersPlayersGlow && entity instanceof PlayerEntity;
    }

    public static boolean showEndCrystalGlow() {
        ClientConfig c = cfg();
        return c != null && c.renders && c.rendersEndCrystalsGlow;
    }

    /**
     * Shield state color for the world-space indicator (Shield Tweaks): green = ready, yellow =
     * rising (raised but not yet registered by the server), red = disabled/on cooldown. Modeled
     * after the MIT-licensed ShieldStatus mod's color scheme (same default colors), reimplemented
     * here using this project's own render path since ShieldStatus's approach of recoloring the
     * shield item texture needs a shader-color hook that Minecraft 1.21.11 removed.
     */
    private static final java.util.Map<java.util.UUID, Long> shieldDisabledUntil = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long SHIELD_DISABLE_MS = 5000L;

    /**
     * The server only sends cooldown packets to the player whose item actually went on cooldown -
     * it never tells observers when SOMEONE ELSE's shield breaks, so getItemCooldownManager() is
     * always empty for other players. The ITEM_SHIELD_BREAK sound, on the other hand, is broadcast
     * positionally to everyone nearby, so this hooks that sound (see SoundSystemMixin) and estimates
     * which player it belongs to by proximity - the same trick the real ShieldFixes mod uses.
     */
    public static void onShieldBreakSound(Object sound) {
        try {
            if (!(sound instanceof net.minecraft.client.sound.SoundInstance si)) {
                return;
            }
            String id = si.getId() == null ? "" : si.getId().toString().toLowerCase(java.util.Locale.ROOT);
            if (!id.contains("shield") || !id.contains("break")) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) {
                return;
            }
            PlayerEntity nearest = null;
            double best = 4.0 * 4.0;
            for (PlayerEntity p : mc.world.getPlayers()) {
                double d = p.squaredDistanceTo(si.getX(), si.getY(), si.getZ());
                if (d < best) {
                    best = d;
                    nearest = p;
                }
            }
            if (nearest != null) {
                shieldDisabledUntil.put(nearest.getUuid(), System.currentTimeMillis() + SHIELD_DISABLE_MS);
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean isShield(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            if (stack.isOf(net.minecraft.item.Items.SHIELD)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        String n = stack.getItem().toString().toLowerCase(java.util.Locale.ROOT);
        if (n.contains("shield")) {
            return true;
        }
        try {
            String path = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).getPath();
            return path.contains("shield");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void markShieldDisabled(PlayerEntity holder) {
        if (holder == null) {
            return;
        }
        shieldDisabledUntil.put(holder.getUuid(), System.currentTimeMillis() + SHIELD_DISABLE_MS);
    }

    public static boolean shieldDisabled(PlayerEntity holder) {
        if (holder == null) {
            return false;
        }
        net.minecraft.item.ItemStack shield = null;
        for (net.minecraft.item.ItemStack stack : new net.minecraft.item.ItemStack[]{
                holder.getMainHandStack(), holder.getOffHandStack()}) {
            if (isShield(stack)) {
                shield = stack;
                break;
            }
        }
        if (shield == null) {
            return false;
        }
        try {
            var cd = holder.getItemCooldownManager();
            if (cd != null && cd.isCoolingDown(shield)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        Long until = shieldDisabledUntil.get(holder.getUuid());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() < until) {
            return true;
        }
        shieldDisabledUntil.remove(holder.getUuid());
        return false;
    }

    private static final ThreadLocal<PlayerEntity> shieldHolder = new ThreadLocal<>();

    public static void pushShieldHolder(PlayerEntity holder) {
        shieldHolder.set(holder);
    }

    public static void popShieldHolder(PlayerEntity previous) {
        if (previous == null) {
            shieldHolder.remove();
        } else {
            shieldHolder.set(previous);
        }
    }

    public static PlayerEntity currentShieldHolder() {
        return shieldHolder.get();
    }

    public static Integer shieldStateColor(ClientConfig c, PlayerEntity holder) {
        if (c == null || !c.shieldTweaks || holder == null) {
            return null;
        }
        boolean has = false;
        for (net.minecraft.item.ItemStack stack : new net.minecraft.item.ItemStack[]{
                holder.getMainHandStack(), holder.getOffHandStack()}) {
            if (isShield(stack)) {
                has = true;
                break;
            }
        }
        if (!has) {
            return null;
        }
        if (shieldDisabled(holder)) {
            return c.shieldDisabled ? c.shieldDisabledColor : 0xFFE05555;
        }
        if (holder.isBlocking() && c.shieldBlocking) {
            return c.shieldBlockingColor;
        }
        return c.shieldReady ? c.shieldReadyColor : 0xFF4CD964;
    }

    /** ARGB multiply-tint for the held shield model, including opacity. */
    public static Integer shieldModelTint() {
        ClientConfig c = cfg();
        PlayerEntity holder = shieldHolder.get();
        if (c == null || !c.shieldTweaks || holder == null) {
            return null;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c.shieldOwnOnly && mc.player != null && holder != mc.player) {
            return null;
        }
        int rgb;
        if (shieldDisabled(holder) && c.shieldDisabled) {
            rgb = c.shieldDisabledColor;
        } else if (holder.isBlocking() && c.shieldBlocking) {
            rgb = c.shieldBlockingColor;
        } else if (c.shieldReady) {
            rgb = c.shieldReadyColor;
        } else {
            rgb = 0xFFFFFFFF;
        }
        int srcA = (rgb >>> 24) & 0xFF;
        if (srcA == 0) {
            srcA = 255;
        }
        int a = Math.max(1, Math.min(255, Math.round(c.shieldOpacity / 100f * srcA)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    public static boolean showHitbox(Entity entity) {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.hitboxes || entity == null || mc.player == null || entity == mc.player) {
            return false;
        }
        String n = entity.getType().toString().toLowerCase();
        if (entity instanceof PlayerEntity) {
            return c.hitboxPlayers;
        }
        if (n.contains("end_crystal")) {
            return c.hitboxEndCrystals;
        }
        if (n.contains("arrow") || n.contains("trident")) {
            return c.hitboxArrows;
        }
        if (n.contains("ender_pearl")) {
            return c.hitboxPearls;
        }
        return c.hitboxEntities;
    }

    /** Matches your own kill in a chat death-message line for the Auto Text module (no server hook exists client-side). */
    public static boolean matchesOwnKill(String chatLine) {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.autoText || chatLine == null || mc.player == null) {
            return false;
        }
        String name = dev.aero.client.auth.AccountManager.currentName();
        if (name == null || name.isBlank()) {
            return false;
        }
        String line = chatLine.toLowerCase(java.util.Locale.ROOT);
        String self = name.toLowerCase(java.util.Locale.ROOT);
        if (!line.contains(" by " + self)) {
            return false;
        }
        return withinAutoTextRange(line);
    }

    private static boolean withinAutoTextRange(String lineLower) {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || mc.player == null || mc.world == null) {
            return true;
        }
        double range = Math.max(4, c.autoTextKillRange);
        boolean namedSomeone = false;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) {
                continue;
            }
            String n = p.getName().getString();
            if (n == null || n.isBlank()) {
                continue;
            }
            if (!lineLower.contains(n.toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            namedSomeone = true;
            if (mc.player.distanceTo(p) <= range) {
                return true;
            }
        }
        return !namedSomeone;
    }

    private static long autoTextCooldownUntilMs = 0L;
    private static int autoTextPendingTicks = -1;

    public static void onChatLine(String line) {
        ClientConfig c = cfg();
        if (c == null || !c.autoText || line == null || line.isBlank()) {
            return;
        }
        boolean kill = matchesOwnKill(line);
        boolean trigger = c.autoTextChatTrigger != null && !c.autoTextChatTrigger.isBlank()
                && line.toLowerCase(java.util.Locale.ROOT).contains(c.autoTextChatTrigger.toLowerCase(java.util.Locale.ROOT));
        if (!kill && !trigger) {
            return;
        }
        if (System.currentTimeMillis() < autoTextCooldownUntilMs) {
            return;
        }
        autoTextPendingTicks = Math.max(0, Math.round(c.autoTextDelayTicks));
    }

    public static void tickAutoText(MinecraftClient mc) {
        if (autoTextPendingTicks < 0) {
            return;
        }
        if (autoTextPendingTicks > 0) {
            autoTextPendingTicks--;
            return;
        }
        autoTextPendingTicks = -1;
        ClientConfig c = cfg();
        if (c == null || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        String msg = c.autoTextMessage;
        if (msg == null || msg.isBlank()) {
            return;
        }
        try {
            mc.getNetworkHandler().sendChatMessage(msg);
        } catch (Throwable ignored) {
        }
        autoTextCooldownUntilMs = System.currentTimeMillis() + (long) (Math.max(0, c.autoTextCooldown) * 1000);
    }

    private static final Map<java.util.UUID, Integer> TOTEM_POPS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void onTotemPop(net.minecraft.entity.LivingEntity entity) {
        if (entity == null) {
            return;
        }
        TOTEM_POPS.merge(entity.getUuid(), 1, Integer::sum);
    }

    public static int totemPopsFor(java.util.UUID uuid) {
        return uuid == null ? 0 : TOTEM_POPS.getOrDefault(uuid, 0);
    }

    public static final String[] EMOTE_PRESETS = {"👋 wave", "gg", "o7", "?", "nice", "gl hf"};

    public static dev.aero.client.module.Module findModule(String name) {
        if (AeroClient.MODULES == null) {
            return null;
        }
        for (var m : AeroClient.MODULES.all) {
            if (m.name.equals(name)) {
                return m;
            }
        }
        return null;
    }

    /** Emotes: reuses the module's own generic Toggle Key as a hold-to-open key for the wheel. */
    public static boolean emoteWheelOpen() {
        ClientConfig c = cfg();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c == null || !c.emotes || mc.currentScreen != null || mc.getWindow() == null) {
            return false;
        }
        var module = findModule("Emotes");
        if (module == null) {
            return false;
        }
        int key = module.style().toggleKey;
        if (key < 0) {
            return false;
        }
        try {
            return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final Map<String, String> TIER_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<String> TIER_INFLIGHT = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * TierTagger: async, read-only lookup of a player's public PvP tier from mctiers.com's real
     * API (verified against the live endpoint: GET https://mctiers.com/api/rankings/{uuid, no
     * dashes} returns an object keyed by gamemode - "axe"/"nethop"/"smp"/"uhc"/"sword"/"pot"/
     * "vanilla"/"mace" - each holding {"tier": 1-5, "pos", "peak_tier", "peak_pos", "attained",
     * "retired"}). Uses the entity's own UUID directly, no separate name-to-UUID lookup needed.
     */
    public static String tierFor(java.util.UUID uuid, String gamemode) {
        ClientConfig c = cfg();
        if (c == null || !c.tierTagger || uuid == null) {
            return "";
        }
        String key = uuid + ":" + gamemode;
        String cached = TIER_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (TIER_INFLIGHT.add(key)) {
            String id = uuid.toString().replace("-", "");
            String gm = (gamemode == null || gamemode.isBlank() ? "vanilla" : gamemode).toLowerCase(java.util.Locale.ROOT);
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                String tier = "";
                try {
                    var client = java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(3)).build();
                    var request = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create("https://mctiers.com/api/rankings/" + id))
                            .timeout(java.time.Duration.ofSeconds(3))
                            .GET().build();
                    var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        java.util.regex.Matcher gmBlock = java.util.regex.Pattern
                                .compile("\"" + java.util.regex.Pattern.quote(gm) + "\"\\s*:\\s*\\{([^}]*)}")
                                .matcher(body);
                        if (gmBlock.find()) {
                            java.util.regex.Matcher tierNum = java.util.regex.Pattern
                                    .compile("\"tier\"\\s*:\\s*(\\d+)").matcher(gmBlock.group(1));
                            if (tierNum.find()) {
                                tier = "T" + tierNum.group(1);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
                TIER_CACHE.put(key, tier);
                TIER_INFLIGHT.remove(key);
            });
        }
        return "";
    }
}
