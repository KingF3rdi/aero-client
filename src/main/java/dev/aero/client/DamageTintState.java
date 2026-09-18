package dev.aero.client;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

/** Shared state for the Damage Tint mixins: the flash moves from the skin to the armor. */
public final class DamageTintState {
    public static final ThreadLocal<Boolean> ARMOR_HURT = ThreadLocal.withInitial(() -> false);

    public static int armorBegin;
    public static int equipHits;
    public static int equipHurtHits;

    private DamageTintState() {}

    public static boolean on(LivingEntityRenderState state) {
        return AeroClient.CONFIG != null && AeroClient.CONFIG.damageTint && state instanceof PlayerEntityRenderState;
    }
}
