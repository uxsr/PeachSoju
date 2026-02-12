package com.peachsoju.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinCancelBlockInteract {

    private static final String[] CANCEL_INTERACT_ITEMS = {
            "Ender Pearl"
    };

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null) return;

        String itemName = player.getItemInHand(hand).getHoverName().getString();

        for (String cancelItem : CANCEL_INTERACT_ITEMS) {
            if (itemName.contains(cancelItem)) {
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }
        }
    }
}