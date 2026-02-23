package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import net.minecraft.client.gui.screens.Screen

class AutoSSScreen(parent: Screen?) : FeatureScreen(parent, "AutoSS") {

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle("Enabled", { config.autoSS() }, { config.toggleAutoSS() }, "Auto Simon Says solver"))

        add(GuiElement.Spacer)

        add(GuiElement.Slider("Click Delay", { config.autoSSDelay() }, { config.setAutoSSDelay(it) }, 50.0, 200.0, "ms"))
        add(GuiElement.Slider("Start Delay", { config.autoSSAutoStartDelay() }, { config.setAutoSSAutoStartDelay(it) }, 50.0, 200.0, "ms"))

        add(GuiElement.Spacer)

        add(GuiElement.Toggle("Force Device", { config.autoSSForceDevice() }, { config.toggleAutoSSForceDevice() }, "Bypass device detection"))
    }
}