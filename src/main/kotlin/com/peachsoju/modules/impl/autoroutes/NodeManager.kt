
//ty leo <3

package com.peachsoju.modules.impl.autoroutes

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.handlers.FileHandler
import com.peachsoju.handlers.drawAnimatedDashedLine
import com.peachsoju.handlers.drawCornerBox
import com.peachsoju.handlers.drawDashedWireBox
import com.peachsoju.handlers.drawDiamond
import com.peachsoju.handlers.drawFilledBox
import com.peachsoju.handlers.drawPulseBox
import com.peachsoju.handlers.drawPulseInfillInvertedPyramid
import com.peachsoju.handlers.drawWireFrameBox
import com.peachsoju.handlers.drawXBox
import com.peachsoju.modules.impl.autoroutes.data.WPType
import com.peachsoju.modules.impl.autoroutes.data.WaypointNode
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.RouteUtils.debug
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Vec2
import com.odtheking.odin.utils.getBlockBounds
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import com.odtheking.odin.utils.skyblock.dungeon.ScanUtils
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Rotations
import com.peachsoju.handlers.drawText
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.floor

object  NodeManager {

    val enabled: Boolean get() = config.waypointRendering()
    val editing: Boolean get() = config.waypointEditing()
    val renderOnlyStartNodes: Boolean get() = config.renderOnlyStartNodes()
    val showLines: Boolean get() = config.showLines()
    var simulating: String? = null

    private const val dashLength = 0.8
    private const val gapLength = 0.4
    private const val animationSpeed = 0.01
    private const val removeRadius = 3.5

    private var fullBlock = false
    private var loaded = false
    private var lineAnimationOffset = 0.0
    private var cachedLineSegments: List<Triple<Vec3, Vec3, Color>>? = null
    private var lastNodeListHash = 0
    private var lastRoomKey: String? = null
    private var lastRemovedNode: WaypointNode? = null
    private var lastRemovedIndex = -1
    private var lastRemovedRoom: String? = null

    private val scannedRooms = mutableMapOf<String, Room>()
    private var lastScanTick = 0
    private const val SCAN_INTERVAL = 20
    private const val ROOM_SIZE = 32
    private const val START = -185

    private val waypoints = mutableMapOf<String, MutableList<WaypointNode>>()

    fun toggle(): Boolean = config.toggleWaypointRendering()
    fun reloadFromDisk() {
        load()
        loaded = true
        debug("Reloaded ${waypoints.size} rooms, ${waypoints.values.sumOf { it.size }} waypoints")
    }

    fun clearCurrentRoom() {
        val key = getCurrentKey() ?: run { debug("No room/area detected"); return }
        waypoints[key]?.clear()
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
        debug("Cleared all waypoints in $key")
    }

    fun getWaypointsForRoom(roomKey: String): List<WaypointNode>? = waypoints[roomKey]?.toList()
    fun getCurrentRoomWaypoints(): List<WaypointNode>? = getCurrentKey()?.let(::getWaypointsForRoom)
    fun getCurrentRoomKey(): String? = getCurrentKey()
    fun getNodeCount(): Int = waypoints.values.sumOf { it.size }
    fun getRoomCount(): Int = waypoints.size

    fun removeClosest(): String {
        val player = mc.player ?: return "§cNo player"
        val room = DungeonUtils.currentRoom
        val key = if (DungeonUtils.inDungeons) room?.data?.name else LocationUtils.currentArea?.toString()
        if (key == null) return "§cCouldn't determine current room"
        val nodes = getWaypointsForRoom(key) ?: return "§cNo nodes in this room"
        if (nodes.isEmpty()) return "§cNo nodes in this room"

        val playerPos = player.position()
        var closestNode: WaypointNode? = null
        var closestIndex = -1
        var closestDistance = Double.MAX_VALUE

        for ((idx, node) in nodes.withIndex()) {
            val nodeWorldPos = RouteUtils.getNodeWorldPosition(node, room)
            val dist = playerPos.distanceTo(nodeWorldPos)
            if (dist <= removeRadius && dist < closestDistance) { closestNode = node; closestIndex = idx; closestDistance = dist }
        }

        if (closestNode == null || closestIndex == -1) return "§cNo node within $removeRadius blocks"

        lastRemovedNode = closestNode
        lastRemovedIndex = closestIndex
        lastRemovedRoom = key

        val mutableNodes = nodes.toMutableList()
        mutableNodes.removeAt(closestIndex)
        saveWaypointsForRoom(key, mutableNodes)

        return "§aRemoved node #$closestIndex (${closestNode.type}) at ${"%.1f".format(closestDistance)} blocks away"
    }

    fun undoRemove(): String {
        val node = lastRemovedNode ?: return "§cNothing to undo"
        val roomKey = lastRemovedRoom ?: return "§cNothing to undo"
        val nodes = getWaypointsForRoom(roomKey)?.toMutableList() ?: mutableListOf()
        val insertIndex = if (lastRemovedIndex <= nodes.size) lastRemovedIndex else nodes.size
        nodes.add(insertIndex, node)
        saveWaypointsForRoom(roomKey, nodes)

        val restoredType = node.type
        lastRemovedNode = null
        lastRemovedIndex = -1
        lastRemovedRoom = null

        return "§aRestored node #$insertIndex ($restoredType)"
    }

    fun addFromCommand(rawArgs: String): String {
        val player = mc.player ?: return "§c[AR] No player"
        val level = mc.level ?: return "§c[AR] No world"
        val room = DungeonUtils.currentRoom
        val key = getCurrentKey(room) ?: return "§c[AR] Room/area not ready"

        val parsed = parseAddArgs(rawArgs) ?: return """
            §c[AR] Usage: /ar add <type> [modifiers...]
            §7Types: §fether, aotv, hype, superboom, await, useitem, nop
            §7Modifiers: §fchained, exact, stop, center, await:N, awaitbat, delay:N, item:NAME
        """.trimIndent()

        val (type, modifiers) = parsed
        val yaw = player.yRot
        val pitch = player.xRot

        val feet = player.blockPosition()
        val floorBlock = if (level.getBlockState(feet).block is AirBlock) feet.below() else feet
        val fracX = player.x - floor(player.x)
        val fracZ = player.z - floor(player.z)

        val targetBlock = if (type == WPType.SUPERBOOM) getTargetBlock(player, room) else null
        if (type == WPType.SUPERBOOM && targetBlock == null) return "§c[AR] Superboom requires looking at a block"

        val node = makeNode(floorBlock, fracX, fracZ, room, type, yaw, pitch, modifiers, targetBlock)
        val list = waypoints.getOrPut(key) { mutableListOf() }

        val existingIndex = list.indexOfFirst { it.type == node.type && it.x == node.x && it.y == node.y && it.z == node.z }
        if (existingIndex >= 0) {
            list.removeAt(existingIndex)
            save()
            if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
            return "§7[AR] Removed §f${type.name.lowercase()} §7at index $existingIndex (room=§f$key§7)"
        }

        list.add(node)
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()

        val index = list.size - 1
        val modStr = buildModifierString(node)
        return "§7[AR] Added §f#$index ${type.name.lowercase()}$modStr §7(room=§f$key§7)"
    }

    fun removeByIndex(index: Int): String {
        val key = getCurrentKey() ?: return "§c[AR] Room/area not ready"
        val list = waypoints[key] ?: return "§c[AR] No waypoints in this room"
        if (index !in 0 until list.size) return "§c[AR] Invalid index. Valid: 0-${list.size - 1}"
        val removed = list.removeAt(index)
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
        return "§7[AR] Removed §f#$index ${removed.type.name.lowercase()}"
    }

    fun listNodes(): String {
        val key = getCurrentKey() ?: return "§c[AR] Room/area not ready"
        val list = waypoints[key] ?: return "§7[AR] No waypoints in §f$key"
        if (list.isEmpty()) return "§7[AR] No waypoints in §f$key"

        val sb = StringBuilder("§7[AR] Waypoints in §f$key§7:\n")
        list.forEachIndexed { i, node ->
            val color = colorCodeFor(node.type)
            val chainedStr = if (node.chained) " §8[chained]" else ""
            val awaitStr = if (node.awaitSecret > 0) " §e[await:${node.awaitSecret}]" else ""
            sb.append("  §f#$i $color${node.type.name}§7 yaw=${"%.1f".format(node.yaw)}$chainedStr$awaitStr\n")
        }
        return sb.toString().trimEnd()
    }

    fun setNodeModifier(index: Int, modifier: String, value: String?): String {
        val key = getCurrentKey() ?: return "§c[AR] Room/area not ready"
        val list = waypoints[key] ?: return "§c[AR] No waypoints in this room"
        if (index !in 0 until list.size) return "§c[AR] Invalid index. Valid: 0-${list.size - 1}"

        val node = list[index]
        val newNode = when (modifier.lowercase()) {
            "chained" -> node.copyWith(chained = value?.toBooleanStrictOrNull() ?: !node.chained)
            "exact" -> node.copyWith(exact = value?.toBooleanStrictOrNull() ?: !node.exact)
            "stop" -> node.copyWith(stop = value?.toBooleanStrictOrNull() ?: !node.stop)
            "center" -> node.copyWith(center = value?.toBooleanStrictOrNull() ?: !node.center)
            "awaitbat" -> node.copyWith(awaitBat = value?.toBooleanStrictOrNull() ?: !node.awaitBat)
            "start" -> node.copyWith(start = value?.toBooleanStrictOrNull() ?: !node.start)
            "await" -> node.copyWith(awaitSecret = value?.toIntOrNull() ?: 1)
            "delay" -> node.copyWith(delay = value?.toIntOrNull() ?: 0)
            "radius" -> node.copyWith(radius = value?.toDoubleOrNull() ?: 0.5)
            "height" -> node.copyWith(height = value?.toDoubleOrNull() ?: 1.5)
            "item" -> node.copyWith(itemName = value)
            "mult" -> node.copyWith(mult = value?.toIntOrNull()?.coerceIn(1, 10) ?: 1)
            else -> return "§c[AR] Unknown modifier: $modifier"
        }

        list[index] = newNode
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
        return "§7[AR] Set §f$modifier §7on node #$index"
    }

    fun moveNode(fromIndex: Int, toIndex: Int): String {
        val key = getCurrentKey() ?: return "§c[AR] Room/area not ready"
        val list = waypoints[key] ?: return "§c[AR] No waypoints in this room"
        if (fromIndex !in 0 until list.size) return "§c[AR] Invalid from index. Valid: 0-${list.size - 1}"
        if (toIndex !in 0 until list.size) return "§c[AR] Invalid to index. Valid: 0-${list.size - 1}"

        val node = list.removeAt(fromIndex)
        list.add(toIndex, node)
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
        return "§7[AR] Moved node from #$fromIndex to #$toIndex"
    }

    fun insertAt(index: Int, rawArgs: String): String {
        val player = mc.player ?: return "§c[AR] No player"
        val level = mc.level ?: return "§c[AR] No world"
        val room = DungeonUtils.currentRoom
        val key = getCurrentKey(room) ?: return "§c[AR] Room/area not ready"

        val parsed = parseAddArgs(rawArgs) ?: return "§c[AR] Invalid arguments"
        val (type, modifiers) = parsed

        val list = waypoints.getOrPut(key) { mutableListOf() }
        if (index !in 0..list.size) return "§c[AR] Invalid index. Valid: 0-${list.size}"

        val yaw = player.yRot
        val pitch = player.xRot
        val feet = player.blockPosition()
        val floorBlock = if (level.getBlockState(feet).block is AirBlock) feet.below() else feet
        val fracX = player.x - floor(player.x)
        val fracZ = player.z - floor(player.z)
        val targetBlock = if (type == WPType.SUPERBOOM) getTargetBlock(player, room) else null
        val node = makeNode(floorBlock, fracX, fracZ, room, type, yaw, pitch, modifiers, targetBlock)

        list.add(index, node)
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
        return "§7[AR] Inserted §f${type.name.lowercase()} §7at index #$index"
    }

    fun updatePosition(index: Int): String {
        val player = mc.player ?: return "§c[AR] No player"
        val level = mc.level ?: return "§c[AR] No world"
        val room = DungeonUtils.currentRoom
        val key = getCurrentKey(room) ?: return "§c[AR] Room/area not ready"

        val list = waypoints[key] ?: return "§c[AR] No waypoints in this room"
        if (index !in 0 until list.size) return "§c[AR] Invalid index. Valid: 0-${list.size - 1}"

        val node = list[index]
        val feet = player.blockPosition()
        val floorBlock = if (level.getBlockState(feet).block is AirBlock) feet.below() else feet
        val base = if (DungeonUtils.inDungeons) getRelativeCoords(floorBlock, room) else floorBlock

        val fracX = player.x - floor(player.x)
        val fracZ = player.z - floor(player.z)
        val x = if (node.exact) base.x.toDouble() + fracX else base.x.toDouble()
        val y = base.y.toDouble()
        val z = if (node.exact) base.z.toDouble() + fracZ else base.z.toDouble()

        list[index] = node.copyWith(x = x, y = y, z = z)
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
        return "§7[AR] Updated position of node #$index"
    }

    fun updateRotation(index: Int): String {
        val player = mc.player ?: return "§c[AR] No player"
        val room = DungeonUtils.currentRoom
        val key = getCurrentKey(room) ?: return "§c[AR] Room/area not ready"

        val list = waypoints[key] ?: return "§c[AR] No waypoints in this room"
        if (index !in 0 until list.size) return "§c[AR] Invalid index. Valid: 0-${list.size - 1}"

        val node = list[index]
        val yaw = player.yRot
        val pitch = player.xRot
        val relativeYaw = if (DungeonUtils.inDungeons && room != null) getRelativeYaw(yaw, room) else yaw

        list[index] = node.copyWith(yaw = relativeYaw, pitch = pitch)
        save()
        if (key == RouteState.currentRoomKey) RouteState.nodeList = getWaypointsForRoom(key) ?: emptyList()
        return "§7[AR] Updated rotation of node #$index to yaw=${"%.1f".format(yaw)}, pitch=${"%.1f".format(pitch)}"
    }

    private fun scanAdjacentRooms() {
        if (!DungeonUtils.inDungeons) return
        val level = mc.level ?: return

        for (room in DungeonUtils.passedRooms) {
            val name = room.data.name
            if (name !in scannedRooms) {
                scannedRooms[name] = room
            }
        }

        val roomsWithWaypoints = waypoints.keys

        for (gridX in 0..5) {
            for (gridZ in 0..5) {
                val roomX = START + gridX * ROOM_SIZE
                val roomZ = START + gridZ * ROOM_SIZE

                if (!level.hasChunkAt(BlockPos(roomX, 70, roomZ))) continue

                try {
                    val room = ScanUtils.scanRoom(Vec2(roomX, roomZ)) ?: continue
                    val roomName = room.data.name

                    if (roomName in roomsWithWaypoints && roomName !in scannedRooms) {
                        scannedRooms[roomName] = room
                        RouteUtils.debug("§a[Scan] Found room: $roomName at grid ($gridX, $gridZ)")
                    }
                } catch (_: Exception) { }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!DungeonUtils.inDungeons) return

        lastScanTick++
        if (lastScanTick >= SCAN_INTERVAL) {
            lastScanTick = 0
            scanAdjacentRooms()
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
        simulating = null
        scannedRooms.clear()
        lastScanTick = 0
        cachedLineSegments = null
        lastNodeListHash = 0
        lastRoomKey = null
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderEvent.Extract) {
        if (!enabled) return

        val player = mc.player ?: return
        val playerPos = player.position()
        val room = DungeonUtils.currentRoom
        val currentKey = getCurrentKey(room)

        if (currentKey != null) {
            val list = waypoints[currentKey]
            if (list != null && list.isNotEmpty()) {
                if (showLines && list.size > 1) {
                    val currentHash = list.hashCode() + config.burstModeAotv().hashCode()
                    if (cachedLineSegments == null || currentHash != lastNodeListHash || currentKey != lastRoomKey) {
                        rebuildLineCache(list, room)
                        lastNodeListHash = currentHash
                        lastRoomKey = currentKey
                    }
                    lineAnimationOffset += animationSpeed
                    val period = dashLength + gapLength
                    if (lineAnimationOffset > period) lineAnimationOffset -= period
                    val segments = cachedLineSegments
                    if (segments != null) {
                        for ((fromApex, toApex, color) in segments) {
                            event.drawAnimatedDashedLine(
                                from = fromApex, to = toApex, color = color,
                                depth = false, thickness = 2f,
                                dashLength = dashLength, gapLength = gapLength, animationOffset = lineAnimationOffset
                            )
                        }
                    }
                }

                for (node in list) {
                    if (renderOnlyStartNodes && !node.start) continue
                    renderNode(event, node, room)
                }
            }
        }

        if (DungeonUtils.inDungeons) {
            for ((roomName, scannedRoom) in scannedRooms) {
                if (roomName == currentKey) continue

                val list = waypoints[roomName] ?: continue

                for (node in list) {
                    if (!node.start) continue

                    val blockPosRel = BlockPos(floor(node.x).toInt(), node.y.toInt(), floor(node.z).toInt())
                    val blockPosWorld = getCoordsOfBlock(blockPosRel, scannedRoom)
                    val nodeWorldPos = Vec3(blockPosWorld.x + 0.5, blockPosWorld.y.toDouble(), blockPosWorld.z + 0.5)

                    if (playerPos.distanceTo(nodeWorldPos) <= 100.0) {
                        renderNode(event, node, scannedRoom)
                    }
                }
            }
        }
    }

    private fun renderNode(event: RenderEvent.Extract, node: WaypointNode, room: Room?) {
        val blockPosRel = BlockPos(floor(node.x).toInt(), node.y.toInt(), floor(node.z).toInt())
        val blockPosWorld = if (DungeonUtils.inDungeons && room != null) getCoordsOfBlock(blockPosRel, room) else blockPosRel

        val aabb = AABB(blockPosWorld)
        val box = aabb.inflate(0.01)

        if (node.start) {
            event.drawFilledBox(box, Color(255, 140, 80, 1.0f), depth = false)
            return
        }

        val style = NodeAppearanceSettings.getStyle(node.type)
        val color = colorFor(node)
        when (style) {
            RenderStyle.PULSE_PYRAMID -> event.drawPulseInfillInvertedPyramid(box, color)
            RenderStyle.WIREFRAME -> event.drawWireFrameBox(box, color, depth = false)
            RenderStyle.FILLED -> event.drawFilledBox(box, color, depth = false)
            RenderStyle.CORNER_BOX -> event.drawCornerBox(box, color, depth = false)
            RenderStyle.DASHED -> event.drawDashedWireBox(box, color, depth = false)
            RenderStyle.DIAMOND -> event.drawDiamond(box, color, style = 1, depth = false)
            RenderStyle.PULSE_BOX -> event.drawPulseBox(box, color, depth = false)
            RenderStyle.X_BOX -> event.drawXBox(box, color, depth = false)
        }

        if (node.type == WPType.AOTV && node.mult > 1) {
            val textPos = Vec3(
                blockPosWorld.x + 0.5,
                blockPosWorld.y + 1.2,
                blockPosWorld.z + 0.5
            )
            event.drawText("§6${node.mult}", textPos, 0.9f, depth = false)
        }
    }

    private fun saveWaypointsForRoom(roomKey: String, nodes: List<WaypointNode>) {
        waypoints[roomKey] = nodes.toMutableList()
        save()
        if (roomKey == RouteState.currentRoomKey) RouteState.nodeList = nodes
    }

    private fun rebuildLineCache(list: List<WaypointNode>, room: Room?) {
        val segments = mutableListOf<Triple<Vec3, Vec3, Color>>()
        val drawn = mutableSetOf<Pair<Int, Int>>()

        for ((index, node) in list.withIndex()) {
            if (node.type != WPType.ETHER) continue
            val chain = BurstMode.findBurstChain(node, index, list, room, skipFirstNodeChecks = true)
            if (chain.indices.size < 2) continue

            for (i in 0 until chain.indices.size - 1) {
                val fromIdx = chain.indices[i]
                val toIdx = chain.indices[i + 1]
                val connection = minOf(fromIdx, toIdx) to maxOf(fromIdx, toIdx)
                if (!drawn.add(connection)) continue

                val fromNode = chain.nodes[i]
                val toNode = chain.nodes[i + 1]
                val fromBlockPos = BlockPos(floor(fromNode.x).toInt(), fromNode.y.toInt(), floor(fromNode.z).toInt())
                val toBlockPos = BlockPos(floor(toNode.x).toInt(), toNode.y.toInt(), floor(toNode.z).toInt())
                val fromWorld = if (DungeonUtils.inDungeons) getCoordsOfBlock(fromBlockPos, room) else fromBlockPos
                val toWorld = if (DungeonUtils.inDungeons) getCoordsOfBlock(toBlockPos, room) else toBlockPos

                val lineColor = Color(85, 255, 255, 1f)
                segments.add(Triple(
                    Vec3(fromWorld.x + 0.5, fromWorld.y.toDouble(), fromWorld.z + 0.5),
                    Vec3(toWorld.x + 0.5, toWorld.y.toDouble(), toWorld.z + 0.5),
                    lineColor
                ))
            }
        }

        if (config.burstModeAotv()) {
            for ((index, node) in list.withIndex()) {
                if (node.type != WPType.AOTV) continue

                val nextNode = list.getOrNull(index + 1) ?: continue
                if (nextNode.type != WPType.ETHER && nextNode.type != WPType.AOTV) continue
                if (nextNode.awaitSecret > 0 || nextNode.awaitBat || nextNode.delay > 0) continue

                val connection = index to (index + 1)
                if (!drawn.add(connection)) continue

                val fromBlockPos = BlockPos(floor(node.x).toInt(), node.y.toInt(), floor(node.z).toInt())
                val toBlockPos = BlockPos(floor(nextNode.x).toInt(), nextNode.y.toInt(), floor(nextNode.z).toInt())
                val fromWorld = if (DungeonUtils.inDungeons) getCoordsOfBlock(fromBlockPos, room) else fromBlockPos
                val toWorld = if (DungeonUtils.inDungeons) getCoordsOfBlock(toBlockPos, room) else toBlockPos
                val lineColor = Color(255, 255, 255, 1f)
                segments.add(Triple(
                    Vec3(fromWorld.x + 0.5, fromWorld.y.toDouble(), fromWorld.z + 0.5),
                    Vec3(toWorld.x + 0.5, toWorld.y.toDouble(), toWorld.z + 0.5),
                    lineColor
                ))
            }
        }

        cachedLineSegments = segments
    }

    private data class ParsedArgs(val type: WPType, val modifiers: Map<String, String?>)

    private fun parseAddArgs(raw: String): ParsedArgs? {
        val parts = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        val type = WPType.fromString(parts[0]) ?: return null
        val modifiers = mutableMapOf<String, String?>()

        for (i in 1 until parts.size) {
            val part = parts[i].lowercase()
            when {
                part == "chained" -> modifiers["chained"] = "true"
                part == "exact" -> modifiers["exact"] = "true"
                part == "stop" -> modifiers["stop"] = "true"
                part == "center" -> modifiers["center"] = "true"
                part == "awaitbat" -> modifiers["awaitbat"] = "true"
                part == "start" -> modifiers["start"] = "true"
                part.startsWith("await:") -> {
                    val awaitParts = part.substringAfter("await:").split(":")
                    modifiers["await"] = awaitParts[0]
                    if (awaitParts.size > 1) modifiers["awaitType"] = awaitParts[1]
                }
                part.startsWith("delay:") -> modifiers["delay"] = part.substringAfter("delay:")
                part.startsWith("item:") -> modifiers["item"] = part.substringAfter("item:")
                part.startsWith("radius:") -> modifiers["radius"] = part.substringAfter("radius:")
                part.startsWith("height:") -> modifiers["height"] = part.substringAfter("height:")
                part.startsWith("mult:") -> modifiers["mult"] = part.substringAfter("mult:")
            }
        }

        return ParsedArgs(type, modifiers)
    }

    private fun makeNode(
        floorBlock: BlockPos,
        fracX: Double,
        fracZ: Double,
        room: Room?,
        type: WPType,
        yaw: Float,
        pitch: Float,
        modifiers: Map<String, String?>,
        targetBlock: BlockPos?
    ): WaypointNode {
        val base = if (DungeonUtils.inDungeons) getRelativeCoords(floorBlock, room) else floorBlock
        val exact = modifiers.containsKey("exact")
        val x = if (exact) base.x.toDouble() + fracX else base.x.toDouble()
        val z = if (exact) base.z.toDouble() + fracZ else base.z.toDouble()
        val relativeYaw = if (DungeonUtils.inDungeons && room != null) getRelativeYaw(yaw, room) else yaw
        val relTargetBlock = if (targetBlock != null && DungeonUtils.inDungeons) getRelativeCoords(targetBlock, room) else targetBlock

        return WaypointNode(
            x = x, y = base.y.toDouble(), z = z,
            exact = exact, type = type, yaw = relativeYaw, pitch = pitch,
            chained = modifiers.containsKey("chained"),
            radius = modifiers["radius"]?.toDoubleOrNull() ?: 0.5,
            height = modifiers["height"]?.toDoubleOrNull() ?: 1.5,
            start = modifiers.containsKey("start"),
            delay = modifiers["delay"]?.toIntOrNull() ?: 0,
            stop = modifiers.containsKey("stop"),
            center = modifiers.containsKey("center"),
            awaitSecret = modifiers["await"]?.toIntOrNull() ?: 0,
            awaitBat = modifiers.containsKey("awaitbat"),
            awaitType = modifiers["awaitType"] ?: "any",
            toBlock = null,
            targetBlock = relTargetBlock,
            itemName = modifiers["item"],
            mult = modifiers["mult"]?.toIntOrNull()?.coerceIn(1, 10) ?: 1
        )
    }

    private fun getTargetBlock(player: LocalPlayer, room: Room?): BlockPos? =
        (mc.hitResult as? BlockHitResult)?.blockPos

    private fun getRelativeCoords(pos: BlockPos, room: Room?): BlockPos = room?.getRelativeCoords(pos) ?: pos
    private fun getCoordsOfBlock(pos: BlockPos, room: Room?): BlockPos = room?.getRealCoords(pos) ?: pos

    private fun getCurrentKey(room: Room? = DungeonUtils.currentRoom): String? {
        if (!LocationUtils.isInSkyblock && simulating == null) return null
        return when {
            simulating != null -> simulating
            DungeonUtils.inDungeons -> room?.data?.name
            else -> LocationUtils.currentArea?.toString()
        }
    }

    private fun buildModifierString(node: WaypointNode): String {
        val parts = mutableListOf<String>()
        if (node.chained) parts.add("chained")
        if (node.exact) parts.add("exact")
        if (node.stop) parts.add("stop")
        if (node.center) parts.add("center")
        if (node.awaitSecret > 0) parts.add("await:${node.awaitSecret}")
        if (node.awaitBat) parts.add("awaitbat")
        if (node.start) parts.add("start")
        if (node.delay > 0) parts.add("delay:${node.delay}")
        if (node.itemName != null) parts.add("item:${node.itemName}")
        if (node.mult > 1) parts.add("mult:${node.mult}")
        return if (parts.isEmpty()) "" else " §8[${parts.joinToString(", ")}]"
    }

    private fun colorCodeFor(type: WPType): String = when (type) {
        WPType.ETHER -> "§b"
        WPType.AOTV -> "§6"
        WPType.HYPE -> "§5"
        WPType.SUPERBOOM -> "§c"
        WPType.USEITEM -> "§a"
        WPType.LOOK -> "§e"
        WPType.NOP -> "§7"
    }

    private fun colorFor(node: WaypointNode): Color {
        val color = NodeAppearanceSettings.getColor(node.type)
        val alpha = if (node.chained) color.alphaFloat * 0.6f else color.alphaFloat
        return Color((color.redFloat * 255).toInt(), (color.greenFloat * 255).toInt(), (color.blueFloat * 255).toInt(), alpha)
    }

    private fun Rotations.toDegrees(): Float = when (this) {
        Rotations.NORTH -> 0f
        Rotations.EAST -> 90f
        Rotations.SOUTH -> 180f
        Rotations.WEST -> 270f
        Rotations.NONE -> 0f
    }

    private fun getRelativeYaw(worldYaw: Float, room: Room): Float = normalizeYaw(worldYaw - room.rotation.toDegrees())

    private fun normalizeYaw(yaw: Float): Float {
        var normalized = yaw % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized < -180f) normalized += 360f
        return normalized
    }

    private fun save() {
        if (!loaded) { load(); loaded = true }

        val json = JsonObject()
        waypoints.forEach { (roomName, nodes) ->
            val array = JsonArray()
            nodes.forEach { node ->
                val obj = JsonObject()
                if (node.exact) {
                    obj.addProperty("x", node.x); obj.addProperty("y", node.y); obj.addProperty("z", node.z)
                } else {
                    obj.addProperty("x", node.x.toInt()); obj.addProperty("y", node.y.toInt()); obj.addProperty("z", node.z.toInt())
                }
                obj.addProperty("exact", node.exact)
                obj.addProperty("type", node.type.name)
                obj.addProperty("yaw", node.yaw)
                obj.addProperty("pitch", node.pitch)
                obj.addProperty("chained", node.chained)
                obj.addProperty("radius", node.radius)
                obj.addProperty("height", node.height)
                obj.addProperty("start", node.start)
                obj.addProperty("delay", node.delay)
                obj.addProperty("stop", node.stop)
                obj.addProperty("center", node.center)
                obj.addProperty("awaitSecret", node.awaitSecret)
                obj.addProperty("awaitBat", node.awaitBat)
                obj.addProperty("awaitType", node.awaitType)
                obj.addProperty("mult", node.mult)

                node.toBlock?.let { tb ->
                    obj.add("toBlock", JsonObject().apply { addProperty("x", tb.x); addProperty("y", tb.y); addProperty("z", tb.z) })
                }
                node.targetBlock?.let { tb ->
                    obj.add("targetBlock", JsonObject().apply { addProperty("x", tb.x); addProperty("y", tb.y); addProperty("z", tb.z) })
                }
                node.itemName?.let { obj.addProperty("itemName", it) }
                array.add(obj)
            }
            json.add(roomName, array)
        }

        FileHandler.writeToFile("waypoints.json", json)
    }

    private fun load() {
        val json = FileHandler.readFromFile("waypoints.json")
        waypoints.clear()

        json.entrySet().forEach { (roomName, element) ->
            val nodes = mutableListOf<WaypointNode>()
            element.asJsonArray.forEach { nodeElement ->
                val obj = nodeElement.asJsonObject

                fun num(n: String) = obj.get(n)?.asDouble ?: 0.0
                fun int(n: String) = obj.get(n)?.asInt ?: 0
                fun bool(n: String) = obj.get(n)?.asBoolean ?: false
                fun str(n: String) = obj.get(n)?.asString

                var x = num("x")
                var z = num("z")
                if (obj.has("ox")) x += obj.get("ox").asDouble
                if (obj.has("oz")) z += obj.get("oz").asDouble

                val toBlock = obj.get("toBlock")?.asJsonObject?.let { BlockPos(it.get("x").asInt, it.get("y").asInt, it.get("z").asInt) }
                val targetBlock = obj.get("targetBlock")?.asJsonObject?.let { BlockPos(it.get("x").asInt, it.get("y").asInt, it.get("z").asInt) }

                nodes.add(
                    WaypointNode(
                        x = x, y = num("y"), z = z,
                        exact = bool("exact"),
                        type = str("type")?.let { WPType.fromString(it) } ?: WPType.ETHER,
                        yaw = obj.get("yaw")?.asFloat ?: 0f,
                        pitch = obj.get("pitch")?.asFloat ?: 0f,
                        chained = bool("chained"),
                        radius = obj.get("radius")?.asDouble ?: 0.5,
                        height = obj.get("height")?.asDouble ?: 1.5,
                        start = bool("start"),
                        delay = int("delay"),
                        stop = bool("stop"),
                        center = bool("center"),
                        awaitSecret = int("awaitSecret"),
                        awaitBat = bool("awaitBat"),
                        awaitType = str("awaitType") ?: "any",
                        toBlock = toBlock,
                        targetBlock = targetBlock,
                        itemName = str("itemName"),
                        mult = obj.get("mult")?.asInt ?: 1
                    )
                )
            }
            waypoints[roomName] = nodes
        }
    }
}
