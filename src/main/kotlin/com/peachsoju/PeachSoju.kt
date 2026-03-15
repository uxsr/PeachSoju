package com.peachsoju

import com.peachsoju.eventbus.EventBus
import com.peachsoju.gui.commands.PeachSojuCommand
import com.peachsoju.gui.commands.RotateCommand
import com.peachsoju.modules.impl.dungeon.autop5.AutoP5Commands
import com.peachsoju.modules.impl.dungeon.autop5.AutoP5Manager
import com.peachsoju.modules.impl.dungeon.autop5.AutoP5Renderer
import com.peachsoju.modules.impl.dungeon.autoroutes.ChestDelay
import com.peachsoju.modules.impl.dungeon.autoroutes.AlignmentExecutor
import com.peachsoju.modules.impl.dungeon.autoroutes.NpcListener
//import com.peachsoju.modules.impl.stormbow.StormBowCommand
import com.peachsoju.utils.handlers.RenderBatchManager
import com.peachsoju.utils.handlers.RightClickHandler
import com.peachsoju.utils.handlers.SneakHandler
import com.peachsoju.modules.impl.misc.chatbypass.ChatBypass
//import com.peachsoju.modules.impl.autoss.AutoSSCommand
import com.peachsoju.modules.impl.dungeon.fmblocks.FMBlocksCommands
import com.peachsoju.modules.impl.dungeon.fmblocks.FMBlocksEditMode
import com.peachsoju.modules.impl.dungeon.fmblocks.FMBlocksHighlightRenderer
import com.peachsoju.modules.impl.dungeon.fmblocks.FMBlocksHighlights
import com.peachsoju.modules.impl.dungeon.fmblocks.FMBlocksManager
//import com.peachsoju.modules.impl.dungeon.icefill.IceFillSolver
import com.peachsoju.modules.impl.dungeon.puzzles.autoss.AutoSS
import com.peachsoju.modules.impl.dungeon.puzzles.autoalign.AutoAlign
import com.peachsoju.modules.impl.dungeon.puzzles.autoicefill.AutoIceFill
//import com.peachsoju.modules.impl.dungeon.puzzles.autoalign.AutoAlignCommand
//import com.peachsoju.modules.impl.dungeon.puzzles.autoicefill.AutoIceFill
import com.peachsoju.modules.impl.dungeon.puzzles.autoweirdos.AutoWeirdos
import com.peachsoju.modules.impl.misc.customitems.ItemCustomize
import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeCommand
import com.peachsoju.modules.impl.misc.customitems.ItemCustomizeManager
import com.peachsoju.modules.impl.misc.nickhider.NickHider
import com.peachsoju.modules.impl.misc.stormbow.StormBowTimer
import com.peachsoju.sync.PeachSojuSync
import com.peachsoju.utils.handlers.JumpHandler
import com.peachsoju.utils.handlers.WalkHandler
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory

object PeachSoju {
	private val logger = LoggerFactory.getLogger("PeachSoju")
	val mc: Minecraft get() = Minecraft.getInstance()
	val eventBus = EventBus()

	fun init() {
		config.load()
		_root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.AutoRoutesCommand.register()
		ItemCustomizeManager.loadAnimatedHeadsFromResources()
		PeachSojuCommand.register()
		RotateCommand.register()
		FMBlocksCommands.register()
		AutoP5Commands.register()
//		AutoAlignCommand.register()
//		StormBowCommand.register()
//		AutoSSCommand.register()
		ItemCustomizeCommand.register()
		ItemCustomize.init()
		PeachSojuSync.init()
		AutoP5Renderer.initialize()
		AutoP5Manager.initialize()
		eventBus.register(PeachSojuSync)
//		eventBus.register(IceFillSolver)
		eventBus.register(AutoIceFill)
		eventBus.register(FMBlocksManager)
		eventBus.register(FMBlocksEditMode)
		eventBus.register(AlignmentExecutor)
		eventBus.register(_root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.NodeManager)
		eventBus.register(_root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.Autoroutes)
		eventBus.register(_root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.SecretListener)
		eventBus.register(_root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.BatListener)
		eventBus.register(_root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.BurstMode)
		eventBus.register(RenderBatchManager)
		eventBus.register(RightClickHandler)
		eventBus.register(SneakHandler)
		eventBus.register(AutoSS)
		eventBus.register(StormBowTimer)
		eventBus.register(_root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.DungeonBreakerListener)
		eventBus.register(AutoAlign)
		eventBus.register(WalkHandler)
		eventBus.register(FMBlocksHighlightRenderer)
		eventBus.register(FMBlocksHighlights)
		eventBus.register(ChatBypass)
		eventBus.register(AutoWeirdos)
		eventBus.register(NickHider)
		eventBus.register(NpcListener)
		eventBus.register(JumpHandler)
//		eventBus.register(ChestDelay)
//		eventBus.register(AutoClose)
//		eventBus.register(SecretAura)
	}
}
