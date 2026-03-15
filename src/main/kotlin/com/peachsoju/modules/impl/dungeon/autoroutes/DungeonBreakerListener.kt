package com.peachsoju.modules.impl.dungeon.autoroutes

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.modules.impl.dungeon.autoroutes.data.WaypointNode
import com.peachsoju.utils.RouteUtils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import us.filthycheaters.legitcatsex.utils.SilentSwap
import java.io.File
import kotlin.math.floor

object DungeonBreakerListener {

    private const val DB_CHECK_RADIUS = 6.0
    private const val SEX5_WAYPOINTS_PATH = "sex5/waypoints.json"

    data class DbWaypoint(
        val x: Int,
        val y: Int,
        val z: Int,
        val roomName: String
    )

    private var sex5Waypoints: Map<String, List<DbWaypoint>> = emptyMap()
    private var lastLoadTime = 0L
    private const val CACHE_DURATION_MS = 5000L

    private var isAwaitingDb = false
    private var awaitingNodeIndex = -1
    private var awaitingNode: WaypointNode? = null
    private var awaitingRoom: Room? = null
    private var awaitingDbPositions: List<BlockPos> = emptyList()

    fun isAwaitingDb(): Boolean = isAwaitingDb

    private fun loadSex5Waypoints() {
        val now = System.currentTimeMillis()
        if (now - lastLoadTime < CACHE_DURATION_MS && sex5Waypoints.isNotEmpty()) {
            return
        }

        try {
            val configDir = FabricLoader.getInstance().configDir.toFile()
            val sex5File = File(configDir, SEX5_WAYPOINTS_PATH)

            if (!sex5File.exists()) {
                RouteUtils.debug("§c[DB] sex5 waypoints.json not found at ${sex5File.absolutePath}")
                sex5Waypoints = emptyMap()
                lastLoadTime = now
                return
            }

            val text = sex5File.readText()
            if (text.isBlank()) {
                sex5Waypoints = emptyMap()
                lastLoadTime = now
                return
            }

            val json = JsonParser.parseString(text).asJsonObject
            val waypointsMap = mutableMapOf<String, MutableList<DbWaypoint>>()

            json.entrySet().forEach { (roomName, element) ->
                val roomWaypoints = mutableListOf<DbWaypoint>()
                element.asJsonArray.forEach { waypointElement ->
                    val obj = waypointElement.asJsonObject
                    val type = obj.get("type")?.asString ?: ""

                    if (type.equals("DUNGEON_BREAKER_AURA", ignoreCase = true)) {
                        val x = obj.get("x")?.asInt ?: 0
                        val y = obj.get("y")?.asInt ?: 0
                        val z = obj.get("z")?.asInt ?: 0
                        roomWaypoints.add(DbWaypoint(x, y, z, roomName))
                    }
                }
                if (roomWaypoints.isNotEmpty()) {
                    waypointsMap[roomName] = roomWaypoints
                }
            }

            sex5Waypoints = waypointsMap
            lastLoadTime = now
            RouteUtils.extraDebug("§a[DB] Loaded ${waypointsMap.values.sumOf { it.size }} DB waypoints from sex5")

        } catch (e: Exception) {
            RouteUtils.debug("§c[DB] Error loading sex5 waypoints: ${e.message}")
            sex5Waypoints = emptyMap()
            lastLoadTime = now
        }
    }

    fun getDbWaypointsNearNode(node: WaypointNode, room: Room?): List<BlockPos> {
        loadSex5Waypoints()

        val roomName = room?.data?.name ?: return emptyList()
        val roomWaypoints = sex5Waypoints[roomName] ?: return emptyList()

        val nodeWorldPos = RouteUtils.getNodeWorldPosition(node, room)

        val nearbyPositions = mutableListOf<BlockPos>()

        for (wp in roomWaypoints) {
            val relativePos = BlockPos(wp.x, wp.y, wp.z)
            val worldPos = if (DungeonUtils.inDungeons && room != null) {
                room.getRealCoords(relativePos) ?: relativePos
            } else {
                relativePos
            }

            val worldVec = Vec3(worldPos.x + 0.5, worldPos.y + 0.5, worldPos.z + 0.5)
            val distance = nodeWorldPos.distanceTo(worldVec)

            if (distance <= DB_CHECK_RADIUS) {
                nearbyPositions.add(worldPos)
                RouteUtils.extraDebug("§7[DB] Found DB waypoint at $worldPos (dist: ${"%.1f".format(distance)})")
            }
        }

        return nearbyPositions
    }

    fun areAllBlocksAir(positions: List<BlockPos>): Boolean {
        val level = mc.level ?: return false

        for (pos in positions) {
            val state = level.getBlockState(pos)
            if (!state.isAir) {
                return false
            }
        }

        return true
    }

    fun shouldWaitForDb(node: WaypointNode, room: Room?): Boolean {
        if (!node.awaitDb) return false

        val dbPositions = getDbWaypointsNearNode(node, room)
        if (dbPositions.isEmpty()) {
            RouteUtils.debug("§a[DB] No DB waypoints found near node, proceeding")
            return false
        }

        val allAir = areAllBlocksAir(dbPositions)
        if (allAir) {
            RouteUtils.debug("§a[DB] All ${dbPositions.size} DB blocks are air, proceeding")
            return false
        }

        RouteUtils.debug("§e[DB] Waiting for ${dbPositions.size} DB blocks to be mined")
        return true
    }

    fun startWaitingForDb(
        node: WaypointNode,
        index: Int,
        room: Room?
    ) {
        val dbPositions = getDbWaypointsNearNode(node, room)
        if (dbPositions.isEmpty()) {
            RouteUtils.debug("§c[DB] startWaitingForDb called but no DB waypoints found")
            return
        }

        isAwaitingDb = true
        awaitingNodeIndex = index
        awaitingNode = node
        awaitingRoom = room
        awaitingDbPositions = dbPositions

        val nonAirCount = dbPositions.count { pos ->
            val state = mc.level?.getBlockState(pos)
            state != null && !state.isAir
        }

        RouteUtils.debug("§e[DB] Waiting for $nonAirCount/${dbPositions.size} blocks to become air...")
    }

    fun cancel() {
        isAwaitingDb = false
        alsoAwaitingSecrets = false
        awaitingNodeIndex = -1
        awaitingNode = null
        awaitingRoom = null
        awaitingDbPositions = emptyList()
    }

    fun manualTrigger() {
        if (!isAwaitingDb) {
            RouteUtils.debug("§c[DB] Manual trigger ignored - not awaiting")
            return
        }
        RouteUtils.debug("§e[DB] Manual trigger!")
        onDbBlocksCleared()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (isAwaitingDb) {
            val nonAirCount = awaitingDbPositions.count { pos ->
                val state = mc.level?.getBlockState(pos)
                state != null && !state.isAir
            }
            RouteUtils.extraDebug("§7[DB] Tick: awaiting=$isAwaitingDb, blocks=$nonAirCount/${awaitingDbPositions.size}")
        }

        if (!isAwaitingDb) return

        if (SilentSwap.isSilent()) {
            RouteUtils.extraDebug("§7[DB] Waiting for SilentSwap to finish...")
            return
        }

        if (areAllBlocksAir(awaitingDbPositions)) {
            if (alsoAwaitingSecrets) {
                if (RouteState.awaitingSecrets <= 0) {
                    RouteUtils.debug("§a[DB] All DB blocks cleared AND secrets done!")
                    onDbBlocksCleared()
                } else {
                    RouteUtils.extraDebug("§7[DB] DB blocks clear, waiting for ${RouteState.awaitingSecrets} more secrets")
                }
            } else {
                RouteUtils.debug("§a[DB] All DB blocks cleared!")
                onDbBlocksCleared()
            }
        }
    }

    private fun onDbBlocksCleared() {
        val node = awaitingNode
        val index = awaitingNodeIndex
        val room = awaitingRoom

        if (node != null) {
            RouteUtils.debug("§a[DB] DB blocks cleared. Executing node #$index")
            RouteState.waitingForTeleport = true
            RouteState.awaitingTeleportNodeIndex = index
            RouteState.lock()
            Autoroutes.executeNodePublic(node, index, room)
        }

        cancel()
    }

    fun getAwaitingNodeIndex(): Int = awaitingNodeIndex
    fun getAwaitingNode(): WaypointNode? = awaitingNode
    fun getAwaitingDbPositions(): List<BlockPos> = awaitingDbPositions

    private var alsoAwaitingSecrets = false

    fun startWaitingForDbAndSecrets(
        node: WaypointNode,
        index: Int,
        room: Room?
    ) {
        val dbPositions = getDbWaypointsNearNode(node, room)
        if (dbPositions.isEmpty()) {
            RouteUtils.debug("§c[DB] startWaitingForDbAndSecrets called but no DB waypoints found")
            return
        }

        isAwaitingDb = true
        alsoAwaitingSecrets = true
        awaitingNodeIndex = index
        awaitingNode = node
        awaitingRoom = room
        awaitingDbPositions = dbPositions

        val nonAirCount = dbPositions.count { pos ->
            val state = mc.level?.getBlockState(pos)
            state != null && !state.isAir
        }

        RouteUtils.debug("§e[DB] Waiting for $nonAirCount/${dbPositions.size} blocks AND ${node.awaitSecret} secrets...")
    }

    fun canProceed(): Boolean {
        if (!isAwaitingDb) return true

        if (!areAllBlocksAir(awaitingDbPositions)) {
            return false
        }

        if (alsoAwaitingSecrets && RouteState.awaitingSecrets > 0) {
            return false
        }

        return true
    }

    fun checkCombinedConditions() {
        if (!isAwaitingDb || !alsoAwaitingSecrets) return

        val dbClear = areAllBlocksAir(awaitingDbPositions)
        val secretsDone = RouteState.awaitingSecrets <= 0

        RouteUtils.debug("§7[DB] Combined check: dbClear=$dbClear, secretsDone=$secretsDone")

        if (dbClear && secretsDone) {
            RouteUtils.debug("§a[DB] Both DB and secrets done! Executing node")
            onDbBlocksCleared()
        }
    }

    fun reloadWaypoints() {
        lastLoadTime = 0L
        loadSex5Waypoints()
    }

    fun isAlsoAwaitingSecrets(): Boolean = alsoAwaitingSecrets
}