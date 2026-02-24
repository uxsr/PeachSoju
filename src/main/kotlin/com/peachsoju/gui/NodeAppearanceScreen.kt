package com.peachsoju.gui

import com.peachsoju.config
import com.peachsoju.gui.components.ColorPickerPopup
import com.peachsoju.modules.impl.autoroutes.NodeAppearanceSettings
import com.peachsoju.modules.impl.autoroutes.RenderStyle
import com.peachsoju.modules.impl.autoroutes.data.WPType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.*

class NodeAppearanceScreen(private val parent: Screen?) : Screen(Component.literal("Node Appearance")) {

    companion object {
        private const val PEACH_DARK = 0xFF8B5A3C.toInt()
        private const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        private const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        private const val PEACH_CREAM = 0xFFFFF5E6.toInt()
        private const val PEACH_GLOW = 0xFFFFE4C4.toInt()

        private const val TEXT_DARK = 0xFF4A3728.toInt()
        private const val TEXT_LIGHT = 0xFFFFFFFF.toInt()

        private const val GUI_WIDTH = 320
        private const val GUI_HEIGHT = 300
        private const val PADDING = 10
        private const val ROW_HEIGHT = 32
        private const val HEADER_HEIGHT = 30
        private const val FOOTER_HEIGHT = 30
        private const val SCROLL_STEP = 14

        private const val SCREEN_OPEN_DURATION = 250f
        private const val SCREEN_CLOSE_DURATION = 150f
        private const val HOVER_LERP_SPEED = 0.18f
        private const val CARD_STAGGER_DELAY = 40f
    }

    private object Easing {
        fun easeOutQuad(t: Float): Float = 1f - (1f - t) * (1f - t)
        fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)
        fun easeOutBack(t: Float): Float {
            val c1 = 1.70158f
            val c3 = c1 + 1f
            return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
        }
    }

    private var guiLeft = 0
    private var guiTop = 0
    private var scrollY = 0
    private var maxScroll = 0

    private var screenOpenTime = 0L
    private var isClosing = false
    private var closeStartTime = 0L
    private var animatedScrollY = 0f

    private val hoverProgress = mutableMapOf<String, Float>()
    private val cardEntryProgress = mutableMapOf<Int, Float>()
    private var backButtonHover = 0f

    private var colorPicker: ColorPickerPopup? = null
    private var editingType: WPType? = null

    private var showStyleDropdown = false
    private var dropdownType: WPType? = null

    override fun init() {
        super.init()
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2

        if (screenOpenTime == 0L) {
            screenOpenTime = System.currentTimeMillis()
        }

        recomputeMaxScroll()
    }

    private fun recomputeMaxScroll() {
        val contentHeight = WPType.entries.size * ROW_HEIGHT
        val viewHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2)
        maxScroll = max(0, contentHeight - viewHeight)
        scrollY = scrollY.coerceIn(0, max(0, maxScroll))
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val currentTime = System.currentTimeMillis()
        val screenProgress: Float
        val screenAlpha: Float
        val slideOffset: Int

        if (isClosing) {
            val closeElapsed = (currentTime - closeStartTime).toFloat()
            val closeProgress = (closeElapsed / SCREEN_CLOSE_DURATION).coerceIn(0f, 1f)
            val easedClose = Easing.easeOutCubic(closeProgress)
            screenProgress = 1f
            screenAlpha = 1f - closeProgress
            slideOffset = (easedClose * 50).toInt()

            if (closeProgress >= 1f) {
                Minecraft.getInstance().setScreen(parent)
                return
            }
        } else {
            val openElapsed = (currentTime - screenOpenTime).toFloat()
            screenProgress = Easing.easeOutBack((openElapsed / SCREEN_OPEN_DURATION).coerceIn(0f, 1f))
            screenAlpha = Easing.easeOutQuad((openElapsed / (SCREEN_OPEN_DURATION * 0.5f)).coerceIn(0f, 1f))
            slideOffset = 0
        }

        animatedScrollY = lerp(animatedScrollY, scrollY.toFloat(), 0.25f)

        val bgAlpha = (screenAlpha * 0.67f * 255).toInt().coerceIn(0, 255)
        graphics.fill(0, 0, width, height, (bgAlpha shl 24))

        val originalGuiLeft = guiLeft
        guiLeft += slideOffset

        drawPanel(graphics, screenAlpha, screenProgress)
        drawHeader(graphics, mouseX, mouseY, screenAlpha)
        drawNodeTypeList(graphics, mouseX, mouseY, screenAlpha)
        drawFooter(graphics, screenAlpha)

        if (showStyleDropdown && dropdownType != null && !isClosing) {
            drawStyleDropdown(graphics, mouseX, mouseY)
        }

        guiLeft = originalGuiLeft

        colorPicker?.render(graphics, mouseX, mouseY)

        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawPanel(graphics: GuiGraphics, alpha: Float, progress: Float) {
        val animatedWidth = (GUI_WIDTH * progress).toInt()
        val animatedHeight = (GUI_HEIGHT * progress).toInt()
        val animatedLeft = guiLeft + (GUI_WIDTH - animatedWidth) / 2
        val animatedTop = guiTop + (GUI_HEIGHT - animatedHeight) / 2

        for (i in 1..3) {
            val glowAlpha = alpha * (0.12f / i)
            val glowColor = withAlpha(PEACH_MEDIUM, glowAlpha)
            graphics.fill(
                animatedLeft - 2 - i * 2, animatedTop - 2 - i * 2,
                animatedLeft + animatedWidth + 2 + i * 2, animatedTop + animatedHeight + 2 + i * 2,
                glowColor
            )
        }

        graphics.fill(animatedLeft - 2, animatedTop - 2, animatedLeft + animatedWidth + 2, animatedTop + animatedHeight + 2, withAlpha(PEACH_DARK, alpha))
        graphics.fill(animatedLeft, animatedTop, animatedLeft + animatedWidth, animatedTop + animatedHeight, withAlpha(PEACH_LIGHT, alpha))
        graphics.fill(
            guiLeft + PADDING, guiTop + HEADER_HEIGHT + PADDING,
            guiLeft + GUI_WIDTH - PADDING, guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING,
            withAlpha(PEACH_CREAM, alpha)
        )
    }

    private fun drawHeader(graphics: GuiGraphics, mouseX: Int, mouseY: Int, alpha: Float) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_HEIGHT, withAlpha(PEACH_MEDIUM, alpha))

        val backText = "← Back"
        val backWidth = font.width(backText) + 10
        val backX = guiLeft + 5
        val backY = guiTop + 5
        val backH = 20

        val backHover = mouseX >= backX && mouseX <= backX + backWidth &&
                mouseY >= backY && mouseY <= backY + backH && !isClosing && colorPicker == null

        val targetBackHover = if (backHover) 1f else 0f
        backButtonHover = lerp(backButtonHover, targetBackHover, HOVER_LERP_SPEED)

        val backColor = lerpColor(PEACH_DARK, brighten(PEACH_DARK, 40), backButtonHover)
        graphics.fill(backX, backY, backX + backWidth, backY + backH, withAlpha(backColor, alpha))
        graphics.drawString(font, backText, backX + 5, backY + 6, withAlpha(TEXT_LIGHT, alpha), false)

        val title = "§l✿ Node Appearance ✿"
        graphics.drawString(font, title, guiLeft + (GUI_WIDTH - font.width(title)) / 2, guiTop + 10, withAlpha(TEXT_DARK, alpha), false)
    }

    private fun drawNodeTypeList(graphics: GuiGraphics, mouseX: Int, mouseY: Int, alpha: Float) {
        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentRight = guiLeft + GUI_WIDTH - PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING

        graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom)

        val startY = contentTop - animatedScrollY.toInt()
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - screenOpenTime).toFloat()

        for ((i, type) in WPType.entries.withIndex()) {
            val rowY = startY + (i * ROW_HEIGHT)

            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) continue

            val entryDelay = i * CARD_STAGGER_DELAY
            val entryProgress = ((elapsed - entryDelay - 100f) / 200f).coerceIn(0f, 1f)
            val easedEntry = Easing.easeOutCubic(entryProgress)
            cardEntryProgress[i] = easedEntry

            val slideOffset = ((1f - easedEntry) * 30).toInt()
            val elementAlpha = alpha * easedEntry

            drawNodeTypeRow(graphics, type, i, contentLeft + slideOffset, rowY, contentRight - contentLeft, mouseX, mouseY, elementAlpha, contentTop, contentBottom)
        }

        graphics.disableScissor()

        if (maxScroll > 0) {
            val scrollbarX = contentRight - 6
            val scrollbarTop = contentTop + 2
            val scrollbarBottom = contentBottom - 2
            val scrollbarHeight = scrollbarBottom - scrollbarTop

            val thumbHeight = max(16, (scrollbarHeight * (scrollbarHeight.toFloat() / (scrollbarHeight + maxScroll))).toInt())
            val thumbY = scrollbarTop + ((scrollY.toFloat() / maxScroll) * (scrollbarHeight - thumbHeight)).toInt()

            graphics.fill(scrollbarX, scrollbarTop, scrollbarX + 4, scrollbarBottom, withAlpha(PEACH_DARK, alpha * 0.3f))
            graphics.fill(scrollbarX + 1, thumbY, scrollbarX + 3, thumbY + thumbHeight, withAlpha(PEACH_MEDIUM, alpha))
        }
    }

    private fun drawNodeTypeRow(
        graphics: GuiGraphics,
        type: WPType,
        index: Int,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        alpha: Float,
        contentTop: Int, contentBottom: Int
    ) {
        val appearance = config.getNodeAppearance(type)

        val hoverKey = "row_$index"
        val hover = mouseX in x..(x + width) && mouseY in y..(y + ROW_HEIGHT) &&
                mouseY in contentTop..contentBottom && !isClosing && colorPicker == null
        val targetHover = if (hover) 1f else 0f
        val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
        val newHover = lerp(currentHover, targetHover, HOVER_LERP_SPEED)
        hoverProgress[hoverKey] = newHover

        val bgColor = lerpColor(PEACH_CREAM, PEACH_GLOW, newHover)
        val yOffset = (-newHover * 1).toInt()

        graphics.fill(x, y + yOffset, x + width, y + ROW_HEIGHT + yOffset - 2, withAlpha(bgColor, alpha))

        val typeName = type.name.lowercase().replaceFirstChar { it.uppercase() }
        graphics.drawString(font, typeName, x + 5, y + 10 + yOffset, withAlpha(TEXT_DARK, alpha), false)

        val colorX = x + 90
        val colorY = y + 6 + yOffset
        val colorSize = 18

        val colorHoverKey = "color_$index"
        val colorHover = mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize) &&
                mouseY in contentTop..contentBottom && !isClosing && colorPicker == null
        val targetColorHover = if (colorHover) 1f else 0f
        val currentColorHover = hoverProgress.getOrDefault(colorHoverKey, 0f)
        val newColorHover = lerp(currentColorHover, targetColorHover, HOVER_LERP_SPEED)
        hoverProgress[colorHoverKey] = newColorHover

        val colorBorderColor = lerpColor(PEACH_DARK, brighten(PEACH_DARK, 30), newColorHover)
        graphics.fill(colorX - 1, colorY - 1, colorX + colorSize + 1, colorY + colorSize + 1, withAlpha(colorBorderColor, alpha))

        val colorInt = (0xFF shl 24) or (appearance.color.r shl 16) or (appearance.color.g shl 8) or appearance.color.b
        graphics.fill(colorX, colorY, colorX + colorSize, colorY + colorSize, withAlpha(colorInt, alpha))

        if (newColorHover > 0.01f) {
            graphics.fill(colorX, colorY, colorX + colorSize, colorY + colorSize, withAlpha(0x40FFFFFF, alpha * newColorHover))
        }

        val styleX = x + 125
        val styleWidth = width - 135
        val styleHeight = 18

        val styleHoverKey = "style_$index"
        val styleHover = mouseX in styleX..(styleX + styleWidth) && mouseY in colorY..(colorY + styleHeight) &&
                mouseY in contentTop..contentBottom && !isClosing && colorPicker == null
        val targetStyleHover = if (styleHover) 1f else 0f
        val currentStyleHover = hoverProgress.getOrDefault(styleHoverKey, 0f)
        val newStyleHover = lerp(currentStyleHover, targetStyleHover, HOVER_LERP_SPEED)
        hoverProgress[styleHoverKey] = newStyleHover

        val styleBg = lerpColor(PEACH_MEDIUM, brighten(PEACH_MEDIUM, 25), newStyleHover)
        graphics.fill(styleX - 1, colorY - 1, styleX + styleWidth + 1, colorY + styleHeight + 1, withAlpha(PEACH_DARK, alpha))
        graphics.fill(styleX, colorY, styleX + styleWidth, colorY + styleHeight, withAlpha(styleBg, alpha))

        val styleText = RenderStyle.fromString(appearance.style).displayName
        graphics.drawString(font, styleText, styleX + 5, colorY + 5, withAlpha(TEXT_LIGHT, alpha), false)
        graphics.drawString(font, "▼", styleX + styleWidth - 12, colorY + 5, withAlpha(TEXT_LIGHT, alpha), false)
    }

    private fun drawStyleDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val type = dropdownType ?: return
        val typeIndex = WPType.entries.indexOf(type)

        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val rowY = contentTop + (typeIndex * ROW_HEIGHT) - animatedScrollY.toInt()
        val dropdownX = guiLeft + PADDING + 125
        val dropdownY = rowY + 24
        val dropdownWidth = GUI_WIDTH - PADDING * 2 - 135
        val itemHeight = 20

        graphics.fill(dropdownX + 2, dropdownY + 2, dropdownX + dropdownWidth + 2, dropdownY + (RenderStyle.entries.size * itemHeight) + 2, 0x40000000)

        graphics.fill(dropdownX - 1, dropdownY - 1, dropdownX + dropdownWidth + 1, dropdownY + (RenderStyle.entries.size * itemHeight) + 1, PEACH_DARK)

        for ((i, style) in RenderStyle.entries.withIndex()) {
            val itemY = dropdownY + (i * itemHeight)
            val isHover = mouseX in dropdownX..(dropdownX + dropdownWidth) && mouseY in itemY..(itemY + itemHeight)

            val hoverKey = "dropdown_$i"
            val targetHover = if (isHover) 1f else 0f
            val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
            val newHover = lerp(currentHover, targetHover, 0.25f)
            hoverProgress[hoverKey] = newHover

            val itemBg = lerpColor(PEACH_CREAM, PEACH_GLOW, newHover)
            graphics.fill(dropdownX, itemY, dropdownX + dropdownWidth, itemY + itemHeight, itemBg)
            graphics.drawString(font, style.displayName, dropdownX + 5, itemY + 6, TEXT_DARK, false)
        }
    }

    private fun drawFooter(graphics: GuiGraphics, alpha: Float) {
        val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT
        graphics.fill(guiLeft, footerY, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, withAlpha(PEACH_LIGHT, alpha))
        graphics.fill(guiLeft + PADDING, footerY + 2, guiLeft + GUI_WIDTH - PADDING, footerY + 3, withAlpha(PEACH_MEDIUM, alpha))

        val footerText = "§7Click color to edit • Click dropdown for style"
        graphics.drawString(font, footerText, guiLeft + (GUI_WIDTH - font.width(footerText)) / 2, footerY + 12, withAlpha(TEXT_DARK, alpha), false)
    }

    override fun mouseClicked(event: MouseButtonEvent, consumed: Boolean): Boolean {
        if (isClosing) return false

        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        if (event.button() != 0) return super.mouseClicked(event, consumed)

        colorPicker?.let { picker ->
            if (picker.isInside(mouseX, mouseY)) {
                picker.mouseClicked(mouseX, mouseY, 0)
                return true
            } else {
                colorPicker = null
                editingType = null
                return true
            }
        }

        val backText = "← Back"
        val backWidth = font.width(backText) + 10
        val backX = guiLeft + 5
        val backY = guiTop + 5
        val backH = 20
        if (mouseX >= backX && mouseX <= backX + backWidth && mouseY >= backY && mouseY <= backY + backH) {
            playClick()
            startClosing()
            return true
        }

        if (showStyleDropdown && dropdownType != null) {
            val typeIndex = WPType.entries.indexOf(dropdownType)
            val contentTop = guiTop + HEADER_HEIGHT + PADDING
            val rowY = contentTop + (typeIndex * ROW_HEIGHT) - animatedScrollY.toInt()
            val dropdownX = guiLeft + PADDING + 125
            val dropdownY = rowY + 24
            val dropdownWidth = GUI_WIDTH - PADDING * 2 - 135
            val itemHeight = 20

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

        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING
        val contentWidth = GUI_WIDTH - PADDING * 2

        if (mouseY < contentTop || mouseY > contentBottom) {
            return super.mouseClicked(event, consumed)
        }

        val startY = contentTop - animatedScrollY.toInt()
        for ((i, type) in WPType.entries.withIndex()) {
            val rowY = startY + (i * ROW_HEIGHT)

            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) continue

            val colorX = contentLeft + 90
            val colorY = rowY + 6
            val colorSize = 18

            if (mouseX in colorX..(colorX + colorSize) && mouseY in colorY..(colorY + colorSize)) {
                openColorPicker(type)
                playClick()
                return true
            }

            val styleX = contentLeft + 125
            val styleWidth = contentWidth - 135
            val styleHeight = 18

            if (mouseX in styleX..(styleX + styleWidth) && mouseY in colorY..(colorY + styleHeight)) {
                dropdownType = type
                showStyleDropdown = true
                playClick()
                return true
            }
        }

        return super.mouseClicked(event, consumed)
    }

    private fun openColorPicker(type: WPType) {
        val appearance = config.getNodeAppearance(type)
        editingType = type
        showStyleDropdown = false

        colorPicker = ColorPickerPopup(
            appearance.color.r,
            appearance.color.g,
            appearance.color.b,
            appearance.color.a,
            onApply = { r, g, b, a ->
                NodeAppearanceSettings.setColor(type, r, g, b, a)
                colorPicker = null
                editingType = null
            },
            onCancel = {
                colorPicker = null
                editingType = null
            }
        ).apply {
            x = guiLeft + GUI_WIDTH + 10
            y = guiTop + 50
        }
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        colorPicker?.let {
            if (it.mouseDragged(event.x().toInt(), event.y().toInt())) {
                return true
            }
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        colorPicker?.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (showStyleDropdown || colorPicker != null) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        if (maxScroll > 0) {
            scrollY = (scrollY - (verticalAmount.toInt() * SCROLL_STEP)).coerceIn(0, maxScroll)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        colorPicker?.let {
            if (it.keyPressed(event.key())) {
                return true
            }
        }

        if (event.key() != 256) return super.keyPressed(event)

        if (colorPicker != null) {
            colorPicker = null
            editingType = null
            return true
        }
        if (showStyleDropdown) {
            showStyleDropdown = false
            dropdownType = null
            return true
        }
        startClosing()
        return true
    }

    override fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        colorPicker?.let {
            if (it.charTyped(event.codepoint().toChar())) {
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

    private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t

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

    private fun brighten(color: Int, amount: Int = 30): Int {
        val a = (color ushr 24) and 0xFF
        var r = (color ushr 16) and 0xFF
        var g = (color ushr 8) and 0xFF
        var b = color and 0xFF
        r = min(255, r + amount)
        g = min(255, g + amount)
        b = min(255, b + amount)
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