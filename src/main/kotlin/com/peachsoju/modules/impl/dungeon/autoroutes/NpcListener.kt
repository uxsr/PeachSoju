package com.peachsoju.modules.impl.dungeon.autoroutes

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.modules.impl.dungeon.autoroutes.data.WaypointNode
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.handlers.EntityInteractionHandler
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

object NpcListener {

    private const val NPC_SEARCH_RANGE = 4.0
    private const val INTERACTION_COOLDOWN_MS = 500L
    private const val MAX_INTERACTION_ATTEMPTS = 3

    private var isAwaitingNpc = false
    private var awaitingNodeIndex = -1
    private var awaitingNode: WaypointNode? = null
    private var awaitingRoom: Room? = null
    private var lastInteractionTime = 0L
    private var interactionAttempts = 0

    fun isAwaitingNpc(): Boolean = isAwaitingNpc

    fun startWaitingForNpc(node: WaypointNode, index: Int, room: Room?) {
        isAwaitingNpc = true
        awaitingNodeIndex = index
        awaitingNode = node
        awaitingRoom = room
        interactionAttempts = 0
        RouteUtils.debug("§e[NPC] Waiting for NPC interaction...")
    }

    fun cancel() {
        isAwaitingNpc = false
        awaitingNodeIndex = -1
        awaitingNode = null
        awaitingRoom = null
        interactionAttempts = 0
    }

    fun manualTrigger() {
        if (!isAwaitingNpc) {
            RouteUtils.debug("§c[NPC] Manual trigger ignored - not awaiting")
            return
        }
        RouteUtils.debug("§e[NPC] Manual trigger!")
        onNpcInteracted()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!isAwaitingNpc) return

        val now = System.currentTimeMillis()
        if (now - lastInteractionTime < INTERACTION_COOLDOWN_MS) return

        val npc = findNpcInRange() ?: return

        val distance = EntityInteractionHandler.distanceToEntity(npc)
        RouteUtils.debug("§a[NPC] Found NPC: ${getNpcName(npc)} at distance ${"%.1f".format(distance)}")

        val result = EntityInteractionHandler.interact(npc, InteractionHand.MAIN_HAND)
        lastInteractionTime = now
        interactionAttempts++

        if (result.consumesAction()) {
            RouteUtils.debug("§a[NPC] Successfully interacted with ${getNpcName(npc)}")
            onNpcInteracted()
        } else if (interactionAttempts >= MAX_INTERACTION_ATTEMPTS) {
            RouteUtils.debug("§c[NPC] Max attempts reached, forcing continue")
            onNpcInteracted()
        } else {
            RouteUtils.debug("§e[NPC] Interaction attempt $interactionAttempts/$MAX_INTERACTION_ATTEMPTS")
        }
    }

    private fun findNpcInRange(): Entity? {
        return EntityInteractionHandler.findNearestEntity<Entity>(NPC_SEARCH_RANGE) { entity ->
            isValidNpc(entity)
        }
    }

    private fun isValidNpc(entity: Entity): Boolean {
        if (entity is Player) return false

        return when (entity) {
            is ArmorStand -> entity.hasCustomName()
            else -> entity.hasCustomName()
        }
    }

    private fun getNpcName(entity: Entity): String {
        return entity.customName?.string ?: entity.name.string
    }

    private fun onNpcInteracted() {
        val node = awaitingNode
        val index = awaitingNodeIndex
        val room = awaitingRoom

        if (node != null) {
            RouteUtils.debug("§a[NPC] NPC interacted. Executing node #$index")
            RouteState.waitingForTeleport = true
            RouteState.awaitingTeleportNodeIndex = index
            RouteState.lock()
            Autoroutes.executeNodePublic(node, index, room)
        }

        cancel()
    }

    fun getAwaitingNodeIndex(): Int = awaitingNodeIndex
    fun getAwaitingNode(): WaypointNode? = awaitingNode
}