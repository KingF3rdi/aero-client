package dev.aero.client.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Entity flag setter (flag 7 = gliding) for the elytra optimizer. */
@Mixin(Entity.class)
public interface EntityFlagInvoker {
    @Invoker("setFlag")
    void aero$setFlag(int index, boolean value);
}
