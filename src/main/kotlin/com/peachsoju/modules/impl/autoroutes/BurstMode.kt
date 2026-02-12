
//BIG ty leo <3

package com.peachsoju.modules.impl.autoroutes

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.handlers.RightClickHandler
import com.peachsoju.handlers.SneakHandler
import com.peachsoju.modules.impl.autoroutes.data.WPType
import com.peachsoju.modules.impl.autoroutes.data.WaypointNode
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.SwapResult
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BubbleColumnBlock
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.CarpetBlock
import net.minecraft.world.level.block.ComparatorBlock
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.DryVegetationBlock
import net.minecraft.world.level.block.FireBlock
import net.minecraft.world.level.block.FlowerBlock
import net.minecraft.world.level.block.FlowerPotBlock
import net.minecraft.world.level.block.GrassBlock
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.MushroomBlock
import net.minecraft.world.level.block.NetherPortalBlock
import net.minecraft.world.level.block.NetherWartBlock
import net.minecraft.world.level.block.RailBlock
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.RedstoneTorchBlock
import net.minecraft.world.level.block.RepeaterBlock
import net.minecraft.world.level.block.SaplingBlock
import net.minecraft.world.level.block.SeagrassBlock
import net.minecraft.world.level.block.SkullBlock
import net.minecraft.world.level.block.SmallDripleafBlock
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.SugarCaneBlock
import net.minecraft.world.level.block.TallFlowerBlock
import net.minecraft.world.level.block.TallGrassBlock
import net.minecraft.world.level.block.TallSeagrassBlock
import net.minecraft.world.level.block.TorchBlock
import net.minecraft.world.level.block.TripWireBlock
import net.minecraft.world.level.block.TripWireHookBlock
import net.minecraft.world.level.block.VineBlock
import net.minecraft.world.level.block.WallSkullBlock
import net.minecraft.world.level.block.WebBlock
import net.minecraft.world.level.block.WoolCarpetBlock
import net.minecraft.world.level.block.piston.PistonHeadBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.BitSet
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

object BurstMode {

    val enabled: Boolean get() = config.burstMode()
    fun toggle(): Boolean = config.toggleBurstMode()

    private const val etherwarpDistance = 61.0
    private const val sneakEyeHeight = 1.54
    private const val maxVoxelSteps = 1000

    private var executingBurst = false

    private val validEtherwarpFeetIds: BitSet by lazy {
        val validTypes = setOf(
            ButtonBlock::class, CarpetBlock::class, SkullBlock::class, WallSkullBlock::class, LadderBlock::class,
            SaplingBlock::class, FlowerBlock::class, StemBlock::class, CropBlock::class, RailBlock::class,
            SnowLayerBlock::class, BubbleColumnBlock::class, TripWireBlock::class, TripWireHookBlock::class,
            FireBlock::class, AirBlock::class, TorchBlock::class, FlowerPotBlock::class, TallFlowerBlock::class,
            TallGrassBlock::class, BushBlock::class, SeagrassBlock::class, TallSeagrassBlock::class, SugarCaneBlock::class,
            LiquidBlock::class, VineBlock::class, MushroomBlock::class, GrassBlock::class, PistonHeadBlock::class,
            WoolCarpetBlock::class, WebBlock::class, DryVegetationBlock::class, SmallDripleafBlock::class, LeverBlock::class,
            NetherWartBlock::class, NetherPortalBlock::class, RedStoneWireBlock::class, ComparatorBlock::class,
            RedstoneTorchBlock::class, RepeaterBlock::class
        )

        BitSet().apply {
            BuiltInRegistries.BLOCK.forEach { block ->
                if (validTypes.any { it.isInstance(block) }) set(Block.getId(block.defaultBlockState()))
            }
        }
    }

    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?, val state: BlockState? = null) {
        companion object { val none = EtherPos(false, null, null) }
    }

    data class BurstChain(val nodes: List<WaypointNode>, val indices: List<Int>)

    fun shouldBurst(node: WaypointNode): Boolean =
        enabled && !executingBurst && node.type == WPType.ETHER && node.awaitSecret <= 0 && !node.awaitBat && node.delay <= 0

    fun wasBurstAttempted(): Boolean = !executingBurst

    fun findBurstChain(
        startNode: WaypointNode,
        startIndex: Int,
        allNodes: List<WaypointNode>,
        room: Room?,
        skipFirstNodeChecks: Boolean = false
    ): BurstChain {
        if (!enabled) return BurstChain(listOf(startNode), listOf(startIndex))

        RouteUtils.extraDebug("§e[Burst] Starting chain from node #$startIndex (skipFirstChecks=$skipFirstNodeChecks)")

        val chain = mutableListOf<WaypointNode>()
        val indices = mutableListOf<Int>()
        val visited = mutableSetOf<Int>()

        var currentNode = startNode
        var currentIndex = startIndex

        val maxIterations = allNodes.size
        for (iteration in 0 until allNodes.size) {
            if (!visited.add(currentIndex)) { RouteUtils.extraDebug("§c[Burst] Stop: node #$currentIndex already visited (loop detected)"); break }
            val isFirstNode = chain.isEmpty()

            if (currentNode.type != WPType.ETHER) { RouteUtils.extraDebug("§e[Burst] Stop: non-ETHER node (type=${currentNode.type})"); break }
            if (!(isFirstNode && skipFirstNodeChecks) && currentNode.awaitSecret > 0) { RouteUtils.extraDebug("§e[Burst] Stop: await secret (${currentNode.awaitSecret})"); break }
            if (!(isFirstNode && skipFirstNodeChecks) && currentNode.awaitBat) { RouteUtils.extraDebug("§e[Burst] Stop: await bat"); break }
            if (!(isFirstNode && skipFirstNodeChecks) && currentNode.delay > 0) { RouteUtils.extraDebug("§e[Burst] Stop: delay (${currentNode.delay} ticks)"); break }

            chain.add(currentNode); indices.add(currentIndex)
            RouteUtils.extraDebug("§a[Burst] Added node #$currentIndex to chain (total: ${chain.size})")

            val nodeWorldPos = RouteUtils.getNodeWorldPosition(currentNode, room)
            val landingPos = predictEtherwarpLanding(currentNode, nodeWorldPos, room) ?: run {
                RouteUtils.extraDebug("§c[Burst] Stop: couldn't predict landing"); break
            }

            RouteUtils.extraDebug("§b[Burst] Predicted landing block: ${landingPos.x}, ${landingPos.y}, ${landingPos.z}")

            val next = findNodeAtPosition(landingPos, allNodes, room, visited) ?: run {
                RouteUtils.extraDebug("§e[Burst] Stop: no matching node found at landing position"); break
            }

            RouteUtils.extraDebug("§e[Burst] findNodeAtPosition returned index=${next.second}, node.yaw=${next.first.yaw}")
            RouteUtils.extraDebug("§a[Burst] Found next node #${next.second} at landing")

            currentNode = next.first
            currentIndex = next.second
        }

        RouteUtils.extraDebug("§a[Burst] Chain complete: ${chain.size} node(s)")
        return BurstChain(chain, indices)
    }

    fun executeBurst(startNode: WaypointNode, startIndex: Int, allNodes: List<WaypointNode>, room: Room?) {
        executingBurst = true
        val chain = findBurstChain(startNode, startIndex, allNodes, room)
        if (chain.nodes.size <= 1) { executingBurst = false; return }

        RouteUtils.extraDebug("§6§l[Burst] Executing ${chain.nodes.size} nodes")
        val now = System.currentTimeMillis()
        chain.indices.forEach { idx ->
            RouteUtils.extraDebug("§c[Burst Cooldown] Setting cooldown for nodes: ${chain.indices}")
            RouteState.nodeCooldowns[idx] = now
        }

        val lastIndex = chain.indices.last()
        RouteState.waitingForTeleport = true
        RouteState.awaitingTeleportNodeIndex = lastIndex

        val clicks = chain.nodes.map { RouteUtils.getRealYaw(it.yaw, room) to it.pitch }

        SneakHandler.setSneak(true) { doBurstClicks(clicks) }
    }

    fun executeBurstChain(chain: BurstChain, room: Room?) {
        if (chain.nodes.isEmpty()) return
        executingBurst = true

        RouteUtils.extraDebug("§6§l[Burst] Executing ${chain.nodes.size} nodes")
        val now = System.currentTimeMillis()
        chain.indices.forEach { idx ->
            RouteUtils.extraDebug("§c[Burst Cooldown] Setting cooldown for nodes: ${chain.indices}")
            RouteState.nodeCooldowns[idx] = now
        }

        val lastIndex = chain.indices.last()
        RouteState.waitingForTeleport = true
        RouteState.awaitingTeleportNodeIndex = lastIndex

        val clicks = chain.nodes.map {
            val realYaw = RouteUtils.getRealYaw(it.yaw, room)
            RouteUtils.extraDebug("§e[Burst] Building click from node yaw=${it.yaw}, realYaw=$realYaw, pitch=${it.pitch}")
            realYaw to it.pitch
        }

        RouteUtils.extraDebug("§e[Burst] About to call setSneak. isSneaking=${SneakHandler.isSneaking()}")
        RouteUtils.extraDebug("§6§l[Burst] Executing ${chain.nodes.size} nodes")
        RouteUtils.extraDebug("§6[Burst] Chain indices: ${chain.indices}")

        SneakHandler.setSneak(true) {
            RouteUtils.extraDebug("§a[Burst] Sneak callback fired")
            doBurstClicks(clicks)
        }

        RouteUtils.extraDebug("§e[Burst] setSneak called, waiting for callback...")
    }

    fun testPredictionFromPlayer(): String {
        val player = mc.player ?: return "§cNo player"
        val startPos = Vec3(player.x, player.y + sneakEyeHeight, player.z)
        val endPos = startPos.add(getLookVector(player.yRot, player.xRot).scale(etherwarpDistance))
        val result = getEtherPos(startPos, endPos)
        return if (result.succeeded && result.pos != null) "§aPredicted landing: ${result.pos.x}, ${result.pos.y}, ${result.pos.z}" else "§cNo valid landing found"
    }

    fun testChainFromNode(nodeIndex: Int): String {
        val room = DungeonUtils.currentRoom
        val nodes = RouteState.nodeList
        if (nodes.isEmpty()) return "§cNo nodes loaded for current room"
        if (nodeIndex !in nodes.indices) return "§cInvalid index: $nodeIndex (have ${nodes.size} nodes)"

        val chain = findBurstChain(nodes[nodeIndex], nodeIndex, nodes, room)
        val sb = StringBuilder()
        sb.append("§e§lBurst Chain Test from Node #$nodeIndex\n")
        sb.append("§7Chain length: §a${chain.nodes.size}\n")
        sb.append("§7Nodes: §f${chain.indices.joinToString(" → ")}\n")
        for ((i, idx) in chain.indices.withIndex()) {
            val node = chain.nodes[i]
            sb.append("§8  [$i] §7#$idx: ${node.type} at (${node.x.toInt()}, ${node.y.toInt()}, ${node.z.toInt()})\n")
        }
        return sb.toString().trimEnd()
    }

    fun getEtherwarpPositionFromPlayer(): BlockPos? {
        val player = mc.player ?: return null
        val startPos = Vec3(player.x, player.y + sneakEyeHeight, player.z)
        val endPos = startPos.add(getLookVector(player.yRot, player.xRot).scale(etherwarpDistance))
        return getEtherPos(startPos, endPos).pos
    }

    private fun doBurstClicks(clicks: List<Pair<Float, Float>>) {
        RouteUtils.extraDebug("§6[Burst] doBurstClicks() entered with ${clicks.size} clicks")
        if (RouteUtils.swapToItem("Aspect of the Void") == SwapResult.FAIL) {
            RouteUtils.extraDebug("§c[Burst] Failed to swap to AOTV")
            SneakHandler.releaseSneak()
            RouteState.unlock()
            RouteState.waitingForTeleport = false
            executingBurst = false
            return
        }

        RouteUtils.extraDebug("§a[Burst] Sending ${clicks.size} right-click packets")
        for ((i, click) in clicks.withIndex()) {
            val (yaw, pitch) = click
            RouteUtils.extraDebug("§7[Burst] Click #$i: yaw=${formatAngle(yaw)}, pitch=${formatAngle(pitch)}")
            RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, yaw, pitch)
        }

        RouteUtils.extraDebug("§a[Burst] All packets sent! Waiting for teleport...")
        executingBurst = false
    }

    private fun predictEtherwarpLanding(node: WaypointNode, nodeWorldPos: Vec3, room: Room?): BlockPos? {
        RouteUtils.extraDebug("§7[Burst] node.x=${node.x}, node.z=${node.z}")
        RouteUtils.extraDebug("§7[Burst] nodeWorldPos.x=${nodeWorldPos.x}, nodeWorldPos.z=${nodeWorldPos.z}")

        val startPos = Vec3(nodeWorldPos.x, nodeWorldPos.y + 1.0 + sneakEyeHeight, nodeWorldPos.z)
        val realYaw = RouteUtils.getRealYaw(node.yaw, room)
        val pitch = node.pitch

        RouteUtils.extraDebug("§7[Burst] Raycast from (${formatCoord(startPos.x)}, ${formatCoord(startPos.y)}, ${formatCoord(startPos.z)})")
        RouteUtils.extraDebug("§7[Burst] Using yaw=${formatAngle(realYaw)}, pitch=${formatAngle(pitch)}")

        val lookVec = getLookVector(realYaw, pitch)
        RouteUtils.extraDebug("§7[Burst] Look vector: (${formatCoord(lookVec.x)}, ${formatCoord(lookVec.y)}, ${formatCoord(lookVec.z)})")

        return getEtherPos(startPos, startPos.add(lookVec.scale(etherwarpDistance))).pos
    }

    fun getEtherPos(start: Vec3, end: Vec3): EtherPos {
        val level = mc.level ?: return EtherPos.none

        val x0 = start.x; val y0 = start.y; val z0 = start.z
        val x1 = end.x; val y1 = end.y; val z1 = end.z

        var x = floor(x0); var y = floor(y0); var z = floor(z0)
        val endX = floor(x1); val endY = floor(y1); val endZ = floor(z1)

        val dirX = x1 - x0; val dirY = y1 - y0; val dirZ = z1 - z0
        val stepX = sign(dirX).toInt(); val stepY = sign(dirY).toInt(); val stepZ = sign(dirZ).toInt()

        val invDirX = if (dirX != 0.0) 1.0 / dirX else Double.MAX_VALUE
        val invDirY = if (dirY != 0.0) 1.0 / dirY else Double.MAX_VALUE
        val invDirZ = if (dirZ != 0.0) 1.0 / dirZ else Double.MAX_VALUE

        val tDeltaX = abs(invDirX * stepX)
        val tDeltaY = abs(invDirY * stepY)
        val tDeltaZ = abs(invDirZ * stepZ)

        var tMaxX = abs((x + max(stepX, 0) - x0) * invDirX)
        var tMaxY = abs((y + max(stepY, 0) - y0) * invDirY)
        var tMaxZ = abs((z + max(stepZ, 0) - z0) * invDirZ)

        repeat(maxVoxelSteps) {
            val blockPos = BlockPos(x.toInt(), y.toInt(), z.toInt())
            val chunk = level.getChunk(SectionPos.blockToSectionCoord(blockPos.x), SectionPos.blockToSectionCoord(blockPos.z)) ?: return EtherPos.none

            val currentBlock = chunk.getBlockState(blockPos)
            if (!validEtherwarpFeetIds.get(Block.getId(currentBlock))) {
                val feetState = chunk.getBlockState(BlockPos(blockPos.x, blockPos.y + 1, blockPos.z))
                if (validEtherwarpFeetIds.get(Block.getId(feetState))) {
                    val headState = chunk.getBlockState(BlockPos(blockPos.x, blockPos.y + 2, blockPos.z))
                    if (validEtherwarpFeetIds.get(Block.getId(headState))) {
                        RouteUtils.extraDebug("§a[Burst] Valid landing at ${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
                        return EtherPos(true, blockPos, currentBlock)
                    }
                }
            }

            if (x.toInt() == endX.toInt() && y.toInt() == endY.toInt() && z.toInt() == endZ.toInt()) {
                RouteUtils.extraDebug("§c[Burst] Reached end of ray without finding landing")
                return EtherPos.none
            }

            when {
                tMaxX <= tMaxY && tMaxX <= tMaxZ -> { tMaxX += tDeltaX; x += stepX }
                tMaxY <= tMaxZ -> { tMaxY += tDeltaY; y += stepY }
                else -> { tMaxZ += tDeltaZ; z += stepZ }
            }
        }

        RouteUtils.extraDebug("§c[Burst] Exceeded $maxVoxelSteps iterations")
        return EtherPos.none
    }

    private fun findNodeAtPosition(targetWorldPos: BlockPos, allNodes: List<WaypointNode>, room: Room?, excludeIndices: Set<Int>): Pair<WaypointNode, Int>? {
        val targetRelative = if (DungeonUtils.inDungeons && room != null) room.getRelativeCoords(targetWorldPos) else targetWorldPos
        RouteUtils.extraDebug("§7[Burst] Searching for node at relative: (${targetRelative.x}, ${targetRelative.y}, ${targetRelative.z})")

        for ((index, node) in allNodes.withIndex()) {
            if (index in excludeIndices) continue
            val nodeX = floor(node.x).toInt()
            val nodeY = floor(node.y).toInt()
            val nodeZ = floor(node.z).toInt()
            val dx = abs(nodeX - targetRelative.x)
            val dy = abs(nodeY - targetRelative.y)
            val dz = abs(nodeZ - targetRelative.z)
            if (dx == 0 && dz == 0 && dy <= 1) {
                RouteUtils.extraDebug("§a[Burst] Exact match! Node #$index at ($nodeX, $nodeY, $nodeZ)")
                return node to index
            }
        }

        RouteUtils.extraDebug("§7[Burst] No matching node found")
        return null
    }

    fun getLookVector(yaw: Float, pitch: Float): Vec3 {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        val xz = cos(pitchRad)
        return Vec3(-xz * sin(yawRad), -sin(pitchRad), xz * cos(yawRad))
    }

    fun getEtherwarpPosFromVec3(pos: Vec3): BlockPos? {
        val player = mc.player ?: return null
        val startPos = pos
        val lookVec = BurstMode.getLookVector(player.yRot, player.xRot)
        val endPos = startPos.add(lookVec.scale(61.0))
        return BurstMode.getEtherPos(startPos, endPos).pos
    }

    private fun formatCoord(value: Double): String = "%.2f".format(value)
    private fun formatAngle(value: Float): String = "%.1f".format(value)
}
