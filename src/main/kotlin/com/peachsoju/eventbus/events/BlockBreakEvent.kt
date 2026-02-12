package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class BlockBreakEvent(val blockPos: BlockPos, val direction: Direction) : Event() {}