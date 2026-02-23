package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import com.peachsoju.modules.impl.fmblocks.FMBlocksEditMode
import net.minecraft.client.gui.screens.Screen

class FMBlocksScreen(parent: Screen?) : FeatureScreen(parent, "FM Blocks") {

    override fun init() {
        super.init()
        FMBlocksEditMode.initialize()
        updateFilteredBlocks()
    }

    override fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.Toggle("Enabled", { config.fmBlocksEnabled() }, { config.toggleFmBlocksEnabled() }, "Main module toggle"))

        add(GuiElement.Spacer)

        add(GuiElement.Toggle("Edit Mode", { config.fmBlocksEditMode() }, { config.toggleFmBlocksEditMode() }, "Edit FM Blocks"))

        add(GuiElement.Spacer)

        add(GuiElement.BlockPicker)
    }
}