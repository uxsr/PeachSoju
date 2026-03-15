package com.peachsoju.modules.impl.dungeon.autoroutes

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.utils.RouteUtils
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ChestDelay {

    private const val DELAY_MS = 5000L

    private val recentlyPlaced = ConcurrentHashMap<BlockPos, BlockState>()

    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "ChestDelay-Scheduler").apply { isDaemon = true }
    }

    @SubscribeEvent
    fun onPacketSend(event: PacketEvent.Send) {
        val packet = event.packet

        if (packet is ServerboundUseItemOnPacket) {
            val hitPos = packet.hitResult.blockPos
            val direction = packet.hitResult.direction
            val pos = hitPos.relative(direction)

            mc.execute {
                val level = mc.level ?: return@execute
                val state = level.getBlockState(pos)
                if (!state.isAir) {
                    recentlyPlaced[pos] = state
                    RouteUtils.debug("§a[ChestDelay] Tracked: $pos -> ${state.block}")
                }
            }

            executor.schedule({
                recentlyPlaced.remove(pos)
            }, 2000, TimeUnit.MILLISECONDS)
        }
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        val packet = event.packet

        if (packet is ClientboundBlockUpdatePacket) {
            val pos = packet.pos
            val newState = packet.blockState

            val placedState = recentlyPlaced[pos]
            if (newState.isAir && placedState != null) {
                event.cancelled = true

                mc.execute {
                    val level = mc.level ?: return@execute
                    level.setBlockAndUpdate(pos, placedState)
                    RouteUtils.debug("§e[ChestDelay] Restored block at $pos, delaying ${DELAY_MS}ms")
                }

                executor.schedule({
                    mc.execute {
                        try {
                            val level = mc.level ?: return@execute
                            level.setBlockAndUpdate(pos, newState)
                            recentlyPlaced.remove(pos)
                            RouteUtils.debug("§c[ChestDelay] Removed block at $pos")
                        } catch (e: Exception) {}
                    }
                }, DELAY_MS, TimeUnit.MILLISECONDS)
            }
        }

        if (packet is ClientboundSectionBlocksUpdatePacket) {
            val toRestore = mutableListOf<Pair<BlockPos, BlockState>>()
            val toRemoveLater = mutableListOf<Pair<BlockPos, BlockState>>()

            packet.runUpdates { pos, newState ->
                val blockPos = BlockPos(pos)
                val placedState = recentlyPlaced[blockPos]
                if (newState.isAir && placedState != null) {
                    toRestore.add(blockPos to placedState)
                    toRemoveLater.add(blockPos to newState)
                }
            }

            if (toRestore.isNotEmpty()) {
                event.cancelled = true

                mc.execute {
                    val level = mc.level ?: return@execute
                    toRestore.forEach { (pos, state) ->
                        level.setBlockAndUpdate(pos, state)
                    }
                    RouteUtils.debug("§e[ChestDelay] Restored ${toRestore.size} blocks")
                }

                executor.schedule({
                    mc.execute {
                        try {
                            val level = mc.level ?: return@execute
                            toRemoveLater.forEach { (pos, state) ->
                                level.setBlockAndUpdate(pos, state)
                                recentlyPlaced.remove(pos)
                            }
                            RouteUtils.debug("§c[ChestDelay] Removed ${toRemoveLater.size} blocks")
                        } catch (e: Exception) {}
                    }
                }, DELAY_MS, TimeUnit.MILLISECONDS)
            }
        }
    }

    fun clear() {
        recentlyPlaced.clear()
    }
}