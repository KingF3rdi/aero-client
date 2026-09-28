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
    private static boolean aero$pingNumbers() {
        var c = dev.aero.client.AeroClient.CONFIG;
        return c != null && c.pingHud && c.pingTabNumbers;
    }

    /** Ping "Numbers in tab list" (like Better Ping Display): widen the ping column for the number. */
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method = "render", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 13), require = 0)
    private int aero$pingColumn(int original) {
        return aero$pingNumbers() ? original + 18 : original;
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$pingNumber(net.minecraft.client.gui.DrawContext context, int width, int x, int y, PlayerListEntry entry,
                                 org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!aero$pingNumbers()) {
            return;
        }
        ci.cancel();
        var c = dev.aero.client.AeroClient.CONFIG;
        int ms = entry.getLatency();
        String s = ms < 0 ? "?" : String.valueOf(ms);
        int col = ms < 0 ? 0xFFAAAAAA : !c.pingColorByLatency ? 0xFFFFFFFF
                : ms < c.pingWarnMs ? c.pingColGood : ms < c.pingBadMs ? c.pingColWarn : c.pingColBad;
        var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
        context.drawText(tr, Text.literal(s), x + width - 1 - tr.getWidth(s), y, col | 0xFF000000, true);
    }

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
