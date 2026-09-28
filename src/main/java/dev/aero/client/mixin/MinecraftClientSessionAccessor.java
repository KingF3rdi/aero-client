package dev.aero.client.mixin;

import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

/** Account switcher: swaps the live session (and its cached profile) in place. */
@Mixin(MinecraftClient.class)
public interface MinecraftClientSessionAccessor {
    @Mutable
    @Accessor("session")
    void aero$setSession(Session session);

    @Mutable
    @Accessor("gameProfileFuture")
    void aero$setGameProfileFuture(CompletableFuture<ProfileResult> future);
}
