package com.peachsoju.utils.handlers

import com.peachsoju.PeachSoju.mc
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.utils.RouteUtils

object JumpHandler {

    private var jumpQueued = false
    private var jumpTicksRemaining = 0
    private var jumpCallback: (() -> Unit)? = null
    private var waitingForGround = false
    private var groundCallback: (() -> Unit)? = null

    /**
     * Queue a single jump. Will press jump key for 3 ticks then release.
     * @param forceJump If true, skip the onGround check
     */
    fun jump(forceJump: Boolean = false, callback: (() -> Unit)? = null) {
        val player = mc.player ?: return

        // Check if already jumping
        if (jumpQueued) {
            RouteUtils.extraDebug("§e[Jump] Already jumping, skipping")
            return
        }

        val onGround = player.onGround()
        RouteUtils.debug("§b[Jump] Attempting jump - onGround=$onGround, forceJump=$forceJump")

        if (!forceJump && !onGround) {
            RouteUtils.extraDebug("§c[Jump] Cannot jump - not on ground")
            return
        }

        jumpQueued = true
        jumpTicksRemaining = 3  // Hold for 3 ticks
        jumpCallback = callback

        // Set jump key
        mc.options.keyJump.setDown(true)
        RouteUtils.debug("§a[Jump] Jump key pressed")
    }

    /**
     * Force a jump regardless of ground state.
     */
    fun forceJump(callback: (() -> Unit)? = null) {
        jump(forceJump = true, callback = callback)
    }

    /**
     * Queue a jump and call back when player lands.
     */
    fun jumpAndWaitForLanding(onLand: () -> Unit) {
        jump {
            waitingForGround = true
            groundCallback = onLand
        }
    }

    /**
     * Check if currently in a jump sequence.
     */
    fun isJumping(): Boolean = jumpQueued || waitingForGround

    /**
     * Cancel any pending jump operations.
     */
    fun cancel() {
        mc.options.keyJump.setDown(false)
        jumpQueued = false
        jumpTicksRemaining = 0
        jumpCallback = null
        waitingForGround = false
        groundCallback = null
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        val player = mc.player ?: return

        // Handle jump key release
        if (jumpQueued) {
            jumpTicksRemaining--

            if (jumpTicksRemaining <= 0) {
                mc.options.keyJump.setDown(false)
                jumpQueued = false

                val callback = jumpCallback
                jumpCallback = null
                callback?.invoke()

                RouteUtils.extraDebug("§e[Jump] Jump key released")
            }
        }

        // Handle waiting for ground
        if (waitingForGround && player.onGround() && !jumpQueued) {
            waitingForGround = false
            val callback = groundCallback
            groundCallback = null
            callback?.invoke()
            RouteUtils.extraDebug("§a[Jump] Landed on ground")
        }
    }

    fun reset() {
        cancel()
    }
}