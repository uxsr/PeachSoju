package com.peachsoju.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DataComponentHolder.class)
public interface DataComponentHolderMixin {

    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private <T> T peachsoju$customComponents(T original, DataComponentType<? extends T> dataComponentType) {
        if (((Object) this) instanceof ItemStack stack) {

            if (dataComponentType == DataComponents.ITEM_MODEL) {
                String customItem = ItemCustomizeManager.INSTANCE.getCustomItemString(stack);
                if (customItem != null && !customItem.isEmpty()) {
                    try {
                        ResourceLocation modelId = ResourceLocation.parse(customItem);
                        return (T) modelId;
                    } catch (Exception ignored) {}
                }
            }

            // Custom Head Textures (only for player heads)
            else if (dataComponentType == DataComponents.PROFILE && stack.is(Items.PLAYER_HEAD)) {
                // Check animated head first
                String animatedId = ItemCustomizeManager.INSTANCE.getAnimatedHelmetId(stack);
                if (animatedId != null && !animatedId.isEmpty()) {
                    ResolvableProfile frame = ItemCustomizeManager.INSTANCE.animateHeadTexture(animatedId);
                    if (frame != null) {
                        return (T) frame;
                    }
                }

                // Fall back to static texture
                String staticTexture = ItemCustomizeManager.INSTANCE.getCustomHelmetTexture(stack);
                if (staticTexture != null && !staticTexture.isEmpty()) {
                    ResolvableProfile profile = ItemCustomizeManager.INSTANCE.createHeadProfile(staticTexture);
                    if (profile != null) {
                        return (T) profile;
                    }
                }
            }
        }
        return original;
    }
}