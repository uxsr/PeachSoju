package com.peachsoju.modules.impl.autoroutes.data

import net.minecraft.core.BlockPos

enum class WPType { ETHER, AOTV, HYPE, SUPERBOOM, USEITEM, LOOK, NOP, WALK, STOP;
    companion object { fun fromString(str: String): WPType? = runCatching { valueOf(str.uppercase()) }.getOrNull() }
}

data class WaypointNode(
    val x: Double, val y: Double, val z: Double, val exact: Boolean = false,
    val type: WPType,
    val yaw: Float, val pitch: Float,
    val chained: Boolean = false, val radius: Double = 0.5, val height: Double = 1.5, val start: Boolean = false,
    val delay: Int = 0, val stop: Boolean = false, val center: Boolean = false,
    val awaitSecret: Int = 0, val awaitBat: Boolean = false, val awaitType: String = "any",
    val awaitDb: Boolean = false,
    val toBlock: BlockPos? = null,
    val targetBlock: BlockPos? = null,
    val itemName: String? = null,
    val mult: Int = 1
) {
    fun copyWith(
        x: Double = this.x, y: Double = this.y, z: Double = this.z, exact: Boolean = this.exact, type: WPType = this.type,
        yaw: Float = this.yaw, pitch: Float = this.pitch, chained: Boolean = this.chained, radius: Double = this.radius,
        height: Double = this.height, start: Boolean = this.start, delay: Int = this.delay, stop: Boolean = this.stop,
        center: Boolean = this.center, awaitSecret: Int = this.awaitSecret, awaitBat: Boolean = this.awaitBat,
        awaitType: String = this.awaitType, awaitDb: Boolean = this.awaitDb, toBlock: BlockPos? = this.toBlock,
        targetBlock: BlockPos? = this.targetBlock, itemName: String? = this.itemName, mult: Int = this.mult
    ): WaypointNode = WaypointNode(
        x, y, z, exact, type, yaw, pitch, chained, radius, height, start,
        delay, stop, center, awaitSecret, awaitBat, awaitType, awaitDb, toBlock, targetBlock, itemName, mult
    )
}