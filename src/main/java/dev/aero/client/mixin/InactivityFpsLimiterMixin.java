package dev.aero.client.mixin;

import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Title screen and other out-of-world menus run at the normal frame cap instead of vanilla's 60 FPS. */
@Mixin(InactivityFpsLimiter.class)
public class InactivityFpsLimiterMixin {
    @Shadow
    private int maxFps;

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$menuFps(CallbackInfoReturnable<Integer> cir) {
        if (((InactivityFpsLimiter) (Object) this).getLimitReason() == InactivityFpsLimiter.LimitReason.OUT_OF_LEVEL_MENU) {
            cir.setReturnValue(maxFps);
        }
    }
}
