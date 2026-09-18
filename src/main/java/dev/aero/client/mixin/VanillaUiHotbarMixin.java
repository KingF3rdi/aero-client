package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.ui.UiDraw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla UI style: accent-colored rounded frame and selection bar on the hotbar. */
@Mixin(value = InGameHud.class, priority = 2200)
public class VanillaUiHotbarMixin {
    @Inject(method = "renderHotbar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("TAIL"), require = 0)
    private void aero$hotbarStyle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.vanillaUi) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        int x = context.getScaledWindowWidth() / 2 - 91;
        int y = context.getScaledWindowHeight() - 22;
        int acc = UiDraw.accent();
        UiDraw.roundBorder(context, x - 1, y - 1, 184, 24, 6, UiDraw.withAlpha(acc, 0x90));
        int sel = mc.player.getInventory().getSelectedSlot();
        UiDraw.roundRect(context, x + sel * 20 + 2, y + 20, 18, 2, 1, acc);
    }
}
