package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event

class MouseEvent(
    val button: Int,
    val action: Int,
    val mods: Int
) : Event()