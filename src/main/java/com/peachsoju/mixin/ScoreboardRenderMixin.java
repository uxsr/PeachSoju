package com.peachsoju.mixin;

import com.peachsoju.modules.impl.misc.nickhider.NickHider;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class ScoreboardRenderMixin {

    /**
     * Set flag before scoreboard rendering starts
     */
    @Inject(
            method = "displayScoreboardSidebar",
            at = @At("HEAD")
    )
    private void peachsoju$beforeScoreboardRender(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        NickHider.INSTANCE.setRenderingScoreboard(true);
    }

    /**
     * Clear flag after scoreboard rendering ends
     */
    @Inject(
            method = "displayScoreboardSidebar",
            at = @At("RETURN")
    )
    private void peachsoju$afterScoreboardRender(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        NickHider.INSTANCE.setRenderingScoreboard(false);
    }
}