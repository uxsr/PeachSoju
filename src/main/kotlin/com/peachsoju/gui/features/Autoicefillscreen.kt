package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import net.minecraft.client.gui.screens.Screen

class AutoIceFillScreen(parent: Screen?) : FeatureScreen(parent, "Auto Ice Fill") {

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle("Enabled", { config.autoIceFill() }, { config.toggleAutoIceFill() }, "Auto Ice Fill solver"))

        add(GuiElement.Spacer)

    }
}