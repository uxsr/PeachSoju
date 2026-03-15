package com.peachsoju.utils.handlers

import com.peachsoju.PeachSoju.mc
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3

object EntityInteractionHandler {

    fun getHitVector(entity: Entity): Vec3? {
        val player = mc.player ?: return null
        val eyePos = player.eyePosition
        val aabb = entity.boundingBox.inflate(entity.pickRadius.toDouble())
        val center = aabb.center
        return aabb.clip(eyePos, center).orElse(null)
    }

    fun interact(entity: Entity, hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult {
        val hitVector = getHitVector(entity) ?: return InteractionResult.PASS
        return interactAt(entity, hitVector, hand)
    }

    fun interactAt(entity: Entity, hitVector: Vec3, hand: InteractionHand = InteractionHand.MAIN_HAND): InteractionResult {
        val gameMode = mc.gameMode ?: return InteractionResult.PASS
        val player = mc.player ?: return InteractionResult.PASS

        if (gameMode.playerMode == GameType.SPECTATOR) return InteractionResult.PASS

        val hitResult = EntityHitResult(entity, hitVector)

        val interactAtResult = gameMode.interactAt(player, entity, hitResult, hand)
        if (interactAtResult.consumesAction()) {
            return interactAtResult
        }

        return gameMode.interact(player, entity, hand)
    }

    fun distanceToEntity(entity: Entity): Double {
        val player = mc.player ?: return Double.MAX_VALUE
        return player.eyePosition.distanceTo(entity.position())
    }

    inline fun <reified T : Entity> findNearestEntity(
        range: Double,
        crossinline predicate: (T) -> Boolean = { true }
    ): T? {
        val level = mc.level ?: return null
        val player = mc.player ?: return null
        val eyePos = player.eyePosition

        return level.entitiesForRendering()
            .filterIsInstance<T>()
            .filter { predicate(it) && eyePos.distanceTo(it.position()) <= range }
            .minByOrNull { eyePos.distanceTo(it.position()) }
    }

    inline fun <reified T : Entity> findEntitiesInRange(
        range: Double,
        crossinline predicate: (T) -> Boolean = { true }
    ): List<T> {
        val level = mc.level ?: return emptyList()
        val player = mc.player ?: return null ?: emptyList()
        val eyePos = player.eyePosition

        return level.entitiesForRendering()
            .filterIsInstance<T>()
            .filter { predicate(it) && eyePos.distanceTo(it.position()) <= range }
            .sortedBy { eyePos.distanceTo(it.position()) }
    }
}