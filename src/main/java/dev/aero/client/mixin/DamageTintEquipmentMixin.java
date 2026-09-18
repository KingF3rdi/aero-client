package dev.aero.client.mixin;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Armor is drawn with a plain overlay in 1.21.11; while a hurt player's armor pass is active, give it the red hurt overlay. */
@Mixin(value = EquipmentRenderer.class, priority = 2000)
public class DamageTintEquipmentMixin {
    @ModifyArg(method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"),
            index = 5, require = 0)
    private int aero$armorOverlay(int overlay) {
        dev.aero.client.DamageTintState.equipHits++;
        if (dev.aero.client.DamageTintState.ARMOR_HURT.get()) {
            dev.aero.client.DamageTintState.equipHurtHits++;
            return OverlayTexture.packUv(OverlayTexture.getU(0f), OverlayTexture.getV(true));
        }
        return overlay;
    }
}
