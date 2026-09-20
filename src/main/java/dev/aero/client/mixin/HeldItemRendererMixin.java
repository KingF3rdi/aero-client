package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeldItemRenderer.class, priority = 2000)
public class HeldItemRendererMixin {
    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.entity.player.PlayerEntity aero$prevShieldHolder;
    @org.spongepowered.asm.mixin.Unique
    private boolean aero$pushedShieldHolder;

    /** Shield Tweaks "Fix blocking animation": skips the equip-in/out tween while actively
     * blocking, so the shield model doesn't visibly dip/glitch mid-block. */
    @ModifyVariable(
            method = "applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private float aero$fixShieldEquip(float equipProgress) {
        var c = AeroClient.CONFIG;
        if (c != null && c.handTweaks && c.handRestart) {
            return 0.0F;
        }
        boolean fixAnim = c != null && c.shieldTweaks && (c.shieldFixAnim || dev.aero.client.OptimizerMods.shieldFixes());
        boolean optimizer = c != null && (c.shieldOptimizer && c.shieldOptimizerInstant || dev.aero.client.Optimizer.shield());
        if (c == null || !(fixAnim || optimizer)) {
            return equipProgress;
        }
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null || !player.isUsingItem()) {
            return equipProgress;
        }
        if (dev.aero.client.Visuals.isShield(player.getMainHandStack())
                || dev.aero.client.Visuals.isShield(player.getOffHandStack())) {
            return 0.0F;
        }
        return equipProgress;
    }

    @Inject(
            method = "applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            at = @At("TAIL"),
            require = 0
    )
    private void aero$shield(MatrixStack matrices, net.minecraft.util.Arm arm, float equipProgress, CallbackInfo ci) {
        if (AeroClient.CONFIG == null) {
            return;
        }
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = arm == net.minecraft.util.Arm.RIGHT ? player.getMainHandStack() : player.getOffHandStack();
        try {
            if (player.getMainArm() == net.minecraft.util.Arm.LEFT) {
                stack = arm == net.minecraft.util.Arm.LEFT ? player.getMainHandStack() : player.getOffHandStack();
            }
        } catch (Throwable ignored) {
        }
        String name = stack.getItem().toString().toLowerCase();
        if (AeroClient.CONFIG.crossbowTweaks && (name.contains("crossbow") || name.contains("bow"))
                && player.isUsingItem()) {
            matrices.translate(0.0F, -0.04F, -0.08F);
            matrices.scale(0.92F, 0.92F, 0.92F);
        }
        if (AeroClient.CONFIG.handTweaks) {
            var c = AeroClient.CONFIG;
            if ("Mini".equalsIgnoreCase(c.handStyle)) {
                matrices.scale(0.55F, 0.55F, 0.55F);
            } else {
                matrices.translate(c.customX - 0.56F, c.customY + 0.32F, c.customZ + 0.72F);
                rotate(matrices, c.customPitch, c.customYaw, c.customRoll);
                boolean main = arm == player.getMainArm();
                if (main) {
                    matrices.translate(c.mainX, c.mainY, c.mainZ);
                    rotate(matrices, c.mainPitch, c.mainYaw, c.mainRoll);
                    matrices.scale(c.mainScale, c.mainScale, c.mainScale);
                } else {
                    matrices.translate(c.offX, c.offY, c.offZ);
                    rotate(matrices, c.offPitch, c.offYaw, c.offRoll);
                    matrices.scale(c.offScale, c.offScale, c.offScale);
                }
            }
        }
    }

    @Inject(method = {"applySwingOffset", "swingArm"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noSwing(CallbackInfo ci) {
        var c = AeroClient.CONFIG;
        if (c == null) {
            return;
        }
        if (c.cleanView && c.noSwing) {
            ci.cancel();
            return;
        }
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        String held = player.getMainHandStack().getItem().toString().toLowerCase();
        boolean anchors = (c.anchorOptimizer && c.anchorOptimizerSwing) || dev.aero.client.Optimizer.cutebowAnchor();
        boolean pearls = (c.pearlOptimizer && c.pearlOptimizerSwing) || dev.aero.client.Optimizer.pearl();
        if (anchors && held.contains("respawn_anchor")) {
            ci.cancel();
        } else if (pearls && held.contains("ender_pearl")) {
            ci.cancel();
        } else if ((c.crossbowOptimizer && c.crossbowOptimizerSwing || dev.aero.client.Optimizer.crossbow()) && held.contains("crossbow")) {
            ci.cancel();
        } else if (dev.aero.client.Optimizer.mace() && held.contains("mace")) {
            ci.cancel();
        }
    }

    private static void rotate(MatrixStack matrices, float pitch, float yaw, float roll) {
        if (pitch == 0 && yaw == 0 && roll == 0) {
            return;
        }
        try {
            var x = net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitch);
            var y = net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yaw);
            var z = net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(roll);
            matrices.multiply(x);
            matrices.multiply(y);
            matrices.multiply(z);
        } catch (Throwable ignored) {
        }
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("HEAD"),
            cancellable = true, require = 0
    )
    private void aero$shieldHolderBegin(net.minecraft.entity.LivingEntity entity, ItemStack stack,
                                        net.minecraft.item.ItemDisplayContext context,
                                        MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        if (entity == net.minecraft.client.MinecraftClient.getInstance().player && dev.aero.client.Visuals.isShield(stack)
                && "spiked".equals(dev.aero.client.cosmetic.Cosmetics.equipped(dev.aero.client.cosmetic.Cosmetics.Kind.SHIELD))) {
            matrices.push();
            matrices.translate(0.0, 0.3, -0.6);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-125f));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-15f));
            matrices.scale(0.8f, 0.8f, 0.8f);
            dev.aero.client.cosmetic.SpikedShield.draw(matrices, queue,
                    net.minecraft.client.render.RenderLayers.entityCutoutNoCull(dev.aero.client.cosmetic.CubeDraw.WHITE), light);
            matrices.pop();
            ci.cancel();
            return;
        }
        if (entity instanceof net.minecraft.entity.player.PlayerEntity player
                && dev.aero.client.Visuals.isShield(stack)) {
            aero$prevShieldHolder = dev.aero.client.Visuals.currentShieldHolder();
            aero$pushedShieldHolder = true;
            dev.aero.client.Visuals.pushShieldHolder(player);
        }
    }

    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("RETURN"),
            require = 0
    )
    private void aero$shieldHolderEnd(net.minecraft.entity.LivingEntity entity, ItemStack stack,
                                      net.minecraft.item.ItemDisplayContext context,
                                      MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        if (aero$pushedShieldHolder) {
            aero$pushedShieldHolder = false;
            dev.aero.client.Visuals.popShieldHolder(aero$prevShieldHolder);
            aero$prevShieldHolder = null;
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideHands(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.cleanView && AeroClient.CONFIG.hideHands) {
            ci.cancel();
        }
    }

    // NOTE: "Renders" hand shader tint is not implemented - Minecraft 1.21.11 removed
    // RenderSystem.setShaderColor entirely as part of its new render-pipeline rewrite, and there is
    // no verified drop-in replacement yet. The module toggle exists and persists but has no visual
    // effect until a real implementation is found for the new pipeline.
}
