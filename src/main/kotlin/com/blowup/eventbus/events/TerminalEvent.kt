package com.blowup.eventbus.events;

import com.blowup.eventbus.Event

abstract class TerminalEvent : Event() {

    class Open() : Event()

    class Close() : Event()
}
