package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import net.minecraft.client.gui.screens.Screen

class AutoWeirdosScreen(parent: Screen?) : FeatureScreen(parent, "Auto Weirdos") {

    override fun buildElements(): List<GuiElement> = listOf(
        GuiElement.Toggle(
            name = "Enabled",
            getter = { config.autoWeirdos() },
            toggler = { config.toggleAutoWeirdos() },
            description = "Auto-click NPCs and correct chest"
        ),
        GuiElement.Spacer,
        GuiElement.Label("§7Requires Odin's Weirdos Solver ")
    )
}