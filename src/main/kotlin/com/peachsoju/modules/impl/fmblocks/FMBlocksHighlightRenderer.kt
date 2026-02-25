package com.peachsoju.modules.impl.fmblocks

import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.utils.handlers.drawFilledBox
import com.odtheking.odin.features.impl.render.Etherwarp
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.handlers.RightClickHandler
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB

object FMBlocksHighlightRenderer {

    private val mc = Minecraft.getInstance()

    private var highlightCache = mutableMapOf<Block, FMBlocksHighlights.HighlightColor>()
    private var etherActivateBlocks = mutableSetOf<Block>()
    private var cacheVersion = 0L
    private var lastCacheUpdate = 0L

    private val pendingEtherwarps = mutableListOf<net.minecraft.world.phys.Vec3>()
    private var timeSinceEther = 0L
    private var lastActivatedBlock: BlockPos? = null

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!config.fmBlocksEnabled()) return
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

        val player = mc.player ?: return
        val level = mc.level ?: return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCacheUpdate > 500) {
            updateCache()
            lastCacheUpdate = currentTime
        }

        if (highlightCache.isEmpty()) return

        val playerPos = player.blockPosition()
        val scanRadius = 32

        for (x in -scanRadius..scanRadius) {
            for (y in -scanRadius / 2..scanRadius / 2) {
                for (z in -scanRadius..scanRadius) {
                    val pos = BlockPos(playerPos.x + x, playerPos.y + y, playerPos.z + z)
                    val state = level.getBlockState(pos)

                    if (state.isAir) continue

                    val block = state.block
                    val highlightColor = highlightCache[block] ?: continue

                    val aabb = AABB(pos)
                    val color = Color(
                        highlightColor.r,
                        highlightColor.g,
                        highlightColor.b,
                        highlightColor.a
                    )

                    event.drawFilledBox(aabb, color, depth = true)
                }
            }
        }
    }

    private fun updateCache() {
        highlightCache.clear()
        etherActivateBlocks.clear()

        val highlights = FMBlocksHighlights.getEnabledHighlights()
        for (highlight in highlights) {
            val loc = net.minecraft.resources.ResourceLocation.tryParse(highlight.itemId) ?: continue

            val block = BuiltInRegistries.BLOCK.getValue(loc)
            if (block != net.minecraft.world.level.block.Blocks.AIR || highlight.itemId == "minecraft:air") {
                highlightCache[block] = highlight.color
                if (highlight.etherActivate) etherActivateBlocks.add(block)
                continue
            }

            val item = BuiltInRegistries.ITEM.getValue(loc)
            if (item is net.minecraft.world.item.BlockItem) {
                highlightCache[item.block] = highlight.color
                if (highlight.etherActivate) etherActivateBlocks.add(item.block)
            }
        }
    }

    fun invalidateCache() {
        lastCacheUpdate = 0L
        pendingEtherwarps.clear()
        lastActivatedBlock = null
        timeSinceEther = 0L
    }

    @SubscribeEvent
    fun onWorldLoad(event: com.peachsoju.eventbus.events.WorldEvent) {
        pendingEtherwarps.clear()
        lastActivatedBlock = null
        timeSinceEther = 0L
    }
}