package com.peachsoju.modules.impl.dungeon.puzzles.autoicefill

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.RoomEnterEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.eventbus.events.TickEvent
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Rotations
import com.peachsoju.eventbus.events.BlockUpdateEvent
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.SwapResult
import com.peachsoju.utils.handlers.RightClickHandler
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import java.lang.reflect.Type
import com.peachsoju.utils.handlers.drawLine
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor

object AutoIceFill {

    private val enabled: Boolean get() = config.autoIceFill()

    private var iceFillFloors: IceFillData = loadPatterns()
    private var currentPatterns: ArrayList<Vec3> = ArrayList()
    private var autoPattern: List<Vec3> = emptyList()

    private val LINE_COLOR = Color(0, 255, 255, 0.8f)

    private var isAutoSolving = false
    private var autoStepIndex = 0
    private var waitingForIce = false
    private var expectedIcePos: BlockPos? = null

    var autoEnabled: Boolean
        get() = config.autoIceFill()
        set(value) { config.setAutoIceFill(value) }

    private var autoStarted = false
    private var waitingTicks = 0
    private var waitingForStairDelay = false
    private var stairDelayTicks = 0

    private var currentFloorY: Double = 0.0
    private var consecutiveFailures = 0
    private const val MAX_FAILURES = 10

    // Completion lock - prevents re-execution until world load
    private var completionLocked = false

    // Layer tracking for per-layer retry
    private var currentLayerIndex = 0
    private var layerStartIndices = mutableListOf<Int>() // Stores the starting index of each layer in autoPattern

    fun isAutoEnabled() = autoEnabled

    fun toggleAuto() {
        autoEnabled = !autoEnabled
        RouteUtils.debug("§7Auto Ice Fill: ${if (autoEnabled) "§aON" else "§cOFF"}")
        if (!autoEnabled) stopAuto()
    }

    /**
     * Retry the current layer from its starting point.
     * Can be called when a layer fails to allow the player to retry just that layer.
     */
    fun retryCurrentLayer() {
        if (!isInIceFill()) {
            RouteUtils.debug("§cNot in Ice Fill room!")
            return
        }

        if (completionLocked) {
            RouteUtils.debug("§cIce Fill is locked until world reload!")
            return
        }

        if (layerStartIndices.isEmpty() || currentLayerIndex < 0) {
            RouteUtils.debug("§cNo layer data available to retry!")
            return
        }

        val layerToRetry = currentLayerIndex.coerceIn(0, layerStartIndices.size - 1)
        val retryIndex = layerStartIndices[layerToRetry]

        RouteUtils.debug("§6Retrying layer ${layerToRetry + 1} from step ${retryIndex + 1}")

        // Reset auto state but keep pattern data
        isAutoSolving = false
        autoStarted = false
        waitingForIce = false
        expectedIcePos = null
        autoStepIndex = retryIndex
        waitingTicks = 0
        waitingForStairDelay = false
        stairDelayTicks = 0
        consecutiveFailures = 0

        // Set floor Y to the layer's Y level
        if (retryIndex < autoPattern.size) {
            currentFloorY = autoPattern[retryIndex].y
        }
    }

    /**
     * Retry a specific layer by index (0-based: 0 = layer 1, 1 = layer 2, 2 = layer 3)
     */
    fun retryLayer(layerIndex: Int) {
        if (!isInIceFill()) {
            RouteUtils.debug("§cNot in Ice Fill room!")
            return
        }

        if (completionLocked) {
            RouteUtils.debug("§cIce Fill is locked until world reload!")
            return
        }

        if (layerIndex < 0 || layerIndex >= layerStartIndices.size) {
            RouteUtils.debug("§cInvalid layer index: ${layerIndex + 1}. Available layers: 1-${layerStartIndices.size}")
            return
        }

        currentLayerIndex = layerIndex
        val retryIndex = layerStartIndices[layerIndex]

        RouteUtils.debug("§6Retrying layer ${layerIndex + 1} from step ${retryIndex + 1}")

        // Reset auto state but keep pattern data
        isAutoSolving = false
        autoStarted = false
        waitingForIce = false
        expectedIcePos = null
        autoStepIndex = retryIndex
        waitingTicks = 0
        waitingForStairDelay = false
        stairDelayTicks = 0
        consecutiveFailures = 0

        // Set floor Y to the layer's Y level
        if (retryIndex < autoPattern.size) {
            currentFloorY = autoPattern[retryIndex].y
        }
    }

    private fun stopAuto(reason: String? = null) {
        if (reason != null && isAutoSolving) {
            RouteUtils.debug("§c§lAuto Ice Fill STOPPED: $reason")
            RouteUtils.debug("§eUse retry command to retry layer ${currentLayerIndex + 1}")
        }
        isAutoSolving = false
        autoStarted = false
        waitingForIce = false
        expectedIcePos = null
        autoStepIndex = 0
        waitingTicks = 0
        waitingForStairDelay = false
        stairDelayTicks = 0
        consecutiveFailures = 0
        currentFloorY = 0.0
        // Note: Do NOT reset currentLayerIndex or layerStartIndices here
        // so that retryCurrentLayer() can still work
    }

    private fun isInIceFill(): Boolean {
        return DungeonUtils.currentRoomName == "Ice Fill"
    }

    private fun isOnCorrectFloor(): Boolean {
        val player = mc.player ?: return false
        val playerY = player.y

        if (currentFloorY == 0.0) return true

        val tolerance = 1.5
        return abs(playerY - currentFloorY) <= tolerance
    }

    private fun getExpectedFloorY(): Double {
        if (autoStepIndex >= autoPattern.size) return currentFloorY
        return autoPattern[autoStepIndex].y
    }

    private fun validateState(): Boolean {
        if (!isInIceFill()) {
            stopAuto("Not in Ice Fill room")
            return false
        }

        if (!isOnCorrectFloor()) {
            stopAuto("Player not on correct floor (expected Y≈${currentFloorY.toInt()}, got ${mc.player?.y?.toInt()})")
            return false
        }

        if (consecutiveFailures > MAX_FAILURES) {
            stopAuto("Too many consecutive failures ($consecutiveFailures)")
            return false
        }

        return true
    }

    /**
     * Determines which layer index a step belongs to based on Y coordinate
     */
    private fun getLayerForStep(stepIndex: Int): Int {
        if (stepIndex >= autoPattern.size) return currentLayerIndex
        val y = autoPattern[stepIndex].y
        return when {
            y < 71.0 -> 0  // Layer 1
            y < 72.0 -> 1  // Layer 2
            else -> 2      // Layer 3
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!autoEnabled || autoStarted || autoPattern.isEmpty()) return

        // Check completion lock
        if (completionLocked) return

        if (!isInIceFill()) return

        val player = mc.player ?: return
        val px = player.blockPosition().x
        val pz = player.blockPosition().z

        for (i in autoPattern.indices) {
            val wp = autoPattern[i]
            val wx = floor(wp.x).toInt()
            val wz = floor(wp.z).toInt()
            if (px == wx && pz == wz) {
                val swapResult = RouteUtils.swapToItem("Aspect of the Void")
                if (swapResult == SwapResult.FAIL) {
                    RouteUtils.debug("§cCould not find Aspect of the Void in hotbar!")
                    return
                }
                autoStarted = true
                isAutoSolving = true
                autoStepIndex = i + 1
                waitingForIce = false
                waitingTicks = 0
                expectedIcePos = null
                consecutiveFailures = 0
                currentFloorY = wp.y
                currentLayerIndex = getLayerForStep(i)
                RouteUtils.debug("§aAuto triggered at block $i (layer ${currentLayerIndex + 1}) — next step ${i + 1}/${autoPattern.size}")
                sendNextClick()
                return
            }
        }
    }

    @SubscribeEvent
    fun onTickTimeout(event: TickEvent.End) {
        if (isAutoSolving && !validateState()) return

        if (waitingForStairDelay) {
            stairDelayTicks--
            if (stairDelayTicks <= 0) {
                waitingForStairDelay = false
                if (validateState()) {
                    sendNextClick()
                }
            }
            return
        }

        if (!isAutoSolving || !waitingForIce) return

        waitingTicks++
        if (waitingTicks >= 5) {
            val player = mc.player ?: return
            val playerBlock = player.blockPosition()
            val expectedBlock = expectedIcePos

            if (expectedBlock != null) {
                val actualBlock = mc.level?.getBlockState(expectedBlock)?.block
                RouteUtils.debug("§eExpected ice at $expectedBlock, found: $actualBlock")
                val movedCorrectly = playerBlock.x == expectedBlock.x && playerBlock.z == expectedBlock.z
                if (!movedCorrectly) {
                    consecutiveFailures++
                    RouteUtils.debug("§eExpected: $expectedBlock, Got: $playerBlock (failure #$consecutiveFailures)")

                    if (consecutiveFailures > MAX_FAILURES) {
                        stopAuto("Block placement failed $consecutiveFailures times")
                        return
                    }

                    RouteUtils.debug("§ePlayer didn't move, retrying step ${autoStepIndex}/${autoPattern.size}")
                    autoStepIndex--
                    waitingForIce = false
                    waitingTicks = 0
                    expectedIcePos = null

                    if (validateState()) {
                        sendNextClick()
                    }
                    return
                }
            }

            consecutiveFailures++
            if (consecutiveFailures > MAX_FAILURES) {
                stopAuto("No ice update after multiple attempts")
                return
            }

            RouteUtils.debug("§eNo ice update after 5 ticks, forcing next step (failure #$consecutiveFailures)")
            waitingForIce = false
            waitingTicks = 0
            expectedIcePos = null

            if (validateState()) {
                sendNextClick()
            }
        }
    }

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) = with(event.room) {
        if (this?.data?.name != "Ice Fill") {
            if (isAutoSolving) {
                stopAuto("Left Ice Fill room")
            }
            return@with
        }

        // Don't re-scan if already completed and locked
        if (completionLocked) {
            RouteUtils.debug("§7Ice Fill already completed this session")
            return@with
        }

        if (currentPatterns.isNotEmpty()) return@with

        val patterns = iceFillFloors.easy

        // Reset layer tracking
        layerStartIndices.clear()
        currentLayerIndex = 0

        repeat(3) { index ->
            val floorIdentifiers = iceFillFloors.identifier[index]
            for (patternIndex in floorIdentifiers.indices) {
                if (isRealAir(floorIdentifiers[patternIndex][0]) && !isRealAir(floorIdentifiers[patternIndex][1])) {
                    // Record the start index for this layer
                    layerStartIndices.add(currentPatterns.size)

                    val waypoints = patterns[index][patternIndex].map {
                        Vec3(getRealCoords(it)).add(0.5, 0.1, 0.5)
                    }

                    if (index == 0) {
                        val firstWp = waypoints[0]
                        val entryBlock = listOf(
                            Vec3(firstWp.x - 1, firstWp.y, firstWp.z),
                            Vec3(firstWp.x + 1, firstWp.y, firstWp.z),
                            Vec3(firstWp.x, firstWp.y, firstWp.z - 1),
                            Vec3(firstWp.x, firstWp.y, firstWp.z + 1)
                        ).firstOrNull { candidate ->
                            val bp = BlockPos(floor(candidate.x).toInt(), floor(candidate.y - 0.1).toInt(), floor(candidate.z).toInt())
                            mc.level?.getBlockState(bp)?.block == Blocks.PACKED_ICE ||
                                    mc.level?.getBlockState(bp)?.block == Blocks.ICE
                        }

                        val fullWaypoints = if (entryBlock != null) listOf(entryBlock) + waypoints else waypoints
                        currentPatterns.addAll(fullWaypoints)
                    } else {
                        currentPatterns.addAll(waypoints)
                    }

                    RouteUtils.debug("§aFloor $index pattern $patternIndex matched (${waypoints.size} pts)")
                    return@repeat
                }
            }
            RouteUtils.debug("§cFailed to scan floor $index")
        }

        stupidStairs(this)
        autoPattern = currentPatterns.toList()
        RouteUtils.debug("§7autoPattern: ${autoPattern.size} pts total, ${layerStartIndices.size} layers")
        RouteUtils.debug("§7Layer start indices: $layerStartIndices")
    }

    private fun stupidStairs(room: Room) {
        val rotation = room.rotation
        val updated = ArrayList<Vec3>()
        var done71 = false
        var done72 = false

        for (point in currentPatterns) {
            if (point.y == 71.1 && !done71) {
                updated.add(adjustStair(point, rotation, 70.6))
                done71 = true
            } else if (point.y == 72.1 && !done72) {
                updated.add(adjustStair(point, rotation, 71.6))
                done72 = true
            }
            updated.add(point)
        }
        currentPatterns = updated
    }

    private fun adjustStair(point: Vec3, rotation: Rotations, adjustedY: Double): Vec3 {
        return when (rotation) {
            Rotations.NORTH -> Vec3(point.x, adjustedY, point.z + 0.35)
            Rotations.SOUTH -> Vec3(point.x, adjustedY, point.z - 0.35)
            Rotations.EAST  -> Vec3(point.x - 0.35, adjustedY, point.z)
            Rotations.WEST  -> Vec3(point.x + 0.35, adjustedY, point.z)
            else -> point
        }
    }

    private fun sendNextClick() {
        if (!validateState()) return

        if (autoStepIndex >= autoPattern.size) {
            RouteUtils.debug("§aIce Fill complete!")
            completionLocked = true  // Lock until world reload
            RouteUtils.debug("§7Ice Fill locked until world reload")
            stopAuto()
            return
        }

        val target = autoPattern[autoStepIndex]
        val from = if (autoStepIndex > 0) autoPattern[autoStepIndex - 1] else target
        val yaw = calculateYaw(from, target)
        val isStairStep = target.y == 70.6 || target.y == 71.6

        currentFloorY = target.y

        // Update current layer index based on step
        currentLayerIndex = getLayerForStep(autoStepIndex)

        if (isStairStep) {
            expectedIcePos = null
            waitingForIce = false
            waitingTicks = 0
            RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, yaw, 25f)
            RouteUtils.debug("§7Step ${autoStepIndex + 1}/${autoPattern.size} (L${currentLayerIndex + 1}) yaw=§e${"%.0f".format(yaw)} §7pitch=§e25.0 §6STAIR")
            autoStepIndex++
            stairDelayTicks = 2
            waitingForStairDelay = true
            return
        }

        expectedIcePos = BlockPos(
            floor(target.x).toInt(),
            floor(target.y - 0.1).toInt() - 1,
            floor(target.z).toInt()
        )
        waitingForIce = true

        val prevYaw = if (autoStepIndex > 0) {
            val prevTarget = autoPattern[autoStepIndex - 1]
            val prevFrom = if (autoStepIndex > 1) autoPattern[autoStepIndex - 2] else prevTarget
            calculateYaw(prevFrom, prevTarget)
        } else null

        RouteUtils.debug("§7Current yaw: ${"%.0f".format(yaw)}, Previous yaw: ${prevYaw?.let { "%.0f".format(it) } ?: "none"}")

        RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, yaw, 45F)
        RouteUtils.debug("§7Step ${autoStepIndex + 1}/${autoPattern.size} (L${currentLayerIndex + 1}) yaw=§e${"%.0f".format(yaw)} §7pitch=§e45.0")
        autoStepIndex++
    }

    @SubscribeEvent
    fun onBlockUpdate(event: BlockUpdateEvent) {
        if (!isAutoSolving || !waitingForIce) return

        if (!validateState()) return

        if (event.blockState.block != Blocks.PACKED_ICE) return
        if (event.pos != expectedIcePos) return

        consecutiveFailures = 0

        waitingForIce = false
        waitingTicks = 0
        expectedIcePos = null
        RouteUtils.debug("§7Ice confirmed, sending next click")
        sendNextClick()
    }

    private fun calculateYaw(from: Vec3, to: Vec3): Float {
        val dx = to.x - from.x
        val dz = to.z - from.z
        var yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        while (yaw > 180) yaw -= 360
        while (yaw < -180) yaw += 360
        return snapToCardinal(yaw)
    }

    private fun snapToCardinal(yaw: Float): Float = when {
        yaw > -45 && yaw <= 45 -> 0f
        yaw > 45 && yaw <= 135 -> 90f
        yaw > 135 || yaw <= -135 -> 180f
        else -> -90f
    }

    @SubscribeEvent
    fun onRender(event: RenderEvent.Extract) {
        if (!enabled || currentPatterns.isEmpty()) return
        if (!isInIceFill()) return
        if (completionLocked) return  // Don't render if completed
        event.drawLine(currentPatterns, LINE_COLOR, depth = false, thickness = 2f)
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent) = reset()

    fun reset() {
        currentPatterns.clear()
        autoPattern = emptyList()
        layerStartIndices.clear()
        currentLayerIndex = 0
        completionLocked = false  // Unlock on world change
        stopAuto()
        RouteUtils.debug("§7Ice Fill reset (unlocked)")
    }

    /**
     * Check if Ice Fill is currently locked (completed this session)
     */
    fun isLocked(): Boolean = completionLocked

    /**
     * Get the current layer index (0-based)
     */
    fun getCurrentLayer(): Int = currentLayerIndex

    /**
     * Get total number of layers
     */
    fun getTotalLayers(): Int = layerStartIndices.size

    private fun Room.isRealAir(pos: BlockPos): Boolean =
        mc.level?.getBlockState(getRealCoords(pos))?.isAir == true

    private fun RouteUtils.debug(msg: String) {
        mc.player?.displayClientMessage(Component.literal("§d[PeachSoju] §f$msg"), false)
    }

    private fun loadPatterns(): IceFillData {
        return try {
            val gson = GsonBuilder()
                .registerTypeAdapter(BlockPos::class.java, BlockPosDeserializer())
                .create()
            val stream = AutoIceFill::class.java
                .getResourceAsStream("/assets/peachsoju/Icefillfloors.json")
            stream?.bufferedReader()?.use { gson.fromJson(it, IceFillData::class.java) }
                ?: IceFillData(emptyList(), emptyList(), emptyList()).also {
                    System.err.println("[PeachSoju] Icefillfloors.json not found!")
                }
        } catch (e: Exception) {
            e.printStackTrace()
            IceFillData(emptyList(), emptyList(), emptyList())
        }
    }

    private data class IceFillData(
        val identifier: List<List<List<BlockPos>>>,
        val easy: List<List<List<BlockPos>>>,
        val hard: List<List<List<BlockPos>>>
    )

    private class BlockPosDeserializer : JsonDeserializer<BlockPos> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): BlockPos {
            val obj = json.asJsonObject
            return BlockPos(obj.get("x").asInt, obj.get("y").asInt, obj.get("z").asInt)
        }
    }
}