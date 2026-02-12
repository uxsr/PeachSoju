package com.blowup.eventbus.events;

import com.blowup.eventbus.Event

abstract class TeleportEvent() : Event() {

    class Pre() : TeleportEvent()

    class Post() : TeleportEvent()
}