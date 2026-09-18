package dev.aero.client.mixin;

import dev.aero.client.McBind;
import dev.aero.client.ui.Menus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClient.class, priority = 2000)
public class MinecraftClientMixin {
    @Unique
    private boolean aeroGuiHeld;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void aero$bind(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        McBind.apply(mc);
        if (mc.getWindow() == null) {
            return;
        }
        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !this.aeroGuiHeld && mc.world != null) {
            Screen screen = mc.currentScreen;
            String name = screen == null ? "" : screen.getClass().getName();
            boolean chat = name.endsWith("ChatScreen") || name.endsWith("SleepingChatScreen");
            if (Menus.isClickGui(screen)) {
                mc.setScreen(null);
            } else if (!chat && (screen == null || Menus.isPauseOverlay(screen))) {
                mc.setScreen(Menus.clickGui(null, false));
            }
        }
        this.aeroGuiHeld = down;
    }
}
