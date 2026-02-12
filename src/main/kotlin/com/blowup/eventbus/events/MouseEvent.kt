package com.blowup.eventbus.events;

import com.blowup.eventbus.Event

class MouseEvent(
    val button: Int,
    val action: Int,
    val mods: Int
) : Event()