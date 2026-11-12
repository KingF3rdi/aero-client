package dev.aero.client.mixin;

import dev.aero.client.AeroClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkingBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ping Boost: once the connection to a server is open, mark its socket "low delay" (IP TOS) so routers and Windows
 * QoS that honour it can send game traffic first, and raise the network thread's priority so incoming packets are
 * read straight away even when high FPS keeps every CPU core busy. Vanilla already sets TCP_NODELAY.
 * Only the transport is touched: packets and their timing are exactly what the game sends.
 */
@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    private static final int IPTOS_LOWDELAY = 0x10;

    @Inject(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/network/NetworkingBackend;Lnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;",
            at = @At("RETURN"), require = 0)
    private static void aero$pingBoost(InetSocketAddress address, NetworkingBackend backend, ClientConnection connection,
                                       CallbackInfoReturnable<ChannelFuture> cir) {
        if (AeroClient.CONFIG == null || !AeroClient.CONFIG.pingBoost) {
            return;
        }
        cir.getReturnValue().addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                return;
            }
            try {
                future.channel().config().setOption(ChannelOption.IP_TOS, IPTOS_LOWDELAY);
            } catch (Throwable ignored) {
                // some systems refuse TOS from user programs; nothing else depends on it
            }
            future.channel().eventLoop().execute(() -> Thread.currentThread().setPriority(Thread.MAX_PRIORITY));
        });
    }
}
