package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.sound.SoundSystem")
public class SoundSystemMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$cutSound(Object sound, CallbackInfo ci) {
        if (Visuals.skipSound(sound)) {
            ci.cancel();
        }
    }
}
