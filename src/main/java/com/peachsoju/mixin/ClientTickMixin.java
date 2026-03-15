package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to increment the tick counter for animated head textures.
 */
@Mixin(Minecraft.class)
public abstract class ClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void peachsoju$onTick(CallbackInfo ci) {
        try {
            ItemCustomizeManager.INSTANCE.incrementTick();
        } catch (Exception ignored) {
            // Silently fail
        }
    }
}