package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The XP bar graphic itself (as opposed to the level-number text, see InGameHudMixin.aero$xpLevel)
 * lives on this dedicated Bar implementation as of 1.21.11.
 */
@Mixin(ExperienceBar.class)
public class ExperienceBarMixin {
    @Inject(method = "renderBar", at = @At("HEAD"), cancellable = true, require = 0)
    private void aero$hideXpBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (AeroClient.CONFIG != null && AeroClient.CONFIG.guiTweaks && !AeroClient.CONFIG.guiXp) {
            ci.cancel();
        }
    }
}
