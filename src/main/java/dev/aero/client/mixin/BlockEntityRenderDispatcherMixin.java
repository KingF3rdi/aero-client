package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher")
public class BlockEntityRenderDispatcherMixin {
    @Inject(method = {"render", "tryRender"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideTile(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.hideTileEntities) {
            ci.cancel();
        }
    }
}
