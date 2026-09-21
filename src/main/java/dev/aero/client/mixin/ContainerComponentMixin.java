package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** Shulker Tooltips: drops vanilla's "Item x64 ... and 5 more" text lines, the grid replaces them. */
@Mixin(value = ContainerComponent.class, priority = 2000)
public class ContainerComponentMixin {
    @Inject(method = "appendTooltip(Lnet/minecraft/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/item/tooltip/TooltipType;Lnet/minecraft/component/ComponentsAccess;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noList(Item.TooltipContext context, Consumer<Text> textConsumer, TooltipType type,
                             net.minecraft.component.ComponentsAccess components, CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c != null && c.shulkerTooltips && c.shulkerTooltipsHideList) {
            ci.cancel();
        }
    }
}
