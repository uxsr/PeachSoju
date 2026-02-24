package com.peachsoju.mixin;

import com.peachsoju.config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(PlayerTeam.class)
public class ServerIDSpooferMixin {
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{2}");

    @Inject(method = "getFormattedName", at = @At("RETURN"), cancellable = true)
    private void onGetFormattedName(Component name, CallbackInfoReturnable<MutableComponent> cir) {
        if (config.INSTANCE.hideServerID()) {
            String content = cir.getReturnValue().getString();
            Matcher matcher = DATE_PATTERN.matcher(content);
            if (matcher.find()) {
                String datePart = matcher.group();
                cir.setReturnValue(Component.literal("§7" + datePart + " §8§kaaaaa"));
            }
        }
    }
}