package com.peachsoju.mixin;

import com.peachsoju.PeachSoju;
import com.peachsoju.eventbus.events.PacketEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinPlayerPosition {

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onPlayerPosition(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        PacketEvent.Receive event = new PacketEvent.Receive(packet);
        PeachSoju.INSTANCE.getEventBus().post(event);
    }
}