package com.peachsoju.modules.impl.dungeon.puzzles.autoweirdos

import com.odtheking.odin.features.impl.dungeon.puzzlesolvers.WeirdosSolver
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.peachsoju.PeachSoju
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.handlers.EntityInteractionHandler
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

object AutoWeirdos {

    private val enabled get() = config.autoWeirdos()
    private const val ENTITY_CLICK_RANGE = 4.5
    private const val CHEST_CLICK_RANGE = 6.0

    private val clickedEntities = mutableSetOf<Int>()
    private var clickedChest = false

    private val correctPosField by lazy {
        try {
            WeirdosSolver::class.java.getDeclaredField("correctPos").apply {
                isAccessible = true
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getCorrectPos(): BlockPos? {
        return try {
            correctPosField?.get(WeirdosSolver) as? BlockPos
        } catch (e: Exception) {
            null
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return
        if (!DungeonUtils.inDungeons) return
        if (clickedChest) return

        val room = DungeonUtils.currentRoom
        if (room?.data?.name != "Three Weirdos") return

        val player = PeachSoju.mc.player ?: return
        val eyePos = player.eyePosition

        val solution = getCorrectPos()

        if (solution == null) {
            val clickableNpc = EntityInteractionHandler.findNearestEntity<ArmorStand>(ENTITY_CLICK_RANGE) { entity ->
                entity.hasCustomName() &&
                        entity.customName?.string?.contains("CLICK", ignoreCase = true) == true &&
                        !clickedEntities.contains(entity.id)
            } ?: return

            val result = EntityInteractionHandler.interact(clickableNpc)

            if (result.consumesAction()) {
                clickedEntities.add(clickableNpc.id)
                debug("§a[Weirdos] Clicked NPC (${clickedEntities.size}/3)")
            }
            return
        }

        if (clickedEntities.size < 3) return

        val distance = eyePos.distanceTo(Vec3.atCenterOf(solution))
        if (distance > CHEST_CLICK_RANGE) return

        val level = PeachSoju.mc.level ?: return
        val blockState = level.getBlockState(solution)
        if (blockState.isAir) return

        val hitResult = raytraceToBlock(eyePos, solution)
        if (hitResult == null) {
            debug("§c[Weirdos] Could not raytrace to chest")
            return
        }

        val interactResult = interactWithBlock(hitResult)
        if (interactResult.consumesAction()) {
            clickedChest = true
            debug("§a[Weirdos] Clicked correct chest!")
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
        reset()
    }

    fun reset() {
        clickedEntities.clear()
        clickedChest = false
    }

    private fun interactWithBlock(hitResult: BlockHitResult): InteractionResult {
        val gameMode = PeachSoju.mc.gameMode ?: return InteractionResult.PASS
        val player = PeachSoju.mc.player ?: return InteractionResult.PASS

        if (gameMode.playerMode == GameType.SPECTATOR) return InteractionResult.PASS

        return gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult)
    }

    private fun raytraceToBlock(eyePos: Vec3, targetBlock: BlockPos): BlockHitResult? {
        val level = PeachSoju.mc.level ?: return null
        val blockCenter = Vec3(targetBlock.x + 0.5, targetBlock.y + 0.5, targetBlock.z + 0.5)
        val ctx =
            ClipContext(eyePos, blockCenter, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, PeachSoju.mc.player)
        val result = level.clip(ctx)
        return if (result.type == HitResult.Type.BLOCK) result as? BlockHitResult else null
    }

    private fun debug(msg: String) {
        if (config.debug()) PeachSoju.mc.player?.displayClientMessage(Component.literal("§7$msg"), false)
    }
}