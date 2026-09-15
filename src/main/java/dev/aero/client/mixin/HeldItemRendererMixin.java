package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    /** Shield Tweaks "Fix blocking animation": skips the equip-in/out tween while actively
     * blocking, so the shield model doesn't visibly dip/glitch mid-block. */
    @ModifyVariable(method = "applyEquipOffset", at = @At("HEAD"), argsOnly = true, require = 0)
    private float aero$fixShieldEquip(float equipProgress, MatrixStack matrices, Hand hand) {
        var c = AeroClient.CONFIG;
        boolean fixAnim = c != null && c.shieldTweaks && c.shieldFixAnim;
        boolean optimizer = c != null && c.shieldOptimizer && c.shieldOptimizerInstant;
        if (c == null || !(fixAnim || optimizer)) {
            return equipProgress;
        }
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null || !player.isUsingItem() || !player.isBlocking()) {
            return equipProgress;
        }
        ItemStack stack = hand == Hand.MAIN_HAND ? player.getMainHandStack() : player.getOffHandStack();
        if (stack.getItem().toString().toLowerCase().contains("shield")) {
            return 1.0F;
        }
        return equipProgress;
    }

    @Inject(method = "applyEquipOffset", at = @At("TAIL"), require = 0)
    private void aero$shield(MatrixStack matrices, Hand hand, float equipProgress, CallbackInfo ci) {
        if (AeroClient.CONFIG == null) {
            return;
        }
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = hand == Hand.MAIN_HAND ? player.getMainHandStack() : player.getOffHandStack();
        String name = stack.getItem().toString().toLowerCase();
        if (AeroClient.CONFIG.shieldTweaks && name.contains("shield") && player.isUsingItem()) {
            matrices.translate(0.0F, -0.18F, -0.22F);
            matrices.scale(0.72F, 0.72F, 0.72F);
        }
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
                if (hand == Hand.MAIN_HAND) {
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
        if (c.anchorOptimizer && c.anchorOptimizerSwing && held.contains("respawn_anchor")) {
            ci.cancel();
        } else if (c.pearlOptimizer && c.pearlOptimizerSwing && held.contains("ender_pearl")) {
            ci.cancel();
        } else if (c.crossbowOptimizer && c.crossbowOptimizerSwing && held.contains("crossbow")) {
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
