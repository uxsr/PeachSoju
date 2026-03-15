package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.nickhider.NickHider;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public abstract class NickHiderStringMixin {

    @ModifyVariable(
            method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static String peachsoju$modifyIterateFormatted(String text) {
        try {
            String result = NickHider.INSTANCE.replaceName(text);
            return result != null ? result : text;
        } catch (Exception e) {
            return text; // Return original on any error
        }
    }

    @ModifyVariable(
            method = "iterate(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static String peachsoju$modifyIterate(String text) {
        try {
            String result = NickHider.INSTANCE.replaceName(text);
            return result != null ? result : text;
        } catch (Exception e) {
            return text;
        }
    }
}