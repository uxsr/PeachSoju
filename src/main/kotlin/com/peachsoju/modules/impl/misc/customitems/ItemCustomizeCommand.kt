package com.peachsoju.modules.impl.misc.customitems

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.peachsoju.PeachSoju.mc
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

object ItemCustomizeCommand {

    private var pendingGuiOpen: Pair<ItemStack, String>? = null
    private var tickHookRegistered = false

    fun register() {
        if (!tickHookRegistered) {
            tickHookRegistered = true
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                val pending = pendingGuiOpen ?: return@register
                pendingGuiOpen = null
                client.setScreen(GuiItemCustomize(pending.first, pending.second))
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("pscustomize")
                    .executes { ctx ->
                        openCustomizeGui(ctx.source::sendFeedback)
                        Command.SINGLE_SUCCESS
                    }
            )
        }
    }

    private fun openCustomizeGui(feedback: (Component) -> Unit) {
        val player = mc.player ?: run { feedback(Component.literal("§cPlayer not found")); return }
        val heldItem = player.mainHandItem
        if (heldItem.isEmpty) {
            feedback(Component.literal("§cYou must be holding an item to customize it!"))
            return
        }
        val uuid = ItemCustomizeManager.getUUIDForItem(heldItem)
        if (uuid == null) {
            feedback(Component.literal("§cThis item doesn't have a UUID and cannot be customized."))
            feedback(Component.literal("§7(Only Skyblock items with UUIDs can be customized)"))
            return
        }
        pendingGuiOpen = Pair(heldItem.copy(), uuid)
    }

}