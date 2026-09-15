package dev.aero.client.mixin;

import dev.aero.client.ui.InventoryScale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Creative Inventory half of the Inventory Scale GUI Tweak (see InventoryScale).
 * CreativeInventoryScreen overrides render/resize/mouseClicked/mouseDragged/mouseReleased/
 * mouseScrolled itself instead of inheriting HandledScreen's/RecipeBookScreen's, so it needs its
 * own copy of every remap rather than sharing the survival-Inventory mixins.
 */
@Mixin(value = CreativeInventoryScreen.class, priority = 2000)
public class CreativeInventoryScaleMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), require = 0)
    private void aero$pushScale(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (applies()) {
            float f = InventoryScale.factor();
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(f, f);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("RETURN"), require = 0)
    private void aero$popScale(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (applies()) {
            context.getMatrices().popMatrix();
        }
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private int aero$shrinkMouseX(int mouseX) {
        return applies() ? InventoryScale.shrink(mouseX) : mouseX;
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private int aero$shrinkMouseY(int mouseY) {
        return applies() ? InventoryScale.shrink(mouseY) : mouseY;
    }

    @ModifyVariable(method = "resize(II)V", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private int aero$shrinkResizeWidth(int width) {
        return applies() ? InventoryScale.shrink(width) : width;
    }

    @ModifyVariable(method = "resize(II)V", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private int aero$shrinkResizeHeight(int height) {
        return applies() ? InventoryScale.shrink(height) : height;
    }

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

    @ModifyVariable(method = "mouseReleased", at = @At("HEAD"), argsOnly = true, require = 0)
    private Click aero$shrinkReleased(Click click) {
        return remap(click);
    }

    @ModifyVariable(method = "mouseScrolled", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private double aero$shrinkScrollX(double mouseX) {
        return applies() ? mouseX / InventoryScale.factor() : mouseX;
    }

    @ModifyVariable(method = "mouseScrolled", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private double aero$shrinkScrollY(double mouseY) {
        return applies() ? mouseY / InventoryScale.factor() : mouseY;
    }

    private Click remap(Click click) {
        if (!applies()) {
            return click;
        }
        float f = InventoryScale.factor();
        return new Click(click.x() / f, click.y() / f, click.buttonInfo());
    }

    private boolean applies() {
        return InventoryScale.applies((CreativeInventoryScreen) (Object) this);
    }
}
