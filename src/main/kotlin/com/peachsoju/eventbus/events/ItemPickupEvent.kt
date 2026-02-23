package com.peachsoju.eventbus.events

import com.peachsoju.eventbus.Event
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack

class ItemPickupEvent(
    val itemEntity: ItemEntity,
    val itemStack: ItemStack
) : Event()