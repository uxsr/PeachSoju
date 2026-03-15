package com.peachsoju.modules.impl.dungeon.puzzles.autoalign

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.RouteUtils
import com.odtheking.odin.features.impl.floor7.ArrowAlign
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3

object AutoAlign {

    private val enabled get() = config.autoAlign()
    private val forceDevice get() = config.autoAlignForceDevice()
    private val delay get() = config.autoAlignDelay()

    private val clickedFrames = mutableListOf<Int>()
    private val frameGridCorner = BlockPos(-2, 120, 75)
    private val deviceCenter = Vec3(0.0, 120.0, 77.0)

    private var ticksSinceLastClick = 0
    private var isAtDeviceCached = false

    private val localClicksRemaining = mutableMapOf<Int, Int>()
    private var currentFrameRotations: List<Int>? = null
    private var targetSolution: List<Int>? = null
    private val recentClickTimestamps = mutableMapOf<Int, Long>()

    private val possibleSolutions = listOf(
        listOf(7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, -1, -1, 7, 1),
        listOf(-1, -1, 7, 7, 5, -1, 7, 1, -1, 5, -1, -1, -1, -1, -1, -1, 7, 5, -1, 1, -1, -1, 7, 7, 1),
        listOf(7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, -1, 7, 5, -1, -1, -1, -1, 5, -1, -1, -1, 3, 3),
        listOf(5, 3, 3, 3, -1, 5, -1, -1, -1, -1, 7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, -1),
        listOf(5, 3, 3, 3, 3, 5, -1, -1, -1, 1, 7, 7, -1, -1, 1, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1),
        listOf(7, 7, 7, 7, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1),
        listOf(-1, -1, -1, -1, -1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1),
        listOf(-1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, 7, 7, 7, 7, 1, -1, -1, -1, -1, -1),
        listOf(-1, -1, -1, -1, -1, -1, 1, -1, 1, -1, 7, 1, 7, 1, 3, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1)
    )

    data class Status(
        val atDevice: Boolean,
        val clickedCount: Int,
        val remainingCount: Int
    )

    fun getStatus(): Status {
        val remaining = if (forceDevice) {
            localClicksRemaining.count { it.value > 0 }
        } else {
            getOdinClicksRemainingMap()?.count { it.value > 0 } ?: 0
        }
        return Status(
            atDevice = isAtDeviceCached,
            clickedCount = clickedFrames.size,
            remainingCount = remaining
        )
    }

    private fun isAtDevice(): Boolean {
        val player = mc.player ?: return false
        val playerPos = player.position()

        val distToCenter = playerPos.distanceToSqr(deviceCenter)
        if (distToCenter < 25.0) return true

        val nearStandingBlocks = playerPos.y in 116.0..119.0 &&
                playerPos.x in -2.0..2.0 &&
                playerPos.z in 78.0..81.0

        return nearStandingBlocks
    }

    private fun isInCorrectPhase(): Boolean {
        if (forceDevice) return true
        return DungeonUtils.getF7Phase() == M7Phases.P3
    }

    private fun getFramePositionFromIndex(index: Int): BlockPos {
        return frameGridCorner.offset(0, index % 5, index / 5)
    }

    private fun getFrameIndexFromEntity(entity: ItemFrame): Int {
        val pos = entity.blockPosition()
        val x = pos.x
        val y = pos.y
        val z = pos.z

        if (x != frameGridCorner.x) return -1

        val index = ((y - frameGridCorner.y) + (z - frameGridCorner.z) * 5)
        return if (index in 0..24) index else -1
    }

    private fun calculateClicksNeeded(currentRotation: Int, targetRotation: Int): Int {
        return (8 - currentRotation + targetRotation) % 8
    }

    private fun getFrames(): List<Int> {
        val level = mc.level ?: return List(25) { -1 }

        val itemFrames = level.entitiesForRendering()
            .filterIsInstance<ItemFrame>()
            .filter { it.item.item == Items.ARROW }
            .takeIf { it.isNotEmpty() } ?: return List(25) { -1 }

        return (0..24).map { index ->
            if (recentClickTimestamps[index]?.let { System.currentTimeMillis() - it < 1000 } == true && currentFrameRotations != null) {
                currentFrameRotations?.get(index) ?: -1
            } else {
                itemFrames.find { it.blockPosition() == getFramePositionFromIndex(index) }?.rotation ?: -1
            }
        }
    }

    private fun updateLocalSolver() {
        localClicksRemaining.clear()

        currentFrameRotations = getFrames()
        val rotations = currentFrameRotations ?: return

        for (solution in possibleSolutions) {
            var matches = true
            for (i in solution.indices) {
                if ((solution[i] == -1 || rotations[i] == -1) && solution[i] != rotations[i]) {
                    matches = false
                    break
                }
            }

            if (matches) {
                targetSolution = solution
                for (i in solution.indices) {
                    val clicks = calculateClicksNeeded(rotations[i], solution[i])
                    if (clicks != 0) {
                        localClicksRemaining[i] = clicks
                    }
                }
                RouteUtils.extraDebug("§a[AutoAlign] Found solution! ${localClicksRemaining.size} frames need clicks")
                return
            }
        }

        RouteUtils.extraDebug("§c[AutoAlign] No matching solution found")
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent) {
        reset()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return
        if (mc.level == null) return

        if (!isInCorrectPhase()) {
            isAtDeviceCached = false
            return
        }

        isAtDeviceCached = isAtDevice()

        if (!isAtDeviceCached) {
            if (clickedFrames.isNotEmpty()) {
                clickedFrames.clear()
                RouteUtils.extraDebug("§e[AutoAlign] Left device area, cleared clicked frames")
            }
            return
        }

        if (forceDevice) {
            updateLocalSolver()
        }

        if (delay > 0) {
            ticksSinceLastClick++
            if (ticksSinceLastClick < delay) return
        }

        val player = mc.player ?: return
        val level = mc.level ?: return

        val frames = level.entitiesForRendering().filter {
            it is ItemFrame &&
                    it.item.item == Items.ARROW &&
                    !clickedFrames.contains(it.id)
        }

        for (entity in frames) {
            val frame = entity as ItemFrame

            val eyePos = player.getEyePosition(1f)
            if (eyePos.distanceTo(frame.position()) > 4.5) continue

            val frameIndex = getFrameIndexFromEntity(frame)
            if (frameIndex == -1) continue

            val clicksLeft = if (forceDevice) {
                localClicksRemaining[frameIndex]
            } else {
                getOdinClicksRemaining(frameIndex)
            } ?: continue

            if (clicksLeft <= 0) continue

            val connection = mc.connection ?: return
            for (i in 1..clicksLeft) {
                connection.send(ServerboundInteractPacket.createInteractionPacket(
                    frame,
                    false,
                    InteractionHand.MAIN_HAND,
                    Vec3(0.03125, 0.0, 0.0)
                ))
                connection.send(ServerboundInteractPacket.createInteractionPacket(
                    frame,
                    false,
                    InteractionHand.MAIN_HAND
                ))
            }

            clickedFrames.add(frame.id)
            recentClickTimestamps[frameIndex] = System.currentTimeMillis()

            currentFrameRotations = currentFrameRotations?.toMutableList()?.apply {
                this[frameIndex] = (this[frameIndex] + clicksLeft) % 8
            }

            localClicksRemaining.remove(frameIndex)

            ticksSinceLastClick = 0
            RouteUtils.extraDebug("§a[AutoAlign] Clicked frame #$frameIndex ($clicksLeft clicks)")
            return
        }
    }

    private fun getOdinClicksRemaining(frameIndex: Int): Int? {
        return getOdinClicksRemainingMap()?.get(frameIndex)
    }

    private fun getOdinClicksRemainingMap(): Map<Int, Int>? {
        return try {
            val field = ArrowAlign::class.java.getDeclaredField("clicksRemaining")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            field.get(ArrowAlign) as? Map<Int, Int>
        } catch (e: Exception) {
            null
        }
    }

    fun reset() {
        clickedFrames.clear()
        ticksSinceLastClick = 0
        isAtDeviceCached = false
        localClicksRemaining.clear()
        currentFrameRotations = null
        targetSolution = null
        recentClickTimestamps.clear()
        RouteUtils.extraDebug("§e[AutoAlign] Reset")
    }
}