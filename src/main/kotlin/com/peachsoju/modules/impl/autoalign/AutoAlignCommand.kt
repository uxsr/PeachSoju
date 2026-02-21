//package com.peachsoju.modules.impl.autoalign
//
//import com.mojang.brigadier.CommandDispatcher
//import com.mojang.brigadier.arguments.IntegerArgumentType
//import com.peachsoju.config
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
//import net.minecraft.commands.CommandBuildContext
//import net.minecraft.network.chat.Component
//
//object AutoAlignCommand {
//
//    fun register() {
//        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
//            registerCommands(dispatcher)
//        }
//    }
//
//    private fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
//        dispatcher.register(
//            literal("autoalign")
//                .executes { ctx ->
//                    val enabled = config.toggleAutoAlign()
//                    ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §${if (enabled) "a" else "c"}${if (enabled) "Enabled" else "Disabled"}"))
//                    1
//                }
//                .then(literal("toggle")
//                    .executes { ctx ->
//                        val enabled = config.toggleAutoAlign()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §${if (enabled) "a" else "c"}${if (enabled) "Enabled" else "Disabled"}"))
//                        1
//                    }
//                )
//                .then(literal("on")
//                    .executes { ctx ->
//                        config.setAutoAlign(true)
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §aEnabled"))
//                        1
//                    }
//                )
//                .then(literal("off")
//                    .executes { ctx ->
//                        config.setAutoAlign(false)
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §cDisabled"))
//                        1
//                    }
//                )
//                .then(literal("force")
//                    .executes { ctx ->
//                        val forced = config.toggleAutoAlignForceDevice()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] Force device: §${if (forced) "a" else "c"}${if (forced) "ON" else "OFF"}"))
//                        if (forced) {
//                            ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §eWill ignore distance check and phase check"))
//                        }
//                        1
//                    }
//                )
//                .then(literal("start")
//                    .executes { ctx ->
//                        config.setAutoAlign(true)
//                        AutoAlign.reset()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §aStarted fresh"))
//                        1
//                    }
//                )
//                .then(literal("stop")
//                    .executes { ctx ->
//                        config.setAutoAlign(false)
//                        AutoAlign.reset()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §cStopped and reset"))
//                        1
//                    }
//                )
//                .then(literal("reset")
//                    .executes { ctx ->
//                        AutoAlign.reset()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] §eReset clicked frames"))
//                        1
//                    }
//                )
//                .then(literal("delay")
//                    .then(argument("ticks", IntegerArgumentType.integer(0, 20))
//                        .executes { ctx ->
//                            val ticks = IntegerArgumentType.getInteger(ctx, "ticks")
//                            config.setAutoAlignDelay(ticks)
//                            ctx.source.sendFeedback(Component.literal("§7[AutoAlign] Delay set to §a$ticks §7ticks"))
//                            1
//                        }
//                    )
//                    .executes { ctx ->
//                        val current = config.autoAlignDelay()
//                        ctx.source.sendFeedback(Component.literal("§7[AutoAlign] Current delay: §a$current §7ticks"))
//                        1
//                    }
//                )
//                .then(literal("status")
//                    .executes { ctx ->
//                        val enabled = config.autoAlign()
//                        val forced = config.autoAlignForceDevice()
//                        val delay = config.autoAlignDelay()
//                        val status = AutoAlign.getStatus()
//
//                        ctx.source.sendFeedback(Component.literal("§7§l[AutoAlign Status]"))
//                        ctx.source.sendFeedback(Component.literal("§7Enabled: §${if (enabled) "a" else "c"}${if (enabled) "Yes" else "No"}"))
//                        ctx.source.sendFeedback(Component.literal("§7Force Device: §${if (forced) "a" else "c"}${if (forced) "Yes" else "No"}"))
//                        ctx.source.sendFeedback(Component.literal("§7Delay: §a$delay §7ticks"))
//                        ctx.source.sendFeedback(Component.literal("§7At Device: §${if (status.atDevice) "a" else "c"}${if (status.atDevice) "Yes" else "No"}"))
//                        ctx.source.sendFeedback(Component.literal("§7Frames Clicked: §a${status.clickedCount}"))
//                        ctx.source.sendFeedback(Component.literal("§7Frames Remaining: §e${status.remainingCount}"))
//                        1
//                    }
//                )
//                .then(literal("help")
//                    .executes { ctx ->
//                        ctx.source.sendFeedback(Component.literal("§7§l[AutoAlign Commands]"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign §7- Toggle on/off"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign on/off §7- Enable/disable"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign start §7- Enable and reset"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign stop §7- Disable and reset"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign reset §7- Clear clicked frames"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign force §7- Toggle force device (ignore checks)"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign delay <ticks> §7- Set delay between clicks"))
//                        ctx.source.sendFeedback(Component.literal("§a/autoalign status §7- Show current status"))
//                        ctx.source.sendFeedback(Component.literal("§7§oRequires Odin's Arrow Align solver!"))
//                        1
//                    }
//                )
//        )
//    }
//}