package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event

abstract class TeleportEvent() : Event() {

    class Pre() : TeleportEvent()

    class Post() : TeleportEvent()
}