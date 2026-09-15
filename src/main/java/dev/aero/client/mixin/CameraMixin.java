package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noFog(CallbackInfoReturnable<CameraSubmersionType> cir) {
        if (AeroClient.CONFIG != null && (AeroClient.CONFIG.noFog || AeroClient.CONFIG.cleanWater)) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
