package com.peachsoju.modules.impl.chatbypass

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.utils.RouteUtils
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundChatPacket

object ChatBypass {

    private const val NORMAL = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789"
    private const val CUSTOM = "ｑｗｅｒｔｙｕｉｏｐａｓｄｆｇｈｊｋｌｚｘｃｖｂｎｍＱＷＥＲＴＹＵＩＯＰＡＳＤＦＧＨＪＫＬＺＸＣＶＢＮＭ０１２３４５６７８９"
    private val DM_COMMANDS = listOf("/msg", "/message", "/t", "/tell", "/w")

    private var prefix = ""

    @SubscribeEvent
    fun onPacketSend(event: PacketEvent.Send) {
        val packet = event.packet

        if (packet is ServerboundChatPacket) {
            val message = packet.message
            prefix = ""

            if (message.startsWith("/")) {
                val parts = message.split(" ")
                prefix = parts[0]

                if (DM_COMMANDS.any { prefix.equals(it, ignoreCase = true) }) {
                    prefix += " "
                    if (parts.size > 1) {
                        prefix += parts[1]
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        if (!config.chatBypass()) return

        val packet = event.packet

        if (packet is ClientboundSystemChatPacket) {
            val message = packet.content.string
            val stripped = message.replace(Regex("§."), "")

            if (message == "§r§6§m-----------------------------------------§r") {
                event.cancelled = true
                return
            }

            if (stripped.startsWith("We blocked your comment \"")) {
                val quoteParts = stripped.split("\"")
                if (quoteParts.size >= 2) {
                    val blockedMsg = quoteParts.drop(1).dropLast(1).joinToString("\"")

                    val newMessage = buildString {
                        if (prefix.isNotEmpty()) {
                            append(prefix)
                            append(" ")
                        }

                        for (char in blockedMsg) {
                            when (config.chatBypassMode()) {
                                "FONT" -> {
                                    val index = NORMAL.indexOf(char)
                                    append(if (index != -1) CUSTOM[index] else char)
                                }
                                "DOTS" -> {
                                    append(char)
                                    if (char != ' ') append('\u02cc')
                                }
                            }
                        }
                    }

                    event.cancelled = true
                    RouteUtils.extraDebug("§e[ChatBypass] Resending with ${config.chatBypassMode()} mode")

                    Thread {
                        Thread.sleep(550)
                        mc.player?.connection?.sendChat(newMessage)
                    }.start()
                }
            }
        }
    }
}