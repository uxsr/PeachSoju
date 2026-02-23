//package com.peachsoju.modules.impl.autoleap
//
//import com.peachsoju.PeachSoju.mc
//import com.peachsoju.config
//import com.peachsoju.eventbus.SubscribeEvent
//import com.peachsoju.eventbus.events.PacketEvent
//import com.peachsoju.eventbus.events.TickEvent
//import com.peachsoju.eventbus.events.WorldEvent
//import com.peachsoju.utils.RouteUtils
//import com.peachsoju.utils.handlers.RightClickHandler
//import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
//import com.odtheking.odin.utils.skyblock.dungeon.DungeonPlayer
//import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
//import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
//import us.filthycheaters.legitcatsex.utils.SilentSwap
//import net.minecraft.network.chat.Component
//import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
//import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
//import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
//import net.minecraft.world.inventory.ClickType
//
//object AutoLeap {
//
//    private val enabled get() = config.autoLeapEnabled()
//
//    // Config getters for each phase
//    private val ee2Enabled get() = config.autoLeapEE2()
//    private val ee2Name get() = config.autoLeapEE2Name()
//    private val ee2Class get() = config.autoLeapEE2Class()
//
//    private val ee3Enabled get() = config.autoLeapEE3()
//    private val ee3Name get() = config.autoLeapEE3Name()
//    private val ee3Class get() = config.autoLeapEE3Class()
//
//    private val coreEnabled get() = config.autoLeapCore()
//    private val coreName get() = config.autoLeapCoreName()
//    private val coreClass get() = config.autoLeapCoreClass()
//
//    // State
//    private var gateBlown = false
//    private var terminalsDone = false
//    private var currentPhase = 0
//    private var leapMenuOpen = false
//    private var leapMenuContainerId = 0
//    private var pendingLeapTarget: String? = null
//    private var leapInProgress = false
//
//    private val classes = listOf(
//        DungeonClass.Archer,
//        DungeonClass.Tank,
//        DungeonClass.Healer,
//        DungeonClass.Mage,
//        DungeonClass.Berserk
//    )
//
//    private fun message(msg: String) {
//        mc.player?.displayClientMessage(Component.literal("§7[AutoLeap] $msg"), false)
//    }
//
//    private fun debug(msg: String) {
//        RouteUtils.extraDebug("§d[AutoLeap] $msg")
//    }
//
//    /**
//     * Find the Infinileap slot in hotbar
//     */
//    private fun findInfinileapSlot(): Int? {
//        val player = mc.player ?: return null
//        for (i in 0..8) {
//            val stack = player.inventory.getItem(i)
//            if (!stack.isEmpty && stack.hoverName.string.contains("Infinileap", ignoreCase = true)) {
//                return i
//            }
//        }
//        return null
//    }
//
//    /**
//     * Get the phase a player is in based on their position
//     */
//    private fun getPhaseFromPosition(player: net.minecraft.world.entity.player.Player?): Int {
//        if (player == null) return 0
//        val x = player.x
//        val y = player.y
//        val z = player.z
//
//        return when {
//            isInBox(x, y, z, 108.0, 100.0, 144.0, 18.0, 150.0, 121.0) -> 2  // P2
//            isInBox(x, y, z, 112.0, 103.0, 29.0, 89.0, 148.0, 122.0) -> 1  // P1
//            isInBox(x, y, z, 20.0, 144.0, 51.0, -3.0, 105.0, 142.0) -> 3   // P3
//            isInBox(x, y, z, 94.0, 103.0, 26.0, -3.0, 158.0, 50.0) ||
//                    isInBox(x, y, z, 51.0, 124.0, 50.0, 60.0, 112.0, 55.0) -> 4    // P4 (pre-core)
//            isInBox(x, y, z, 68.0, 145.0, 55.0, 46.0, 107.0, 119.0) -> 5   // Core
//            isInBox(x, y, z, 112.0, 18.0, 19.0, 0.0, 2.0, 130.0) -> 6      // P5
//            else -> 0
//        }
//    }
//
//    private fun isInBox(x: Double, y: Double, z: Double, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Boolean {
//        return x >= minOf(x1, x2) && x <= maxOf(x1, x2) &&
//                y >= minOf(y1, y2) && y <= maxOf(y1, y2) &&
//                z >= minOf(z1, z2) && z <= maxOf(z1, z2)
//    }
//
//    /**
//     * Find a player by name in the dungeon party
//     */
//    private fun findPlayerByName(name: String): DungeonPlayer? {
//        if (name.isBlank()) return null
//        return DungeonUtils.dungeonTeammatesNoSelf.find {
//            it.name.equals(name, ignoreCase = true) || it.name.contains(name, ignoreCase = true)
//        }
//    }
//
//    /**
//     * Find a player by class in the dungeon party
//     */
//    private fun findPlayerByClass(classIndex: Int): DungeonPlayer? {
//        if (classIndex !in classes.indices) return null
//        val targetClass = classes[classIndex]
//        return DungeonUtils.dungeonTeammatesNoSelf.find { it.clazz == targetClass }
//    }
//
//    /**
//     * Get the target player for a specific phase
//     */
//    private fun getLeapTarget(phase: Int): DungeonPlayer? {
//        return when (phase) {
//            1 -> { // EE2
//                if (!ee2Enabled) return null
//                findPlayerByName(ee2Name) ?: findPlayerByClass(ee2Class)
//            }
//            2 -> { // EE3
//                if (!ee3Enabled) return null
//                findPlayerByName(ee3Name) ?: findPlayerByClass(ee3Class)
//            }
//            3, 4 -> { // Core entrance / Core
//                if (!coreEnabled) return null
//                findPlayerByName(coreName) ?: findPlayerByClass(coreClass)
//            }
//            else -> null
//        }
//    }
//
//    /**
//     * Check if we should leap based on current state
//     */
//    private fun shouldLeap() {
//        if (!gateBlown || !terminalsDone) return
//
//        currentPhase++
//        debug("Phase incremented to $currentPhase")
//
//        val myPhase = getPhaseFromPosition(mc.player)
//        if (currentPhase < myPhase) {
//            debug("Already past phase $currentPhase (currently in $myPhase)")
//            gateBlown = false
//            terminalsDone = false
//            return
//        }
//
//        val target = getLeapTarget(currentPhase)
//        if (target == null) {
//            debug("No target configured for phase $currentPhase")
//            gateBlown = false
//            terminalsDone = false
//            return
//        }
//
//        if (target.name == mc.player?.name?.string) {
//            debug("Target is self, skipping")
//            gateBlown = false
//            terminalsDone = false
//            return
//        }
//
//        // For core phases, verify target is in correct position
//        if (currentPhase == 3 || currentPhase == 4) {
//            val targetPhase = target.entity?.let { getPhaseFromPosition(it) } ?: 0
//            val expectedPhase = if (currentPhase == 3) 4 else 5
//
//            if (targetPhase != expectedPhase) {
//                message("§c${target.name} is not in the right spot (phase $targetPhase, expected $expectedPhase)")
//                gateBlown = false
//                terminalsDone = false
//                return
//            }
//        }
//
//        // Start leap process
//        startLeap(target.name)
//
//        gateBlown = false
//        terminalsDone = false
//    }
//
//    /**
//     * Start the leap process - silent swap to infinileap and use it
//     */
//    private fun startLeap(targetName: String) {
//        if (leapInProgress) {
//            debug("§cLeap already in progress, skipping")
//            message("§cLeap already in progress")
//            return
//        }
//
//        val leapSlot = findInfinileapSlot()
//        if (leapSlot == null) {
//            message("§cCould not find Infinileap in hotbar!")
//            debug("Infinileap not found in hotbar")
//            return
//        }
//
//        debug("Found Infinileap at slot $leapSlot, starting silent swap")
//
//        leapInProgress = true
//        pendingLeapTarget = targetName
//        message("§aLeaping to §c$targetName")
//
//        // Use SilentSwap to swap and right click
//        SilentSwap.scheduleSwap(leapSlot, "AutoLeap", "leap to $targetName") {
//            debug("Silent swap complete, using Infinileap")
//            RightClickHandler.doPacketInteract()
//            debug("Right click sent")
//        }
//    }
//
//    @SubscribeEvent
//    fun onPacketReceive(event: PacketEvent.Receive) {
//        if (!enabled && !config.autoLeapForce()) return
//        if (!config.autoLeapForce() && DungeonUtils.getF7Phase() != M7Phases.P3) return
//
//        val packet = event.packet
//
//        // Handle Spirit Leap menu opening
//        if (packet is ClientboundOpenScreenPacket) {
//            val title = packet.title.string
//            debug("Screen opened: $title")
//            if (title == "Spirit Leap" && pendingLeapTarget != null) {
//                leapMenuOpen = true
//                leapMenuContainerId = packet.containerId
//                debug("Spirit Leap menu detected, container ID: $leapMenuContainerId, looking for: $pendingLeapTarget")
//                // Don't cancel - let the menu open so we receive slot packets
//            }
//        }
//
//        // Handle slot updates in the leap menu
//        if (packet is ClientboundContainerSetSlotPacket && leapMenuOpen) {
//            if (packet.containerId != leapMenuContainerId) return
//
//            val slot = packet.slot
//            val item = packet.item
//
//            debug("Slot update: container=${packet.containerId}, slot=$slot, item=${item.hoverName.string}")
//
//            if (item.isEmpty) return
//
//            // Spirit Leap slots are 11-15 for the player heads
//            if (slot !in 11..15) return
//
//            val itemName = item.hoverName.string.replace("§[0-9a-fk-or]".toRegex(), "").trim()
//            val target = pendingLeapTarget ?: return
//
//            debug("Checking slot $slot: '$itemName' vs target '$target'")
//
//            if (itemName.equals(target, ignoreCase = true) || itemName.contains(target, ignoreCase = true)) {
//                debug("Found target at slot $slot, clicking...")
//                message("§aFound §c$target §aat slot $slot")
//
//                // Click the slot
//                mc.execute {
//                    clickSlot(leapMenuContainerId, slot)
//                    resetLeapState()
//                }
//            }
//        }
//
//        // Handle chat messages
//        if (packet is ClientboundSystemChatPacket) {
//            val msg = packet.content.string
//
//            // Gate destroyed
//            if (msg.contains("The gate has been destroyed!") || msg.contains("The Core entrance is opening!")) {
//                gateBlown = true
//                debug("Gate blown detected")
//            }
//
//            // Terminals/devices completed (7/7 or 8/8)
//            val terminalRegex = Regex("(\\w+) (activated|completed) (a terminal|a device|a lever)! \\((?:7/7|8/8)\\)")
//            if (terminalRegex.containsMatchIn(msg)) {
//                terminalsDone = true
//                debug("Terminals done detected")
//            }
//
//            // Leap cooldown
//            if (msg.contains("This ability is on cooldown for")) {
//                message("§cLeap on cooldown!")
//                resetLeapState()
//            }
//
//            // Successful leap
//            if (msg.contains("You have teleported to")) {
//                debug("Leap successful")
//                resetLeapState()
//            }
//        }
//    }
//
//    @SubscribeEvent
//    fun onTick(event: TickEvent.Start) {
//        if (!enabled && !config.autoLeapForce()) return
//        if (!config.autoLeapForce() && DungeonUtils.getF7Phase() != M7Phases.P3) return
//
//        if (gateBlown && terminalsDone) {
//            shouldLeap()
//        }
//    }
//
//    @SubscribeEvent
//    fun onWorldLoad(event: WorldEvent) {
//        reset()
//    }
//
//    private fun clickSlot(containerId: Int, slot: Int) {
//        val player = mc.player ?: return
//        debug("Clicking slot $slot in container $containerId")
//        mc.gameMode?.handleInventoryMouseClick(containerId, slot, 0, ClickType.PICKUP, player)
//        player.closeContainer()
//    }
//
//    private fun resetLeapState() {
//        leapMenuOpen = false
//        leapMenuContainerId = 0
//        pendingLeapTarget = null
//        leapInProgress = false
//        debug("Leap state reset")
//    }
//
//    // Public methods for commands
//    fun reset() {
//        gateBlown = false
//        terminalsDone = false
//        currentPhase = 0
//        resetLeapState()
//        debug("Reset all state")
//    }
//
//    fun simulateGateBlown() {
//        gateBlown = true
//        debug("Gate blown simulated")
//    }
//
//    fun simulateTerminalsDone() {
//        terminalsDone = true
//        debug("Terminals done simulated")
//    }
//
//    fun forceLeapTo(name: String) {
//        resetLeapState() // Clear any previous state
//        startLeap(name)
//    }
//
//    fun setCurrentPhase(phase: Int) {
//        currentPhase = phase
//        debug("Phase set to $phase")
//    }
//
//    fun getPhaseFromPositionPublic(player: net.minecraft.world.entity.player.Player): Int {
//        return getPhaseFromPosition(player)
//    }
//
//    data class LeapStatus(
//        val gateBlown: Boolean,
//        val terminalsDone: Boolean,
//        val currentPhase: Int,
//        val leapInProgress: Boolean,
//        val pendingTarget: String?
//    )
//
//    fun getStatus(): LeapStatus {
//        return LeapStatus(
//            gateBlown = gateBlown,
//            terminalsDone = terminalsDone,
//            currentPhase = currentPhase,
//            leapInProgress = leapInProgress,
//            pendingTarget = pendingLeapTarget
//        )
//    }
//}