package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ParticleManager.class, priority = 2000)
public class ParticleManagerMixin {
    private static int spawned;
    private static long window;

    /** The client handles the totem-pop status (35) itself and starts this emitter without ever calling
     * Entity.handleStatus, so this is the one reliable place that fires exactly once per pop. */
    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V", at = @At("HEAD"), require = 0)
    private void aero$totemPop(net.minecraft.entity.Entity entity, net.minecraft.particle.ParticleEffect effect, int maxAge, CallbackInfo ci) {
        if (effect == net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING && entity instanceof net.minecraft.entity.LivingEntity living) {
            dev.aero.client.Visuals.onTotemPop(living);
        }
    }

    /** Remember which particle type is being spawned, so the filters below can go by its registry id. */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), require = 0)
    private void aero$typeStart(net.minecraft.particle.ParticleEffect effect, double x, double y, double z,
                                double vx, double vy, double vz, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Particle> cir) {
        var id = net.minecraft.registry.Registries.PARTICLE_TYPE.getId(effect.getType());
        dev.aero.client.Visuals.currentParticle = id == null ? "" : id.getPath();
    }

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("RETURN"), require = 0)
    private void aero$typeEnd(net.minecraft.particle.ParticleEffect effect, double x, double y, double z,
                              double vx, double vy, double vz, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Particle> cir) {
        dev.aero.client.Visuals.currentParticle = "";
    }

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
