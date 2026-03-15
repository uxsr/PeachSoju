package com.peachsoju.modules.impl.dungeon.autop5

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.SwapResult
import com.peachsoju.utils.handlers.JumpHandler
import com.peachsoju.utils.handlers.RightClickHandler
import com.odtheking.odin.features.impl.floor7.WitherDragonsEnum
import net.minecraft.world.InteractionHand

/**
 * Handles combat automation for P5:
 * - Last Breath bow charging with dragon-specific delays
 * - Ice Spray Wand timing
 * - Soul Whip follow-up
 */
object AutoP5Combat {

    // Last Breath state
    private var isChargingLastBreath = false
    private var lastBreathStartTime = 0L
    private var targetDragon: WitherDragonsEnum? = null
    private var lastReleaseTime = 0L
    private const val RECHARGE_DELAY = 100L // ms between shots

    // Ice Spray state
    private var iceSprayQueued = false
    private var iceSprayTickCounter = 0

    // Soul Whip state
    private var soulWhipQueued = false
    private var soulWhipTickCounter = 0

    // Jump spray state
    private var jumpSprayActive = false
    private var jumpSprayPhase = 0
    private var jumpSprayStartTime = 0L

    // Key release tracking
    private var keyReleaseCounter = 0
    private var keyReleaseActive = false

    /**
     * Start charging Last Breath for a specific dragon.
     */
    fun startLastBreathCharge(dragon: WitherDragonsEnum) {
        if (isChargingLastBreath) return
        if (System.currentTimeMillis() - lastReleaseTime < RECHARGE_DELAY) return

        val player = mc.player ?: return

        // Check if holding Last Breath
        if (!RouteUtils.isHoldingItem("Last Breath")) {
            val result = RouteUtils.swapToItem("last breath")
            if (result == SwapResult.FAIL) {
                RouteUtils.debug("§c[Combat] Last Breath not in hotbar!")
                return
            }
        }

        // Check pitch - must be looking up
        if (player.xRot > -70) {
            RouteUtils.extraDebug("§c[Combat] Must look up to charge (pitch: ${player.xRot})")
            return
        }

        // Check Y level - not too high
        if (player.y > 31) {
            RouteUtils.extraDebug("§c[Combat] Too high for Last Breath (Y: ${player.y})")
            return
        }

        targetDragon = dragon
        isChargingLastBreath = true
        lastBreathStartTime = System.currentTimeMillis()

        // Hold right click
        mc.options.keyUse.setDown(true)

        RouteUtils.debug("§a[Combat] Charging Last Breath for §${dragon.colorCode}${dragon.name}")
    }

    /**
     * Stop charging Last Breath.
     */
    fun stopLastBreathCharge() {
        if (!isChargingLastBreath) return

        mc.options.keyUse.setDown(false)
        isChargingLastBreath = false
        targetDragon = null
        lastReleaseTime = System.currentTimeMillis()

        RouteUtils.extraDebug("§e[Combat] Stopped Last Breath charge")
    }

    /**
     * Queue Ice Spray for dragon spawn timing.
     * Called when a dragon starts spawning.
     */
    fun queueIceSpray() {
        if (!config.autoP5IceSpray()) return

        iceSprayQueued = true
        iceSprayTickCounter = 0

        RouteUtils.debug("§b[Combat] Ice Spray queued")
    }

    /**
     * Cancel Ice Spray queue.
     */
    fun cancelIceSpray() {
        iceSprayQueued = false
        iceSprayTickCounter = 0
        jumpSprayActive = false
        jumpSprayPhase = 0
    }

    /**
     * Check if combat automation is active.
     */
    fun isActive(): Boolean {
        return isChargingLastBreath || iceSprayQueued || soulWhipQueued || jumpSprayActive
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        handleKeyRelease()
        handleLastBreath()
        handleIceSpray()
        handleSoulWhip()
        handleJumpSpray()
    }

    private fun handleKeyRelease() {
        if (!keyReleaseActive) return

        keyReleaseCounter--
        if (keyReleaseCounter <= 0) {
            mc.options.keyUse.setDown(false)
            keyReleaseActive = false
        }
    }

    private fun handleLastBreath() {
        if (!isChargingLastBreath) return
        if (!config.autoP5LastBreath()) {
            stopLastBreathCharge()
            return
        }

        val player = mc.player ?: return

        // Check if still holding Last Breath
        if (!RouteUtils.isHoldingItem("Last Breath")) {
            stopLastBreathCharge()
            return
        }

        // Check pitch - stop if not looking up
        if (player.xRot > -70) {
            stopLastBreathCharge()
            return
        }

        // Get delay for target dragon
        val delay = getLastBreathDelay(targetDragon)
        val chargeTime = System.currentTimeMillis() - lastBreathStartTime

        // Release after delay
        if (chargeTime >= delay) {
            mc.options.keyUse.setDown(false)
            lastReleaseTime = System.currentTimeMillis()
            isChargingLastBreath = false

            RouteUtils.debug("§a[Combat] Released Last Breath after ${chargeTime}ms")
        }
    }

    private fun getLastBreathDelay(dragon: WitherDragonsEnum?): Long {
        return when (dragon) {
            WitherDragonsEnum.Purple -> config.autoP5PurpleDelay().toLong()
            WitherDragonsEnum.Red -> config.autoP5RedDelay().toLong()
            WitherDragonsEnum.Orange -> config.autoP5OrangeDelay().toLong()
            WitherDragonsEnum.Green -> config.autoP5GreenDelay().toLong()
            WitherDragonsEnum.Blue -> config.autoP5BlueDelay().toLong()
            null -> 750L  // Default
        }
    }

    private fun handleIceSpray() {
        if (!iceSprayQueued) return
        if (!config.autoP5IceSpray()) {
            cancelIceSpray()
            return
        }

        iceSprayTickCounter++

        val targetTick = config.autoP5IceSprayTick()

        // Ice spray at specific tick relative to dragon spawn
        // The dragon spawns at tick 100 (5 seconds), we want to spray before it lands
        if (iceSprayTickCounter == targetTick) {
            // Start the jump spray sequence
            jumpSprayActive = true
            jumpSprayPhase = 0
            jumpSprayStartTime = System.currentTimeMillis()

            RouteUtils.debug("§b[Combat] Starting Ice Spray sequence at tick $iceSprayTickCounter")
        }

        // Timeout after 120 ticks
        if (iceSprayTickCounter > 120) {
            iceSprayQueued = false
            iceSprayTickCounter = 0
        }
    }

    private fun handleJumpSpray() {
        if (!jumpSprayActive) return

        val player = mc.player ?: return

        when (jumpSprayPhase) {
            0 -> {
                // Swap to Ice Spray Wand and jump
                val swapResult = RouteUtils.swapToItem("ice spray wand")
                if (swapResult == SwapResult.FAIL) {
                    RouteUtils.debug("§c[Combat] Ice Spray Wand not in hotbar!")
                    jumpSprayActive = false
                    iceSprayQueued = false
                    return
                }

                if (player.onGround()) {
                    JumpHandler.forceJump()
                }
                jumpSprayPhase = 1
            }
            1 -> {
                // Wait for specific Y position during jump
                val fy = player.y - player.y.toInt()
                if (fy >= 0.19 && fy <= 0.25) {
                    // Use Ice Spray via packet
                    RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND)
                    RouteUtils.debug("§b[Combat] Ice Spray used at Y fractional: ${"%.3f".format(fy)}")
                    jumpSprayPhase = 2

                    // Queue Soul Whip if enabled
                    if (config.autoP5SoulWhip()) {
                        soulWhipQueued = true
                        soulWhipTickCounter = 4  // 4 tick delay before soul whip
                    }
                }

                // Timeout if we missed the window
                if (System.currentTimeMillis() - jumpSprayStartTime > 1000) {
                    RouteUtils.debug("§c[Combat] Ice Spray timing missed!")
                    jumpSprayPhase = 2
                }
            }
            2 -> {
                // Done with ice spray sequence
                jumpSprayActive = false
                jumpSprayPhase = 0
                iceSprayQueued = false
            }
        }
    }

    private fun handleSoulWhip() {
        if (!soulWhipQueued) return
        if (!config.autoP5SoulWhip()) {
            soulWhipQueued = false
            return
        }

        soulWhipTickCounter--

        if (soulWhipTickCounter <= 0) {
            // Swap to Soul Whip
            val swapResult = RouteUtils.swapToItem("soul whip")
            if (swapResult == SwapResult.FAIL) {
                RouteUtils.debug("§c[Combat] Soul Whip not in hotbar!")
                soulWhipQueued = false
                return
            }

            // Hold right click
            mc.options.keyUse.setDown(true)

            // Schedule release after configured ticks
            val releaseTick = config.autoP5SoulWhipTick()
            keyReleaseCounter = releaseTick
            keyReleaseActive = true

            soulWhipQueued = false

            RouteUtils.debug("§d[Combat] Soul Whip used, releasing in $releaseTick ticks")
        }
    }

    fun reset() {
        stopLastBreathCharge()
        cancelIceSpray()
        soulWhipQueued = false
        soulWhipTickCounter = 0
        keyReleaseActive = false
        keyReleaseCounter = 0
        mc.options.keyUse.setDown(false)
    }
}