package com.peachsoju

import com.peachsoju.eventbus.EventBus
import com.peachsoju.gui.commands.PeachSojuCommand
import com.peachsoju.gui.commands.RotateCommand
import com.peachsoju.modules.impl.autoalign.AutoAlign
//import com.peachsoju.modules.impl.autoalign.AutoAlignCommand
//import com.peachsoju.modules.impl.stormbow.StormBowCommand
import com.peachsoju.modules.impl.autoicefill.AutoIceFill
import com.peachsoju.utils.handlers.RenderBatchManager
import com.peachsoju.utils.handlers.RightClickHandler
import com.peachsoju.utils.handlers.SneakHandler
import com.peachsoju.modules.impl.autoroutes.Autoroutes
import com.peachsoju.modules.impl.autoroutes.AutoRoutesCommand
import com.peachsoju.modules.impl.autoroutes.BatListener
import com.peachsoju.modules.impl.autoroutes.BurstMode
import com.peachsoju.modules.impl.autoroutes.DungeonBreakerListener
import com.peachsoju.modules.impl.autoroutes.SecretListener
import com.peachsoju.modules.impl.autoroutes.NodeManager
import com.peachsoju.modules.impl.autoss.AutoSS
//import com.peachsoju.modules.impl.autoss.AutoSSCommand
import com.peachsoju.modules.impl.fmblocks.FMBlocksCommands
import com.peachsoju.modules.impl.fmblocks.FMBlocksEditMode
import com.peachsoju.modules.impl.fmblocks.FMBlocksManager
import com.peachsoju.modules.impl.stormbow.StormBowTimer
import com.peachsoju.utils.handlers.WalkHandler
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
//		AutoAlignCommand.register()
//		StormBowCommand.register()
//		AutoSSCommand.register()
		eventBus.register(AutoIceFill)
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
		eventBus.register(AutoSS)
		eventBus.register(StormBowTimer)
		eventBus.register(DungeonBreakerListener)
		eventBus.register(AutoAlign)
		eventBus.register(WalkHandler)
//		eventBus.register(AutoClose)
//		eventBus.register(SecretAura)
	}
}
