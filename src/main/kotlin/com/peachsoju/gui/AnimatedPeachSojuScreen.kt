package com.peachsoju.gui

import com.peachsoju.config
import com.peachsoju.gui.features.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.*

class AnimatedPeachSojuScreen : Screen(Component.literal("PeachSoju")) {

    companion object {
        private const val PEACH_DARK = 0xFF8B5A3C.toInt()
        private const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        private const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        private const val PEACH_CREAM = 0xFFFFF5E6.toInt()
        private const val PEACH_GLOW = 0xFFFFE8D0.toInt()

        private const val TEXT_DARK = 0xFF4A3728.toInt()
        private const val TEXT_LIGHT = 0xFFFFFFFF.toInt()

        private const val STATUS_ON = 0xFF7CB342.toInt()
        private const val STATUS_OFF = 0xFFB85C5C.toInt()

        private const val GUI_WIDTH = 300
        private const val GUI_HEIGHT = 320
        private const val PADDING = 10
        private const val HEADER_HEIGHT = 30
        private const val FOOTER_HEIGHT = 26
        private const val CARD_HEIGHT = 32
        private const val CARD_SPACING = 6
        private const val SCROLL_STEP = 14

        private const val SCREEN_OPEN_DURATION = 300f
        private const val SCREEN_CLOSE_DURATION = 180f
        private const val CARD_STAGGER_DELAY = 50f
        private const val HOVER_LERP_SPEED = 0.18f
        private const val SCROLL_LERP_SPEED = 0.22f
    }

    private object Easing {
        fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)
        fun easeOutBack(t: Float): Float {
            val c1 = 1.70158f
            val c3 = c1 + 1f
            return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
        }
        fun easeOutQuad(t: Float): Float = 1f - (1f - t) * (1f - t)
    }

    private var guiLeft = 0
    private var guiTop = 0
    private var scrollY = 0
    private var maxScroll = 0

    private var screenOpenTime = 0L
    private var isClosing = false
    private var closeStartTime = 0L
    private var pendingScreen: Screen? = null

    private var animatedScrollY = 0f
    private val cardHoverProgress = mutableMapOf<Int, Float>()
    private val cardEntryProgress = mutableMapOf<Int, Float>()
    private val statusBarProgress = mutableMapOf<Int, Float>()

    private var titleBounce = 0f

    data class FeatureCard(
        val name: String,
        val description: String,
        val enabledGetter: () -> Boolean,
        val screenOpener: () -> Unit
    )

    private val features = listOf(
        FeatureCard("AutoRoutes", "Etherwarp waypoints & burst mode", { config.autoroutes() }, { openScreen(AutoRoutesScreen(this)) }),
        FeatureCard("FM Blocks", "Interactable custom blocks", { config.fmBlocksEnabled() }, { openScreen(FMBlocksScreen(this)) }),
        FeatureCard("AutoSS", "Simon Says solver", { config.autoSS() }, { openScreen(AutoSSScreen(this)) }),
        FeatureCard("Auto Ice Fill", "Ice Fill puzzle solver", { config.autoIceFill() }, { openScreen(AutoIceFillScreen(this)) }),
        FeatureCard("Auto Align", "Arrow alignment solver", { config.autoAlign() }, { openScreen(AutoAlignScreen(this)) }),
        FeatureCard("Storm Bow Timer", "Erectile Dysfunction", { config.stormBowTimer() }, { openScreen(StormBowTimerScreen(this)) }),
        FeatureCard("Legacy Animations", "1.8.9 animations", { config.stormLegacyAnimation() }, { openScreen(LegacyAnimationsScreen(this)) }),
        FeatureCard("Misc", "Miscellaneous settings", { config.hideServerID() }, { openScreen(MiscScreen(this)) })
    )

    override fun init() {
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2
        recomputeMaxScroll()

        if (screenOpenTime == 0L) {
            screenOpenTime = System.currentTimeMillis()
        }
        animatedScrollY = scrollY.toFloat()
    }

    private fun recomputeMaxScroll() {
        val contentHeight = features.size * (CARD_HEIGHT + CARD_SPACING) - CARD_SPACING
        val viewHeight = GUI_HEIGHT - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2)
        maxScroll = max(0, contentHeight - viewHeight)
        scrollY = scrollY.coerceIn(0, max(0, maxScroll))
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
                pendingScreen?.let { Minecraft.getInstance().setScreen(it) }
                    ?: Minecraft.getInstance().setScreen(null)
                return
            }
        } else {
            val openElapsed = (currentTime - screenOpenTime).toFloat()
            screenProgress = Easing.easeOutBack((openElapsed / SCREEN_OPEN_DURATION).coerceIn(0f, 1f))
            screenAlpha = Easing.easeOutQuad((openElapsed / (SCREEN_OPEN_DURATION * 0.6f)).coerceIn(0f, 1f))
            slideOffset = 0
        }

        animatedScrollY = lerp(animatedScrollY, scrollY.toFloat(), SCROLL_LERP_SPEED)

        val elapsed = (currentTime - screenOpenTime) / 1000f
        titleBounce = sin(elapsed * 2f) * 0.5f

        val bgAlpha = (screenAlpha * 0.67f * 255).toInt().coerceIn(0, 255)
        graphics.fill(0, 0, width, height, (bgAlpha shl 24))

        val originalGuiLeft = guiLeft
        guiLeft += slideOffset

        drawPanel(graphics, screenAlpha, screenProgress)
        drawHeader(graphics, screenAlpha)
        drawFeatureCards(graphics, mouseX, mouseY, screenAlpha)
        drawFooter(graphics, screenAlpha)

        guiLeft = originalGuiLeft

        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun drawPanel(graphics: GuiGraphics, alpha: Float, progress: Float) {
        val animatedWidth = (GUI_WIDTH * progress).toInt()
        val animatedHeight = (GUI_HEIGHT * progress).toInt()
        val animatedLeft = guiLeft + (GUI_WIDTH - animatedWidth) / 2
        val animatedTop = guiTop + (GUI_HEIGHT - animatedHeight) / 2

        val glowLayers = 4
        for (i in 1..glowLayers) {
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
            withAlpha(PEACH_LIGHT, alpha)
        )
    }

    private fun drawHeader(graphics: GuiGraphics, alpha: Float) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_HEIGHT, withAlpha(PEACH_MEDIUM, alpha))

        val title = "§l✿ PeachSoju ✿"
        val titleX = guiLeft + (GUI_WIDTH - font.width(title)) / 2
        val titleY = guiTop + 10 + titleBounce.toInt()

        graphics.drawString(font, title, titleX + 1, titleY + 1, withAlpha(PEACH_DARK, alpha * 0.3f), false)
        graphics.drawString(font, title, titleX, titleY, withAlpha(TEXT_DARK, alpha), false)
    }

    private fun drawFeatureCards(graphics: GuiGraphics, mouseX: Int, mouseY: Int, alpha: Float) {
        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING
        val cardWidth = GUI_WIDTH - PADDING * 2

        graphics.enableScissor(contentLeft, contentTop, guiLeft + GUI_WIDTH - PADDING, contentBottom)

        var y = contentTop - animatedScrollY.toInt()
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - screenOpenTime).toFloat()

        for ((index, feature) in features.withIndex()) {
            if (y + CARD_HEIGHT >= contentTop && y < contentBottom) {
                val entryDelay = index * CARD_STAGGER_DELAY
                val entryProgress = ((elapsed - entryDelay - 80f) / 250f).coerceIn(0f, 1f)
                val easedEntry = Easing.easeOutCubic(entryProgress)
                cardEntryProgress[index] = easedEntry

                val slideOffset = ((1f - easedEntry) * 40).toInt()
                val cardAlpha = alpha * easedEntry

                val hover = !isClosing &&
                        mouseX >= contentLeft && mouseX <= contentLeft + cardWidth &&
                        mouseY >= y && mouseY <= y + CARD_HEIGHT &&
                        mouseY >= contentTop && mouseY < contentBottom

                val targetHover = if (hover) 1f else 0f
                val currentHover = cardHoverProgress.getOrDefault(index, 0f)
                val newHover = lerp(currentHover, targetHover, HOVER_LERP_SPEED)
                cardHoverProgress[index] = newHover

                drawFeatureCard(graphics, feature, index, contentLeft + slideOffset, y, cardWidth, newHover, cardAlpha)
            }
            y += CARD_HEIGHT + CARD_SPACING
        }

        graphics.disableScissor()
    }

    private fun drawFeatureCard(
        graphics: GuiGraphics,
        feature: FeatureCard,
        index: Int,
        x: Int, y: Int, width: Int,
        hoverProgress: Float,
        alpha: Float
    ) {
        val enabled = feature.enabledGetter()

        val yOffset = (-hoverProgress * 2).toInt()
        val bgColor = lerpColor(PEACH_CREAM, PEACH_GLOW, hoverProgress)

        if (hoverProgress > 0.01f) {
            val shadowAlpha = alpha * hoverProgress * 0.15f
            graphics.fill(x + 2, y + yOffset + 3, x + width + 2, y + CARD_HEIGHT + yOffset + 3, withAlpha(0xFF000000.toInt(), shadowAlpha))
        }

        val borderColor = lerpColor(PEACH_DARK, brighten(PEACH_DARK, 20), hoverProgress)
        graphics.fill(x - 1, y - 1 + yOffset, x + width + 1, y + CARD_HEIGHT + 1 + yOffset, withAlpha(borderColor, alpha))

        graphics.fill(x, y + yOffset, x + width, y + CARD_HEIGHT + yOffset, withAlpha(bgColor, alpha))

        val targetStatus = if (enabled) 1f else 0f
        val currentStatus = statusBarProgress.getOrDefault(index, targetStatus)
        val newStatus = lerp(currentStatus, targetStatus, 0.15f)
        statusBarProgress[index] = newStatus

        val statusColor = lerpColor(STATUS_OFF, STATUS_ON, newStatus)
        val statusWidth = 4 + (hoverProgress * 2).toInt()
        graphics.fill(x, y + yOffset, x + statusWidth, y + CARD_HEIGHT + yOffset, withAlpha(statusColor, alpha))

        val nameColor = lerpColor(TEXT_DARK, brighten(TEXT_DARK, -20), hoverProgress)
        graphics.drawString(font, "§l${feature.name}", x + 10, y + 6 + yOffset, withAlpha(nameColor, alpha), false)

        graphics.drawString(font, "§7${feature.description}", x + 10, y + 18 + yOffset, withAlpha(TEXT_DARK, alpha * 0.85f), false)

        val statusText = if (enabled) "§aON" else "§cOFF"
        val statusTextX = x + width - font.width(statusText) - 8
        val statusTextY = y + 11 + yOffset

        if (hoverProgress > 0.1f && enabled) {
            graphics.drawString(font, statusText, statusTextX + 1, statusTextY, withAlpha(STATUS_ON, alpha * hoverProgress * 0.3f), false)
        }
        graphics.drawString(font, statusText, statusTextX, statusTextY, withAlpha(TEXT_DARK, alpha), false)
    }

    private fun drawFooter(graphics: GuiGraphics, alpha: Float) {
        val footerY = guiTop + GUI_HEIGHT - FOOTER_HEIGHT

        graphics.fill(guiLeft, footerY, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, withAlpha(PEACH_LIGHT, alpha))
        graphics.fill(guiLeft + PADDING, footerY + 2, guiLeft + GUI_WIDTH - PADDING, footerY + 3, withAlpha(PEACH_MEDIUM, alpha))

        val footerText = "§7ESC to close"
        graphics.drawString(font, footerText, guiLeft + (GUI_WIDTH - font.width(footerText)) / 2, footerY + 10, withAlpha(TEXT_DARK, alpha), false)
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

    private fun brighten(color: Int, amount: Int = 20): Int {
        val a = (color ushr 24) and 0xFF
        var r = (color ushr 16) and 0xFF
        var g = (color ushr 8) and 0xFF
        var b = color and 0xFF
        r = (r + amount).coerceIn(0, 255)
        g = (g + amount).coerceIn(0, 255)
        b = (b + amount).coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        if (isClosing) return false

        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()

        if (event.button() != 0) return super.mouseClicked(event, bl)

        val contentLeft = guiLeft + PADDING
        val contentTop = guiTop + HEADER_HEIGHT + PADDING
        val contentBottom = guiTop + GUI_HEIGHT - FOOTER_HEIGHT - PADDING
        val cardWidth = GUI_WIDTH - PADDING * 2

        if (mouseY < contentTop || mouseY >= contentBottom) {
            return super.mouseClicked(event, bl)
        }

        var y = contentTop - animatedScrollY.toInt()

        for ((index, feature) in features.withIndex()) {
            if (mouseX >= contentLeft && mouseX <= contentLeft + cardWidth &&
                mouseY >= y && mouseY <= y + CARD_HEIGHT &&
                y + CARD_HEIGHT >= contentTop && y < contentBottom) {

                val entryProgress = cardEntryProgress.getOrDefault(index, 0f)
                if (entryProgress > 0.5f) {
                    playClickSound()
                    feature.screenOpener()
                    return true
                }
            }
            y += CARD_HEIGHT + CARD_SPACING
        }

        return super.mouseClicked(event, bl)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (maxScroll > 0) {
            scrollY = (scrollY - (verticalAmount.toInt() * SCROLL_STEP)).coerceIn(0, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: net.minecraft.client.input.KeyEvent): Boolean {
        if (event.key() == 256) {
            startClosing(null)
            return true
        }

        val mc = Minecraft.getInstance()
        if (mc.options.keyInventory.matches(event)) {
            startClosing(null)
            return true
        }

        return super.keyPressed(event)
    }

    private fun openScreen(screen: Screen) {
        Minecraft.getInstance().setScreen(screen)
    }

    private fun startClosing(nextScreen: Screen?) {
        if (!isClosing) {
            isClosing = true
            closeStartTime = System.currentTimeMillis()
            pendingScreen = nextScreen
        }
    }

    private fun playClickSound() {
        Minecraft.getInstance().soundManager.play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
            )
        )
    }
}