//Not registered use sex5 aura instead

package com.blowup.modules

import com.blowup.PeachSoju.mc
import com.odtheking.odin.utils.equalsOneOf
import com.blowup.eventbus.SubscribeEvent
import com.blowup.eventbus.events.GuiEvent
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object AutoClose {
    @SubscribeEvent
    fun onGuiOpen(event: GuiEvent.Open) {
//        if (!DungeonUtils.inDungeons) return

        val chest = (event.screen as? AbstractContainerScreen<*>) ?: return

        if (chest.title?.string.equalsOneOf("Chest", "Large Chest"))
            mc.player?.closeContainer()
    }
}