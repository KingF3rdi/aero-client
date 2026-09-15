package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = {"tiltViewWhenHurt", "bobViewWhenHurt", "tiltView"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noHurtcam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (AeroClient.CONFIG == null) {
            return;
        }
        boolean nostalgiaOld = AeroClient.CONFIG.nostalgia && AeroClient.CONFIG.nostalgiaOldHurtCamera;
        if (AeroClient.CONFIG.noHurtcam || nostalgiaOld) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$cleanCharge(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (AeroClient.CONFIG == null) {
            return;
        }
        if (AeroClient.CONFIG.noBobbing) {
            ci.cancel();
            return;
        }
        if (!AeroClient.CONFIG.crossbowTweaks) {
            return;
        }
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player != null && mc.player.isUsingItem()) {
            String item = mc.player.getActiveItem().getItem().toString().toLowerCase();
            if (item.contains("crossbow") || item.contains("bow")) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void aero$zoom(CallbackInfoReturnable<Double> cir) {
        if (dev.aero.client.Visuals.zooming() && AeroClient.CONFIG != null) {
            Object v = cir.getReturnValue();
            if (v instanceof Number n) {
                cir.setReturnValue(n.doubleValue() / Math.max(1.0, AeroClient.CONFIG.zoomInitial));
            }
        }
    }

    @Inject(method = {"showFloatingItem", "renderFloatingItem"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$totem(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && ((AeroClient.CONFIG.totemTweaks && AeroClient.CONFIG.totemNoEquip)
                || dev.aero.client.Optimizer.totem())) {
            ci.cancel();
        }
    }
}

