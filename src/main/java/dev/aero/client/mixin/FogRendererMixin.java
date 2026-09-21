package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.SkyFx;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sky Changer: pulls the distance fog toward the chosen sky color so the horizon matches (overworld, not underwater/in lava). */
@Mixin(value = FogRenderer.class, priority = 2000)
public class FogRendererMixin {
    @Inject(method = "getFogColor(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)Lorg/joml/Vector4f;",
            at = @At("RETURN"), require = 0)
    private void aero$fog(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float darkness,
                          CallbackInfoReturnable<Vector4f> cir) {
        if (!SkyFx.on() || !AeroClient.CONFIG.skyFogMatch || camera.getSubmersionType() != CameraSubmersionType.NONE
                || !world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) {
            return;
        }
        // keep the day/night brightness of the original fog: scale the sky color by its luminance ratio
        Vector4f v = cir.getReturnValue();
        float orig = Math.max(0.02f, Math.max(v.x, Math.max(v.y, v.z)));
        int rgb = SkyFx.skyScaled();
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float top = Math.max(0.02f, Math.max(r, Math.max(g, b)));
        float dim = Math.min(1f, orig / 0.75f); // ~0.75 is the brightest daytime fog channel
        v.set(r / top * dim * top, g / top * dim * top, b / top * dim * top, v.w);
    }
}
