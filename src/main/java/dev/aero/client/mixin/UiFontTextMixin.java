package dev.aero.client.mixin;

import dev.aero.client.ui.UiFont;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.StyleSpriteSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Glyph lookups (widths and immediate draws) use the smooth menu font while it is active. */
@Mixin(TextRenderer.class)
public class UiFontTextMixin {
    @ModifyVariable(method = "getGlyphs", at = @At("HEAD"), argsOnly = true)
    private StyleSpriteSource aero$font(StyleSpriteSource source) {
        return UiFont.map(source);
    }
}
