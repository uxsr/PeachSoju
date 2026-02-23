package com.peachsoju.modules.impl.fmblocks

import com.peachsoju.config
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks

object FMBlocksCommands {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("fm")
                    .executes { ctx ->
                        val enabled = FMBlocksManager.enabled
                        val editMode = FMBlocksEditMode.enabled
                        val simulating = FMBlocksManager.simulating
                        val (rooms, blocks) = FMBlocksManager.getStats()
                        ctx.source.sendFeedback(
                            Component.literal(
                                "§e§lFMBlocks: ${if (enabled) "§aON" else "§cOFF"} §8| " +
                                        "§7Edit: ${if (editMode) "§aON" else "§cOFF"} §8| " +
                                        "§7$blocks blocks in $rooms rooms" +
                                        if (simulating != null) " §8| §dSim: $simulating" else ""
                            )
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        ClientCommandManager.literal("toggle").executes { ctx ->
                            val on = config.toggleFmBlocksEnabled()
                            ctx.source.sendFeedback(Component.literal("§eFMBlocks: ${if (on) "§aON" else "§cOFF"}"))
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("edit").executes { ctx ->
                            val on = config.toggleFmBlocksEditMode()
                            ctx.source.sendFeedback(Component.literal("§eFMBlocks Edit Mode: ${if (on) "§aON" else "§cOFF"}"))
                            if (on) {
                                ctx.source.sendFeedback(
                                    Component.literal(
                                        "§7Controls:\n" +
                                                "  §fLeft Click§7: Ghost block / Remove custom\n" +
                                                "  §fMiddle Click§7: Pick block\n" +
                                                "  §fSneak + Right Click§7: Place custom block"
                                    )
                                )
                            }
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("clear").executes { ctx ->
                            val roomKey = FMBlocksManager.getCurrentRoomKey()
                            if (roomKey == null) {
                                ctx.source.sendFeedback(Component.literal("§c[FMBlocks] Not in a valid room"))
                                return@executes Command.SINGLE_SUCCESS
                            }
                            FMBlocksManager.clearCurrentRoom()
                            ctx.source.sendFeedback(Component.literal("§e[FMBlocks] Cleared all blocks in $roomKey"))
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("reload").executes { ctx ->
                            FMBlocksManager.reloadFromDisk()
                            val (rooms, blocks) = FMBlocksManager.getStats()
                            ctx.source.sendFeedback(Component.literal("§a[FMBlocks] Reloaded $blocks blocks from $rooms rooms"))
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("info").executes { ctx ->
                            val enabled = FMBlocksManager.enabled
                            val editMode = FMBlocksEditMode.enabled
                            val simulating = FMBlocksManager.simulating
                            val roomKey = FMBlocksManager.getCurrentRoomKey()
                            val currentBlocks = roomKey?.let {
                                FMBlocksManager.getBlocksForRoom(it)?.blocks?.values?.sumOf { v -> v.size } ?: 0
                            } ?: 0
                            val (totalRooms, totalBlocks) = FMBlocksManager.getStats()
                            val selectedBlock = FMBlocksEditMode.currentBlockState.block.descriptionId

                            ctx.source.sendFeedback(Component.literal("§e§lFMBlocks Info"))
                            ctx.source.sendFeedback(Component.literal("§7Enabled: ${if (enabled) "§aYES" else "§cNO"}"))
                            ctx.source.sendFeedback(Component.literal("§7Edit Mode: ${if (editMode) "§aON" else "§cOFF"}"))
                            ctx.source.sendFeedback(Component.literal("§7Simulating: ${if (simulating != null) "§d$simulating" else "§cNO"}"))
                            ctx.source.sendFeedback(Component.literal("§7Room: §a${roomKey ?: "None"}"))
                            ctx.source.sendFeedback(Component.literal("§7Blocks in room: §a$currentBlocks"))
                            ctx.source.sendFeedback(Component.literal("§7Total rooms: §a$totalRooms"))
                            ctx.source.sendFeedback(Component.literal("§7Total blocks: §a$totalBlocks"))
                            ctx.source.sendFeedback(Component.literal("§7Selected block: §a$selectedBlock"))
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("set")
                            .then(
                                ClientCommandManager.argument("block", StringArgumentType.greedyString())
                                    .executes { ctx ->
                                        val blockStr = StringArgumentType.getString(ctx, "block")
                                        val blockId = if (blockStr.contains(":")) {
                                            ResourceLocation.tryParse(blockStr)
                                        } else {
                                            ResourceLocation.tryParse("minecraft:$blockStr")
                                        }

                                        if (blockId == null) {
                                            ctx.source.sendFeedback(Component.literal("§c[FMBlocks] Invalid block ID: $blockStr"))
                                            return@executes Command.SINGLE_SUCCESS
                                        }

                                        val block = BuiltInRegistries.BLOCK.getValue(blockId)
                                        if (block == Blocks.AIR && blockStr != "air" && blockStr != "minecraft:air") {
                                            ctx.source.sendFeedback(Component.literal("§c[FMBlocks] Block not found: $blockStr"))
                                            return@executes Command.SINGLE_SUCCESS
                                        }

                                        FMBlocksEditMode.setCurrentBlock(block.defaultBlockState())
                                        ctx.source.sendFeedback(Component.literal("§a[FMBlocks] Selected: ${block.descriptionId}"))
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
                    .then(
                        ClientCommandManager.literal("sim")
                            .executes { ctx ->
                                val current = FMBlocksManager.simulating
                                if (current != null) {
                                    FMBlocksManager.setSimulating(null)
                                    ctx.source.sendFeedback(Component.literal("§e[FMBlocks] Simulation disabled"))
                                } else {
                                    ctx.source.sendFeedback(Component.literal("§e[FMBlocks] Not simulating. Use §f/fm sim <room>§7 to start"))
                                    ctx.source.sendFeedback(Component.literal("§7Examples: §f/fm sim boss_7§7, §f/fm sim Three Weirdos"))
                                }
                                Command.SINGLE_SUCCESS
                            }
                            .then(
                                ClientCommandManager.literal("off").executes { ctx ->
                                    FMBlocksManager.setSimulating(null)
                                    ctx.source.sendFeedback(Component.literal("§e[FMBlocks] Simulation disabled"))
                                    Command.SINGLE_SUCCESS
                                }
                            )
                            .then(
                                ClientCommandManager.literal("list").executes { ctx ->
                                    val rooms = FMBlocksManager.getRoomList()
                                    if (rooms.isEmpty()) {
                                        ctx.source.sendFeedback(Component.literal("§c[FMBlocks] No rooms with blocks saved"))
                                    } else {
                                        ctx.source.sendFeedback(Component.literal("§e[FMBlocks] Saved rooms (${rooms.size}):"))
                                        rooms.forEach { room ->
                                            val blockCount = FMBlocksManager.getBlocksForRoom(room)?.blocks?.values?.sumOf { it.size } ?: 0
                                            ctx.source.sendFeedback(Component.literal("  §7- §f$room §8($blockCount blocks)"))
                                        }
                                    }
                                    Command.SINGLE_SUCCESS
                                }
                            )
                            .then(
                                ClientCommandManager.argument("room", StringArgumentType.greedyString())
                                    .executes { ctx ->
                                        val roomName = StringArgumentType.getString(ctx, "room")
                                        FMBlocksManager.setSimulating(roomName)
                                        val blockCount = FMBlocksManager.getBlocksForRoom(roomName)?.blocks?.values?.sumOf { it.size } ?: 0
                                        ctx.source.sendFeedback(
                                            Component.literal("§a[FMBlocks] Simulating: §d$roomName §7($blockCount blocks)")
                                        )
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
            )
        }
    }
}