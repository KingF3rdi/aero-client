package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.hud.OverlayHud;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Armor HUD "Durability on all items" (like Durability Viewer): percent on every damaged item in slots. */
@Mixin(DrawContext.class)
public class DrawContextDurabilityMixin {
    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
            at = @At("TAIL"), require = 0)
    private void aero$durability(TextRenderer textRenderer, ItemStack stack, int x, int y, String countText, CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c == null || !c.armorHud || !c.durabilityOnItems || stack.isEmpty() || !stack.isDamaged()) {
            return;
        }
        float d = OverlayHud.durability(stack);
        if (d < 0f) {
            return;
        }
        DrawContext self = (DrawContext) (Object) this;
        String pct = Math.round(d * 100f) + "%";
        self.getMatrices().pushMatrix();
        self.getMatrices().translate(x + 1, y + 1);
        self.getMatrices().scale(0.5f, 0.5f);
        self.drawText(textRenderer, Text.literal(pct), 0, 0, OverlayHud.durabilityColor(d), true);
        self.getMatrices().popMatrix();
    }
}
