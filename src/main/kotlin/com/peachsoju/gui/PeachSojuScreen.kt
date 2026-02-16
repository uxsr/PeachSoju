package com.peachsoju.gui

import com.peachsoju.config
import com.peachsoju.modules.impl.fmblocks.FMBlocksEditMode
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import kotlin.math.max
import kotlin.math.min

class PeachSojuScreen : Screen(Component.literal("PeachSoju")) {

    companion object {
        private const val PEACH_DARK = 0xFF8B5A3C.toInt()
        private const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        private const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        private const val PEACH_CREAM = 0xFFFFF5E6.toInt()

        private const val TEXT_DARK = 0xFF4A3728.toInt()
        private const val TEXT_LIGHT = 0xFFFFFFFF.toInt()
        private const val TEXT_GRAY = 0xFF888888.toInt()

        private const val TOGGLE_ON = 0xFF7CB342.toInt()
        private const val TOGGLE_OFF = 0xFFB85C5C.toInt()
        private const val SECTION_HEADER_ON = 0xFF5A8F3E.toInt()
        private const val SECTION_HEADER_OFF = 0xFFB85C5C.toInt()

        private const val GUI_WIDTH = 300
        private const val GUI_HEIGHT = 320
        private const val PADDING = 10
        private const val ROW_HEIGHT = 22
        private const val TOGGLE_WIDTH = 40
        private const val TOGGLE_HEIGHT = 16
        private const val HEADER_HEIGHT = 30
        private const val FOOTER_HEIGHT = 26
        private const val SCROLL_STEP = 14
    }

    private var guiLeft = 0
    private var guiTop = 0
    private var scrollY = 0
    private var maxScroll = 0

    private var autoroutesExpanded = true
    private var fmBlocksExpanded = true
    private var autoSSExpanded = true

    private var blockSearchText = ""
    private var blockSearchActive = false
    private var filteredBlocks: List<Block> = emptyList()
    private var blockDropdownOpen = false
    private var blockDropdownScroll = 0

    private var tooltipToDraw: Triple<Int, Int, String>? = null

    private sealed class GuiElement {
        data class SectionHeader(val name: String, val expandedGetter: () -> Boolean, val toggle: () -> Unit, val enabledGetter: () -> Boolean) : GuiElement()
        data class Toggle(val name: String, val getter: () -> Boolean, val toggler: () -> Boolean, val description: String = "", val indent: Int = 0) : GuiElement()
        data class Button(val name: String, val action: () -> Unit, val description: String = "") : GuiElement()
        data class Slider(val name: String, val getter: () -> Double, val setter: (Double) -> Unit, val min: Double, val max: Double, val suffix: String = "", val indent: Int = 0) : GuiElement()
        object BlockPicker : GuiElement()
        object Spacer : GuiElement()
    }

    private fun buildElements(): List<GuiElement> = buildList {
        add(GuiElement.SectionHeader("AutoRoutes", { autoroutesExpanded }, { autoroutesExpanded = !autoroutesExpanded }, { config.autoroutes() }))

        if (autoroutesExpanded) {
            add(GuiElement.Toggle("Enabled", { config.autoroutes() }, { config.toggleAutoroutes() }, "Main module toggle", 1))

            if (config.autoroutes()) {
                add(GuiElement.Toggle("Burst Mode", { config.burstMode() }, { config.toggleBurstMode() }, "Chain etherwarp teleports", 2))

                if (config.burstMode()) {
                    add(GuiElement.Toggle("AOTV Burst", { config.burstModeAotv() }, { config.toggleBurstModeAotv() }, "§c⚠ Ask me how to use first", 3))
                }

                add(GuiElement.Toggle("Config Mode", { config.configMode() }, { config.toggleConfigMode() }, "Trigger any node", 2))
                add(GuiElement.Toggle("Waypoint Render", { config.waypointRendering() }, { config.toggleWaypointRendering() }, "Show waypoint boxes", 2))

                if (config.waypointRendering()) {
                    add(GuiElement.Toggle("Show Lines", { config.showLines() }, { config.toggleShowLines() }, "Draw burst chain lines", 3))
                    add(GuiElement.Toggle("Start Only", { config.renderOnlyStartNodes() }, { config.toggleRenderOnlyStartNodes() }, "Only render start nodes", 3))
                }

                add(GuiElement.Toggle("Debug Mode", { config.debug() }, { config.toggleDebug() }, "Show debug messages", 2))
                add(GuiElement.Button("Node Appearance", { openNodeAppearance() }, "Customize node colors"))
                add(GuiElement.Button("Help / Commands", { openHelp() }, "View all commands"))
            }
        }

        add(GuiElement.Spacer)

        add(GuiElement.SectionHeader("FM Blocks", { fmBlocksExpanded }, { fmBlocksExpanded = !fmBlocksExpanded }, { config.fmBlocksEnabled() }))

        if (fmBlocksExpanded) {
            add(GuiElement.Toggle("Enabled", { config.fmBlocksEnabled() }, { config.toggleFmBlocksEnabled() }, "Main module toggle", 1))

            if (config.fmBlocksEnabled()) {
                add(GuiElement.Toggle("Edit Mode", { config.fmBlocksEditMode() }, { config.toggleFmBlocksEditMode() }, "Edit FM Blocks", 2))
                add(GuiElement.BlockPicker)
            }
        }

        add(GuiElement.Spacer)

        add(GuiElement.SectionHeader("AutoSS", { autoSSExpanded }, { autoSSExpanded = !autoSSExpanded }, { config.autoSS() }))

        if (autoSSExpanded) {
            add(GuiElement.Toggle("Enabled", { config.autoSS() }, { config.toggleAutoSS() }, "Auto Simon Says solver", 1))

            if (config.autoSS()) {
                add(GuiElement.Slider("Click Delay", { config.autoSSDelay() }, { config.setAutoSSDelay(it) }, 50.0, 200.0, "ms", 2))
                add(GuiElement.Slider("Start Delay", { config.autoSSAutoStartDelay() }, { config.setAutoSSAutoStartDelay(it) }, 50.0, 200.0, "ms", 2))
                add(GuiElement.Toggle("Force Device", { config.autoSSForceDevice() }, { config.toggleAutoSSForceDevice() }, "Bypass device detection", 2))

            }
        }
    }

    override fun init() {
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2
        FMBlocksEditMode.initialize()
        updateFilteredBlocks()
        recomputeMaxScroll()
    }

    private fun recomputeMaxScroll() {
        val elements = buildElements()
        var contentHeight = 0
        for (element in elements) {
            contentHeight += when (element) {
                is GuiElement.SectionHeader -> ROW_HEIGHT + 4
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.BlockPicker -> ROW_HEIGHT + 24
                is GuiElement.Spacer -> 10
            }
        }
        val viewHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2)
        maxScroll = max(0, contentHeight - viewHeight)
        scrollY = scrollY.coerceIn(0, max(0, maxScroll))
    }

    private fun updateFilteredBlocks() {
        val searchLower = blockSearchText.lowercase()
        filteredBlocks = if (searchLower.isEmpty()) {
            BuiltInRegistries.BLOCK.sortedBy {
                BuiltInRegistries.BLOCK.getKey(it).toString()
            }.take(100)
        } else {
            val searchWords = searchLower.split(" ").filter { it.isNotEmpty() }

            BuiltInRegistries.BLOCK.filter { block ->
                val key = BuiltInRegistries.BLOCK.getKey(block).toString().lowercase()
                val cleanKey = key.removePrefix("minecraft:").replace("_", " ")

                searchWords.all { word -> cleanKey.contains(word) }
            }.sortedBy {
                BuiltInRegistries.BLOCK.getKey(it).toString()
            }.take(50)
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        recomputeMaxScroll()

        graphics.fill(0, 0, width, height, 0xAA000000.toInt())
        drawPanel(graphics)
        drawHeader(graphics)
        drawContent(graphics, mouseX, mouseY)
        drawFooter(graphics)

        if (blockDropdownOpen) {
            drawBlockDropdown(graphics, mouseX, mouseY)
        }

        tooltipToDraw?.let { (tx, ty, text) ->
            drawTooltip(graphics, tx, ty, text)
        }

        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawPanel(graphics: GuiGraphics) {
        graphics.fill(guiLeft - 2, guiTop - 2, guiLeft + GUI_WIDTH + 2, guiTop + GUI_HEIGHT + 2, PEACH_DARK)
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, PEACH_LIGHT)
        graphics.fill(guiLeft + PADDING, guiTop + HEADER_HEIGHT + PADDING, guiLeft + GUI_WIDTH - PADDING, guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING, PEACH_CREAM)
    }

    private fun drawHeader(graphics: GuiGraphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_HEIGHT, PEACH_MEDIUM)
        val title = "§l✿ PeachSoju ✿"
        graphics.drawString(font, title, guiLeft + (GUI_WIDTH - font.width(title)) / 2, guiTop + 10, TEXT_DARK, false)
    }

    private fun drawContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentRight = guiLeft + GUI_WIDTH - PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING

        tooltipToDraw = null

        graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom)

        val elements = buildElements()
        var y = contentTop - scrollY

        for (element in elements) {
            when (element) {
                is GuiElement.SectionHeader -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawSectionHeader(graphics, element, contentLeft, y, contentRight - contentLeft, mouseX, mouseY)
                    }
                    y += ROW_HEIGHT + 4
                }
                is GuiElement.Toggle -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawToggle(graphics, element, contentLeft, y, contentRight - contentLeft, mouseX, mouseY)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.Button -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawButton(graphics, element, contentLeft, y, contentRight - contentLeft, mouseX, mouseY)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.Slider -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawSlider(graphics, element, contentLeft, y, contentRight - contentLeft, mouseX, mouseY)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.BlockPicker -> {
                    if (y + ROW_HEIGHT + 24 >= contentTop && y < contentBottom) {
                        drawBlockPicker(graphics, element, contentLeft, y, contentRight - contentLeft, mouseX, mouseY)
                    }
                    y += ROW_HEIGHT + 24
                }
                is GuiElement.Spacer -> {
                    y += 10
                }
            }
        }

        graphics.disableScissor()
    }

    private fun drawSectionHeader(graphics: GuiGraphics, header: GuiElement.SectionHeader, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        val expanded = header.expandedGetter()
        val enabled = header.enabledGetter()
        val hover = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT

        val baseColor = if (enabled) SECTION_HEADER_ON else SECTION_HEADER_OFF
        val bgColor = if (hover) brighten(baseColor) else baseColor
        graphics.fill(x, y, x + width, y + ROW_HEIGHT, bgColor)

        val arrow = if (expanded) "▼" else "▶"
        val text = "$arrow §l${header.name}"
        graphics.drawString(font, text, x + 5, y + 6, TEXT_LIGHT, false)
    }

    private fun drawToggle(graphics: GuiGraphics, toggle: GuiElement.Toggle, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        val indent = toggle.indent * 12
        val enabled = toggle.getter()

        graphics.drawString(font, toggle.name, x + 5 + indent, y + 6, TEXT_DARK, false)

        val toggleX = x + width - TOGGLE_WIDTH - 5
        val toggleY = y + 3
        val hover = mouseX >= toggleX && mouseX <= toggleX + TOGGLE_WIDTH && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT

        val base = if (enabled) TOGGLE_ON else TOGGLE_OFF
        val fill = if (hover) brighten(base) else base

        graphics.fill(toggleX - 1, toggleY - 1, toggleX + TOGGLE_WIDTH + 1, toggleY + TOGGLE_HEIGHT + 1, PEACH_DARK)
        graphics.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, fill)

        val label = if (enabled) "ON" else "OFF"
        graphics.drawString(font, label, toggleX + (TOGGLE_WIDTH - font.width(label)) / 2, toggleY + 4, TEXT_LIGHT, false)

        if (hover && toggle.description.isNotEmpty()) {
            tooltipToDraw = Triple(mouseX, mouseY, toggle.description)
        }
    }

    private fun drawButton(graphics: GuiGraphics, button: GuiElement.Button, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        val buttonX = x + 5
        val buttonW = width - 10
        val buttonH = ROW_HEIGHT - 4
        val buttonY = y + 2

        val hover = mouseX >= buttonX && mouseX <= buttonX + buttonW && mouseY >= buttonY && mouseY <= buttonY + buttonH

        val bg = if (hover) brighten(PEACH_MEDIUM) else PEACH_MEDIUM
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonW + 1, buttonY + buttonH + 1, PEACH_DARK)
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + buttonH, bg)

        graphics.drawString(font, button.name, buttonX + (buttonW - font.width(button.name)) / 2, buttonY + 5, TEXT_LIGHT, false)

        if (hover && button.description.isNotEmpty()) {
            tooltipToDraw = Triple(mouseX, mouseY, button.description)
        }
    }

    private var draggingSlider: GuiElement.Slider? = null

    private fun drawSlider(graphics: GuiGraphics, slider: GuiElement.Slider, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        val indent = slider.indent * 12
        val currentValue = slider.getter()

        val valueText = "${currentValue.toInt()}${slider.suffix}"
        graphics.drawString(font, slider.name, x + 5 + indent, y + 6, TEXT_DARK, false)

        val sliderX = x + width - 130
        val sliderW = 80
        val sliderY = y + 7
        val sliderH = 8

        graphics.fill(sliderX - 1, sliderY - 1, sliderX + sliderW + 1, sliderY + sliderH + 1, PEACH_DARK)
        graphics.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, 0xFFCCCCCC.toInt())

        val progress = ((currentValue - slider.min) / (slider.max - slider.min)).coerceIn(0.0, 1.0)
        val thumbX = sliderX + (progress * (sliderW - 6)).toInt()

        graphics.fill(sliderX, sliderY, thumbX + 3, sliderY + sliderH, PEACH_MEDIUM)

        graphics.fill(thumbX, sliderY - 1, thumbX + 6, sliderY + sliderH + 1, PEACH_DARK)

        graphics.drawString(font, valueText, sliderX + sliderW + 5, y + 6, TEXT_DARK, false)

        if (draggingSlider == slider) {
            val newProgress = ((mouseX - sliderX).toDouble() / sliderW).coerceIn(0.0, 1.0)
            val newValue = slider.min + (newProgress * (slider.max - slider.min))
            slider.setter(newValue)
        }
    }

    private fun drawBlockPicker(graphics: GuiGraphics, picker: GuiElement.BlockPicker, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int) {
        val labelText = "Selected Block:"
        val currentBlock = FMBlocksEditMode.currentBlockState.block
        val blockKey = BuiltInRegistries.BLOCK.getKey(currentBlock).toString()
        val cleanName = blockKey.removePrefix("minecraft:").replace("_", " ")
        val truncatedName = if (cleanName.length > 25) cleanName.takeLast(22) + "..." else cleanName

        graphics.drawString(font, labelText, x + 5, y + 4, TEXT_DARK, false)
        graphics.drawString(font, "§6$truncatedName", x + 5 + font.width(labelText) + 5, y + 4, TEXT_DARK, false)

        val searchX = x + 5
        val searchY = y + ROW_HEIGHT
        val searchW = width - 10
        val searchH = 18

        val searchHover = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH
        val searchBg = if (blockSearchActive) 0xFFFFFFFF.toInt() else if (searchHover) 0xFFF0F0F0.toInt() else PEACH_CREAM

        graphics.fill(searchX - 1, searchY - 1, searchX + searchW + 1, searchY + searchH + 1, PEACH_DARK)
        graphics.fill(searchX, searchY, searchX + searchW, searchY + searchH, searchBg)

        val displayText = if (blockSearchText.isEmpty() && !blockSearchActive) "§7Search blocks..." else blockSearchText + (if (blockSearchActive) "_" else "")
        graphics.drawString(font, displayText, searchX + 4, searchY + 5, TEXT_DARK, false)
    }

    private fun drawBlockDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (filteredBlocks.isEmpty()) return

        val elements = buildElements()
        var pickerY = guiTop + HEADER_HEIGHT + PADDING - scrollY
        for (element in elements) {
            if (element is GuiElement.BlockPicker) break
            pickerY += when (element) {
                is GuiElement.SectionHeader -> ROW_HEIGHT + 4
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.BlockPicker -> ROW_HEIGHT + 24
                is GuiElement.Spacer -> 10
            }
        }

        val dropdownX = guiLeft + PADDING + 5
        val dropdownY = pickerY + ROW_HEIGHT + 20
        val dropdownW = GUI_WIDTH - PADDING * 2 - 10 - 12
        val itemHeight = 16
        val maxVisible = 8
        val dropdownH = min(filteredBlocks.size, maxVisible) * itemHeight

        graphics.fill(dropdownX - 2, dropdownY - 2, dropdownX + dropdownW + 2, dropdownY + dropdownH + 2, PEACH_DARK)
        graphics.fill(dropdownX, dropdownY, dropdownX + dropdownW, dropdownY + dropdownH, 0xFFFFFFFF.toInt())

        val visibleStart = blockDropdownScroll
        val visibleEnd = min(visibleStart + maxVisible, filteredBlocks.size)

        for (i in visibleStart until visibleEnd) {
            val block = filteredBlocks[i]
            val itemY = dropdownY + (i - visibleStart) * itemHeight
            val hover = mouseX >= dropdownX && mouseX <= dropdownX + dropdownW && mouseY >= itemY && mouseY <= itemY + itemHeight

            if (hover) {
                graphics.fill(dropdownX, itemY, dropdownX + dropdownW, itemY + itemHeight, 0xFFE0E0E0.toInt())
            }

            val key = BuiltInRegistries.BLOCK.getKey(block).toString()
            val cleanKey = key.removePrefix("minecraft:").replace("_", " ")
            val truncated = if (cleanKey.length > 35) cleanKey.takeLast(32) + "..." else cleanKey
            graphics.drawString(font, truncated, dropdownX + 4, itemY + 4, TEXT_DARK, false)
        }

        if (filteredBlocks.size > maxVisible) {
            val scrollbarX = dropdownX + dropdownW - 6
            val scrollbarH = dropdownH
            val thumbH = max(10, (maxVisible.toFloat() / filteredBlocks.size * scrollbarH).toInt())
            val maxDropdownScroll = filteredBlocks.size - maxVisible
            val thumbY = dropdownY + ((blockDropdownScroll.toFloat() / maxDropdownScroll) * (scrollbarH - thumbH)).toInt()

            graphics.fill(scrollbarX, dropdownY, scrollbarX + 4, dropdownY + scrollbarH, 0xFFCCCCCC.toInt())
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbH, PEACH_DARK)
        }
    }

    private fun drawFooter(graphics: GuiGraphics) {
        val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT
        graphics.fill(guiLeft + PADDING, footerY - 5, guiLeft + GUI_WIDTH - PADDING, footerY - 4, PEACH_MEDIUM)

        val footerText = "§7ESC to close"
        graphics.drawString(font, footerText, guiLeft + (GUI_WIDTH - font.width(footerText)) / 2, footerY + 2, TEXT_DARK, false)
    }

    private fun drawTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int, text: String) {
        val w = font.width(text) + 8
        val h = 14
        val x = mouseX + 10
        val y = mouseY - 10

        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, PEACH_DARK)
        graphics.fill(x, y, x + w, y + h, PEACH_CREAM)
        graphics.drawString(font, text, x + 4, y + 3, TEXT_DARK, false)
    }

    private fun brighten(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        var r = (color ushr 16) and 0xFF
        var g = (color ushr 8) and 0xFF
        var b = color and 0xFF
        r = min(255, r + 30)
        g = min(255, g + 30)
        b = min(255, b + 30)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()

        if (event.button() != 0) return super.mouseClicked(event, bl)

        if (blockDropdownOpen) {
            if (handleBlockDropdownClick(mouseX, mouseY)) {
                playClickSound()
                return true
            }
            blockDropdownOpen = false
            blockSearchActive = false
            return true
        }

        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentRight = guiLeft + GUI_WIDTH - PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING

        if (mouseX < contentLeft || mouseX > contentRight || mouseY < contentTop || mouseY > contentBottom) {
            return super.mouseClicked(event, bl)
        }

        val elements = buildElements()
        var y = contentTop - scrollY

        for (element in elements) {
            val elementHeight = when (element) {
                is GuiElement.SectionHeader -> ROW_HEIGHT + 4
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.BlockPicker -> ROW_HEIGHT + 24
                is GuiElement.Spacer -> 10
            }

            if (mouseY >= y && mouseY < y + elementHeight) {
                when (element) {
                    is GuiElement.SectionHeader -> {
                        element.toggle()
                        playClickSound()
                        return true
                    }
                    is GuiElement.Toggle -> {
                        val toggleX = contentRight - TOGGLE_WIDTH - 5
                        val toggleY = y + 3
                        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_WIDTH && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT) {
                            element.toggler()
                            playClickSound()
                            return true
                        }
                    }
                    is GuiElement.Button -> {
                        val buttonX = contentLeft + 5
                        val buttonW = (contentRight - contentLeft) - 10
                        val buttonY = y + 2
                        val buttonH = ROW_HEIGHT - 4
                        if (mouseX >= buttonX && mouseX <= buttonX + buttonW && mouseY >= buttonY && mouseY <= buttonY + buttonH) {
                            element.action()
                            playClickSound()
                            return true
                        }
                    }
                    is GuiElement.Slider -> {
                        val sliderX = contentLeft + (contentRight - contentLeft) - 130
                        val sliderW = 80
                        val sliderY = y + 7
                        val sliderH = 8
                        if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= sliderY - 2 && mouseY <= sliderY + sliderH + 2) {
                            draggingSlider = element
                            val progress = ((mouseX - sliderX).toDouble() / sliderW).coerceIn(0.0, 1.0)
                            val newValue = element.min + (progress * (element.max - element.min))
                            element.setter(newValue)
                            return true
                        }
                    }
                    is GuiElement.BlockPicker -> {
                        val searchX = contentLeft + 5
                        val searchY = y + ROW_HEIGHT
                        val searchW = (contentRight - contentLeft) - 10
                        val searchH = 18
                        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                            blockSearchActive = true
                            blockDropdownOpen = true
                            playClickSound()
                            return true
                        }
                    }
                    is GuiElement.Spacer -> {}
                }
            }

            y += elementHeight
        }

        blockSearchActive = false
        return super.mouseClicked(event, bl)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && draggingSlider != null) {
            draggingSlider = null
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (event.button() == 0 && draggingSlider != null) {
            val mouseX = event.x()
            val slider = draggingSlider!!
            val contentLeft = guiLeft + PADDING
            val contentRight = guiLeft + GUI_WIDTH - PADDING
            val sliderX = contentLeft + (contentRight - contentLeft) - 130
            val sliderW = 80

            val progress = ((mouseX - sliderX) / sliderW).coerceIn(0.0, 1.0)
            val newValue = slider.min + (progress * (slider.max - slider.min))
            slider.setter(newValue)
            return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    private fun handleBlockDropdownClick(mouseX: Int, mouseY: Int): Boolean {
        if (filteredBlocks.isEmpty()) return false

        val elements = buildElements()
        var pickerY = guiTop + HEADER_HEIGHT + PADDING - scrollY
        for (element in elements) {
            if (element is GuiElement.BlockPicker) break
            pickerY += when (element) {
                is GuiElement.SectionHeader -> ROW_HEIGHT + 4
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.BlockPicker -> ROW_HEIGHT + 24
                is GuiElement.Spacer -> 10
            }
        }

        val dropdownX = guiLeft + PADDING + 5
        val dropdownY = pickerY + ROW_HEIGHT + 20
        val dropdownW = (GUI_WIDTH - PADDING * 2) - 10
        val itemHeight = 16
        val maxVisible = 8
        val dropdownH = min(filteredBlocks.size, maxVisible) * itemHeight

        if (mouseX < dropdownX || mouseX > dropdownX + dropdownW || mouseY < dropdownY || mouseY > dropdownY + dropdownH) {
            return false
        }

        val clickedIndex = blockDropdownScroll + ((mouseY - dropdownY) / itemHeight)
        if (clickedIndex >= 0 && clickedIndex < filteredBlocks.size) {
            val block = filteredBlocks[clickedIndex]
            FMBlocksEditMode.setCurrentBlock(block.defaultBlockState())
            blockSearchText = ""
            blockDropdownOpen = false
            blockSearchActive = false
            updateFilteredBlocks()
            return true
        }

        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (blockDropdownOpen && filteredBlocks.size > 6) {
            val maxDropdownScroll = filteredBlocks.size - 6
            blockDropdownScroll = (blockDropdownScroll - verticalAmount.toInt()).coerceIn(0, maxDropdownScroll)
            return true
        }

        if (maxScroll > 0) {
            scrollY = (scrollY - (verticalAmount.toInt() * SCROLL_STEP)).coerceIn(0, maxScroll)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (event.key() == 256) {
            if (blockDropdownOpen) {
                blockDropdownOpen = false
                blockSearchActive = false
                return true
            }
            onClose()
            return true
        }

        if (blockSearchActive) {
            val keyCode = event.key()
            when {
                keyCode == 259 -> {
                    if (blockSearchText.isNotEmpty()) {
                        blockSearchText = blockSearchText.dropLast(1)
                        updateFilteredBlocks()
                        blockDropdownScroll = 0
                    }
                    return true
                }
                keyCode == 257 || keyCode == 335 -> {
                    if (filteredBlocks.isNotEmpty()) {
                        FMBlocksEditMode.setCurrentBlock(filteredBlocks[0].defaultBlockState())
                        blockSearchText = ""
                        blockDropdownOpen = false
                        blockSearchActive = false
                        updateFilteredBlocks()
                    }
                    return true
                }
            }
            return true
        }

        val mc = Minecraft.getInstance()
        if (mc.options.keyInventory.matches(event)) {
            onClose()
            return true
        }

        return super.keyPressed(event)
    }

    override fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        if (blockSearchActive) {
            val codepoint = event.codepoint()
            val char = codepoint.toChar()
            if (char.isLetterOrDigit() || char == '_' || char == ':' || char == ' ') {
                blockSearchText += char
                updateFilteredBlocks()
                blockDropdownScroll = 0
                return true
            }
        }
        return super.charTyped(event)
    }

    private fun openNodeAppearance() {
        Minecraft.getInstance().setScreen(NodeAppearanceScreen(this))
    }

    private fun openHelp() {
        Minecraft.getInstance().setScreen(AutoRoutesHelpScreen(this))
    }

    private fun playClickSound() {
        Minecraft.getInstance().soundManager.play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
            )
        )
    }
}