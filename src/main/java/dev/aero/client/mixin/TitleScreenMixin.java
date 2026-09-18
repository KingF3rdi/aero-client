package dev.aero.client.mixin;

import dev.aero.client.ui.UiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Small "AERO CLIENT" wordmark in the title screen's otherwise-empty top-left corner, in the same
 * accent blue and mark glyph as the launcher app - drawn as an overlay on top of vanilla's own
 * render rather than replacing the screen, so every vanilla button/notification keeps working
 * exactly as before. */
@Mixin(value = TitleScreen.class, priority = 2000)
public class TitleScreenMixin {
    private static final int ACCENT = 0xFF4F8EFF;

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"), require = 0)
    private void aero$brand(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var tr = MinecraftClient.getInstance().textRenderer;
        int x = 10;
        int y = 10;
        UiDraw.aeroMark(context, x, y, 12, ACCENT);
        context.drawText(tr, Text.literal("AERO CLIENT"), x + 16, y + 2, ACCENT, true);
    }
}
