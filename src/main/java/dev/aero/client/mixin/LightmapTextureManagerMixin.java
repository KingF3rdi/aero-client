package dev.aero.client.mixin;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LightmapTextureManager.class, priority = 2000)
public class LightmapTextureManagerMixin {
}
