package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @ModifyVariable(
            method = "renderArmorPiece",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private ItemStack peachsoju$modifyArmorStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;

        try {
            if (ItemCustomizeManager.INSTANCE.hasCustomItem(stack)) {
                return ItemCustomizeManager.INSTANCE.getCustomizedStackForRendering(stack);
            }

            if (ItemCustomizeManager.INSTANCE.hasCustomArmorModel(stack)) {
                ItemStack customStack = ItemCustomizeManager.INSTANCE.getCustomArmorStack(stack);
                if (customStack != null && !customStack.isEmpty()) {
                    ItemStack dyed = ItemCustomizeManager.INSTANCE.getCustomizedStackForRendering(customStack);
                    return dyed;
                }
            }
        } catch (Exception ignored) {
        }

        return stack;
    }
}