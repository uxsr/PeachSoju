package com.peachsoju.modules.impl.misc.customitems

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import kotlin.math.abs

class ColorPickerPopup(
    private val initialR: Int,
    private val initialG: Int,
    private val initialB: Int,
    private val initialA: Float,
    private val onApply: (r: Int, g: Int, b: Int, a: Float) -> Unit,
    private val onCancel: () -> Unit
) {
    companion object {
        private const val PICKER_SIZE = 100
        private const val HUE_BAR_WIDTH = 12
        private const val ALPHA_BAR_HEIGHT = 12
        private const val POPUP_PADDING = 8
        private const val HEX_FIELD_HEIGHT = 18

        private const val BG_COLOR = 0xFF2A2A2A.toInt()
        private const val BORDER_COLOR = 0xFF8B5A3C.toInt()
        private const val TEXT_COLOR = 0xFFFFFFFF.toInt()
        private const val FIELD_BG = 0xFF3A3A3A.toInt()
        private const val BUTTON_COLOR = 0xFF8B5A3C.toInt()
        private const val BUTTON_HOVER = 0xFFD4956A.toInt()
    }

    private var currentHue: Float
    private var currentSat: Float
    private var currentVal: Float
    private var currentAlpha: Float

    private var hexText: String
    private var hexFieldActive = false
    private var hexCursorBlink = 0L

    private var draggingSV = false
    private var draggingHue = false
    private var draggingAlpha = false

    private var cachedSvGradient: IntArray? = null
    private var cachedHue: Float = -1f
    private var cachedHueGradient: IntArray? = null

    var x = 0
    var y = 0
    val width = PICKER_SIZE + HUE_BAR_WIDTH + POPUP_PADDING * 3
    val height = PICKER_SIZE + ALPHA_BAR_HEIGHT + HEX_FIELD_HEIGHT + 30 + POPUP_PADDING * 5

    init {
        val (h, s, v) = rgbToHsv(initialR, initialG, initialB)
        currentHue = h
        currentSat = s
        currentVal = v
        currentAlpha = initialA
        hexText = rgbToHex(initialR, initialG, initialB)
    }

    fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val font = Minecraft.getInstance().font

        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, BORDER_COLOR)
        graphics.fill(x, y, x + width, y + height, BG_COLOR)

        val pickerX = x + POPUP_PADDING
        val pickerY = y + POPUP_PADDING

        drawSvPicker(graphics, pickerX, pickerY)

        val svCursorX = pickerX + (currentSat * PICKER_SIZE).toInt()
        val svCursorY = pickerY + ((1f - currentVal) * PICKER_SIZE).toInt()
        drawCursor(graphics, svCursorX, svCursorY)

        val hueBarX = pickerX + PICKER_SIZE + POPUP_PADDING
        drawHueBar(graphics, hueBarX, pickerY)

        val hueCursorY = pickerY + (currentHue / 360f * PICKER_SIZE).toInt()
        graphics.fill(hueBarX - 1, hueCursorY - 2, hueBarX + HUE_BAR_WIDTH + 1, hueCursorY + 2, TEXT_COLOR)

        val alphaY = pickerY + PICKER_SIZE + POPUP_PADDING
        drawAlphaBar(graphics, pickerX, alphaY, PICKER_SIZE + POPUP_PADDING + HUE_BAR_WIDTH)

        val alphaCursorX = pickerX + (currentAlpha * (PICKER_SIZE + POPUP_PADDING + HUE_BAR_WIDTH)).toInt()
        graphics.fill(alphaCursorX - 2, alphaY - 1, alphaCursorX + 2, alphaY + ALPHA_BAR_HEIGHT + 1, TEXT_COLOR)

        val hexY = alphaY + ALPHA_BAR_HEIGHT + POPUP_PADDING
        val hexFieldWidth = 70
        val fieldBg = if (hexFieldActive) 0xFF4A4A4A.toInt() else FIELD_BG
        graphics.fill(pickerX - 1, hexY - 1, pickerX + hexFieldWidth + 1, hexY + HEX_FIELD_HEIGHT + 1, BORDER_COLOR)
        graphics.fill(pickerX, hexY, pickerX + hexFieldWidth, hexY + HEX_FIELD_HEIGHT, fieldBg)

        val cursorVisible = hexFieldActive && ((System.currentTimeMillis() / 500) % 2 == 0L)
        val displayHex = "#$hexText${if (cursorVisible) "|" else ""}"
        graphics.drawString(font, displayHex, pickerX + 4, hexY + 5, TEXT_COLOR, false)

        val previewX = pickerX + PICKER_SIZE + POPUP_PADDING + HUE_BAR_WIDTH - 35
        val (r, g, b) = hsvToRgbValues(currentHue, currentSat, currentVal)
        val previewColor = ((currentAlpha * 255).toInt() shl 24) or (r shl 16) or (g shl 8) or b

        graphics.fill(previewX - 1, hexY - 1, previewX + 36, hexY + HEX_FIELD_HEIGHT + 1, BORDER_COLOR)
        drawCheckerboard(graphics, previewX, hexY, 35, HEX_FIELD_HEIGHT)
        graphics.fill(previewX, hexY, previewX + 35, hexY + HEX_FIELD_HEIGHT, previewColor)

        val alphaPercent = "${(currentAlpha * 100).toInt()}%"
        val alphaTextX = previewX + (35 - font.width(alphaPercent)) / 2
        val alphaTextY = hexY + (HEX_FIELD_HEIGHT - font.lineHeight) / 2
        graphics.drawString(font, alphaPercent, alphaTextX, alphaTextY, TEXT_COLOR, true)

        val buttonY = hexY + HEX_FIELD_HEIGHT + POPUP_PADDING
        val buttonWidth = 50
        val buttonHeight = 20

        val applyHover = mouseX in pickerX..(pickerX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight)
        val applyColor = if (applyHover) BUTTON_HOVER else BUTTON_COLOR
        graphics.fill(pickerX, buttonY, pickerX + buttonWidth, buttonY + buttonHeight, applyColor)
        graphics.drawString(font, "Apply", pickerX + 10, buttonY + 6, TEXT_COLOR, false)

        val cancelX = pickerX + buttonWidth + 10
        val cancelHover = mouseX in cancelX..(cancelX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight)
        val cancelColor = if (cancelHover) 0xFF666666.toInt() else 0xFF444444.toInt()
        graphics.fill(cancelX, buttonY, cancelX + buttonWidth, buttonY + buttonHeight, cancelColor)
        graphics.drawString(font, "Cancel", cancelX + 7, buttonY + 6, TEXT_COLOR, false)
    }

    private fun drawSvPicker(graphics: GuiGraphics, px: Int, py: Int) {
        if (cachedSvGradient == null || cachedHue != currentHue) {
            cachedSvGradient = IntArray(PICKER_SIZE * PICKER_SIZE)
            for (y in 0 until PICKER_SIZE) {
                for (x in 0 until PICKER_SIZE) {
                    val s = x.toFloat() / PICKER_SIZE
                    val v = 1f - (y.toFloat() / PICKER_SIZE)
                    cachedSvGradient!![y * PICKER_SIZE + x] = hsvToRgb(currentHue, s, v)
                }
            }
            cachedHue = currentHue
        }

        val chunkSize = 5
        for (cy in 0 until PICKER_SIZE step chunkSize) {
            for (cx in 0 until PICKER_SIZE step chunkSize) {
                val color = cachedSvGradient!![cy * PICKER_SIZE + cx]
                graphics.fill(px + cx, py + cy, px + cx + chunkSize, py + cy + chunkSize, color)
            }
        }
    }

    private fun drawHueBar(graphics: GuiGraphics, hx: Int, hy: Int) {
        if (cachedHueGradient == null) {
            cachedHueGradient = IntArray(PICKER_SIZE)
            for (y in 0 until PICKER_SIZE) {
                val hue = y.toFloat() / PICKER_SIZE * 360f
                cachedHueGradient!![y] = hsvToRgb(hue, 1f, 1f)
            }
        }

        val chunkSize = 4
        for (y in 0 until PICKER_SIZE step chunkSize) {
            val color = cachedHueGradient!![y]
            graphics.fill(hx, hy + y, hx + HUE_BAR_WIDTH, hy + y + chunkSize, color)
        }
    }

    private fun drawAlphaBar(graphics: GuiGraphics, ax: Int, ay: Int, width: Int) {
        drawCheckerboard(graphics, ax, ay, width, ALPHA_BAR_HEIGHT)

        val (r, g, b) = hsvToRgbValues(currentHue, currentSat, currentVal)
        val chunkSize = 8
        for (x in 0 until width step chunkSize) {
            val alpha = x.toFloat() / width
            val a = (alpha * 255).toInt()
            val color = (a shl 24) or (r shl 16) or (g shl 8) or b
            graphics.fill(ax + x, ay, ax + x + chunkSize, ay + ALPHA_BAR_HEIGHT, color)
        }
    }

    private fun drawCheckerboard(graphics: GuiGraphics, cx: Int, cy: Int, w: Int, h: Int) {
        val checkSize = 4
        for (y in 0 until h step checkSize) {
            for (x in 0 until w step checkSize) {
                val isLight = ((x / checkSize) + (y / checkSize)) % 2 == 0
                val color = if (isLight) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                graphics.fill(cx + x, cy + y, (cx + x + checkSize).coerceAtMost(cx + w), (cy + y + checkSize).coerceAtMost(cy + h), color)
            }
        }
    }

    private fun drawCursor(graphics: GuiGraphics, cx: Int, cy: Int) {
        graphics.fill(cx - 4, cy - 1, cx + 4, cy + 1, 0xFFFFFFFF.toInt())
        graphics.fill(cx - 1, cy - 4, cx + 1, cy + 4, 0xFFFFFFFF.toInt())
        graphics.fill(cx - 3, cy, cx + 3, cy, 0xFF000000.toInt())
        graphics.fill(cx, cy - 3, cx, cy + 3, 0xFF000000.toInt())
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, button: Int): Boolean {
        if (button != 0) return false

        val pickerX = x + POPUP_PADDING
        val pickerY = y + POPUP_PADDING
        val hueBarX = pickerX + PICKER_SIZE + POPUP_PADDING
        val alphaY = pickerY + PICKER_SIZE + POPUP_PADDING
        val hexY = alphaY + ALPHA_BAR_HEIGHT + POPUP_PADDING

        if (mouseX in pickerX until (pickerX + PICKER_SIZE) && mouseY in pickerY until (pickerY + PICKER_SIZE)) {
            draggingSV = true
            updateSV(mouseX, mouseY, pickerX, pickerY)
            hexFieldActive = false
            return true
        }

        if (mouseX in hueBarX until (hueBarX + HUE_BAR_WIDTH) && mouseY in pickerY until (pickerY + PICKER_SIZE)) {
            draggingHue = true
            updateHue(mouseY, pickerY)
            hexFieldActive = false
            return true
        }

        val alphaBarWidth = PICKER_SIZE + POPUP_PADDING + HUE_BAR_WIDTH
        if (mouseX in pickerX until (pickerX + alphaBarWidth) && mouseY in alphaY until (alphaY + ALPHA_BAR_HEIGHT)) {
            draggingAlpha = true
            updateAlpha(mouseX, pickerX, alphaBarWidth)
            hexFieldActive = false
            return true
        }

        val hexFieldWidth = 70
        if (mouseX in pickerX until (pickerX + hexFieldWidth) && mouseY in hexY until (hexY + HEX_FIELD_HEIGHT)) {
            hexFieldActive = true
            return true
        }

        val buttonY = hexY + HEX_FIELD_HEIGHT + POPUP_PADDING
        val buttonWidth = 50
        if (mouseX in pickerX until (pickerX + buttonWidth) && mouseY in buttonY until (buttonY + 20)) {
            val (r, g, b) = hsvToRgbValues(currentHue, currentSat, currentVal)
            onApply(r, g, b, currentAlpha)
            return true
        }

        val cancelX = pickerX + buttonWidth + 10
        if (mouseX in cancelX until (cancelX + buttonWidth) && mouseY in buttonY until (buttonY + 20)) {
            onCancel()
            return true
        }

        hexFieldActive = false
        return false
    }

    fun mouseDragged(mouseX: Int, mouseY: Int): Boolean {
        val pickerX = x + POPUP_PADDING
        val pickerY = y + POPUP_PADDING

        if (draggingSV) {
            updateSV(mouseX, mouseY, pickerX, pickerY)
            return true
        }
        if (draggingHue) {
            updateHue(mouseY, pickerY)
            return true
        }
        if (draggingAlpha) {
            val alphaBarWidth = PICKER_SIZE + POPUP_PADDING + HUE_BAR_WIDTH
            updateAlpha(mouseX, pickerX, alphaBarWidth)
            return true
        }
        return false
    }

    fun mouseReleased(): Boolean {
        val wasDragging = draggingSV || draggingHue || draggingAlpha
        draggingSV = false
        draggingHue = false
        draggingAlpha = false
        return wasDragging
    }

    fun keyPressed(keyCode: Int): Boolean {
        if (!hexFieldActive) return false

        when (keyCode) {
            259 -> {
                if (hexText.isNotEmpty()) {
                    hexText = hexText.dropLast(1)
                    tryApplyHex()
                }
                return true
            }
            257, 335 -> {
                tryApplyHex()
                hexFieldActive = false
                return true
            }
            256 -> {
                hexFieldActive = false
                return true
            }
        }
        return true
    }

    fun charTyped(char: Char): Boolean {
        if (!hexFieldActive) return false

        if (hexText.length < 6 && char.isHexChar()) {
            hexText += char.uppercaseChar()
            tryApplyHex()
            return true
        }
        return true
    }

    private fun Char.isHexChar(): Boolean = this in '0'..'9' || this.uppercaseChar() in 'A'..'F'

    private fun tryApplyHex() {
        if (hexText.length == 6) {
            try {
                val r = hexText.substring(0, 2).toInt(16)
                val g = hexText.substring(2, 4).toInt(16)
                val b = hexText.substring(4, 6).toInt(16)
                val (h, s, v) = rgbToHsv(r, g, b)
                currentHue = h
                currentSat = s
                currentVal = v
                cachedSvGradient = null
            } catch (e: Exception) {
            }
        }
    }

    private fun updateSV(mouseX: Int, mouseY: Int, pickerX: Int, pickerY: Int) {
        currentSat = ((mouseX - pickerX).toFloat() / PICKER_SIZE).coerceIn(0f, 1f)
        currentVal = 1f - ((mouseY - pickerY).toFloat() / PICKER_SIZE).coerceIn(0f, 1f)
        updateHexFromHsv()
    }

    private fun updateHue(mouseY: Int, pickerY: Int) {
        currentHue = ((mouseY - pickerY).toFloat() / PICKER_SIZE * 360f).coerceIn(0f, 359.9f)
        cachedSvGradient = null
        updateHexFromHsv()
    }

    private fun updateAlpha(mouseX: Int, alphaX: Int, alphaWidth: Int) {
        currentAlpha = ((mouseX - alphaX).toFloat() / alphaWidth).coerceIn(0f, 1f)
    }

    private fun updateHexFromHsv() {
        val (r, g, b) = hsvToRgbValues(currentHue, currentSat, currentVal)
        hexText = rgbToHex(r, g, b)
    }

    fun isInside(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in x..(x + width) && mouseY in y..(y + height)
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val (r, g, b) = hsvToRgbValues(h, s, v)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun hsvToRgbValues(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
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
        return Triple(r, g, b)
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
        return Triple(h, s, max)
    }

    private fun rgbToHex(r: Int, g: Int, b: Int): String {
        return String.format("%02X%02X%02X", r, g, b)
    }
}