package dev.aero.client.mixin;

import dev.aero.client.Visuals;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SoundSystem.class, priority = 2000)
public class SoundSystemMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$cutSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        Visuals.onShieldBreakSound(sound);
        if (Visuals.skipSound(sound)) {
            cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }
}
