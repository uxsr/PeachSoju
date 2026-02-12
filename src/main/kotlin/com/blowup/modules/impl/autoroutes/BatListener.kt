package com.blowup.modules.impl.autoroutes

import com.blowup.PeachSoju.mc
import com.blowup.eventbus.SubscribeEvent
import com.blowup.eventbus.events.TickEvent
import com.blowup.modules.impl.autoroutes.data.WaypointNode
import com.blowup.utils.RouteUtils
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import net.minecraft.world.entity.ambient.Bat

object BatListener {

    private var isAwaitingBat = false
    private var awaitingNodeIndex = -1
    private var awaitingNode: WaypointNode? = null
    private var awaitingRoom: Room? = null

    fun startWaitingForBat(
        node: WaypointNode,
        index: Int,
        room: Room?
    ) {
        isAwaitingBat = true
        awaitingNodeIndex = index
        awaitingNode = node
        awaitingRoom = room
        RouteUtils.debug("§e[Bat] Waiting for bat spawn...")
    }

    fun cancel() {
        isAwaitingBat = false
        awaitingNodeIndex = -1
        awaitingNode = null
        awaitingRoom = null
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!isAwaitingBat) return

        val player = mc.player ?: return
        val level = mc.level ?: return
        val searchBox = player.boundingBox.inflate(15.0)
        val bats = level.getEntitiesOfClass(Bat::class.java, searchBox)

        if (bats.isNotEmpty()) {
            val distance = player.position().distanceTo(bats[0].position())
            RouteUtils.debug("§a[Bat] Bat detected at distance ${"%.1f".format(distance)}")
            onBatSpawned()
        }
    }

    private fun onBatSpawned() {
        val node = awaitingNode
        val index = awaitingNodeIndex
        val room = awaitingRoom

        if (node != null) {
            RouteUtils.debug("§a[Bat] Bat spawned. Executing node #$index")
            RouteState.waitingForTeleport = true
            RouteState.awaitingTeleportNodeIndex = index
            RouteState.lock()
            Autoroutes.executeNodePublic(node, index, room)
        }

        cancel()
    }
}