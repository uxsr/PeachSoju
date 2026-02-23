package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import net.minecraft.client.gui.screens.Screen

class AutoAlignScreen(parent: Screen?) : FeatureScreen(parent, "Auto Align") {

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle("Enabled", { config.autoAlign() }, { config.toggleAutoAlign() }, "Auto arrow align solver"))

        add(GuiElement.Spacer)

    }
}