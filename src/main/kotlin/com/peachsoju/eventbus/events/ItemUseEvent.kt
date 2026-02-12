package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event
import net.minecraft.world.phys.BlockHitResult

class ItemUseEvent(val hitResult: BlockHitResult) : Event()