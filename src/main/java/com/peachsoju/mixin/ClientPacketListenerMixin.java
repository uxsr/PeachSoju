package com.peachsoju.mixin;

import com.peachsoju.PeachSoju;
import com.peachsoju.eventbus.EventDispatcher;
import com.peachsoju.eventbus.events.ItemPickupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    private static final double PICKUP_RANGE = 6.0;

    @Inject(method = "handleBlockUpdate", at = @At("RETURN"))
    private void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        EventDispatcher.INSTANCE.onBlockUpdate(packet.getPos(), packet.getBlockState());
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
    private void onMultiBlockUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        packet.runUpdates((pos, state) -> {
            EventDispatcher.INSTANCE.onBlockUpdate(pos, state);
        });
    }

    @Inject(method = "handleTakeItemEntity", at = @At("HEAD"))
    private void onHandleTakeItemEntity(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (packet.getPlayerId() != mc.player.getId()) return;

        Entity entity = mc.level.getEntity(packet.getItemId());
        if (!(entity instanceof ItemEntity itemEntity)) return;

        if (mc.player.distanceTo(itemEntity) > PICKUP_RANGE) return;

        PeachSoju.INSTANCE.getEventBus().post(new ItemPickupEvent(itemEntity, itemEntity.getItem().copy()));
    }
}