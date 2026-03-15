package com.peachsoju.modules.impl.dungeon.autop5

import com.peachsoju.PeachSoju
import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.handlers.JumpHandler
import com.peachsoju.utils.handlers.WalkHandler
import com.odtheking.odin.features.impl.floor7.DragonCheck
import com.odtheking.odin.features.impl.floor7.WitherDragons
import com.odtheking.odin.features.impl.floor7.WitherDragonsEnum
import com.odtheking.odin.features.impl.floor7.WitherDragonState
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.network.chat.Component

/**
 * Main manager for P5 dragon automation.
 * Hooks into Odin's WitherDragons system for dragon detection and priority.
 *
 * Features:
 * - Automatic pathfinding to assigned dragon
 * - Last Breath charging automation
 * - Ice Spray + Soul Whip combo
 * - Return to middle after dragon death
 */
object AutoP5Manager {

    private var lastPriorityDragon: WitherDragonsEnum? = null
    private var lastDragonDeath: WitherDragonsEnum? = null
    private var wasInP5 = false
    private var lastTickTime = 0L

    val isEnabled: Boolean get() = config.autoP5Enabled()
    val isInP5: Boolean get() = DungeonUtils.getF7Phase() == M7Phases.P5

    fun initialize() {
        PeachSoju.eventBus.register(this)
        PeachSoju.eventBus.register(AutoP5Pathfinder)
        PeachSoju.eventBus.register(AutoP5Combat)
        PeachSoju.eventBus.register(JumpHandler)

        RouteUtils.debug("§a[AutoP5] Initialized and registered")
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!isEnabled) return
        if (!DungeonUtils.inDungeons) return

        // Detect P5 phase entry
        val currentlyInP5 = isInP5
        if (currentlyInP5 && !wasInP5) {
            onP5Enter()
        } else if (!currentlyInP5 && wasInP5) {
            onP5Exit()
        }
        wasInP5 = currentlyInP5

        if (!currentlyInP5) return

        // Check for new priority dragon from Odin
        checkPriorityDragon()

        // Check for dragon death
        checkDragonDeath()

        // Handle combat if at position
        handleCombatAutomation()
    }

    private fun onP5Enter() {
        AutoP5State.isActive = true
        RouteUtils.debug("§d[AutoP5] Entered P5 phase!")

        mc.player?.displayClientMessage(
            Component.literal("§d§l[AutoP5] §fP5 Started - Automation active"),
            false
        )
    }

    private fun onP5Exit() {
        AutoP5State.reset()
        AutoP5Pathfinder.reset()
        AutoP5Combat.reset()
        lastPriorityDragon = null
        lastDragonDeath = null

        RouteUtils.debug("§e[AutoP5] Exited P5 phase")
    }

    /**
     * Check if Odin has assigned a new priority dragon.
     */
    private fun checkPriorityDragon() {
        val odinPriority = WitherDragons.priorityDragon

        // New dragon assigned
        if (odinPriority != null && odinPriority != lastPriorityDragon) {
            lastPriorityDragon = odinPriority
            onDragonAssigned(odinPriority)
        }
    }

    /**
     * Check if a dragon has died (via Odin's tracking).
     */
    private fun checkDragonDeath() {
        val odinLastDeath = DragonCheck.lastDragonDeath

        if (odinLastDeath != null && odinLastDeath != lastDragonDeath) {
            lastDragonDeath = odinLastDeath
            onDragonDeath(odinLastDeath)
        }
    }

    /**
     * Called when Odin assigns a priority dragon.
     */
    private fun onDragonAssigned(dragon: WitherDragonsEnum) {
        RouteUtils.debug("§a[AutoP5] Priority dragon: §${dragon.colorCode}${dragon.name}")

        AutoP5State.onDragonAssigned(dragon)

        // Queue ice spray
        AutoP5Combat.queueIceSpray()

        // Start pathfinding if enabled
        if (config.autoP5Pathfind()) {
            val debuffPos = DragonDebuffPositions.getDebuffPosition(dragon)

            mc.player?.displayClientMessage(
                Component.literal("§${dragon.colorCode}§l${dragon.name}§r §7- Pathfinding to debuff position"),
                false
            )

            AutoP5Pathfinder.pathfindTo(debuffPos) {
                onArrivedAtDragon(dragon)
            }
        }
    }

    /**
     * Called when arrived at dragon debuff position.
     */
    private fun onArrivedAtDragon(dragon: WitherDragonsEnum) {
        RouteUtils.debug("§a[AutoP5] Arrived at §${dragon.colorCode}${dragon.name}")

        AutoP5State.onArrivedAtPosition()

        mc.player?.displayClientMessage(
            Component.literal("§a[AutoP5] §fAt ${dragon.colorCode}${dragon.name}§f - Look up for Last Breath"),
            false
        )

        // Swap to Last Breath
        RouteUtils.swapToItem("last breath")

        // Look up
        mc.player?.let { player ->
            player.xRot = -90f
        }
    }

    /**
     * Handle combat automation when at dragon position.
     */
    private fun handleCombatAutomation() {
        if (!config.autoP5LastBreath()) return

        val dragon = AutoP5State.assignedDragon ?: return
        if (!AutoP5State.isAtDebuffPosition) return

        val player = mc.player ?: return

        // Check if dragon is alive and we should be shooting
        if (dragon.state != WitherDragonState.ALIVE) return

        // Check if looking up and holding Last Breath
        if (player.xRot <= -70 && RouteUtils.isHoldingItem("Last Breath")) {
            AutoP5Combat.startLastBreathCharge(dragon)
        }
    }

    /**
     * Called when a dragon dies.
     */
    private fun onDragonDeath(dragon: WitherDragonsEnum) {
        RouteUtils.debug("§c[AutoP5] Dragon died: §${dragon.colorCode}${dragon.name}")

        AutoP5State.onDragonDeath()

        // Stop current actions
        AutoP5Pathfinder.stop()
        AutoP5Combat.stopLastBreathCharge()

        // Go to middle if enabled
        if (config.autoP5GoMiddle()) {
            mc.player?.displayClientMessage(
                Component.literal("§7[AutoP5] §fMoving to middle..."),
                false
            )

            AutoP5Pathfinder.pathfindTo(DragonDebuffPositions.MIDDLE_POSITION) {
                mc.player?.displayClientMessage(
                    Component.literal("§a[AutoP5] §fAt middle - Waiting for next dragon"),
                    false
                )
            }
        }

        // Reset for next dragon
        lastPriorityDragon = null
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
        reset()
    }

    fun reset() {
        AutoP5State.reset()
        AutoP5Pathfinder.reset()
        AutoP5Combat.reset()
        JumpHandler.reset()
        WalkHandler.reset()
        lastPriorityDragon = null
        lastDragonDeath = null
        wasInP5 = false
    }

    // ==================== MANUAL COMMANDS ====================

    /**
     * Manual command to go to a specific dragon.
     */
    fun goToDragon(dragonName: String) {
        val dragon = WitherDragonsEnum.entries.find {
            it.name.equals(dragonName, ignoreCase = true)
        }

        if (dragon == null) {
            mc.player?.displayClientMessage(
                Component.literal("§c[AutoP5] Unknown dragon: $dragonName"),
                false
            )
            mc.player?.displayClientMessage(
                Component.literal("§7Valid dragons: Red, Orange, Green, Blue, Purple"),
                false
            )
            return
        }

        AutoP5State.onDragonAssigned(dragon)
        val debuffPos = DragonDebuffPositions.getDebuffPosition(dragon)

        mc.player?.displayClientMessage(
            Component.literal("§a[AutoP5] §fPathfinding to §${dragon.colorCode}${dragon.name}"),
            false
        )

        AutoP5Pathfinder.pathfindTo(debuffPos) {
            mc.player?.displayClientMessage(
                Component.literal("§a[AutoP5] §fArrived at §${dragon.colorCode}${dragon.name}"),
                false
            )
            AutoP5State.onArrivedAtPosition()
        }
    }

    /**
     * Manual command to go to middle.
     */
    fun goToMiddle() {
        mc.player?.displayClientMessage(
            Component.literal("§7[AutoP5] §fPathfinding to middle..."),
            false
        )

        AutoP5Pathfinder.pathfindTo(DragonDebuffPositions.MIDDLE_POSITION) {
            mc.player?.displayClientMessage(
                Component.literal("§a[AutoP5] §fArrived at middle"),
                false
            )
        }
    }

    /**
     * Stop all automation.
     */
    fun stopAll() {
        AutoP5Pathfinder.stop()
        AutoP5Combat.reset()
        AutoP5State.isPathfinding = false
        AutoP5State.assignedDragon = null

        mc.player?.displayClientMessage(
            Component.literal("§e[AutoP5] §fStopped all automation"),
            false
        )
    }
}