package dev.aero.client.mixin;

import dev.aero.client.ui.InventoryScale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * RecipeBookScreen (the survival Inventory's superclass) owns mouseClicked/mouseDragged itself,
 * so the Inventory Scale click remap (see InventoryScale) has to land here instead of on
 * InventoryScreen directly. Gated to InventoryScreen only - other RecipeBookScreen users (crafting
 * table, furnace, stonecutter, ...) aren't part of the "Inventory Scale" setting.
 */
@Mixin(RecipeBookScreen.class)
public class RecipeBookScreenScaleMixin {
    @ModifyVariable(method = "mouseClicked", at = @At("HEAD"), argsOnly = true, require = 0)
    private Click aero$shrinkClicked(Click click) {
        return remap(click);
    }

    @ModifyVariable(method = "mouseDragged", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private Click aero$shrinkDraggedClick(Click click) {
        return remap(click);
    }

    @ModifyVariable(method = "mouseDragged", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private double aero$shrinkDragDX(double deltaX) {
        return applies() ? deltaX / InventoryScale.factor() : deltaX;
    }

    @ModifyVariable(method = "mouseDragged", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private double aero$shrinkDragDY(double deltaY) {
        return applies() ? deltaY / InventoryScale.factor() : deltaY;
    }

    private Click remap(Click click) {
        if (!applies()) {
            return click;
        }
        float f = InventoryScale.factor();
        return new Click(click.x() / f, click.y() / f, click.buttonInfo());
    }

    private boolean applies() {
        Screen self = (Screen) (Object) this;
        return self instanceof InventoryScreen && InventoryScale.applies(self);
    }
}
