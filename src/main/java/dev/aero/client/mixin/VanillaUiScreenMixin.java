package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.ui.UiDraw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla UI style: accent-colored rounded frame around inventory/container screens. */
@Mixin(value = HandledScreen.class, priority = 2200)
public abstract class VanillaUiScreenMixin {
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"), require = 0)
    private void aero$frame(DrawContext context, int mx, int my, float delta, CallbackInfo ci) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.vanillaUi) {
            return;
        }
        int acc = UiDraw.accent();
        UiDraw.roundBorder(context, x - 2, y - 2, backgroundWidth + 4, backgroundHeight + 4, 8, UiDraw.withAlpha(acc, 0xA0));
        UiDraw.roundRect(context, x + 8, y - 2, Math.max(0, backgroundWidth - 16), 2, 1, acc);
    }
}
