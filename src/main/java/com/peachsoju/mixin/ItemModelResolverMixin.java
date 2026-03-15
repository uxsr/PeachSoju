package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {

    @ModifyVariable(
            method = "updateForTopItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack peachsoju$modifyItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;

        try {
            if (ItemCustomizeManager.INSTANCE.hasCustomItem(stack)) {
                return ItemCustomizeManager.INSTANCE.getCustomizedStackForRendering(stack);
            }
        } catch (Exception ignored) {
        }

        return stack;
    }

    @ModifyVariable(
            method = "updateForNonTopItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )

    private ItemStack peachsoju$modifyItemStackNonTop(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;

        try {
            if (ItemCustomizeManager.INSTANCE.hasCustomItem(stack)) {
                return ItemCustomizeManager.INSTANCE.getCustomizedStackForRendering(stack);
            }
        } catch (Exception ignored) {
        }

        return stack;
    }
}