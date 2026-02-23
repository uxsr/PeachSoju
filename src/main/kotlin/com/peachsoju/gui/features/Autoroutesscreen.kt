package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import com.peachsoju.gui.NodeAppearanceScreen
import com.peachsoju.gui.AutoRoutesHelpScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

class AutoRoutesScreen(parent: Screen?) : FeatureScreen(parent, "AutoRoutes") {

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle("Enabled", { config.autoroutes() }, { config.toggleAutoroutes() }, "Main module toggle"))

        add(GuiElement.Spacer)

        add(GuiElement.Toggle("Burst Mode", { config.burstMode() }, { config.toggleBurstMode() }, "Chain etherwarp teleports"))

        if (config.burstMode()) {
            add(GuiElement.Toggle("AOTV Burst", { config.burstModeAotv() }, { config.toggleBurstModeAotv() }, "§c⚠ Ask me how to use first", 1))
        }

        add(GuiElement.Spacer)

        add(GuiElement.Toggle("Config Mode", { config.configMode() }, { config.toggleConfigMode() }, "Trigger any node"))

        add(GuiElement.Spacer)

        add(GuiElement.Toggle("Waypoint Render", { config.waypointRendering() }, { config.toggleWaypointRendering() }, "Show waypoint boxes"))

        if (config.waypointRendering()) {
            add(GuiElement.Toggle("Show Lines", { config.showLines() }, { config.toggleShowLines() }, "Draw burst chain lines", 1))
            add(GuiElement.Toggle("Start Only", { config.renderOnlyStartNodes() }, { config.toggleRenderOnlyStartNodes() }, "Only render start nodes", 1))
            add(GuiElement.Toggle("ESP", { config.waypointEsp() }, { config.toggleWaypointEsp() }, "See nodes through walls", 1))
            add(GuiElement.Toggle("Start Block ESP", { config.espStartNodesDepthTest() }, { config.toggleEspStartNodesDepthTest() }, "Don't show start blocks through walls", 1))
        }

        add(GuiElement.Spacer)

        add(GuiElement.Toggle("Debug Mode", { config.debug() }, { config.toggleDebug() }, "Show debug messages"))

        add(GuiElement.Spacer)

        add(GuiElement.Button("Node Appearance", { openNodeAppearance() }, "Customize node colors"))
        add(GuiElement.Button("Help / Commands", { openHelp() }, "View all commands"))
    }

    private fun openNodeAppearance() {
        Minecraft.getInstance().setScreen(NodeAppearanceScreen(this))
    }

    private fun openHelp() {
        Minecraft.getInstance().setScreen(AutoRoutesHelpScreen(this))
    }
}