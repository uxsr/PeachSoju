package com.blowup.eventbus.events;

import com.blowup.eventbus.Event

abstract class PlayerUpdateEvent : Event() {

    class Yaw(var yaw: Float) : PlayerUpdateEvent()

    class Pitch(var pitch: Float) : PlayerUpdateEvent()

    class Pre() : PlayerUpdateEvent()
}
