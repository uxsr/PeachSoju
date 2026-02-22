package com.peachsoju.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.max

class AutoRoutesHelpScreen(private val parent: Screen? = null) : Screen(Component.literal("AutoRoutes Help")) {

    companion object {
        private const val PEACH_DARK = 0xFF8B5A3C.toInt()
        private const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        private const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        private const val PEACH_CREAM = 0xFFFFF5E6.toInt()
        private const val TEXT_DARK = 0xFF000000.toInt()
        private const val TEXT_HEADER = 0xFF6B3D2E.toInt()

        private const val GUI_WIDTH = 340
        private const val GUI_HEIGHT = 280
        private const val PADDING = 10
        private const val HEADER_H = 30
        private const val FOOTER_H = 26
        private const val LINE_H = 11
        private const val SCROLL_STEP = 14
    }

    private var guiLeft = 0
    private var guiTop = 0
    private var scrollY = 0
    private var maxScroll = 0

    private val rawLines = listOf(
        "§0§l§n== BASIC CONTROLS ==",
        "",
        "§8/ar §0- Show status",
        "§8/ar toggle §0- Enable/disable module",
        "§8/ar config §0- Toggle config mode (trigger any node)",
        "§8/ar reload §0- Reload waypoints from disk",
        "§8/ar reset §0- Reload + reset route state",
        "",
        "§0§l§n== RENDER OPTIONS ==",
        "",
        "§8/ar render §0- Toggle waypoint rendering",
        "§8/ar lines §0- Toggle burst chain lines",
        "§8/ar startsonly §0- Only show start nodes",
        "§8/ar burst §0- Toggle ETHER burst mode",
        "",
        "§0§l§n== NODE TYPES ==",
        "",
        "§3ether §0- Etherwarp (sneaking + AOTV)",
        "§6aotv §0- Regular AOTV teleport",
        "§5hype §0- Hyperion/Sceptre ability",
        "§4superboom §0- Place superboom TNT",
        "§2useitem §0- Use a specific item",
        "§elook §0- Set rotation + end route",
        "§8nop §0- No action (just a waypoint)",
        "§awalk §0- Walk forward in yaw/pitch direction",
        "§cstop §0- Stop all movement (releases all keys)",
        "",
        "§0§l§n== ADDING NODES ==",
        "",
        "§8/ar add <type> §0[modifiers...]",
        "§8/ar insert <index> <type> §0[modifiers...]",
        "",
        "§0Examples:",
        "§8/ar add ether",
        "§8/ar add ether start",
        "§8/ar add aotv mult:3",
        "§8/ar add useitem item:pearl",
        "§8/ar add superboom §0(look at target block)",
        "§8/ar add walk §0(walks in current direction)",
        "§8/ar add stop §0(stops walking when triggered)",
        "",
        "§0§l§n== MODIFIERS ==",
        "",
        "§6start §0- Mark as route start point",
        "§6delay:<ticks> §0- Wait before executing",
        "§6await:<n> §0- Wait for N secrets",
        "§6await:<n>:<type> §0- Wait for N secrets (chest/item/lever)",
        "§6awaitbat §0- Wait for bat spawn",
        "§6db §0- Wait for dungeon breaker blocks",
        "§6mult:<n> §0- Send N clicks (AOTV only, 1-10)",
        "§6item:<name> §0- Item name (USEITEM only)",
        "§6radius:<r> §0- Trigger radius (default 0.5)",
        "§6height:<h> §0- Trigger height (default 1.5)",
        "",
        "§0§l§n== WALK/STOP USAGE ==",
        "",
        "§0The §awalk§0 node holds W and sets your rotation.",
        "§0Walking stops when you:",
        "§8  - §0Step on a §cstop§0 node",
        "§8  - §0Press any movement key (WASD/Space/Shift)",
        "",
        "§0§l§n== EDITING NODES ==",
        "",
        "§8/ar remove §0- Remove closest node",
        "§8/ar remove <index> §0- Remove by index",
        "§8/ar undo §0- Restore last removed",
        "§8/ar move <from> <to> §0- Reorder node",
        "§8/ar set <index> <mod> [val] §0- Change modifier",
        "§8/ar updatepos <index> §0- Update position",
        "§8/ar updaterot <index> §0- Update rotation",
        "",
        "§0Examples:",
        "§8/ar set 0 start",
        "§8/ar set 3 mult 2",
        "§8/ar set 5 await 1",
        "",
        "§0§l§n== OTHER ==",
        "",
        "§8/ar list §0- List all nodes in room",
        "§8/ar clear §0- Delete all nodes in room",
        "§8/ar info §0- Show loaded rooms/nodes",
        "§8/ar testether §0- Test etherwarp prediction",
        "§8/ar testchain <index> §0- Test burst chain",
        ""
    )

    private val wrappedLines: List<String> by lazy {
        val maxTextW = GUI_WIDTH - (PADDING * 2) - 18
        buildList {
            for (line in rawLines) {
                if (line.isBlank()) add("")
                else addAll(wrapFormattedLine(line, maxTextW))
            }
        }
    }

    override fun init() {
        super.init()
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2
        recomputeMaxScroll()
    }

    private fun recomputeMaxScroll() {
        val contentH = wrappedLines.size * LINE_H
        val viewH = GUI_HEIGHT - HEADER_H - FOOTER_H - (PADDING * 2) - 12
        maxScroll = max(0, contentH - viewH)
        scrollY = scrollY.coerceIn(0, maxScroll)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, 0xAA000000.toInt())
        drawPanel(graphics)
        drawHeader(graphics)
        drawContent(graphics)
        drawFooter(graphics)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawPanel(graphics: GuiGraphics) {
        graphics.fill(guiLeft - 2, guiTop - 2, guiLeft + GUI_WIDTH + 2, guiTop + GUI_HEIGHT + 2, PEACH_DARK)
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, PEACH_LIGHT)
        graphics.fill(
            guiLeft + PADDING,
            guiTop + HEADER_H + PADDING,
            guiLeft + GUI_WIDTH - PADDING,
            guiTop + GUI_HEIGHT - FOOTER_H - PADDING,
            PEACH_CREAM
        )
    }

    private fun drawHeader(graphics: GuiGraphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_H, PEACH_MEDIUM)
        val title = "§l✿ AutoRoutes Help ✿"
        val w = font.width(title)
        graphics.drawString(font, title, guiLeft + (GUI_WIDTH - w) / 2, guiTop + 10, TEXT_DARK, false)
    }

    private fun drawContent(graphics: GuiGraphics) {
        val contentLeft = guiLeft + PADDING + 6
        val contentTop = guiTop + HEADER_H + PADDING + 6
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_H - PADDING - 6

        val clipL = guiLeft + PADDING + 2
        val clipT = guiTop + HEADER_H + PADDING + 2
        val clipR = guiLeft + GUI_WIDTH - PADDING - 2
        val clipB = guiTop + GUI_HEIGHT - FOOTER_H - PADDING - 2

        graphics.enableScissor(clipL, clipT, clipR, clipB)

        var y = contentTop - scrollY
        for (line in wrappedLines) {
            if (y > contentBottom) break
            if (y + LINE_H >= contentTop) {
                graphics.drawString(font, line, contentLeft, y, TEXT_DARK, false)
            }
            y += LINE_H
        }

        graphics.disableScissor()

        if (maxScroll > 0) drawScrollBar(graphics, clipR - 6, clipT + 2, clipB - 2)
    }

    private fun drawScrollBar(graphics: GuiGraphics, x: Int, top: Int, bottom: Int) {
        val trackH = bottom - top
        graphics.fill(x, top, x + 3, bottom, PEACH_DARK)

        val thumbH = max(16, (trackH * (trackH.toFloat() / (trackH + maxScroll))).toInt())
        val maxThumbY = trackH - thumbH
        val thumbY = if (maxScroll == 0) 0 else ((scrollY.toFloat() / maxScroll) * maxThumbY).toInt()

        graphics.fill(x + 1, top + thumbY, x + 2, top + thumbY + thumbH, TEXT_DARK)
    }

    private fun drawFooter(graphics: GuiGraphics) {
        val y = guiTop + GUI_HEIGHT - FOOTER_H
        graphics.fill(guiLeft + PADDING, y, guiLeft + GUI_WIDTH - PADDING, y + 1, PEACH_MEDIUM)
        val footer = "§7Scroll for more • ESC to close"
        val w = font.width(footer)
        graphics.drawString(font, footer, guiLeft + (GUI_WIDTH - w) / 2, y + 8, TEXT_DARK, false)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (maxScroll == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        scrollY = (scrollY - (verticalAmount.toInt() * SCROLL_STEP)).coerceIn(0, maxScroll)
        return true
    }

    override fun keyPressed(keyEvent: net.minecraft.client.input.KeyEvent): Boolean {
        if (keyEvent.key() == 256) { onClose(); return true }
        val mc = Minecraft.getInstance()
        if (mc.options.keyInventory.matches(keyEvent)) { onClose(); return true }
        return super.keyPressed(keyEvent)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun onClose() {
        Minecraft.getInstance().setScreen(parent)
    }

    private fun wrapFormattedLine(line: String, maxWidth: Int): List<String> {
        if (font.width(line) <= maxWidth) return listOf(line)

        val tokens = line.split(" ")
        val out = mutableListOf<String>()
        var current = ""
        var activeFormat = extractTrailingFormat("")

        fun push() {
            out.add(current)
            activeFormat = extractTrailingFormat(current)
            current = activeFormat
        }

        for (t in tokens) {
            val token = if (current == activeFormat || current.isEmpty()) t else " $t"
            val test = current + token
            if (current.isNotEmpty() && font.width(test) > maxWidth) {
                push()
                current += t
            } else {
                current += token
            }
        }
        if (current.isNotEmpty()) out.add(current)
        return out
    }

    private fun extractTrailingFormat(s: String): String {
        var i = 0
        val formats = StringBuilder()
        while (i < s.length - 1) {
            if (s[i] == '§') {
                val code = s[i + 1]
                formats.append('§').append(code)
                i += 2
            } else i++
        }
        return formats.toString()
    }
}