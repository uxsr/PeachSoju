//package com.peachsoju.modules.impl.stormbow
//
//import com.mojang.brigadier.Command
//import com.mojang.brigadier.arguments.DoubleArgumentType
//import com.peachsoju.PeachSoju.mc
//import com.peachsoju.config
//import com.peachsoju.modules.impl.stormbow.StormBowTimer
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//import net.minecraft.network.chat.Component
//
//object StormBowCommand {
//    fun register() {
//        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
//            dispatcher.register(
//                ClientCommandManager.literal("stormbowtest")
//                    .executes { ctx ->
//                        val enabled = config.stormBowTimer()
//                        val auto = config.stormAutoRelease()
//                        val time = config.stormReleaseTime()
//                        ctx.source.sendFeedback(Component.literal("§6StormBowTimer: ${if (enabled) "§aON" else "§cOFF"} §8| §7Auto: ${if (auto) "§aON" else "§cOFF"} §8| §7Time: §e${String.format("%.2f", time)}s"))
//                        Command.SINGLE_SUCCESS
//                    }
//                    .then(ClientCommandManager.literal("start").executes { ctx ->
//                        // Directly start the timer for testing
//                        StormBowTimer.forceStart()
//                        ctx.source.sendFeedback(Component.literal("§aTimer started (test mode)"))
//                        Command.SINGLE_SUCCESS
//                    })
//                    .then(ClientCommandManager.literal("stop").executes { ctx ->
//                        // Directly stop the timer
//                        StormBowTimer.forceStop()
//                        ctx.source.sendFeedback(Component.literal("§eTimer stopped"))
//                        Command.SINGLE_SUCCESS
//                    })
//                    .then(ClientCommandManager.literal("toggle").executes { ctx ->
//                        val on = config.toggleStormBowTimer()
//                        ctx.source.sendFeedback(Component.literal("§6StormBowTimer: ${if (on) "§aON" else "§cOFF"}"))
//                        Command.SINGLE_SUCCESS
//                    })
//                    .then(ClientCommandManager.literal("auto").executes { ctx ->
//                        val on = config.toggleStormAutoRelease()
//                        ctx.source.sendFeedback(Component.literal("§6Auto Release: ${if (on) "§aON" else "§cOFF"}"))
//                        Command.SINGLE_SUCCESS
//                    })
//                    .then(ClientCommandManager.literal("time")
//                        .then(ClientCommandManager.argument("seconds", DoubleArgumentType.doubleArg(5.0, 45.0))
//                            .executes { ctx ->
//                                val time = DoubleArgumentType.getDouble(ctx, "seconds")
//                                config.setStormReleaseTime(time)
//                                ctx.source.sendFeedback(Component.literal("§6Release time set to §e${String.format("%.2f", time)}s"))
//                                Command.SINGLE_SUCCESS
//                            }
//                        )
//                    )
//                    .then(ClientCommandManager.literal("status").executes { ctx ->
//                        val info = StormBowTimer.getRenderInfo()
//                        if (info.running) {
//                            ctx.source.sendFeedback(Component.literal("§aRunning §8| §7Time: §e${String.format("%.2f", info.currentTime)}s §8| §7Target: §e${String.format("%.2f", info.releaseTime)}s"))
//                        } else {
//                            ctx.source.sendFeedback(Component.literal("§cNot running"))
//                        }
//                        Command.SINGLE_SUCCESS
//                    })
//            )
//        }
//    }
//}