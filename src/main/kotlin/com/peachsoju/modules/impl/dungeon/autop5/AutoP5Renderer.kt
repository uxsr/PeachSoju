package com.peachsoju.modules.impl.dungeon.autop5

import com.peachsoju.PeachSoju
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.RenderEvent
import com.peachsoju.utils.handlers.drawLine
import com.peachsoju.utils.handlers.drawFilledBox
import com.peachsoju.utils.handlers.drawWireFrameBox
import com.peachsoju.utils.handlers.drawText
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import com.odtheking.odin.features.impl.floor7.WitherDragonsEnum
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Renders debug visualization for AutoP5.
 */
object AutoP5Renderer {

    // Dragon colors
    private val DRAGON_COLORS = mapOf(
        WitherDragonsEnum.Red to Color(255, 85, 85, 0.6f),
        WitherDragonsEnum.Orange to Color(255, 170, 0, 0.6f),
        WitherDragonsEnum.Green to Color(85, 255, 85, 0.6f),
        WitherDragonsEnum.Blue to Color(85, 170, 255, 0.6f),
        WitherDragonsEnum.Purple to Color(170, 85, 255, 0.6f)
    )

    private val PATH_COLOR = Color(255, 255, 85, 0.8f)
    private val WAYPOINT_COLOR = Color(85, 255, 255, 0.5f)
    private val TARGET_COLOR = Color(85, 255, 85, 0.7f)

    fun initialize() {
        PeachSoju.eventBus.register(this)
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderEvent.Extract) {
        if (!config.autoP5Enabled()) return
        if (!config.autoP5Debug()) return
        if (DungeonUtils.getF7Phase() != M7Phases.P5) return

        renderDebuffPositions(event)
        renderPath(event)
        renderAssignedDragon(event)
    }

    /**
     * Render all dragon debuff positions.
     */
    private fun renderDebuffPositions(event: RenderEvent.Extract) {
        for (dragon in WitherDragonsEnum.entries) {
            val pos = DragonDebuffPositions.getDebuffPosition(dragon)
            val color = DRAGON_COLORS[dragon] ?: continue

            val aabb = AABB(
                pos.x - 0.3, pos.y, pos.z - 0.3,
                pos.x + 0.3, pos.y + 0.1, pos.z + 0.3
            )

            event.drawFilledBox(aabb, color, depth = false)
            event.drawText(
                "§${dragon.colorCode}${dragon.name}",
                pos.add(0.0, 1.5, 0.0),
                2f,
                false
            )
        }

        // Render middle position
        val middlePos = DragonDebuffPositions.MIDDLE_POSITION
        val middleAABB = AABB(
            middlePos.x - 0.3, middlePos.y, middlePos.z - 0.3,
            middlePos.x + 0.3, middlePos.y + 0.1, middlePos.z + 0.3
        )
        event.drawFilledBox(middleAABB, Color(255, 255, 255, 0.5f), depth = false)
        event.drawText("§fMiddle", middlePos.add(0.0, 1.5, 0.0), 2f, false)
    }

    /**
     * Render current pathfinding path.
     */
    private fun renderPath(event: RenderEvent.Extract) {
        val path = AutoP5Pathfinder.getCurrentPath() ?: return
        if (path.isEmpty()) return

        val currentIndex = AutoP5Pathfinder.getCurrentIndex()

        // Draw path line
        for (i in 0 until path.size - 1) {
            val from = path[i]
            val to = path[i + 1]

            val fromVec = Vec3(from.x + 0.5, from.y + 0.1, from.z + 0.5)
            val toVec = Vec3(to.x + 0.5, to.y + 0.1, to.z + 0.5)

            // Different color for completed vs remaining path
            val color = if (i < currentIndex) {
                Color(100, 100, 100, 0.4f) // Gray for completed
            } else {
                PATH_COLOR
            }

            event.drawLine(listOf(fromVec, toVec), color, depth = false, thickness = 3f)
        }

        // Draw waypoint markers for remaining path
        for (i in currentIndex until path.size) {
            val pos = path[i]
            val aabb = AABB(
                pos.x + 0.3, pos.y.toDouble(), pos.z + 0.3,
                pos.x + 0.7, pos.y + 0.2, pos.z + 0.7
            )
            event.drawWireFrameBox(aabb, WAYPOINT_COLOR, depth = false)
        }
    }

    /**
     * Highlight assigned dragon position.
     */
    private fun renderAssignedDragon(event: RenderEvent.Extract) {
        val dragon = AutoP5State.assignedDragon ?: return
        val pos = DragonDebuffPositions.getDebuffPosition(dragon)
        val color = DRAGON_COLORS[dragon] ?: return

        // Larger highlight box
        val aabb = AABB(
            pos.x - 0.5, pos.y, pos.z - 0.5,
            pos.x + 0.5, pos.y + 2.0, pos.z + 0.5
        )

        // Create a solid version of the color for the wireframe
        val solidColor = Color(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            1f
        )
        event.drawWireFrameBox(aabb, solidColor, depth = false, thickness = 3f)

        // Status text
        val status = when {
            AutoP5State.isAtDebuffPosition -> "§aAt Position"
            AutoP5State.isPathfinding -> "§ePathfinding..."
            else -> "§7Waiting"
        }

        event.drawText(
            "${dragon.colorCode}${dragon.name}\n$status",
            pos.add(0.0, 2.5, 0.0),
            2.5f,
            false
        )
    }
}