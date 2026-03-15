package com.peachsoju

import com.google.gson.GsonBuilder
import com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType
import com.peachsoju.sync.PeachSojuSync
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
        var legacyAnimationsExpanded: Boolean = true,
        var hideServerID: Boolean = false,
        var startNodeAppearance: NodeAppearance = NodeAppearance(NodeColor(255, 140, 80, 1.0f), "FILLED"),
        var chatBypass: Boolean = false,
        var chatBypassMode: String = "FONT",
        var autoWeirdos: Boolean = false,
        var autoWeirdosExpanded: Boolean = true,
        var nickHider: Boolean = false,
        var nickHiderNickRaw: String = "PeachSoju",
        var nickHiderColor: String = "§d",
        var nickHiderBold: Boolean = false,
        var nickHiderItalic: Boolean = false,
        var nickHiderUnderline: Boolean = false,
        var nickHiderStrikethrough: Boolean = false,
        var nickHiderExpanded: Boolean = true,
        var nickHiderRankEnabled: Boolean = false,
        var nickHiderRankOrdinal: Int = 0,
        var nickHiderPlusColor: String = "§c",
        var nickHiderMvpPlusPlusBracketColor: String = "§6",
        var nickHiderUseCustomNick: Boolean = false,
        var autoIceFillTickDelay: Int = 1,
        var autoP5Enabled: Boolean = false,
        var autoP5Pathfind: Boolean = true,
        var autoP5LastBreath: Boolean = true,
        var autoP5IceSpray: Boolean = false,
        var autoP5SoulWhip: Boolean = false,
        var autoP5GoMiddle: Boolean = true,
        var autoP5Debug: Boolean = false,
        var autoP5HealerTeam: Int = 0,  // 0 = Solo, 1 = With Archer
        var autoP5IceSprayTick: Int = 82,
        var autoP5SoulWhipTick: Int = 8,
        var autoP5RedDelay: Double = 750.0,
        var autoP5OrangeDelay: Double = 750.0,
        var autoP5GreenDelay: Double = 750.0,
        var autoP5BlueDelay: Double = 750.0,
        var autoP5PurpleDelay: Double = 750.0,
        var autoP5Expanded: Boolean = true
    )

    @Volatile private var data = Data()

    private val defaultAppearances = mapOf(
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.ETHER to NodeAppearance(NodeColor(85, 255, 255, 0.60f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.AOTV to NodeAppearance(NodeColor(255, 179, 142, 0.60f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.HYPE to NodeAppearance(NodeColor(170, 85, 255, 0.60f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.SUPERBOOM to NodeAppearance(NodeColor(255, 85, 85, 0.60f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.USEITEM to NodeAppearance(NodeColor(85, 255, 85, 0.60f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.LOOK to NodeAppearance(NodeColor(255, 255, 85, 0.60f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.NOP to NodeAppearance(NodeColor(170, 170, 170, 0.60f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.WALK to NodeAppearance(NodeColor(34, 139, 34, 0.8f), "PULSE_PYRAMID"),
        _root_ide_package_.com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType.STOP to NodeAppearance(NodeColor(139, 0, 0, 0.8f), "PULSE_PYRAMID"),
        WPType.ALIGN to NodeAppearance(NodeColor(255, 215, 0, 0.8f), "DIAMOND")
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
    fun getNodeAppearance(type: com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType): NodeAppearance {
        return data.nodeAppearances[type.name] ?: defaultAppearances[type] ?: NodeAppearance()
    }
    fun chatBypass(): Boolean = data.chatBypass
    fun toggleChatBypass() = (!data.chatBypass).also { data.chatBypass = it; save() }
    fun chatBypassMode(): String = data.chatBypassMode
    fun setChatBypassMode(mode: String) { data.chatBypassMode = mode; save() }
    fun cycleChatBypassMode() {
        data.chatBypassMode = if (data.chatBypassMode == "FONT") "DOTS" else "FONT"
        save()
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

    fun iceFillTickDelay() = data.autoIceFillTickDelay
    fun setIceFillTickDelay(v: Int) { data.autoIceFillTickDelay = v.coerceIn(1, 10); save() }

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

    fun nickHider() = data.nickHider
    fun setNickHider(v: Boolean) { data.nickHider = v; save(); PeachSojuSync.markDirty() }
    fun toggleNickHider() = (!data.nickHider).also { data.nickHider = it; save(); PeachSojuSync.markDirty() }

    fun nickHiderNickRaw(): String = data.nickHiderNickRaw
    fun setNickHiderNickRaw(nick: String) {
        data.nickHiderNickRaw = nick
        save()
        PeachSojuSync.markDirty()
    }

    fun nickHiderColor(): String = data.nickHiderColor
    fun setNickHiderColor(color: String) {
        data.nickHiderColor = color
        save()
        PeachSojuSync.markDirty()
    }

    fun nickHiderBold() = data.nickHiderBold
    fun setNickHiderBold(v: Boolean) { data.nickHiderBold = v; save(); PeachSojuSync.markDirty() }
    fun toggleNickHiderBold() = (!data.nickHiderBold).also { data.nickHiderBold = it; save(); PeachSojuSync.markDirty() }

    fun nickHiderItalic() = data.nickHiderItalic
    fun setNickHiderItalic(v: Boolean) { data.nickHiderItalic = v; save(); PeachSojuSync.markDirty() }
    fun toggleNickHiderItalic() = (!data.nickHiderItalic).also { data.nickHiderItalic = it; save(); PeachSojuSync.markDirty() }

    fun nickHiderUnderline() = data.nickHiderUnderline
    fun setNickHiderUnderline(v: Boolean) { data.nickHiderUnderline = v; save(); PeachSojuSync.markDirty() }
    fun toggleNickHiderUnderline() = (!data.nickHiderUnderline).also { data.nickHiderUnderline = it; save(); PeachSojuSync.markDirty() }

    fun nickHiderStrikethrough() = data.nickHiderStrikethrough
    fun setNickHiderStrikethrough(v: Boolean) { data.nickHiderStrikethrough = v; save(); PeachSojuSync.markDirty() }
    fun toggleNickHiderStrikethrough() = (!data.nickHiderStrikethrough).also { data.nickHiderStrikethrough = it; save(); PeachSojuSync.markDirty() }

    fun nickHiderRankEnabled() = data.nickHiderRankEnabled
    fun setNickHiderRankEnabled(v: Boolean) { data.nickHiderRankEnabled = v; save(); PeachSojuSync.markDirty() }
    fun toggleNickHiderRankEnabled() = (!data.nickHiderRankEnabled).also { data.nickHiderRankEnabled = it; save(); PeachSojuSync.markDirty() }

    fun nickHiderRankOrdinal() = data.nickHiderRankOrdinal
    fun setNickHiderRankOrdinal(ordinal: Int) { data.nickHiderRankOrdinal = ordinal; save(); PeachSojuSync.markDirty() }

    fun nickHiderPlusColor() = data.nickHiderPlusColor
    fun setNickHiderPlusColor(color: String) { data.nickHiderPlusColor = color; save(); PeachSojuSync.markDirty() }

    fun nickHiderMvpPlusPlusBracketColor() = data.nickHiderMvpPlusPlusBracketColor
    fun setNickHiderMvpPlusPlusBracketColor(color: String) { data.nickHiderMvpPlusPlusBracketColor = color; save(); PeachSojuSync.markDirty() }

    // These don't need markDirty() - they're just UI state:
    fun nickHiderExpanded() = data.nickHiderExpanded
    fun setNickHiderExpanded(v: Boolean) { data.nickHiderExpanded = v; save() }

    fun nickHiderUseCustomNick() = data.nickHiderUseCustomNick
    fun setNickHiderUseCustomNick(v: Boolean) { data.nickHiderUseCustomNick = v; save(); PeachSojuSync.markDirty() }
    fun toggleNickHiderUseCustomNick() = (!data.nickHiderUseCustomNick).also { data.nickHiderUseCustomNick = it; save(); PeachSojuSync.markDirty() }

    fun getStartNodeAppearance(): NodeAppearance = data.startNodeAppearance
    fun setStartNodeColor(r: Int, g: Int, b: Int, a: Float) {
        data.startNodeAppearance.color = NodeColor(r, g, b, a); save()
    }
    fun setStartNodeStyle(style: String) {
        data.startNodeAppearance.style = style; save()
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

    fun autoWeirdos() = data.autoWeirdos
    fun setAutoWeirdos(v: Boolean) { data.autoWeirdos = v; save() }
    fun toggleAutoWeirdos() = (!data.autoWeirdos).also { data.autoWeirdos = it; save() }
    fun autoWeirdosExpanded() = data.autoWeirdosExpanded
    fun setAutoWeirdosExpanded(v: Boolean) { data.autoWeirdosExpanded = v; save() }

    fun hideServerID() = data.hideServerID
    fun setHideServerID(v: Boolean) { data.hideServerID = v; save() }
    fun toggleHideServerID() = (!data.hideServerID).also { data.hideServerID = it; save() }

    fun setAutoIceFill(v: Boolean) {
        data.autoIceFill = v
        save()
    }

    fun setNodeColor(type: com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType, r: Int, g: Int, b: Int, a: Float) {
        val appearance = data.nodeAppearances.getOrPut(type.name) { NodeAppearance() }
        appearance.color = NodeColor(r, g, b, a)
        save()
    }

    fun setNodeStyle(type: com.peachsoju.modules.impl.dungeon.autoroutes.data.WPType, style: String) {
        val appearance = data.nodeAppearances.getOrPut(type.name) { NodeAppearance() }
        appearance.style = style
        save()
    }

    fun resetNodeAppearances() {
        data.nodeAppearances.clear()
        initDefaults()
        save()
    }

    fun autoP5Enabled() = data.autoP5Enabled
    fun setAutoP5Enabled(v: Boolean) { data.autoP5Enabled = v; save() }
    fun toggleAutoP5Enabled() = (!data.autoP5Enabled).also { data.autoP5Enabled = it; save() }

    fun autoP5Pathfind() = data.autoP5Pathfind
    fun setAutoP5Pathfind(v: Boolean) { data.autoP5Pathfind = v; save() }
    fun toggleAutoP5Pathfind() = (!data.autoP5Pathfind).also { data.autoP5Pathfind = it; save() }

    fun autoP5LastBreath() = data.autoP5LastBreath
    fun setAutoP5LastBreath(v: Boolean) { data.autoP5LastBreath = v; save() }
    fun toggleAutoP5LastBreath() = (!data.autoP5LastBreath).also { data.autoP5LastBreath = it; save() }

    fun autoP5IceSpray() = data.autoP5IceSpray
    fun setAutoP5IceSpray(v: Boolean) { data.autoP5IceSpray = v; save() }
    fun toggleAutoP5IceSpray() = (!data.autoP5IceSpray).also { data.autoP5IceSpray = it; save() }

    fun autoP5SoulWhip() = data.autoP5SoulWhip
    fun setAutoP5SoulWhip(v: Boolean) { data.autoP5SoulWhip = v; save() }
    fun toggleAutoP5SoulWhip() = (!data.autoP5SoulWhip).also { data.autoP5SoulWhip = it; save() }

    fun autoP5GoMiddle() = data.autoP5GoMiddle
    fun setAutoP5GoMiddle(v: Boolean) { data.autoP5GoMiddle = v; save() }
    fun toggleAutoP5GoMiddle() = (!data.autoP5GoMiddle).also { data.autoP5GoMiddle = it; save() }

    fun autoP5Debug() = data.autoP5Debug
    fun setAutoP5Debug(v: Boolean) { data.autoP5Debug = v; save() }
    fun toggleAutoP5Debug() = (!data.autoP5Debug).also { data.autoP5Debug = it; save() }

    fun autoP5HealerTeam() = data.autoP5HealerTeam
    fun setAutoP5HealerTeam(v: Int) { data.autoP5HealerTeam = v; save() }

    fun autoP5IceSprayTick() = data.autoP5IceSprayTick
    fun setAutoP5IceSprayTick(v: Int) { data.autoP5IceSprayTick = v.coerceIn(1, 100); save() }

    fun autoP5SoulWhipTick() = data.autoP5SoulWhipTick
    fun setAutoP5SoulWhipTick(v: Int) { data.autoP5SoulWhipTick = v.coerceIn(1, 20); save() }

    fun autoP5RedDelay() = data.autoP5RedDelay
    fun setAutoP5RedDelay(v: Double) { data.autoP5RedDelay = v.coerceIn(0.0, 2000.0); save() }

    fun autoP5OrangeDelay() = data.autoP5OrangeDelay
    fun setAutoP5OrangeDelay(v: Double) { data.autoP5OrangeDelay = v.coerceIn(0.0, 2000.0); save() }

    fun autoP5GreenDelay() = data.autoP5GreenDelay
    fun setAutoP5GreenDelay(v: Double) { data.autoP5GreenDelay = v.coerceIn(0.0, 2000.0); save() }

    fun autoP5BlueDelay() = data.autoP5BlueDelay
    fun setAutoP5BlueDelay(v: Double) { data.autoP5BlueDelay = v.coerceIn(0.0, 2000.0); save() }

    fun autoP5PurpleDelay() = data.autoP5PurpleDelay
    fun setAutoP5PurpleDelay(v: Double) { data.autoP5PurpleDelay = v.coerceIn(0.0, 2000.0); save() }

    fun autoP5Expanded() = data.autoP5Expanded
    fun setAutoP5Expanded(v: Boolean) { data.autoP5Expanded = v; save() }

}