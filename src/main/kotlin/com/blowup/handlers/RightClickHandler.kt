package com.blowup.handlers

import com.blowup.PeachSoju.mc
import com.blowup.eventbus.SubscribeEvent
import com.blowup.eventbus.events.TickEvent
import com.blowup.mixin.IMultiPlayerGameModeAccessor
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.BlockHitResult
import org.apache.commons.lang3.mutable.MutableObject

object RightClickHandler {
    private var clickCount = 0
    fun queueClicks(count: Int) { clickCount = count }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (clickCount <= 0) return
        for (i in 0 until clickCount) doPacketInteract(InteractionHand.MAIN_HAND, 15f * i, 0f)
        clickCount = 0
    }

    fun doPacketInteract(interactionHand: InteractionHand = InteractionHand.MAIN_HAND, yaw: Float? = null, pitch: Float? = null): InteractionResult {
        val gm = mc.gameMode ?: return InteractionResult.PASS
        val p = mc.player ?: return InteractionResult.PASS
        if (gm.playerMode == GameType.SPECTATOR) return InteractionResult.PASS
        (gm as IMultiPlayerGameModeAccessor).callEnsureHasSentCarriedItem()
        val out = MutableObject<InteractionResult>()
        gm.callStartPrediction(mc.level!!) { i ->
            val pkt = ServerboundUseItemPacket(interactionHand, i, yaw ?: p.yRot, pitch ?: p.xRot)
            val stack = p.getItemInHand(interactionHand)
            if (p.cooldowns.isOnCooldown(stack)) { out.setValue(InteractionResult.PASS); pkt }
            else {
                val res = stack.use(mc.level!!, p, interactionHand)
                val stack2 = if (res is InteractionResult.Success) (res.heldItemTransformedTo() ?: p.getItemInHand(interactionHand)) else p.getItemInHand(interactionHand)
                if (stack2 != stack) p.setItemInHand(interactionHand, stack2)
                out.setValue(res); pkt
            }
        }
        return out.value
    }

    fun doBlockInteract(
        blockHitResult: BlockHitResult,
        interactionHand: InteractionHand = InteractionHand.MAIN_HAND
    ): InteractionResult {
        val gm = mc.gameMode ?: return InteractionResult.PASS
        val p = mc.player ?: return InteractionResult.PASS
        if (gm.playerMode == GameType.SPECTATOR) return InteractionResult.PASS
        (gm as IMultiPlayerGameModeAccessor).callEnsureHasSentCarriedItem()
        val out = MutableObject<InteractionResult>()
        gm.callStartPrediction(mc.level!!) { i ->
            out.setValue(InteractionResult.SUCCESS)
            ServerboundUseItemOnPacket(interactionHand, blockHitResult, i)
        }
        return out.value
    }

}
