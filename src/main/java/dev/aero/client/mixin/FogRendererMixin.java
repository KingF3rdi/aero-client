package dev.aero.client.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** Kept for older trees. Not registered on 1.21.11 — BackgroundRenderer is gone. */
@Mixin(targets = "net.minecraft.client.render.fog.FogRenderer")
public class FogRendererMixin {
}
