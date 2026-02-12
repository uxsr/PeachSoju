package com.blowup.eventbus.events;

import com.blowup.eventbus.Event
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

class BlockBreakEvent(val blockPos: BlockPos, val direction: Direction) : Event() {}