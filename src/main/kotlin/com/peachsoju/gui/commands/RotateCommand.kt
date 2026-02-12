package com.peachsoju.gui.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.FloatArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.util.Mth

object RotateCommand {

    private val mc: Minecraft get() = Minecraft.getInstance()
    private var showYaw = false

    fun register() {
        HudRenderCallback.EVENT.register(HudRenderCallback { graphics, _ -> renderHud(graphics) })

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("showyaw").executes {
                    showYaw = !showYaw
                    Command.SINGLE_SUCCESS
                }
            )
            dispatcher.register(
                ClientCommandManager.literal("rotate")
                    .then(
                        ClientCommandManager.argument("yaw", FloatArgumentType.floatArg(-180f, 180f))
                        .then(
                            ClientCommandManager.argument("pitch", FloatArgumentType.floatArg(-90f, 90f))
                            .executes { ctx ->
                                val p = mc.player ?: return@executes Command.SINGLE_SUCCESS
                                val yaw = Mth.wrapDegrees(FloatArgumentType.getFloat(ctx, "yaw"))
                                val pitch = FloatArgumentType.getFloat(ctx, "pitch").coerceIn(-90f, 90f)
                                p.yRot = yaw
                                p.xRot = pitch
                                Command.SINGLE_SUCCESS
                            }
                        )
                    )
            )
        }
    }

    private fun renderHud(graphics: GuiGraphics) {
        if (!showYaw) return
        val p = mc.player ?: return
        val w = mc.window.guiScaledWidth
        val h = mc.window.guiScaledHeight

        val yaw = Mth.wrapDegrees(p.yRot)
        val pitch = p.xRot.coerceIn(-90f, 90f)

        val text = "Yaw: %.1f  Pitch: %.1f".format(yaw, pitch)
        val x = w / 2 - mc.font.width(text) / 2
        val y = h / 2 + 10

        graphics.drawString(mc.font, text, x, y, 0xFFFFFFFF.toInt(), true)
    }
}