package dev.aero.client.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prepends the small blue "A" mark used across the launcher/mod branding to every player's name
 * in the tab list (all entries, not just self) - client-side only, so it's a personal touch on how
 * names are shown to this player rather than something broadcast to others. */
@Mixin(value = PlayerListHud.class, priority = 2000)
public class PlayerListHudMixin {
    private static final Style MARK_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x4F8EFF)).withBold(true);

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$badge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        MutableText marked = Text.literal("A ").setStyle(MARK_STYLE).append(cir.getReturnValue());
        cir.setReturnValue(marked);
    }
}
