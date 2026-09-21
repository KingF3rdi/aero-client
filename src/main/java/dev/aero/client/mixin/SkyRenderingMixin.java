package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.SkyFx;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sky Changer: overrides the overworld sky color, horizon glow and star brightness in the sky render state. */
@Mixin(value = SkyRendering.class, priority = 2000)
public class SkyRenderingMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/state/SkyRenderState;)V",
            at = @At("TAIL"), require = 0)
    private void aero$sky(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (!SkyFx.on() || !"OVERWORLD".equals(String.valueOf(state.skybox))) {
            return;
        }
        var c = AeroClient.CONFIG;
        state.skyColor = 0xFF000000 | SkyFx.skyScaled();
        int glowAlpha = (state.sunriseAndSunsetColor >>> 24) & 0xFF;
        if (c.skyGlowAlways) {
            glowAlpha = 0xFF;
        }
        if (glowAlpha != 0) {
            state.sunriseAndSunsetColor = (glowAlpha << 24) | SkyFx.horizon();
        }
        if (c.skyStars) {
            state.starBrightness = 1f;
        }
    }
}
