package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    private static int spawned;
    private static long window;

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$limit(Particle particle, CallbackInfo ci) {
        if (dev.aero.client.Visuals.skipParticle(particle)) {
            ci.cancel();
            return;
        }
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.particleLimiter) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - window > 1000L) {
            window = now;
            spawned = 0;
        }
        if (++spawned > AeroClient.CONFIG.maxParticles) {
            ci.cancel();
        }
    }
}
