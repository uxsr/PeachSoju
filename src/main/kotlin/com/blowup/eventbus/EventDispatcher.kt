package com.blowup.eventbus;
import com.blowup.PeachSoju
import com.blowup.PeachSoju.mc
import com.blowup.eventbus.events.GuiEvent
import com.blowup.eventbus.events.RenderEvent
import com.blowup.eventbus.events.RenderOverlayEvent
import com.blowup.eventbus.events.TickEvent
import com.blowup.eventbus.events.WorldEvent
import com.blowup.handlers.RenderBatchManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics

object EventDispatcher {

    fun initialize() {
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            if (mc.player == null) return@register

            PeachSoju.eventBus.post(TickEvent.Start())
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (mc.player == null) return@register

            PeachSoju.eventBus.post(TickEvent.End())
        }

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register { _, _ ->
            PeachSoju.eventBus.post(WorldEvent())
        }

        WorldRenderEvents.END_EXTRACTION.register { handler ->
            mc.level?.let { PeachSoju.eventBus.post(RenderEvent.Extract(handler, RenderBatchManager.renderConsumer)) }
        }

        WorldRenderEvents.END_MAIN.register { context ->
            mc.level?.let { PeachSoju.eventBus.post(RenderEvent.Last(context)) }
        }

        ScreenEvents.AFTER_INIT.register { client, screen, scaledWidth, scaledHeight ->
            PeachSoju.eventBus.post(GuiEvent.Open(screen))
        }

    }

    fun render(context: GuiGraphics, tickCounter: DeltaTracker) {
        if (mc.level == null || mc.player == null) return
        context.pose().pushMatrix()
        val sf = mc.window.guiScale.toFloat()

        PeachSoju.eventBus.post(RenderOverlayEvent(context, tickCounter))

        context.pose().popMatrix()
    }
}