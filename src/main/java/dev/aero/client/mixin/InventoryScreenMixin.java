package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.minecraft.client.gui.screen.ingame.InventoryScreen",
        "net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen"
}, priority = 2000)
public class InventoryScreenMixin {
    @Inject(method = {"drawEntity", "drawPlayer"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void aero$hideModel(CallbackInfo ci) {
        if (dev.aero.client.cosmetic.CosmeticPreview.drawing) {
            return;
        }
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.hideInvModel
                && !AeroClient.CONFIG.hideInvTransparent) {
            ci.cancel();
        }
    }
}
