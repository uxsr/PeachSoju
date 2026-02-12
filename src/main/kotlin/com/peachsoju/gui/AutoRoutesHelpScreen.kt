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

        private const val GUI_WIDTH = 330
        private const val GUI_HEIGHT = 260
        private const val PADDING = 10
        private const val HEADER_H = 30
        private const val FOOTER_H = 26
        private const val LINE_H = 10
        private const val SCROLL_STEP = 12
    }

    private var guiLeft = 0
    private var guiTop = 0
    private var scrollY = 0
    private var maxScroll = 0

    private val rawLines = listOf(
        "AutoRoutes Commands",
        "",
        "/ar - show status",
        "/ar toggle - toggle autoroutes",
        "/ar render - toggle waypoint rendering",
        "/ar lines - toggle line rendering",
        "/ar startsonly - toggle rendering only start nodes",
        "/ar burst - toggle burst mode",
        "/ar config - toggle config mode",
        "/ar info - show autoroutes info",
        "/ar clear - clear all waypoints in current room",
        "/ar reload - reload waypoints from disk",
        "/ar list - list waypoints in current room",
        "/ar remove - remove closest waypoint in current room",
        "/ar remove <index> - remove waypoint by index",
        "/ar undo - undo last remove",
        "/ar move <from> <to> - move waypoint index",
        "/ar insert <index> <type> [modifiers...] - insert waypoint at index",
        "/ar updatepos <index> - update waypoint position to your current position",
        "/ar updaterot <index> - update waypoint rotation to your current rotation",
        "/ar set <index> <modifier> [value] - set/toggle a modifier on a waypoint",
        "",
        "Add / Insert",
        "",
        "/ar add <type> [modifiers...]",
        "/ar insert <index> <type> [modifiers...]",
        "types: ether, aotv, hype, superboom, await, useitem, nop",
        "",
        "Modifiers",
        "",
        "exact - store exact fractional x/z (kinda useless)",
        "start - mark node as a start node",
        "delay:<ticks> - wait N ticks before executing",
        "radius:<r> - trigger radius (default 0.5)",
        "height:<h> - trigger height (default 1.5)",
        "item:<name> - useitem name",
        "",
        "Await",
        "",
        "await:<n> - wait for N secrets (any type)",
        "await:<n>:<type> - wait for N secrets of a specific type",
        "awaitbat - wait for bat spawn",
        "examples:",
        "/ar add ether await:2",
        "/ar add ether await:2:lever",
        "/ar add aotv await:1:chest",
        "/ar add ether awaitbat",
        "",
        "Type Examples",
        "",
        "/ar add ether",
        "/ar add ether chained exact stop center",
        "/ar add aotv",
        "/ar add hype",
        "/ar add superboom (look at a block first)",
        "/ar add useitem item:<itemname> (example: item:Inflatable_Jerry)",
        "/ar add nop",
        "",
        "Debug",
        "",
        "/ar testether - test etherwarp landing prediction from your position",
        "/ar testchain <index> - test burst chain from node index",
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
        val footer = "§7 "
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
