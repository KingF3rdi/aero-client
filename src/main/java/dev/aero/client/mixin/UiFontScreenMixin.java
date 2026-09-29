package dev.aero.client.mixin;

import dev.aero.client.ui.UiFont;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Aero's own screens render with the smooth menu font (see UiFont). */
@Mixin(Screen.class)
public class UiFontScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void aero$fontOn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        UiFont.active = UiFont.enabled() && getClass().getName().startsWith("dev.aero.");
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void aero$fontOff(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        UiFont.active = false;
    }
}
