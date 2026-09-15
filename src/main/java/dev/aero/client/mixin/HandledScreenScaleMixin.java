package dev.aero.client.mixin;

import dev.aero.client.ui.InventoryScale;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Scroll-wheel half of the Inventory Scale GUI Tweak (see InventoryScale), for the survival
 * Inventory only - CreativeInventoryScreen overrides mouseScrolled itself for its tab scrollbar
 * (see CreativeInventoryScaleMixin), so it's excluded here to avoid remapping it twice.
 */
@Mixin(value = HandledScreen.class, priority = 2000)
public class HandledScreenScaleMixin {
    @ModifyVariable(method = "mouseScrolled", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private double aero$shrinkScrollX(double mouseX) {
        return applies() ? mouseX / InventoryScale.factor() : mouseX;
    }

    @ModifyVariable(method = "mouseScrolled", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private double aero$shrinkScrollY(double mouseY) {
        return applies() ? mouseY / InventoryScale.factor() : mouseY;
    }

    private boolean applies() {
        Screen self = (Screen) (Object) this;
        return self instanceof InventoryScreen && InventoryScale.applies(self);
    }
}
