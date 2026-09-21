package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import dev.aero.client.ui.UiDraw;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shulker Tooltips: hovering a shulker box in any inventory screen shows its contents as a 9x3 grid. */
@Mixin(value = HandledScreen.class, priority = 2500)
public abstract class ShulkerTooltipMixin {
    @Shadow protected Slot focusedSlot;

    private static boolean aero$shift(MinecraftClient mc) {
        long h = mc.getWindow().getHandle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    @Inject(method = "drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("RETURN"), require = 0)
    private void aero$shulker(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        var cfg = AeroClient.CONFIG;
        if (cfg == null || !cfg.shulkerTooltips || focusedSlot == null) {
            return;
        }
        ItemStack stack = focusedSlot.getStack();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock)) {
            return;
        }
        var mc = MinecraftClient.getInstance();
        if (cfg.shulkerTooltipsShift && !aero$shift(mc)) {
            return;
        }
        ContainerComponent comp = stack.get(DataComponentTypes.CONTAINER);
        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        if (comp != null) {
            comp.copyTo(items);
        }

        int w = 9 * 18 + 8;
        int h = 3 * 18 + 8;
        int x = Math.max(2, Math.min(context.getScaledWindowWidth() - w - 2, mouseX + 8));
        int y = mouseY - h - 8;
        if (y < 2) {
            y = Math.min(context.getScaledWindowHeight() - h - 2, mouseY + 28);
        }
        context.createNewRootLayer();
        UiDraw.glass(context, x, y, w, h, 0xF014121E, 10);
        for (int i = 0; i < 27; i++) {
            int sx = x + 4 + (i % 9) * 18;
            int sy = y + 4 + (i / 9) * 18;
            ItemStack s = items.get(i);
            if (s.isEmpty() && !cfg.shulkerTooltipsEmptySlots) {
                continue;
            }
            UiDraw.roundRect(context, sx, sy, 17, 17, 3, 0x33FFFFFF);
            if (!s.isEmpty()) {
                context.drawItem(s, sx + 1, sy + 1);
                context.drawStackOverlay(mc.textRenderer, s, sx + 1, sy + 1);
            }
        }
    }
}
