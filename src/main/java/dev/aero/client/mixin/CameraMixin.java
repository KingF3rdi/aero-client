package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Camera.class, priority = 2000)
public class CameraMixin {
    /** Freelook: the camera uses the freelook angles instead of the player's. */
    @org.spongepowered.asm.mixin.injection.Redirect(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1), require = 0)
    private void aero$freelookRotation(Camera camera, float yaw, float pitch) {
        if (dev.aero.client.Freelook.active()) {
            ((CameraInvoker) camera).aero$setRotation(dev.aero.client.Freelook.yaw(), dev.aero.client.Freelook.pitch());
        } else {
            ((CameraInvoker) camera).aero$setRotation(yaw, pitch);
        }
    }

    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noFog(CallbackInfoReturnable<CameraSubmersionType> cir) {
        if (AeroClient.CONFIG != null && (AeroClient.CONFIG.noFog || AeroClient.CONFIG.cleanWater)) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
