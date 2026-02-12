package com.blowup.gui.commands

import com.blowup.gui.AutoRoutesHelpScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

object AutoRoutesHelpOpener {

    private var pendingOpen = false
    private var tickHookRegistered = false

    fun open() {
        if (!tickHookRegistered) {
            tickHookRegistered = true
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                if (!pendingOpen) return@register
                pendingOpen = false
                val parent = Minecraft.getInstance().screen
                client.setScreen(AutoRoutesHelpScreen(parent))
            }
        }
        pendingOpen = true
    }
}