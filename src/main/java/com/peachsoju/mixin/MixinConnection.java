package com.peachsoju.mixin;

import com.peachsoju.PeachSoju;
import com.peachsoju.eventbus.EventBus;
import com.peachsoju.eventbus.events.PacketEvent;
//import com.peachsoju.modules.impl.dungeon.icefill.IceFillSolver;
import com.peachsoju.modules.impl.misc.stormbow.StormBowTimer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
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

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"
            )
    )
    private void onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundPingPacket pingPacket) {
            if (pingPacket.getId() == 0) return;
            StormBowTimer.INSTANCE.onServerTick();
//            IceFillSolver.INSTANCE.onServerTick();
        }
    }
}