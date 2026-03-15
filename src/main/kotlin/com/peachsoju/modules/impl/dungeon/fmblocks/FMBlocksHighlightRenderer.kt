package com.peachsoju.modules.impl.dungeon.fmblocks

import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.handlers.RightClickHandler
import com.peachsoju.utils.handlers.drawFilledBox
import com.odtheking.odin.features.impl.render.Etherwarp
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object FMBlocksHighlightRenderer {

    private val mc = Minecraft.getInstance()

    private var highlightCache = mutableMapOf<Block, FMBlocksHighlights.HighlightColor>()
    private var etherActivateBlocks = mutableSetOf<Block>()
    private var espBlocks = mutableSetOf<Block>()
    private var lastConfigUpdate = 0L

    private val pendingEtherwarps = mutableListOf<Vec3>()
    private var timeSinceEther = 0L
    private var lastActivatedBlock: BlockPos? = null

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!config.fmBlocksEnabled()) return

        updateConfigCache()
        handleEtherActivation()
    }

    private fun updateConfigCache() {
        val now = System.currentTimeMillis()
        if (now - lastConfigUpdate < 2000L && highlightCache.isNotEmpty()) return
        lastConfigUpdate = now

        highlightCache.clear()
        etherActivateBlocks.clear()
        espBlocks.clear()

        val highlights = FMBlocksHighlights.getEnabledHighlights()
        for (highlight in highlights) {
            val loc = ResourceLocation.tryParse(highlight.itemId) ?: continue

            val block = BuiltInRegistries.BLOCK.getValue(loc)
            if (block != Blocks.AIR || highlight.itemId == "minecraft:air") {
                highlightCache[block] = highlight.color
                if (highlight.etherActivate) etherActivateBlocks.add(block)
                if (highlight.esp) espBlocks.add(block)
                continue
            }

            val item = BuiltInRegistries.ITEM.getValue(loc)
            if (item is BlockItem) {
                highlightCache[item.block] = highlight.color
                if (highlight.etherActivate) etherActivateBlocks.add(item.block)
                if (highlight.esp) espBlocks.add(item.block)
            }
        }
    }

    private fun handleEtherActivation() {
        val player = mc.player ?: return

        if (etherActivateBlocks.isEmpty()) return
        if (DungeonUtils.inBoss) return

        if (System.currentTimeMillis() - timeSinceEther > 500L) {
            pendingEtherwarps.clear()
        }

        if (!RouteUtils.isHoldingItem("Aspect of the Void")) return
        if (!player.isShiftKeyDown && !player.isCrouching) return

        val etherwarpResult = if (pendingEtherwarps.isNotEmpty()) {
            Etherwarp.getEtherPos(pendingEtherwarps.last(), 61.0, etherWarp = true)
        } else {
            Etherwarp.getEtherPos(player.position(), 61.0, etherWarp = true)
        }

        if (!etherwarpResult.succeeded) return
        val etherPos = etherwarpResult.pos ?: return
        val level = mc.level ?: return
        val blockAtTarget = level.getBlockState(etherPos).block

        if (blockAtTarget !in etherActivateBlocks) return

        if (lastActivatedBlock == etherPos && (System.currentTimeMillis() - timeSinceEther) <= 500L) return

        RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, player.yRot, player.xRot)
        pendingEtherwarps.add(etherwarpResult.vec3.add(0.5, 0.05 + player.eyeHeight.toDouble(), 0.5))
        timeSinceEther = System.currentTimeMillis()
        lastActivatedBlock = etherPos

        RouteUtils.extraDebug("§b[FMHighlight] Ether-activate fired on ${blockAtTarget.descriptionId} at $etherPos")
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderEvent.Extract) {
        if (!config.fmBlocksEnabled()) return
        if (!DungeonUtils.inDungeons) return
        if (highlightCache.isEmpty()) return

        val roomKey = FMBlocksManager.getCurrentRoomKey() ?: return
        val roomData = FMBlocksManager.getBlocksForRoom(roomKey) ?: return
        val room = if (!DungeonUtils.inBoss) DungeonUtils.currentRoom else null

        for ((blockState, positions) in roomData.blocks) {
            val block = blockState.block
            val highlightColor = highlightCache[block] ?: continue
            val useEsp = block in espBlocks

            val color = Color(
                highlightColor.r,
                highlightColor.g,
                highlightColor.b,
                highlightColor.a
            )

            for (relativePos in positions) {
                val worldPos = FMBlocksManager.relativeToWorld(relativePos, room)
                val aabb = AABB(worldPos)
                event.drawFilledBox(aabb, color, depth = !useEsp)
            }
        }
    }

    fun invalidateCache() {
        lastConfigUpdate = 0L
        pendingEtherwarps.clear()
        lastActivatedBlock = null
        timeSinceEther = 0L
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
        invalidateCache()
    }
}