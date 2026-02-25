package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import net.minecraft.client.gui.screens.Screen

class MiscScreen(parent: Screen?) : FeatureScreen(parent, "Misc") {
    override fun buildElements() = listOf(
        GuiElement.Toggle(
            "Hide Server ID",
            { config.hideServerID() },
            { config.toggleHideServerID() },
            "Hides server ID in scoreboard"
        ),
        GuiElement.Toggle(
            "Chat Bypass",
            { config.chatBypass() },
            { config.toggleChatBypass() },
            "Bypasses chat filters"
        )
    )
}