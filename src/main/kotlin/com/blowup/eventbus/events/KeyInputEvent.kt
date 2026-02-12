package com.blowup.eventbus.events;

import com.mojang.blaze3d.platform.InputConstants
import com.blowup.eventbus.Event

class KeyInputEvent(val key: InputConstants.Key) : Event() {}
