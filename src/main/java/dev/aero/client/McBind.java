package dev.aero.client;

import dev.aero.client.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticlesMode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Applies module flags to live Minecraft options every tick. A module that forces an option remembers the
 * player's own value and puts it back when the module is switched off. Direct calls only: reflection by
 * method name breaks in the released jar, where Minecraft's names are intermediary ones.
 * (Fullbright is not an option here - it drives the lightmap, see LightmapTextureManagerMixin.)
 */
public final class McBind {
    private static final Map<String, Object> BACKUP = new HashMap<>();

    private McBind() {}

    public static void apply(MinecraftClient mc) {
        ClientConfig c = AeroClient.CONFIG;
        if (c == null || mc == null || mc.options == null) {
            return;
        }
        GameOptions opt = mc.options;
        try {
            bind("clouds", opt.getCloudRenderMode(), c.cloudsOff, CloudRenderMode.OFF);
            bind("particles", opt.getParticles(), c.particleLimiter, ParticlesMode.MINIMAL);
            bind("entityDistance", opt.getEntityDistanceScaling(), c.entityDistance,
                    Math.max(0.5, Math.min(5.0, c.entityRange / 64.0)));
            bind("distortion", opt.getDistortionEffectScale(), c.guiTweaks, 0.0);
            bind("fovEffect", opt.getFovEffectScale(), c.guiTweaks, 0.0);
            boolean bow = c.crossbowTweaks && usingRanged(mc);
            bind("bob", opt.getBobView(), bow || c.noHurtcam || c.noBobbing, false);
            bind("shadows", opt.getEntityShadows(), c.noShadows, false);
            bind("graphics", opt.getPreset(), c.fastGraphics, GraphicsMode.FAST);
            bind("sneakToggle", opt.getSneakToggled(), c.toggleSneak, true);
            bind("mute", opt.getSoundVolumeOption(net.minecraft.sound.SoundCategory.MASTER),
                    c.unfocusedCpu && c.unfocusedMute && !mc.isWindowFocused(), 0.0);
        } catch (Throwable ignored) {
        }

        if ((c.toggleSprint || c.alwaysSprint) && mc.player != null && opt.forwardKey.isPressed() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
        }
    }

    /** While on: force value (remembering the player's own). When switched off: restore it once. */
    @SuppressWarnings("unchecked")
    private static <T> void bind(String key, SimpleOption<T> option, boolean on, T value) {
        if (on) {
            BACKUP.putIfAbsent(key, option.getValue());
            if (!Objects.equals(option.getValue(), value)) {
                option.setValue(value);
            }
        } else if (BACKUP.containsKey(key)) {
            option.setValue((T) BACKUP.remove(key));
        }
    }

    private static boolean usingRanged(MinecraftClient mc) {
        if (mc.player == null || !mc.player.isUsingItem()) {
            return false;
        }
        var item = mc.player.getActiveItem();
        return item.isOf(Items.BOW) || item.isOf(Items.CROSSBOW) || item.isOf(Items.TRIDENT);
    }
}
