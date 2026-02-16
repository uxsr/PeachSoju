//Not registered use sex5 aura instead

package com.peachsoju.modules

import com.peachsoju.PeachSoju
import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.handlers.RightClickHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SkullBlock
import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

object SecretAura {

    var enabled = false

    private val clickedBlocks = mutableSetOf<BlockPos>()
    private val blockCooldowns = mutableMapOf<BlockPos, Long>()

    var auraRange = 6.0
    var skullRange = 4.5
    private const val COOLDOWN_MS = 500L
    private const val WITHER_ESSENCE_UUID = "e0f3e929-869e-3dca-9504-54c666ee6f23"

    private enum class ClickType { CHEST, SKULL }

    private data class QueuedClick(
        val pos: BlockPos,
        val hit: BlockHitResult,
        val type: ClickType
    )

    private val clickQueue = ArrayDeque<QueuedClick>()
    private val queuedBlocks = mutableSetOf<BlockPos>()
    private const val MAX_QUEUE = 32

    fun init() {
        PeachSoju.eventBus.register(this)
    }

    fun toggle(): Boolean {
        enabled = !enabled
        if (!enabled) {
            clickedBlocks.clear()
            blockCooldowns.clear()
            clickQueue.clear()
            queuedBlocks.clear()
        }
        return enabled
    }

    private fun debug(message: String) {
        if (!com.peachsoju.config.debug()) return
        mc.player?.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§7[SecretAura] $message"),
            false
        )
    }

    private fun processQueue(currentTime: Long) {
        if (clickQueue.isEmpty()) return

        val next = clickQueue.removeFirst()
        queuedBlocks.remove(next.pos)

        val lastClick = blockCooldowns[next.pos]
        if (lastClick != null && currentTime - lastClick < COOLDOWN_MS) return

        RightClickHandler.doBlockInteract(next.hit)

        blockCooldowns[next.pos] = currentTime
        clickedBlocks.add(next.pos)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return
//         if (!LocationUtils.inDungeon) return

        val player = mc.player ?: return
        val level = mc.level ?: return

        val eyePos = player.eyePosition
        val currentTime = System.currentTimeMillis()

        processQueue(currentTime)

        val range = auraRange.toInt() + 1
        val minPos = BlockPos.containing(eyePos.x - range, eyePos.y - range, eyePos.z - range)
        val maxPos = BlockPos.containing(eyePos.x + range, eyePos.y + range, eyePos.z + range)

        for (pos in BlockPos.betweenClosed(minPos, maxPos)) {
            val blockPos = pos.immutable()

            if (blockPos in clickedBlocks) continue
            if (blockPos in queuedBlocks) continue

            val lastClick = blockCooldowns[blockPos]
            if (lastClick != null && currentTime - lastClick < COOLDOWN_MS) continue

            val state = level.getBlockState(blockPos)
            val block = state.block

            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.LEVER) {
                val distance = eyePos.distanceTo(Vec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5))  //thanks leo ur the goat
                if (distance > auraRange) continue

                val hitResult = findHitVec(blockPos, eyePos) ?: continue
                debug("§aCHEST CLICK")
                debug(" blockPos=${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
                debug(" eyePos=${"%.3f".format(eyePos.x)}, ${"%.3f".format(eyePos.y)}, ${"%.3f".format(eyePos.z)}")
                debug(" distance=${"%.3f".format(distance)}")
                debug(" hitVec=${"%.5f".format(hitResult.location.x)}, ${"%.5f".format(hitResult.location.y)}, ${"%.5f".format(hitResult.location.z)}")
                debug(" face=${hitResult.direction}")
                debug(" playerYaw=${"%.2f".format(player.yRot)}, playerPitch=${"%.2f".format(player.xRot)}")

                if (clickQueue.size < MAX_QUEUE && queuedBlocks.add(blockPos)) {
                    clickQueue.addLast(QueuedClick(blockPos, hitResult, ClickType.CHEST))
                }
                continue
            }

            if (block is SkullBlock) {
                val blockEntity = level.getBlockEntity(blockPos) as? SkullBlockEntity ?: continue
                val ownerProfile = blockEntity.ownerProfile ?: continue
                val uuid = ownerProfile.partialProfile().id.toString()
                if (uuid != WITHER_ESSENCE_UUID) continue

                val targetVec = Vec3(blockPos.x + 0.5, blockPos.y + 0.25, blockPos.z + 0.5)
                val distance = eyePos.distanceTo(targetVec)
                if (distance > skullRange) continue

                val hitResult = findHitVec(blockPos, eyePos) ?: continue

                debug("§aSKULL CLICK")
                debug(" blockPos=${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
                debug(" eyePos=${"%.3f".format(eyePos.x)}, ${"%.3f".format(eyePos.y)}, ${"%.3f".format(eyePos.z)}")
                debug(" distance=${"%.3f".format(distance)}")
                debug(" hitVec=${"%.5f".format(hitResult.location.x)}, ${"%.5f".format(hitResult.location.y)}, ${"%.5f".format(hitResult.location.z)}")
                debug(" face=${hitResult.direction}")
                debug(" playerYaw=${"%.2f".format(player.yRot)}, playerPitch=${"%.2f".format(player.xRot)}")

                if (clickQueue.size < MAX_QUEUE && queuedBlocks.add(blockPos)) {
                    clickQueue.addLast(QueuedClick(blockPos, hitResult, ClickType.SKULL))
                }
            }
        }
    }

    private val placeOffsets = listOf(
        0.03125, 0.09375, 0.15625, 0.21875,
        0.28125, 0.34375, 0.40625, 0.46875,
        0.53125, 0.59375, 0.65625, 0.71875,
        0.78125, 0.84375, 0.90625, 0.96875
    )

    private fun findHitVec(pos: BlockPos, eyePos: Vec3): BlockHitResult? {
        val player = mc.player ?: return null

        val side = when {
            player.x - pos.x >= 1.0 -> Direction.EAST
            player.x - pos.x < 0.0 -> Direction.WEST
            player.z - pos.z >= 1.0 -> Direction.SOUTH
            player.z - pos.z < 0.0 -> Direction.NORTH
            else -> Direction.UP
        }

        var best: Vec3? = null

        for (first in placeOffsets) {
            for (second in placeOffsets) {
                val relative = when (side) {
                    Direction.UP -> Vec3(first, 1.0, second)
                    Direction.NORTH -> Vec3(first, second, 0.0)
                    Direction.SOUTH -> Vec3(first, second, 1.0)
                    Direction.WEST -> Vec3(0.0, first, second)
                    Direction.EAST -> Vec3(1.0, first, second)
                    Direction.DOWN -> return null
                }

                val world = Vec3(pos.x + relative.x, pos.y + relative.y, pos.z + relative.z)
                val bestWorld = best?.let { Vec3(pos.x + it.x, pos.y + it.y, pos.z + it.z) }

                if (bestWorld == null || eyePos.distanceTo(world) < eyePos.distanceTo(bestWorld)) {
                    best = relative
                }
            }
        }

        val hitVec = best ?: return null
        val worldHit = Vec3(pos.x + hitVec.x, pos.y + hitVec.y, pos.z + hitVec.z)

        return BlockHitResult(worldHit, side, pos, false)
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent) {
        clickedBlocks.clear()
        blockCooldowns.clear()
        clickQueue.clear()
        queuedBlocks.clear()
    }
}