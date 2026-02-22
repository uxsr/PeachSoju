package com.peachsoju.utils.handlers

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.utils.RouteUtils
import net.minecraft.client.KeyMapping

object WalkHandler {

    private var isWalking = false
    private var walkCallback: (() -> Unit)? = null

    fun startWalk(yaw: Float, pitch: Float, callback: (() -> Unit)? = null) {
        if (isWalking) {
            stopWalk()
        }

        val player = mc.player ?: return

        player.yRot = yaw
        player.xRot = pitch

        isWalking = true
        walkCallback = callback

        mc.options.keyUp.setDown(true)

        RouteUtils.debug("§a[Walk] Started: yaw=${"%.1f".format(yaw)}, pitch=${"%.1f".format(pitch)}")
    }

    fun stopWalk() {
        if (!isWalking) return

        mc.options.keyUp.setDown(false)

        val callback = walkCallback
        isWalking = false
        walkCallback = null

        callback?.invoke()
        RouteUtils.debug("§e[Walk] Stopped")
    }

    fun isWalking(): Boolean = isWalking

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!isWalking) return

        val options = mc.options

        if (options.keyDown.isDown || options.keyLeft.isDown || options.keyRight.isDown) {
            RouteUtils.debug("§e[Walk] Manual input detected, stopping")
            stopWalk()
            return
        }
    }

    fun releaseAllMovement() {
        mc.options.keyUp.setDown(false)
        mc.options.keyDown.setDown(false)
        mc.options.keyLeft.setDown(false)
        mc.options.keyRight.setDown(false)
        mc.options.keyJump.setDown(false)
        mc.options.keyShift.setDown(false)

        RouteUtils.debug("§c[Walk] Released all movement")
    }

    fun reset() {
        if (isWalking) stopWalk()
        releaseAllMovement()
    }
}