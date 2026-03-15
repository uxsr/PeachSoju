package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackCustomizeMixin {

    @Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void peachsoju$getCustomName(CallbackInfoReturnable<Component> cir) {
        ItemStack self = (ItemStack) (Object) this;
        try {
            String customName = ItemCustomizeManager.INSTANCE.getDisplayName(self);
            if (customName != null) {
                cir.setReturnValue(Component.literal(customName));
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void peachsoju$hasCustomFoil(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        try {
            Boolean customGlint = ItemCustomizeManager.INSTANCE.shouldShowGlint(self);
            if (customGlint != null) {
                cir.setReturnValue(customGlint);
            }
        } catch (Exception ignored) {
        }
    }
}