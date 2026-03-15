package com.peachsoju.modules.impl.dungeon.puzzles.autoss

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.BlockUpdateEvent
import com.peachsoju.eventbus.events.PacketEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.eventbus.events.WorldEvent
import com.peachsoju.utils.handlers.RightClickHandler
import com.peachsoju.utils.handlers.drawFilledBox
import com.peachsoju.utils.handlers.drawText
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

object AutoSS {

    private val enabled: Boolean get() = config.autoSS()
    private val delay: Double get() = config.autoSSDelay()
    private val forceDevice: Boolean get() = config.autoSSForceDevice()
    private val autoStartDelay: Double get() = config.autoSSAutoStartDelay()
    private val dontCheck: Boolean get() = config.autoSSDontCheck()

    private var lastClickTime: Long = 0L
    private var progress: Int = 0
    private var doneFirst: Boolean = false
    private var doingSS: Boolean = false
    private var clicked: Boolean = false
    private var clicks: ArrayList<BlockPos> = ArrayList()
    private var clickedButton: Vec3? = null
    private var allButtons: ArrayList<Vec3> = ArrayList()
    private val startButton: BlockPos = BlockPos(110, 121, 91)
    private var lastInteractTime: Long = 0L

    private var startupPhase: Int = 0
    private var startupClickTime: Long = 0L

    private val PEACH = Color(212, 149, 106, 0.7f)

    fun reset() {
        allButtons.clear()
        clicks.clear()
        progress = 0
        doneFirst = false
        doingSS = false
        clicked = false
        startupPhase = 0
        extraDebug("Reset!")
    }

    fun onKeyBind() {
        start()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent) {
        reset()
    }

    fun start() {
        val player = mc.player ?: return
        if (player.blockPosition().distSqr(startButton) > 25.0) return

        if (!clicked) {
            extraDebug("Starting SS")
            reset()
            clicked = true
            startupPhase = 1
            startupClickTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (!enabled) return

        val player = mc.player ?: return
        if (player.blockPosition().distSqr(startButton) > 25.0) return

        if (startupPhase > 0) {
            handleStartupSequence()
            return
        }

        ssLoop()
    }

    private fun handleStartupSequence() {
        val now = System.currentTimeMillis()
        val timeSinceLastClick = now - startupClickTime

        when (startupPhase) {
            1 -> {
                clickButton(startButton.x, startButton.y, startButton.z)
                reset()
                startupPhase = 2
                startupClickTime = now
                extraDebug("Startup click 1")
            }
            2 -> {
                if (timeSinceLastClick >= autoStartDelay) {
                    clickButton(startButton.x, startButton.y, startButton.z)
                    reset()
                    startupPhase = 3
                    startupClickTime = now
                    extraDebug("Startup click 2")
                }
            }
            3 -> {
                if (timeSinceLastClick >= autoStartDelay) {
                    clickButton(startButton.x, startButton.y, startButton.z)
                    doingSS = true
                    startupPhase = 0
                    extraDebug("Startup click 3 - SS active!")
                }
            }
        }
    }

    private fun ssLoop() {
        val level = mc.level ?: return
        val player = mc.player ?: return

        if (!LocationUtils.isInSkyblock && !forceDevice) return

        val now = System.currentTimeMillis()
        if (now - lastClickTime < delay) return

        var device = false
        val searchBox = player.boundingBox.inflate(6.0)
        val armorStands = level.getEntitiesOfClass(ArmorStand::class.java, searchBox)

        for (stand in armorStands) {
            val name = stand.displayName?.string ?: stand.customName?.string ?: ""
            if (name.contains("Device")) {
                device = true
                break
            }
        }

        if (forceDevice) device = true

        if (!device) {
            clicked = false
            return
        }

        if (!doingSS) return

        val detectPos = BlockPos(110, 123, 92)
        val detectBlock = level.getBlockState(detectPos).block
        val hasStoneButton = detectBlock == Blocks.STONE_BUTTON

        if ((hasStoneButton || (dontCheck && doneFirst))) {
            if (!doneFirst && clicks.size == 3) {
                clicks.removeAt(0)
                allButtons.removeAt(0)
                extraDebug("Removed first click (had 3), now have ${clicks.size}")
            }

            doneFirst = true

            if (progress < clicks.size) {
                val nextButton: BlockPos = clicks[progress]

                if (level.getBlockState(nextButton).block == Blocks.STONE_BUTTON) {
                    clickButton(nextButton.x, nextButton.y, nextButton.z)
                    progress++
                    extraDebug("Clicked button #$progress")
                }
            }
        }
    }

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.Receive) {
        val pkt = event.packet as? ClientboundSystemChatPacket ?: return
        val msg = pkt.content().string

        val player = mc.player ?: return
        if (player.blockPosition().distSqr(startButton) > 25.0) return

        if (msg.contains("Who dares trespass into my domain", ignoreCase = true)) {
            extraDebug("Detected trespass message - Starting SS")
            start()
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderEvent.Extract) {
        if (!enabled) return
        if (!LocationUtils.isInSkyblock && !forceDevice) return
        if (mc.level == null) return

        val player = mc.player ?: return

        if (System.currentTimeMillis() - lastClickTime > delay) {
            clickedButton = null
        }

        if (player.blockPosition().distSqr(startButton) < 1600.0) {
            clickedButton?.let { btn ->
                val box = AABB(
                    btn.x + 0.875, btn.y + 0.375, btn.z + 0.3125,
                    btn.x + 0.875 + 0.125, btn.y + 0.375 + 0.25, btn.z + 0.3125 + 0.375
                )
                event.drawFilledBox(box, PEACH, depth = false)
            }

            allButtons.forEachIndexed { index, location ->
                val textPos = Vec3(
                    location.x - 0.0625,
                    location.y + 0.5625,
                    location.z + 0.5
                )
                event.drawText((index + 1).toString(), textPos, 0.6f, depth = false)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PacketEvent.Send) {
        if (event.packet !is ServerboundUseItemOnPacket) return

        val player = mc.player ?: return
        val packet = event.packet as ServerboundUseItemOnPacket
        val hitResult = packet.hitResult

        val now = System.currentTimeMillis()
        if (now - lastInteractTime < 1000L) return
        lastInteractTime = now

        if (hitResult.blockPos == startButton) {
            clicked = false
            reset()
            start()
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: BlockUpdateEvent) {
        val pos = event.pos
        val block = event.blockState.block

        if (!enabled) return
        if (!doingSS) return

        if (pos.x == 111 && pos.y >= 120 && pos.y <= 123 && pos.z >= 92 && pos.z <= 95) {
            val button = BlockPos(110, pos.y, pos.z)

            if (block == Blocks.SEA_LANTERN) {
                extraDebug("Sea lantern detected at $pos")

                if (clicks.size == 2 && clicks[0] == button && !doneFirst) {
                    doneFirst = true
                    clicks.removeFirstOrNull()
                    allButtons.removeFirstOrNull()
                    extraDebug("Removed duplicate first click")
                }

                if (!clicks.contains(button)) {
                    extraDebug("Added button: $button (total: ${clicks.size + 1})")
                    progress = 0
                    clicks.add(button)
                    allButtons.add(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble()))
                }
            }
        }
    }

    private fun clickButton(x: Int, y: Int, z: Int) {
        val player = mc.player ?: return

        if (player.blockPosition().distSqr(BlockPos(x, y, z)) > 25.0) return

        extraDebug("Clicking button at: ($x, $y, $z)")
        clickedButton = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
        lastClickTime = System.currentTimeMillis()

        val blockPos = BlockPos(x, y, z)
        val hitVec = Vec3(x + 0.875, y + 0.5, z + 0.5)
        val blockHitResult = BlockHitResult(
            hitVec,
            Direction.EAST,
            blockPos,
            false
        )

        RightClickHandler.doBlockInteract(blockHitResult, InteractionHand.MAIN_HAND)
    }

    private fun extraDebug(msg: String) {
        if (config.extraDebug()) {
            mc.player?.displayClientMessage(
                Component.literal("§7[AutoSS] $msg"),
                false
            )
        }
    }

}