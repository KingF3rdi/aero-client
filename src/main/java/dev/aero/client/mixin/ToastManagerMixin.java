package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ToastManager.class, priority = 2000)
public class ToastManagerMixin {
    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noToasts(DrawContext context, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.uiBoost && AeroClient.CONFIG.uiBoostToasts) {
            ci.cancel();
        }
    }
}
