package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    
    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
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
            method = "renderItem(Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private ItemStack peachsoju$modifyRenderItemSimple(ItemStack stack) {
        return modifyStack(stack);
    }

    
    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private ItemStack peachsoju$modifyRenderItemWithSeed(ItemStack stack) {
        return modifyStack(stack);
    }

    
    @ModifyVariable(
            method = "renderFakeItem(Lnet/minecraft/world/item/ItemStack;II)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private ItemStack peachsoju$modifyRenderFakeItem(ItemStack stack) {
        return modifyStack(stack);
    }

    
    @ModifyVariable(
            method = "renderFakeItem(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private ItemStack peachsoju$modifyRenderFakeItemWithSeed(ItemStack stack) {
        return modifyStack(stack);
    }

    
    @ModifyVariable(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            require = 0
    )
    private ItemStack peachsoju$modifyRenderItemWithEntity(ItemStack stack) {
        return modifyStack(stack);
    }

    private static ItemStack modifyStack(ItemStack stack) {
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