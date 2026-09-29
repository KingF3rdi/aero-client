package dev.aero.client.mixin;

import dev.aero.client.ui.UiFont;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** GUI text is laid out after the screen finished rendering, so pin the menu font when it is submitted. */
@Mixin(TextGuiElementRenderState.class)
public class UiFontStateMixin {
    @Shadow @Final @Mutable public OrderedText orderedText;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aero$pinFont(CallbackInfo ci) {
        orderedText = UiFont.pin(orderedText);
    }
}
