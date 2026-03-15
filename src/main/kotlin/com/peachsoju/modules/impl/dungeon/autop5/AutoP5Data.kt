package com.peachsoju.modules.impl.dungeon.autop5

import com.odtheking.odin.features.impl.floor7.WitherDragonsEnum
import net.minecraft.world.phys.Vec3

/**
 * Dragon debuff positions - where to stand to debuff each dragon.
 * These are the optimal positions for Last Breath usage.
 */
object DragonDebuffPositions {

    private val positions = mapOf(
        WitherDragonsEnum.Red to Vec3(28.0, 6.0, 56.0),
        WitherDragonsEnum.Orange to Vec3(82.5, 6.0, 57.5),
        WitherDragonsEnum.Green to Vec3(27.0, 6.0, 91.5),
        WitherDragonsEnum.Blue to Vec3(82.5, 6.0, 97.5),
        WitherDragonsEnum.Purple to Vec3(56.5, 8.0, 123.5)
    )

    val MIDDLE_POSITION = Vec3(54.5, 5.0, 76.5)

    fun getDebuffPosition(dragon: WitherDragonsEnum): Vec3 {
        return positions[dragon] ?: dragon.spawnPos.let {
            Vec3(it.x + 0.5, it.y.toDouble() - 8.0, it.z + 0.5)
        }
    }

    fun getClosestDragon(playerX: Double, playerZ: Double): WitherDragonsEnum {
        return WitherDragonsEnum.entries.minBy { dragon ->
            val pos = getDebuffPosition(dragon)
            val dx = pos.x - playerX
            val dz = pos.z - playerZ
            dx * dx + dz * dz
        }
    }
}

/**
 * State tracking for AutoP5 that complements Odin's tracking.
 */
object AutoP5State {
    var isActive = false
    var assignedDragon: WitherDragonsEnum? = null
    var isPathfinding = false
    var isAtDebuffPosition = false
    var lastDragonDeathTime = 0L

    fun reset() {
        isActive = false
        assignedDragon = null
        isPathfinding = false
        isAtDebuffPosition = false
        lastDragonDeathTime = 0L
    }

    fun onDragonAssigned(dragon: WitherDragonsEnum) {
        assignedDragon = dragon
        isAtDebuffPosition = false
    }

    fun onArrivedAtPosition() {
        isAtDebuffPosition = true
        isPathfinding = false
    }

    fun onDragonDeath() {
        lastDragonDeathTime = System.currentTimeMillis()
        assignedDragon = null
        isAtDebuffPosition = false
    }
}