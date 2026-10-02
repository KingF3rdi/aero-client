package dev.aero.client.mixin;

import dev.aero.client.ui.PackPreview;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.resource.ResourcePackCompatibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * Resource pack list entries: remember the hovered pack for the texture preview, and stop old packs
 * from showing the red "incompatible" warning (they almost always still work).
 */
@Mixin(targets = "net.minecraft.client.gui.screen.pack.PackListWidget$ResourcePackEntry")
public class PackEntryMixin {
    @Shadow
    @Final
    private ResourcePackOrganizer.Pack pack;

    /**
     * WrapOperation, not @Redirect: a call can only be redirected once, and mods like "No Resource Pack Warnings"
     * redirect this same call - two redirects made the resource pack list crash on open.
     */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourcePackCompatibility;isCompatible()Z"), require = 0)
    private boolean aero$quietOldPacks(ResourcePackCompatibility compatibility, Operation<Boolean> original) {
        return true;
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void aero$hover(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks, CallbackInfo ci) {
        if (hovered && pack instanceof PackOrganizerAccessor acc) {
            PackPreview.hover(acc.aero$profile(), mouseX, mouseY);
        }
    }
}
