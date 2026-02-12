package com.peachsoju.eventbus.events;

import com.mojang.blaze3d.platform.InputConstants
import com.peachsoju.eventbus.Event

class KeyInputEvent(val key: InputConstants.Key) : Event() {}
