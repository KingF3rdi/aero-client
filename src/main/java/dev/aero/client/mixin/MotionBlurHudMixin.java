package dev.aero.client.mixin;

import dev.aero.client.MotionBlurState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameHud.class, priority = 2100)
public class MotionBlurHudMixin {
    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void aero$blur(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        MotionBlurState.update(mc);
        MotionBlurState.apply(mc);
    }
}
