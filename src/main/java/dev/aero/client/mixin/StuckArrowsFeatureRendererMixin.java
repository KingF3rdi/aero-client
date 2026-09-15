package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.minecraft.client.render.entity.feature.StuckArrowsFeatureRenderer",
        "net.minecraft.client.render.entity.feature.StuckStingersFeatureRenderer"
})
public class StuckArrowsFeatureRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$clean(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.cleanView && AeroClient.CONFIG.cleanArrows) {
            ci.cancel();
        }
    }
}
