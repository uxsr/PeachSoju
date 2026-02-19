package com.peachsoju.modules.impl.autoroutes

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.utils.RouteUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SkullBlock
import net.minecraft.world.level.block.entity.SkullBlockEntity

object SecretListener {

    private var currentAwaitType: String = "any"
    private val recentItemPickups = mutableListOf<Pair<Long, String>>()
    private const val ITEM_BUFFER_MS = 250L
    private val recentClicks = mutableMapOf<BlockPos, Long>()
    private const val CLICK_COOLDOWN_MS = 500L
    private const val WITHER_ESSENCE_UUID = "e0f3e929-869e-3dca-9504-54c666ee6f23"

    fun setAwaitType(awaitType: String) {
        currentAwaitType = awaitType
    }

    private fun onSecretFound(secretInfo: String, secretType: String = "chest") {
        RouteUtils.debug("§d[Secret] onSecretFound called: info=$secretInfo, type=$secretType, awaiting=${RouteState.awaitingSecrets}, awaitType=$currentAwaitType")
        if (RouteState.awaitingSecrets <= 0) return
        if (currentAwaitType != "any" && currentAwaitType != secretType) {
            RouteUtils.debug("§e[Secret] Ignoring $secretType, waiting for $currentAwaitType")
            return
        }

        RouteState.awaitingSecrets--
        RouteUtils.debug("§a✓ Secret found! ($secretInfo) - ${RouteState.awaitingSecrets} remaining")

        if (RouteState.awaitingSecrets <= 0) {
            val index = RouteState.pendingNodeIndex
            val node = RouteState.nodeList.getOrNull(index)
            val room = RouteState.currentRoom

            if (node != null) {
                if (DungeonBreakerListener.isAwaitingDb() && DungeonBreakerListener.isAlsoAwaitingSecrets()) {
                    RouteUtils.debug("§a§lAll secrets collected! Checking DB blocks...")
                    DungeonBreakerListener.checkCombinedConditions()
                } else {
                    RouteUtils.debug("§a§lAll secrets collected! going immediately")

                    RouteState.awaitingSecretConfirmation = true
                    RouteState.secretConfirmationTicks = 3
                    RouteState.pendingAwaitNode = node
                    RouteState.pendingAwaitNodeIndex = index
                    RouteState.pendingAwaitNodeRoom = room

                    RouteState.waitingForTeleport = true
                    RouteState.awaitingTeleportNodeIndex = index
                }
            }

            RouteState.pendingNodeIndex = -1
            currentAwaitType = "any"
        }
    }

    @SubscribeEvent
    fun onPacketSend(event: PacketEvent.Send) {
        val packet = event.packet

        if (packet is ServerboundUseItemOnPacket) {
            if (RouteState.awaitingSecrets <= 0) return

            val level = mc.level ?: return
            val blockPos = packet.hitResult.blockPos
            val now = System.currentTimeMillis()
            val lastClick = recentClicks[blockPos]
            if (lastClick != null && now - lastClick < CLICK_COOLDOWN_MS) {
                return
            }
            recentClicks[blockPos] = now

            recentClicks.entries.removeIf { now - it.value > CLICK_COOLDOWN_MS * 2 }

            val state = level.getBlockState(blockPos)
            val block = state.block

            when {
                block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST -> {
                    RouteUtils.debug("§e[SecretListener] Chest interaction detected at $blockPos")
                    onSecretFound("Chest at $blockPos", "chest")
                }
                block == Blocks.LEVER -> {
                    RouteUtils.debug("§e[SecretListener] Lever interaction detected at $blockPos")
                    onSecretFound("Lever at $blockPos", "lever")
                }
                block is SkullBlock -> {
                    val blockEntity = level.getBlockEntity(blockPos) as? SkullBlockEntity
                    val uuid = blockEntity?.ownerProfile?.partialProfile()?.id?.toString()
                    if (uuid == WITHER_ESSENCE_UUID) {
                        RouteUtils.debug("§e[SecretListener] Skull interaction detected at $blockPos")
                        onSecretFound("Skull at $blockPos", "chest") //im lazy
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        val packet = event.packet
        if (packet is ClientboundTakeItemEntityPacket) {
            val player = mc.player ?: return

            if (packet.playerId == player.id) {
                val now = System.currentTimeMillis()
                recentItemPickups.add(now to "Item pickup")
                recentItemPickups.removeIf { now - it.first > ITEM_BUFFER_MS }
                onSecretFound("Item pickup", "item")
            }
        }
    }

    fun clearClickHistory() {
        recentClicks.clear()
    }

    fun checkBufferedItems() {
        if (RouteState.awaitingSecrets <= 0) return
        if (currentAwaitType != "any" && currentAwaitType != "item") return

        val now = System.currentTimeMillis()
        recentItemPickups.removeIf { now - it.first > ITEM_BUFFER_MS }
        if (recentItemPickups.isNotEmpty()) {
            RouteUtils.debug("§e[Secret] Found ${recentItemPickups.size} buffered item pickup(s)")
            val pickup = recentItemPickups.removeFirst()
            onSecretFound(pickup.second, "item")
        }
    }

    fun manualTrigger() {
        if (RouteState.awaitingSecrets <= 0) {
            RouteUtils.debug("§c[Secret] Manual trigger ignored - not awaiting")
            return
        }
        RouteUtils.debug("§e[Secret] Manual trigger!")
        onSecretFound("Manual trigger", currentAwaitType)
    }
}