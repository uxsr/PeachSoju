package com.peachsoju.modules.impl.dungeon.autoroutes

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.modules.impl.dungeon.autoroutes.data.WaypointNode
import com.peachsoju.utils.RouteUtils
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import net.minecraft.world.entity.ai.attributes.Attributes
import kotlin.math.*

/**
 * AlignmentExecutor - Physics-based precision alignment.
 *
 * Core insight: Instead of trying to walk exact distances, we simulate
 * ALL possible actions each tick and pick the one that lands us closest
 * to the target after coasting to a stop.
 *
 * Possible actions each tick:
 * - NO_INPUT: Just coast (apply friction)
 * - FORWARD: Press W (accelerate toward target)
 * - BACKWARD: Press S (brake / reverse)
 * - BRAKE: Press opposite of velocity direction (FullStop style)
 *
 * Each tick we simulate all options, coast each to rest, and pick
 * the action that results in minimum distance to target.
 */
object AlignmentExecutor {

    // ============== PHYSICS CONSTANTS (from LivingEntity.java) ==============
    private const val GROUND_FRICTION = 0.546
    private const val VELOCITY_THRESHOLD = 0.003
    private const val SNEAK_ACCEL = 0.04  // Approximate sneak acceleration per tick

    // Thresholds
    private const val CENTER_TOLERANCE = 0.025  // Success if within this
    private const val STUCK_THRESHOLD = 0.0001  // Position change below this = stuck

    // Timeouts
    private const val MAX_TICKS = 120
    private const val MAX_STUCK_TICKS = 10

    // ============== ACTION ENUM ==============
    enum class Action {
        NO_INPUT,   // Coast only
        FORWARD,    // W key (toward target)
        BACKWARD,   // S key (away from target)
        BRAKE       // Opposite of velocity (FullStop style)
    }

    // ============== STATE ==============
    private var active = false
    private var aligningNode: WaypointNode? = null
    private var aligningRoom: Room? = null
    private var aligningNodeIndex = -1

    private var targetX = 0.5
    private var targetZ = 0.5
    private var targetYaw = 0f

    private var tickCount = 0
    private var lastPosX = 0.0
    private var lastPosZ = 0.0
    private var stuckTicks = 0
    private var currentAction = Action.NO_INPUT

    // ============== PUBLIC API ==============
    fun isAligning(): Boolean = active
    fun getAwaitingNodeIndex(): Int = aligningNodeIndex
    fun getAwaitingNode(): WaypointNode? = aligningNode

    fun startAlignment(node: WaypointNode, index: Int, room: Room?) {
        val player = mc.player ?: return

        val nodeWorldPos = RouteUtils.getNodeWorldPosition(node, room)
        targetX = floor(nodeWorldPos.x) + 0.5
        targetZ = floor(nodeWorldPos.z) + 0.5

        aligningNode = node
        aligningRoom = room
        aligningNodeIndex = index

        // Calculate yaw to face target
        val dx = targetX - player.x
        val dz = targetZ - player.z
        targetYaw = Math.toDegrees(atan2(-dx, dz)).toFloat()

        // Snap yaw immediately
        player.yRot = targetYaw
        player.yHeadRot = targetYaw
        player.yBodyRot = targetYaw

        tickCount = 0
        lastPosX = player.x
        lastPosZ = player.z
        stuckTicks = 0
        currentAction = Action.NO_INPUT
        active = true

        val dist = sqrt(dx * dx + dz * dz)
        val vel = sqrt(player.deltaMovement.x.pow(2) + player.deltaMovement.z.pow(2))

        RouteUtils.debug("§b[Align] === START ===")
        RouteUtils.debug("§b[Align] Target: (${"%.3f".format(targetX)}, ${"%.3f".format(targetZ)})")
        RouteUtils.debug("§b[Align] Dist: ${"%.4f".format(dist)}, Vel: ${"%.4f".format(vel)}")

        RouteState.lock()
    }

    fun cancel() {
        if (!active) return
        RouteUtils.debug("§c[Align] Cancelled")
        releaseAllKeys()
        active = false
        aligningNode = null
        aligningRoom = null
        aligningNodeIndex = -1
        RouteState.unlock()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!active) return
        val player = mc.player ?: run { cancel(); return }

        tickCount++

        // Get current state
        val posX = player.x
        val posZ = player.z
        val velX = player.deltaMovement.x
        val velZ = player.deltaMovement.z

        val dx = targetX - posX
        val dz = targetZ - posZ
        val dist = sqrt(dx * dx + dz * dz)
        val vel = sqrt(velX * velX + velZ * velZ)

        // Check success
        if (dist < CENTER_TOLERANCE && vel < VELOCITY_THRESHOLD) {
            RouteUtils.debug("§a[Align] === SUCCESS ===")
            RouteUtils.debug("§a[Align] Final dist: ${"%.4f".format(dist)}, ticks: $tickCount")
            finishAlignment(true)
            return
        }

        // Check timeout
        if (tickCount > MAX_TICKS) {
            RouteUtils.debug("§c[Align] Timeout!")
            finishAlignment(false)
            return
        }

        // Check stuck
        val posDelta = sqrt((posX - lastPosX).pow(2) + (posZ - lastPosZ).pow(2))
        if (posDelta < STUCK_THRESHOLD && vel < VELOCITY_THRESHOLD) {
            stuckTicks++
            if (stuckTicks > MAX_STUCK_TICKS) {
                // If stuck and close enough, accept it
                if (dist < 0.1) {
                    RouteUtils.debug("§e[Align] Stuck but close enough (${"%.4f".format(dist)})")
                    finishAlignment(true)
                } else {
                    RouteUtils.debug("§c[Align] Stuck!")
                    finishAlignment(false)
                }
                return
            }
        } else {
            stuckTicks = 0
        }
        lastPosX = posX
        lastPosZ = posZ

        // Re-snap yaw if needed (in case we drifted)
        val currentYawToTarget = Math.toDegrees(atan2(-dx, dz)).toFloat()
        if (abs(angleDiff(player.yRot, currentYawToTarget)) > 5) {
            targetYaw = currentYawToTarget
            player.yRot = targetYaw
            player.yHeadRot = targetYaw
            player.yBodyRot = targetYaw
        }

        // Decide best action by simulating all options
        val bestAction = findBestAction(posX, posZ, velX, velZ)
        currentAction = bestAction

        // Apply action
        applyAction(bestAction, velX, velZ)

        // Debug output (less verbose)
        if (tickCount % 5 == 0 || tickCount <= 3) {
            RouteUtils.debug("§8[Align] t=$tickCount: dist=${"%.4f".format(dist)}, vel=${"%.4f".format(vel)}, action=$bestAction")
        }
    }

    // ============== SIMULATION ==============

    /**
     * Find the action that results in landing closest to target.
     */
    private fun findBestAction(posX: Double, posZ: Double, velX: Double, velZ: Double): Action {
        var bestAction = Action.NO_INPUT
        var bestDist = Double.MAX_VALUE

        for (action in Action.values()) {
            val (finalX, finalZ) = simulateAction(posX, posZ, velX, velZ, action)
            val dist = sqrt((finalX - targetX).pow(2) + (finalZ - targetZ).pow(2))

            if (dist < bestDist) {
                bestDist = dist
                bestAction = action
            }
        }

        return bestAction
    }

    /**
     * Simulate one tick of action, then coast to rest.
     * Returns final resting position.
     */
    private fun simulateAction(
        posX: Double, posZ: Double,
        velX: Double, velZ: Double,
        action: Action
    ): Pair<Double, Double> {
        // Calculate acceleration based on action
        val yawRad = Math.toRadians(targetYaw.toDouble())
        val forwardX = -sin(yawRad)
        val forwardZ = cos(yawRad)

        var accelX = 0.0
        var accelZ = 0.0

        when (action) {
            Action.NO_INPUT -> {
                // No acceleration
            }
            Action.FORWARD -> {
                accelX = forwardX * SNEAK_ACCEL
                accelZ = forwardZ * SNEAK_ACCEL
            }
            Action.BACKWARD -> {
                accelX = -forwardX * SNEAK_ACCEL
                accelZ = -forwardZ * SNEAK_ACCEL
            }
            Action.BRAKE -> {
                // Opposite of velocity direction
                val velMag = sqrt(velX * velX + velZ * velZ)
                if (velMag > VELOCITY_THRESHOLD) {
                    accelX = -(velX / velMag) * SNEAK_ACCEL
                    accelZ = -(velZ / velMag) * SNEAK_ACCEL
                }
            }
        }

        // Apply one tick: vel = (vel + accel) * friction
        var newVelX = (velX + accelX) * GROUND_FRICTION
        var newVelZ = (velZ + accelZ) * GROUND_FRICTION

        // Zero small velocities
        if (abs(newVelX) < VELOCITY_THRESHOLD) newVelX = 0.0
        if (abs(newVelZ) < VELOCITY_THRESHOLD) newVelZ = 0.0

        // Update position
        var newPosX = posX + newVelX
        var newPosZ = posZ + newVelZ

        // Now coast to rest
        return simulateCoast(newPosX, newPosZ, newVelX, newVelZ)
    }

    /**
     * Simulate coasting until stopped.
     */
    private fun simulateCoast(
        startX: Double, startZ: Double,
        startVelX: Double, startVelZ: Double
    ): Pair<Double, Double> {
        var x = startX
        var z = startZ
        var vx = startVelX
        var vz = startVelZ

        repeat(100) {
            vx *= GROUND_FRICTION
            vz *= GROUND_FRICTION

            if (abs(vx) < VELOCITY_THRESHOLD) vx = 0.0
            if (abs(vz) < VELOCITY_THRESHOLD) vz = 0.0

            x += vx
            z += vz

            if (vx == 0.0 && vz == 0.0) return Pair(x, z)
        }

        return Pair(x, z)
    }

    // ============== INPUT HANDLING ==============

    private fun applyAction(action: Action, velX: Double, velZ: Double) {
        releaseAllKeys()

        when (action) {
            Action.NO_INPUT -> {
                // Nothing pressed - just coast
            }
            Action.FORWARD -> {
                mc.options.keyUp.setDown(true)
                mc.options.keyShift.setDown(true)
            }
            Action.BACKWARD -> {
                mc.options.keyDown.setDown(true)
                mc.options.keyShift.setDown(true)
            }
            Action.BRAKE -> {
                // Press opposite of velocity direction (like FullStop)
                val player = mc.player ?: return
                val yawRad = Math.toRadians(player.yRot.toDouble())
                val sinY = sin(yawRad)
                val cosY = cos(yawRad)

                // Convert world velocity to local
                val localForward = -velX * sinY + velZ * cosY
                val localStrafe = velX * cosY + velZ * sinY

                val threshold = 0.005
                mc.options.keyUp.setDown(localForward < -threshold)
                mc.options.keyDown.setDown(localForward > threshold)
                mc.options.keyLeft.setDown(localStrafe < -threshold)
                mc.options.keyRight.setDown(localStrafe > threshold)
                mc.options.keyShift.setDown(true)
            }
        }

        mc.options.keySprint.setDown(false)
    }

    // ============== UTILITIES ==============

    private fun angleDiff(a: Float, b: Float): Float {
        var diff = (a - b) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    private fun releaseAllKeys() {
        mc.options.keyUp.setDown(false)
        mc.options.keyDown.setDown(false)
        mc.options.keyLeft.setDown(false)
        mc.options.keyRight.setDown(false)
        mc.options.keySprint.setDown(false)
        mc.options.keyShift.setDown(false)
    }

    private fun finishAlignment(success: Boolean) {
        releaseAllKeys()
        val player = mc.player

        if (player != null) {
            val dist = sqrt((player.x - targetX).pow(2) + (player.z - targetZ).pow(2))
            val status = if (success) "§a" else "§c"
            RouteUtils.debug("$status[Align] Final pos: (${"%.4f".format(player.x)}, ${"%.4f".format(player.z)})")
            RouteUtils.debug("$status[Align] Error: ${"%.4f".format(dist)} blocks in $tickCount ticks")
        }

        active = false
        aligningNode = null
        aligningRoom = null
        aligningNodeIndex = -1
        RouteState.unlock()
    }
}