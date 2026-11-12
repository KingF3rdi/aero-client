package dev.aero.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.function.Supplier;
import net.minecraft.client.font.GlyphAtlasTexture;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Glyph atlases are sampled NEAREST, right for the pixel font but it makes the smooth menu font (Inter, rendered
 * at several times its size) jagged when shrunk onto the screen. Only the aero:ui font's atlases ("aero:ui/0", ...)
 * get linear filtering; vanilla text stays pixel-sharp.
 */
@Mixin(GlyphAtlasTexture.class)
public abstract class GlyphAtlasTextureMixin extends AbstractTexture {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void aero$smoothUiFont(Supplier<String> name, TextRenderLayerSet layers, boolean hasColor, CallbackInfo ci) {
        if (name.get().startsWith("aero:ui/")) {
            this.sampler = RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR);
        }
    }
}
