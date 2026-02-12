package com.peachsoju

import com.peachsoju.eventbus.EventBus
import com.peachsoju.gui.commands.PeachSojuCommand
import com.peachsoju.gui.commands.RotateCommand
import com.peachsoju.handlers.RenderBatchManager
import com.peachsoju.handlers.RightClickHandler
import com.peachsoju.handlers.SneakHandler
import com.peachsoju.modules.impl.autoroutes.Autoroutes
import com.peachsoju.modules.impl.autoroutes.AutoRoutesCommand
import com.peachsoju.modules.impl.autoroutes.BatListener
import com.peachsoju.modules.impl.autoroutes.BurstMode
import com.peachsoju.modules.impl.autoroutes.SecretListener
import com.peachsoju.modules.impl.autoroutes.NodeManager
import com.peachsoju.modules.impl.fmblocks.FMBlocksCommands
import com.peachsoju.modules.impl.fmblocks.FMBlocksEditMode
import com.peachsoju.modules.impl.fmblocks.FMBlocksManager
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
		FMBlocksCommands.register()
		eventBus.register(FMBlocksManager)
		eventBus.register(FMBlocksEditMode)
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
