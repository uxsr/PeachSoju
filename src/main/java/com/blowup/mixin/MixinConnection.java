package com.blowup.mixin;

import com.blowup.PeachSoju;
import com.blowup.eventbus.EventBus;
import com.blowup.eventbus.events.PacketEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class MixinConnection {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        try {
            EventBus bus = PeachSoju.INSTANCE.getEventBus();
            if (bus.post(new PacketEvent.Receive(packet))) ci.cancel();
        } catch (Exception ignored) {}
    }

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void sendImmediately(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean flush, CallbackInfo ci) {
        try {
            EventBus bus = PeachSoju.INSTANCE.getEventBus();
            if (bus.post(new PacketEvent.Send(packet))) ci.cancel();
        } catch (Exception ignored) {}
    }
}