package com.peachsoju.gui

import com.peachsoju.config
import com.peachsoju.modules.impl.autoroutes.NodeAppearanceSettings
import com.peachsoju.modules.impl.autoroutes.RenderStyle
import com.peachsoju.modules.impl.autoroutes.data.WPType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.abs
import kotlin.math.max

class NodeAppearanceScreen(private val parent: Screen?) : Screen(Component.literal("Node Appearance")) {

    companion object {
        private const val PEACH_DARK = 0xFF8B5A3C.toInt()
        private const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        private const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        private const val PEACH_CREAM = 0xFFFFF5E6.toInt()
        private const val TEXT_DARK = 0xFF4A3728.toInt()
        private const val TEXT_LIGHT = 0xFFFFFFFF.toInt()

        private const val GUI_WIDTH = 320
        private const val GUI_HEIGHT = 300
        private const val PADDING = 10
        private const val ROW_HEIGHT = 36
        private const val HEADER_HEIGHT = 30
        private const val SCROLL_STEP = 14

        private const val COLOR_PICKER_SIZE = 100
        private const val HUE_BAR_WIDTH = 15
    }

    private var guiLeft = 0
    private var guiTop = 0
    private var scrollY = 0
    private var maxScroll = 0

    private var selectedType: WPType? = null
    private var showColorPicker = false
    private var showStyleDropdown = false
    private var dropdownType: WPType? = null

    private var currentHue = 0f
    private var currentSaturation = 1f
    private var currentValue = 1f
    private var draggingHue = false
    private var draggingSV = false

    override fun init() {
        super.init()
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2
        recomputeMaxScroll()
    }

    private fun recomputeMaxScroll() {
        val contentHeight = WPType.entries.size * ROW_HEIGHT
        val viewHeight = GUI_HEIGHT - HEADER_HEIGHT - PADDING * 2
        maxScroll = max(0, contentHeight - viewHeight)
        scrollY = scrollY.coerceIn(0, max(0, maxScroll))
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, 0xAA000000.toInt())
        drawPanel(graphics)
        drawHeader(graphics)
        drawNodeTypeList(graphics, mouseX, mouseY)
        if (showColorPicker && selectedType != null) drawColorPicker(graphics)
        if (showStyleDropdown && dropdownType != null) drawStyleDropdown(graphics, mouseX, mouseY)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun mouseClicked(event: MouseButtonEvent, consumed: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        if (event.button() != 0) return super.mouseClicked(event, consumed)

        if (mouseX in (guiLeft + 5)..(guiLeft + 60) && mouseY in (guiTop + 5)..(guiTop + 25)) {
            Minecraft.getInstance().setScreen(parent)
            return true
        }

        if (showStyleDropdown && dropdownType != null) {
            val typeIndex = WPType.entries.indexOf(dropdownType)
            val rowY = (guiTop + HEADER_HEIGHT + PADDING) + (typeIndex * ROW_HEIGHT) - scrollY
            val dropdownX = guiLeft + 140
            val dropdownY = rowY + 20
            val dropdownWidth = 120
            val itemHeight = 18

            for ((i, style) in RenderStyle.entries.withIndex()) {
                val itemY = dropdownY + (i * itemHeight)
                if (mouseX in dropdownX..(dropdownX + dropdownWidth) && mouseY in itemY..(itemY + itemHeight)) {
                    NodeAppearanceSettings.setStyle(dropdownType!!, style)
                    showStyleDropdown = false
                    dropdownType = null
                    playClick()
                    return true
                }
            }

            showStyleDropdown = false
            dropdownType = null
            return true
        }

        if (showColorPicker && selectedType != null) {
            val pickerX = guiLeft + GUI_WIDTH + 10
            val pickerY = guiTop + 50
            val hueBarX = pickerX + COLOR_PICKER_SIZE + 5

            if (mouseX in pickerX until (pickerX + COLOR_PICKER_SIZE) && mouseY in pickerY until (pickerY + COLOR_PICKER_SIZE)) {
                draggingSV = true
                updateSv(mouseX, mouseY, pickerX, pickerY)
                return true
            }

            if (mouseX in hueBarX until (hueBarX + HUE_BAR_WIDTH) && mouseY in pickerY until (pickerY + COLOR_PICKER_SIZE)) {
                draggingHue = true
                updateHue(mouseY, pickerY)
                return true
            }

            val previewY = pickerY + COLOR_PICKER_SIZE + 10
            if (mouseX in (pickerX + 35)..(pickerX + 110) && mouseY in previewY..(previewY + 20)) {
                applyColor()
                showColorPicker = false
                selectedType = null
                playClick()
                return true
            }

            if (mouseX < pickerX - 5 || mouseX > pickerX + COLOR_PICKER_SIZE + HUE_BAR_WIDTH + 15) {
                showColorPicker = false
                selectedType = null
                return true
            }
        }

        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentBottom = guiTop + GUI_HEIGHT - PADDING

        if (mouseY < contentTop || mouseY > contentBottom) {
            return super.mouseClicked(event, consumed)
        }

        val startY = contentTop - scrollY
        for ((i, type) in WPType.entries.withIndex()) {
            val rowY = startY + (i * ROW_HEIGHT)

            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) continue

            val colorX = guiLeft + 100
            val colorY = rowY + 2
            val colorSize = 20

            if (mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)) {
                selectedType = type
                showColorPicker = true
                showStyleDropdown = false

                val a = config.getNodeAppearance(type)
                val (h, s, v) = rgbToHsv(a.color.r, a.color.g, a.color.b)
                currentHue = h
                currentSaturation = s
                currentValue = v

                playClick()
                return true
            }

            val styleX = guiLeft + 140
            val styleWidth = 120
            val styleHeight = 18
            if (mouseX in styleX..(styleX + styleWidth) && mouseY in colorY..(colorY + styleHeight)) {
                dropdownType = type
                showStyleDropdown = true
                showColorPicker = false
                playClick()
                return true
            }
        }

        return super.mouseClicked(event, consumed)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (!(showColorPicker && selectedType != null)) return super.mouseDragged(event, deltaX, deltaY)

        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        val pickerX = guiLeft + GUI_WIDTH + 10
        val pickerY = guiTop + 50

        if (draggingSV) {
            updateSv(mouseX, mouseY, pickerX, pickerY)
            return true
        }
        if (draggingHue) {
            updateHue(mouseY, pickerY)
            return true
        }

        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingSV = false
        draggingHue = false
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (showStyleDropdown || showColorPicker) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        if (maxScroll > 0) {
            scrollY = (scrollY - (verticalAmount.toInt() * SCROLL_STEP)).coerceIn(0, maxScroll)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() != 256) return super.keyPressed(event)
        if (showColorPicker) {
            showColorPicker = false
            selectedType = null
            return true
        }
        if (showStyleDropdown) {
            showStyleDropdown = false
            dropdownType = null
            return true
        }
        Minecraft.getInstance().setScreen(parent)
        return true
    }

    private fun drawPanel(graphics: GuiGraphics) {
        graphics.fill(guiLeft - 2, guiTop - 2, guiLeft + GUI_WIDTH + 2, guiTop + GUI_HEIGHT + 2, PEACH_DARK)
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, PEACH_LIGHT)
        graphics.fill(guiLeft + PADDING, guiTop + HEADER_HEIGHT + PADDING, guiLeft + GUI_WIDTH - PADDING, guiTop + GUI_HEIGHT - PADDING, PEACH_CREAM)
    }

    private fun drawHeader(graphics: GuiGraphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_HEIGHT, PEACH_MEDIUM)
        val title = "§l✿ Node Appearance ✿"
        graphics.drawString(font, title, guiLeft + (GUI_WIDTH - font.width(title)) / 2, guiTop + 10, TEXT_DARK, false)
        graphics.drawString(font, "< Back", guiLeft + 10, guiTop + 10, TEXT_DARK, false)
    }

    private fun drawNodeTypeList(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentRight = guiLeft + GUI_WIDTH - PADDING
        val contentBottom = guiTop + GUI_HEIGHT - PADDING

        graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom)

        val startY = contentTop - scrollY
        for ((i, type) in WPType.entries.withIndex()) {
            val rowY = startY + (i * ROW_HEIGHT)

            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) continue

            val appearance = config.getNodeAppearance(type)

            graphics.drawString(font, type.name.lowercase().replaceFirstChar { it.uppercase() }, guiLeft + PADDING + 5, rowY + 4, TEXT_DARK, false)

            val colorX = guiLeft + 100
            val colorY = rowY + 2
            val colorSize = 20
            val isColorHover = mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize) && mouseY in contentTop..contentBottom

            graphics.fill(colorX - 1, colorY - 1, colorX + colorSize + 1, colorY + colorSize + 1, PEACH_DARK)

            val colorInt = (0xFF shl 24) or (appearance.color.r shl 16) or (appearance.color.g shl 8) or appearance.color.b
            graphics.fill(colorX, colorY, colorX + colorSize, colorY + colorSize, colorInt)
            if (isColorHover) graphics.fill(colorX, colorY, colorX + colorSize, colorY + colorSize, 0x40FFFFFF)

            val styleX = guiLeft + 140
            val styleWidth = 120
            val styleHeight = 18
            val isStyleHover = mouseX in styleX..(styleX + styleWidth) && mouseY in colorY..(colorY + styleHeight) && mouseY in contentTop..contentBottom
            val styleBg = if (isStyleHover) brighten(PEACH_MEDIUM) else PEACH_MEDIUM

            graphics.fill(styleX - 1, colorY - 1, styleX + styleWidth + 1, colorY + styleHeight + 1, PEACH_DARK)
            graphics.fill(styleX, colorY, styleX + styleWidth, colorY + styleHeight, styleBg)

            val styleText = RenderStyle.fromString(appearance.style).displayName
            graphics.drawString(font, styleText, styleX + 5, colorY + 5, TEXT_LIGHT, false)
            graphics.drawString(font, "▼", styleX + styleWidth - 12, colorY + 5, TEXT_LIGHT, false)
        }

        graphics.disableScissor()

        if (maxScroll > 0) {
            val scrollbarX = contentRight - 6
            val scrollbarTop = contentTop + 2
            val scrollbarBottom = contentBottom - 2
            val scrollbarHeight = scrollbarBottom - scrollbarTop

            val thumbHeight = max(16, (scrollbarHeight * (scrollbarHeight.toFloat() / (scrollbarHeight + maxScroll))).toInt())
            val thumbY = scrollbarTop + ((scrollY.toFloat() / maxScroll) * (scrollbarHeight - thumbHeight)).toInt()

            graphics.fill(scrollbarX, scrollbarTop, scrollbarX + 4, scrollbarBottom, PEACH_DARK)
            graphics.fill(scrollbarX + 1, thumbY, scrollbarX + 3, thumbY + thumbHeight, PEACH_MEDIUM)
        }
    }

    private fun drawColorPicker(graphics: GuiGraphics) {
        val pickerX = guiLeft + GUI_WIDTH + 10
        val pickerY = guiTop + 50

        graphics.fill(pickerX - 5, pickerY - 5, pickerX + COLOR_PICKER_SIZE + HUE_BAR_WIDTH + 15, pickerY + COLOR_PICKER_SIZE + 40, PEACH_DARK)
        graphics.fill(pickerX - 3, pickerY - 3, pickerX + COLOR_PICKER_SIZE + HUE_BAR_WIDTH + 13, pickerY + COLOR_PICKER_SIZE + 38, PEACH_LIGHT)

        for (x in 0 until COLOR_PICKER_SIZE) for (y in 0 until COLOR_PICKER_SIZE) {
            val s = x.toFloat() / COLOR_PICKER_SIZE
            val v = 1f - (y.toFloat() / COLOR_PICKER_SIZE)
            graphics.fill(pickerX + x, pickerY + y, pickerX + x + 1, pickerY + y + 1, hsvToRgb(currentHue, s, v))
        }

        val svCursorX = pickerX + (currentSaturation * COLOR_PICKER_SIZE).toInt()
        val svCursorY = pickerY + ((1f - currentValue) * COLOR_PICKER_SIZE).toInt()
        graphics.fill(svCursorX - 3, svCursorY - 3, svCursorX + 3, svCursorY + 3, 0xFFFFFFFF.toInt())
        graphics.fill(svCursorX - 2, svCursorY - 2, svCursorX + 2, svCursorY + 2, 0xFF000000.toInt())

        val hueBarX = pickerX + COLOR_PICKER_SIZE + 5
        for (y in 0 until COLOR_PICKER_SIZE) graphics.fill(
            hueBarX,
            pickerY + y,
            hueBarX + HUE_BAR_WIDTH,
            pickerY + y + 1,
            hsvToRgb(y.toFloat() / COLOR_PICKER_SIZE * 360f, 1f, 1f)
        )

        val hueCursorY = pickerY + (currentHue / 360f * COLOR_PICKER_SIZE).toInt()
        graphics.fill(hueBarX - 2, hueCursorY - 2, hueBarX + HUE_BAR_WIDTH + 2, hueCursorY + 2, 0xFFFFFFFF.toInt())

        val previewY = pickerY + COLOR_PICKER_SIZE + 10
        graphics.fill(pickerX, previewY, pickerX + 30, previewY + 20, hsvToRgb(currentHue, currentSaturation, currentValue))
        graphics.drawString(font, "Click to apply", pickerX + 35, previewY + 6, TEXT_DARK, false)
    }

    private fun drawStyleDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val type = dropdownType ?: return
        val typeIndex = WPType.entries.indexOf(type)

        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val rowY = contentTop + (typeIndex * ROW_HEIGHT) - scrollY
        val dropdownX = guiLeft + 140
        val dropdownY = rowY + 20
        val dropdownWidth = 120
        val itemHeight = 18

        graphics.fill(dropdownX - 1, dropdownY - 1, dropdownX + dropdownWidth + 1, dropdownY + (RenderStyle.entries.size * itemHeight) + 1, PEACH_DARK)

        for ((i, style) in RenderStyle.entries.withIndex()) {
            val itemY = dropdownY + (i * itemHeight)
            val isHover = mouseX in dropdownX..(dropdownX + dropdownWidth) && mouseY in itemY..(itemY + itemHeight)
            graphics.fill(dropdownX, itemY, dropdownX + dropdownWidth, itemY + itemHeight, if (isHover) brighten(PEACH_CREAM) else PEACH_CREAM)
            graphics.drawString(font, style.displayName, dropdownX + 5, itemY + 5, TEXT_DARK, false)
        }
    }

    private fun updateSv(mouseX: Int, mouseY: Int, pickerX: Int, pickerY: Int) {
        currentSaturation = ((mouseX - pickerX).toFloat() / COLOR_PICKER_SIZE).coerceIn(0f, 1f)
        currentValue = 1f - ((mouseY - pickerY).toFloat() / COLOR_PICKER_SIZE).coerceIn(0f, 1f)
    }

    private fun updateHue(mouseY: Int, pickerY: Int) {
        currentHue = ((mouseY - pickerY).toFloat() / COLOR_PICKER_SIZE * 360f).coerceIn(0f, 359.9f)
    }

    private fun applyColor() {
        val type = selectedType ?: return
        val rgb = hsvToRgb(currentHue, currentSaturation, currentValue)
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        val current = config.getNodeAppearance(type)
        NodeAppearanceSettings.setColor(type, r, g, b, current.color.a)
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val m = v - c

        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f

        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min

        val h = when {
            delta == 0f -> 0f
            max == rf -> 60f * (((gf - bf) / delta) % 6)
            max == gf -> 60f * (((bf - rf) / delta) + 2)
            else -> 60f * (((rf - gf) / delta) + 4)
        }.let { if (it < 0) it + 360 else it }

        val s = if (max == 0f) 0f else delta / max
        val v = max
        return Triple(h, s, v)
    }

    private fun brighten(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        var r = (color ushr 16) and 0xFF
        var g = (color ushr 8) and 0xFF
        var b = color and 0xFF
        r = minOf(255, r + 30)
        g = minOf(255, g + 30)
        b = minOf(255, b + 30)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun playClick() {
        Minecraft.getInstance().soundManager.play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
            )
        )
    }
}