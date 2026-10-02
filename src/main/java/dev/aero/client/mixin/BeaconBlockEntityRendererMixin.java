package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer", priority = 2000)
public class BeaconBlockEntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noBeacon(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noBeacons) {
            ci.cancel();
        }
    }

    // 1.21.11: beams go through the render command queue (both static renderBeam overloads, all void).
    @Inject(method = "renderBeam", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aero$noBeaconStatic(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noBeacons) {
            ci.cancel();
        }
    }
}
