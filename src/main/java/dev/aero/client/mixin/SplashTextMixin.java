package dev.aero.client.mixin;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Title screen splash text in black instead of vanilla's hardcoded yellow. */
@Mixin(SplashTextRenderer.class)
public class SplashTextMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/font/DrawnTextConsumer;text(Lnet/minecraft/client/font/Alignment;IILnet/minecraft/client/font/DrawnTextConsumer$Transformation;Lnet/minecraft/text/Text;)V"),
            index = 4, require = 0)
    private Text aero$black(Text text) {
        return text.copy().withColor(0x000000);
    }
}
