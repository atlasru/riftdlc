package dev.riftdlc.mixin;

import dev.riftdlc.RiftDLC;
import dev.riftdlc.core.PacketEvents;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void riftdlc$incoming(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        RiftDLC app = RiftDLC.instance();
        if (app != null && !app.packetEvents().dispatch(PacketEvents.Direction.INCOMING, packet)) ci.cancel();
    }
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void riftdlc$outgoing(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        RiftDLC app = RiftDLC.instance();
        if (app != null && !app.packetEvents().dispatch(PacketEvents.Direction.OUTGOING, packet)) ci.cancel();
    }
}
