package com.peachsoju.mixin;

import com.peachsoju.config;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
    private void preventReequipAnimation(ItemStack oldStack, ItemStack newStack, CallbackInfoReturnable<Boolean> cir) {
        if (!config.INSTANCE.stormLegacyAnimation()) return;

        String oldName = oldStack.isEmpty() ? "" : oldStack.getHoverName().getString().toLowerCase();
        String newName = newStack.isEmpty() ? "" : newStack.getHoverName().getString().toLowerCase();

        if (oldName.contains("terminator") || newName.contains("terminator")) {
            cir.setReturnValue(true);
        }
    }
}