package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.sound.SoundSystem", priority = 2000)
public class SoundSystemMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$cutSound(Object sound, CallbackInfo ci) {
        Visuals.onShieldBreakSound(sound);
        if (Visuals.skipSound(sound)) {
            ci.cancel();
        }
    }
}
