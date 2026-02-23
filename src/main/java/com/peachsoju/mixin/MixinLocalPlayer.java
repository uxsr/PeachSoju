package com.peachsoju.mixin;

import com.peachsoju.config;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void preventTerminatorPullback(CallbackInfoReturnable<Boolean> cir) {
        if (!com.peachsoju.config.INSTANCE.stormLegacyAnimation()) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemStack mainHand = player.getMainHandItem();

        if (!mainHand.isEmpty() && mainHand.getHoverName().getString().toLowerCase().contains("terminator")) {
            cir.setReturnValue(false);
        }
    }
}