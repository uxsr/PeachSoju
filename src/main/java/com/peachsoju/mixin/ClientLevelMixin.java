package com.peachsoju.mixin;

import com.peachsoju.eventbus.EventDispatcher;
import com.peachsoju.utils.RouteUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "setBlock", at = @At("RETURN"))
    private void onBlockUpdate(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            EventDispatcher.INSTANCE.onBlockUpdate(pos, state);
        }
    }

//    @Inject(method = "removeEntity", at = @At("HEAD"))
//    private void onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
//        ClientLevel level = (ClientLevel)(Object)this;
//        Entity entity = level.getEntity(entityId);
//        RouteUtils.debug("§7[Mixin] removeEntity called, entityId=" + entityId + ", entity=" + (entity != null ? entity.getClass().getSimpleName() : "null"));
//        if (entity != null) {
//            EventDispatcher.INSTANCE.onEntityRemoved(entity);
//        }
//    }
}