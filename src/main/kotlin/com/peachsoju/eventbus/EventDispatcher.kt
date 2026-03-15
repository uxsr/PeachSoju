package com.peachsoju.eventbus;
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.peachsoju.PeachSoju
import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.events.BlockUpdateEvent
import com.peachsoju.eventbus.events.GuiEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.eventbus.events.RenderOverlayEvent
import com.peachsoju.eventbus.events.RoomEnterEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.modules.impl.dungeon.autoroutes.SecretListener
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.handlers.RenderBatchManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.Entity

object EventDispatcher {

    var lastRoom: com.odtheking.odin.utils.skyblock.dungeon.tiles.Room? = null

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

        ClientTickEvents.START_CLIENT_TICK.register { _ ->
            if (mc.player == null) return@register
            val current = DungeonUtils.currentRoom
            if (current != lastRoom) {
                lastRoom = current
                PeachSoju.eventBus.post(RoomEnterEvent(current))
            }
        }

    }

    fun render(context: GuiGraphics, tickCounter: DeltaTracker) {
        if (mc.level == null || mc.player == null) return
        context.pose().pushMatrix()
        val sf = mc.window.guiScale.toFloat()

        PeachSoju.eventBus.post(RenderOverlayEvent(context, tickCounter))

        context.pose().popMatrix()
    }

    fun onBlockUpdate(pos: BlockPos, blockState: BlockState) {
        PeachSoju.eventBus.post(BlockUpdateEvent(pos, blockState))
    }

}