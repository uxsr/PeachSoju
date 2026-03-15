package com.peachsoju.modules.impl.misc.stormbow

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.RouteUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import kotlin.random.Random

object StormBowTimer {

    private const val STORM_START = "[BOSS] Storm: Pathetic Maxor, just like expected."
    private const val STORM_END = "[BOSS] Storm: I should have known that I stood no chance."
    private const val TICK_DURATION = 0.05

    private var serverTicks = 0
    private var running = false
    private var releaseFired = false
    private var releaseVisibleUntil = 0L
    private var releaseDelayTicks = 0
    private var scheduleTerminatorSwap = false
    private var swapTickDelay = 0

    private val releaseTimeSeconds: Double get() = config.stormReleaseTime()
    private val autoRelease: Boolean get() = config.stormAutoRelease()
    private val enabled: Boolean get() = config.stormBowTimer()

    fun isRunning(): Boolean = running
    fun getCurrentTime(): Double = serverTicks * TICK_DURATION
    fun shouldShowRelease(): Boolean = System.currentTimeMillis() <= releaseVisibleUntil

    private fun message(message: String) {
        mc.player?.displayClientMessage(Component.literal("§7[StormBow] $message"), false)
    }

    fun onServerTick() {
        if (!running) return

        serverTicks++

        val currentTime = getCurrentTime()

        if (serverTicks % 20 == 0) {
            RouteUtils.extraDebug("§7[Tick] ${String.format("%.2f", currentTime)}s / ${String.format("%.2f", releaseTimeSeconds)}s ($serverTicks ticks)")
        }

        if (!releaseFired && currentTime >= releaseTimeSeconds) {
            releaseFired = true
            releaseVisibleUntil = System.currentTimeMillis() + 800

            RouteUtils.extraDebug("§e[Trigger] Release time reached!")

            if (autoRelease) {
                val delay = Random.nextInt(0, 3)
                if (delay == 0) {
                    if (releaseBow()) {
                        mc.player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
                    }
                } else {
                    releaseDelayTicks = delay
                }
            } else {
                mc.player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                message("§c§lRELEASE NOW!")
            }
        }
    }

    private fun releaseBow(): Boolean {
        val player = mc.player ?: return false
        val connection = mc.connection ?: return false

        if (!RouteUtils.isHoldingItem("Last Breath")) {
            RouteUtils.extraDebug("§c[Release] Not holding Last Breath")
            return false
        }

        try {
            RouteUtils.extraDebug("§a[Release] Releasing at ${String.format("%.2f", getCurrentTime())}s")

            player.releaseUsingItem()

            connection.send(ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN
            ))

            scheduleTerminatorSwap = true
            swapTickDelay = 2

            return true
        } catch (e: Exception) {
            message("§cFailed to release: ${e.message}")
            return false
        }
    }

    private fun reset() {
        serverTicks = 0
        running = false
        releaseFired = false
        releaseVisibleUntil = 0L
        scheduleTerminatorSwap = false
        swapTickDelay = 0
        releaseDelayTicks = 0
    }

    private fun startTimer() {
        if (!enabled) return

        RouteUtils.extraDebug("§a[Start] Starting timer")
        serverTicks = 0
        running = true
        releaseFired = false
        releaseVisibleUntil = 0L

        message("§aStorm timer started")
    }

    private fun stopTimer() {
        if (running) {
            message("§eTimer stopped at ${String.format("%.2f", getCurrentTime())}s")
        }
        running = false
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        val packet = event.packet

        if (packet is ClientboundSystemChatPacket) {
            val msg = packet.content().string

            if (msg.contains(STORM_START)) {
                RouteUtils.extraDebug("§a[Chat] Storm START detected")
                startTimer()
            } else if (msg.contains(STORM_END)) {
                RouteUtils.extraDebug("§e[Chat] Storm END detected")
                stopTimer()
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return

        if (releaseDelayTicks > 0) {
            releaseDelayTicks--
            if (releaseDelayTicks == 0) {
                if (releaseBow()) {
                    mc.player?.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f)
                }
            }
        }

        if (scheduleTerminatorSwap) {
            if (swapTickDelay > 0) {
                if (swapTickDelay == 2) {
                    mc.options.keyUse.isDown = false
                } else if (swapTickDelay == 1) {
                    mc.options.keyUse.isDown = true
                }
                swapTickDelay--
            } else {
                RouteUtils.swapToItem("Terminator")
                scheduleTerminatorSwap = false
            }
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
        reset()
    }

    fun getRenderInfo() = StormTimerRenderInfo(
        running = running,
        currentTime = getCurrentTime(),
        releaseTime = releaseTimeSeconds,
        shouldFlashRelease = shouldShowRelease(),
        autoRelease = autoRelease
    )

    data class StormTimerRenderInfo(
        val running: Boolean,
        val currentTime: Double,
        val releaseTime: Double,
        val shouldFlashRelease: Boolean,
        val autoRelease: Boolean
    )

    fun forceStart() = startTimer()
    fun forceStop() = stopTimer()
}