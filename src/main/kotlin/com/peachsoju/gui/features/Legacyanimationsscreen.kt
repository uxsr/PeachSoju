package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import net.minecraft.client.gui.screens.Screen

class LegacyAnimationsScreen(parent: Screen?) : FeatureScreen(parent, "Legacy Animations") {

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle("Terminator", { config.stormLegacyAnimation() }, { config.toggleStormLegacyAnimation() }, "Disable Terminator pullback and re-equip animations"))

        add(GuiElement.Spacer)

    }
}