package com.blowup

import com.blowup.eventbus.EventBus
import com.blowup.gui.commands.PeachSojuCommand
import com.blowup.gui.commands.RotateCommand
import com.blowup.handlers.RenderBatchManager
import com.blowup.handlers.RightClickHandler
import com.blowup.handlers.SneakHandler
import com.blowup.modules.impl.autoroutes.Autoroutes
import com.blowup.modules.impl.autoroutes.AutoRoutesCommand
import com.blowup.modules.impl.autoroutes.BatListener
import com.blowup.modules.impl.autoroutes.BurstMode
import com.blowup.modules.impl.autoroutes.SecretListener
import com.blowup.modules.impl.autoroutes.NodeManager
import net.fabricmc.api.ModInitializer
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

object PeachSoju : ModInitializer {
	private val logger = LoggerFactory.getLogger("PeachSoju")
	val mc: Minecraft get() = Minecraft.getInstance()
	val eventBus = EventBus()

	override fun onInitialize() {
		config.load()
		AutoRoutesCommand.register()
		PeachSojuCommand.register()
		RotateCommand.register()
		eventBus.register(NodeManager)
		eventBus.register(Autoroutes)
		eventBus.register(SecretListener)
		eventBus.register(BatListener)
		eventBus.register(BurstMode)
		eventBus.register(RenderBatchManager)
		eventBus.register(RightClickHandler)
		eventBus.register(SneakHandler)
//		eventBus.register(AutoClose)
//		eventBus.register(SecretAura)
	}
}
