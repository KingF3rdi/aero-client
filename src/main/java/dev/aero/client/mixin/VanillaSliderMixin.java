package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.ui.UiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla UI style: sliders as a glass pill with an accent fill and a round handle. */
@Mixin(value = SliderWidget.class, priority = 2200)
public abstract class VanillaSliderMixin {
    @Shadow protected double value;

    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$slider(DrawContext context, int mx, int my, float delta, CallbackInfo ci) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.vanillaUi) {
            return;
        }
        SliderWidget w = (SliderWidget) (Object) this;
        int x = w.getX();
        int y = w.getY();
        int bw = w.getWidth();
        int bh = w.getHeight();
        boolean hover = (w.isHovered() || w.isFocused()) && w.active;
        int r = Math.min(bh / 2, 10);
        UiDraw.roundRect(context, x, y, bw, bh, r, 0x9912111A);
        int fill = (int) Math.round((bw - bh) * Math.max(0, Math.min(1, value))) + bh / 2;
        UiDraw.roundRect(context, x, y, Math.max(bh, fill), bh, r, UiDraw.withAlpha(UiDraw.accent(), hover ? 0x88 : 0x55));
        UiDraw.roundBorder(context, x, y, bw, bh, r, hover ? UiDraw.withAlpha(UiDraw.accent(), 0xCC) : 0x30FFFFFF);
        int hx = x + (int) Math.round((bw - 8) * Math.max(0, Math.min(1, value)));
        UiDraw.roundRect(context, hx, y + 1, 8, bh - 2, 4, w.active ? 0xFFF6F3FB : 0xFF7A748A);
        var tr = MinecraftClient.getInstance().textRenderer;
        context.drawCenteredTextWithShadow(tr, w.getMessage(), x + bw / 2, y + (bh - 8) / 2, w.active ? 0xFFF6F3FB : 0xFF7A748A);
        ci.cancel();
    }
}
