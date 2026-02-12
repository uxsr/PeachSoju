package com.blowup.modules.impl.fmblocks

import com.blowup.PeachSoju.mc
import com.blowup.config
import com.blowup.eventbus.SubscribeEvent
import com.blowup.eventbus.events.TickEvent
import com.blowup.utils.RouteUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object FMBlocksEditMode {

    val enabled: Boolean get() = config.fmBlocksEditMode()

    var currentBlockState: BlockState = Blocks.STONE.defaultBlockState()
        private set

    private var wasLeftClickDown = false
    private var wasMiddleClickDown = false
    private var wasRightClickDown = false

    private var lastActionTime = 0L
    private const val actionCooldownMs = 200L

    fun setCurrentBlock(state: BlockState) {
        currentBlockState = state
        RouteUtils.debug("§a[FMBlocks] Selected: ${state.block.descriptionId}")
    }

    fun setCurrentBlockFromHand(): Boolean {
        val player = mc.player ?: return false
        val heldItem = player.mainHandItem
        val blockItem = heldItem.item as? BlockItem ?: return false
        val block = blockItem.block
        currentBlockState = block.defaultBlockState()
        RouteUtils.debug("§a[FMBlocks] Selected from hand: ${block.descriptionId}")
        return true
    }

    private fun getTargetBlock(): Pair<BlockPos, BlockHitResult>? {
        val hitResult = mc.hitResult ?: return null
        if (hitResult.type != HitResult.Type.BLOCK) return null
        val blockHit = hitResult as? BlockHitResult ?: return null
        return blockHit.blockPos to blockHit
    }

    private fun canDoAction(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < actionCooldownMs) return false
        lastActionTime = now
        return true
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return
        if (!FMBlocksManager.enabled) return

        val options = mc.options

        val leftClickDown = options.keyAttack.isDown
        if (leftClickDown && !wasLeftClickDown) onLeftClick()
        wasLeftClickDown = leftClickDown

        val middleClickDown = options.keyPickItem.isDown
        if (middleClickDown && !wasMiddleClickDown) onMiddleClick()
        wasMiddleClickDown = middleClickDown

        val rightClickDown = options.keyUse.isDown
        val sneaking = mc.player?.isCrouching == true
        if (rightClickDown && !wasRightClickDown && sneaking) onRightClick()
        wasRightClickDown = rightClickDown
    }

    private fun onLeftClick() {
        if (!canDoAction()) return
        val (blockPos, _) = getTargetBlock() ?: return

        if (FMBlocksManager.hasBlockAt(blockPos)) {
            FMBlocksManager.removeBlock(blockPos)
            return
        }

        val level = mc.level ?: return
        val currentState = level.getBlockState(blockPos)
        if (currentState.isAir) return
        FMBlocksManager.addGhostBlock(blockPos)
    }

    private fun onMiddleClick() {
        if (!canDoAction()) return
        val (blockPos, _) = getTargetBlock() ?: return
        val level = mc.level ?: return
        val state = level.getBlockState(blockPos)
        if (state.isAir) return
        setCurrentBlock(state)
    }

    private fun onRightClick() {
        if (!canDoAction()) return

        val (blockPos, hitResult) = getTargetBlock() ?: return
        val player = mc.player ?: return
        val placePos = blockPos.relative(hitResult.direction)

        if (FMBlocksManager.isGhostBlock(blockPos)) {
            FMBlocksManager.removeBlock(blockPos)
            return
        }

        if (FMBlocksManager.isGhostBlock(placePos)) {
            FMBlocksManager.removeBlock(placePos)
        }

        val heldItem = player.mainHandItem
        val blockState = if (heldItem.item is BlockItem) {
            val blockItem = heldItem.item as BlockItem
            val placementContext = BlockPlaceContext(player, InteractionHand.MAIN_HAND, heldItem, hitResult)
            blockItem.block.getStateForPlacement(placementContext) ?: blockItem.block.defaultBlockState()
        } else {
            currentBlockState
        }

        FMBlocksManager.addBlock(placePos, blockState)
    }

    private val quickSelectBlocks = listOf(
        Blocks.STONE.defaultBlockState(),
        Blocks.COBBLESTONE.defaultBlockState(),
        Blocks.OAK_PLANKS.defaultBlockState(),
        Blocks.GLASS.defaultBlockState(),
        Blocks.GLOWSTONE.defaultBlockState(),
        Blocks.REDSTONE_LAMP.defaultBlockState(),
        Blocks.SEA_LANTERN.defaultBlockState(),
        Blocks.WHITE_STAINED_GLASS.defaultBlockState(),
        Blocks.IRON_BARS.defaultBlockState(),
        Blocks.LADDER.defaultBlockState()
    )

    private var quickSelectIndex = 0

    fun cycleQuickSelect() {
        quickSelectIndex = (quickSelectIndex + 1) % quickSelectBlocks.size
        currentBlockState = quickSelectBlocks[quickSelectIndex]
        RouteUtils.debug("§e[FMBlocks] Quick select: ${currentBlockState.block.name.string}")
    }
}
