package dev.aero.client.mixin;

import dev.aero.client.ui.InventoryScale;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Lays the inventory screens out against a shrunk "virtual" width/height so the Inventory Scale
 * GUI Tweak (see {@link InventoryScale}) can blow the panel back up with a matrix scale without
 * touching Minecraft's real GUI Scale option. init(II) is final on Screen and only ever has one
 * body, so it's safe to gate for both InventoryScreen and CreativeInventoryScreen here.
 *
 * <p>resize(II) is only handled here for InventoryScreen - CreativeInventoryScreen overrides
 * resize itself (see CreativeInventoryScaleMixin), and handling it in both places risks shrinking
 * it twice if that override calls super.resize().
 */
@Mixin(Screen.class)
public class ScreenScaleMixin {
    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private int aero$shrinkInitWidth(int width) {
        return isInitTarget() ? InventoryScale.shrink(width) : width;
    }

    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private int aero$shrinkInitHeight(int height) {
        return isInitTarget() ? InventoryScale.shrink(height) : height;
    }

    @ModifyVariable(method = "resize(II)V", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private int aero$shrinkResizeWidth(int width) {
        return isResizeTarget() ? InventoryScale.shrink(width) : width;
    }

    @ModifyVariable(method = "resize(II)V", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private int aero$shrinkResizeHeight(int height) {
        return isResizeTarget() ? InventoryScale.shrink(height) : height;
    }

    private boolean isInitTarget() {
        Screen self = (Screen) (Object) this;
        return (self instanceof InventoryScreen || self instanceof CreativeInventoryScreen) && InventoryScale.applies(self);
    }

    private boolean isResizeTarget() {
        Screen self = (Screen) (Object) this;
        return self instanceof InventoryScreen && InventoryScale.applies(self);
    }
}
