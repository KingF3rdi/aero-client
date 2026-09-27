package dev.aero.client.mixin;

import dev.aero.client.ui.TitleSky;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Out-of-world menus draw the chosen Sky (title screen Sky button) instead of the vanilla panorama. */
@Mixin(Screen.class)
public class ScreenBackgroundMixin {
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$sky(DrawContext context, float deltaTicks, CallbackInfo ci) {
        if (TitleSky.active()) {
            TitleSky.draw(context, width, height);
            ci.cancel();
        }
    }
}
