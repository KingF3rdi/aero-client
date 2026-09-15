package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = {"hasGlint", "hasEnchantmentGlint", "hasGlintOverride"}, at = @At("HEAD"),
            cancellable = true, require = 0)
    private void aero$noGlint(CallbackInfoReturnable<Boolean> cir) {
        if (AeroClient.CONFIG == null) {
            return;
        }
        boolean nostalgiaOld = AeroClient.CONFIG.nostalgia && AeroClient.CONFIG.nostalgiaOldGlint;
        if (AeroClient.CONFIG.noGlint || nostalgiaOld) {
            cir.setReturnValue(false);
        }
    }
}
