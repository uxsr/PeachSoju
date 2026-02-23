package com.peachsoju.gui.commands

import com.peachsoju.gui.AnimatedPeachSojuScreen
import com.mojang.brigadier.Command
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

object PeachSojuCommand {

    private val mc get() = Minecraft.getInstance()
    private var pendingOpen = false
    private var tickHookRegistered = false

    fun register() {
        if (!tickHookRegistered) {
            tickHookRegistered = true
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                if (!pendingOpen) return@register
                pendingOpen = false
                client.setScreen(AnimatedPeachSojuScreen())
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            fun registerOne(name: String) {
                dispatcher.register(
                    ClientCommandManager.literal(name).executes {
                        pendingOpen = true
                        Command.SINGLE_SUCCESS
                    }
                )
            }
            registerOne("peachsoju")
            registerOne("peach")
            registerOne("ps")
        }
    }
}