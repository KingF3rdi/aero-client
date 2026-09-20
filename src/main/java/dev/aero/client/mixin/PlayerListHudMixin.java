package dev.aero.client.mixin;

import dev.aero.client.social.ClientUsers;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blue "A" for Aero Client users and TierTagger ranks in the tab list. */
@Mixin(value = PlayerListHud.class, priority = 2000)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$badge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        java.util.UUID id = entry.getProfile().id();
        if (ClientUsers.showBadge(id, entry.getProfile().name(), "tab")) {
            cir.setReturnValue(Text.empty().append(ClientUsers.badge(id)).append(cir.getReturnValue()));
        }
        var cfg = dev.aero.client.AeroClient.CONFIG;
        if (cfg != null && cfg.tierTagger && cfg.tierShowTab) {
            try {
                String tier = dev.aero.client.Visuals.tierFor(id, cfg.tierGamemode);
                if (tier != null && !tier.isBlank()) {
                    net.minecraft.text.MutableText tag = Text.empty();
                    if (cfg.tierGamemodeIcon) {
                        tag.append(dev.aero.client.Icons.mode(cfg.tierGamemode)).append(Text.literal(" "));
                    }
                    tag.append(Text.literal(tier).setStyle(net.minecraft.text.Style.EMPTY
                            .withColor(dev.aero.client.Icons.tierColor(tier)).withBold(true)));
                    cir.setReturnValue("Right".equalsIgnoreCase(cfg.tierSide)
                            ? cir.getReturnValue().copy().append(Text.literal(" ")).append(tag)
                            : Text.empty().append(tag).append(Text.literal(" ")).append(cir.getReturnValue()));
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
