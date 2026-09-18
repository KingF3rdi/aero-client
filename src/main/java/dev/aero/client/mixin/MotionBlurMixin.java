package dev.aero.client.mixin;

import dev.aero.client.MotionBlurState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Real motion blur: feeds the camera-speed radius to the global blur setting and runs vanilla's blur pass over the world. */
@Mixin(value = GameRenderer.class, priority = 2000)
public class MotionBlurMixin {
    @ModifyArg(method = "render(Lnet/minecraft/client/render/RenderTickCounter;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlobalSettings;set(IIDJLnet/minecraft/client/render/RenderTickCounter;ILnet/minecraft/client/render/Camera;Z)V"),
            index = 5, require = 0)
    private int aero$blurRadius(int original) {
        int r = MotionBlurState.radius();
        return r > 0 ? r : original;
    }
}
