//package com.peachsoju.modules.impl.dungeon.icefill
//
//import com.odtheking.odin.utils.Color
//import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
//import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRealCoords
//import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
//import com.odtheking.odin.utils.skyblock.dungeon.tiles.Rotations
//import com.peachsoju.PeachSoju.mc
//import com.peachsoju.config
//import com.peachsoju.eventbus.SubscribeEvent
//import com.peachsoju.eventbus.events.RenderEvent
//import com.peachsoju.eventbus.events.RoomEnterEvent
//import com.peachsoju.eventbus.events.WorldEvent
//import com.peachsoju.utils.RouteUtils
//import com.peachsoju.utils.SwapResult
//import com.peachsoju.utils.handlers.RightClickHandler
//import com.peachsoju.utils.handlers.drawLine
//import net.minecraft.core.BlockPos
//import net.minecraft.core.Vec3i
//import net.minecraft.world.level.block.Blocks
//import net.minecraft.world.phys.Vec3
//import kotlin.math.atan2
//import kotlin.math.floor
//
//object IceFillSolver {
//
//    // Current solved path as world coordinates
//    private var currentPath: MutableList<Vec3> = mutableListOf()
//
//    // Current index in the path
//    private var currentIndex: Int = 0
//
//    // State tracking
//    private var isActive: Boolean = false
//    private var lastRoom: Room? = null
//
//    // Server tick tracking
//    private var serverTickCounter: Int = 0
//    private var lastTeleportServerTick: Int = 0
//
//    // Line color for rendering
//    private val pathColor = Color(0, 255, 255, 1f)  // Cyan
//    private val currentSegmentColor = Color(255, 165, 0, 1f)  // Orange for current segment
//
//    /**
//     * Called from MixinConnection when a server tick is detected (ClientboundPingPacket)
//     */
//    fun onServerTick() {
//        if (!config.autoIceFill() || !isActive || currentPath.isEmpty()) return
//        if (DungeonUtils.currentRoomName != "Ice Fill") return
//
//        serverTickCounter++
//
//        val player = mc.player ?: return
//
//        // Update which node we're currently at
//        updateCurrentIndex()
//
//        // Check if we should teleport to next node
//        if (shouldTeleportToNext()) {
//            performTeleport()
//        }
//    }
//
//    @SubscribeEvent
//    fun onRoomEnter(event: RoomEnterEvent) {
//        val room = event.room ?: return
//        if (room.data?.name != "Ice Fill") return
//        if (!config.autoIceFill()) return
//
//        // Don't rescan if we already have a path for this room
//        if (currentPath.isNotEmpty() && lastRoom == room) return
//
//        lastRoom = room
//        scanAllFloors(room)
//    }
//
//    @SubscribeEvent
//    fun onWorldChange(event: WorldEvent) {
//        reset()
//    }
//
//    @SubscribeEvent
//    fun onRender(event: RenderEvent.Extract) {
//        if (!config.autoIceFillShowPath() || currentPath.isEmpty()) return
//        if (DungeonUtils.currentRoomName != "Ice Fill") return
//
//        // Draw the path
//        for (i in 0 until currentPath.size - 1) {
//            val p1 = currentPath[i]
//            val p2 = currentPath[i + 1]
//
//            // Use different color for current/upcoming segment
//            val color = if (i >= currentIndex - 1 && i <= currentIndex) currentSegmentColor else pathColor
//
//            event.drawLine(listOf(p1, p2), color, depth = false, thickness = 3f)
//        }
//    }
//
//    private fun scanAllFloors(room: Room) {
//        currentPath.clear()
//        currentIndex = 0
//        serverTickCounter = 0
//        lastTeleportServerTick = 0
//
//        val rotation = room.rotation
//
//        // Get base position - room relative coords (15, 70, 7)
//        val basePos = room.getRealCoords(BlockPos(15, 70, 7)) ?: return
//        val baseVec = Vec3(basePos.x.toDouble(), basePos.y.toDouble(), basePos.z.toDouble())
//
//        // Floor start positions:
//        // Floor 0: base
//        // Floor 1: base + transform(5, 1, 0)
//        // Floor 2: base + transform(12, 2, 0)
//        val floorStarts = listOf(
//            baseVec,
//            baseVec.add(transformTo(Vec3i(5, 1, 0), rotation)),
//            baseVec.add(transformTo(Vec3i(12, 2, 0), rotation))
//        )
//
//        RouteUtils.debug("§b[IceFill] Starting scan. Base: ${basePos.x}, ${basePos.y}, ${basePos.z}, Rotation: $rotation")
//
//        for (floorIndex in 0..2) {
//            val startPosition = floorStarts[floorIndex]
//            val startTime = System.nanoTime()
//
//            // Try each pattern's representative blocks
//            val representatives = IceFillFloors.representativeBlocks[floorIndex]
//            var matchedPattern = -1
//
//            for (patternIndex in representatives.indices) {
//                val rep = representatives[patternIndex]
//                val airX = rep[0]
//                val airZ = rep[1]
//                val solidX = rep[2]
//                val solidZ = rep[3]
//
//                // Transform the check positions based on rotation
//                val airTransform = transform(airX, airZ, rotation)
//                val solidTransform = transform(solidX, solidZ, rotation)
//
//                val airPos = BlockPos(startPosition.x.toInt(), startPosition.y.toInt(), startPosition.z.toInt())
//                    .offset(airTransform.first, 0, airTransform.second)
//                val solidPos = BlockPos(startPosition.x.toInt(), startPosition.y.toInt(), startPosition.z.toInt())
//                    .offset(solidTransform.first, 0, solidTransform.second)
//
//                if (isAir(airPos) && !isAir(solidPos)) {
//                    matchedPattern = patternIndex
//                    break
//                }
//            }
//
//            if (matchedPattern == -1) {
//                RouteUtils.debug("§c[IceFill] Failed to identify pattern for floor ${floorIndex + 1}")
//                continue
//            }
//
//            val scanTime = (System.nanoTime() - startTime) / 1000000.0
//            RouteUtils.debug("§a[IceFill] Floor ${floorIndex + 1} scan took ${String.format("%.2f", scanTime)}ms, pattern: $matchedPattern")
//
//            // Add all path nodes for this floor
//            val floorPattern = IceFillFloors.floors[floorIndex][matchedPattern]
//            for (node in floorPattern) {
//                val worldPos = startPosition
//                    .add(transformTo(node, rotation))
//                    .add(0.5, 0.1, 0.5)  // Center on block + small y offset
//                currentPath.add(worldPos)
//            }
//        }
//
//        if (currentPath.isNotEmpty()) {
//            // Add stair transition points
//            addStairTransitions(rotation)
//            isActive = true
//            RouteUtils.debug("§a[IceFill] Path solved with ${currentPath.size} nodes")
//        }
//    }
//
//    /**
//     * Transform x,z offsets based on room rotation
//     * Returns Pair(newX, newZ)
//     */
//    private fun transform(x: Int, z: Int, rotation: Rotations): Pair<Int, Int> {
//        return when (rotation) {
//            Rotations.NORTH -> Pair(z, -x)
//            Rotations.WEST -> Pair(-x, -z)
//            Rotations.SOUTH -> Pair(-z, x)
//            Rotations.EAST -> Pair(x, z)
//            Rotations.NONE -> Pair(x, z)
//        }
//    }
//
//    /**
//     * Transform a Vec3i offset to Vec3 based on rotation
//     */
//    private fun transformTo(vec: Vec3i, rotation: Rotations): Vec3 {
//        val (newX, newZ) = transform(vec.x, vec.z, rotation)
//        return Vec3(newX.toDouble(), vec.y.toDouble(), newZ.toDouble())
//    }
//
//    /**
//     * Add intermediate stair points for floor transitions
//     */
//    private fun addStairTransitions(rotation: Rotations) {
//        val updatedPath = mutableListOf<Vec3>()
//
//        var added71 = false
//        var added72 = false
//
//        for (point in currentPath) {
//            // First point at Y ~71.1 (floor 1 start)
//            if (!added71 && point.y > 70.5 && point.y < 71.5) {
//                updatedPath.add(adjustForStair(point, rotation, 70.6))
//                added71 = true
//            }
//            // First point at Y ~72.1 (floor 2 start)
//            else if (!added72 && point.y > 71.5 && point.y < 72.5) {
//                updatedPath.add(adjustForStair(point, rotation, 71.6))
//                added72 = true
//            }
//
//            updatedPath.add(point)
//        }
//
//        currentPath.clear()
//        currentPath.addAll(updatedPath)
//    }
//
//    /**
//     * Create a stair approach point offset slightly in approach direction
//     */
//    private fun adjustForStair(point: Vec3, rotation: Rotations, adjustedY: Double): Vec3 {
//        return when (rotation) {
//            Rotations.NORTH -> Vec3(point.x, adjustedY, point.z + 0.35)
//            Rotations.SOUTH -> Vec3(point.x, adjustedY, point.z - 0.35)
//            Rotations.EAST -> Vec3(point.x - 0.35, adjustedY, point.z)
//            Rotations.WEST -> Vec3(point.x + 0.35, adjustedY, point.z)
//            Rotations.NONE -> Vec3(point.x, adjustedY, point.z)
//        }
//    }
//
//    private fun isAir(pos: BlockPos): Boolean {
//        val level = mc.level ?: return false
//        val state = level.getBlockState(pos)
//        return state.isAir || state.block == Blocks.AIR || state.block == Blocks.CAVE_AIR
//    }
//
//    private fun updateCurrentIndex() {
//        val player = mc.player ?: return
//        val playerX = floor(player.x).toInt()
//        val playerZ = floor(player.z).toInt()
//
//        // Find if we're standing on any path node
//        for (i in currentIndex until minOf(currentIndex + 3, currentPath.size)) {
//            val node = currentPath[i]
//            val nodeX = floor(node.x).toInt()
//            val nodeZ = floor(node.z).toInt()
//
//            // Check if player is on this node (same block X/Z, Y within 1.5 blocks for stair tolerance)
//            val sameX = playerX == nodeX
//            val sameZ = playerZ == nodeZ
//            val closeY = kotlin.math.abs(player.y - node.y) < 1.5
//
//            if (sameX && sameZ && closeY) {
//                currentIndex = i
//                return
//            }
//        }
//    }
//
//    private fun shouldTeleportToNext(): Boolean {
//        val player = mc.player ?: return false
//
//        if (currentIndex >= currentPath.size - 1) return false
//
//        val tickDelay = config.iceFillTickDelay()
//        if (serverTickCounter - lastTeleportServerTick < tickDelay) return false
//
//        if (!player.onGround()) return false
//
//        val currentNode = currentPath[currentIndex]
//        val playerX = floor(player.x).toInt()
//        val playerZ = floor(player.z).toInt()
//        val nodeX = floor(currentNode.x).toInt()
//        val nodeZ = floor(currentNode.z).toInt()
//
//        // Must be on the current block (X/Z match, Y within tolerance for stairs)
//        val onCurrentNode = playerX == nodeX && playerZ == nodeZ
//        val closeY = kotlin.math.abs(player.y - currentNode.y) < 1.5
//
//        return onCurrentNode && closeY
//    }
//
//    private fun performTeleport() {
//        val player = mc.player ?: return
//
//        // Swap to AOTV
//        val swapResult = RouteUtils.swapToItem("Aspect of the Void")
//        if (swapResult == SwapResult.FAIL) {
//            val aoteResult = RouteUtils.swapToItem("Aspect of the End")
//            if (aoteResult == SwapResult.FAIL) {
//                RouteUtils.debug("§c[IceFill] No AOTV or AOTE found!")
//                return
//            }
//        }
//
//        val nextIndex = currentIndex + 1
//        if (nextIndex >= currentPath.size) return
//
//        val nextNode = currentPath[nextIndex]
//        val currentNode = currentPath[currentIndex]
//
//        // Calculate yaw to target
//        val dx = nextNode.x - player.x
//        val dz = nextNode.z - player.z
//        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
//
//        // Pitch 25 for stairs, 45 for normal
//        val isStair = nextNode.y > currentNode.y + 0.3
//        val pitch = if (isStair) 25f else 45f
//
//        // Send packet with the rotation (don't rotate client)
//        RightClickHandler.doPacketInteract(yaw = yaw, pitch = pitch)
//
//        lastTeleportServerTick = serverTickCounter
//        currentIndex = nextIndex
//
//        RouteUtils.debug("§b[IceFill] TP to node $nextIndex: yaw=${"%.1f".format(yaw)}, pitch=$pitch${if (isStair) " (stair)" else ""}")
//    }
//
//    fun reset() {
//        currentPath.clear()
//        currentIndex = 0
//        isActive = false
//        lastRoom = null
//        serverTickCounter = 0
//        lastTeleportServerTick = 0
//    }
//
//    fun isPathActive(): Boolean = isActive && currentPath.isNotEmpty()
//
//    fun getCurrentProgress(): String {
//        if (currentPath.isEmpty()) return "No path"
//        return "${currentIndex + 1}/${currentPath.size}"
//    }
//}