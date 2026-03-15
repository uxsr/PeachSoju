package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin to apply custom armor trims when the game checks for trim data.
 * This intercepts the get() call on DataComponents.TRIM to return custom trims.
 */
@Mixin(DataComponentHolder.class)
public interface ArmorTrimMixin {

    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private <T> T peachsoju$customComponents(T original, DataComponentType<? extends T> dataComponentType) {
        if (((Object) this) instanceof ItemStack stack) {
            if (dataComponentType == DataComponents.TRIM) {
                try {
                    if (ItemCustomizeManager.INSTANCE.hasCustomArmorTrim(stack)) {
                        String materialStr = ItemCustomizeManager.INSTANCE.getCustomTrimMaterial(stack);
                        String patternStr = ItemCustomizeManager.INSTANCE.getCustomTrimPattern(stack);

                        if (materialStr != null && patternStr != null) {
                            ResourceLocation materialId = ResourceLocation.parse(materialStr);
                            ResourceLocation patternId = ResourceLocation.parse(patternStr);

                            ArmorTrim customTrim = ItemCustomizeManager.INSTANCE.getArmorTrim(materialId, patternId);
                            if (customTrim != null) {
                                return (T) customTrim;
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Silently fail to avoid crashes
                }
            }
        }
        return original;
    }
}