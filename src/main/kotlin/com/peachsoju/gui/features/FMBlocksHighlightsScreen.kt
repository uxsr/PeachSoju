package com.peachsoju.gui.features

import com.peachsoju.modules.impl.misc.customitems.ColorPickerPopup
import com.peachsoju.modules.impl.dungeon.fmblocks.FMBlocksHighlights
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import kotlin.math.*

class FMBlocksHighlightsScreen(parent: Screen?) : Screen(Component.literal("Block Highlights")) {

    companion object {
        private const val PEACH_DARK = 0xFF8B5A3C.toInt()
        private const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        private const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        private const val PEACH_CREAM = 0xFFFFF5E6.toInt()
        private const val PEACH_GLOW = 0xFFFFE4C4.toInt()

        private const val TEXT_DARK = 0xFF4A3728.toInt()
        private const val TEXT_LIGHT = 0xFFFFFFFF.toInt()
        private const val TEXT_GRAY = 0xFF888888.toInt()

        private const val TOGGLE_ON = 0xFF7CB342.toInt()
        private const val TOGGLE_OFF = 0xFFB85C5C.toInt()
        private const val ETHER_ON = 0xFF5599FF.toInt()
        private const val ETHER_OFF = 0xFF445566.toInt()
        private const val ESP_ON = 0xFF9955FF.toInt()
        private const val ESP_OFF = 0xFF554466.toInt()

        private const val GUI_WIDTH = 320
        private const val GUI_HEIGHT = 300
        private const val PADDING = 10
        private const val ROW_HEIGHT = 28
        private const val HEADER_HEIGHT = 30
        private const val FOOTER_HEIGHT = 40
        private const val SCROLL_STEP = 14

        private const val SCREEN_OPEN_DURATION = 250f
        private const val SCREEN_CLOSE_DURATION = 150f
        private const val HOVER_LERP_SPEED = 0.18f
        private const val CARD_STAGGER_DELAY = 35f
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

    private val parentScreen = parent

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
    private var editingItemId: String? = null

    private var searchText = ""
    private var searchActive = false
    private var filteredItems: List<Item> = emptyList()
    private var dropdownOpen = false
    private var dropdownScroll = 0

    override fun init() {
        super.init()
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2

        if (screenOpenTime == 0L) {
            screenOpenTime = System.currentTimeMillis()
        }

        recomputeMaxScroll()
        updateFilteredItems()
    }

    private fun recomputeMaxScroll() {
        val highlights = FMBlocksHighlights.getAllHighlights()
        val contentHeight = (highlights.size + 1) * ROW_HEIGHT
        val viewHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2)
        maxScroll = max(0, contentHeight - viewHeight)
        scrollY = scrollY.coerceIn(0, max(0, maxScroll))
    }

    private fun updateFilteredItems() {
        val searchLower = searchText.lowercase()
        val existingIds = FMBlocksHighlights.getAllHighlights().map { it.itemId }.toSet()

        filteredItems = if (searchLower.isEmpty()) {
            BuiltInRegistries.ITEM.filter {
                val id = BuiltInRegistries.ITEM.getKey(it).toString()
                id !in existingIds && it != Items.AIR
            }.take(50)
        } else {
            BuiltInRegistries.ITEM.filter { item ->
                val id = BuiltInRegistries.ITEM.getKey(item).toString()
                val cleanId = id.removePrefix("minecraft:").replace("_", " ")
                id !in existingIds && item != Items.AIR && cleanId.contains(searchLower)
            }.take(30)
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        recomputeMaxScroll()

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
                Minecraft.getInstance().setScreen(parentScreen)
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
        drawHighlightsList(graphics, mouseX, mouseY, screenAlpha)
        drawFooter(graphics, mouseX, mouseY, screenAlpha)

        if (dropdownOpen && !isClosing) {
            drawItemDropdown(graphics, mouseX, mouseY)
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
                mouseY >= backY && mouseY <= backY + backH && !isClosing

        val targetBackHover = if (backHover) 1f else 0f
        backButtonHover = lerp(backButtonHover, targetBackHover, HOVER_LERP_SPEED)

        val backColor = lerpColor(PEACH_DARK, brighten(PEACH_DARK, 40), backButtonHover)
        graphics.fill(backX, backY, backX + backWidth, backY + backH, withAlpha(backColor, alpha))
        graphics.drawString(font, backText, backX + 5, backY + 6, withAlpha(TEXT_LIGHT, alpha), false)

        val title = "§lBlock Highlights"
        graphics.drawString(font, title, guiLeft + (GUI_WIDTH - font.width(title)) / 2, guiTop + 10, withAlpha(TEXT_DARK, alpha), false)
    }

    private fun drawHighlightsList(graphics: GuiGraphics, mouseX: Int, mouseY: Int, alpha: Float) {
        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentRight = guiLeft + GUI_WIDTH - PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING

        graphics.enableScissor(contentLeft, contentTop, contentRight, contentBottom)

        val highlights = FMBlocksHighlights.getAllHighlights()
        var y = contentTop - animatedScrollY.toInt()
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - screenOpenTime).toFloat()

        for ((index, highlight) in highlights.withIndex()) {
            val entryDelay = index * CARD_STAGGER_DELAY
            val entryProgress = ((elapsed - entryDelay - 100f) / 200f).coerceIn(0f, 1f)
            val easedEntry = Easing.easeOutCubic(entryProgress)
            cardEntryProgress[index] = easedEntry

            val slideOffset = ((1f - easedEntry) * 30).toInt()
            val elementAlpha = alpha * easedEntry

            if (y + ROW_HEIGHT >= contentTop && y < contentBottom) {
                drawHighlightRow(graphics, highlight, index, contentLeft + slideOffset, y, contentRight - contentLeft, mouseX, mouseY, elementAlpha)
            }
            y += ROW_HEIGHT
        }

        graphics.disableScissor()
    }

    private fun drawHighlightRow(
        graphics: GuiGraphics,
        highlight: FMBlocksHighlights.BlockHighlight,
        index: Int,
        x: Int, y: Int, width: Int,
        mouseX: Int, mouseY: Int,
        alpha: Float
    ) {
        val hoverKey = "row_$index"
        val hover = mouseX in x..(x + width) && mouseY in y..(y + ROW_HEIGHT) && !isClosing && colorPicker == null
        val targetHover = if (hover) 1f else 0f
        val currentHover = hoverProgress.getOrDefault(hoverKey, 0f)
        val newHover = lerp(currentHover, targetHover, HOVER_LERP_SPEED)
        hoverProgress[hoverKey] = newHover

        val bgColor = lerpColor(PEACH_CREAM, PEACH_GLOW, newHover)
        val yOffset = (-newHover * 1).toInt()

        graphics.fill(x, y + yOffset, x + width, y + ROW_HEIGHT + yOffset - 2, withAlpha(bgColor, alpha))

        val toggleX = x + 5
        val toggleY = y + 6 + yOffset
        val toggleW = 30
        val toggleH = 14

        val toggleColor = if (highlight.enabled) TOGGLE_ON else TOGGLE_OFF
        graphics.fill(toggleX - 1, toggleY - 1, toggleX + toggleW + 1, toggleY + toggleH + 1, withAlpha(PEACH_DARK, alpha))
        graphics.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, withAlpha(toggleColor, alpha))

        val knobX = toggleX + 2 + (if (highlight.enabled) toggleW - 14 else 0)
        graphics.fill(knobX, toggleY + 2, knobX + 10, toggleY + toggleH - 2, withAlpha(0xFFFFFFFF.toInt(), alpha))

        val item = highlight.getItem()
        val itemName = item?.let {
            BuiltInRegistries.ITEM.getKey(it).toString().removePrefix("minecraft:").replace("_", " ")
        } ?: highlight.itemId
        val truncatedName = if (itemName.length > 18) itemName.take(15) + "..." else itemName
        graphics.drawString(font, truncatedName, toggleX + toggleW + 10, y + 9 + yOffset, withAlpha(TEXT_DARK, alpha), false)

        val deleteX = x + width - 35
        val deleteY = y + 5 + yOffset
        val deleteW = 25
        val deleteH = 16

        val espBtnX = x + width - 63
        val espBtnY = y + 5 + yOffset
        val espBtnW = 25
        val espBtnH = 16

        val etherBtnX = x + width - 91
        val etherBtnY = y + 5 + yOffset
        val etherBtnW = 25
        val etherBtnH = 16

        val colorX = x + width - 126
        val colorY = y + 5 + yOffset
        val colorW = 25
        val colorH = 16

        val checkSize = 4
        for (cy in 0 until colorH step checkSize) {
            for (cx in 0 until colorW step checkSize) {
                val isLight = ((cx / checkSize) + (cy / checkSize)) % 2 == 0
                val checkColor = if (isLight) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                graphics.fill(colorX + cx, colorY + cy, colorX + cx + checkSize, colorY + cy + checkSize, withAlpha(checkColor, alpha))
            }
        }
        graphics.fill(colorX, colorY, colorX + colorW, colorY + colorH, withAlpha(highlight.color.toArgb(), alpha))
        graphics.fill(colorX - 1, colorY - 1, colorX + colorW + 1, colorY + colorH + 1, withAlpha(PEACH_DARK, alpha * 0.5f))

        val etherHoverKey = "ether_$index"
        val etherHover = mouseX in etherBtnX..(etherBtnX + etherBtnW) && mouseY in etherBtnY..(etherBtnY + etherBtnH) && !isClosing && colorPicker == null
        val targetEtherHover = if (etherHover) 1f else 0f
        val currentEtherHover = hoverProgress.getOrDefault(etherHoverKey, 0f)
        val newEtherHover = lerp(currentEtherHover, targetEtherHover, HOVER_LERP_SPEED)
        hoverProgress[etherHoverKey] = newEtherHover

        val etherBaseColor = if (highlight.etherActivate) ETHER_ON else ETHER_OFF
        val etherColor = lerpColor(etherBaseColor, brighten(etherBaseColor, 30), newEtherHover)
        graphics.fill(etherBtnX, etherBtnY, etherBtnX + etherBtnW, etherBtnY + etherBtnH, withAlpha(etherColor, alpha))
        graphics.drawString(font, "⚡", etherBtnX + 9, etherBtnY + 4, withAlpha(TEXT_LIGHT, alpha), false)

        // ESP button
        val espHoverKey = "esp_$index"
        val espHover = mouseX in espBtnX..(espBtnX + espBtnW) && mouseY in espBtnY..(espBtnY + espBtnH) && !isClosing && colorPicker == null
        val targetEspHover = if (espHover) 1f else 0f
        val currentEspHover = hoverProgress.getOrDefault(espHoverKey, 0f)
        val newEspHover = lerp(currentEspHover, targetEspHover, HOVER_LERP_SPEED)
        hoverProgress[espHoverKey] = newEspHover

        val espBaseColor = if (highlight.esp) ESP_ON else ESP_OFF
        val espColor = lerpColor(espBaseColor, brighten(espBaseColor, 30), newEspHover)
        graphics.fill(espBtnX, espBtnY, espBtnX + espBtnW, espBtnY + espBtnH, withAlpha(espColor, alpha))
        graphics.drawString(font, "👁", espBtnX + 8, espBtnY + 4, withAlpha(TEXT_LIGHT, alpha), false)

        val deleteHoverKey = "delete_$index"
        val deleteHover = mouseX in deleteX..(deleteX + deleteW) && mouseY in deleteY..(deleteY + deleteH) && !isClosing && colorPicker == null
        val targetDeleteHover = if (deleteHover) 1f else 0f
        val currentDeleteHover = hoverProgress.getOrDefault(deleteHoverKey, 0f)
        val newDeleteHover = lerp(currentDeleteHover, targetDeleteHover, HOVER_LERP_SPEED)
        hoverProgress[deleteHoverKey] = newDeleteHover

        val deleteColor = lerpColor(0xFFCC5555.toInt(), 0xFFFF6666.toInt(), newDeleteHover)
        graphics.fill(deleteX, deleteY, deleteX + deleteW, deleteY + deleteH, withAlpha(deleteColor, alpha))
        graphics.drawString(font, "✕", deleteX + 9, deleteY + 4, withAlpha(TEXT_LIGHT, alpha), false)
    }

    private fun drawFooter(graphics: GuiGraphics, mouseX: Int, mouseY: Int, alpha: Float) {
        val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT
        graphics.fill(guiLeft, footerY, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, withAlpha(PEACH_LIGHT, alpha))
        graphics.fill(guiLeft + PADDING, footerY + 2, guiLeft + GUI_WIDTH - PADDING, footerY + 3, withAlpha(PEACH_MEDIUM, alpha))

        val searchX = guiLeft + PADDING + 5
        val searchY = footerY + 10
        val searchW = GUI_WIDTH - PADDING * 2 - 80
        val searchH = 20

        val searchHoverKey = "search_field"
        val searchHover = mouseX in searchX..(searchX + searchW) && mouseY in searchY..(searchY + searchH) && !isClosing
        val targetSearchHover = if (searchHover || searchActive) 1f else 0f
        val currentSearchHover = hoverProgress.getOrDefault(searchHoverKey, 0f)
        val newSearchHover = lerp(currentSearchHover, targetSearchHover, HOVER_LERP_SPEED)
        hoverProgress[searchHoverKey] = newSearchHover

        val searchBg = lerpColor(PEACH_CREAM, 0xFFFFFFFF.toInt(), newSearchHover)
        graphics.fill(searchX - 1, searchY - 1, searchX + searchW + 1, searchY + searchH + 1, withAlpha(PEACH_DARK, alpha))
        graphics.fill(searchX, searchY, searchX + searchW, searchY + searchH, withAlpha(searchBg, alpha))

        val cursorVisible = searchActive && ((System.currentTimeMillis() / 500) % 2 == 0L)
        val displayText = if (searchText.isEmpty() && !searchActive) {
            "§7Search items to add..."
        } else {
            searchText + (if (cursorVisible) "|" else "")
        }
        graphics.drawString(font, displayText, searchX + 4, searchY + 6, withAlpha(TEXT_DARK, alpha), false)

        val resetX = searchX + searchW + 10
        val resetY = searchY
        val resetW = 55
        val resetH = 20

        val resetHoverKey = "reset_btn"
        val resetHover = mouseX in resetX..(resetX + resetW) && mouseY in resetY..(resetY + resetH) && !isClosing
        val targetResetHover = if (resetHover) 1f else 0f
        val currentResetHover = hoverProgress.getOrDefault(resetHoverKey, 0f)
        val newResetHover = lerp(currentResetHover, targetResetHover, HOVER_LERP_SPEED)
        hoverProgress[resetHoverKey] = newResetHover

        val resetColor = lerpColor(PEACH_MEDIUM, brighten(PEACH_MEDIUM, 30), newResetHover)
        graphics.fill(resetX, resetY, resetX + resetW, resetY + resetH, withAlpha(resetColor, alpha))
        graphics.drawString(font, "Reset", resetX + 12, resetY + 6, withAlpha(TEXT_LIGHT, alpha), false)
    }

    private fun drawItemDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (filteredItems.isEmpty()) return

        val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT
        val searchX = guiLeft + PADDING + 5
        val dropdownY = footerY - min(filteredItems.size, 6) * 18 - 2
        val dropdownW = GUI_WIDTH - PADDING * 2 - 80
        val itemHeight = 18
        val maxVisible = 6
        val dropdownH = min(filteredItems.size, maxVisible) * itemHeight

        graphics.fill(searchX + 2, dropdownY + 2, searchX + dropdownW + 2, dropdownY + dropdownH + 2, 0x40000000)

        graphics.fill(searchX - 1, dropdownY - 1, searchX + dropdownW + 1, dropdownY + dropdownH + 1, PEACH_DARK)
        graphics.fill(searchX, dropdownY, searchX + dropdownW, dropdownY + dropdownH, 0xFFFFFFFF.toInt())

        val visibleStart = dropdownScroll
        val visibleEnd = min(visibleStart + maxVisible, filteredItems.size)

        for (i in visibleStart until visibleEnd) {
            val item = filteredItems[i]
            val itemY = dropdownY + (i - visibleStart) * itemHeight

            val itemHover = mouseX >= searchX && mouseX <= searchX + dropdownW &&
                    mouseY >= itemY && mouseY <= itemY + itemHeight

            if (itemHover) {
                graphics.fill(searchX, itemY, searchX + dropdownW, itemY + itemHeight, 0xFFE0E0E0.toInt())
            }

            val itemId = BuiltInRegistries.ITEM.getKey(item).toString()
            val cleanId = itemId.removePrefix("minecraft:").replace("_", " ")
            val truncated = if (cleanId.length > 35) cleanId.take(32) + "..." else cleanId
            graphics.drawString(font, truncated, searchX + 4, itemY + 5, TEXT_DARK, false)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (isClosing) return false

        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        if (event.button() != 0) return super.mouseClicked(event, bl)

        colorPicker?.let { picker ->
            if (picker.isInside(mouseX, mouseY)) {
                picker.mouseClicked(mouseX, mouseY, 0)
                return true
            } else {
                colorPicker = null
                editingItemId = null
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

        if (dropdownOpen) {
            val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT
            val searchX = guiLeft + PADDING + 5
            val dropdownY = footerY - min(filteredItems.size, 6) * 18 - 2
            val dropdownW = GUI_WIDTH - PADDING * 2 - 80
            val itemHeight = 18
            val maxVisible = 6

            val visibleStart = dropdownScroll
            val visibleEnd = min(visibleStart + maxVisible, filteredItems.size)

            for (i in visibleStart until visibleEnd) {
                val itemY = dropdownY + (i - visibleStart) * itemHeight
                if (mouseX >= searchX && mouseX <= searchX + dropdownW && mouseY >= itemY && mouseY <= itemY + itemHeight) {
                    val item = filteredItems[i]
                    val itemId = BuiltInRegistries.ITEM.getKey(item).toString()
                    FMBlocksHighlights.addHighlight(itemId)
                    searchText = ""
                    dropdownOpen = false
                    searchActive = false
                    updateFilteredItems()
                    recomputeMaxScroll()
                    playClick()
                    return true
                }
            }

            dropdownOpen = false
            searchActive = false
            return true
        }

        val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT
        val searchX = guiLeft + PADDING + 5
        val searchY = footerY + 10
        val searchW = GUI_WIDTH - PADDING * 2 - 80
        val searchH = 20
        if (mouseX in searchX..(searchX + searchW) && mouseY in searchY..(searchY + searchH)) {
            searchActive = true
            dropdownOpen = true
            playClick()
            return true
        }

        val resetX = searchX + searchW + 10
        val resetY = searchY
        val resetW = 55
        val resetH = 20
        if (mouseX in resetX..(resetX + resetW) && mouseY in resetY..(resetY + resetH)) {
            FMBlocksHighlights.resetToDefaults()
            recomputeMaxScroll()
            playClick()
            return true
        }

        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING
        val contentWidth = GUI_WIDTH - PADDING * 2

        if (mouseY >= contentTop && mouseY < contentBottom) {
            val highlights = FMBlocksHighlights.getAllHighlights()
            var y = contentTop - animatedScrollY.toInt()

            for ((index, highlight) in highlights.withIndex()) {
                if (mouseY >= y && mouseY < y + ROW_HEIGHT) {
                    val toggleX = contentLeft + 5
                    val toggleY = y + 6
                    val toggleW = 30
                    val toggleH = 14
                    if (mouseX in toggleX..(toggleX + toggleW) && mouseY in toggleY..(toggleY + toggleH)) {
                        FMBlocksHighlights.setHighlightEnabled(highlight.itemId, !highlight.enabled)
                        playClick()
                        return true
                    }

                    val colorX = contentLeft + contentWidth - 126
                    val colorY = y + 5
                    val colorW = 25
                    val colorH = 16
                    if (mouseX in colorX..(colorX + colorW) && mouseY in colorY..(colorY + colorH)) {
                        openColorPicker(highlight)
                        playClick()
                        return true
                    }

                    val etherBtnX = contentLeft + contentWidth - 91
                    val etherBtnY = y + 5
                    val etherBtnW = 25
                    val etherBtnH = 16
                    if (mouseX in etherBtnX..(etherBtnX + etherBtnW) && mouseY in etherBtnY..(etherBtnY + etherBtnH)) {
                        FMBlocksHighlights.setEtherActivate(highlight.itemId, !highlight.etherActivate)
                        playClick()
                        return true
                    }

                    val espBtnX = contentLeft + contentWidth - 63
                    val espBtnY = y + 5
                    val espBtnW = 25
                    val espBtnH = 16
                    if (mouseX in espBtnX..(espBtnX + espBtnW) && mouseY in espBtnY..(espBtnY + espBtnH)) {
                        FMBlocksHighlights.setEsp(highlight.itemId, !highlight.esp)
                        playClick()
                        return true
                    }

                    val deleteX = contentLeft + contentWidth - 35
                    val deleteY = y + 5
                    val deleteW = 25
                    val deleteH = 16
                    if (mouseX in deleteX..(deleteX + deleteW) && mouseY in deleteY..(deleteY + deleteH)) {
                        FMBlocksHighlights.removeHighlight(highlight.itemId)
                        recomputeMaxScroll()
                        playClick()
                        return true
                    }
                }
                y += ROW_HEIGHT
            }
        }

        searchActive = false
        return super.mouseClicked(event, bl)
    }

    private fun openColorPicker(highlight: FMBlocksHighlights.BlockHighlight) {
        editingItemId = highlight.itemId
        colorPicker = ColorPickerPopup(
            highlight.color.r,
            highlight.color.g,
            highlight.color.b,
            highlight.color.a,
            onApply = { r, g, b, a ->
                FMBlocksHighlights.setHighlightColor(highlight.itemId, r, g, b, a)
                colorPicker = null
                editingItemId = null
            },
            onCancel = {
                colorPicker = null
                editingItemId = null
            }
        ).apply {
            x = guiLeft + GUI_WIDTH + 10
            y = guiTop + 50
        }
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        colorPicker?.let {
            if (it.mouseDragged(event.x().toInt(), event.y().toInt())) {
                return true
            }
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        colorPicker?.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (dropdownOpen && filteredItems.size > 6) {
            val maxDropdownScroll = filteredItems.size - 6
            dropdownScroll = (dropdownScroll - verticalAmount.toInt()).coerceIn(0, maxDropdownScroll)
            return true
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

        if (event.key() == 256) {
            if (colorPicker != null) {
                colorPicker = null
                editingItemId = null
                return true
            }
            if (dropdownOpen) {
                dropdownOpen = false
                searchActive = false
                return true
            }
            startClosing()
            return true
        }

        if (searchActive) {
            when (event.key()) {
                259 -> {
                    if (searchText.isNotEmpty()) {
                        searchText = searchText.dropLast(1)
                        updateFilteredItems()
                        dropdownScroll = 0
                    }
                    return true
                }
                257, 335 -> {
                    if (filteredItems.isNotEmpty()) {
                        val item = filteredItems[0]
                        val itemId = BuiltInRegistries.ITEM.getKey(item).toString()
                        FMBlocksHighlights.addHighlight(itemId)
                        searchText = ""
                        dropdownOpen = false
                        searchActive = false
                        updateFilteredItems()
                        recomputeMaxScroll()
                    }
                    return true
                }
            }
            return true
        }

        return super.keyPressed(event)
    }

    override fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        colorPicker?.let {
            if (it.charTyped(event.codepoint().toChar())) {
                return true
            }
        }

        if (searchActive) {
            val char = event.codepoint().toChar()
            if (char.isLetterOrDigit() || char == '_' || char == ':' || char == ' ') {
                searchText += char
                updateFilteredItems()
                dropdownScroll = 0
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