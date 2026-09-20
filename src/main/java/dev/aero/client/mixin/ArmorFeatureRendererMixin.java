package dev.aero.client.mixin;

import dev.aero.client.DamageTintState;
import dev.aero.client.Visuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide Armor (whole set or a single slot) for the entities the Visuals filter picks. */
@Mixin(value = ArmorFeatureRenderer.class, priority = 2000)
public class ArmorFeatureRendererMixin {
    private static Entity aero$entity(BipedEntityRenderState state) {
        var mc = MinecraftClient.getInstance();
        if (mc.world == null || !(state instanceof net.minecraft.client.render.entity.state.PlayerEntityRenderState p)) {
            return null;
        }
        return mc.world.getEntityById(p.id);
    }

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideEntity(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                 BipedEntityRenderState state, float yaw, float pitch, CallbackInfo ci) {
        Entity e = aero$entity(state);
        if (e != null && Visuals.hideArmor(e)) {
            DamageTintState.ARMOR_HURT.set(false);
            ci.cancel();
        }
    }

    @Inject(method = "renderArmor(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideSlot(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack,
                               EquipmentSlot slot, int light, BipedEntityRenderState state, CallbackInfo ci) {
        Entity e = aero$entity(state);
        if (e != null && Visuals.hideArmorSlot(e, slot)) {
            ci.cancel();
        }
    }
}
