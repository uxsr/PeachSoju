package com.peachsoju.modules.impl.autoroutes

import com.peachsoju.modules.impl.autoroutes.data.WaypointNode
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import com.peachsoju.modules.impl.autoroutes.data.WPType
import net.minecraft.world.phys.Vec3

object RouteState {

    private const val ACTION_LOCK_TIMEOUT_TELEPORT_MS = 50L
    private const val ACTION_LOCK_TIMEOUT_DEFAULT_MS = 2000L
    private const val actionLockTimeoutMs = 2000L
    const val ACTION_LOCK_TIMEOUT_MS = actionLockTimeoutMs
    var consumed = 0
    var routeActive = false
    var currentRoomKey: String? = null
    var currentRoom: Room? = null
    var nodeList: List<WaypointNode> = emptyList()
    var previousPosition: Vec3 = Vec3.ZERO
    var awaitingSecrets = 0
    var pendingNodeIndex = -1
    var awaitingSecretConfirmation = false
    var secretConfirmationTicks = 0
    var pendingAwaitNode: WaypointNode? = null
    var pendingAwaitNodeIndex = -1
    var pendingAwaitNodeRoom: Room? = null
    var waitingForTeleport = false
    var awaitingTeleportNodeIndex = -1
    var delayingNode: WaypointNode? = null
    var delayingNodeIndex = -1
    var delayingNodeRoom: Room? = null
    var delayTicksRemaining = 0
    val nodeCooldowns = mutableMapOf<Int, Long>()
    val actionLockedNodes = mutableSetOf<Int>()
    val actionLockTimes = mutableMapOf<Int, Long>()

    fun reset() {
        consumed = 0
        routeActive = false
        awaitingSecrets = 0
        pendingNodeIndex = -1
        awaitingSecretConfirmation = false
        secretConfirmationTicks = 0
        pendingAwaitNode = null
        pendingAwaitNodeIndex = -1
        pendingAwaitNodeRoom = null
        waitingForTeleport = false
        awaitingTeleportNodeIndex = -1
        delayingNode = null
        delayingNodeIndex = -1
        delayingNodeRoom = null
        delayTicksRemaining = 0
        nodeCooldowns.clear()
        actionLockedNodes.clear()
        actionLockTimes.clear()
    }

    fun fullReset() {
        reset()
        currentRoomKey = null
        currentRoom = null
        nodeList = emptyList()
        previousPosition = Vec3.ZERO
    }

    fun getActionLockTimeout(type: WPType, node: WaypointNode? = null): Long {
        if (node != null && hasModifiers(node)) {
            return ACTION_LOCK_TIMEOUT_DEFAULT_MS
        }

        return when (type) {
            WPType.ETHER, WPType.AOTV -> ACTION_LOCK_TIMEOUT_TELEPORT_MS
            else -> ACTION_LOCK_TIMEOUT_DEFAULT_MS
        }
    }

    private fun hasModifiers(node: WaypointNode): Boolean {
        return node.awaitSecret > 0 ||
                node.awaitBat ||
                node.delay > 0
    }

    fun isLocked() = consumed > 0 || waitingForTeleport || awaitingSecretConfirmation
    fun lock() { consumed++ }
    fun unlock() { if (--consumed < 0) consumed = 0 }
    fun isDelaying() = delayTicksRemaining > 0
}