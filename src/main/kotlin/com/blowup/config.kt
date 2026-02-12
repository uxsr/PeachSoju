package com.blowup

import com.blowup.modules.impl.autoroutes.data.WPType
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object config {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val path: Path = FabricLoader.getInstance().configDir.resolve("PeachSoju.json")

    data class NodeColor(var r: Int = 255, var g: Int = 255, var b: Int = 255, var a: Float = 0.35f)

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
        var fmBlocksEnabled: Boolean = false,
        var fmBlocksEditMode: Boolean = false
    )

    @Volatile private var data = Data()

    private val defaultAppearances = mapOf(
        WPType.ETHER to NodeAppearance(NodeColor(85, 255, 255, 0.35f), "PULSE_PYRAMID"),
        WPType.AOTV to NodeAppearance(NodeColor(255, 179, 142, 0.35f), "PULSE_PYRAMID"),
        WPType.HYPE to NodeAppearance(NodeColor(170, 85, 255, 0.35f), "PULSE_PYRAMID"),
        WPType.SUPERBOOM to NodeAppearance(NodeColor(255, 85, 85, 0.35f), "PULSE_PYRAMID"),
        WPType.USEITEM to NodeAppearance(NodeColor(85, 255, 85, 0.35f), "PULSE_PYRAMID"),
        WPType.LOOK to NodeAppearance(NodeColor(255, 255, 85, 0.35f), "PULSE_PYRAMID"),
        WPType.NOP to NodeAppearance(NodeColor(170, 170, 170, 0.35f), "PULSE_PYRAMID")
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
    fun showLines() = data.showLines
    fun setShowLines(v: Boolean) { data.showLines = v; save() }
    fun toggleShowLines() = (!data.showLines).also { data.showLines = it; save() }
    fun getNodeAppearance(type: WPType): NodeAppearance {
        return data.nodeAppearances[type.name] ?: defaultAppearances[type] ?: NodeAppearance()
    }
    fun fmBlocksEnabled() = data.fmBlocksEnabled
    fun setFmBlocksEnabled(v: Boolean) { data.fmBlocksEnabled = v; save() }
    fun toggleFmBlocksEnabled() = (!data.fmBlocksEnabled).also { data.fmBlocksEnabled = it; save() }
    fun fmBlocksEditMode() = data.fmBlocksEditMode
    fun setFmBlocksEditMode(v: Boolean) { data.fmBlocksEditMode = v; save() }
    fun toggleFmBlocksEditMode() = (!data.fmBlocksEditMode).also { data.fmBlocksEditMode = it; save() }

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