package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event
import net.minecraft.world.entity.Entity

class EntityInteractEvent(val entity: Entity?, val interactionType: InteractionType?): Event() {
    enum class InteractionType {
        ATTACK,
        INTERACT,
        INTERACT_AT
    }
}
