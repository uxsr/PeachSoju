package com.blowup.eventbus.events;

import com.mojang.blaze3d.platform.Window
import com.blowup.eventbus.Event

class GuiScaleEvent(var scale: Float? = null, var yOffset: Float? = null) : Event()