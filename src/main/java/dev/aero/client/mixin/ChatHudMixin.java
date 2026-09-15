package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.Visuals;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatHud.class)
public class ChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true, require = 0)
    private Text aero$stamp(Text message) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.chatTimestamps || message == null) {
            return message;
        }
        return Text.literal(Visuals.clockPrefix()).append(message);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), index = 4, require = 0)
    private int aero$noChatFill(int color) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noChatBg && ((color & 0x00FFFFFF) == 0)) {
            return 0;
        }
        return color;
    }

    @Inject(method = {"drawBackground", "renderBackground", "drawChatBackground"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noChatBg(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.noChatBg) {
            ci.cancel();
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), require = 0)
    private void aero$autoText(Text message, CallbackInfo ci) {
        if (message == null) {
            return;
        }
        dev.aero.client.Visuals.onChatLine(message.getString());
    }
}
