package com.blowup.gui

import com.blowup.config
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class PeachSojuScreen : Screen(Component.literal("PeachSoju")) {

    companion object {
        private const val peachDark = 0xFF8B5A3C.toInt()
        private const val peachMedium = 0xFFD4956A.toInt()
        private const val peachLight = 0xFFFFDAB9.toInt()
        private const val peachCream = 0xFFFFF5E6.toInt()

        private const val textDark = 0xFF4A3728.toInt()
        private const val textLight = 0xFFFFFFFF.toInt()

        private const val toggleOn = 0xFF7CB342.toInt()
        private const val toggleOff = 0xFFB85C5C.toInt()

        private const val guiWidth = 220
        private const val guiHeight = 280
        private const val padding = 10
        private const val rowHeight = 22
        private const val toggleWidth = 40
        private const val toggleHeight = 16
    }

    private var guiLeft = 0
    private var guiTop = 0

    private data class ToggleOption(
        val name: String,
        val getter: () -> Boolean,
        val toggler: () -> Boolean,
        val description: String = ""
    )

    private val masterToggle = ToggleOption("AutoRoutes", { config.autoroutes() }, { config.toggleAutoroutes() }, "Main module toggle")

    private val dependentToggles: List<ToggleOption>
        get() = buildList {
            add(ToggleOption("Burst Mode", { config.burstMode() }, { config.toggleBurstMode() }, "Chain etherwarp teleports"))
            add(ToggleOption("Config Mode", { config.configMode() }, { config.toggleConfigMode() }, "Allow triggering any node"))
            add(ToggleOption("Waypoint Render", { config.waypointRendering() }, { config.toggleWaypointRendering() }, "Show waypoint boxes"))
            if (config.waypointRendering()) {
                add(ToggleOption("  Show Lines", { config.showLines() }, { config.toggleShowLines() }, "Draw lines between nodes"))
                add(ToggleOption("  Start Only", { config.renderOnlyStartNodes() }, { config.toggleRenderOnlyStartNodes() }, "Only render start nodes"))
            }
            add(ToggleOption("Debug Mode", { config.debug() }, { config.toggleDebug() }, "Show debug messages"))
        }

    private val visibleToggles: List<ToggleOption>
        get() = if (config.autoroutes()) listOf(masterToggle) + dependentToggles else listOf(masterToggle)

    override fun init() {
        guiLeft = (width - guiWidth) / 2
        guiTop = (height - guiHeight) / 2
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, 0xAA000000.toInt())
        drawPanel(graphics)
        drawHeader(graphics)
        drawToggles(graphics, mouseX, mouseY)
        drawFooter(graphics)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawPanel(graphics: GuiGraphics) {
        graphics.fill(guiLeft - 2, guiTop - 2, guiLeft + guiWidth + 2, guiTop + guiHeight + 2, peachDark)
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, peachLight)
        graphics.fill(guiLeft + padding, guiTop + 35, guiLeft + guiWidth - padding, guiTop + guiHeight - 30, peachCream)
    }

    private fun drawHeader(graphics: GuiGraphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + 30, peachMedium)
        val title = "§l✿ PeachSoju ✿"
        graphics.drawString(font, title, guiLeft + (guiWidth - font.width(title)) / 2, guiTop + 10, textDark, false)
    }

    private fun drawToggles(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val startY = guiTop + 45
        val toggles = visibleToggles

        for ((i, opt) in toggles.withIndex()) {
            val rowY = startY + i * rowHeight
            val enabled = opt.getter()

            graphics.drawString(font, opt.name, guiLeft + padding + 5, rowY + 4, textDark, false)

            val toggleX = guiLeft + guiWidth - padding - toggleWidth - 5
            val toggleY = rowY + 2
            val hover = mouseX in toggleX..(toggleX + toggleWidth) && mouseY in toggleY..(toggleY + toggleHeight)

            val base = if (enabled) toggleOn else toggleOff
            val fill = if (hover) brighten(base) else base

            graphics.fill(toggleX - 1, toggleY - 1, toggleX + toggleWidth + 1, toggleY + toggleHeight + 1, peachDark)
            graphics.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, fill)

            val label = if (enabled) "ON" else "OFF"
            graphics.drawString(font, label, toggleX + (toggleWidth - font.width(label)) / 2, toggleY + 4, textLight, false)

            if (hover && opt.description.isNotEmpty()) drawTooltip(graphics, mouseX, mouseY, opt.description)
        }

        if (!config.autoroutes()) return

        val buttonY = startY + toggles.size * rowHeight + 10
        val buttonX = guiLeft + padding + 5
        val buttonW = guiWidth - padding * 2 - 10
        val buttonH = 20
        val hover = mouseX in buttonX..(buttonX + buttonW) && mouseY in buttonY..(buttonY + buttonH)

        val bg = if (hover) brighten(peachMedium) else peachMedium
        graphics.fill(buttonX - 1, buttonY - 1, buttonX + buttonW + 1, buttonY + buttonH + 1, peachDark)
        graphics.fill(buttonX, buttonY, buttonX + buttonW, buttonY + buttonH, bg)

        val text = "Node Appearance Settings"
        graphics.drawString(font, text, buttonX + (buttonW - font.width(text)) / 2, buttonY + 6, textLight, false)
    }

    private fun drawFooter(graphics: GuiGraphics) {
        val footerY = guiTop + guiHeight - 22
        graphics.fill(guiLeft + padding, footerY - 5, guiLeft + guiWidth - padding, footerY - 4, peachMedium)

        val footerText = "§7/ar help for commands"
        graphics.drawString(font, footerText, guiLeft + (guiWidth - font.width(footerText)) / 2, footerY + 2, textDark, false)
    }

    private fun drawTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int, text: String) {
        val w = font.width(text) + 8
        val h = 14
        val x = mouseX + 10
        val y = mouseY - 10
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, peachDark)
        graphics.fill(x, y, x + w, y + h, peachCream)
        graphics.drawString(font, text, x + 4, y + 3, textDark, false)
    }

    private fun brighten(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        var r = (color ushr 16) and 0xFF
        var g = (color ushr 8) and 0xFF
        var b = color and 0xFF
        r = minOf(255, r + 30); g = minOf(255, g + 30); b = minOf(255, b + 30)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        val mouseX = event.x()
        val mouseY = event.y()
        if (event.button() != 0) return super.mouseClicked(event, bl)

        val startY = guiTop + 45
        val toggles = visibleToggles

        if (config.autoroutes()) {
            val buttonY = startY + toggles.size * rowHeight + 10
            val buttonX = guiLeft + padding + 5
            val buttonW = guiWidth - padding * 2 - 10
            val buttonH = 20

            if (mouseX >= buttonX && mouseX <= buttonX + buttonW && mouseY >= buttonY && mouseY <= buttonY + buttonH) {
                Minecraft.getInstance().setScreen(NodeAppearanceScreen(this))
                Minecraft.getInstance().soundManager.play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
                    )
                )
                return true
            }
        }

        for ((i, opt) in toggles.withIndex()) {
            val rowY = startY + i * rowHeight
            val toggleX = guiLeft + guiWidth - padding - toggleWidth - 5
            val toggleY = rowY + 2
            if (mouseX >= toggleX && mouseX <= toggleX + toggleWidth && mouseY >= toggleY && mouseY <= toggleY + toggleHeight) {
                opt.toggler()
                Minecraft.getInstance().soundManager.play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
                    )
                )
                return true
            }
        }

        return super.mouseClicked(event, bl)
    }

    override fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (event.key() == 256) { onClose(); return true }
        val mc = Minecraft.getInstance()
        if (mc.options.keyInventory.matches(event)) { onClose(); return true }
        return super.keyPressed(event)
    }
}
