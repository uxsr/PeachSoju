package com.peachsoju.eventbus.events;

import com.peachsoju.eventbus.Event
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

class RenderOverlayEvent(val context: GuiGraphics, val partialTicks: DeltaTracker) : Event() {}