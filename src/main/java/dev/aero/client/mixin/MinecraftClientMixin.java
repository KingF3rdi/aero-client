package dev.aero.client.mixin;

import dev.aero.client.McBind;
import dev.aero.client.ui.Menus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClient.class, priority = 2000)
public class MinecraftClientMixin {
    @Unique
    private boolean aeroGuiHeld;

    @Shadow private int itemUseCooldown;
    @Shadow private int attackCooldown;

    /** Crystal optimizer zero delay: no place/hit cooldown while holding a crystal or aiming at one. */
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void aero$zeroDelay(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        var cfg = dev.aero.client.AeroClient.CONFIG;
        if (mc.player == null || cfg == null || !cfg.crystalZeroDelay || !dev.aero.client.Optimizer.crystal()) {
            return;
        }
        if (mc.player.getMainHandStack().isOf(net.minecraft.item.Items.END_CRYSTAL)
                || mc.player.getOffHandStack().isOf(net.minecraft.item.Items.END_CRYSTAL)) {
            this.itemUseCooldown = 0;
        }
        if (mc.targetedEntity instanceof net.minecraft.entity.decoration.EndCrystalEntity) {
            this.attackCooldown = 0;
        }
    }

    @Unique
    private boolean aeroSneakHeld;

    /** No Esc Menu: Windows releases all keys on alt-tab, so re-hold sneak until the window is focused again. */
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void aero$keepSneak(CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        var cfg = dev.aero.client.AeroClient.CONFIG;
        if (mc.player == null || mc.options.getSneakToggled().getValue()) {
            return; // toggle-sneak already survives focus loss
        }
        var sneak = mc.options.sneakKey;
        if (mc.isWindowFocused()) {
            var key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(sneak);
            if (this.aeroSneakHeld && mc.currentScreen == null && key.getCategory() == net.minecraft.client.util.InputUtil.Type.KEYSYM) {
                // we faked the hold, so sync to the real key once back in the game
                sneak.setPressed(net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow(), key.getCode()));
            }
            this.aeroSneakHeld = sneak.isPressed();
        } else if (this.aeroSneakHeld && cfg != null && cfg.noEscMenu && mc.currentScreen == null) {
            sneak.setPressed(true);
        }
    }

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
            boolean chat = screen instanceof net.minecraft.client.gui.screen.ChatScreen;
            if (Menus.isClickGui(screen)) {
                mc.setScreen(null);
            } else if (!chat && (screen == null || Menus.isPauseOverlay(screen))) {
                mc.setScreen(Menus.clickGui(null, false));
            }
        }
        this.aeroGuiHeld = down;
    }
}
