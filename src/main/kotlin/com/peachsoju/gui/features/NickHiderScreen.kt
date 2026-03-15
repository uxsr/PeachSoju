package com.peachsoju.gui.features

import com.peachsoju.config
import com.peachsoju.gui.FeatureScreen
import com.peachsoju.modules.impl.misc.nickhider.HypixelRank
import com.peachsoju.modules.impl.misc.nickhider.NickHider
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class NickHiderScreen(parent: Screen?) : FeatureScreen(parent, "✿ Nick Hider ✿") {

    companion object {
        val COLOR_PRESETS = listOf(
            ColorPreset("§0", "Black", 0x000000),
            ColorPreset("§1", "Dark Blue", 0x0000AA),
            ColorPreset("§2", "Dark Green", 0x00AA00),
            ColorPreset("§3", "Dark Aqua", 0x00AAAA),
            ColorPreset("§4", "Dark Red", 0xAA0000),
            ColorPreset("§5", "Dark Purple", 0xAA00AA),
            ColorPreset("§6", "Gold", 0xFFAA00),
            ColorPreset("§7", "Gray", 0xAAAAAA),
            ColorPreset("§8", "Dark Gray", 0x555555),
            ColorPreset("§9", "Blue", 0x5555FF),
            ColorPreset("§a", "Green", 0x55FF55),
            ColorPreset("§b", "Aqua", 0x55FFFF),
            ColorPreset("§c", "Red", 0xFF5555),
            ColorPreset("§d", "Light Purple", 0xFF55FF),
            ColorPreset("§e", "Yellow", 0xFFFF55),
            ColorPreset("§f", "White", 0xFFFFFF)
        )
    }

    data class ColorPreset(val code: String, val name: String, val rgb: Int)

    // Popup states
    private var showColorPicker = false
    private var showRankPicker = false
    private var showPlusColorPicker = false
    private var showBracketColorPicker = false

    private var hoveredColorIndex = -1
    private var hoveredRankIndex = -1
    private var hoveredPlusColorIndex = -1
    private var hoveredBracketColorIndex = -1

    override fun buildElements(): List<GuiElement> = buildList {
        // Main sync toggle
        add(GuiElement.Toggle(
            name = "Sync Enabled",
            getter = { config.nickHider() },
            toggler = { config.toggleNickHider() },
            description = "Sync your config with other PeachSoju users"
        ))

        add(GuiElement.Spacer)
        add(GuiElement.Label(text = "§8══════ Custom Nick ══════"))

        // Custom nick toggle
        add(GuiElement.Toggle(
            name = "Use Custom Nick",
            getter = { config.nickHiderUseCustomNick() },
            toggler = { config.toggleNickHiderUseCustomNick() },
            description = "Replace your name with a custom nick"
        ))

        // Only show nick options if custom nick is enabled
        if (config.nickHiderUseCustomNick()) {
            add(GuiElement.StringField(
                id = "nick_field",
                name = "Nick Text",
                getter = { config.nickHiderNickRaw() },
                setter = { config.setNickHiderNickRaw(it) },
                maxLength = 24,
                description = "The base name (without formatting)"
            ))

            add(GuiElement.Spacer)
            add(GuiElement.Label(text = "§8══════ Hypixel Rank ══════"))

            add(GuiElement.Toggle(
                name = "Custom Rank",
                getter = { config.nickHiderRankEnabled() },
                toggler = { config.toggleNickHiderRankEnabled() },
                description = "Enable custom rank prefix"
            ))

            add(GuiElement.Button(
                name = "Select Rank: ${NickHider.getCurrentRank().displayName}",
                action = { showRankPicker = !showRankPicker },
                description = "Choose your Hypixel rank"
            ))

            // Show plus color option only for ranks with plus
            val currentRank = NickHider.getCurrentRank()
            if (currentRank.hasPlusSymbol) {
                add(GuiElement.Button(
                    name = "Plus Color: ${getPlusColorName(config.nickHiderPlusColor())}",
                    action = { showPlusColorPicker = !showPlusColorPicker },
                    description = "Choose the color of your + symbol"
                ))
            }

            // Show bracket color option only for MVP++
            if (currentRank == HypixelRank.MVP_PLUS_PLUS) {
                add(GuiElement.Button(
                    name = "Bracket Color: ${getBracketColorName(config.nickHiderMvpPlusPlusBracketColor())}",
                    action = { showBracketColorPicker = !showBracketColorPicker },
                    description = "Choose the color of [MVP] and your name"
                ))
            }

            add(GuiElement.Spacer)
            add(GuiElement.Label(text = "§8══════ Name Formatting ══════"))

            add(GuiElement.Button(
                name = "Name Color",
                action = { showColorPicker = !showColorPicker },
                description = "Choose nick color"
            ))

            add(GuiElement.Toggle(
                name = "Bold",
                getter = { config.nickHiderBold() },
                toggler = { config.toggleNickHiderBold() },
                description = "Make nick §lbold"
            ))

            add(GuiElement.Toggle(
                name = "Italic",
                getter = { config.nickHiderItalic() },
                toggler = { config.toggleNickHiderItalic() },
                description = "Make nick §oitalic"
            ))

            add(GuiElement.Toggle(
                name = "Underline",
                getter = { config.nickHiderUnderline() },
                toggler = { config.toggleNickHiderUnderline() },
                description = "Make nick §nunderlined"
            ))

            add(GuiElement.Toggle(
                name = "Strikethrough",
                getter = { config.nickHiderStrikethrough() },
                toggler = { config.toggleNickHiderStrikethrough() },
                description = "Make nick §mstrikethrough"
            ))
        }

        add(GuiElement.Spacer)
        add(GuiElement.Label(text = "§8══════ Preview ══════"))

        add(GuiElement.Label(
            text = "Preview: ${NickHider.getNickToDisplay()}",
            color = TEXT_DARK
        ))

        add(GuiElement.Spacer)

        add(GuiElement.Button(
            name = "Reset to Default",
            action = { resetToDefaults() },
            description = "Reset all nick settings"
        ))
    }

    private fun getPlusColorName(code: String): String {
        return HypixelRank.PLUS_COLORS.find { it.code == code }?.name ?: "Red"
    }

    private fun getBracketColorName(code: String): String {
        return HypixelRank.MVP_PLUS_PLUS_BRACKET_COLORS.find { it.code == code }?.name ?: "Gold"
    }

    private fun resetToDefaults() {
        config.setNickHiderNickRaw("PeachSoju")
        config.setNickHiderColor("§d")
        config.setNickHiderBold(false)
        config.setNickHiderItalic(false)
        config.setNickHiderUnderline(false)
        config.setNickHiderStrikethrough(false)
        config.setNickHiderRankEnabled(false)
        config.setNickHiderRankOrdinal(HypixelRank.NONE.ordinal)
        config.setNickHiderPlusColor("§c")
        config.setNickHiderMvpPlusPlusBracketColor("§6")
        config.setNickHiderUseCustomNick(false)
        stringFieldValues["nick_field"] = "PeachSoju"
        playClickSound()
    }

    override fun initTextFields() {
        stringFieldValues["nick_field"] = config.nickHiderNickRaw()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // If any popup is open, don't pass mouse coords to parent (prevents hover effects underneath)
        val hasPopup = showColorPicker || showRankPicker || showPlusColorPicker || showBracketColorPicker
        if (hasPopup) {
            super.render(graphics, -1000, -1000, partialTick)
        } else {
            super.render(graphics, mouseX, mouseY, partialTick)
        }

        // Draw popups on top
        when {
            showColorPicker -> drawColorPicker(graphics, mouseX, mouseY)
            showRankPicker -> drawRankPicker(graphics, mouseX, mouseY)
            showPlusColorPicker -> drawPlusColorPicker(graphics, mouseX, mouseY)
            showBracketColorPicker -> drawBracketColorPicker(graphics, mouseX, mouseY)
        }
    }

    // ===================== COLOR PICKER =====================

    private fun drawColorPicker(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pickerWidth = 180
        val pickerHeight = 140
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 80

        drawPopupBackground(graphics, pickerX, pickerY, pickerWidth, pickerHeight)
        drawPopupTitle(graphics, pickerX, pickerWidth, pickerY, "§lSelect Color")
        drawCloseButton(graphics, pickerX, pickerY, pickerWidth, mouseX, mouseY)

        val gridStartX = pickerX + 10
        val gridStartY = pickerY + 20
        val colorSize = 20
        val spacing = 2
        val colorsPerRow = 4

        hoveredColorIndex = -1

        for ((index, preset) in COLOR_PRESETS.withIndex()) {
            val row = index / colorsPerRow
            val col = index % colorsPerRow

            val colorX = gridStartX + col * (colorSize + spacing)
            val colorY = gridStartY + row * (colorSize + spacing)

            val isHovered = mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)
            if (isHovered) hoveredColorIndex = index

            val isSelected = config.nickHiderColor() == preset.code
            drawColorSwatch(graphics, colorX, colorY, colorSize, preset.rgb, isSelected, isHovered)
        }

        // Preview
        val previewY = pickerY + pickerHeight - 25
        graphics.drawString(font, "Current:", pickerX + 10, previewY + 3, TEXT_DARK, false)
        val previewText = NickHider.getNickToDisplay()
        graphics.drawString(font, previewText, pickerX + 55, previewY + 3, 0xFFFFFFFF.toInt(), false)

        // Tooltip
        if (hoveredColorIndex in COLOR_PRESETS.indices) {
            drawTooltip(graphics, mouseX, mouseY, COLOR_PRESETS[hoveredColorIndex].name)
        }
    }

    // ===================== RANK PICKER =====================

    private fun drawRankPicker(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pickerWidth = 200
        val pickerHeight = 220
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 60

        drawPopupBackground(graphics, pickerX, pickerY, pickerWidth, pickerHeight)
        drawPopupTitle(graphics, pickerX, pickerWidth, pickerY, "§lSelect Rank")
        drawCloseButton(graphics, pickerX, pickerY, pickerWidth, mouseX, mouseY)

        val listStartY = pickerY + 22
        val itemHeight = 16
        val currentRank = NickHider.getCurrentRank()

        hoveredRankIndex = -1

        for ((index, rank) in HypixelRank.entries.withIndex()) {
            val itemY = listStartY + index * itemHeight

            if (itemY + itemHeight > pickerY + pickerHeight - 10) break

            val isHovered = mouseX in (pickerX + 5)..(pickerX + pickerWidth - 5) && mouseY in itemY..(itemY + itemHeight)
            if (isHovered) hoveredRankIndex = index

            val isSelected = rank == currentRank

            // Background
            if (isSelected) {
                graphics.fill(pickerX + 5, itemY, pickerX + pickerWidth - 5, itemY + itemHeight, 0x44FFFFFF)
            } else if (isHovered) {
                graphics.fill(pickerX + 5, itemY, pickerX + pickerWidth - 5, itemY + itemHeight, 0x22FFFFFF)
            }

            // Rank preview
            val rankPreview = if (rank == HypixelRank.NONE) {
                "§7(No Rank)"
            } else {
                rank.buildPrefix() + "§7Player"
            }
            graphics.drawString(font, rankPreview, pickerX + 10, itemY + 4, 0xFFFFFFFF.toInt(), false)

            // Selection indicator
            if (isSelected) {
                graphics.drawString(font, "§a✓", pickerX + pickerWidth - 18, itemY + 4, 0xFFFFFFFF.toInt(), false)
            }
        }
    }

    // ===================== PLUS COLOR PICKER =====================

    private fun drawPlusColorPicker(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pickerWidth = 180
        val pickerHeight = 160
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 80

        drawPopupBackground(graphics, pickerX, pickerY, pickerWidth, pickerHeight)
        drawPopupTitle(graphics, pickerX, pickerWidth, pickerY, "§l+ Color")
        drawCloseButton(graphics, pickerX, pickerY, pickerWidth, mouseX, mouseY)

        val gridStartX = pickerX + 10
        val gridStartY = pickerY + 20
        val colorSize = 20
        val spacing = 2
        val colorsPerRow = 4

        hoveredPlusColorIndex = -1

        for ((index, plusColor) in HypixelRank.PLUS_COLORS.withIndex()) {
            val row = index / colorsPerRow
            val col = index % colorsPerRow

            val colorX = gridStartX + col * (colorSize + spacing)
            val colorY = gridStartY + row * (colorSize + spacing)

            val isHovered = mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)
            if (isHovered) hoveredPlusColorIndex = index

            val isSelected = config.nickHiderPlusColor() == plusColor.code
            drawColorSwatch(graphics, colorX, colorY, colorSize, plusColor.rgb, isSelected, isHovered)
        }

        // Preview
        val previewY = pickerY + pickerHeight - 25
        graphics.drawString(font, "Preview:", pickerX + 10, previewY + 3, TEXT_DARK, false)
        val previewText = NickHider.getNickToDisplay()
        graphics.drawString(font, previewText, pickerX + 55, previewY + 3, 0xFFFFFFFF.toInt(), false)

        // Tooltip
        if (hoveredPlusColorIndex in HypixelRank.PLUS_COLORS.indices) {
            drawTooltip(graphics, mouseX, mouseY, HypixelRank.PLUS_COLORS[hoveredPlusColorIndex].name)
        }
    }

    // ===================== BRACKET COLOR PICKER (MVP++ only) =====================

    private fun drawBracketColorPicker(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val pickerWidth = 160
        val pickerHeight = 100
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 100

        drawPopupBackground(graphics, pickerX, pickerY, pickerWidth, pickerHeight)
        drawPopupTitle(graphics, pickerX, pickerWidth, pickerY, "§lBracket Color")
        drawCloseButton(graphics, pickerX, pickerY, pickerWidth, mouseX, mouseY)

        val gridStartX = pickerX + (pickerWidth - 2 * 30) / 2
        val gridStartY = pickerY + 30
        val colorSize = 25
        val spacing = 10

        hoveredBracketColorIndex = -1

        for ((index, bracketColor) in HypixelRank.MVP_PLUS_PLUS_BRACKET_COLORS.withIndex()) {
            val colorX = gridStartX + index * (colorSize + spacing)
            val colorY = gridStartY

            val isHovered = mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)
            if (isHovered) hoveredBracketColorIndex = index

            val isSelected = config.nickHiderMvpPlusPlusBracketColor() == bracketColor.code
            drawColorSwatch(graphics, colorX, colorY, colorSize, bracketColor.rgb, isSelected, isHovered)
        }

        // Preview
        val previewY = pickerY + pickerHeight - 25
        val previewText = NickHider.getNickToDisplay()
        val previewX = pickerX + (pickerWidth - font.width(previewText.replace("§.", ""))) / 2
        graphics.drawString(font, previewText, previewX, previewY + 3, 0xFFFFFFFF.toInt(), false)

        // Tooltip
        if (hoveredBracketColorIndex in HypixelRank.MVP_PLUS_PLUS_BRACKET_COLORS.indices) {
            drawTooltip(graphics, mouseX, mouseY, HypixelRank.MVP_PLUS_PLUS_BRACKET_COLORS[hoveredBracketColorIndex].name)
        }
    }

    // ===================== HELPER METHODS =====================

    private fun drawPopupBackground(graphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        // Dim background
        graphics.fill(0, 0, width, height, 0x88000000.toInt())

        // Popup frame
        graphics.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0xFF000000.toInt())
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, PEACH_DARK)
        graphics.fill(x, y, x + w, y + h, PEACH_CREAM)
    }

    private fun drawPopupTitle(graphics: GuiGraphics, x: Int, w: Int, y: Int, title: String) {
        graphics.drawString(font, title, x + (w - font.width(title)) / 2, y + 5, TEXT_DARK, false)
    }

    private fun drawCloseButton(graphics: GuiGraphics, pickerX: Int, pickerY: Int, pickerWidth: Int, mouseX: Int, mouseY: Int) {
        val closeX = pickerX + pickerWidth - 25
        val closeY = pickerY + 5
        val closeHover = mouseX in closeX..(closeX + 20) && mouseY in closeY..(closeY + 12)
        val closeColor = if (closeHover) 0xFFFF5555.toInt() else PEACH_MEDIUM
        graphics.fill(closeX, closeY, closeX + 20, closeY + 12, closeColor)
        graphics.drawString(font, "✕", closeX + 6, closeY + 2, TEXT_LIGHT, false)
    }

    private fun drawColorSwatch(graphics: GuiGraphics, x: Int, y: Int, size: Int, rgb: Int, selected: Boolean, hovered: Boolean) {
        if (selected) {
            graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0xFFFFFFFF.toInt())
        }
        graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000.toInt())
        graphics.fill(x, y, x + size, y + size, (0xFF shl 24) or rgb)
        if (hovered) {
            graphics.fill(x, y, x + size, y + size, 0x44FFFFFF)
        }
    }

    private fun drawTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int, text: String) {
        val tooltipX = mouseX + 12
        val tooltipY = mouseY - 4
        val tooltipWidth = font.width(text) + 8
        val tooltipHeight = 14

        graphics.fill(tooltipX - 1, tooltipY - 1, tooltipX + tooltipWidth + 1, tooltipY + tooltipHeight + 1, 0xFF000000.toInt())
        graphics.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFF1a1a2e.toInt())
        graphics.drawString(font, text, tooltipX + 4, tooltipY + 3, 0xFFFFFFFF.toInt(), false)
    }

    // ===================== MOUSE HANDLING =====================

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()

        if (event.button() != 0) {
            return super.mouseClicked(event, bl)
        }

        // Handle each popup
        when {
            showColorPicker -> return handleColorPickerClick(mouseX, mouseY)
            showRankPicker -> return handleRankPickerClick(mouseX, mouseY)
            showPlusColorPicker -> return handlePlusColorPickerClick(mouseX, mouseY)
            showBracketColorPicker -> return handleBracketColorPickerClick(mouseX, mouseY)
        }

        return super.mouseClicked(event, bl)
    }

    private fun handleColorPickerClick(mouseX: Int, mouseY: Int): Boolean {
        val pickerWidth = 180
        val pickerHeight = 140
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 80

        // Close button
        val closeX = pickerX + pickerWidth - 25
        val closeY = pickerY + 5
        if (mouseX in closeX..(closeX + 20) && mouseY in closeY..(closeY + 12)) {
            showColorPicker = false
            playClickSound()
            return true
        }

        // Color grid
        val gridStartX = pickerX + 10
        val gridStartY = pickerY + 20
        val colorSize = 20
        val spacing = 2
        val colorsPerRow = 4

        for ((index, preset) in COLOR_PRESETS.withIndex()) {
            val row = index / colorsPerRow
            val col = index % colorsPerRow
            val colorX = gridStartX + col * (colorSize + spacing)
            val colorY = gridStartY + row * (colorSize + spacing)

            if (mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)) {
                config.setNickHiderColor(preset.code)
                playClickSound()
                return true
            }
        }

        // Click outside to close
        if (mouseX !in pickerX..(pickerX + pickerWidth) || mouseY !in pickerY..(pickerY + pickerHeight)) {
            showColorPicker = false
            return true
        }

        return true
    }

    private fun handleRankPickerClick(mouseX: Int, mouseY: Int): Boolean {
        val pickerWidth = 200
        val pickerHeight = 220
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 60

        // Close button
        val closeX = pickerX + pickerWidth - 25
        val closeY = pickerY + 5
        if (mouseX in closeX..(closeX + 20) && mouseY in closeY..(closeY + 12)) {
            showRankPicker = false
            playClickSound()
            return true
        }

        // Rank list
        val listStartY = pickerY + 22
        val itemHeight = 16

        for ((index, rank) in HypixelRank.entries.withIndex()) {
            val itemY = listStartY + index * itemHeight
            if (itemY + itemHeight > pickerY + pickerHeight - 10) break

            if (mouseX in (pickerX + 5)..(pickerX + pickerWidth - 5) && mouseY in itemY..(itemY + itemHeight)) {
                NickHider.setCurrentRank(rank)
                showRankPicker = false
                playClickSound()
                return true
            }
        }

        // Click outside to close
        if (mouseX !in pickerX..(pickerX + pickerWidth) || mouseY !in pickerY..(pickerY + pickerHeight)) {
            showRankPicker = false
            return true
        }

        return true
    }

    private fun handlePlusColorPickerClick(mouseX: Int, mouseY: Int): Boolean {
        val pickerWidth = 180
        val pickerHeight = 160
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 80

        // Close button
        val closeX = pickerX + pickerWidth - 25
        val closeY = pickerY + 5
        if (mouseX in closeX..(closeX + 20) && mouseY in closeY..(closeY + 12)) {
            showPlusColorPicker = false
            playClickSound()
            return true
        }

        // Color grid
        val gridStartX = pickerX + 10
        val gridStartY = pickerY + 20
        val colorSize = 20
        val spacing = 2
        val colorsPerRow = 4

        for ((index, plusColor) in HypixelRank.PLUS_COLORS.withIndex()) {
            val row = index / colorsPerRow
            val col = index % colorsPerRow
            val colorX = gridStartX + col * (colorSize + spacing)
            val colorY = gridStartY + row * (colorSize + spacing)

            if (mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)) {
                config.setNickHiderPlusColor(plusColor.code)
                playClickSound()
                return true
            }
        }

        // Click outside to close
        if (mouseX !in pickerX..(pickerX + pickerWidth) || mouseY !in pickerY..(pickerY + pickerHeight)) {
            showPlusColorPicker = false
            return true
        }

        return true
    }

    private fun handleBracketColorPickerClick(mouseX: Int, mouseY: Int): Boolean {
        val pickerWidth = 160
        val pickerHeight = 100
        val pickerX = guiLeft + (GUI_WIDTH - pickerWidth) / 2
        val pickerY = guiTop + 100

        // Close button
        val closeX = pickerX + pickerWidth - 25
        val closeY = pickerY + 5
        if (mouseX in closeX..(closeX + 20) && mouseY in closeY..(closeY + 12)) {
            showBracketColorPicker = false
            playClickSound()
            return true
        }

        // Color buttons
        val gridStartX = pickerX + (pickerWidth - 2 * 30) / 2
        val gridStartY = pickerY + 30
        val colorSize = 25
        val spacing = 10

        for ((index, bracketColor) in HypixelRank.MVP_PLUS_PLUS_BRACKET_COLORS.withIndex()) {
            val colorX = gridStartX + index * (colorSize + spacing)
            val colorY = gridStartY

            if (mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)) {
                config.setNickHiderMvpPlusPlusBracketColor(bracketColor.code)
                playClickSound()
                return true
            }
        }

        // Click outside to close
        if (mouseX !in pickerX..(pickerX + pickerWidth) || mouseY !in pickerY..(pickerY + pickerHeight)) {
            showBracketColorPicker = false
            return true
        }

        return true
    }

    override fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (event.key() == 256) { // ESC
            when {
                showColorPicker -> { showColorPicker = false; return true }
                showRankPicker -> { showRankPicker = false; return true }
                showPlusColorPicker -> { showPlusColorPicker = false; return true }
                showBracketColorPicker -> { showBracketColorPicker = false; return true }
            }
        }
        return super.keyPressed(event)
    }
}