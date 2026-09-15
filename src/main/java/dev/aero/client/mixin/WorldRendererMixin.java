package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer")
public class WorldRendererMixin {
    @Inject(method = {"renderClouds", "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V"},
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noClouds(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.cloudsOff) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderWeather", "renderRain", "renderPrecipitation", "renderSnowAndRain"},
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noWeatherMesh(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noWeather) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderSky", "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V"},
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideSky(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.hideSky) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderStars", "renderSkybox"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideStars(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && (AeroClient.CONFIG.hideSky || AeroClient.CONFIG.hideStars)) {
            ci.cancel();
        }
    }

    /**
     * Block Overlay: best-effort hook into the block-outline draw call. The exact vanilla method
     * name/signature for this in the current mappings isn't confirmed against the actual game jar,
     * so several candidate names are listed and require = 0 makes this a silent no-op if none
     * match - verify in-game after building and report back if the outline color never changes.
     */
    @Inject(method = {"drawBlockOutline", "drawCursor", "method_22713"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$blockOverlaySkipVanilla(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.blockOverlay && !AeroClient.CONFIG.blockOverlayOutline) {
            ci.cancel();
        }
    }
}
