package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import com.peachsoju.modules.impl.fmblocks.FMBlocksEditMode
import com.peachsoju.modules.impl.fmblocks.FMBlocksHighlights
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

class FMBlocksScreen(parent: Screen?) : FeatureScreen(parent, "FM Blocks") {

    override fun init() {
        super.init()
        FMBlocksEditMode.initialize()
        FMBlocksHighlights.load()
        updateFilteredBlocks()
    }

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle(
            "Enabled",
            { config.fmBlocksEnabled() },
            { config.toggleFmBlocksEnabled() },
            "Main module toggle"
        ))

        add(GuiElement.Spacer)

        add(GuiElement.Toggle(
            "Edit Mode",
            { config.fmBlocksEditMode() },
            { config.toggleFmBlocksEditMode() },
            "Place/remove custom blocks"
        ))

        add(GuiElement.Spacer)

        add(GuiElement.Label("§7Selected Block:"))
        add(GuiElement.BlockPicker)

        add(GuiElement.Spacer)

        add(GuiElement.Button(
            "Configure Block Highlights",
            { Minecraft.getInstance().setScreen(FMBlocksHighlightsScreen(this@FMBlocksScreen)) },
            "Highlight specific blocks with colors"
        ))

        add(GuiElement.Spacer)

    }
}