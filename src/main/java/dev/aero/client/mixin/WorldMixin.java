package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noRain(float delta, CallbackInfoReturnable<Float> cir) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noWeather) {
            cir.setReturnValue(0.0F);
            return;
        }
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.weatherChanger) {
            boolean clear = AeroClient.CONFIG.weatherClear || "Clear".equalsIgnoreCase(AeroClient.CONFIG.weatherMode);
            cir.setReturnValue(clear ? 0.0F : 1.0F);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noWeather) {
            cir.setReturnValue(0.0F);
            return;
        }
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.weatherChanger) {
            cir.setReturnValue("Thunder".equalsIgnoreCase(AeroClient.CONFIG.weatherMode) ? 1.0F : 0.0F);
        }
    }

    @Inject(method = {"getTimeOfDay", "getLunarTime"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$time(CallbackInfoReturnable<Long> cir) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.timeChanger) {
            cir.setReturnValue(dev.aero.client.Visuals.timeOfDay());
        }
    }
}
