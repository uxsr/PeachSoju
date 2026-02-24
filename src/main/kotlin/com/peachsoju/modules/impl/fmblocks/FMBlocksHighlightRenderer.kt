package com.peachsoju.modules.impl.fmblocks

import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.utils.handlers.drawFilledBox
import com.odtheking.odin.utils.Color
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB

object FMBlocksHighlightRenderer {

    private val mc = Minecraft.getInstance()

    private var highlightCache = mutableMapOf<Block, FMBlocksHighlights.HighlightColor>()
    private var cacheVersion = 0L
    private var lastCacheUpdate = 0L

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

                    event.drawFilledBox(aabb, color, depth = false)
                }
            }
        }
    }

    private fun updateCache() {
        highlightCache.clear()

        val highlights = FMBlocksHighlights.getEnabledHighlights()
        for (highlight in highlights) {
            val loc = net.minecraft.resources.ResourceLocation.tryParse(highlight.itemId) ?: continue

            val block = BuiltInRegistries.BLOCK.getValue(loc)
            if (block != net.minecraft.world.level.block.Blocks.AIR || highlight.itemId == "minecraft:air") {
                highlightCache[block] = highlight.color
                continue
            }

            val item = BuiltInRegistries.ITEM.getValue(loc)
            if (item is net.minecraft.world.item.BlockItem) {
                highlightCache[item.block] = highlight.color
            }
        }
    }

    fun invalidateCache() {
        lastCacheUpdate = 0L
    }
}