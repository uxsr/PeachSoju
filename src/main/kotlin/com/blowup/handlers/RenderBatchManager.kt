package com.blowup.handlers

import com.blowup.eventbus.SubscribeEvent
import com.blowup.eventbus.events.RenderEvent
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.odtheking.mixin.accessors.BeaconBeamAccessor
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.multiplyAlpha
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.render.CustomRenderLayer
import com.odtheking.odin.utils.renderPos
import com.odtheking.odin.utils.unaryMinus
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

private const val depthOn = 0
private const val depthOff = 1
private val beamTexture = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png")

internal data class LineData(val from: Vec3, val to: Vec3, val color1: Int, val color2: Int, val thickness: Float)
internal data class BoxData(val aabb: AABB, val r: Float, val g: Float, val b: Float, val a: Float, val thickness: Float)
internal data class BeaconData(val pos: BlockPos, val color: Color, val isScoping: Boolean, val gameTime: Long)
internal data class TextData(val text: String, val pos: Vec3, val scale: Float, val depth: Boolean, val cameraRotation: Quaternionf, val font: Font, val textWidth: Float)

class RenderConsumer {
    internal val lines = listOf(ObjectArrayList<LineData>(), ObjectArrayList())
    internal val filledBoxes = listOf(ObjectArrayList<BoxData>(), ObjectArrayList())
    internal val wireBoxes = listOf(ObjectArrayList<BoxData>(), ObjectArrayList())
    internal val beaconBeams = ObjectArrayList<BeaconData>()
    internal val texts = ObjectArrayList<TextData>()
    fun clear() {
        lines.forEach { it.clear() }
        filledBoxes.forEach { it.clear() }
        wireBoxes.forEach { it.clear() }
        beaconBeams.clear()
        texts.clear()
    }
}

object RenderBatchManager {

    val renderConsumer = RenderConsumer()

    @SubscribeEvent
    fun onRenderLast(event: RenderEvent.Last) {
        val matrices = event.context.matrices() ?: return
        val bufferSource = event.context.consumers() as? MultiBufferSource.BufferSource ?: return
        val camera = event.context.gameRenderer().mainCamera?.position ?: return

        matrices.pushPose()
        matrices.translate(-camera.x, -camera.y, -camera.z)
        matrices.renderBatchedLinesAndWireBoxes(renderConsumer.lines, renderConsumer.wireBoxes, bufferSource)
        matrices.renderBatchedFilledBoxes(renderConsumer.filledBoxes, bufferSource)
        matrices.popPose()

        matrices.renderBatchedBeaconBeams(renderConsumer.beaconBeams, camera)
        matrices.renderBatchedTexts(renderConsumer.texts, bufferSource, camera)
        renderConsumer.clear()
    }
}

private fun PoseStack.renderBatchedLinesAndWireBoxes(
    lines: List<List<LineData>>,
    wireBoxes: List<List<BoxData>>,
    bufferSource: MultiBufferSource.BufferSource
) {
    val layers = listOf(CustomRenderLayer.LINE_LIST, CustomRenderLayer.LINE_LIST_ESP)
    val last = last()
    for (depthState in 0..1) {
        if (lines[depthState].isEmpty() && wireBoxes[depthState].isEmpty()) continue
        val buffer = bufferSource.getBuffer(layers[depthState])

        for (line in lines[depthState]) {
            val dx = line.to.x - line.from.x
            val dy = line.to.y - line.from.y
            val dz = line.to.z - line.from.z
            PrimitiveRenderer.renderVector(
                last,
                buffer,
                Vector3f(line.from.x.toFloat(), line.from.y.toFloat(), line.from.z.toFloat()),
                Vec3(dx, dy, dz),
                line.color1,
                line.color2
            )
        }

        for (box in wireBoxes[depthState]) PrimitiveRenderer.renderLineBox(last, buffer, box.aabb, box.r, box.g, box.b, box.a)
        bufferSource.endBatch(layers[depthState])
    }
}

private fun PoseStack.renderBatchedFilledBoxes(boxesByDepth: List<List<BoxData>>, bufferSource: MultiBufferSource.BufferSource) {
    val layers = listOf(CustomRenderLayer.TRIANGLE_STRIP, CustomRenderLayer.TRIANGLE_STRIP_ESP)
    val last = last()
    for ((depthState, boxes) in boxesByDepth.withIndex()) {
        if (boxes.isEmpty()) continue
        val buffer = bufferSource.getBuffer(layers[depthState])
        for (box in boxes) {
            PrimitiveRenderer.addChainedFilledBoxVertices(
                last,
                buffer,
                box.aabb.minX.toFloat(),
                box.aabb.minY.toFloat(),
                box.aabb.minZ.toFloat(),
                box.aabb.maxX.toFloat(),
                box.aabb.maxY.toFloat(),
                box.aabb.maxZ.toFloat(),
                box.r,
                box.g,
                box.b,
                box.a
            )
        }
        bufferSource.endBatch(layers[depthState])
    }
}

private fun PoseStack.renderBatchedBeaconBeams(beacons: List<BeaconData>, camera: Vec3) {
    for (beacon in beacons) {
        pushPose()
        translate(beacon.pos.x - camera.x, beacon.pos.y - camera.y, beacon.pos.z - camera.z)

        val cx = beacon.pos.x + 0.5
        val cz = beacon.pos.z + 0.5
        val dx = camera.x - cx
        val dz = camera.z - cz
        val length = sqrt(dx * dx + dz * dz).toFloat()
        val scale = if (beacon.isScoping) 1f else maxOf(1f, length * 0.010416667f)

        BeaconBeamAccessor.invokeRenderBeam(
            this,
            mc.gameRenderer.featureRenderDispatcher.submitNodeStorage,
            beamTexture,
            1f,
            beacon.gameTime.toFloat(),
            0,
            319,
            beacon.color.rgba,
            0.2f * scale,
            0.25f * scale
        )
        popPose()
    }
}

private fun PoseStack.renderBatchedTexts(texts: List<TextData>, bufferSource: MultiBufferSource.BufferSource, camera: Vec3) {
    val cameraPos = -camera
    for (t in texts) {
        pushPose()
        val pose = last().pose()
        val scale = t.scale * 0.025f
        pose.translate(t.pos.toVector3f())
            .translate(cameraPos.x.toFloat(), cameraPos.y.toFloat(), cameraPos.z.toFloat())
            .rotate(t.cameraRotation)
            .scale(scale, -scale, scale)

        t.font.drawInBatch(
            t.text,
            -t.textWidth / 2f,
            0f,
            -1,
            true,
            pose,
            bufferSource,
            if (t.depth) Font.DisplayMode.NORMAL else Font.DisplayMode.SEE_THROUGH,
            0,
            LightTexture.FULL_BRIGHT
        )
        popPose()
    }
}

fun RenderEvent.Extract.drawTracer(to: Vec3, color: Color, depth: Boolean, thickness: Float = 3f) {
    val from = mc.player?.let { p -> p.renderPos.add(p.forward.add(0.0, p.eyeHeight.toDouble(), 0.0)) } ?: return
    drawLine(listOf(from, to), color, depth, thickness)
}

fun RenderEvent.Extract.drawLine(points: Collection<Vec3>, color: Color, depth: Boolean, thickness: Float = 3f) =
    drawLine(points, color, color, depth, thickness)

fun RenderEvent.Extract.drawLine(points: Collection<Vec3>, color1: Color, color2: Color, depth: Boolean, thickness: Float = 3f) {
    if (points.size < 2) return
    val batch = consumer.lines[if (depth) depthOn else depthOff]
    val rgba1 = color1.rgba
    val rgba2 = color2.rgba
    val it = points.iterator()
    var cur = it.next()
    while (it.hasNext()) {
        val next = it.next()
        batch.add(LineData(cur, next, rgba1, rgba2, thickness))
        cur = next
    }
}

fun RenderEvent.Extract.drawWireFrameBox(aabb: AABB, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    consumer.wireBoxes[if (depth) depthOn else depthOff].add(BoxData(aabb, color.redFloat, color.greenFloat, color.blueFloat, color.alphaFloat, thickness))
}

fun RenderEvent.Extract.drawFilledBox(aabb: AABB, color: Color, depth: Boolean = false) {
    consumer.filledBoxes[if (depth) depthOn else depthOff].add(BoxData(aabb, color.redFloat, color.greenFloat, color.blueFloat, color.alphaFloat, 3f))
}

fun RenderEvent.Extract.drawStyledBox(aabb: AABB, color: Color, style: Int = 0, depth: Boolean = true) {
    when (style) {
        0 -> drawFilledBox(aabb, color, depth)
        1 -> drawWireFrameBox(aabb, color, depth = depth)
        2 -> {
            drawFilledBox(aabb, color.multiplyAlpha(0.5f), depth)
            drawWireFrameBox(aabb, color, depth = depth)
        }
    }
}

fun RenderEvent.Extract.drawDiamond(aabb: AABB, color: Color, style: Int = 0, depth: Boolean = false) {
    val midX = (aabb.minX + aabb.maxX) / 2
    val midY = (aabb.minY + aabb.maxY) / 2
    val midZ = (aabb.minZ + aabb.maxZ) / 2

    val topNorth = Vec3(midX, aabb.maxY, aabb.minZ)
    val topEast = Vec3(aabb.maxX, aabb.maxY, midZ)
    val topSouth = Vec3(midX, aabb.maxY, aabb.maxZ)
    val topWest = Vec3(aabb.minX, aabb.maxY, midZ)

    val botNorth = Vec3(midX, aabb.minY, aabb.minZ)
    val botEast = Vec3(aabb.maxX, aabb.minY, midZ)
    val botSouth = Vec3(midX, aabb.minY, aabb.maxZ)
    val botWest = Vec3(aabb.minX, aabb.minY, midZ)

    val rgba = color.rgba

    if (style == 1 || style == 2) {
        val batch = consumer.lines[if (depth) depthOn else depthOff]
        val thick = 3f
        batch.add(LineData(topNorth, topEast, rgba, rgba, thick))
        batch.add(LineData(topEast, topSouth, rgba, rgba, thick))
        batch.add(LineData(topSouth, topWest, rgba, rgba, thick))
        batch.add(LineData(topWest, topNorth, rgba, rgba, thick))
        batch.add(LineData(botNorth, botEast, rgba, rgba, thick))
        batch.add(LineData(botEast, botSouth, rgba, rgba, thick))
        batch.add(LineData(botSouth, botWest, rgba, rgba, thick))
        batch.add(LineData(botWest, botNorth, rgba, rgba, thick))
        batch.add(LineData(topNorth, botNorth, rgba, rgba, thick))
        batch.add(LineData(topEast, botEast, rgba, rgba, thick))
        batch.add(LineData(topSouth, botSouth, rgba, rgba, thick))
        batch.add(LineData(topWest, botWest, rgba, rgba, thick))
    }

    if (style == 0 || style == 2) {
    }
}


private fun Vec3.lerp(to: Vec3, t: Double) = Vec3(x + (to.x - x) * t, y + (to.y - y) * t, z + (to.z - z) * t)

private fun RenderEvent.Extract.addLine(from: Vec3, to: Vec3, color: Color, depth: Boolean, thickness: Float) {
    consumer.lines[if (depth) depthOn else depthOff].add(LineData(from, to, color.rgba, color.rgba, thickness))
}

private fun RenderEvent.Extract.addLine(from: Vec3, to: Vec3, color1: Color, color2: Color, depth: Boolean, thickness: Float) {
    consumer.lines[if (depth) depthOn else depthOff].add(LineData(from, to, color1.rgba, color2.rgba, thickness))
}

private fun AABB.corners(): Array<Vec3> {
    val x0 = minX; val y0 = minY; val z0 = minZ
    val x1 = maxX; val y1 = maxY; val z1 = maxZ
    return arrayOf(
        Vec3(x0, y0, z0), Vec3(x1, y0, z0), Vec3(x1, y1, z0), Vec3(x0, y1, z0),
        Vec3(x0, y0, z1), Vec3(x1, y0, z1), Vec3(x1, y1, z1), Vec3(x0, y1, z1)
    )
}

private fun AABB.edges(): Array<Pair<Vec3, Vec3>> {
    val c = corners()
    fun e(a: Int, b: Int) = c[a] to c[b]
    return arrayOf(
        e(0, 1), e(1, 2), e(2, 3), e(3, 0),
        e(4, 5), e(5, 6), e(6, 7), e(7, 4),
        e(0, 4), e(1, 5), e(2, 6), e(3, 7)
    )
}

fun RenderEvent.Extract.drawCornerBox(aabb: AABB, color: Color, cornerFrac: Float = 0.25f, thickness: Float = 3f, depth: Boolean = false) {
    val c = aabb.corners()
    val dx = ((aabb.maxX - aabb.minX).coerceAtLeast(0.0001)).toFloat() * cornerFrac
    val dy = ((aabb.maxY - aabb.minY).coerceAtLeast(0.0001)).toFloat() * cornerFrac
    val dz = ((aabb.maxZ - aabb.minZ).coerceAtLeast(0.0001)).toFloat() * cornerFrac
    fun corner(p: Vec3, sx: Int, sy: Int, sz: Int) {
        addLine(p, p.add((dx * sx).toDouble(), 0.0, 0.0), color, depth, thickness)
        addLine(p, p.add(0.0, (dy * sy).toDouble(), 0.0), color, depth, thickness)
        addLine(p, p.add(0.0, 0.0, (dz * sz).toDouble()), color, depth, thickness)
    }
    corner(c[0], +1, +1, +1); corner(c[1], -1, +1, +1); corner(c[2], -1, -1, +1); corner(c[3], +1, -1, +1)
    corner(c[4], +1, +1, -1); corner(c[5], -1, +1, -1); corner(c[6], -1, -1, -1); corner(c[7], +1, -1, -1)
}

fun RenderEvent.Extract.drawDashedWireBox(aabb: AABB, color: Color, dashCount: Int = 10, dutyCycle: Float = 0.55f, thickness: Float = 3f, depth: Boolean = false) {
    val edges = aabb.edges()
    val n = dashCount.coerceAtLeast(1)
    val on = dutyCycle.coerceIn(0.05f, 0.95f).toDouble()
    for ((a, b) in edges) for (i in 0 until n) if (i % 2 == 0) {
        val t0 = i.toDouble() / n.toDouble()
        val start = a.lerp(b, t0)
        val end = a.lerp(b, (t0 + (1.0 / n.toDouble()) * on).coerceAtMost(1.0))
        addLine(start, end, color, depth, thickness)
    }
}

fun RenderEvent.Extract.drawGradientWireBox(aabb: AABB, bottom: Color, top: Color, thickness: Float = 3f, depth: Boolean = false) {
    val edges = aabb.edges()
    val midY = (aabb.minY + aabb.maxY) * 0.5
    for ((a, b) in edges) addLine(a, b, if (a.y > midY) top else bottom, if (b.y > midY) top else bottom, depth, thickness)
}

fun RenderEvent.Extract.drawXBox(aabb: AABB, color: Color, thickness: Float = 3f, depth: Boolean = false) {
    drawWireFrameBox(aabb, color, thickness, depth)
    val x0 = aabb.minX; val y0 = aabb.minY; val z0 = aabb.minZ
    val x1 = aabb.maxX; val y1 = aabb.maxY; val z1 = aabb.maxZ
    addLine(Vec3(x0, y1, z0), Vec3(x1, y1, z1), color, depth, thickness)
    addLine(Vec3(x1, y1, z0), Vec3(x0, y1, z1), color, depth, thickness)
    addLine(Vec3(x0, y0, z0), Vec3(x1, y0, z1), color, depth, thickness)
    addLine(Vec3(x1, y0, z0), Vec3(x0, y0, z1), color, depth, thickness)
}

fun RenderEvent.Extract.drawRing(center: Vec3, radius: Float, yOffset: Float = 0f, color: Color, segments: Int = 48, thickness: Float = 3f, depth: Boolean = false) {
    val batch = consumer.lines[if (depth) depthOn else depthOff]
    val rgba = color.rgba
    val seg = segments.coerceAtLeast(8)
    val step = 2.0 * Math.PI / seg.toDouble()
    val y = center.y + yOffset.toDouble()
    var prev = Vec3(center.x + radius.toDouble(), y, center.z)
    for (i in 1..seg) {
        val a = i * step
        val next = Vec3(center.x + (radius * cos(a)).toDouble(), y, center.z + (radius * sin(a)).toDouble())
        batch.add(LineData(prev, next, rgba, rgba, thickness))
        prev = next
    }
}

fun RenderEvent.Extract.draw3RingSphere(center: Vec3, radius: Float, color: Color, segments: Int = 48, thickness: Float = 3f, depth: Boolean = false) {
    val seg = segments.coerceAtLeast(12)
    val step = 2.0 * Math.PI / seg.toDouble()
    val rgba = color.rgba
    val batch = consumer.lines[if (depth) depthOn else depthOff]
    fun loop(pointAt: (Double) -> Vec3) {
        var prev = pointAt(0.0)
        for (i in 1..seg) {
            val a = i * step
            val next = pointAt(a)
            batch.add(LineData(prev, next, rgba, rgba, thickness))
            prev = next
        }
    }
    loop { a -> Vec3(center.x + (radius * cos(a)).toDouble(), center.y, center.z + (radius * sin(a)).toDouble()) }
    loop { a -> Vec3(center.x + (radius * cos(a)).toDouble(), center.y + (radius * sin(a)).toDouble(), center.z) }
    loop { a -> Vec3(center.x, center.y + (radius * sin(a)).toDouble(), center.z + (radius * cos(a)).toDouble()) }
}

fun RenderEvent.Extract.drawPulseBox(aabb: AABB, color: Color, baseAlphaMul: Float = 0.35f, expand: Float = 0.08f, speed: Float = 0.18f, thickness: Float = 3f, depth: Boolean = false) {
    val t = (mc.level?.gameTime ?: 0L).toFloat()
    val s = (0.5f + 0.5f * sin(t * speed)).coerceIn(0f, 1f)
    val pulsed = aabb.inflate((expand * s).toDouble())
    drawWireFrameBox(pulsed, color.multiplyAlpha((baseAlphaMul + (1f - baseAlphaMul) * s).coerceIn(0f, 1f)), thickness, depth)
}

fun RenderEvent.Extract.drawPulseInfillBox(
    aabb: AABB,
    color: Color,
    outerThickness: Float = 3f,
    outerDepth: Boolean = false,
    innerDepth: Boolean = false,
    innerAlphaMin: Float = 0.06f,
    innerAlphaMax: Float = 0.35f,
    speed: Float = 0.18f,
    minSize: Double = 0.02
) {
    drawWireFrameBox(aabb, color, outerThickness, outerDepth)
    val t = (mc.level?.gameTime ?: 0L).toFloat()
    val wave = (0.5f + 0.5f * kotlin.math.sin(t * speed)).coerceIn(0f, 1f)
    val s = (wave * wave)
    val cx = (aabb.minX + aabb.maxX) * 0.5
    val cy = (aabb.minY + aabb.maxY) * 0.5
    val cz = (aabb.minZ + aabb.maxZ) * 0.5
    fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
    val inner = AABB(
        lerp(aabb.minX, cx - minSize * 0.5, s.toDouble()),
        lerp(aabb.minY, cy - minSize * 0.5, s.toDouble()),
        lerp(aabb.minZ, cz - minSize * 0.5, s.toDouble()),
        lerp(aabb.maxX, cx + minSize * 0.5, s.toDouble()),
        lerp(aabb.maxY, cy + minSize * 0.5, s.toDouble()),
        lerp(aabb.maxZ, cz + minSize * 0.5, s.toDouble())
    )
    val alpha = (innerAlphaMin + (innerAlphaMax - innerAlphaMin) * (1f - s)).coerceIn(0f, 1f)
    drawFilledBox(inner, color.multiplyAlpha(alpha), innerDepth)
}

fun RenderEvent.Extract.drawPulseInfillTopFace(
    aabb: AABB,
    color: Color,
    outerThickness: Float = 3f,
    outerDepth: Boolean = false,
    innerDepth: Boolean = false,
    innerAlphaMin: Float = 0.06f,
    innerAlphaMax: Float = 0.35f,
    speed: Float = 0.18f,
    yOffset: Double = 0.002,
    slabThickness: Double = 0.01,
    minSizeXz: Double = 0.02
) {
    val topY = aabb.maxY + yOffset
    val x0 = aabb.minX; val x1 = aabb.maxX; val z0 = aabb.minZ; val z1 = aabb.maxZ
    val p00 = Vec3(x0, topY, z0); val p10 = Vec3(x1, topY, z0); val p11 = Vec3(x1, topY, z1); val p01 = Vec3(x0, topY, z1)
    val batch = consumer.lines[if (outerDepth) depthOn else depthOff]
    val rgba = color.rgba
    batch.add(LineData(p00, p10, rgba, rgba, outerThickness))
    batch.add(LineData(p10, p11, rgba, rgba, outerThickness))
    batch.add(LineData(p11, p01, rgba, rgba, outerThickness))
    batch.add(LineData(p01, p00, rgba, rgba, outerThickness))

    val t = (mc.level?.gameTime ?: 0L).toFloat()
    val wave = (0.5f + 0.5f * kotlin.math.sin(t * speed)).coerceIn(0f, 1f)
    val s = (wave * wave).toDouble()
    fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t
    val cx = (x0 + x1) * 0.5
    val cz = (z0 + z1) * 0.5
    val innerMinX = lerp(x0, cx - minSizeXz * 0.5, s)
    val innerMaxX = lerp(x1, cx + minSizeXz * 0.5, s)
    val innerMinZ = lerp(z0, cz - minSizeXz * 0.5, s)
    val innerMaxZ = lerp(z1, cz + minSizeXz * 0.5, s)
    val alpha = (innerAlphaMin + (innerAlphaMax - innerAlphaMin) * (1f - s.toFloat())).coerceIn(0f, 1f)
    drawFilledBox(AABB(innerMinX, topY, innerMinZ, innerMaxX, topY + slabThickness, innerMaxZ), color.multiplyAlpha(alpha), innerDepth)
}

private fun RenderEvent.Extract.drawHatchedTriangle(a: Vec3, b: Vec3, c: Vec3, color: Color, depth: Boolean, thickness: Float, steps: Int) {
    val n = steps.coerceAtLeast(2)
    for (i in 0..n) {
        val t = i.toDouble() / n.toDouble()
        addLine(a.lerp(c, t), b.lerp(c, t), color, depth, thickness)
    }
}

fun RenderEvent.Extract.drawPulseInfillInvertedPyramid(
    aabb: AABB,
    color: Color,
    outerThickness: Float = 3f,
    outerDepth: Boolean = false,
    topSlabThickness: Double = 0.01,
    topYoffset: Double = 0.002,
    topAlphaMin: Float = 0.06f,
    topAlphaMax: Float = 0.35f,
    topMinSizeXz: Double = 0.02,
    sideDepth: Boolean = false,
    sideLineThickness: Float = 2f,
    sideHatchSteps: Int = 18,
    sideAlphaMin: Float = 0.05f,
    sideAlphaMax: Float = 0.28f,
    speed: Float = 0.18f,
    apexYoffset: Double = 0.001
) {
    val x0 = aabb.minX; val x1 = aabb.maxX; val z0 = aabb.minZ; val z1 = aabb.maxZ
    val topY = aabb.maxY + topYoffset
    val bottomY = aabb.minY + apexYoffset
    val cx = (x0 + x1) * 0.5
    val cz = (z0 + z1) * 0.5
    val p00 = Vec3(x0, topY, z0); val p10 = Vec3(x1, topY, z0); val p11 = Vec3(x1, topY, z1); val p01 = Vec3(x0, topY, z1)
    val apex = Vec3(cx, bottomY, cz)

    val t = (mc.level?.gameTime ?: 0L).toFloat()
    val wave = (0.5f + 0.5f * kotlin.math.sin(t * speed)).coerceIn(0f, 1f)
    val s = (wave * wave).toDouble()
    fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t

    addLine(p00, p10, color, outerDepth, outerThickness); addLine(p10, p11, color, outerDepth, outerThickness)
    addLine(p11, p01, color, outerDepth, outerThickness); addLine(p01, p00, color, outerDepth, outerThickness)
    addLine(p00, apex, color, outerDepth, outerThickness); addLine(p10, apex, color, outerDepth, outerThickness)
    addLine(p11, apex, color, outerDepth, outerThickness); addLine(p01, apex, color, outerDepth, outerThickness)

    val topAlpha = (topAlphaMin + (topAlphaMax - topAlphaMin) * (1f - s.toFloat())).coerceIn(0f, 1f)
    val innerMinX = lerp(x0, cx - topMinSizeXz * 0.5, s)
    val innerMaxX = lerp(x1, cx + topMinSizeXz * 0.5, s)
    val innerMinZ = lerp(z0, cz - topMinSizeXz * 0.5, s)
    val innerMaxZ = lerp(z1, cz + topMinSizeXz * 0.5, s)
    drawFilledBox(AABB(innerMinX, topY, innerMinZ, innerMaxX, topY + topSlabThickness, innerMaxZ), color.multiplyAlpha(topAlpha), outerDepth)

    val sideAlpha = (sideAlphaMin + (sideAlphaMax - sideAlphaMin) * (1f - s.toFloat())).coerceIn(0f, 1f)
    val sideColor = color.multiplyAlpha(sideAlpha)
    fun faceFill(a: Vec3, b: Vec3) = drawHatchedTriangle(a.lerp(apex, s), b.lerp(apex, s), apex, sideColor, sideDepth, sideLineThickness, sideHatchSteps)
    faceFill(p00, p10); faceFill(p10, p11); faceFill(p11, p01); faceFill(p01, p00)
}

private fun Color.opaque(): Color = Color(this.red, this.green, this.blue, 1f)

fun RenderEvent.Extract.drawAnimatedDashedLine(
    from: Vec3,
    to: Vec3,
    color: Color,
    depth: Boolean,
    thickness: Float = 2f,
    dashLength: Double = 0.8,
    gapLength: Double = 0.4,
    animationOffset: Double = 0.0
) {
    val c = color.opaque()
    val direction = to.subtract(from)
    val totalLength = direction.length()
    if (totalLength < 0.01) return
    val normalized = direction.normalize()
    val cycle = dashLength + gapLength
    var current = animationOffset % cycle
    while (current < totalLength) {
        val dashStart = current
        val dashEnd = minOf(current + dashLength, totalLength)
        if (dashEnd > 0 && dashStart < totalLength) {
            val clampedStart = maxOf(dashStart, 0.0)
            val startPoint = from.add(normalized.scale(clampedStart))
            val endPoint = from.add(normalized.scale(dashEnd))
            drawLine(listOf(startPoint, endPoint), color, depth, thickness)
        }
        current += cycle
    }
}

fun RenderEvent.Extract.drawBeaconBeam(position: BlockPos, color: Color) {
    consumer.beaconBeams.add(BeaconData(position, color, mc.player?.isScoping == true, mc.level?.gameTime ?: 0L))
}

fun RenderEvent.Extract.drawText(text: String, pos: Vec3, scale: Float, depth: Boolean) {
    val font = mc.font ?: return
    consumer.texts.add(TextData(text, pos, scale, depth, mc.gameRenderer.mainCamera.rotation(), font, font.width(text).toFloat()))
}

fun RenderEvent.Extract.drawCustomBeacon(title: String, position: BlockPos, color: Color, increase: Boolean = true, distance: Boolean = true) {
    val dist = mc.player?.blockPosition()?.distManhattan(position) ?: return
    drawWireFrameBox(AABB(position), color, depth = false)
    drawBeaconBeam(position, color)
    drawText(if (distance) "$title §r§f(§3${dist}m§f)" else title, position.center.addVec(y = 1.7), if (increase) max(1f, dist * 0.05f) else 2f, false)
}

fun RenderEvent.Extract.drawCylinder(center: Vec3, radius: Float, height: Float, color: Color, segments: Int = 32, thickness: Float = 5f, depth: Boolean = false) {
    val batch = consumer.lines[if (depth) depthOn else depthOff]
    val step = 2.0 * Math.PI / segments
    val rgba = color.rgba
    for (i in 0 until segments) {
        val a1 = i * step
        val a2 = (i + 1) * step
        val x1 = (radius * cos(a1)).toFloat()
        val z1 = (radius * sin(a1)).toFloat()
        val x2 = (radius * cos(a2)).toFloat()
        val z2 = (radius * sin(a2)).toFloat()

        val p1Top = center.add(x1.toDouble(), height.toDouble(), z1.toDouble())
        val p2Top = center.add(x2.toDouble(), height.toDouble(), z2.toDouble())
        val p1Bot = center.add(x1.toDouble(), 0.0, z1.toDouble())
        val p2Bot = center.add(x2.toDouble(), 0.0, z2.toDouble())

        batch.add(LineData(p1Top, p2Top, rgba, rgba, thickness))
        batch.add(LineData(p1Bot, p2Bot, rgba, rgba, thickness))
        batch.add(LineData(p1Bot, p1Top, rgba, rgba, thickness))
    }
}

object PrimitiveRenderer {

    private val edges = intArrayOf(
        0, 1, 1, 5, 5, 4, 4, 0,
        3, 2, 2, 6, 6, 7, 7, 3,
        0, 3, 1, 2, 5, 6, 4, 7
    )

    fun renderLineBox(pose: PoseStack.Pose, buffer: VertexConsumer, aabb: AABB, r: Float, g: Float, b: Float, a: Float) {
        val x0 = aabb.minX.toFloat(); val y0 = aabb.minY.toFloat(); val z0 = aabb.minZ.toFloat()
        val x1 = aabb.maxX.toFloat(); val y1 = aabb.maxY.toFloat(); val z1 = aabb.maxZ.toFloat()

        val corners = floatArrayOf(
            x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0,
            x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1
        )

        for (i in edges.indices step 2) {
            val i0 = edges[i] * 3
            val i1 = edges[i + 1] * 3

            val sx = corners[i0]; val sy = corners[i0 + 1]; val sz = corners[i0 + 2]
            val ex = corners[i1]; val ey = corners[i1 + 1]; val ez = corners[i1 + 2]

            val dx = ex - sx; val dy = ey - sy; val dz = ez - sz
            buffer.addVertex(pose, sx, sy, sz).setColor(r, g, b, a).setNormal(pose, dx, dy, dz)
            buffer.addVertex(pose, ex, ey, ez).setColor(r, g, b, a).setNormal(pose, dx, dy, dz)
        }
    }

    fun addChainedFilledBoxVertices(
        pose: PoseStack.Pose,
        buffer: VertexConsumer,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val matrix = pose.pose()
        fun v(x: Float, y: Float, z: Float) = buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a)

        v(minX, minY, minZ); v(minX, minY, minZ); v(minX, minY, minZ)
        v(minX, minY, maxZ); v(minX, maxY, minZ); v(minX, maxY, maxZ)
        v(minX, maxY, maxZ)
        v(minX, minY, maxZ); v(maxX, maxY, maxZ); v(maxX, minY, maxZ)
        v(maxX, minY, maxZ)
        v(maxX, minY, minZ); v(maxX, maxY, maxZ); v(maxX, maxY, minZ)
        v(maxX, maxY, minZ)
        v(maxX, minY, minZ); v(minX, maxY, minZ); v(minX, minY, minZ)
        v(minX, minY, minZ)
        v(maxX, minY, minZ); v(minX, minY, maxZ); v(maxX, minY, maxZ)
        v(maxX, minY, maxZ)
        v(minX, maxY, minZ); v(minX, maxY, minZ); v(minX, maxY, maxZ); v(maxX, maxY, minZ); v(maxX, maxY, maxZ)
        v(maxX, maxY, maxZ); v(maxX, maxY, maxZ)
    }

    fun renderVector(pose: PoseStack.Pose, buffer: VertexConsumer, start: Vector3f, direction: Vec3, startColor: Int, endColor: Int) {
        val endX = start.x() + direction.x.toFloat()
        val endY = start.y() + direction.y.toFloat()
        val endZ = start.z() + direction.z.toFloat()
        val nx = direction.x.toFloat()
        val ny = direction.y.toFloat()
        val nz = direction.z.toFloat()

        buffer.addVertex(pose, start.x(), start.y(), start.z()).setColor(startColor).setNormal(pose, nx, ny, nz)
        buffer.addVertex(pose, endX, endY, endZ).setColor(endColor).setNormal(pose, nx, ny, nz)
    }
}
