package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import net.minecraft.client.gui.screens.Screen

class StormBowTimerScreen(parent: Screen?) : FeatureScreen(parent, "Storm Bow Timer") {

    override fun initTextFields() {
        textFieldValues["stormReleaseTime"] = String.format("%.2f", config.stormReleaseTime())
    }

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle("Enabled", { config.stormBowTimer() }, { config.toggleStormBowTimer() }, "Storm bow release timer"))

        add(GuiElement.Spacer)

        add(GuiElement.Toggle("Auto Release", { config.stormAutoRelease() }, { config.toggleStormAutoRelease() }, "Automatically release bow"))

        add(GuiElement.Spacer)

        add(GuiElement.TextField(
            id = "stormReleaseTime",
            name = "Release Time",
            getter = { config.stormReleaseTime() },
            setter = { config.setStormReleaseTime(it) },
            min = 20.0,
            max = 45.0,
            suffix = "s",
            description = "Time to release bow (20-45s)"
        ))

        add(GuiElement.Spacer)

    }
}