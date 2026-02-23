package com.peachsoju.mixin;

import com.peachsoju.PeachSoju;
import com.peachsoju.eventbus.events.ItemPickupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class IMixinClientLevel {

    private static final double PICKUP_RANGE = 6.0;

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(int entityId, Entity.RemovalReason reason, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) return;

        if (!(entity instanceof ItemEntity itemEntity)) return;

        double distance = mc.player.distanceTo(itemEntity);
        if (distance > PICKUP_RANGE) return;

        PeachSoju.INSTANCE.getEventBus().post(new ItemPickupEvent(itemEntity, itemEntity.getItem().copy()));
    }
}