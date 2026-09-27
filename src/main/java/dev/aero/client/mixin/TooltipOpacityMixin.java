package dev.aero.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** GUI Tweaks "Tooltip opacity %": fades the tooltip background and frame (the text stays solid). */
@Mixin(TooltipBackgroundRenderer.class)
public class TooltipOpacityMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"),
            require = 0)
    private static void aero$tooltipAlpha(DrawContext context, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        var c = dev.aero.client.AeroClient.CONFIG;
        if (c == null || !c.guiTweaks || c.guiTooltipOpacity >= 100f) {
            context.drawGuiTexture(pipeline, sprite, x, y, w, h);
            return;
        }
        int a = Math.round(Math.max(0f, c.guiTooltipOpacity) * 2.55f);
        if (a > 0) {
            context.drawGuiTexture(pipeline, sprite, x, y, w, h, (a << 24) | 0xFFFFFF);
        }
    }
}
