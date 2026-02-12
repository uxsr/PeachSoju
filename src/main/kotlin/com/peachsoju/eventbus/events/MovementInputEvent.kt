package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event
import net.minecraft.client.player.ClientInput

class MovementInputEvent(var input: ClientInput) : Event()