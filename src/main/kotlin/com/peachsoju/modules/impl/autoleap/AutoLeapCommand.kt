//package com.peachsoju.modules.impl.autoleap
//
//import com.mojang.brigadier.CommandDispatcher
//import com.mojang.brigadier.arguments.IntegerArgumentType
//import com.mojang.brigadier.arguments.StringArgumentType
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
//import net.minecraft.commands.CommandBuildContext
//import net.minecraft.network.chat.Component
//import com.peachsoju.PeachSoju.mc
//import com.peachsoju.config
//import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
//import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
//
//object AutoLeapCommand {
//
//    private val classes = listOf("Archer", "Tank", "Healer", "Mage", "Bers")
//
//    fun register() {
//        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
//            registerCommands(dispatcher)
//        }
//    }
//
//    private fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
//        dispatcher.register(
//            literal("autoleap")
//                .executes { ctx ->
//                    showHelp(ctx.source)
//                    1
//                }
//                .then(literal("toggle")
//                    .executes { ctx ->
//                        val newState = config.toggleAutoLeapEnabled()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fModule ${if (newState) "§aEnabled" else "§cDisabled"}"))
//                        1
//                    }
//                )
//                .then(literal("status")
//                    .executes { ctx ->
//                        showStatus(ctx.source)
//                        1
//                    }
//                )
//                .then(literal("teammates")
//                    .executes { ctx ->
//                        showTeammates(ctx.source)
//                        1
//                    }
//                )
//                .then(literal("phase")
//                    .executes { ctx ->
//                        showPhase(ctx.source)
//                        1
//                    }
//                )
//                .then(literal("simulate")
//                    .then(literal("gate")
//                        .executes { ctx ->
//                            AutoLeap.simulateGateBlown()
//                            ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §aSimulated gate blown"))
//                            1
//                        }
//                    )
//                    .then(literal("terminals")
//                        .executes { ctx ->
//                            AutoLeap.simulateTerminalsDone()
//                            ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §aSimulated terminals done"))
//                            1
//                        }
//                    )
//                    .then(literal("both")
//                        .executes { ctx ->
//                            AutoLeap.simulateGateBlown()
//                            AutoLeap.simulateTerminalsDone()
//                            ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §aSimulated gate + terminals"))
//                            1
//                        }
//                    )
//                )
//                .then(literal("leapto")
//                    .then(argument("name", StringArgumentType.word())
//                        .executes { ctx ->
//                            val name = StringArgumentType.getString(ctx, "name")
//                            AutoLeap.forceLeapTo(name)
//                            ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §aForcing leap to §c$name"))
//                            1
//                        }
//                    )
//                )
//                .then(literal("setphase")
//                    .then(argument("phase", IntegerArgumentType.integer(0, 4))
//                        .executes { ctx ->
//                            val phase = IntegerArgumentType.getInteger(ctx, "phase")
//                            AutoLeap.setCurrentPhase(phase)
//                            ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §aSet current phase to §e$phase"))
//                            1
//                        }
//                    )
//                )
//                .then(literal("reset")
//                    .executes { ctx ->
//                        AutoLeap.reset()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §aReset all state"))
//                        1
//                    }
//                )
//                .then(literal("config")
//                    .executes { ctx ->
//                        showConfig(ctx.source)
//                        1
//                    }
//                    .then(literal("ee2")
//                        .then(literal("toggle")
//                            .executes { ctx ->
//                                val newState = config.toggleAutoLeapEE2()
//                                ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fEE2 ${if (newState) "§aEnabled" else "§cDisabled"}"))
//                                1
//                            }
//                        )
//                        .then(literal("name")
//                            .then(argument("name", StringArgumentType.greedyString())
//                                .executes { ctx ->
//                                    val name = StringArgumentType.getString(ctx, "name")
//                                    config.setAutoLeapEE2Name(name)
//                                    ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fEE2 name set to §a$name"))
//                                    1
//                                }
//                            )
//                        )
//                        .then(literal("class")
//                            .then(argument("class", IntegerArgumentType.integer(0, 4))
//                                .executes { ctx ->
//                                    val classIdx = IntegerArgumentType.getInteger(ctx, "class")
//                                    config.setAutoLeapEE2Class(classIdx)
//                                    ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fEE2 class set to §a${classes[classIdx]}"))
//                                    1
//                                }
//                            )
//                        )
//                    )
//                    .then(literal("ee3")
//                        .then(literal("toggle")
//                            .executes { ctx ->
//                                val newState = config.toggleAutoLeapEE3()
//                                ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fEE3 ${if (newState) "§aEnabled" else "§cDisabled"}"))
//                                1
//                            }
//                        )
//                        .then(literal("name")
//                            .then(argument("name", StringArgumentType.greedyString())
//                                .executes { ctx ->
//                                    val name = StringArgumentType.getString(ctx, "name")
//                                    config.setAutoLeapEE3Name(name)
//                                    ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fEE3 name set to §a$name"))
//                                    1
//                                }
//                            )
//                        )
//                        .then(literal("class")
//                            .then(argument("class", IntegerArgumentType.integer(0, 4))
//                                .executes { ctx ->
//                                    val classIdx = IntegerArgumentType.getInteger(ctx, "class")
//                                    config.setAutoLeapEE3Class(classIdx)
//                                    ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fEE3 class set to §a${classes[classIdx]}"))
//                                    1
//                                }
//                            )
//                        )
//                    )
//                    .then(literal("core")
//                        .then(literal("toggle")
//                            .executes { ctx ->
//                                val newState = config.toggleAutoLeapCore()
//                                ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fCore ${if (newState) "§aEnabled" else "§cDisabled"}"))
//                                1
//                            }
//                        )
//                        .then(literal("name")
//                            .then(argument("name", StringArgumentType.greedyString())
//                                .executes { ctx ->
//                                    val name = StringArgumentType.getString(ctx, "name")
//                                    config.setAutoLeapCoreName(name)
//                                    ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fCore name set to §a$name"))
//                                    1
//                                }
//                            )
//                        )
//                        .then(literal("class")
//                            .then(argument("class", IntegerArgumentType.integer(0, 4))
//                                .executes { ctx ->
//                                    val classIdx = IntegerArgumentType.getInteger(ctx, "class")
//                                    config.setAutoLeapCoreClass(classIdx)
//                                    ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fCore class set to §a${classes[classIdx]}"))
//                                    1
//                                }
//                            )
//                        )
//                    )
//                )
//                .then(literal("force")
//                    .executes { ctx ->
//                        val newState = config.toggleAutoLeapForce()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoLeap] §fForce mode ${if (newState) "§aEnabled" else "§cDisabled"} §7(bypasses P3 check)"))
//                        1
//                    }
//                )
//        )
//    }
//
//    private fun showHelp(source: FabricClientCommandSource) {
//        source.sendFeedback(Component.literal("""
//            §6§l=== AutoLeap Commands ===
//            §e/autoleap toggle §7- Enable/disable module
//            §e/autoleap status §7- Show current state
//            §e/autoleap teammates §7- List dungeon teammates
//            §e/autoleap phase §7- Show current F7 phase
//            §e/autoleap reset §7- Reset all state
//            §e/autoleap force §7- Toggle force mode (bypass P3 check)
//            §6§l--- Testing ---
//            §e/autoleap simulate gate §7- Simulate gate blown
//            §e/autoleap simulate terminals §7- Simulate terminals done
//            §e/autoleap simulate both §7- Simulate both triggers
//            §e/autoleap leapto <name> §7- Force leap to player
//            §e/autoleap setphase <0-4> §7- Set internal phase counter
//            §6§l--- Config ---
//            §e/autoleap config §7- Show current config
//            §e/autoleap config ee2/ee3/core toggle §7- Toggle phase
//            §e/autoleap config ee2/ee3/core name <name> §7- Set IGN
//            §e/autoleap config ee2/ee3/core class <0-4> §7- Set class
//            §7Classes: 0=Archer, 1=Tank, 2=Healer, 3=Mage, 4=Bers
//        """.trimIndent()))
//    }
//
//    private fun showStatus(source: FabricClientCommandSource) {
//        val status = AutoLeap.getStatus()
//        source.sendFeedback(Component.literal("""
//            §6§l=== AutoLeap Status ===
//            §fEnabled: ${if (config.autoLeapEnabled()) "§aYes" else "§cNo"}
//            §fForce Mode: ${if (config.autoLeapForce()) "§aYes" else "§cNo"}
//            §fGate Blown: ${if (status.gateBlown) "§aYes" else "§cNo"}
//            §fTerminals Done: ${if (status.terminalsDone) "§aYes" else "§cNo"}
//            §fCurrent Phase: §e${status.currentPhase}
//            §fLeap In Progress: ${if (status.leapInProgress) "§aYes" else "§cNo"}
//            §fPending Target: §e${status.pendingTarget ?: "None"}
//            §fF7 Phase: §e${DungeonUtils.getF7Phase().displayName}
//        """.trimIndent()))
//    }
//
//    private fun showTeammates(source: FabricClientCommandSource) {
//        val teammates = DungeonUtils.dungeonTeammatesNoSelf
//        if (teammates.isEmpty()) {
//            source.sendFeedback(Component.literal("§7[AutoLeap] §cNo teammates found (not in dungeon?)"))
//            return
//        }
//
//        source.sendFeedback(Component.literal("§6§l=== Dungeon Teammates ==="))
//        teammates.forEach { player ->
//            val classColor = when (player.clazz) {
//                DungeonClass.Archer -> "§6"
//                DungeonClass.Berserk -> "§4"
//                DungeonClass.Healer -> "§d"
//                DungeonClass.Mage -> "§b"
//                DungeonClass.Tank -> "§2"
//                else -> "§7"
//            }
//            val pos = player.entity?.let { "(${it.x.toInt()}, ${it.y.toInt()}, ${it.z.toInt()})" } ?: "(unknown)"
//            val dead = if (player.isDead) " §c[DEAD]" else ""
//            source.sendFeedback(Component.literal("  $classColor${player.clazz.name} §f${player.name}$dead §7$pos"))
//        }
//    }
//
//    private fun showPhase(source: FabricClientCommandSource) {
//        val f7Phase = DungeonUtils.getF7Phase()
//        val player = mc.player
//        val myPhase = if (player != null) AutoLeap.getPhaseFromPositionPublic(player) else 0
//
//        source.sendFeedback(Component.literal("""
//            §6§l=== Phase Info ===
//            §fF7 Phase (Odin): §e${f7Phase.displayName}
//            §fMy Position Phase: §e$myPhase §7(1=P1, 2=P2, 3=P3, 4=P4, 5=Core, 6=P5)
//            §fIn Dungeons: ${if (DungeonUtils.inDungeons) "§aYes" else "§cNo"}
//            §fIn Boss: ${if (DungeonUtils.inBoss) "§aYes" else "§cNo"}
//            §fFloor: §e${DungeonUtils.floor?.name ?: "Unknown"}
//        """.trimIndent()))
//    }
//
//    private fun showConfig(source: FabricClientCommandSource) {
//        source.sendFeedback(Component.literal("""
//            §6§l=== AutoLeap Config ===
//            §fEE2: ${if (config.autoLeapEE2()) "§aEnabled" else "§cDisabled"}
//            §f  Name: §e${config.autoLeapEE2Name().ifEmpty { "(not set)" }}
//            §f  Class: §e${classes[config.autoLeapEE2Class()]}
//            §fEE3: ${if (config.autoLeapEE3()) "§aEnabled" else "§cDisabled"}
//            §f  Name: §e${config.autoLeapEE3Name().ifEmpty { "(not set)" }}
//            §f  Class: §e${classes[config.autoLeapEE3Class()]}
//            §fCore: ${if (config.autoLeapCore()) "§aEnabled" else "§cDisabled"}
//            §f  Name: §e${config.autoLeapCoreName().ifEmpty { "(not set)" }}
//            §f  Class: §e${classes[config.autoLeapCoreClass()]}
//        """.trimIndent()))
//    }
//}