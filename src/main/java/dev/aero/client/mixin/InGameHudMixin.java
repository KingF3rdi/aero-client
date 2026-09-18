package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameHud.class, priority = 2000)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void aero$overlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        dev.aero.client.hud.OverlayHud.render(context, tickCounter);
    }
    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void aero$hideScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.hideScoreboard) {
            ci.cancel();
        }
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.guiTweaks && !AeroClient.CONFIG.guiScoreboard) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderCrosshair(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void aero$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (dev.aero.client.hud.OverlayHud.hideVanillaCrosshair()) {
            ci.cancel();
        }
    }

    private static boolean aero$boostOverlays() {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.uiBoost && AeroClient.CONFIG.uiBoostOverlays;
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$noVignette(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && (AeroClient.CONFIG.noVignette || aero$boostOverlays())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$lowFire(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (AeroClient.CONFIG == null) {
            return;
        }
        String path = texture.getPath();
        if (AeroClient.CONFIG.lowFire && path.contains("fire")) {
            ci.cancel();
        }
        if (AeroClient.CONFIG.cleanView && AeroClient.CONFIG.hideFireOverlay && path.contains("fire")) {
            ci.cancel();
        }
        if (AeroClient.CONFIG.cleanWater && (path.contains("underwater") || path.contains("water"))) {
            ci.cancel();
        }
        if ((AeroClient.CONFIG.guiTweaks || aero$boostOverlays()) && (path.contains("pumpkin") || path.contains("spyglass")
                || path.contains("powder_snow") || path.contains("portal"))) {
            ci.cancel();
        }
        if (AeroClient.CONFIG.shieldTweaks && path.contains("shield")) {
            ci.cancel();
        }
        if (AeroClient.CONFIG.totemTweaks && path.contains("totem")) {
            ci.cancel();
        }
        if (AeroClient.CONFIG.cleanView && AeroClient.CONFIG.hideHands && path.contains("spyglass")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$guiPortal(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && (AeroClient.CONFIG.guiTweaks || aero$boostOverlays())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$guiNausea(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && (AeroClient.CONFIG.guiTweaks || aero$boostOverlays())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$guiSpy(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && (AeroClient.CONFIG.guiTweaks || aero$boostOverlays())) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderStatusEffectOverlay", "renderStatusEffects"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$potionVanilla(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.potionHud && AeroClient.CONFIG.potionHideVanilla) {
            ci.cancel();
        }
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.guiTweaks && !AeroClient.CONFIG.guiStatus) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderHeldItemTooltip", "renderHeldItemName"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$itemName(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.guiTweaks && AeroClient.CONFIG.guiNoItemName) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderOverlayMessage", "renderActionBar"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$action(CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.guiTweaks && AeroClient.CONFIG.guiHideActionBar) {
            ci.cancel();
        }
    }

    // 1.21.11 moved the XP level number out into a static helper on the new Bar interface (the bar
    // graphic itself moved to ExperienceBarMixin) - the old renderExperienceBar/renderExperienceLevel
    // method names this used to target don't exist anymore, so this toggle silently did nothing
    // (every injector here defaults to require = 0, which hides a dead target instead of erroring).
    @Redirect(method = "renderMainHud", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"),
            require = 0)
    private void aero$xpLevel(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int level) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.guiTweaks && !AeroClient.CONFIG.guiXp) {
            return;
        }
        net.minecraft.client.gui.hud.bar.Bar.drawExperienceLevel(context, textRenderer, level);
    }
}
