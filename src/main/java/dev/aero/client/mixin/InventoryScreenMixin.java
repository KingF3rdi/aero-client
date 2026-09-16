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
    // InventoryScreen actually declares two overloads named drawEntity: the public 10-arg one that
    // draws the GUI preview widget (what we want to cancel), and a private
    // `drawEntity(LivingEntity): EntityRenderState` helper that fetches/updates the entity's shared
    // render state. An unqualified "drawEntity" target matches both, and cancelling the private one
    // makes it return null, which the public overload then dereferences unconditionally - so instead
    // of just hiding the inventory preview, this could throw/skip mid-frame while a render state for
    // a real player was being fetched elsewhere in the same call graph. Pinning the exact descriptor
    // keeps this to only the public preview-drawing overload.
    @Inject(
            method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
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
