package com.peachsoju.modules.impl.fmblocks

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.handlers.FileHandler
import com.peachsoju.utils.RouteUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Rotations
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.block.Rotation
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object FMBlocksManager {

    val enabled: Boolean get() = config.fmBlocksEnabled()
    val editModeEnabled: Boolean get() = config.fmBlocksEditMode()
    private var reapplyTickCounter = 0
    private const val reapplyInterval = 5

    private const val FMBLOCKS_FILE = "fmblocks.json"
    private const val BOSS_FMBLOCKS_FILE = "boss_fmblocks.json"

    private val roomBlocks = mutableMapOf<String, FMBlocksRoomData>()
    private val bossRoomBlocks = mutableMapOf<String, FMBlocksRoomData>()
    private val worldBlockCache = mutableMapOf<BlockPos, BlockState>()
    private var lastRoomKey: String? = null
    private var loaded = false

    var simulating: String? = null
        private set

    /**
     * Determines if a room key represents a boss room.
     * Boss rooms typically follow patterns like "boss_1", "boss_2", etc.
     */
    private fun isBossRoom(roomKey: String): Boolean {
        val lowerKey = roomKey.lowercase()
        return lowerKey.startsWith("boss_") ||
                lowerKey.startsWith("boss ") ||
                lowerKey == "boss"
    }

    /**
     * Gets the appropriate block map for a room key.
     */
    private fun getBlockMapForRoom(roomKey: String): MutableMap<String, FMBlocksRoomData> {
        return if (isBossRoom(roomKey)) bossRoomBlocks else roomBlocks
    }

    fun setSimulating(roomKey: String?) {
        simulating = roomKey
        if (roomKey != null) {
            lastRoomKey = null
            worldBlockCache.clear()
            RouteUtils.debug("§e[FMBlocks] Simulating room: $roomKey")
        } else {
            worldBlockCache.clear()
            lastRoomKey = null
            RouteUtils.debug("§e[FMBlocks] Simulation disabled")
        }
    }

    fun getBlocksForRoom(roomKey: String): FMBlocksRoomData? {
        return bossRoomBlocks[roomKey] ?: roomBlocks[roomKey]
    }

    fun getOrCreateBlocksForRoom(roomKey: String): FMBlocksRoomData {
        val blockMap = getBlockMapForRoom(roomKey)
        return blockMap.getOrPut(roomKey) { FMBlocksRoomData() }
    }

    fun getCurrentRoomKey(): String? {
        if (simulating != null) return simulating

        if (!LocationUtils.isInSkyblock) return null

        return if (DungeonUtils.inDungeons) {
            if (DungeonUtils.inBoss) {
                "boss_${DungeonUtils.floor?.floorNumber ?: return null}"
            } else {
                DungeonUtils.currentRoom?.data?.name
            }
        } else {
            LocationUtils.currentArea?.toString()
        }
    }

    private fun getCurrentRoom(): Room? {
        if (simulating != null) return null
        if (DungeonUtils.inBoss) return null
        return DungeonUtils.currentRoom
    }

    fun worldToRelative(worldPos: BlockPos, room: Room?): BlockPos =
        if (DungeonUtils.inDungeons && room != null) room.getRelativeCoords(worldPos) ?: worldPos else worldPos

    fun relativeToWorld(relativePos: BlockPos, room: Room?): BlockPos =
        if (DungeonUtils.inDungeons && room != null) room.getRealCoords(relativePos) ?: relativePos else relativePos

    fun getRoomRotation(room: Room?): Int = room?.rotation?.toDegrees() ?: 0

    fun addBlock(worldPos: BlockPos, state: BlockState): Boolean {
        val room = getCurrentRoom()
        val roomKey = getCurrentRoomKey() ?: return false
        val relativePos = worldToRelative(worldPos, room)
        val rotation = getRoomRotation(room)
        val rotatedState = rotateBlockState(state, -rotation)
        val data = getOrCreateBlocksForRoom(roomKey)

        if (data.hasBlockAt(relativePos)) data.removeBlock(relativePos)
        data.addBlock(relativePos, rotatedState)
        save()

        mc.level?.setBlockAndUpdate(worldPos, state)
        worldBlockCache[worldPos] = state

        RouteUtils.debug("§a[FMBlocks] Added block at $relativePos")
        return true
    }

    fun addGhostBlock(worldPos: BlockPos): Boolean {
        val room = getCurrentRoom()
        val roomKey = getCurrentRoomKey() ?: return false
        val relativePos = worldToRelative(worldPos, room)
        val data = getOrCreateBlocksForRoom(roomKey)

        data.addBlock(relativePos, Blocks.AIR.defaultBlockState())
        save()

        val airState = Blocks.AIR.defaultBlockState()
        mc.level?.setBlockAndUpdate(worldPos, airState)
        worldBlockCache[worldPos] = airState

        RouteUtils.debug("§7[FMBlocks] Added ghost block at $relativePos")
        return true
    }

    fun removeBlock(worldPos: BlockPos): Boolean {
        val room = getCurrentRoom()
        val roomKey = getCurrentRoomKey() ?: return false
        val relativePos = worldToRelative(worldPos, room)
        val data = getBlocksForRoom(roomKey) ?: return false
        val removed = data.removeBlock(relativePos)

        if (removed) {
            save()
            worldBlockCache.remove(worldPos)
            RouteUtils.debug("§c[FMBlocks] Removed block at $relativePos")
        }
        return removed
    }

    fun hasBlockAt(worldPos: BlockPos): Boolean {
        val room = getCurrentRoom()
        val roomKey = getCurrentRoomKey() ?: return false
        val relativePos = worldToRelative(worldPos, room)
        return getBlocksForRoom(roomKey)?.hasBlockAt(relativePos) == true
    }

    fun isGhostBlock(worldPos: BlockPos): Boolean {
        val room = getCurrentRoom()
        val roomKey = getCurrentRoomKey() ?: return false
        val relativePos = worldToRelative(worldPos, room)
        return getBlocksForRoom(roomKey)?.getBlockAt(relativePos)?.isAir == true
    }

    fun clearCurrentRoom() {
        val roomKey = getCurrentRoomKey() ?: return
        val blockMap = getBlockMapForRoom(roomKey)
        blockMap[roomKey]?.clear()
        save()
        worldBlockCache.clear()
        RouteUtils.debug("§e[FMBlocks] Cleared all blocks in $roomKey")
    }

    fun getStats(): Pair<Int, Int> {
        val normalRoomCount = roomBlocks.count { !it.value.isEmpty() }
        val bossRoomCount = bossRoomBlocks.count { !it.value.isEmpty() }
        val normalBlockCount = roomBlocks.values.sumOf { data -> data.blocks.values.sumOf { it.size } }
        val bossBlockCount = bossRoomBlocks.values.sumOf { data -> data.blocks.values.sumOf { it.size } }
        return (normalRoomCount + bossRoomCount) to (normalBlockCount + bossBlockCount)
    }

    fun getBossStats(): Pair<Int, Int> {
        val roomCount = bossRoomBlocks.count { !it.value.isEmpty() }
        val blockCount = bossRoomBlocks.values.sumOf { data -> data.blocks.values.sumOf { it.size } }
        return roomCount to blockCount
    }

    fun getNormalStats(): Pair<Int, Int> {
        val roomCount = roomBlocks.count { !it.value.isEmpty() }
        val blockCount = roomBlocks.values.sumOf { data -> data.blocks.values.sumOf { it.size } }
        return roomCount to blockCount
    }

    fun getRoomList(): List<String> {
        val normalRooms = roomBlocks.keys.filter { !roomBlocks[it]!!.isEmpty() }
        val bossRooms = bossRoomBlocks.keys.filter { !bossRoomBlocks[it]!!.isEmpty() }
        return (normalRooms + bossRooms).sorted()
    }

    fun reloadFromDisk() {
        load()
        loaded = true
        worldBlockCache.clear()
        lastRoomKey = null
        val (rooms, blocks) = getStats()
        val (bossRooms, bossBlocks) = getBossStats()
        RouteUtils.debug("§a[FMBlocks] Reloaded $blocks blocks from $rooms rooms ($bossRooms boss rooms with $bossBlocks blocks)")
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
        worldBlockCache.clear()
        lastRoomKey = null
        simulating = null
        if (!loaded) {
            load()
            loaded = true
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return

        val room = getCurrentRoom()
        val roomKey = getCurrentRoomKey() ?: return

        if (roomKey != lastRoomKey) {
            lastRoomKey = roomKey
            worldBlockCache.clear()
            applyBlocksToWorld(roomKey, room)
        }
    }

    private fun reapplyCachedBlocks() {
        val level = mc.level ?: return
        for ((pos, state) in worldBlockCache) {
            val currentState = level.getBlockState(pos)
            if (currentState != state) level.setBlockAndUpdate(pos, state)
        }
    }

    private val pendingReapply = mutableSetOf<BlockPos>()
    private var reapplyScheduled = false

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        if (!enabled) return
        when (val packet = event.packet) {
            is ClientboundBlockUpdatePacket -> {
                val pos = packet.pos
                if (worldBlockCache.containsKey(pos)) {
                    pendingReapply.add(pos)
                    scheduleReapply()
                }
            }
            is ClientboundSectionBlocksUpdatePacket -> {
                packet.runUpdates { pos, _ ->
                    if (worldBlockCache.containsKey(pos)) {
                        pendingReapply.add(pos.immutable())
                        scheduleReapply()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onPacketSend(event: PacketEvent.Send) {
        if (!enabled) return
        val packet = event.packet
        if (packet is ServerboundPlayerActionPacket && packet.action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            val pos = packet.pos
            val state = worldBlockCache[pos] ?: return
            CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS).execute {
                mc.execute { mc.level?.setBlock(pos, state, 3) }
            }
        }
    }

    private fun scheduleReapply() {
        if (reapplyScheduled) return
        reapplyScheduled = true
        mc.execute {
            val level = mc.level ?: return@execute
            for (pos in pendingReapply) {
                val state = worldBlockCache[pos] ?: continue
                level.setBlock(pos, state, 3)
            }
            pendingReapply.clear()
            reapplyScheduled = false
        }
    }

    private fun applyBlocksToWorld(roomKey: String, room: Room?) {
        val level = mc.level ?: return
        val data = getBlocksForRoom(roomKey) ?: return
        val rotation = getRoomRotation(room)

        for ((state, positions) in data.blocks) {
            val rotatedState = rotateBlockState(state, rotation)
            for (relativePos in positions) {
                val worldPos = relativeToWorld(relativePos, room)
                level.setBlockAndUpdate(worldPos, rotatedState)
                worldBlockCache[worldPos] = rotatedState
            }
        }

        if (worldBlockCache.isNotEmpty()) {
            RouteUtils.debug("§a[FMBlocks] Applied ${worldBlockCache.size} blocks to $roomKey")
        }
    }

    private fun rotateBlockState(state: BlockState, rotation: Int): BlockState {
        if (rotation == 0) return state
        val normalizedRotation = ((rotation % 360) + 360) % 360
        val steps = normalizedRotation / 90
        var result = state
        repeat(steps) { result = result.rotate(Rotation.CLOCKWISE_90) }
        return result
    }

    private fun save() {
        saveNormalBlocks()
        saveBossBlocks()
    }

    private fun saveNormalBlocks() {
        val json = JsonObject()
        roomBlocks.forEach { (roomName, data) ->
            if (data.isEmpty()) return@forEach
            val roomArray = serializeRoomData(data)
            if (roomArray.size() > 0) json.add(roomName, roomArray)
        }
        FileHandler.writeToFile(FMBLOCKS_FILE, json)
    }

    private fun saveBossBlocks() {
        val json = JsonObject()
        bossRoomBlocks.forEach { (roomName, data) ->
            if (data.isEmpty()) return@forEach
            val roomArray = serializeRoomData(data)
            if (roomArray.size() > 0) json.add(roomName, roomArray)
        }
        FileHandler.writeToFile(BOSS_FMBLOCKS_FILE, json)
    }

    private fun serializeRoomData(data: FMBlocksRoomData): JsonArray {
        val roomArray = JsonArray()
        for ((state, positions) in data.blocks) {
            if (positions.isEmpty()) continue
            val blockObj = JsonObject()
            blockObj.addProperty("state", serializeBlockState(state))

            val posArray = JsonArray()
            for (pos in positions) {
                val posObj = JsonObject()
                posObj.addProperty("x", pos.x)
                posObj.addProperty("y", pos.y)
                posObj.addProperty("z", pos.z)
                posArray.add(posObj)
            }
            blockObj.add("positions", posArray)
            roomArray.add(blockObj)
        }
        return roomArray
    }

    private fun load() {
        loadNormalBlocks()
        loadBossBlocks()
    }

    private fun loadNormalBlocks() {
        val json = FileHandler.readFromFile(FMBLOCKS_FILE)
        roomBlocks.clear()

        json.entrySet().forEach { (roomName, element) ->
            if (isBossRoom(roomName)) return@forEach

            val data = deserializeRoomData(element)
            if (data != null && !data.isEmpty()) {
                roomBlocks[roomName] = data
            }
        }
    }

    private fun loadBossBlocks() {
        val json = FileHandler.readFromFile(BOSS_FMBLOCKS_FILE)
        bossRoomBlocks.clear()

        json.entrySet().forEach { (roomName, element) ->
            val data = deserializeRoomData(element)
            if (data != null && !data.isEmpty()) {
                bossRoomBlocks[roomName] = data
            }
        }

        val normalJson = FileHandler.readFromFile(FMBLOCKS_FILE)
        var migrated = false
        normalJson.entrySet().forEach { (roomName, element) ->
            if (isBossRoom(roomName) && !bossRoomBlocks.containsKey(roomName)) {
                val data = deserializeRoomData(element)
                if (data != null && !data.isEmpty()) {
                    bossRoomBlocks[roomName] = data
                    migrated = true
                    RouteUtils.debug("§e[FMBlocks] Migrated boss room '$roomName' to boss_fmblocks.json")
                }
            }
        }

        if (migrated) {
            save()
        }
    }

    private fun deserializeRoomData(element: com.google.gson.JsonElement): FMBlocksRoomData? {
        return try {
            val data = FMBlocksRoomData()

            element.asJsonArray.forEach { blockElement ->
                val obj = blockElement.asJsonObject
                val stateStr = obj.get("state")?.asString ?: return@forEach
                val state = deserializeBlockState(stateStr) ?: return@forEach

                val positions = mutableSetOf<BlockPos>()
                obj.getAsJsonArray("positions")?.forEach { posElement ->
                    val posObj = posElement.asJsonObject
                    val x = posObj.get("x")?.asInt ?: return@forEach
                    val y = posObj.get("y")?.asInt ?: return@forEach
                    val z = posObj.get("z")?.asInt ?: return@forEach
                    positions.add(BlockPos(x, y, z))
                }

                if (positions.isNotEmpty()) data.blocks[state] = positions
            }

            data
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeBlockState(state: BlockState): String {
        val block = state.block
        val blockId = BuiltInRegistries.BLOCK.getKey(block).toString()
        if (state == block.defaultBlockState()) return blockId

        val properties = state.values.entries.joinToString(",") { (prop, value) ->
            "${prop.name}=${value.toString().lowercase()}"
        }

        return if (properties.isNotEmpty()) "$blockId[$properties]" else blockId
    }

    private fun deserializeBlockState(str: String): BlockState? {
        return try {
            val bracketIndex = str.indexOf('[')
            val blockIdStr = if (bracketIndex >= 0) str.substring(0, bracketIndex) else str
            val propertiesStr = if (bracketIndex >= 0) str.substring(bracketIndex + 1, str.length - 1) else null

            val blockId = ResourceLocation.tryParse(blockIdStr) ?: return null
            val block = BuiltInRegistries.BLOCK.getValue(blockId)
            if (block == Blocks.AIR && blockIdStr != "minecraft:air") return null

            var state = block.defaultBlockState()
            if (!propertiesStr.isNullOrEmpty()) {
                for (prop in propertiesStr.split(",")) {
                    val parts = prop.split("=")
                    if (parts.size != 2) continue
                    val propName = parts[0]
                    val propValue = parts[1]
                    val property = block.stateDefinition.getProperty(propName) ?: continue
                    state = applyProperty(state, property, propValue)
                }
            }
            state
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyProperty(state: BlockState, property: Property<*>, valueStr: String): BlockState {
        return try {
            val optional = property.getValue(valueStr)
            if (optional.isEmpty) return state
            val value = optional.get()
            state.setValue(property as Property<Comparable<Any>>, value as Comparable<Any>)
        } catch (e: Exception) {
            state
        }
    }

    private fun Rotations.toDegrees(): Int = when (this) {
        Rotations.NORTH -> 0
        Rotations.EAST -> 90
        Rotations.SOUTH -> 180
        Rotations.WEST -> 270
        Rotations.NONE -> 0
    }
}