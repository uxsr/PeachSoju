package com.blowup.modules.impl.fmblocks

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

data class FmBlock(
    val pos: BlockPos,
    val state: BlockState,
    val isGhostBlock: Boolean = false
)

data class FMBlocksRoomData(
    val blocks: MutableMap<BlockState, MutableSet<BlockPos>> = mutableMapOf()
) {
    fun isEmpty() = blocks.isEmpty() || blocks.all { it.value.isEmpty() }

    fun addBlock(pos: BlockPos, state: BlockState) {
        blocks.getOrPut(state) { mutableSetOf() }.add(pos)
    }

    fun removeBlock(pos: BlockPos): Boolean = blocks.values.any { it.remove(pos) }

    fun hasBlockAt(pos: BlockPos): Boolean = blocks.values.any { pos in it }

    fun getBlockAt(pos: BlockPos): BlockState? = blocks.entries.find { pos in it.value }?.key

    fun getAllBlocks(): List<FmBlock> =
        blocks.flatMap { (state, positions) -> positions.map { pos -> FmBlock(pos, state, state.isAir) } }

    fun clear() {
        blocks.clear()
    }
}

enum class FmEditAction {
    PLACE_CUSTOM_BLOCK,
    REMOVE_CUSTOM_BLOCK,
    ADD_GHOST_BLOCK,
    REMOVE_GHOST_BLOCK
}
