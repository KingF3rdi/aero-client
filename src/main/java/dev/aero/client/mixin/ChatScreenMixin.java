package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.ChatTranslate;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chat translation: the "Translate my message" key rewrites the chat box into the chosen language. */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method = "keyPressed(Lnet/minecraft/client/input/KeyInput;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$translateOwn(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        var c = AeroClient.CONFIG;
        if (c == null || c.chatTranslateOwnKey < 0 || input.key() != c.chatTranslateOwnKey || chatField == null) {
            return;
        }
        String text = chatField.getText();
        if (text == null || text.isBlank() || text.startsWith("/")) {
            return;
        }
        TextFieldWidget field = chatField;
        field.setEditable(false);
        ChatTranslate.translate(text, ChatTranslate.code(c.chatTranslateOwnLang), out -> {
            field.setEditable(true);
            if (out != null && text.equals(field.getText())) {
                field.setText(out);
            }
        });
        cir.setReturnValue(true);
    }
}
