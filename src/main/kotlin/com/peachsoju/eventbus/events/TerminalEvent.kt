package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event

abstract class TerminalEvent : Event() {

    class Open() : Event()

    class Close() : Event()
}
