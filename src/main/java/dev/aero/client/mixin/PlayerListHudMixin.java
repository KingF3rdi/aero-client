package dev.aero.client.mixin;

import dev.aero.client.social.ClientUsers;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blue "A" before the names of Aero Client users in the tab list. */
@Mixin(value = PlayerListHud.class, priority = 2000)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$badge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (ClientUsers.isUser(entry.getProfile().id())) {
            cir.setReturnValue(ClientUsers.badge().append(cir.getReturnValue()));
        }
    }
}
