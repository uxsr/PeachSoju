package com.peachsoju.modules.impl.dungeon.autop5

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.handlers.JumpHandler
import com.peachsoju.utils.handlers.WalkHandler
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A* Pathfinder with obstacle avoidance for P5 dragon navigation.
 */
object AutoP5Pathfinder {

    private var currentPath: List<BlockPos>? = null
    private var currentPathIndex = 0
    private var isPathfinding = false
    private var targetPosition: Vec3? = null
    private var onArrivalCallback: (() -> Unit)? = null

    // Pathfinding settings
    private const val MAX_ITERATIONS = 2000
    private const val ARRIVAL_DISTANCE = 1.5
    private const val WAYPOINT_DISTANCE = 1.0

    // State
    private var lastJumpTime = 0L
    private var stuckTicks = 0
    private var lastPosition: Vec3? = null

    /**
     * Start pathfinding to a target position.
     */
    fun pathfindTo(target: Vec3, onArrival: (() -> Unit)? = null) {
        val player = mc.player ?: return
        val startPos = player.blockPosition()
        val endPos = BlockPos(target.x.toInt(), target.y.toInt(), target.z.toInt())

        RouteUtils.debug("§b[Pathfinder] Starting path from $startPos to $endPos")

        val path = findPath(startPos, endPos)
        if (path == null || path.isEmpty()) {
            RouteUtils.debug("§c[Pathfinder] No path found! Walking directly.")
            // Fall back to direct walking
            currentPath = listOf(endPos)
            currentPathIndex = 0
            targetPosition = target
            isPathfinding = true
            onArrivalCallback = onArrival
            AutoP5State.isPathfinding = true
            return
        }

        currentPath = path
        currentPathIndex = 0
        targetPosition = target
        isPathfinding = true
        onArrivalCallback = onArrival
        stuckTicks = 0
        lastPosition = null
        AutoP5State.isPathfinding = true

        RouteUtils.debug("§a[Pathfinder] Path found with ${path.size} waypoints")
        // Show first 5 waypoints for debugging
        path.take(5).forEachIndexed { i, wp ->
            RouteUtils.debug("§7  [$i] (${wp.x}, ${wp.y}, ${wp.z})")
        }
    }

    /**
     * Stop pathfinding and release controls.
     */
    fun stop() {
        if (isPathfinding) {
            WalkHandler.stopWalk()
            JumpHandler.cancel()
            RouteUtils.debug("§e[Pathfinder] Stopped")
        }

        currentPath = null
        currentPathIndex = 0
        isPathfinding = false
        targetPosition = null
        onArrivalCallback = null
        stuckTicks = 0
        lastPosition = null
        AutoP5State.isPathfinding = false
    }

    /**
     * Check if currently pathfinding.
     */
    fun isActive(): Boolean = isPathfinding

    /**
     * Get current path for rendering.
     */
    fun getCurrentPath(): List<BlockPos>? = currentPath

    /**
     * Get current path index.
     */
    fun getCurrentIndex(): Int = currentPathIndex

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!isPathfinding) return

        val player = mc.player ?: return
        val path = currentPath ?: return
        val target = targetPosition ?: return

        // Check if arrived at final destination
        val distToTarget = horizontalDistance(player.position(), target)
        if (distToTarget < ARRIVAL_DISTANCE) {
            RouteUtils.debug("§a[Pathfinder] Arrived at destination!")
            val callback = onArrivalCallback
            stop()
            callback?.invoke()
            return
        }

        // Check for stuck detection
        val currentPos = player.position()
        lastPosition?.let { last ->
            if (last.distanceTo(currentPos) < 0.05) {
                stuckTicks++
                if (stuckTicks > 30) {
                    RouteUtils.debug("§c[Pathfinder] Stuck detected, attempting recovery")
                    handleStuck()
                    stuckTicks = 0
                }
            } else {
                stuckTicks = 0
            }
        }
        lastPosition = currentPos

        // Get current waypoint
        if (currentPathIndex >= path.size) {
            // Reached end of path but not at target, walk directly
            walkTowards(target)
            return
        }

        val waypoint = path[currentPathIndex]
        val waypointVec = Vec3(waypoint.x + 0.5, waypoint.y.toDouble(), waypoint.z + 0.5)
        val distToWaypoint = horizontalDistance(player.position(), waypointVec)

        // Check if reached current waypoint
        if (distToWaypoint < WAYPOINT_DISTANCE) {
            currentPathIndex++
            if (currentPathIndex < path.size) {
                val nextWp = path[currentPathIndex]
                RouteUtils.debug("§7[Path] Advanced to waypoint $currentPathIndex/${path.size} -> (${nextWp.x}, ${nextWp.y}, ${nextWp.z})")
            }
            return
        }

        // Jump when waypoint is higher than player
        val nextY = waypoint.y.toDouble()
        val playerY = player.position().y
        val heightDiff = nextY - playerY
        val onGround = player.onGround()

        // Debug every 20 ticks to avoid spam
        if (System.currentTimeMillis() % 1000 < 50) {
            RouteUtils.debug("§7[Path] waypointY=${String.format("%.2f", nextY)} playerY=${String.format("%.2f", playerY)} diff=${String.format("%.2f", heightDiff)} onGround=$onGround")
        }

        if (heightDiff > 0.9 && onGround) {
            val now = System.currentTimeMillis()
            if (now - lastJumpTime > 800) {
                RouteUtils.debug("§e[Pathfinder] JUMPING! heightDiff=${String.format("%.2f", heightDiff)} waypoint=$waypoint")
                JumpHandler.forceJump()
                lastJumpTime = now
            }
        }

        // Walk towards waypoint
        walkTowards(waypointVec)
    }

    private fun walkTowards(target: Vec3) {
        val player = mc.player ?: return
        val dx = target.x - player.x
        val dz = target.z - player.z
        val yaw = Math.toDegrees(kotlin.math.atan2(-dx, dz)).toFloat()

        // Use current pitch or slight downward for walking
        val pitch = 0f

        if (!WalkHandler.isWalking()) {
            WalkHandler.startWalk(yaw, pitch)
        } else {
            // Update rotation while walking
            player.yRot = yaw
            player.xRot = pitch
        }
    }

    private fun handleStuck() {
        // Just recalculate path - no jumping needed in P5
        targetPosition?.let { target ->
            val callback = onArrivalCallback
            stop()
            pathfindTo(target, callback)
        }
    }

    private fun horizontalDistance(a: Vec3, b: Vec3): Double {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return sqrt(dx * dx + dz * dz)
    }

    // ==================== A* PATHFINDING ====================

    private data class PathNode(
        val pos: BlockPos,
        val parent: PathNode?,
        val gCost: Double,  // Cost from start
        val hCost: Double   // Heuristic to end
    ) : Comparable<PathNode> {
        val fCost: Double get() = gCost + hCost

        override fun compareTo(other: PathNode): Int = fCost.compareTo(other.fCost)
    }

    /**
     * Find path using A* algorithm.
     */
    private fun findPath(start: BlockPos, end: BlockPos): List<BlockPos>? {
        val level = mc.level ?: return null

        val openSet = PriorityQueue<PathNode>()
        val closedSet = mutableSetOf<BlockPos>()
        val gCosts = mutableMapOf<BlockPos, Double>()

        val startNode = PathNode(start, null, 0.0, heuristic(start, end))
        openSet.add(startNode)
        gCosts[start] = 0.0

        var iterations = 0

        while (openSet.isNotEmpty() && iterations < MAX_ITERATIONS) {
            iterations++

            val current = openSet.poll()

            // Check if reached end (allow some Y tolerance)
            if (current.pos.x == end.x && current.pos.z == end.z &&
                abs(current.pos.y - end.y) <= 3) {
                return reconstructPath(current)
            }

            if (current.pos in closedSet) continue
            closedSet.add(current.pos)

            // Explore neighbors
            for (neighbor in getNeighbors(current.pos)) {
                if (neighbor in closedSet) continue
                if (!isWalkable(neighbor)) continue

                val moveCost = getMoveCost(current.pos, neighbor)
                val newGCost = current.gCost + moveCost

                val existingGCost = gCosts[neighbor]
                if (existingGCost != null && newGCost >= existingGCost) continue

                gCosts[neighbor] = newGCost
                val node = PathNode(neighbor, current, newGCost, heuristic(neighbor, end))
                openSet.add(node)
            }
        }

        RouteUtils.debug("§c[Pathfinder] A* exhausted after $iterations iterations")
        return null
    }

    private fun reconstructPath(endNode: PathNode): List<BlockPos> {
        val path = mutableListOf<BlockPos>()
        var current: PathNode? = endNode

        while (current != null) {
            path.add(current.pos)
            current = current.parent
        }

        // Simplify path - remove unnecessary waypoints
        return simplifyPath(path.reversed())
    }

    private fun simplifyPath(path: List<BlockPos>): List<BlockPos> {
        if (path.size <= 2) return path

        val simplified = mutableListOf(path.first())
        var i = 0

        while (i < path.size - 1) {
            var furthest = i + 1
            // Find furthest point we can walk to directly
            for (j in i + 2 until path.size) {
                if (hasLineOfSight(path[i], path[j])) {
                    furthest = j
                }
            }
            simplified.add(path[furthest])
            i = furthest
        }

        return simplified
    }

    private fun hasLineOfSight(from: BlockPos, to: BlockPos): Boolean {
        val level = mc.level ?: return false

        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z
        val steps = maxOf(abs(dx), abs(dy), abs(dz))

        if (steps == 0) return true

        for (i in 1 until steps) {
            val t = i.toDouble() / steps
            val checkPos = BlockPos(
                (from.x + dx * t).toInt(),
                (from.y + dy * t).toInt(),
                (from.z + dz * t).toInt()
            )
            if (!isWalkable(checkPos)) return false
        }

        return true
    }

    private fun heuristic(from: BlockPos, to: BlockPos): Double {
        val dx = abs(from.x - to.x).toDouble()
        val dy = abs(from.y - to.y).toDouble()
        val dz = abs(from.z - to.z).toDouble()
        // 3D Manhattan with slight weight on Y
        return dx + dz + dy * 2.0
    }

    private fun getMoveCost(from: BlockPos, to: BlockPos): Double {
        val dx = abs(from.x - to.x)
        val dy = to.y - from.y
        val dz = abs(from.z - to.z)

        var cost = if (dx + dz == 2) 1.414 else 1.0

        // Penalize going up (jumping)
        if (dy > 0) cost += 1.0 * dy

        // Slight penalty for going down
        if (dy < 0) cost += 0.2 * abs(dy)

        return cost
    }

    private fun getNeighbors(pos: BlockPos): List<BlockPos> {
        val neighbors = mutableListOf<BlockPos>()

        // Cardinal directions first (preferred)
        neighbors.add(pos.north())
        neighbors.add(pos.south())
        neighbors.add(pos.east())
        neighbors.add(pos.west())

        // Diagonals
        neighbors.add(pos.north().east())
        neighbors.add(pos.north().west())
        neighbors.add(pos.south().east())
        neighbors.add(pos.south().west())

        // Up variations (for jumping)
        for (n in neighbors.toList()) {
            neighbors.add(n.above())
        }

        // Down variations (for dropping)
        neighbors.add(pos.below())
        neighbors.add(pos.north().below())
        neighbors.add(pos.south().below())
        neighbors.add(pos.east().below())
        neighbors.add(pos.west().below())

        return neighbors
    }

    /**
     * Check if a position is walkable (has solid ground and air for player).
     */
    private fun isWalkable(pos: BlockPos): Boolean {
        val level = mc.level ?: return false

        // Check ground (block below must be solid)
        val groundState = level.getBlockState(pos.below())
        if (!isSolidGround(groundState)) {
            // Check two blocks below for stairs/slabs
            val groundState2 = level.getBlockState(pos.below(2))
            if (!isSolidGround(groundState2)) return false
        }

        // Check player space (2 blocks of air)
        val feetState = level.getBlockState(pos)
        val headState = level.getBlockState(pos.above())

        if (!isPassable(feetState)) return false
        if (!isPassable(headState)) return false

        return true
    }

    private fun isSolidGround(state: BlockState): Boolean {
        if (state.isAir) return false
        val block = state.block

        // Common non-solid blocks to avoid
        if (block == Blocks.WATER || block == Blocks.LAVA) return false
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) return false

        return state.isSolid
    }

    private fun isPassable(state: BlockState): Boolean {
        if (state.isAir) return true

        val block = state.block

        // Passable blocks
        if (block == Blocks.TALL_GRASS) return true
        if (block == Blocks.SHORT_GRASS) return true
        if (block == Blocks.FERN) return true
        if (block == Blocks.LARGE_FERN) return true

        return !state.isSolid
    }

    fun reset() {
        stop()
    }
}