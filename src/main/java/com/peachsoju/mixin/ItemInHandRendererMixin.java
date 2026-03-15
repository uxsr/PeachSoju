package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @ModifyVariable(
            method = "renderItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack peachsoju$modifyRenderItem(ItemStack stack) {
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
            method = "renderArmWithItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack peachsoju$modifyRenderArmWithItem(ItemStack stack) {
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