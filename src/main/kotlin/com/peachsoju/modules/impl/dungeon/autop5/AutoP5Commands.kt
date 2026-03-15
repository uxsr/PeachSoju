package com.peachsoju.modules.impl.dungeon.autop5

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.network.chat.Component
import com.peachsoju.config

object AutoP5Commands {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("p5")
                    .executes { ctx ->
                        // Show status
                        val enabled = config.autoP5Enabled()
                        val inP5 = AutoP5Manager.isInP5
                        val assigned = AutoP5State.assignedDragon
                        val pathfinding = AutoP5State.isPathfinding
                        val atPosition = AutoP5State.isAtDebuffPosition

                        ctx.source.sendFeedback(Component.literal("§d§lAutoP5 Status"))
                        ctx.source.sendFeedback(Component.literal("§7Enabled: ${if (enabled) "§aYES" else "§cNO"}"))
                        ctx.source.sendFeedback(Component.literal("§7In P5: ${if (inP5) "§aYES" else "§cNO"}"))
                        ctx.source.sendFeedback(Component.literal("§7Assigned: ${assigned?.let { "§${it.colorCode}${it.name}" } ?: "§7None"}"))
                        ctx.source.sendFeedback(Component.literal("§7Pathfinding: ${if (pathfinding) "§aYES" else "§cNO"}"))
                        ctx.source.sendFeedback(Component.literal("§7At Position: ${if (atPosition) "§aYES" else "§cNO"}"))
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        ClientCommandManager.literal("toggle").executes { ctx ->
                            val on = config.toggleAutoP5Enabled()
                            ctx.source.sendFeedback(Component.literal("§d[AutoP5] ${if (on) "§aEnabled" else "§cDisabled"}"))
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("go")
                            .then(
                                ClientCommandManager.argument("dragon", StringArgumentType.word())
                                    .executes { ctx ->
                                        val dragonName = StringArgumentType.getString(ctx, "dragon")
                                        AutoP5Manager.goToDragon(dragonName)
                                        Command.SINGLE_SUCCESS
                                    }
                            )
                    )
                    .then(
                        ClientCommandManager.literal("middle").executes { ctx ->
                            AutoP5Manager.goToMiddle()
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("stop").executes { ctx ->
                            AutoP5Manager.stopAll()
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("reset").executes { ctx ->
                            AutoP5Manager.reset()
                            ctx.source.sendFeedback(Component.literal("§e[AutoP5] Reset complete"))
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("delays").executes { ctx ->
                            ctx.source.sendFeedback(Component.literal("§d§lLast Breath Delays (ms)"))
                            ctx.source.sendFeedback(Component.literal("§cRed: §f${config.autoP5RedDelay()}"))
                            ctx.source.sendFeedback(Component.literal("§6Orange: §f${config.autoP5OrangeDelay()}"))
                            ctx.source.sendFeedback(Component.literal("§aGreen: §f${config.autoP5GreenDelay()}"))
                            ctx.source.sendFeedback(Component.literal("§bBlue: §f${config.autoP5BlueDelay()}"))
                            ctx.source.sendFeedback(Component.literal("§5Purple: §f${config.autoP5PurpleDelay()}"))
                            ctx.source.sendFeedback(Component.literal(""))
                            ctx.source.sendFeedback(Component.literal("§7Use /p5 delay <dragon> <ms> to change"))
                            Command.SINGLE_SUCCESS
                        }
                    )
                    .then(
                        ClientCommandManager.literal("delay")
                            .then(
                                ClientCommandManager.argument("dragon", StringArgumentType.word())
                                    .then(
                                        ClientCommandManager.argument("ms", StringArgumentType.word())
                                            .executes { ctx ->
                                                val dragonName = StringArgumentType.getString(ctx, "dragon").lowercase()
                                                val msStr = StringArgumentType.getString(ctx, "ms")
                                                val ms = msStr.toDoubleOrNull()

                                                if (ms == null || ms < 0 || ms > 2000) {
                                                    ctx.source.sendFeedback(Component.literal("§cInvalid delay. Use 0-2000ms"))
                                                    return@executes Command.SINGLE_SUCCESS
                                                }

                                                when (dragonName) {
                                                    "red" -> config.setAutoP5RedDelay(ms)
                                                    "orange" -> config.setAutoP5OrangeDelay(ms)
                                                    "green" -> config.setAutoP5GreenDelay(ms)
                                                    "blue" -> config.setAutoP5BlueDelay(ms)
                                                    "purple" -> config.setAutoP5PurpleDelay(ms)
                                                    else -> {
                                                        ctx.source.sendFeedback(Component.literal("§cUnknown dragon: $dragonName"))
                                                        return@executes Command.SINGLE_SUCCESS
                                                    }
                                                }

                                                ctx.source.sendFeedback(Component.literal("§a[AutoP5] Set $dragonName delay to ${ms.toInt()}ms"))
                                                Command.SINGLE_SUCCESS
                                            }
                                    )
                            )
                    )
                    .then(
                        ClientCommandManager.literal("help").executes { ctx ->
                            ctx.source.sendFeedback(Component.literal("§d§lAutoP5 Commands"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 §f- Show status"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 toggle §f- Enable/disable"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 go <dragon> §f- Pathfind to dragon"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 middle §f- Go to middle"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 stop §f- Stop all automation"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 reset §f- Full reset"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 delays §f- Show Last Breath delays"))
                            ctx.source.sendFeedback(Component.literal("§7/p5 delay <dragon> <ms> §f- Set delay"))
                            Command.SINGLE_SUCCESS
                        }
                    )
            )
        }
    }
}