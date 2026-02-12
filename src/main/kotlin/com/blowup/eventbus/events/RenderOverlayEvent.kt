package com.blowup.eventbus.events;

import com.blowup.eventbus.Event
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

class RenderOverlayEvent(val context: GuiGraphics, val partialTicks: DeltaTracker) : Event() {}