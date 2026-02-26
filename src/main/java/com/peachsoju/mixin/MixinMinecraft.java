package com.peachsoju.mixin;

import com.peachsoju.PeachSoju;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "<init>", at = @At("TAIL"))
    public void onFinishInit(CallbackInfo ci) {
        PeachSoju.INSTANCE.init();
    }
}
