package com.peachsoju.modules.impl.autoroutes

import com.odtheking.odin.features.impl.render.Etherwarp
import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.handlers.RightClickHandler
import com.peachsoju.utils.handlers.SneakHandler
import com.peachsoju.modules.impl.autoroutes.data.WPType
import com.peachsoju.modules.impl.autoroutes.data.WaypointNode
import com.peachsoju.utils.RouteUtils
import com.peachsoju.utils.SwapResult
import com.odtheking.odin.utils.skyblock.LocationUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import com.odtheking.odin.utils.skyblock.dungeon.tiles.Room
import com.peachsoju.utils.handlers.WalkHandler
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import us.filthycheaters.legitcatsex.utils.SilentSwap
import kotlin.math.floor

object Autoroutes {

    private val enabled get() = config.autoroutes()
    private const val nodeCooldownMs = 150L
    private var leftClickWasDown = false
    private var lastTeleportTime = 0L
    private const val TELEPORT_GRACE_MS = 250L

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return
        val player = mc.player ?: return
        val room = DungeonUtils.currentRoom
        val key = getCurrentKey(room) ?: return

        RouteState.nodeList = NodeManager.getWaypointsForRoom(key) ?: emptyList()

        if (key != RouteState.currentRoomKey) {
            RouteState.currentRoomKey = key
            RouteState.currentRoom = room
            RouteState.consumed = 0
            RouteState.routeActive = false
            RouteState.awaitingSecrets = 0
            RouteState.pendingNodeIndex = -1
            RouteState.awaitingSecretConfirmation = false
            RouteState.secretConfirmationTicks = 0
            RouteState.delayTicksRemaining = 0
            RouteState.delayTicksRemaining = 0
            RouteState.nodeCooldowns.clear()
            RouteState.previousPosition = player.position()
            SneakHandler.releaseSneak()
            RouteUtils.debug("§aRoom changed to: $key (${RouteState.nodeList.size} nodes)")
        }

        if (RouteState.awaitingSecrets > 0) {
            val awaitIndex = RouteState.pendingNodeIndex
            val awaitNode = RouteState.nodeList.getOrNull(awaitIndex)

            if (awaitNode != null) {
                val nodeWorldPos = RouteUtils.getNodeWorldPosition(awaitNode, room)
                val currentPos = player.position()

                if (!intersectsNode(currentPos, nodeWorldPos, awaitNode.radius, awaitNode.height)) {
                    RouteUtils.debug("§c[Await] Player left await node #$awaitIndex - cancelling await state")
                    RouteState.awaitingSecrets = 0
                    RouteState.pendingNodeIndex = -1
                    SecretListener.setAwaitType("any")
                    SneakHandler.releaseSneak()
                }
            }
        }

        if (RouteState.awaitingSecretConfirmation) {
            val awaitIndex = RouteState.pendingAwaitNodeIndex
            val awaitNode = RouteState.pendingAwaitNode

            if (awaitNode != null) {
                val nodeWorldPos = RouteUtils.getNodeWorldPosition(awaitNode, room)
                val currentPos = player.position()

                if (!intersectsNode(currentPos, nodeWorldPos, awaitNode.radius, awaitNode.height)) {
                    RouteUtils.debug("§c[Await] Player left await confirmation node #$awaitIndex - cancelling")
                    RouteState.awaitingSecretConfirmation = false
                    RouteState.secretConfirmationTicks = 0
                    RouteState.pendingAwaitNode = null
                    RouteState.pendingAwaitNodeIndex = -1
                    RouteState.pendingAwaitNodeRoom = null
                    RouteState.waitingForTeleport = false
                    RouteState.awaitingTeleportNodeIndex = -1
                    SneakHandler.releaseSneak()
                }
            }
        }

        if (RouteState.awaitingSecrets > 0 || RouteState.awaitingSecretConfirmation || BatListener.isAwaitingBat() || DungeonBreakerListener.isAwaitingDb()) {
            val leftClickDown = mc.options.keyAttack.isDown
            if (leftClickDown && !leftClickWasDown) {
                when {
                    BatListener.isAwaitingBat() -> BatListener.manualTrigger()
                    DungeonBreakerListener.isAwaitingDb() -> DungeonBreakerListener.manualTrigger()
                    else -> SecretListener.manualTrigger()
                }
            }
            leftClickWasDown = leftClickDown
        } else leftClickWasDown = mc.options.keyAttack.isDown

        if (BatListener.isAwaitingBat()) {
            val awaitIndex = BatListener.getAwaitingNodeIndex()
            val awaitNode = BatListener.getAwaitingNode()

            if (awaitNode != null) {
                val nodeWorldPos = RouteUtils.getNodeWorldPosition(awaitNode, room)
                val currentPos = player.position()

                if (!intersectsNode(currentPos, nodeWorldPos, awaitNode.radius, awaitNode.height)) {
                    RouteUtils.debug("§c[Bat] Player left bat await node #$awaitIndex - cancelling")
                    BatListener.cancel()
                }
            }
        }

        if (DungeonBreakerListener.isAwaitingDb()) {
            val awaitIndex = DungeonBreakerListener.getAwaitingNodeIndex()
            val awaitNode = DungeonBreakerListener.getAwaitingNode()

            if (awaitNode != null) {
                val nodeWorldPos = RouteUtils.getNodeWorldPosition(awaitNode, room)
                val currentPos = player.position()

                if (!intersectsNode(currentPos, nodeWorldPos, awaitNode.radius, awaitNode.height)) {
                    RouteUtils.debug("§c[DB] Player left DB await node #$awaitIndex - cancelling")
                    DungeonBreakerListener.cancel()
                }
            }
        }

        if (!RouteState.routeActive && !config.configMode()) checkStartNodeEtherwarp(room)

        if (RouteState.awaitingSecretConfirmation) {
            val node = RouteState.pendingAwaitNode
            if (node?.type == WPType.ETHER && !SneakHandler.isSneaking()) SneakHandler.setSneak(true)
            if (SilentSwap.isSilent()) {
                RouteUtils.debug("§eWaiting for SilentSwap to finish...")
                RouteState.previousPosition = player.position()
                return
            }
            RouteState.secretConfirmationTicks--
            RouteUtils.debug("§eWaiting for SecretAura... ${RouteState.secretConfirmationTicks} ticks")
            if (RouteState.secretConfirmationTicks <= 0) {
                val idx = RouteState.pendingAwaitNodeIndex
                val awaitRoom = RouteState.pendingAwaitNodeRoom
                if (node != null) {
                    RouteUtils.debug("§aSecretAura wait finished. Executing node #$idx")
                    RouteState.lock()
                    if (BurstMode.enabled && node.type == WPType.ETHER) {
                        val chain = BurstMode.findBurstChain(node, idx, RouteState.nodeList, awaitRoom, skipFirstNodeChecks = true)
                        if (chain.nodes.size > 1) { RouteUtils.debug("§6[Burst] Starting burst chain from await node #$idx (${chain.nodes.size} nodes)"); BurstMode.executeBurstChain(chain, awaitRoom) }
                        else doNodeAction(node, idx, awaitRoom)
                    } else doNodeAction(node, idx, awaitRoom)
                }
                RouteState.awaitingSecretConfirmation = false
                RouteState.pendingAwaitNode = null
                RouteState.pendingAwaitNodeIndex = -1
                RouteState.pendingAwaitNodeRoom = null
            }
            RouteState.previousPosition = player.position()
            return
        }

        if (RouteState.isDelaying()) {
            RouteState.delayTicksRemaining--
            if (RouteState.delayTicksRemaining <= 0) {
                val node = RouteState.delayingNode
                val idx = RouteState.delayingNodeIndex
                val delayRoom = RouteState.delayingNodeRoom
                if (node != null) {
                    RouteUtils.debug("§aDelay finished Executing node #$idx")
                    RouteState.lock()
                    if (BurstMode.enabled && node.type == WPType.ETHER) {
                        val chain = BurstMode.findBurstChain(node, idx, RouteState.nodeList, delayRoom, skipFirstNodeChecks = true)
                        if (chain.nodes.size > 1) { RouteUtils.debug("§6[Burst] Starting burst chain from delay node #$idx (${chain.nodes.size} nodes)"); BurstMode.executeBurstChain(chain, delayRoom) }
                        else doNodeAction(node, idx, delayRoom)
                    } else doNodeAction(node, idx, delayRoom)
                }
                RouteState.delayingNode = null
                RouteState.delayingNodeIndex = -1
                RouteState.delayingNodeRoom = null
            }
            RouteState.previousPosition = player.position()
            return
        }

        if (RouteState.isLocked() || RouteState.awaitingSecrets > 0) { RouteState.previousPosition = player.position(); return }

        val currentPos = player.position()
        checkIntersection(RouteState.previousPosition, currentPos, room)
        RouteState.previousPosition = currentPos
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        val packet = event.packet
        if (packet !is ClientboundPlayerPositionPacket) return

        lastTeleportTime = System.currentTimeMillis()

        if (pendingEtherwarps.isNotEmpty()) {
            pendingEtherwarps.removeFirst()
        }

        val pos = packet.change().position()
        val newPos = Vec3(pos.x, pos.y, pos.z)
        RouteState.previousPosition = newPos

        RouteUtils.extraDebug("§a[Packet] Got teleport packet, waitingForTeleport=${RouteState.waitingForTeleport}")

        if (!RouteState.waitingForTeleport) return

        val idx = RouteState.awaitingTeleportNodeIndex
        RouteUtils.debug("§aTeleport received. Node #$idx complete")

        BurstMode.resetExecutingState()

        RouteState.nodeCooldowns[idx] = System.currentTimeMillis()
        RouteState.pendingTeleportNodes.remove(idx)
        RouteState.actionLockedNodes.remove(idx)
        RouteState.actionLockTimes.remove(idx)
        SneakHandler.clearCallbacks()
        RouteState.waitingForTeleport = false
        RouteState.awaitingTeleportNodeIndex = -1
        RouteState.unlock()

        checkLandingNode(newPos)
    }

    private fun checkLandingNode(landingPos: Vec3) {
        val room = RouteState.currentRoom
        val now = System.currentTimeMillis()

        for ((nodeIndex, node) in RouteState.nodeList.withIndex()) {
            if (DungeonBreakerListener.isAwaitingDb() && DungeonBreakerListener.getAwaitingNodeIndex() == nodeIndex) {
                continue
            }
            val lastTrigger = RouteState.nodeCooldowns[nodeIndex]
            if (lastTrigger != null && now - lastTrigger < nodeCooldownMs) continue

            val pendingTime = RouteState.pendingTeleportNodes[nodeIndex]
            if (pendingTime != null) {
                if (now - pendingTime < RouteState.TELEPORT_CONFIRMATION_TIMEOUT_MS) {
                    continue
                } else {
                    RouteState.pendingTeleportNodes.remove(nodeIndex)
                    RouteUtils.debug("§e[Retry] Node #$nodeIndex teleport timed out, allowing retry")
                }
            }

            if (nodeIndex in RouteState.actionLockedNodes) {
                val lockTime = RouteState.actionLockTimes[nodeIndex] ?: 0L
                val timeout = RouteState.getActionLockTimeout(node.type, node)
                if (now - lockTime < timeout) continue
                RouteState.actionLockedNodes.remove(nodeIndex)
                RouteState.actionLockTimes.remove(nodeIndex)
            }

            val nodeWorldPos = RouteUtils.getNodeWorldPosition(node, room)
            if (!intersectsNode(landingPos, nodeWorldPos, node.radius, node.height)) continue

            if (!config.configMode() && !RouteState.routeActive) {
                if (!node.start && !node.force) continue
                if (node.start) {
                    RouteState.routeActive = true
                    RouteUtils.debug("§a[Route] Activated from start node #$nodeIndex (post-teleport)")
                } else {
                    RouteUtils.debug("§a[Route] Force-executing node #$nodeIndex (post-teleport)")
                }
            }

            RouteUtils.debug("§b>>> Post-teleport triggered node #$nodeIndex (${node.type})")
            RouteState.nodeCooldowns[nodeIndex] = now
            executeNode(node, nodeIndex, room)
            return
        }

        RouteUtils.extraDebug("§7[Post-teleport] No node found at landing position")
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent) {
        RouteState.fullReset()
        SneakHandler.releaseSneak()
        BatListener.cancel()
        WalkHandler.reset()
        DungeonBreakerListener.cancel()
        NodeManager.reloadFromDisk()
        pendingEtherwarps.clear()
        RouteState.pendingTeleportNodes.clear()
        BurstMode.resetExecutingState()
        lastStartNode = null
    }

    private fun checkIntersection(prevPos: Vec3, currentPos: Vec3, room: Room?): Boolean {
        val now = System.currentTimeMillis()

        for ((index, node) in RouteState.nodeList.withIndex()) {

            if (DungeonBreakerListener.isAwaitingDb() && DungeonBreakerListener.getAwaitingNodeIndex() == index) {
                continue
            }

            if (index in RouteState.actionLockedNodes) {
                val lockTime = RouteState.actionLockTimes[index] ?: 0L
                val timeout = RouteState.getActionLockTimeout(node.type, node)
                if (now - lockTime < timeout) continue
                RouteState.actionLockedNodes.remove(index)
                RouteState.actionLockTimes.remove(index)
            }

            val lastTrigger = RouteState.nodeCooldowns[index]
            if (lastTrigger != null && now - lastTrigger < nodeCooldownMs) continue

            val pendingTime = RouteState.pendingTeleportNodes[index]
            if (pendingTime != null) {
                if (now - pendingTime < RouteState.TELEPORT_CONFIRMATION_TIMEOUT_MS) {
                    continue
                } else {
                    RouteState.pendingTeleportNodes.remove(index)
                    RouteUtils.debug("§e[Retry] Node #$index teleport timed out, allowing retry")
                }
            }

            val nodeWorldPos = RouteUtils.getNodeWorldPosition(node, room)
            if (!intersectsNode(currentPos, nodeWorldPos, node.radius, node.height)) continue

            if (node.start && !config.configMode()) {
                val arrivedViaTeleport = (now - lastTeleportTime) <= TELEPORT_GRACE_MS
                if (!arrivedViaTeleport) {
                    continue
                }
                RouteUtils.debug("§a[Route] Start node #$index activated via teleport")
            }

            if (!config.configMode() && !RouteState.routeActive) {
                if (!node.start && !node.force) continue
                if (node.start) {
                    RouteState.routeActive = true
                    RouteUtils.debug("§a[Route] Activated from start node #$index")
                } else {
                    RouteUtils.debug("§a[Route] Force-executing node #$index")
                }
            }

            RouteUtils.debug("§b>>> Triggered node #$index (${node.type})")
            RouteUtils.debug("§c[Cooldown] Setting cooldown for node #$index")
            RouteState.nodeCooldowns[index] = now
            executeNode(node, index, room)
            return true
        }
        return false
    }

    private val pendingEtherwarps = mutableListOf<Vec3>()
    private var timeSinceEther = 0L
    private var lastStartNode: WaypointNode? = null

    private fun checkStartNodeEtherwarp(room: Room?) {
        val player = mc.player ?: return

        if (System.currentTimeMillis() - timeSinceEther > 500L) {
            pendingEtherwarps.clear()
        }

        if (!RouteUtils.isHoldingItem("Aspect of the Void")) return
        if (!player.isShiftKeyDown && !player.isCrouching) return

        val etherwarpBlock = if (pendingEtherwarps.isNotEmpty()) {
            Etherwarp.getEtherPos(pendingEtherwarps.last(), 61.0, etherWarp = true)
        } else {
            Etherwarp.getEtherPos(player.position(), 61.0, etherWarp = true)
        }

        if (!etherwarpBlock.succeeded) return
        val etherPos = etherwarpBlock.pos ?: return

        for (node in RouteState.nodeList) {
            if (!node.start) continue

            val nodeBlockPos = BlockPos(floor(node.x).toInt(), node.y.toInt(), floor(node.z).toInt())
            val worldBlockPos = if (DungeonUtils.inDungeons && room != null) {
                room.getRealCoords(nodeBlockPos) ?: nodeBlockPos
            } else {
                nodeBlockPos
            }

            if ((lastStartNode != node || (System.currentTimeMillis() - timeSinceEther) > 500L) && etherPos == worldBlockPos) {

                RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, player.yRot, player.xRot)

                pendingEtherwarps.add(etherwarpBlock.vec3.add(0.5, 0.05 + player.eyeHeight.toDouble(), 0.5))
                timeSinceEther = System.currentTimeMillis()
                lastStartNode = node

                return
            }
        }
    }

    private fun intersectsNode(currentPos: Vec3, nodePos: Vec3, radius: Double, height: Double): Boolean {
        val dx = currentPos.x - nodePos.x
        val dz = currentPos.z - nodePos.z
        val inRadius = dx * dx + dz * dz <= radius * radius
        val inHeight = currentPos.y >= nodePos.y && currentPos.y <= nodePos.y + height
        return inRadius && inHeight
    }

    fun executeNodePublic(node: WaypointNode, index: Int, room: Room?) =
        doNodeAction(node, index, room)

    private fun executeNode(node: WaypointNode, index: Int, room: Room?) {
        RouteState.lock()
        RouteState.actionLockedNodes.add(index)
        RouteState.actionLockTimes[index] = System.currentTimeMillis()

        when {
            node.delay > 0 -> {
                RouteUtils.debug("§eNode #$index delaying ${node.delay} ticks")
                RouteState.delayingNode = node
                RouteState.delayingNodeIndex = index
                RouteState.delayingNodeRoom = room
                RouteState.delayTicksRemaining = node.delay
                RouteState.unlock()
            }
            node.awaitBat -> {
                RouteUtils.debug("§eNode #$index waiting for bat spawn")
                BatListener.startWaitingForBat(node, index, room)
                RouteState.unlock()
            }

            node.awaitDb && node.awaitSecret > 0 -> {
                RouteUtils.debug("§eNode #$index requires ${node.awaitSecret} secret(s) AND DB blocks to be mined")

                if (DungeonBreakerListener.shouldWaitForDb(node, room)) {
                    SecretListener.setAwaitType(node.awaitType)
                    RouteState.awaitingSecrets = node.awaitSecret
                    RouteState.pendingNodeIndex = index

                    DungeonBreakerListener.startWaitingForDbAndSecrets(node, index, room)
                    RouteState.unlock()
                } else {
                    SecretListener.setAwaitType(node.awaitType)
                    if (node.type == WPType.ETHER) { RouteUtils.debug("§eETHER await node - starting sneak"); SneakHandler.setSneak(true) }
                    else if (node.type == WPType.AOTV) { RouteUtils.debug("§eAOTV await node - releasing sneak"); SneakHandler.releaseSneak() }
                    RouteState.awaitingSecrets = node.awaitSecret
                    RouteState.pendingNodeIndex = index
                    RouteState.unlock()
                }
            }

            node.awaitDb -> {
                if (DungeonBreakerListener.shouldWaitForDb(node, room)) {
                    RouteUtils.debug("§eNode #$index waiting for DB blocks to be mined")
                    if (node.type == WPType.ETHER) { RouteUtils.debug("§eETHER DB node - starting sneak"); SneakHandler.setSneak(true) }
                    DungeonBreakerListener.startWaitingForDb(node, index, room)
                    RouteState.unlock()
                } else {
                    doNodeAction(node, index, room)
                }
            }
            node.awaitSecret > 0 -> {
                RouteUtils.debug("§eNode #$index requires ${node.awaitSecret} secret(s) of type: ${node.awaitType}")
                SecretListener.setAwaitType(node.awaitType)
                if (node.type == WPType.ETHER) { RouteUtils.debug("§eETHER await node - starting sneak"); SneakHandler.setSneak(true) }
                else if (node.type == WPType.AOTV) { RouteUtils.debug("§eAOTV await node - releasing sneak"); SneakHandler.releaseSneak() }
                RouteState.awaitingSecrets = node.awaitSecret
                RouteState.pendingNodeIndex = index
                RouteState.unlock()
            }
            else -> doNodeAction(node, index, room)
        }
    }

    private fun doNodeAction(node: WaypointNode, index: Int, room: Room?) {
        val player = mc.player ?: run { RouteState.unlock(); return }

        if (node.stop) player.deltaMovement = Vec3(0.0, player.deltaMovement.y, 0.0)
        if (node.center) RouteUtils.getNodeWorldPosition(node, room).let { player.setPos(it.x, player.y, it.z) }

        val realYaw = RouteUtils.getRealYaw(node.yaw, room)
        val pitch = node.pitch
        RouteUtils.debug("§d  yaw=${"%.1f".format(realYaw)}, pitch=${"%.1f".format(pitch)}")

        if (BurstMode.shouldBurst(node)) {
            val chain = BurstMode.findBurstChain(node, index, RouteState.nodeList, room)
            if (chain.nodes.size > 1) {
                RouteUtils.debug("§6[Burst] Executing ${chain.nodes.size}-node ETHER chain from #$index")
                RouteState.pendingTeleportNodes[index] = System.currentTimeMillis()
                BurstMode.executeBurstChain(chain, room)
                return
            }
            RouteUtils.debug("§7[Burst] Single ETHER node, using normal execution")
        }

        if (BurstMode.shouldAotvBurst(node)) {
            val chain = BurstMode.findAotvBurstChain(node, index, RouteState.nodeList)
            if (chain.nodes.size > 1) {
                RouteUtils.debug("§6[AOTV Burst] Executing ${chain.nodes.size}-node AOTV chain from #$index")
                RouteState.pendingTeleportNodes[index] = System.currentTimeMillis()
                BurstMode.executeAotvBurstChain(chain, room)
                return
            }
            RouteUtils.debug("§7[AOTV Burst] Single AOTV node, using normal execution")
        }

        if (node.type == WPType.ETHER || node.type == WPType.AOTV || node.type == WPType.HYPE) {
            RouteState.waitingForTeleport = true
            RouteState.awaitingTeleportNodeIndex = index
            RouteState.pendingTeleportNodes[index] = System.currentTimeMillis()
        }

        when (node.type) {
            WPType.ETHER -> executeEther(realYaw, pitch, index, room)
            WPType.AOTV -> executeAotv(realYaw, pitch, node.mult)
            WPType.HYPE -> executeHype(realYaw, pitch, index, room)
            WPType.SUPERBOOM -> executeSuperboom(node, room, realYaw, pitch)
            WPType.USEITEM -> executeUseItem(node, realYaw, pitch)
            WPType.LOOK -> executeLook(realYaw, pitch)
            WPType.NOP -> { RouteUtils.debug("§7  NOP - no action"); RouteState.unlock() }
            WPType.WALK -> executeWalk(realYaw, pitch)
            WPType.STOP -> executeStop()
        }
    }

    private fun executeEther(yaw: Float, pitch: Float, nodeIndex: Int, room: Room?) {
        val currentNode = RouteState.nodeList.getOrNull(nodeIndex)
        val shouldReleaseSneak = if (currentNode != null) {
            val nodeWorldPos = RouteUtils.getNodeWorldPosition(currentNode, room)
            val landingPos = BurstMode.predictEtherwarpLanding(currentNode, nodeWorldPos, room)
            val landingNode = if (landingPos != null) {
                BurstMode.findNodeAtLandingPosition(landingPos, RouteState.nodeList, room, setOf(nodeIndex))
            } else null
            landingNode?.type == WPType.AOTV
        } else false

        if (SneakHandler.isSneaking()) {
            doEtherClick(yaw, pitch, shouldReleaseSneak)
        } else {
            SneakHandler.setSneak(true) { doEtherClick(yaw, pitch, shouldReleaseSneak) }
        }
    }

    private fun doEtherClick(yaw: Float, pitch: Float, releaseSneak: Boolean = false) {
        if (RouteUtils.swapToItem("Aspect of the Void") == SwapResult.FAIL) {
            RouteUtils.debug("§c  Failed to swap to AOTV")
            SneakHandler.releaseSneak()
            RouteState.unlock()
            RouteState.waitingForTeleport = false
            return
        }
        RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, yaw, pitch)

        if (releaseSneak) {
            RouteUtils.debug("§e[Ether] Releasing sneak for AOTV landing")
            SneakHandler.releaseSneak()
        }

        if (!RouteState.waitingForTeleport) RouteState.unlock()
    }

    private fun executeAotv(yaw: Float, pitch: Float, mult: Int = 1) {
        val player = mc.player ?: run { RouteState.unlock(); return }
        RouteUtils.debug("§b[AOTV] Before - playerYaw=${player.yRot}, playerPitch=${player.xRot}")
        RouteUtils.debug("§b[AOTV] Packet will use yaw=$yaw, pitch=$pitch, mult=$mult")

        SneakHandler.releaseSneak()
        if (RouteUtils.swapToItem("Aspect of the Void") == SwapResult.FAIL) {
            RouteUtils.debug("§c  Failed to swap to AOTV")
            RouteState.unlock()
            return
        }

        repeat(mult) { i ->
            RouteUtils.debug("§b[AOTV] Sending click ${i + 1}/$mult")
            RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, yaw, pitch)
        }

        RouteUtils.debug("§b[AOTV] After - playerYaw=${player.yRot}, playerPitch=${player.xRot}")
        if (!RouteState.waitingForTeleport) RouteState.unlock()
    }

    private fun executeHype(yaw: Float, pitch: Float, nodeIndex: Int, room: Room?) {
        val nextNode = RouteState.nodeList.getOrNull(nodeIndex + 1)
        val shouldReleaseSneak = nextNode?.type == WPType.AOTV

        if (SneakHandler.isSneaking()) {
            doHypeClick(yaw, pitch, shouldReleaseSneak)
        } else {
            SneakHandler.setSneak(true) { doHypeClick(yaw, pitch, shouldReleaseSneak) }
        }
    }

    private fun doHypeClick(yaw: Float, pitch: Float, releaseSneak: Boolean = false) {
        val witherBlades = listOf("Hyperion", "Scylla", "Astrea", "Valkyrie")
        val result = witherBlades.firstNotNullOfOrNull { blade ->
            RouteUtils.swapToItem(blade).takeIf { it != SwapResult.FAIL }
        }

        if (result == null) {
            RouteUtils.debug("§c  No wither blade found")
            SneakHandler.releaseSneak()
            RouteState.unlock()
            RouteState.waitingForTeleport = false
            return
        }

        RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, yaw, pitch)

        if (releaseSneak) {
            RouteUtils.debug("§e[Hype] Releasing sneak for AOTV landing")
            SneakHandler.releaseSneak()
        }

        if (!RouteState.waitingForTeleport) RouteState.unlock()
    }

    private fun executeSuperboom(node: WaypointNode, room: Room?, yaw: Float, pitch: Float) {
        val targetBlock = node.targetBlock ?: run { RouteUtils.debug("§c  Superboom has no target block"); RouteState.unlock(); return }
        if (RouteUtils.swapToItem("Superboom") == SwapResult.FAIL) { RouteUtils.debug("§c  Failed to swap to Superboom TNT"); RouteState.unlock(); return }

        val player = mc.player ?: run { RouteState.unlock(); return }
        val worldTarget = if (DungeonUtils.inDungeons && room != null) room.getRealCoords(targetBlock) ?: targetBlock else targetBlock
        val hit = RouteUtils.raytraceToBlock(player.eyePosition, worldTarget) ?: run { RouteUtils.debug("§c  Could not raytrace to superboom target"); RouteState.unlock(); return }

        RightClickHandler.doBlockInteract(hit)
        RouteState.unlock()
    }

    private fun executeUseItem(node: WaypointNode, yaw: Float, pitch: Float) {
        val itemName = node.itemName ?: run { RouteUtils.debug("§c  UseItem has no item name"); RouteState.unlock(); return }
        if (RouteUtils.swapToItem(itemName) == SwapResult.FAIL) { RouteUtils.debug("§c  Failed to swap to $itemName"); RouteState.unlock(); return }
        RightClickHandler.doPacketInteract(InteractionHand.MAIN_HAND, yaw, pitch)
        RouteState.unlock()
    }

    private fun executeLook(yaw: Float, pitch: Float) {
        val player = mc.player ?: run { RouteState.unlock(); return }
        player.yRot = yaw
        player.xRot = pitch
        SneakHandler.releaseSneak()
        RouteState.routeActive = false
        RouteUtils.debug("§c[Route] Ended (look node)")
        RouteState.unlock()
    }

    private fun executeWalk(yaw: Float, pitch: Float) {
        RouteUtils.debug("§a[Walk] Walking: yaw=${"%.1f".format(yaw)}, pitch=${"%.1f".format(pitch)}")

        SneakHandler.releaseSneak()

        WalkHandler.startWalk(yaw, pitch) {
            RouteUtils.debug("§a[Walk] Walk ended by manual input")
        }

        RouteState.unlock()
    }

    private fun executeStop() {
        RouteUtils.debug("§c[Stop] Stopping all movement")

        if (WalkHandler.isWalking()) {
            WalkHandler.stopWalk()
        }

        WalkHandler.releaseAllMovement()
        SneakHandler.releaseSneak()
        RouteState.unlock()
    }

    fun onSecretCollected() {
        if (RouteState.awaitingSecrets <= 0) return
        RouteState.awaitingSecrets--
        RouteUtils.debug("§aSecret! (${RouteState.awaitingSecrets} remaining)")
        if (RouteState.awaitingSecrets > 0) return

        val idx = RouteState.pendingNodeIndex
        val node = RouteState.nodeList.getOrNull(idx)
        val room = RouteState.currentRoom
        if (node != null) { RouteUtils.debug("§a§lSecrets done, executing node #$idx"); RouteState.lock(); doNodeAction(node, idx, room) }
        RouteState.pendingNodeIndex = -1
    }

    private fun getCurrentKey(room: Room?): String? =
        if (!LocationUtils.isInSkyblock) null else if (DungeonUtils.inDungeons) room?.data?.name else LocationUtils.currentArea?.toString()
}
