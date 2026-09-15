package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientWorld.class, priority = 2000)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$sky(CallbackInfoReturnable<Vec3d> cir) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.ambience) {
            return;
        }
        ClientWorld world = (ClientWorld) (Object) this;
        String dim = "";
        try {
            dim = world.getRegistryKey().getValue().toString().toLowerCase();
        } catch (Throwable ignored) {
        }
        if (AeroClient.CONFIG.netherSky && dim.contains("nether")) {
            cir.setReturnValue(rgb(AeroClient.CONFIG.netherColor, 0.42, 0.18, 0.16));
            return;
        }
        if (AeroClient.CONFIG.endSky && dim.contains("end")) {
            cir.setReturnValue(rgb(AeroClient.CONFIG.endColor, 0.12, 0.04, 0.18));
            return;
        }
        if ((AeroClient.CONFIG.overworldSky || AeroClient.CONFIG.skyGradient)
                && !dim.contains("nether") && !dim.contains("end")) {
            int color = AeroClient.CONFIG.skyGradient ? AeroClient.CONFIG.gradientColor : AeroClient.CONFIG.skyColor;
            cir.setReturnValue(rgb(color, 0.52, 0.46, 0.78));
        }
    }

    private static Vec3d rgb(int argb, double fr, double fg, double fb) {
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        if (r == 0 && g == 0 && b == 0) {
            return new Vec3d(fr, fg, fb);
        }
        return new Vec3d(r / 255.0, g / 255.0, b / 255.0);
    }
}
