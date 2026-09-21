package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.ui.UiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

/**
 * Custom loading screen: the startup overlay keeps all of vanilla's reload/fade logic, but its Mojang
 * background, logo and progress bar are swapped for the Aero look (dark background, mark, accent bar).
 */
@Mixin(value = SplashOverlay.class, priority = 2000)
public abstract class SplashOverlayMixin {
    private static final int BG = 0xFF07080D;

    @Shadow private float progress;

    private static boolean aero$on() {
        return AeroClient.CONFIG == null || AeroClient.CONFIG.customLoadingScreen;
    }

    @Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"), require = 0)
    private int aero$background(IntSupplier brand) {
        return aero$on() ? BG : brand.getAsInt();
    }

    /** First logo half: draw the Aero mark and name instead; the second half is skipped. */
    @Redirect(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIIIIII)V"),
            require = 0)
    private void aero$logo(DrawContext context, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, net.minecraft.util.Identifier id,
                           int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {
        if (!aero$on()) {
            context.drawTexture(pipeline, id, x, y, u, v, w, h, rw, rh, tw, th, color);
            return;
        }
        if (u >= 0f) {
            return;
        }
        int alpha = (color >>> 24) & 0xFF;
        int cx = context.getScaledWindowWidth() / 2;
        int cy = (int) (context.getScaledWindowHeight() * 0.42);
        var tr = MinecraftClient.getInstance().textRenderer;
        int accent = (alpha << 24) | (UiDraw.accent() & 0xFFFFFF);
        UiDraw.aeroMark(context, cx - 24, cy - 40, 48, accent);
        String name = "AERO CLIENT";
        context.drawText(tr, Text.literal(name), cx - tr.getWidth(name) / 2, cy + 18, (alpha << 24) | 0xF2F4FB, false);
        String sub = "Loading";
        context.drawText(tr, Text.literal(sub), cx - tr.getWidth(sub) / 2, cy + 32, (alpha << 24) | 0x9AA3B8, false);
    }

    @Inject(method = "renderProgressBar(Lnet/minecraft/client/gui/DrawContext;IIIIF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$bar(DrawContext context, int x1, int y1, int x2, int y2, float opacity, CallbackInfo ci) {
        if (!aero$on()) {
            return;
        }
        int a = Math.round(MathHelperClamp(opacity) * 255f);
        int w = x2 - x1;
        int cy = (y1 + y2) / 2;
        int bw = Math.min(w, 200);
        int bx = (x1 + x2) / 2 - bw / 2;
        context.fill(bx, cy - 1, bx + bw, cy + 1, (a << 24) | 0x1C2030);
        int filled = Math.round(bw * Math.max(0f, Math.min(1f, progress)));
        context.fill(bx, cy - 1, bx + filled, cy + 1, (a << 24) | (UiDraw.accent() & 0xFFFFFF));
        ci.cancel();
    }

    private static float MathHelperClamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
