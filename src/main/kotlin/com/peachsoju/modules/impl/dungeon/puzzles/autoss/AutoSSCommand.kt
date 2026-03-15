//package com.peachsoju.modules.impl.autoss
//
//import com.peachsoju.config
//import com.mojang.brigadier.Command
//import com.mojang.brigadier.arguments.DoubleArgumentType
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
//import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//import net.minecraft.network.chat.Component
//
//object AutoSSCommand {
//
//    fun register() {
//        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
//            dispatcher.register(
//                ClientCommandManager.literal("autoss")
//                    .executes { ctx ->
//                        val enabled = config.autoSS()
//                        val delay = config.autoSSDelay()
//                        val autoStartDelay = config.autoSSAutoStartDelay()
//                        val smoothRotate = config.autoSSSmoothRotate()
//                        val rotationTime = config.autoSSRotationTime()
//                        val forceDevice = config.autoSSForceDevice()
//                        val dontCheck = config.autoSSDontCheck()
//
//                        ctx.source.sendFeedback(Component.literal("§d§lAutoSS Settings"))
//                        ctx.source.sendFeedback(Component.literal("§7Status: ${if (enabled) "§aENABLED" else "§cDISABLED"}"))
//                        ctx.source.sendFeedback(Component.literal("§7Delay: §e${delay.toInt()}ms"))
//                        ctx.source.sendFeedback(Component.literal("§7Auto Start Delay: §e${autoStartDelay.toInt()}ms"))
//                        ctx.source.sendFeedback(Component.literal("§7Smooth Rotate: ${if (smoothRotate) "§aON" else "§cOFF"}"))
//                        ctx.source.sendFeedback(Component.literal("§7Rotation Time: §e${rotationTime.toInt()}ms"))
//                        ctx.source.sendFeedback(Component.literal("§7Force Device: ${if (forceDevice) "§aON" else "§cOFF"}"))
//                        ctx.source.sendFeedback(Component.literal("§7Faster SS: ${if (dontCheck) "§aON" else "§cOFF"}"))
//                        ctx.source.sendFeedback(Component.literal(""))
//                        ctx.source.sendFeedback(Component.literal("§7Use §e/autoss help §7for commands"))
//
//                        Command.SINGLE_SUCCESS
//                    }
//
//                    .then(ClientCommandManager.literal("help").executes { ctx ->
//                        ctx.source.sendFeedback(Component.literal("§d§lAutoSS Commands"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss §7- Show current settings"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss toggle §7- Toggle AutoSS on/off"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss delay <ms> §7- Set click delay (50-500)"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss startdelay <ms> §7- Set auto start delay (50-200)"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss rotate §7- Toggle smooth rotation"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss rotationtime <ms> §7- Set rotation time (0-500)"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss force §7- Toggle force device mode"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss faster §7- Toggle faster SS mode"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss reset §7- Reset to device start"))
//                        ctx.source.sendFeedback(Component.literal("§e/autoss start §7- Manually start AutoSS"))
//
//                        Command.SINGLE_SUCCESS
//                    })
//
//                    .then(ClientCommandManager.literal("toggle").executes { ctx ->
//                        val on = config.toggleAutoSS()
//                        ctx.source.sendFeedback(Component.literal("§dAutoSS: ${if (on) "§aENABLED" else "§cDISABLED"}"))
//                        Command.SINGLE_SUCCESS
//                    })
//
//                    .then(
//                        ClientCommandManager.literal("delay")
//                            .then(
//                                ClientCommandManager.argument("ms", DoubleArgumentType.doubleArg(50.0, 500.0))
//                                    .executes { ctx ->
//                                        val ms = DoubleArgumentType.getDouble(ctx, "ms")
//                                        config.setAutoSSDelay(ms)
//                                        ctx.source.sendFeedback(Component.literal("§dAutoSS delay set to §e${ms.toInt()}ms"))
//                                        Command.SINGLE_SUCCESS
//                                    }
//                            )
//                    )
//
//                    .then(
//                        ClientCommandManager.literal("startdelay")
//                            .then(
//                                ClientCommandManager.argument("ms", DoubleArgumentType.doubleArg(50.0, 200.0))
//                                    .executes { ctx ->
//                                        val ms = DoubleArgumentType.getDouble(ctx, "ms")
//                                        config.setAutoSSAutoStartDelay(ms)
//                                        ctx.source.sendFeedback(Component.literal("§dAutoSS auto start delay set to §e${ms.toInt()}ms"))
//                                        Command.SINGLE_SUCCESS
//                                    }
//                            )
//                    )
//
//                    .then(ClientCommandManager.literal("rotate").executes { ctx ->
//                        val on = config.toggleAutoSSSmoothRotate()
//                        ctx.source.sendFeedback(Component.literal("§dSmooth rotation: ${if (on) "§aON" else "§cOFF"}"))
//                        Command.SINGLE_SUCCESS
//                    })
//
//                    .then(
//                        ClientCommandManager.literal("rotationtime")
//                            .then(
//                                ClientCommandManager.argument("ms", DoubleArgumentType.doubleArg(0.0, 500.0))
//                                    .executes { ctx ->
//                                        val ms = DoubleArgumentType.getDouble(ctx, "ms")
//                                        config.setAutoSSRotationTime(ms)
//                                        ctx.source.sendFeedback(Component.literal("§dRotation time set to §e${ms.toInt()}ms"))
//                                        Command.SINGLE_SUCCESS
//                                    }
//                            )
//                    )
//
//                    .then(ClientCommandManager.literal("force").executes { ctx ->
//                        val on = config.toggleAutoSSForceDevice()
//                        ctx.source.sendFeedback(Component.literal("§dForce device mode: ${if (on) "§aON" else "§cOFF"}"))
//                        Command.SINGLE_SUCCESS
//                    })
//
//                    .then(ClientCommandManager.literal("reset").executes { ctx ->
//                        AutoSS.reset()
//                        ctx.source.sendFeedback(Component.literal("§dAutoSS reset"))
//                        Command.SINGLE_SUCCESS
//                    })
//
//                    .then(ClientCommandManager.literal("start").executes { ctx ->
//                        AutoSS.start()
//                        ctx.source.sendFeedback(Component.literal("§dAutoSS started manually"))
//                        Command.SINGLE_SUCCESS
//                    })
//            )
//
//            // Short alias
//            dispatcher.register(
//                ClientCommandManager.literal("ss")
//                    .executes { ctx ->
//                        val on = config.toggleAutoSS()
//                        ctx.source.sendFeedback(Component.literal("§dAutoSS: ${if (on) "§aENABLED" else "§cDISABLED"}"))
//                        Command.SINGLE_SUCCESS
//                    }
//            )
//        }
//    }
//}