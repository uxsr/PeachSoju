package com.peachsoju.utils.handlers

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.utils.RouteUtils

object SneakHandler {

    private var waitingForConfirm: Boolean = false
    private var ticksSinceSneakSent: Int = 0
    private var requiredTicks: Int = 0
    private var onConfirmCallback: (() -> Unit)? = null

    fun setSneak(sneak: Boolean, onConfirm: (() -> Unit)? = null) {
        mc.options.keyShift.isDown = sneak

        RouteUtils.debug("§d[Sneak] setSneak($sneak), callback=${onConfirm != null}")

        if (onConfirm != null) {
            waitingForConfirm = true
            ticksSinceSneakSent = 0
            requiredTicks = getTicksForPing()
            onConfirmCallback = onConfirm
            RouteUtils.debug("§d[Sneak] Waiting $requiredTicks ticks for confirm")
        } else {
            waitingForConfirm = false
            onConfirmCallback = null
            ticksSinceSneakSent = 0
        }
    }

    fun isSneaking(): Boolean {
        return mc.options.keyShift.isDown
    }

    fun releaseSneak() {
        mc.options.keyShift.isDown = false
        waitingForConfirm = false
        onConfirmCallback = null
    }

    fun clearCallbacks() {
        waitingForConfirm = false
        onConfirmCallback = null
        ticksSinceSneakSent = 0
    }

    private fun getTicksForPing(): Int {
        val player = mc.player ?: return 2
        val ping = mc.connection?.getPlayerInfo(player.uuid)?.latency ?: 50
        return ((ping / 50).coerceAtLeast(1)) + 1
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (waitingForConfirm) {
            ticksSinceSneakSent++
            if (ticksSinceSneakSent >= requiredTicks) {
                waitingForConfirm = false
                onConfirmCallback?.invoke()
                onConfirmCallback = null
            }
        }
    }
}