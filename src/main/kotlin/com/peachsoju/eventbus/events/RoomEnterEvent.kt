package com.peachsoju.eventbus.events

import com.peachsoju.eventbus.Event
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room

class RoomEnterEvent(val room: Room?) : Event()