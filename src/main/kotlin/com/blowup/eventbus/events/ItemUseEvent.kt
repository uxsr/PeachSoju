package com.blowup.eventbus.events;

import com.blowup.eventbus.Event
import net.minecraft.world.phys.BlockHitResult

class ItemUseEvent(val hitResult: BlockHitResult) : Event()