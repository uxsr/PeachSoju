package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

class BlockUpdateEvent(val pos: BlockPos, val blockState: BlockState): Event() {}