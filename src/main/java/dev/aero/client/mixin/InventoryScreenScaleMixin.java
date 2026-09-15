package dev.aero.client.mixin;

import dev.aero.client.ui.InventoryScale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Survival Inventory half of the Inventory Scale GUI Tweak (see InventoryScale). Wraps the render
 * pass in a matrix scale and shrinks the mouseX/mouseY it hands to the vanilla slot-highlight
 * logic so hover/highlight lines up with what's actually drawn; mouseClicked/mouseDragged are
 * inherited from RecipeBookScreen (see RecipeBookScreenScaleMixin) but mouseReleased is overridden
 * here directly, so it needs its own remap.
 */
@Mixin(value = InventoryScreen.class, priority = 2000)
public class InventoryScreenScaleMixin {
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

    @ModifyVariable(method = "mouseReleased", at = @At("HEAD"), argsOnly = true, require = 0)
    private Click aero$shrinkReleased(Click click) {
        if (!applies()) {
            return click;
        }
        float f = InventoryScale.factor();
        return new Click(click.x() / f, click.y() / f, click.buttonInfo());
    }

    private boolean applies() {
        return InventoryScale.applies((InventoryScreen) (Object) this);
    }
}
