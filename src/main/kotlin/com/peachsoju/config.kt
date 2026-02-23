package com.peachsoju

import com.peachsoju.modules.impl.autoroutes.data.WPType
import com.google.gson.GsonBuilder
import com.peachsoju.modules.impl.autoicefill.AutoIceFill
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object config {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val path: Path = FabricLoader.getInstance().configDir.resolve("PeachSoju.json")

    data class NodeColor(var r: Int = 255, var g: Int = 255, var b: Int = 255, var a: Float = 0.60f)

    data class NodeAppearance(
        var color: NodeColor = NodeColor(),
        var style: String = "PULSE_PYRAMID"
    )

    data class Data(
        var enabled: Boolean = false,
        var debug: Boolean = false,
        var extraDebug: Boolean = false,
        var highPing: Boolean = false,
        var autoroutes: Boolean = false,
        var burstMode: Boolean = false,
        var configMode: Boolean = false,
        var waypointRendering: Boolean = true,
        var waypointEditing: Boolean = false,
        var showLines: Boolean = false,
        var nodeAppearances: MutableMap<String, NodeAppearance> = mutableMapOf(),
        var renderOnlyStartNodes: Boolean = false,
        var waypointEsp: Boolean = true,
        var fmBlocksEnabled: Boolean = false,
        var fmBlocksEditMode: Boolean = false,
        var burstModeAotv: Boolean = false,
        var fmBlocksSelectedBlock: String = "minecraft:white_stained_glass",
        var autoSS: Boolean = false,
        var autoSSDelay: Double = 200.0,
        var autoSSForceDevice: Boolean = false,
        var autoSSAutoStartDelay: Double = 125.0,
        var autoSSSmoothRotate: Boolean = false,
        var autoSSRotationTime: Double = 200.0,
        var autoSSDontCheck: Boolean = false,
        var autoIceFill: Boolean = false,
        var autoIceFillShowPath: Boolean = true,
        var autoIceFillOptimize: Boolean = true,
        var autoroutesExpanded: Boolean = true,
        var fmBlocksExpanded: Boolean = true,
        var autoSSExpanded: Boolean = true,
        var autoIceFillExpanded: Boolean = true,
        var stormBowTimer: Boolean = false,
        var stormAutoRelease: Boolean = false,
        var stormReleaseTime: Double = 34.25,
        var stormBowTimerExpanded: Boolean = true,
        var stormLegacyAnimation: Boolean = false,
        var autoAlign: Boolean = false,
        var autoAlignExpanded: Boolean = true,
        var autoAlignForceDevice: Boolean = false,
        var autoAlignDelay: Int = 0,
        var espStartNodesDepthTest: Boolean = false,
        var legacyAnimationsExpanded: Boolean = true
    )

    @Volatile private var data = Data()

    private val defaultAppearances = mapOf(
        WPType.ETHER to NodeAppearance(NodeColor(85, 255, 255, 0.60f), "PULSE_PYRAMID"),
        WPType.AOTV to NodeAppearance(NodeColor(255, 179, 142, 0.60f), "PULSE_PYRAMID"),
        WPType.HYPE to NodeAppearance(NodeColor(170, 85, 255, 0.60f), "PULSE_PYRAMID"),
        WPType.SUPERBOOM to NodeAppearance(NodeColor(255, 85, 85, 0.60f), "PULSE_PYRAMID"),
        WPType.USEITEM to NodeAppearance(NodeColor(85, 255, 85, 0.60f), "PULSE_PYRAMID"),
        WPType.LOOK to NodeAppearance(NodeColor(255, 255, 85, 0.60f), "PULSE_PYRAMID"),
        WPType.NOP to NodeAppearance(NodeColor(170, 170, 170, 0.60f), "PULSE_PYRAMID"),
        WPType.WALK to NodeAppearance(NodeColor(34, 139, 34, 0.8f), "PULSE_PYRAMID"),
        WPType.STOP to NodeAppearance(NodeColor(139, 0, 0, 0.8f), "PULSE_PYRAMID")
    )

    fun load(): Data {
        data = try {
            if (!Files.exists(path)) Data().also { initDefaults(); save(it) }
            else Files.newBufferedReader(path).use { gson.fromJson(it, Data::class.java) ?: Data() }
        } catch (_: Exception) { Data() }

        initDefaults()
        return data
    }

    private fun initDefaults() {
        defaultAppearances.forEach { (type, default) ->
            if (!data.nodeAppearances.containsKey(type.name)) {
                data.nodeAppearances[type.name] = NodeAppearance(
                    NodeColor(default.color.r, default.color.g, default.color.b, default.color.a),
                    default.style
                )
            }
        }
    }

    private fun save(d: Data = data) {
        data = d
        try {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(path).use { gson.toJson(data, it) }
        } catch (_: Exception) {}
    }

    fun enabled() = data.enabled
    fun debug() = data.debug
    fun extraDebug() = data.extraDebug
    fun toggleextraDebug() = (!data.extraDebug).also { data.extraDebug = it; save() }
    fun setEnabled(v: Boolean) { data.enabled = v; save() }
    fun toggleEnabled() = (!data.enabled).also { data.enabled = it; save() }
    fun toggleDebug() = (!data.debug).also { data.debug = it; save() }
    fun autoroutes() = data.autoroutes
    fun setAutoroutes(v: Boolean) { data.autoroutes = v; save() }
    fun toggleAutoroutes() = (!data.autoroutes).also { data.autoroutes = it; save() }
    fun burstMode() = data.burstMode
    fun setBurstMode(v: Boolean) { data.burstMode = v; save() }
    fun toggleBurstMode() = (!data.burstMode).also { data.burstMode = it; save() }
    fun configMode() = data.configMode
    fun setConfigMode(v: Boolean) { data.configMode = v; save() }
    fun toggleConfigMode() = (!data.configMode).also { data.configMode = it; save() }
    fun waypointRendering() = data.waypointRendering
    fun setWaypointRendering(v: Boolean) { data.waypointRendering = v; save() }
    fun toggleWaypointRendering() = (!data.waypointRendering).also { data.waypointRendering = it; save() }
    fun waypointEditing() = data.waypointEditing
    fun setWaypointEditing(v: Boolean) { data.waypointEditing = v; save() }
    fun toggleWaypointEditing() = (!data.waypointEditing).also { data.waypointEditing = it; save() }
    fun renderOnlyStartNodes() = data.renderOnlyStartNodes
    fun setRenderOnlyStartNodes(v: Boolean) { data.renderOnlyStartNodes = v; save() }
    fun toggleRenderOnlyStartNodes() = (!data.renderOnlyStartNodes).also { data.renderOnlyStartNodes = it; save() }
    fun waypointEsp() = data.waypointEsp
    fun setWaypointEsp(v: Boolean) { data.waypointEsp = v; save() }
    fun toggleWaypointEsp() = (!data.waypointEsp).also { data.waypointEsp = it; save() }
    fun espStartNodesDepthTest() = data.espStartNodesDepthTest
    fun setEspStartNodesDepthTest(v: Boolean) { data.espStartNodesDepthTest = v; save() }
    fun toggleEspStartNodesDepthTest() = (!data.espStartNodesDepthTest).also { data.espStartNodesDepthTest = it; save() }
    fun showLines() = data.showLines
    fun setShowLines(v: Boolean) { data.showLines = v; save() }
    fun toggleShowLines() = (!data.showLines).also { data.showLines = it; save() }
    fun getNodeAppearance(type: WPType): NodeAppearance {
        return data.nodeAppearances[type.name] ?: defaultAppearances[type] ?: NodeAppearance()
    }
    fun legacyAnimationsExpanded() = data.legacyAnimationsExpanded
    fun setLegacyAnimationsExpanded(v: Boolean) { data.legacyAnimationsExpanded = v; save() }
    fun fmBlocksEnabled() = data.fmBlocksEnabled
    fun setFmBlocksEnabled(v: Boolean) { data.fmBlocksEnabled = v; save() }
    fun toggleFmBlocksEnabled() = (!data.fmBlocksEnabled).also { data.fmBlocksEnabled = it; save() }
    fun fmBlocksEditMode() = data.fmBlocksEditMode
    fun setFmBlocksEditMode(v: Boolean) { data.fmBlocksEditMode = v; save() }
    fun toggleFmBlocksEditMode() = (!data.fmBlocksEditMode).also { data.fmBlocksEditMode = it; save() }
    fun burstModeAotv() = data.burstModeAotv
    fun setBurstModeAotv(v: Boolean) { data.burstModeAotv = v; save() }
    fun toggleBurstModeAotv() = (!data.burstModeAotv).also { data.burstModeAotv = it; save() }
    fun fmBlocksSelectedBlock() = data.fmBlocksSelectedBlock
    fun setFmBlocksSelectedBlock(blockId: String) { data.fmBlocksSelectedBlock = blockId; save() }

    fun autoSS() = data.autoSS
    fun setAutoSS(v: Boolean) { data.autoSS = v; save() }
    fun toggleAutoSS() = (!data.autoSS).also { data.autoSS = it; save() }

    fun autoSSDelay() = data.autoSSDelay
    fun setAutoSSDelay(v: Double) { data.autoSSDelay = v; save() }

    fun autoSSForceDevice() = data.autoSSForceDevice
    fun setAutoSSForceDevice(v: Boolean) { data.autoSSForceDevice = v; save() }
    fun toggleAutoSSForceDevice() = (!data.autoSSForceDevice).also { data.autoSSForceDevice = it; save() }

    fun autoSSAutoStartDelay() = data.autoSSAutoStartDelay
    fun setAutoSSAutoStartDelay(v: Double) { data.autoSSAutoStartDelay = v; save() }

    fun autoSSSmoothRotate() = data.autoSSSmoothRotate
    fun setAutoSSSmoothRotate(v: Boolean) { data.autoSSSmoothRotate = v; save() }
    fun toggleAutoSSSmoothRotate() = (!data.autoSSSmoothRotate).also { data.autoSSSmoothRotate = it; save() }

    fun autoSSRotationTime() = data.autoSSRotationTime
    fun setAutoSSRotationTime(v: Double) { data.autoSSRotationTime = v; save() }

    fun autoSSDontCheck() = data.autoSSDontCheck
    fun setAutoSSDontCheck(v: Boolean) { data.autoSSDontCheck = v; save() }
    fun toggleAutoSSDontCheck() = (!data.autoSSDontCheck).also { data.autoSSDontCheck = it; save() }

    fun autoIceFillShowPath() = data.autoIceFillShowPath
    fun setAutoIceFillShowPath(v: Boolean) { data.autoIceFillShowPath = v; save() }
    fun toggleAutoIceFillShowPath() = (!data.autoIceFillShowPath).also { data.autoIceFillShowPath = it; save() }

    fun autoIceFillOptimize() = data.autoIceFillOptimize
    fun setAutoIceFillOptimize(v: Boolean) { data.autoIceFillOptimize = v; save() }
    fun toggleAutoIceFillOptimize() = (!data.autoIceFillOptimize).also { data.autoIceFillOptimize = it; save() }

    fun autoroutesExpanded() = data.autoroutesExpanded
    fun setAutoroutesExpanded(v: Boolean) { data.autoroutesExpanded = v; save() }
    fun fmBlocksExpanded() = data.fmBlocksExpanded
    fun setFmBlocksExpanded(v: Boolean) { data.fmBlocksExpanded = v; save() }
    fun autoSSExpanded() = data.autoSSExpanded
    fun setAutoSSExpanded(v: Boolean) { data.autoSSExpanded = v; save() }
    fun autoIceFillExpanded() = data.autoIceFillExpanded
    fun setAutoIceFillExpanded(v: Boolean) { data.autoIceFillExpanded = v; save() }

    fun stormBowTimer() = data.stormBowTimer
    fun setStormBowTimer(v: Boolean) { data.stormBowTimer = v; save() }
    fun toggleStormBowTimer() = (!data.stormBowTimer).also { data.stormBowTimer = it; save() }

    fun stormAutoRelease() = data.stormAutoRelease
    fun setStormAutoRelease(v: Boolean) { data.stormAutoRelease = v; save() }
    fun toggleStormAutoRelease() = (!data.stormAutoRelease).also { data.stormAutoRelease = it; save() }

    fun stormReleaseTime() = data.stormReleaseTime
    fun setStormReleaseTime(v: Double) { data.stormReleaseTime = v; save() }
    fun stormBowTimerExpanded() = data.stormBowTimerExpanded
    fun setStormBowTimerExpanded(v: Boolean) { data.stormBowTimerExpanded = v; save() }

    fun stormLegacyAnimation() = data.stormLegacyAnimation
    fun setStormLegacyAnimation(v: Boolean) { data.stormLegacyAnimation = v; save() }
    fun toggleStormLegacyAnimation() = (!data.stormLegacyAnimation).also { data.stormLegacyAnimation = it; save() }

    fun autoIceFill(): Boolean = data.autoIceFill

    fun toggleAutoIceFill(): Boolean {
        data.autoIceFill = !data.autoIceFill
        save()
        return data.autoIceFill
    }

    fun autoAlign() = data.autoAlign
    fun setAutoAlign(v: Boolean) { data.autoAlign = v; save() }
    fun toggleAutoAlign() = (!data.autoAlign).also { data.autoAlign = it; save() }
    fun autoAlignExpanded() = data.autoAlignExpanded
    fun setAutoAlignExpanded(v: Boolean) { data.autoAlignExpanded = v; save() }
    fun autoAlignForceDevice() = data.autoAlignForceDevice
    fun setAutoAlignForceDevice(v: Boolean) { data.autoAlignForceDevice = v; save() }
    fun toggleAutoAlignForceDevice() = (!data.autoAlignForceDevice).also { data.autoAlignForceDevice = it; save() }
    fun autoAlignDelay() = data.autoAlignDelay
    fun setAutoAlignDelay(v: Int) { data.autoAlignDelay = v; save() }

    fun setAutoIceFill(v: Boolean) {
        data.autoIceFill = v
        save()
    }

    fun setNodeColor(type: WPType, r: Int, g: Int, b: Int, a: Float) {
        val appearance = data.nodeAppearances.getOrPut(type.name) { NodeAppearance() }
        appearance.color = NodeColor(r, g, b, a)
        save()
    }

    fun setNodeStyle(type: WPType, style: String) {
        val appearance = data.nodeAppearances.getOrPut(type.name) { NodeAppearance() }
        appearance.style = style
        save()
    }

    fun resetNodeAppearances() {
        data.nodeAppearances.clear()
        initDefaults()
        save()
    }
}