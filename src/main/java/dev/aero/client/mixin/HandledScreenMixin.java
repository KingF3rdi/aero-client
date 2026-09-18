package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.hud.OverlayHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HandledScreen.class, priority = 2500)
public class HandledScreenMixin {
    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V",
            at = @At("HEAD"), require = 0)
    private void aero$highlight(DrawContext context, Slot slot, int x, int y, CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.itemHighlighter || !c.highlightInventories || slot == null) {
            return;
        }
        ItemStack stack = slot.getStack();
        int col = OverlayHud.highlightBg(c, stack);
        if (col == 0) {
            return;
        }
        int sx = slot.x;
        int sy = slot.y;
        context.fill(sx, sy, sx + 16, sy + 16, col);
    }
}
