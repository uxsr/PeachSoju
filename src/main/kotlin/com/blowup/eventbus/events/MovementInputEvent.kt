package com.blowup.eventbus.events;

import com.blowup.eventbus.Event
import net.minecraft.client.player.ClientInput

class MovementInputEvent(var input: ClientInput) : Event()