package com.peachsoju.modules.impl.stormbow

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.RouteUtils
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import kotlin.random.Random

object StormBowTimer {

    private const val STORM_START = "[BOSS] Storm: Pathetic Maxor, just like expected."
    private const val STORM_END = "[BOSS] Storm: I should have known that I stood no chance."
    private const val TICK_DURATION = 0.05

    private var serverTicks = 0
    private var running = false
    private var releaseFired = false
    private var releaseVisibleUntil = 0L

    private val releaseTimeSeconds: Double get() = config.stormReleaseTime()
    private val autoRelease: Boolean get() = config.stormAutoRelease()
    private val enabled: Boolean get() = config.stormBowTimer()

    fun isRunning(): Boolean = running
    fun getCurrentTime(): Double = serverTicks * TICK_DURATION
    fun shouldShowRelease(): Boolean = System.currentTimeMillis() <= releaseVisibleUntil

    private fun message(message: String) {
        mc.player?.displayClientMessage(Component.literal("§7[StormBow] $message"), false)
    }

    private fun releaseBow(): Boolean {
        val player = mc.player ?: run {
             RouteUtils.extraDebug("§c[Release] No player found")
            return false
        }
        val connection = mc.connection ?: run {
             RouteUtils.extraDebug("§c[Release] No connection found")
            return false
        }

        val heldItem = player.mainHandItem.hoverName.string
         RouteUtils.extraDebug("§e[Release] Attempting release, holding: $heldItem")

        if (!RouteUtils.isHoldingItem("Last Breath")) {
             RouteUtils.extraDebug("§c[Release] Item check failed - not holding Last Breath")
            return false
        }

        try {
             RouteUtils.extraDebug("§a[Release] Releasing Last Breath at ${String.format("%.2f", getCurrentTime())}s (target: ${String.format("%.2f", releaseTimeSeconds)}s)")

            player.releaseUsingItem()
             RouteUtils.extraDebug("§7[Release] Called releaseUsingItem()")

            val releasePacket = ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                BlockPos.ZERO,
                Direction.DOWN
            )
            connection.send(releasePacket)
             RouteUtils.extraDebug("§7[Release] Sent RELEASE_USE_ITEM packet")

             RouteUtils.extraDebug("§a[Release] Last Breath released successfully at ${String.format("%.2f", getCurrentTime())}s")

            scheduleTerminatorSwap = true
            swapTickDelay = 2
             RouteUtils.extraDebug("§7[Release] Scheduled Terminator swap in 2 ticks")

            return true
        } catch (e: Exception) {
            message("§cFailed to release Last Breath: ${e.message}")
             RouteUtils.extraDebug("§c[Release] Exception: ${e.message}")
            return false
        }
    }

    private var scheduleTerminatorSwap = false
    private var swapTickDelay = 0

    private fun reset() {
         RouteUtils.extraDebug("§7[Reset] Resetting all state")
        serverTicks = 0
        running = false
        releaseFired = false
        releaseVisibleUntil = 0L
        scheduleTerminatorSwap = false
        swapTickDelay = 0
    }

    private fun startTimer() {
        if (!enabled) {
             RouteUtils.extraDebug("§c[Start] Timer disabled, ignoring start")
            return
        }

         RouteUtils.extraDebug("§a[Start] Starting timer - releaseTime=${String.format("%.2f", releaseTimeSeconds)}s, autoRelease=$autoRelease")
        serverTicks = 0
        running = true
        releaseFired = false
        releaseVisibleUntil = 0L

        message("§aStorm timer started")
    }

    private fun stopTimer() {
        if (running) {
            message("§eStorm timer stopped at ${String.format("%.2f", getCurrentTime())}s")
             RouteUtils.extraDebug("§e[Stop] Timer stopped at ${String.format("%.2f", getCurrentTime())}s (${serverTicks} ticks)")
        }
        running = false
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        val packet = event.packet

        if (packet is ClientboundPingPacket && running) {
            serverTicks++

            val currentTime = getCurrentTime()

            if (serverTicks % 20 == 0) {
                 RouteUtils.extraDebug("§7[Tick] ${String.format("%.2f", currentTime)}s / ${String.format("%.2f", releaseTimeSeconds)}s (${serverTicks} ticks)")
            }

            if (!releaseFired && currentTime >= releaseTimeSeconds) {
                releaseFired = true
                releaseVisibleUntil = System.currentTimeMillis() + 800

                 RouteUtils.extraDebug("§e[Trigger] Release time reached! currentTime=${String.format("%.2f", currentTime)}s, target=${String.format("%.2f", releaseTimeSeconds)}s")

                if (autoRelease) {
                    val delay = Random.nextInt(0, 3)
                     RouteUtils.extraDebug("§e[Trigger] Auto-release enabled, delay=$delay ticks")

                    if (delay == 0) {
                        if (releaseBow()) {
                            mc.player?.playSound(
                                net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                                1.0f, 1.5f
                            )
                             RouteUtils.extraDebug("§a[Trigger] Immediate release successful")
                        } else {
                             RouteUtils.extraDebug("§c[Trigger] Immediate release failed")
                        }
                    } else {
                        releaseDelayTicks = delay
                         RouteUtils.extraDebug("§7[Trigger] Delayed release scheduled for $delay ticks")
                    }
                } else {
                    mc.player?.playSound(
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        1.0f, 1.0f
                    )
                    message("§c§lRELEASE NOW!")
                     RouteUtils.extraDebug("§e[Trigger] Manual release - sound cue played")
                }
            }
        }

        if (packet is ClientboundSystemChatPacket) {
            val msg = packet.content().string

            if (msg.contains("[BOSS]")) {
                 RouteUtils.extraDebug("§7[Chat] Boss message: $msg")
            }

            if (msg.contains(STORM_START)) {
                 RouteUtils.extraDebug("§a[Chat] Storm START detected")
                startTimer()
            } else if (msg.contains(STORM_END)) {
                 RouteUtils.extraDebug("§e[Chat] Storm END detected")
                stopTimer()
            }
        }
    }

    private var releaseDelayTicks = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return

        if (releaseDelayTicks > 0) {
            releaseDelayTicks--
             RouteUtils.extraDebug("§7[DelayTick] releaseDelayTicks=$releaseDelayTicks")
            if (releaseDelayTicks == 0) {
                 RouteUtils.extraDebug("§e[DelayTick] Executing delayed release")
                if (releaseBow()) {
                    mc.player?.playSound(
                        net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                        1.0f, 1.5f
                    )
                     RouteUtils.extraDebug("§a[DelayTick] Delayed release successful")
                } else {
                     RouteUtils.extraDebug("§c[DelayTick] Delayed release failed")
                }
            }
        }

        if (scheduleTerminatorSwap) {
            if (swapTickDelay > 0) {
                swapTickDelay--
                 RouteUtils.extraDebug("§7[Swap] swapTickDelay=$swapTickDelay")
            } else {
                 RouteUtils.extraDebug("§e[Swap] Swapping to Terminator")
                val result = RouteUtils.swapToItem("Terminator")
                 RouteUtils.extraDebug("§7[Swap] Swap result: $result")
                scheduleTerminatorSwap = false
            }
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
         RouteUtils.extraDebug("§7[World] World loaded, resetting state")
        reset()
    }

    fun getRenderInfo(): StormTimerRenderInfo {
        return StormTimerRenderInfo(
            running = running,
            currentTime = getCurrentTime(),
            releaseTime = releaseTimeSeconds,
            shouldFlashRelease = shouldShowRelease(),
            autoRelease = autoRelease
        )
    }

    data class StormTimerRenderInfo(
        val running: Boolean,
        val currentTime: Double,
        val releaseTime: Double,
        val shouldFlashRelease: Boolean,
        val autoRelease: Boolean
    )

    fun forceStart() {
         RouteUtils.extraDebug("§a[ForceStart] Manually starting timer")
        startTimer()
    }

    fun forceStop() {
         RouteUtils.extraDebug("§e[ForceStop] Manually stopping timer")
        stopTimer()
    }
}