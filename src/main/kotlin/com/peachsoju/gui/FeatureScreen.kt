package com.peachsoju.gui

import com.peachsoju.modules.impl.fmblocks.FMBlocksEditMode
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Block
import kotlin.math.*

abstract class FeatureScreen(
    private val parent: Screen?,
    private val title: String
) : Screen(Component.literal(title)) {

    companion object {
        const val PEACH_DARK = 0xFF8B5A3C.toInt()
        const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        const val PEACH_CREAM = 0xFFFFF5E6.toInt()
        const val PEACH_GLOW = 0xFFFFE4C4.toInt()

        const val TEXT_DARK = 0xFF4A3728.toInt()
        const val TEXT_LIGHT = 0xFFFFFFFF.toInt()
        const val TEXT_GRAY = 0xFF888888.toInt()

        const val TOGGLE_ON = 0xFF7CB342.toInt()
        const val TOGGLE_OFF = 0xFFB85C5C.toInt()

        const val GUI_WIDTH = 300
        const val GUI_HEIGHT = 280
        const val PADDING = 10
        const val ROW_HEIGHT = 22
        const val TOGGLE_WIDTH = 40
        const val TOGGLE_HEIGHT = 16
        const val HEADER_HEIGHT = 30
        const val FOOTER_HEIGHT = 26
        const val SCROLL_STEP = 14

        const val SCREEN_OPEN_DURATION = 250f
        const val SCREEN_CLOSE_DURATION = 150f
        const val HOVER_LERP_SPEED = 0.18f
        const val TOGGLE_LERP_SPEED = 0.20f
        const val SCROLL_LERP_SPEED = 0.25f
        const val CARD_STAGGER_DELAY = 40f
    }

    object Easing {
        fun easeOutQuad(t: Float): Float = 1f - (1f - t) * (1f - t)
        fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)
        fun easeOutBack(t: Float): Float {
            val c1 = 1.70158f
            val c3 = c1 + 1f
            return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
        }
    }

    protected var guiLeft = 0
    protected var guiTop = 0
    protected var scrollY = 0
    protected var maxScroll = 0

    private var screenOpenTime = 0L
    private var isClosing = false
    private var closeStartTime = 0L
    private var animatedScrollY = 0f
    private val hoverProgress = mutableMapOf<String, Float>()
    private val toggleProgress = mutableMapOf<String, Float>()
    private val cardEntryProgress = mutableMapOf<Int, Float>()
    private var backButtonHover = 0f

    private var tooltipToDraw: Triple<Int, Int, String>? = null
    private var tooltipAlpha = 0f
    private var lastTooltipKey: String? = null

    protected var activeTextField: GuiElement.TextField? = null
    protected val textFieldValues = mutableMapOf<String, String>()

    protected var blockSearchText = ""
    protected var blockSearchActive = false
    protected var filteredBlocks: List<Block> = emptyList()
    protected var blockDropdownOpen = false
    protected var blockDropdownScroll = 0

    private var draggingSlider: GuiElement.Slider? = null

    sealed class GuiElement {
        data class Toggle(
            val name: String,
            val getter: () -> Boolean,
            val toggler: () -> Boolean,
            val description: String = "",
            val indent: Int = 0
        ) : GuiElement()

        data class Button(
            val name: String,
            val action: () -> Unit,
            val description: String = ""
        ) : GuiElement()

        data class Slider(
            val name: String,
            val getter: () -> Double,
            val setter: (Double) -> Unit,
            val min: Double,
            val max: Double,
            val suffix: String = "",
            val indent: Int = 0
        ) : GuiElement()

        data class TextField(
            val id: String,
            val name: String,
            val getter: () -> Double,
            val setter: (Double) -> Unit,
            val min: Double,
            val max: Double,
            val suffix: String = "",
            val description: String = "",
            val indent: Int = 0
        ) : GuiElement()

        data class Label(
            val text: String,
            val color: Int = TEXT_DARK
        ) : GuiElement()

        object BlockPicker : GuiElement()
        object Spacer : GuiElement()
    }

    abstract fun buildElements(): List<GuiElement>

    open fun initTextFields() {}

    override fun init() {
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2
        recomputeMaxScroll()
        initTextFields()

        if (screenOpenTime == 0L) {
            screenOpenTime = System.currentTimeMillis()
        }
        animatedScrollY = scrollY.toFloat()
    }

    protected fun recomputeMaxScroll() {
        val elements = buildElements()
        var contentHeight = 0
        for (element in elements) {
            contentHeight += when (element) {
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.TextField -> ROW_HEIGHT
                is GuiElement.Label -> ROW_HEIGHT
                is GuiElement.BlockPicker -> ROW_HEIGHT + 24
                is GuiElement.Spacer -> 10
            }
        }
        val viewHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2)
        maxScroll = max(0, contentHeight - viewHeight)
        scrollY = scrollY.coerceIn(0, max(0, maxScroll))
    }

    protected fun updateFilteredBlocks() {
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

        val currentTime = System.currentTimeMillis()
        val screenProgress: Float
        val screenAlpha: Float

        if (isClosing) {
            val closeElapsed = (currentTime - closeStartTime).toFloat()
            val closeProgress = (closeElapsed / SCREEN_CLOSE_DURATION).coerceIn(0f, 1f)
            screenProgress = 1f - Easing.easeOutQuad(closeProgress)
            screenAlpha = 1f - closeProgress

            if (closeProgress >= 1f) {
                Minecraft.getInstance().setScreen(parent)
                return
            }
        } else {
            val openElapsed = (currentTime - screenOpenTime).toFloat()
            screenProgress = Easing.easeOutBack((openElapsed / SCREEN_OPEN_DURATION).coerceIn(0f, 1f))
            screenAlpha = Easing.easeOutQuad((openElapsed / (SCREEN_OPEN_DURATION * 0.5f)).coerceIn(0f, 1f))
        }

        animatedScrollY = lerp(animatedScrollY, scrollY.toFloat(), SCROLL_LERP_SPEED)

        val bgAlpha = (screenAlpha * 0.67f * 255).toInt().coerceIn(0, 255)
        graphics.fill(0, 0, width, height, (bgAlpha shl 24))

        drawPanel(graphics, screenAlpha, screenProgress)
        drawHeader(graphics, mouseX, mouseY, screenAlpha)
        drawContent(graphics, mouseX, mouseY, screenAlpha)
        drawFooter(graphics, screenAlpha)

        if (blockDropdownOpen && screenProgress > 0.9f) {
            drawBlockDropdown(graphics, mouseX, mouseY)
        }

        val currentTooltipKey = tooltipToDraw?.third
        if (currentTooltipKey != null && currentTooltipKey == lastTooltipKey) {
            tooltipAlpha = lerp(tooltipAlpha, 1f, 0.25f)
        } else if (currentTooltipKey != null) {
            tooltipAlpha = 0f
            lastTooltipKey = currentTooltipKey
        } else {
            tooltipAlpha = lerp(tooltipAlpha, 0f, 0.3f)
            if (tooltipAlpha < 0.05f) lastTooltipKey = null
        }

        tooltipToDraw?.let { (tx, ty, text) ->
            if (tooltipAlpha > 0.05f) {
                drawTooltip(graphics, tx, ty, text, tooltipAlpha)
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawPanel(graphics: GuiGraphics, alpha: Float, progress: Float) {
        val borderColor = withAlpha(PEACH_DARK, alpha)
        val bgColor = withAlpha(PEACH_LIGHT, alpha)
        val contentBg = withAlpha(PEACH_CREAM, alpha)

        val animatedWidth = (GUI_WIDTH * progress).toInt()
        val animatedHeight = (GUI_HEIGHT * progress).toInt()
        val animatedLeft = guiLeft + (GUI_WIDTH - animatedWidth) / 2
        val animatedTop = guiTop + (GUI_HEIGHT - animatedHeight) / 2

        val glowAlpha = (alpha * 0.15f)
        for (i in 1..3) {
            val glowColor = withAlpha(PEACH_DARK, glowAlpha / i)
            graphics.fill(
                animatedLeft - 2 - i, animatedTop - 2 - i,
                animatedLeft + animatedWidth + 2 + i, animatedTop + animatedHeight + 2 + i,
                glowColor
            )
        }

        graphics.fill(animatedLeft - 2, animatedTop - 2, animatedLeft + animatedWidth + 2, animatedTop + animatedHeight + 2, borderColor)
        graphics.fill(animatedLeft, animatedTop, animatedLeft + animatedWidth, animatedTop + animatedHeight, bgColor)
        graphics.fill(
            guiLeft + PADDING, guiTop + HEADER_HEIGHT + PADDING,
            guiLeft + GUI_WIDTH - PADDING, guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING,
            contentBg
        )
    }

    private fun drawHeader(graphics: GuiGraphics, mouseX: Int, mouseY: Int, alpha: Float) {
        val headerColor = withAlpha(PEACH_MEDIUM, alpha)
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_HEIGHT, headerColor)

        val backText = "← Back"
        val backWidth = font.width(backText) + 10
        val backX = guiLeft + 5
        val backY = guiTop + 5
        val backH = 20

        val backHover = mouseX >= backX && mouseX <= backX + backWidth &&
                mouseY >= backY && mouseY <= backY + backH && !isClosing

        val targetBackHover = if (backHover) 1f else 0f
        backButtonHover = lerp(backButtonHover, targetBackHover, HOVER_LERP_SPEED)

        val backColor = lerpColor(PEACH_DARK, brighten(PEACH_DARK, 40), backButtonHover)

        graphics.fill(backX, backY, backX + backWidth, backY + backH, withAlpha(backColor, alpha))
        graphics.drawString(font, backText, backX + 5, backY + 6, withAlpha(TEXT_LIGHT, alpha), false)

        val titleText = "§l$title"
        graphics.drawString(
            font, titleText,
            guiLeft + (GUI_WIDTH - font.width(titleText)) / 2,
            guiTop + 10,
            withAlpha(TEXT_DARK, alpha), false
        )
    }

    private fun drawContent(graphics: GuiGraphics, mouseX: Int, mouseY: Int, alpha: Float) {
        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentRight = guiLeft + GUI_WIDTH - PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING

        tooltipToDraw = null

        graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom)

        val elements = buildElements()
        var y = contentTop - animatedScrollY.toInt()
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - screenOpenTime).toFloat()

        for ((index, element) in elements.withIndex()) {
            val entryDelay = index * CARD_STAGGER_DELAY
            val entryProgress = ((elapsed - entryDelay - 100f) / 200f).coerceIn(0f, 1f)
            val easedEntry = Easing.easeOutCubic(entryProgress)
            cardEntryProgress[index] = easedEntry

            val slideOffset = ((1f - easedEntry) * 30).toInt()
            val elementAlpha = alpha * easedEntry

            when (element) {
                is GuiElement.Toggle -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawToggle(graphics, element, contentLeft + slideOffset, y, contentRight - contentLeft, mouseX, mouseY, elementAlpha)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.Button -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawButton(graphics, element, contentLeft + slideOffset, y, contentRight - contentLeft, mouseX, mouseY, elementAlpha)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.Slider -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawSlider(graphics, element, contentLeft + slideOffset, y, contentRight - contentLeft, mouseX, mouseY, elementAlpha)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.TextField -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        drawTextField(graphics, element, contentLeft + slideOffset, y, contentRight - contentLeft, mouseX, mouseY, elementAlpha)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.Label -> {
                    if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                        graphics.drawString(font, element.text, contentLeft + 5 + slideOffset, y + 6, withAlpha(element.color, elementAlpha), false)
                    }
                    y += ROW_HEIGHT
                }
                is GuiElement.BlockPicker -> {
                    if (y + ROW_HEIGHT + 24 >= contentTop && y < contentBottom) {
                        drawBlockPicker(graphics, contentLeft + slideOffset, y, contentRight - contentLeft, mouseX, mouseY, elementAlpha)
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

    private fun drawToggle(
        graphics: GuiGraphics,
        toggle: GuiElement.Toggle,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        alpha: Float
    ) {
        val indent = toggle.indent * 12
        val enabled = toggle.getter()

        graphics.drawString(font, toggle.name, x + 5 + indent, y + 6, withAlpha(TEXT_DARK, alpha), false)

        val toggleX = x + width - TOGGLE_WIDTH - 5
        val toggleY = y + 3

        val hover = mouseX >= toggleX && mouseX <= toggleX + TOGGLE_WIDTH &&
                mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT && !isClosing

        val hoverKey = "toggle_hover_${toggle.name}"
        val targetHover = if (hover) 1f else 0f
        val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
        val newHover = lerp(currentHover, targetHover, HOVER_LERP_SPEED)
        hoverProgress[hoverKey] = newHover

        val toggleKey = "toggle_state_${toggle.name}"
        val targetToggle = if (enabled) 1f else 0f
        val currentToggle = toggleProgress.getOrDefault(toggleKey, targetToggle)
        val newToggle = lerp(currentToggle, targetToggle, TOGGLE_LERP_SPEED)
        toggleProgress[toggleKey] = newToggle

        val trackColor = lerpColor(TOGGLE_OFF, TOGGLE_ON, newToggle)
        val finalTrackColor = lerpColor(trackColor, brighten(trackColor, 25), newHover)

        graphics.fill(toggleX - 1, toggleY - 1, toggleX + TOGGLE_WIDTH + 1, toggleY + TOGGLE_HEIGHT + 1, withAlpha(PEACH_DARK, alpha))
        graphics.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, withAlpha(finalTrackColor, alpha))

        val knobWidth = 12
        val knobPadding = 2
        val knobTravel = TOGGLE_WIDTH - knobWidth - (knobPadding * 2)
        val knobX = toggleX + knobPadding + (newToggle * knobTravel).toInt()
        val knobY = toggleY + knobPadding
        val knobH = TOGGLE_HEIGHT - (knobPadding * 2)

        graphics.fill(knobX, knobY, knobX + knobWidth, knobY + knobH, withAlpha(0xFFFFFFFF.toInt(), alpha))

        if (hover && toggle.description.isNotEmpty()) {
            tooltipToDraw = Triple(mouseX, mouseY, toggle.description)
        }
    }

    private fun drawButton(
        graphics: GuiGraphics,
        button: GuiElement.Button,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        alpha: Float
    ) {
        val buttonX = x + 5
        val buttonW = width - 10
        val buttonH = ROW_HEIGHT - 4
        val buttonY = y + 2

        val hover = mouseX >= buttonX && mouseX <= buttonX + buttonW &&
                mouseY >= buttonY && mouseY <= buttonY + buttonH && !isClosing

        val hoverKey = "button_${button.name}"
        val targetHover = if (hover) 1f else 0f
        val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
        val newHover = lerp(currentHover, targetHover, HOVER_LERP_SPEED)
        hoverProgress[hoverKey] = newHover

        val bgColor = lerpColor(PEACH_MEDIUM, brighten(PEACH_MEDIUM, 30), newHover)
        val yOffset = (-newHover * 1.5f).toInt()

        graphics.fill(buttonX - 1, buttonY - 1 + yOffset, buttonX + buttonW + 1, buttonY + buttonH + 1 + yOffset, withAlpha(PEACH_DARK, alpha))
        graphics.fill(buttonX, buttonY + yOffset, buttonX + buttonW, buttonY + buttonH + yOffset, withAlpha(bgColor, alpha))
        graphics.drawString(font, button.name, buttonX + (buttonW - font.width(button.name)) / 2, buttonY + 5 + yOffset, withAlpha(TEXT_LIGHT, alpha), false)

        if (hover && button.description.isNotEmpty()) {
            tooltipToDraw = Triple(mouseX, mouseY, button.description)
        }
    }

    private fun drawSlider(
        graphics: GuiGraphics,
        slider: GuiElement.Slider,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        alpha: Float
    ) {
        val indent = slider.indent * 12
        val currentValue = slider.getter()
        val valueText = "${currentValue.toInt()}${slider.suffix}"

        graphics.drawString(font, slider.name, x + 5 + indent, y + 6, withAlpha(TEXT_DARK, alpha), false)

        val sliderX = x + width - 130
        val sliderW = 80
        val sliderY = y + 7
        val sliderH = 8

        graphics.fill(sliderX - 1, sliderY - 1, sliderX + sliderW + 1, sliderY + sliderH + 1, withAlpha(PEACH_DARK, alpha))
        graphics.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, withAlpha(0xFFCCCCCC.toInt(), alpha))

        val progress = ((currentValue - slider.min) / (slider.max - slider.min)).coerceIn(0.0, 1.0)
        val thumbX = sliderX + (progress * (sliderW - 6)).toInt()

        graphics.fill(sliderX, sliderY, thumbX + 3, sliderY + sliderH, withAlpha(PEACH_MEDIUM, alpha))

        val thumbHover = mouseX >= thumbX - 2 && mouseX <= thumbX + 8 &&
                mouseY >= sliderY - 4 && mouseY <= sliderY + sliderH + 4

        val thumbKey = "slider_${slider.name}"
        val targetThumbHover = if (thumbHover || draggingSlider == slider) 1f else 0f
        val currentThumbHover = hoverProgress.getOrDefault(thumbKey, 0f)
        val newThumbHover = lerp(currentThumbHover, targetThumbHover, HOVER_LERP_SPEED)
        hoverProgress[thumbKey] = newThumbHover

        val thumbColor = lerpColor(PEACH_DARK, brighten(PEACH_DARK, 30), newThumbHover)

        graphics.fill(thumbX, sliderY - 1, thumbX + 6, sliderY + sliderH + 1, withAlpha(thumbColor, alpha))

        graphics.drawString(font, valueText, sliderX + sliderW + 5, y + 6, withAlpha(TEXT_DARK, alpha), false)

        if (draggingSlider == slider) {
            val newProgress = ((mouseX - sliderX).toDouble() / sliderW).coerceIn(0.0, 1.0)
            val newValue = slider.min + (newProgress * (slider.max - slider.min))
            slider.setter(newValue)
        }
    }

    private fun drawTextField(
        graphics: GuiGraphics,
        textField: GuiElement.TextField,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        alpha: Float
    ) {
        val indent = textField.indent * 12
        val isActive = activeTextField == textField

        graphics.drawString(font, textField.name, x + 5 + indent, y + 6, withAlpha(TEXT_DARK, alpha), false)

        val fieldX = x + width - 80
        val fieldY = y + 3
        val fieldW = 50
        val fieldH = 16

        val hover = mouseX >= fieldX && mouseX <= fieldX + fieldW &&
                mouseY >= fieldY && mouseY <= fieldY + fieldH && !isClosing

        val hoverKey = "textfield_${textField.id}"
        val targetHover = if (hover || isActive) 1f else 0f
        val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
        val newHover = lerp(currentHover, targetHover, HOVER_LERP_SPEED)
        hoverProgress[hoverKey] = newHover

        val borderColor = lerpColor(PEACH_DARK, PEACH_MEDIUM, newHover)
        val bgColor = lerpColor(0xFFE8E8E8.toInt(), 0xFFFFFFFF.toInt(), newHover)

        graphics.fill(fieldX - 1, fieldY - 1, fieldX + fieldW + 1, fieldY + fieldH + 1, withAlpha(borderColor, alpha))
        graphics.fill(fieldX, fieldY, fieldX + fieldW, fieldY + fieldH, withAlpha(bgColor, alpha))

        val currentText = textFieldValues[textField.id] ?: String.format("%.2f", textField.getter())

        val cursorVisible = isActive && ((System.currentTimeMillis() / 500) % 2 == 0L)
        val displayText = if (cursorVisible) "$currentText|" else currentText

        graphics.drawString(font, displayText, fieldX + 4, fieldY + 4, withAlpha(TEXT_DARK, alpha), false)
        graphics.drawString(font, textField.suffix, fieldX + fieldW + 5, y + 6, withAlpha(TEXT_GRAY, alpha), false)

        if (hover && textField.description.isNotEmpty()) {
            tooltipToDraw = Triple(mouseX, mouseY, textField.description)
        }
    }

    private fun drawBlockPicker(
        graphics: GuiGraphics,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        alpha: Float
    ) {
        val labelText = "Selected Block:"
        val currentBlock = FMBlocksEditMode.currentBlockState.block
        val blockKey = BuiltInRegistries.BLOCK.getKey(currentBlock).toString()
        val cleanName = blockKey.removePrefix("minecraft:").replace("_", " ")
        val truncatedName = if (cleanName.length > 25) cleanName.takeLast(22) + "..." else cleanName

        graphics.drawString(font, labelText, x + 5, y + 4, withAlpha(TEXT_DARK, alpha), false)
        graphics.drawString(font, "§6$truncatedName", x + 5 + font.width(labelText) + 5, y + 4, withAlpha(TEXT_DARK, alpha), false)

        val searchX = x + 5
        val searchY = y + ROW_HEIGHT
        val searchW = width - 10
        val searchH = 18

        val searchHover = mouseX >= searchX && mouseX <= searchX + searchW &&
                mouseY >= searchY && mouseY <= searchY + searchH && !isClosing

        val hoverKey = "block_search"
        val targetHover = if (searchHover || blockSearchActive) 1f else 0f
        val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
        val newHover = lerp(currentHover, targetHover, HOVER_LERP_SPEED)
        hoverProgress[hoverKey] = newHover

        val searchBg = lerpColor(PEACH_CREAM, 0xFFFFFFFF.toInt(), newHover)

        graphics.fill(searchX - 1, searchY - 1, searchX + searchW + 1, searchY + searchH + 1, withAlpha(PEACH_DARK, alpha))
        graphics.fill(searchX, searchY, searchX + searchW, searchY + searchH, withAlpha(searchBg, alpha))

        val cursorVisible = blockSearchActive && ((System.currentTimeMillis() / 500) % 2 == 0L)
        val displayText = if (blockSearchText.isEmpty() && !blockSearchActive) {
            "§7Search blocks..."
        } else {
            blockSearchText + (if (cursorVisible) "_" else "")
        }
        graphics.drawString(font, displayText, searchX + 4, searchY + 5, withAlpha(TEXT_DARK, alpha), false)
    }

    private fun drawBlockDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (filteredBlocks.isEmpty()) return

        val elements = buildElements()
        var pickerY = guiTop + HEADER_HEIGHT + PADDING - animatedScrollY.toInt()
        for (element in elements) {
            if (element is GuiElement.BlockPicker) break
            pickerY += when (element) {
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.TextField -> ROW_HEIGHT
                is GuiElement.Label -> ROW_HEIGHT
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

        for (i in 1..4) {
            val shadowAlpha = 0.08f / i
            graphics.fill(
                dropdownX - 2 + i, dropdownY - 2 + i,
                dropdownX + dropdownW + 2 + i, dropdownY + dropdownH + 2 + i,
                withAlpha(0xFF000000.toInt(), shadowAlpha)
            )
        }

        graphics.fill(dropdownX - 2, dropdownY - 2, dropdownX + dropdownW + 2, dropdownY + dropdownH + 2, PEACH_DARK)
        graphics.fill(dropdownX, dropdownY, dropdownX + dropdownW, dropdownY + dropdownH, 0xFFFFFFFF.toInt())

        val visibleStart = blockDropdownScroll
        val visibleEnd = min(visibleStart + maxVisible, filteredBlocks.size)

        for (i in visibleStart until visibleEnd) {
            val block = filteredBlocks[i]
            val itemY = dropdownY + (i - visibleStart) * itemHeight

            val itemHover = mouseX >= dropdownX && mouseX <= dropdownX + dropdownW &&
                    mouseY >= itemY && mouseY <= itemY + itemHeight

            val hoverKey = "dropdown_item_$i"
            val targetHover = if (itemHover) 1f else 0f
            val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
            val newHover = lerp(currentHover, targetHover, 0.25f)
            hoverProgress[hoverKey] = newHover

            if (newHover > 0.01f) {
                val hoverColor = lerpColor(0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), newHover)
                graphics.fill(dropdownX, itemY, dropdownX + dropdownW, itemY + itemHeight, hoverColor)
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

    private fun drawFooter(graphics: GuiGraphics, alpha: Float) {
        val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT
        graphics.fill(guiLeft + PADDING, footerY - 5, guiLeft + GUI_WIDTH - PADDING, footerY - 4, withAlpha(PEACH_MEDIUM, alpha))

        val footerText = "§7ESC to go back"
        graphics.drawString(font, footerText, guiLeft + (GUI_WIDTH - font.width(footerText)) / 2, footerY + 2, withAlpha(TEXT_DARK, alpha), false)
    }

    private fun drawTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int, text: String, alpha: Float) {
        val w = font.width(text) + 8
        val h = 14
        val tx = mouseX + 10
        val ty = mouseY - 10

        graphics.fill(tx - 2, ty - 2, tx + w + 2, ty + h + 2, withAlpha(PEACH_DARK, alpha))
        graphics.fill(tx, ty, tx + w, ty + h, withAlpha(PEACH_CREAM, alpha))
        graphics.drawString(font, text, tx + 4, ty + 3, withAlpha(TEXT_DARK, alpha), false)
    }

    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }

    private fun lerpColor(colorA: Int, colorB: Int, t: Float): Int {
        val aA = (colorA ushr 24) and 0xFF
        val rA = (colorA ushr 16) and 0xFF
        val gA = (colorA ushr 8) and 0xFF
        val bA = colorA and 0xFF

        val aB = (colorB ushr 24) and 0xFF
        val rB = (colorB ushr 16) and 0xFF
        val gB = (colorB ushr 8) and 0xFF
        val bB = colorB and 0xFF

        val a = (aA + (aB - aA) * t).toInt().coerceIn(0, 255)
        val r = (rA + (rB - rA) * t).toInt().coerceIn(0, 255)
        val g = (gA + (gB - gA) * t).toInt().coerceIn(0, 255)
        val b = (bA + (bB - bA) * t).toInt().coerceIn(0, 255)

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = ((color ushr 24) and 0xFF) * alpha
        return (a.toInt() shl 24) or (color and 0x00FFFFFF)
    }

    protected fun brighten(color: Int, amount: Int = 30): Int {
        val a = (color ushr 24) and 0xFF
        var r = (color ushr 16) and 0xFF
        var g = (color ushr 8) and 0xFF
        var b = color and 0xFF
        r = min(255, r + amount)
        g = min(255, g + amount)
        b = min(255, b + amount)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (isClosing) return false

        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()

        if (event.button() != 0) return super.mouseClicked(event, bl)

        val backText = "← Back"
        val backWidth = font.width(backText) + 10
        val backX = guiLeft + 5
        val backY = guiTop + 5
        val backH = 20
        if (mouseX >= backX && mouseX <= backX + backWidth && mouseY >= backY && mouseY <= backY + backH) {
            playClickSound()
            startClosing()
            return true
        }

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
            if (activeTextField != null) {
                commitTextField()
            }
            return super.mouseClicked(event, bl)
        }

        val elements = buildElements()
        var y = contentTop - animatedScrollY.toInt()

        for (element in elements) {
            val elementHeight = when (element) {
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.TextField -> ROW_HEIGHT
                is GuiElement.Label -> ROW_HEIGHT
                is GuiElement.BlockPicker -> ROW_HEIGHT + 24
                is GuiElement.Spacer -> 10
            }

            if (mouseY >= y && mouseY < y + elementHeight) {
                when (element) {
                    is GuiElement.Toggle -> {
                        val toggleX = contentRight - TOGGLE_WIDTH - 5
                        val toggleY = y + 3
                        if (mouseX >= toggleX && mouseX <= toggleX + TOGGLE_WIDTH && mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT) {
                            commitTextField()
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
                            commitTextField()
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
                            commitTextField()
                            draggingSlider = element
                            val progress = ((mouseX - sliderX).toDouble() / sliderW).coerceIn(0.0, 1.0)
                            val newValue = element.min + (progress * (element.max - element.min))
                            element.setter(newValue)
                            return true
                        }
                    }
                    is GuiElement.TextField -> {
                        val fieldX = contentLeft + (contentRight - contentLeft) - 80
                        val fieldY = y + 3
                        val fieldW = 50
                        val fieldH = 16
                        if (mouseX >= fieldX && mouseX <= fieldX + fieldW && mouseY >= fieldY && mouseY <= fieldY + fieldH) {
                            if (activeTextField != null && activeTextField != element) {
                                commitTextField()
                            }
                            activeTextField = element
                            if (!textFieldValues.containsKey(element.id)) {
                                textFieldValues[element.id] = String.format("%.2f", element.getter())
                            }
                            playClickSound()
                            return true
                        } else if (activeTextField == element) {
                            commitTextField()
                        }
                    }
                    is GuiElement.BlockPicker -> {
                        val searchX = contentLeft + 5
                        val searchY = y + ROW_HEIGHT
                        val searchW = (contentRight - contentLeft) - 10
                        val searchH = 18
                        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                            commitTextField()
                            blockSearchActive = true
                            blockDropdownOpen = true
                            playClickSound()
                            return true
                        }
                    }
                    else -> {}
                }
            }

            y += elementHeight
        }

        if (activeTextField != null) {
            commitTextField()
        }
        blockSearchActive = false
        return super.mouseClicked(event, bl)
    }

    private fun handleBlockDropdownClick(mouseX: Int, mouseY: Int): Boolean {
        if (filteredBlocks.isEmpty()) return false

        val elements = buildElements()
        var pickerY = guiTop + HEADER_HEIGHT + PADDING - animatedScrollY.toInt()
        for (element in elements) {
            if (element is GuiElement.BlockPicker) break
            pickerY += when (element) {
                is GuiElement.Toggle -> ROW_HEIGHT
                is GuiElement.Button -> ROW_HEIGHT
                is GuiElement.Slider -> ROW_HEIGHT
                is GuiElement.TextField -> ROW_HEIGHT
                is GuiElement.Label -> ROW_HEIGHT
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

    protected fun commitTextField() {
        val field = activeTextField ?: return
        val text = textFieldValues[field.id] ?: return

        val value = text.toDoubleOrNull()
        if (value != null) {
            val clampedValue = value.coerceIn(field.min, field.max)
            field.setter(clampedValue)
            textFieldValues[field.id] = String.format("%.2f", clampedValue)
        } else {
            textFieldValues[field.id] = String.format("%.2f", field.getter())
        }

        activeTextField = null
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

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (blockDropdownOpen && filteredBlocks.size > 8) {
            val maxDropdownScroll = filteredBlocks.size - 8
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
            if (activeTextField != null) {
                commitTextField()
                return true
            }
            if (blockDropdownOpen) {
                blockDropdownOpen = false
                blockSearchActive = false
                return true
            }
            startClosing()
            return true
        }

        if (activeTextField != null) {
            val field = activeTextField!!
            val keyCode = event.key()

            when {
                keyCode == 259 -> {
                    val current = textFieldValues[field.id] ?: ""
                    if (current.isNotEmpty()) {
                        textFieldValues[field.id] = current.dropLast(1)
                    }
                    return true
                }
                keyCode == 257 || keyCode == 335 -> {
                    commitTextField()
                    return true
                }
                keyCode == 258 -> {
                    commitTextField()
                    return true
                }
            }
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
            startClosing()
            return true
        }

        return super.keyPressed(event)
    }

    override fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        if (activeTextField != null) {
            val field = activeTextField!!
            val codepoint = event.codepoint()
            val char = codepoint.toChar()

            if (char.isDigit() || char == '.' || char == '-') {
                val current = textFieldValues[field.id] ?: ""
                val newText = current + char

                if (char == '.' && current.contains('.')) return true
                if (char == '-' && current.isNotEmpty()) return true

                if (newText.length <= 8) {
                    textFieldValues[field.id] = newText
                }
                return true
            }
            return true
        }

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

    private fun startClosing() {
        if (!isClosing) {
            isClosing = true
            closeStartTime = System.currentTimeMillis()
        }
    }

    protected fun playClickSound() {
        Minecraft.getInstance().soundManager.play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
            )
        )
    }
}