package com.blowup.utils

import com.blowup.PeachSoju.mc
import com.blowup.config
import com.blowup.modules.impl.autoroutes.data.WaypointNode
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Rotations
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

enum class SwapResult { SUCCESS, ALREADY_HOLDING, FAIL }

object RouteUtils {

    fun getRealYaw(relativeYaw: Float, room: Room?): Float =
        if (room == null || !DungeonUtils.inDungeons) relativeYaw else normalizeYaw(relativeYaw + room.rotation.toDegrees())

    fun getNodeWorldPosition(node: WaypointNode, room: Room?): Vec3 {
        val blockPos = BlockPos(floor(node.x).toInt(), node.y.toInt(), floor(node.z).toInt())
        val worldPos = if (DungeonUtils.inDungeons && room != null) room.getRealCoords(blockPos) ?: blockPos else blockPos
        return if (node.exact) {
            val fracX = node.x - floor(node.x)
            val fracZ = node.z - floor(node.z)
            Vec3(worldPos.x + fracX, worldPos.y.toDouble(), worldPos.z + fracZ)
        } else Vec3(worldPos.x + 0.5, worldPos.y.toDouble(), worldPos.z + 0.5)
    }

    fun swapToItem(itemName: String): SwapResult {
        val player = mc.player ?: return SwapResult.FAIL
        val inv = player.inventory
        val want = itemName.lowercase()

        val held = inv.getItem(inv.selectedSlot)
        if (!held.isEmpty && held.hoverName.string.lowercase().contains(want)) return SwapResult.ALREADY_HOLDING

        for (i in 0..8) {
            if (i == inv.selectedSlot) continue
            val stack = inv.getItem(i)
            if (!stack.isEmpty && stack.hoverName.string.lowercase().contains(want)) {
                inv.selectedSlot = i
                return SwapResult.SUCCESS
            }
        }
        return SwapResult.FAIL
    }

    fun raytraceToBlock(eyePos: Vec3, targetBlock: BlockPos): BlockHitResult? {
        val level = mc.level ?: return null
        val blockCenter = Vec3(targetBlock.x + 0.5, targetBlock.y + 0.5, targetBlock.z + 0.5)
        val ctx = ClipContext(eyePos, blockCenter, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player)
        val result = level.clip(ctx)
        return if (result.type == HitResult.Type.BLOCK) result as? BlockHitResult else null
    }

    fun debug(msg: String) { if (config.debug()) mc.player?.displayClientMessage(Component.literal("§7[AR] $msg"), false) }
    fun extraDebug(msg: String) { if (config.extraDebug()) mc.player?.displayClientMessage(Component.literal("§7[DEV] $msg"), false) }

    private fun Rotations.toDegrees(): Float = when (this) {
        Rotations.NORTH -> 0f
        Rotations.EAST -> 90f
        Rotations.SOUTH -> 180f
        Rotations.WEST -> 270f
        Rotations.NONE -> 0f
    }

    private fun normalizeYaw(yaw: Float): Float {
        var y = yaw % 360f
        if (y > 180f) y -= 360f
        if (y < -180f) y += 360f
        return y
    }
}
