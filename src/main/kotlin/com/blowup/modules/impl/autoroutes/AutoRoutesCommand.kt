package com.blowup.modules.impl.autoroutes

import com.blowup.config
import com.blowup.gui.commands.AutoRoutesHelpOpener
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component

object AutoRoutesCommand {

    private var clearConfirmUntilMs = 0L
    private var clearConfirmRoomKey: String? = null
    private const val CLEAR_CONFIRM_WINDOW_MS = 6000L

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("ar")
                    .executes { ctx ->
                        val enabled = config.autoroutes()
                        val rendering = NodeManager.enabled
                        val editing = NodeManager.editing
                        ctx.source.sendFeedback(
                            Component.literal(
                                "AutoRoutes: ${if (enabled) "§aENABLED" else "§cDISABLED"} §8| §7Render: ${if (rendering) "§aON" else "§cOFF"} §8| §7Edit: ${if (editing) "§aON" else "§cOFF"}"
                            )
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .then(ClientCommandManager.literal("help").executes { _ ->
                        AutoRoutesHelpOpener.open()
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("toggle").executes { ctx ->
                        val ar = config.toggleAutoroutes()
                        ctx.source.sendFeedback(Component.literal("AutoRoutes: ${if (ar) "§aON" else "§cOFF"}"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("render").executes { ctx ->
                        val on = NodeManager.toggle()
                        ctx.source.sendFeedback(Component.literal("Waypoint rendering: ${if (on) "§aON" else "§cOFF"}"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("lines").executes { ctx ->
                        val on = config.toggleShowLines()
                        ctx.source.sendFeedback(Component.literal("Line rendering: ${if (on) "§aON" else "§cOFF"}"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("startsonly").executes { ctx ->
                        val on = config.toggleRenderOnlyStartNodes()
                        ctx.source.sendFeedback(Component.literal("Render only start nodes: ${if (on) "§aON" else "§cOFF"}"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("clear").executes { ctx ->
                        val now = System.currentTimeMillis()
                        val roomKey = RouteState.currentRoomKey ?: "unknown"
                        val confirmed =
                            now <= clearConfirmUntilMs &&
                                    clearConfirmRoomKey == roomKey
                        if (!confirmed) {
                            clearConfirmRoomKey = roomKey
                            clearConfirmUntilMs = now + CLEAR_CONFIRM_WINDOW_MS
                            ctx.source.sendFeedback(
                                Component.literal("§cThis will remove ALL nodes in this room. Run §e/ar clear§c again to confirm.")
                            )
                            return@executes Command.SINGLE_SUCCESS
                        }
                        clearConfirmUntilMs = 0L
                        clearConfirmRoomKey = null
                        NodeManager.clearCurrentRoom()
                        ctx.source.sendFeedback(Component.literal("§eCleared all waypoints in current room"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("reload").executes { ctx ->
                        NodeManager.reloadFromDisk()
                        ctx.source.sendFeedback(Component.literal("§aReloaded waypoints from disk"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("burst").executes { ctx ->
                        val on = BurstMode.toggle()
                        ctx.source.sendFeedback(Component.literal("Burst mode: ${if (on) "§aON" else "§cOFF"}"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("config").executes { ctx ->
                        val on = config.toggleConfigMode()
                        ctx.source.sendFeedback(Component.literal("Config mode: ${if (on) "§aON" else "§cOFF"}"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("testether").executes { ctx ->
                        ctx.source.sendFeedback(Component.literal(BurstMode.testPredictionFromPlayer()))
                        Command.SINGLE_SUCCESS
                    })
                    .then(
                        ClientCommandManager.literal("testchain")
                            .then(
                                ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .executes { ctx ->
                                        val index = IntegerArgumentType.getInteger(ctx, "index")
                                        BurstMode.testChainFromNode(index).split("\n").forEach { ctx.source.sendFeedback(Component.literal(it)) }
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
                    .then(ClientCommandManager.literal("info").executes { ctx ->
                        val enabled = config.autoroutes()
                        val rendering = NodeManager.enabled
                        val editing = NodeManager.editing
                        val burst = BurstMode.enabled
                        val roomKey = NodeManager.getCurrentRoomKey()
                        val nodeCount = NodeManager.getCurrentRoomWaypoints()?.size ?: 0

                        ctx.source.sendFeedback(Component.literal("§e§lAutoroutes Info"))
                        ctx.source.sendFeedback(Component.literal("§7Enabled: ${if (enabled) "§aYES" else "§cNO"}"))
                        ctx.source.sendFeedback(Component.literal("§7Rendering: ${if (rendering) "§aON" else "§cOFF"}"))
                        ctx.source.sendFeedback(Component.literal("§7Editing: ${if (editing) "§aON" else "§cOFF"}"))
                        ctx.source.sendFeedback(Component.literal("§7Burst Mode: ${if (burst) "§aON" else "§cOFF"}"))
                        ctx.source.sendFeedback(Component.literal("§7Config Mode: ${if (config.configMode()) "§aON" else "§cOFF"}"))
                        ctx.source.sendFeedback(Component.literal("§7Route Active: ${if (RouteState.routeActive) "§aYES" else "§cNO"}"))
                        ctx.source.sendFeedback(Component.literal("§7Room: §a${roomKey ?: "None"}"))
                        ctx.source.sendFeedback(Component.literal("§7Nodes: §a$nodeCount"))
                        ctx.source.sendFeedback(Component.literal("§7Total Rooms: §a${NodeManager.getRoomCount()}"))
                        ctx.source.sendFeedback(Component.literal("§7Total Nodes: §a${NodeManager.getNodeCount()}"))
                        Command.SINGLE_SUCCESS
                    })
                    .then(
                        ClientCommandManager.literal("add")
                            .then(
                                ClientCommandManager.argument("args", StringArgumentType.greedyString())
                                    .executes { ctx ->
                                        val result = NodeManager.addFromCommand(StringArgumentType.getString(ctx, "args"))
                                        ctx.source.sendFeedback(Component.literal(result))
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
//                    .then(ClientCommandManager.literal("dev").executes { ctx ->
//                        val dev = config.toggleextraDebug()
//                        ctx.source.sendFeedback(Component.literal("dev mode: ${if (dev) "§aON" else "§cOFF"}"))
//                        Command.SINGLE_SUCCESS
//                    })
                    .then(
                        ClientCommandManager.literal("remove")
                            .executes { ctx ->
                                ctx.source.sendFeedback(Component.literal(NodeManager.removeClosest()))
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .executes { ctx ->
                                        val index = IntegerArgumentType.getInteger(ctx, "index")
                                        ctx.source.sendFeedback(Component.literal(NodeManager.removeByIndex(index)))
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
                    .then(ClientCommandManager.literal("undo").executes { ctx ->
                        ctx.source.sendFeedback(Component.literal(NodeManager.undoRemove()))
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommandManager.literal("list").executes { ctx ->
                        ctx.source.sendFeedback(Component.literal(NodeManager.listNodes()))
                        Command.SINGLE_SUCCESS
                    })
                    .then(
                        ClientCommandManager.literal("set")
                            .then(
                                ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .then(
                                        ClientCommandManager.argument("modifier", StringArgumentType.word())
                                            .then(
                                                ClientCommandManager.argument("value", StringArgumentType.word())
                                                    .executes { ctx ->
                                                        val index = IntegerArgumentType.getInteger(ctx, "index")
                                                        val modifier = StringArgumentType.getString(ctx, "modifier")
                                                        val value = StringArgumentType.getString(ctx, "value")
                                                        ctx.source.sendFeedback(Component.literal(NodeManager.setNodeModifier(index, modifier, value)))
                                                        Command.SINGLE_SUCCESS
                                                    }
                                            )
                                            .executes { ctx ->
                                                val index = IntegerArgumentType.getInteger(ctx, "index")
                                                val modifier = StringArgumentType.getString(ctx, "modifier")
                                                ctx.source.sendFeedback(Component.literal(NodeManager.setNodeModifier(index, modifier, null)))
                                                Command.SINGLE_SUCCESS
                                            }
                                    )
                            )
                    )
                    .then(
                        ClientCommandManager.literal("move")
                            .then(
                                ClientCommandManager.argument("from", IntegerArgumentType.integer(0))
                                    .then(
                                        ClientCommandManager.argument("to", IntegerArgumentType.integer(0))
                                            .executes { ctx ->
                                                val from = IntegerArgumentType.getInteger(ctx, "from")
                                                val to = IntegerArgumentType.getInteger(ctx, "to")
                                                ctx.source.sendFeedback(Component.literal(NodeManager.moveNode(from, to)))
                                                Command.SINGLE_SUCCESS
                                            }
                                    )
                            )
                    )
                    .then(
                        ClientCommandManager.literal("insert")
                            .then(
                                ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .then(
                                        ClientCommandManager.argument("args", StringArgumentType.greedyString())
                                            .executes { ctx ->
                                                val index = IntegerArgumentType.getInteger(ctx, "index")
                                                val args = StringArgumentType.getString(ctx, "args")
                                                ctx.source.sendFeedback(Component.literal(NodeManager.insertAt(index, args)))
                                                Command.SINGLE_SUCCESS
                                            }
                                    )
                            )
                    )
                    .then(
                        ClientCommandManager.literal("updatepos")
                            .then(
                                ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .executes { ctx ->
                                        val index = IntegerArgumentType.getInteger(ctx, "index")
                                        ctx.source.sendFeedback(Component.literal(NodeManager.updatePosition(index)))
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
                    .then(
                        ClientCommandManager.literal("updaterot")
                            .then(
                                ClientCommandManager.argument("index", IntegerArgumentType.integer(0))
                                    .executes { ctx ->
                                        val index = IntegerArgumentType.getInteger(ctx, "index")
                                        ctx.source.sendFeedback(Component.literal(NodeManager.updateRotation(index)))
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
            )
        }
    }
}
