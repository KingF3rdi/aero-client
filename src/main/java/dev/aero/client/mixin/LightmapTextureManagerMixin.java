package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fullbright, done like Gamma Utils: instead of pushing the gamma option past its 100 % limit (Minecraft
 * rejects that and resets it), the lightmap gets the boosted value directly. "Gamma" mode raises the
 * brightness factor, "Night vision" mode sets the night-vision strength. Both fade in and out.
 */
@Mixin(value = LightmapTextureManager.class, priority = 2000)
public class LightmapTextureManagerMixin {
    private static float aero$fade;
    private static long aero$last;

    private static float aero$amount() {
        var c = AeroClient.CONFIG;
        long now = System.currentTimeMillis();
        float dt = aero$last == 0 ? 0f : Math.min(0.1f, (now - aero$last) / 1000f);
        aero$last = now;
        float target = c != null && c.fullbright ? 1f : 0f;
        float speed = c != null && c.fullbrightFade ? 4f : 1000f; // ~250 ms fade
        aero$fade += Math.max(-speed * dt, Math.min(speed * dt, target - aero$fade));
        if (dt == 0f) {
            aero$fade = target;
        }
        return aero$fade;
    }

    private static boolean aero$nightVision() {
        var c = AeroClient.CONFIG;
        return c != null && "Night vision".equalsIgnoreCase(c.fullbrightMode);
    }

    /** 4th value: night-vision strength. */
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 3), require = 0)
    private float aero$nightVisionStrength(float vanilla) {
        float k = aero$amount();
        return aero$nightVision() ? Math.max(vanilla, k) : vanilla;
    }

    /** 7th value: brightness (gamma minus the darkness effect). */
    @ModifyArg(method = "update", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;", ordinal = 6), require = 0)
    private float aero$gamma(float vanilla) {
        var c = AeroClient.CONFIG;
        if (c == null || aero$nightVision() || aero$fade <= 0f) {
            return vanilla;
        }
        float boosted = Math.max(1f, Math.min(15f, c.brightness));
        return vanilla + (boosted - vanilla) * aero$fade;
    }
}
