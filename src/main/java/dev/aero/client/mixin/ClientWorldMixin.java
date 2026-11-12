package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientWorld.class, priority = 2000)
public class ClientWorldMixin {
    // 1.21.11: ClientWorld.getSkyColor is gone; the sky renderer and fog read the
    // visual/sky_color environment attribute. Add a last modifier so ours wins over
    // biome, time-of-day, weather and lightning, like the old HEAD override did.
    @Inject(method = "addClientSideAttributes", at = @At("RETURN"), require = 0)
    private void aero$sky(WorldEnvironmentAttributeAccess.Builder builder,
                          CallbackInfoReturnable<WorldEnvironmentAttributeAccess.Builder> cir) {
        ClientWorld world = (ClientWorld) (Object) this;
        builder.timeBased(EnvironmentAttributes.SKY_COLOR_VISUAL, (color, time) -> aero$skyColor(world, color));
    }

    private static int aero$skyColor(ClientWorld world, int color) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.ambience) {
            return color;
        }
        String dim = "";
        try {
            dim = world.getRegistryKey().getValue().toString().toLowerCase();
        } catch (Throwable ignored) {
        }
        if (AeroClient.CONFIG.netherSky && dim.contains("nether")) {
            return rgb(AeroClient.CONFIG.netherColor, 0x6B2E29);
        }
        if (AeroClient.CONFIG.endSky && dim.contains("end")) {
            return rgb(AeroClient.CONFIG.endColor, 0x1F0A2E);
        }
        if ((AeroClient.CONFIG.overworldSky || AeroClient.CONFIG.skyGradient)
                && !dim.contains("nether") && !dim.contains("end")) {
            return rgb(AeroClient.CONFIG.skyGradient ? AeroClient.CONFIG.gradientColor : AeroClient.CONFIG.skyColor, 0x8575C7);
        }
        return color;
    }

    /** Opaque RGB from the configured color; black (unset) falls back to the default tint. */
    private static int rgb(int argb, int fallback) {
        int rgb = argb & 0xFFFFFF;
        return 0xFF000000 | (rgb == 0 ? fallback : rgb);
    }
}
