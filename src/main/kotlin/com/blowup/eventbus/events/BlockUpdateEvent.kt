package com.blowup.eventbus.events;

import com.blowup.eventbus.Event
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

class BlockUpdateEvent(val pos: BlockPos, val blockState: BlockState): Event() {}