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

    /** GUI Tweaks "Container background %": the darkening behind inventories and chests. */
    @Inject(method = "renderDarkening(Lnet/minecraft/client/gui/DrawContext;IIII)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$containerDim(DrawContext context, int x, int y, int w, int h, CallbackInfo ci) {
        var c = dev.aero.client.AeroClient.CONFIG;
        if (c == null || !c.guiTweaks || c.guiContainerOpacity >= 100f
                || !((Object) this instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?>)) {
            return;
        }
        ci.cancel();
        int a = Math.round(Math.max(0f, c.guiContainerOpacity) * 2.55f);
        if (a > 0) {
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, net.minecraft.util.Identifier.ofVanilla("textures/gui/inworld_menu_background.png"),
                    x, y, 0f, 0f, w, h, w, h, 32, 32, (a << 24) | 0xFFFFFF);
        }
    }

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$sky(DrawContext context, float deltaTicks, CallbackInfo ci) {
        if (TitleSky.active()) {
            TitleSky.draw(context, width, height);
            ci.cancel();
        }
    }
}
