package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.ui.UiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla UI style: menu buttons drawn as rounded glass pills with an accent border on hover. */
@Mixin(value = PressableWidget.class, priority = 2200)
public abstract class VanillaButtonMixin {
    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$button(DrawContext context, int mx, int my, float delta, CallbackInfo ci) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.vanillaUi || !((Object) this instanceof ButtonWidget)) {
            return;
        }
        ClickableWidget w = (ClickableWidget) (Object) this;
        int x = w.getX();
        int y = w.getY();
        int bw = w.getWidth();
        int bh = w.getHeight();
        boolean hover = w.isHovered() && w.active;
        int r = Math.min(bh / 2, 10);
        UiDraw.roundRect(context, x, y, bw, bh, r, hover ? UiDraw.withAlpha(UiDraw.accent(), 0x55) : 0x9912111A);
        UiDraw.roundBorder(context, x, y, bw, bh, r, hover ? UiDraw.withAlpha(UiDraw.accent(), 0xCC) : 0x30FFFFFF);
        var tr = MinecraftClient.getInstance().textRenderer;
        int col = w.active ? 0xFFF6F3FB : 0xFF7A748A;
        context.drawCenteredTextWithShadow(tr, w.getMessage(), x + bw / 2, y + (bh - 8) / 2, col);
        ci.cancel();
    }
}
