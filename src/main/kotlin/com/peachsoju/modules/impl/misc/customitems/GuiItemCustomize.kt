package com.peachsoju.modules.impl.misc.customitems

import com.peachsoju.PeachSoju.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.lwjgl.glfw.GLFW
import kotlin.math.*


class GuiItemCustomize(
    private val stack: ItemStack,
    private val itemUUID: String
) : Screen(Component.literal("Item Customizer")) {

    companion object {

        private const val PEACH_DARK = 0xFF8B5A3C.toInt()
        private const val PEACH_MEDIUM = 0xFFD4956A.toInt()
        private const val PEACH_LIGHT = 0xFFFFDAB9.toInt()
        private const val PEACH_CREAM = 0xFFFFF5E6.toInt()
        private const val PEACH_GLOW = 0xFFFFE4C4.toInt()

        private const val TEXT_DARK = 0xFF4A3728.toInt()
        private const val TEXT_LIGHT = 0xFFFFFFFF.toInt()
        private const val TEXT_MUTED = 0xFF8B7355.toInt()
        private const val TEXT_PLACEHOLDER = 0xFFB8A89A.toInt()

        private const val TOGGLE_ON = 0xFF7CB342.toInt()
        private const val TOGGLE_OFF = 0xFF9E9E9E.toInt()

        private const val ACCENT_GREEN = 0xFF81C784.toInt()
        private const val ACCENT_RED = 0xFFE57373.toInt()
        private const val ACCENT_BLUE = 0xFF64B5F6.toInt()
        private const val ACCENT_PURPLE = 0xFFBA68C8.toInt()
        private const val ACCENT_ORANGE = 0xFFFFB74D.toInt()
        private const val ACCENT_TEAL = 0xFF4DB6AC.toInt()

        private const val SHADOW = 0x40000000

        private const val GUI_WIDTH = 260
        private const val GUI_HEIGHT = 380
        private const val PADDING = 14
        private const val HEADER_H = 24
        private const val PREVIEW_SIZE = 80
        private const val FIELD_H = 18
        private const val TAB_H = 22

        private const val TAB_GENERAL = 0
        private const val TAB_ARMOR = 1
        private const val TAB_DYE = 2
        private const val TAB_HEAD = 3
        private const val TAB_TRIM = 4

        private const val DROPDOWN_MAX = 6
        private const val DROPDOWN_ITEM_H = 18
    }

    private var guiLeft = 0
    private var guiTop = 0

    private var openTime = 0L
    private var animProgress = 0f
    private var itemBob = 0f

    private var currentTab = TAB_GENERAL

    private var nameFieldHover = 0f
    private var itemFieldHover = 0f
    private var armorFieldHover = 0f
    private var glintHover = 0f
    private var resetHover = 0f
    private var tabHovers = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    private val dropdownHovers = mutableMapOf<Int, Float>()

    private var dyeColorHover = 0f
    private var dyeResetHover = 0f

    // Head texture tab
    private var headTextureFieldHover = 0f
    private var animatedHeadFieldHover = 0f
    private var headResetHover = 0f

    // Trim tab
    private var trimMaterialFieldHover = 0f
    private var trimPatternFieldHover = 0f
    private var trimResetHover = 0f

    private var nameText = ""
    private var nameFocused = false
    private var nameCursor = 0
    private var nameBlink = 0

    private var itemSearchText = ""
    private var itemFocused = false
    private var itemCursor = 0
    private var itemBlink = 0

    private var armorSearchText = ""
    private var armorFocused = false
    private var armorCursor = 0
    private var armorBlink = 0

    // Head texture fields
    private var headTextureText = ""
    private var headTextureFocused = false
    private var headTextureCursor = 0
    private var headTextureBlink = 0

    private var animatedHeadSearchText = ""
    private var animatedHeadFocused = false
    private var animatedHeadCursor = 0
    private var animatedHeadBlink = 0

    // Trim fields
    private var trimMaterialText = ""
    private var trimMaterialFocused = false
    private var trimMaterialCursor = 0
    private var trimMaterialBlink = 0

    private var trimPatternText = ""
    private var trimPatternFocused = false
    private var trimPatternCursor = 0
    private var trimPatternBlink = 0

    private var itemDropdownOpen = false
    private var itemDropdownScroll = 0
    private var filteredItems: List<Item> = emptyList()
    private var selectedItemIndex = 0
    private var itemDropdownAnim = 0f

    private var armorDropdownOpen = false
    private var armorDropdownScroll = 0
    private var filteredArmor: List<ItemCustomizeManager.ArmorInfo> = emptyList()
    private var selectedArmorIndex = 0
    private var armorDropdownAnim = 0f

    // Animated head dropdown
    private var animatedHeadDropdownOpen = false
    private var animatedHeadDropdownScroll = 0
    private var filteredAnimatedHeads: List<String> = emptyList()
    private var selectedAnimatedHeadIndex = 0
    private var animatedHeadDropdownAnim = 0f

    // Trim dropdowns
    private var trimMaterialDropdownOpen = false
    private var trimMaterialDropdownScroll = 0
    private var filteredTrimMaterials: List<ResourceLocation> = emptyList()
    private var selectedTrimMaterialIndex = 0
    private var trimMaterialDropdownAnim = 0f

    private var trimPatternDropdownOpen = false
    private var trimPatternDropdownScroll = 0
    private var filteredTrimPatterns: List<ResourceLocation> = emptyList()
    private var selectedTrimPatternIndex = 0
    private var trimPatternDropdownAnim = 0f

    private var enchantGlint = false
    private var previewStack: ItemStack = stack.copy()

    private var currentDyeColor: Int? = null
    private var colorPicker: ColorPickerPopup? = null

    private val isArmor = ItemCustomizeManager.isArmor(stack)
    private val isHelmet = ItemCustomizeManager.isHelmet(stack)
    private val isTrimmable = ItemCustomizeManager.isTrimmableArmor(stack)
    private val isDyeable: Boolean by lazy {
        stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR) != null ||
                stack.item.toString().contains("leather", ignoreCase = true)
    }
    private val originalIsArmor = ItemCustomizeManager.isArmor(stack)
    private val originalIsHelmet = ItemCustomizeManager.isHelmet(stack)

    init {
        loadData()
        updateFilteredItems()
        if (isArmor) {
            filteredArmor = ItemCustomizeManager.getCompatibleArmorItems(stack)
        }
        if (isHelmet) {
            filteredAnimatedHeads = ItemCustomizeManager.getAnimatedHeadIds().toList()
        }
        if (isTrimmable) {
            ItemCustomizeManager.ensureTrimsInitialized()
            filteredTrimMaterials = ItemCustomizeManager.getAllTrimMaterials()
            filteredTrimPatterns = ItemCustomizeManager.getAllTrimPatterns()
        }
    }

    private fun loadData() {
        val data = ItemCustomizeManager.getDataForUUID(itemUUID)
        if (data != null) {
            nameText = data.customName ?: ""
            enchantGlint = if (data.overrideEnchantGlint) data.enchantGlintValue else stack.hasFoil()
            itemSearchText = data.customItem?.removePrefix("minecraft:") ?: ""
            armorSearchText = data.customArmorModel?.removePrefix("minecraft:") ?: ""
            data.customLeatherColour?.let {
                currentDyeColor = ItemCustomizeManager.parseSpecialColor(it)
            }
            // Load head texture data
            headTextureText = data.customHelmetTexture ?: ""
            animatedHeadSearchText = data.animatedHelmetId ?: ""
            // Load trim data
            trimMaterialText = data.customTrimMaterial?.removePrefix("minecraft:") ?: ""
            trimPatternText = data.customTrimPattern?.removePrefix("minecraft:") ?: ""
        } else {
            enchantGlint = stack.hasFoil()
        }
        nameCursor = nameText.length
        itemCursor = itemSearchText.length
        armorCursor = armorSearchText.length
        headTextureCursor = headTextureText.length
        animatedHeadCursor = animatedHeadSearchText.length
        trimMaterialCursor = trimMaterialText.length
        trimPatternCursor = trimPatternText.length
        updatePreviewStack()
    }

    override fun init() {
        super.init()
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2
        if (openTime == 0L) openTime = System.currentTimeMillis()
    }

    override fun onClose() {
        saveData()
        super.onClose()
    }

    private fun saveData() {
        val data = ItemCustomizeManager.getDataForUUID(itemUUID) ?: ItemData()
        data.defaultItem = stack.item.builtInRegistryHolder().key().location().toString()

        if (nameText.isNotEmpty()) {
            data.customName = nameText
            val originalName = stack.hoverName.string
            var prefix = ""
            val chars = originalName.toCharArray()
            var i = 0
            while (i < chars.size - 1) {
                if (chars[i] == '§') { prefix += "§${chars[i + 1]}"; i += 2 } else break
            }
            data.customNamePrefix = prefix
        } else {
            data.customName = null
            data.customNamePrefix = null
        }

        val stackHasGlint = stack.hasFoil()
        if (enchantGlint != stackHasGlint) {
            data.overrideEnchantGlint = true
            data.enchantGlintValue = enchantGlint
        } else {
            data.overrideEnchantGlint = false
        }

        if (itemSearchText.isNotEmpty()) {
            data.customItem = if (itemSearchText.contains(":")) itemSearchText else "minecraft:$itemSearchText"
        } else {
            data.customItem = null
        }

        if (armorSearchText.isNotEmpty() && isArmor) {
            data.customArmorModel = if (armorSearchText.contains(":")) armorSearchText else "minecraft:$armorSearchText"
        } else {
            data.customArmorModel = null
        }

        if (currentDyeColor != null && isEffectiveDyeable()) {
            val r = (currentDyeColor!! shr 16) and 0xFF
            val g = (currentDyeColor!! shr 8) and 0xFF
            val b = currentDyeColor!! and 0xFF
            data.customLeatherColour = ItemCustomizeManager.createSpecialColor(0, 255, r, g, b)
        } else {
            data.customLeatherColour = null
        }

        // Save head texture data
        if (originalIsHelmet || isEffectiveHelmet()) {
            if (headTextureText.isNotEmpty()) {
                data.customHelmetTexture = headTextureText
            } else {
                data.customHelmetTexture = null
            }
            if (animatedHeadSearchText.isNotEmpty()) {
                data.animatedHelmetId = animatedHeadSearchText
            } else {
                data.animatedHelmetId = null
            }
        }

        // Save trim data
        if (isEffectiveTrimmable()) {
            if (trimMaterialText.isNotEmpty() && trimPatternText.isNotEmpty()) {
                data.customTrimMaterial = if (trimMaterialText.contains(":")) trimMaterialText else "minecraft:$trimMaterialText"
                data.customTrimPattern = if (trimPatternText.contains(":")) trimPatternText else "minecraft:$trimPatternText"
            } else {
                data.customTrimMaterial = null
                data.customTrimPattern = null
            }
        }

        ItemCustomizeManager.putItemData(itemUUID, data)
    }

    private fun getCurrentCustomItem(): Item? {
        if (itemSearchText.isEmpty()) return null
        return try {
            val id = if (itemSearchText.contains(":")) itemSearchText else "minecraft:$itemSearchText"
            val loc = ResourceLocation.parse(id)
            BuiltInRegistries.ITEM.getOptional(loc).orElse(null)
        } catch (e: Exception) { null }
    }

    private fun getEffectiveStack(): ItemStack {
        val customItem = getCurrentCustomItem()
        return if (customItem != null) ItemStack(customItem) else stack
    }

    private fun isEffectiveArmor(): Boolean {
        return ItemCustomizeManager.isArmor(getEffectiveStack())
    }

    private fun isEffectiveHelmet(): Boolean {
        return ItemCustomizeManager.isHelmet(getEffectiveStack())
    }

    private fun isEffectiveTrimmable(): Boolean {
        val effectiveStack = getEffectiveStack()
        return effectiveStack.`is`(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR)
    }

    private fun isEffectiveDyeable(): Boolean {
        val effectiveStack = getEffectiveStack()
        val itemId = BuiltInRegistries.ITEM.getKey(effectiveStack.item)?.toString() ?: return false
        return itemId.contains("leather", ignoreCase = true)
    }

    private fun updateFilteredItems() {
        val search = itemSearchText.lowercase().trim()
        if (search.isEmpty()) {
            filteredItems = emptyList()
            return
        }
        val words = search.replace("_", " ").split(" ").filter { it.isNotEmpty() }
        filteredItems = BuiltInRegistries.ITEM
            .filter { item ->
                val key = BuiltInRegistries.ITEM.getKey(item)?.toString()?.lowercase() ?: return@filter false
                val clean = key.removePrefix("minecraft:").replace("_", " ")
                words.all { w -> clean.contains(w) || key.contains(w) }
            }
            .sortedBy { item ->
                val key = BuiltInRegistries.ITEM.getKey(item)?.toString()?.lowercase() ?: ""
                val clean = key.removePrefix("minecraft:")
                when {
                    clean == search -> 0
                    clean.startsWith(search) -> 1
                    clean.contains(search) -> 2
                    else -> 3
                }
            }
            .take(30)
    }

    private fun updateFilteredArmor() {
        if (!isArmor) return
        val search = armorSearchText.lowercase().trim()
        val all = ItemCustomizeManager.getCompatibleArmorItems(stack)
        if (search.isEmpty()) {
            filteredArmor = all
            return
        }
        filteredArmor = all.filter { info ->
            info.displayName.lowercase().contains(search) ||
                    info.id.lowercase().contains(search) ||
                    info.material.lowercase().contains(search)
        }
    }

    private fun updateFilteredAnimatedHeads() {
        val search = animatedHeadSearchText.lowercase().trim()
        val all = ItemCustomizeManager.getAnimatedHeadIds().toList()
        if (search.isEmpty()) {
            filteredAnimatedHeads = all
            return
        }
        filteredAnimatedHeads = all.filter { id ->
            id.lowercase().contains(search) ||
                    ItemCustomizeManager.formatAnimatedHeadName(id).lowercase().contains(search)
        }
    }

    private fun updateFilteredTrimMaterials() {
        val search = trimMaterialText.lowercase().trim()
        val all = ItemCustomizeManager.getAllTrimMaterials()
        if (search.isEmpty()) {
            filteredTrimMaterials = all
            return
        }
        filteredTrimMaterials = all.filter { loc ->
            loc.path.lowercase().contains(search) ||
                    loc.toString().lowercase().contains(search)
        }
    }

    private fun updateFilteredTrimPatterns() {
        val search = trimPatternText.lowercase().trim()
        val all = ItemCustomizeManager.getAllTrimPatterns()
        if (search.isEmpty()) {
            filteredTrimPatterns = all
            return
        }
        filteredTrimPatterns = all.filter { loc ->
            loc.path.lowercase().contains(search) ||
                    loc.toString().lowercase().contains(search)
        }
    }

    private fun updatePreviewStack() {
        val baseItem: Item? = if (itemSearchText.isNotEmpty()) {
            try {
                val id = if (itemSearchText.contains(":")) itemSearchText else "minecraft:$itemSearchText"
                val loc = net.minecraft.resources.ResourceLocation.parse(id)
                BuiltInRegistries.ITEM.getOptional(loc).orElse(null)
            } catch (e: Exception) { null }
        } else null

        previewStack = if (baseItem != null && baseItem != Items.AIR) {
            ItemStack(baseItem, stack.count)
        } else {
            stack.copy()
        }

        if (currentDyeColor != null) {
            previewStack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                net.minecraft.world.item.component.DyedItemColor(currentDyeColor!! and 0x00FFFFFF))
        }
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        updateAnimations(mouseX, mouseY)

        val bgAlpha = (animProgress * 0.7f * 255).toInt().coerceIn(0, 255)
        graphics.fill(0, 0, width, height, bgAlpha shl 24)

        drawPanel(graphics)
        drawHeader(graphics)
        drawTabs(graphics, mouseX, mouseY)
        drawItemPreview(graphics)

        when (currentTab) {
            TAB_GENERAL -> drawGeneralTab(graphics, mouseX, mouseY)
            TAB_ARMOR -> drawArmorTab(graphics, mouseX, mouseY)
            TAB_DYE -> drawDyeTab(graphics, mouseX, mouseY)
            TAB_HEAD -> drawHeadTab(graphics, mouseX, mouseY)
            TAB_TRIM -> drawTrimTab(graphics, mouseX, mouseY)
        }

        drawResetButton(graphics, mouseX, mouseY)
        drawHelpText(graphics)

        // Draw dropdowns on top
        if (itemDropdownOpen && filteredItems.isNotEmpty() && itemDropdownAnim > 0.1f) {
            drawItemDropdown(graphics, mouseX, mouseY)
        }
        if (armorDropdownOpen && filteredArmor.isNotEmpty() && armorDropdownAnim > 0.1f) {
            drawArmorDropdown(graphics, mouseX, mouseY)
        }
        if (animatedHeadDropdownOpen && filteredAnimatedHeads.isNotEmpty() && animatedHeadDropdownAnim > 0.1f) {
            drawAnimatedHeadDropdown(graphics, mouseX, mouseY)
        }
        if (trimMaterialDropdownOpen && filteredTrimMaterials.isNotEmpty() && trimMaterialDropdownAnim > 0.1f) {
            drawTrimMaterialDropdown(graphics, mouseX, mouseY)
        }
        if (trimPatternDropdownOpen && filteredTrimPatterns.isNotEmpty() && trimPatternDropdownAnim > 0.1f) {
            drawTrimPatternDropdown(graphics, mouseX, mouseY)
        }

        colorPicker?.render(graphics, mouseX, mouseY)

        super.render(graphics, mouseX, mouseY, partialTick)
    }

    private fun updateAnimations(mouseX: Int, mouseY: Int) {
        nameBlink++; itemBlink++; armorBlink++
        headTextureBlink++; animatedHeadBlink++
        trimMaterialBlink++; trimPatternBlink++

        val elapsed = (System.currentTimeMillis() - openTime).toFloat()
        animProgress = easeOutCubic((elapsed / 250f).coerceIn(0f, 1f))
        itemBob = sin(elapsed / 1000f * 1.8f) * 2f

        itemDropdownAnim = lerp(itemDropdownAnim, if (itemDropdownOpen && filteredItems.isNotEmpty()) 1f else 0f, 0.22f)
        armorDropdownAnim = lerp(armorDropdownAnim, if (armorDropdownOpen && filteredArmor.isNotEmpty()) 1f else 0f, 0.22f)
        animatedHeadDropdownAnim = lerp(animatedHeadDropdownAnim, if (animatedHeadDropdownOpen && filteredAnimatedHeads.isNotEmpty()) 1f else 0f, 0.22f)
        trimMaterialDropdownAnim = lerp(trimMaterialDropdownAnim, if (trimMaterialDropdownOpen && filteredTrimMaterials.isNotEmpty()) 1f else 0f, 0.22f)
        trimPatternDropdownAnim = lerp(trimPatternDropdownAnim, if (trimPatternDropdownOpen && filteredTrimPatterns.isNotEmpty()) 1f else 0f, 0.22f)

        updateHovers(mouseX, mouseY)
        updatePreviewStack()
    }

    private fun updateHovers(mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        val tabs = buildTabList()
        val numTabs = tabs.size
        val tabW = (GUI_WIDTH - PADDING * 2) / numTabs
        for (i in tabs.indices) {
            val tabX = guiLeft + PADDING + i * tabW
            val tabY = guiTop + HEADER_H
            val hover = mouseX in tabX..(tabX + tabW) && mouseY in tabY..(tabY + TAB_H)
            tabHovers[i] = lerp(tabHovers[i], if (hover) 1f else 0f, 0.18f)
        }

        when (currentTab) {
            TAB_GENERAL -> {
                val nameY = contentY
                val nameHover = mouseX in fieldL..fieldR && mouseY in nameY..(nameY + FIELD_H)
                nameFieldHover = lerp(nameFieldHover, if (nameHover || nameFocused) 1f else 0f, 0.18f)

                val itemY = nameY + FIELD_H + 24
                val itemHover = mouseX in fieldL..fieldR && mouseY in itemY..(itemY + FIELD_H)
                itemFieldHover = lerp(itemFieldHover, if (itemHover || itemFocused) 1f else 0f, 0.18f)

                val glintY = itemY + FIELD_H + 24
                val toggleX = fieldR - 36
                val glintToggleHover = mouseX in toggleX..fieldR && mouseY in glintY..(glintY + 16)
                glintHover = lerp(glintHover, if (glintToggleHover) 1f else 0f, 0.18f)
            }
            TAB_ARMOR -> {
                val armorHover = mouseX in fieldL..fieldR && mouseY in contentY..(contentY + FIELD_H)
                armorFieldHover = lerp(armorFieldHover, if (armorHover || armorFocused) 1f else 0f, 0.18f)
            }
            TAB_DYE -> {
                val colorBoxX = fieldL + 85
                val colorBoxY = contentY
                val colorHover = mouseX in colorBoxX..(colorBoxX + 24) && mouseY in colorBoxY..(colorBoxY + 24)
                dyeColorHover = lerp(dyeColorHover, if (colorHover) 1f else 0f, 0.18f)

                val resetY = colorBoxY + 34
                val resetHov = mouseX in fieldL..(fieldL + 50) && mouseY in resetY..(resetY + 16)
                dyeResetHover = lerp(dyeResetHover, if (resetHov) 1f else 0f, 0.18f)
            }
            TAB_HEAD -> {
                val textureY = contentY
                val textureHover = mouseX in fieldL..fieldR && mouseY in textureY..(textureY + FIELD_H)
                headTextureFieldHover = lerp(headTextureFieldHover, if (textureHover || headTextureFocused) 1f else 0f, 0.18f)

                val animatedY = textureY + FIELD_H + 24
                val animatedHover = mouseX in fieldL..fieldR && mouseY in animatedY..(animatedY + FIELD_H)
                animatedHeadFieldHover = lerp(animatedHeadFieldHover, if (animatedHover || animatedHeadFocused) 1f else 0f, 0.18f)

                val resetY = animatedY + FIELD_H + 42
                val resetHov = mouseX in fieldL..(fieldL + 50) && mouseY in resetY..(resetY + 16)
                headResetHover = lerp(headResetHover, if (resetHov) 1f else 0f, 0.18f)
            }
            TAB_TRIM -> {
                val materialY = contentY
                val materialHover = mouseX in fieldL..fieldR && mouseY in materialY..(materialY + FIELD_H)
                trimMaterialFieldHover = lerp(trimMaterialFieldHover, if (materialHover || trimMaterialFocused) 1f else 0f, 0.18f)

                val patternY = materialY + FIELD_H + 24
                val patternHover = mouseX in fieldL..fieldR && mouseY in patternY..(patternY + FIELD_H)
                trimPatternFieldHover = lerp(trimPatternFieldHover, if (patternHover || trimPatternFocused) 1f else 0f, 0.18f)

                val resetY = patternY + FIELD_H + 42
                val resetHov = mouseX in fieldL..(fieldL + 50) && mouseY in resetY..(resetY + 16)
                trimResetHover = lerp(trimResetHover, if (resetHov) 1f else 0f, 0.18f)
            }
        }

        val resetY = guiTop + GUI_HEIGHT - 36
        val resetX = guiLeft + (GUI_WIDTH - 60) / 2
        val resetHov = mouseX in resetX..(resetX + 60) && mouseY in resetY..(resetY + 18)
        resetHover = lerp(resetHover, if (resetHov) 1f else 0f, 0.18f)
    }

    private fun buildTabList(): List<Pair<String, Int>> {
        val tabs = mutableListOf<Pair<String, Int>>()
        tabs.add("General" to TAB_GENERAL)

        // Show Armor tab if EITHER the original OR the custom item is armor
        // (so you can change armor models for armor pieces)
        if (originalIsArmor || isEffectiveArmor()) tabs.add("Armor" to TAB_ARMOR)

        // Show Dye tab if the EFFECTIVE (custom) item is dyeable
        if (isEffectiveDyeable()) tabs.add("Dye" to TAB_DYE)

        // Show Head tab if EITHER original OR effective is a helmet
        if (originalIsHelmet || isEffectiveHelmet()) tabs.add("Head" to TAB_HEAD)

        // Show Trim tab if the EFFECTIVE item is trimmable
        if (isEffectiveTrimmable()) tabs.add("Trim" to TAB_TRIM)

        return tabs
    }

    private fun drawPanel(graphics: GuiGraphics) {
        for (i in 1..3) {
            val a = animProgress * (0.1f / i)
            graphics.fill(guiLeft - i * 2, guiTop - i * 2, guiLeft + GUI_WIDTH + i * 2, guiTop + GUI_HEIGHT + i * 2, withAlpha(PEACH_MEDIUM, a))
        }

        graphics.fill(guiLeft - 2, guiTop - 2, guiLeft + GUI_WIDTH + 2, guiTop + GUI_HEIGHT + 2, withAlpha(PEACH_DARK, animProgress))
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, withAlpha(PEACH_LIGHT, animProgress))
        graphics.fill(guiLeft + 6, guiTop + HEADER_H + TAB_H + 4, guiLeft + GUI_WIDTH - 6, guiTop + GUI_HEIGHT - 44, withAlpha(PEACH_CREAM, animProgress * 0.7f))
    }

    private fun drawHeader(graphics: GuiGraphics) {
        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_H, withAlpha(PEACH_MEDIUM, animProgress))
        val title = "✿ Item Customizer"
        val tw = font.width(title)
        graphics.drawString(font, title, guiLeft + (GUI_WIDTH - tw) / 2, guiTop + (HEADER_H - font.lineHeight) / 2, withAlpha(TEXT_DARK, animProgress), false)
    }

    private fun drawTabs(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val tabY = guiTop + HEADER_H
        val tabs = buildTabList()
        val numTabs = tabs.size
        val tabW = (GUI_WIDTH - PADDING * 2) / numTabs

        for (i in tabs.indices) {
            val (tabName, tabIndex) = tabs[i]
            val tabX = guiLeft + PADDING + i * tabW
            val selected = currentTab == tabIndex
            val hoverProg = tabHovers.getOrElse(i) { 0f }

            val bgColor = if (selected) PEACH_CREAM else lerpColor(PEACH_LIGHT, PEACH_GLOW, hoverProg)
            graphics.fill(tabX, tabY, tabX + tabW, tabY + TAB_H, withAlpha(bgColor, animProgress))

            if (selected) {
                val accentColor = when (tabIndex) {
                    TAB_GENERAL -> ACCENT_PURPLE
                    TAB_ARMOR -> ACCENT_BLUE
                    TAB_DYE -> ACCENT_ORANGE
                    TAB_HEAD -> ACCENT_TEAL
                    TAB_TRIM -> ACCENT_GREEN
                    else -> ACCENT_PURPLE
                }
                graphics.fill(tabX, tabY + TAB_H - 2, tabX + tabW, tabY + TAB_H, withAlpha(accentColor, animProgress))
            }

            val textColor = if (selected) TEXT_DARK else TEXT_MUTED
            val tw = font.width(tabName)
            graphics.drawString(font, tabName, tabX + (tabW - tw) / 2, tabY + (TAB_H - font.lineHeight) / 2, withAlpha(textColor, animProgress), false)
        }
    }

    private fun drawItemPreview(graphics: GuiGraphics) {
        val previewX = guiLeft + (GUI_WIDTH - PREVIEW_SIZE) / 2
        val previewY = guiTop + HEADER_H + TAB_H + 10 + itemBob.toInt()

        graphics.fill(previewX - 1, previewY - 1, previewX + PREVIEW_SIZE + 1, previewY + PREVIEW_SIZE + 1, withAlpha(PEACH_DARK, animProgress * 0.5f))
        graphics.fill(previewX, previewY, previewX + PREVIEW_SIZE, previewY + PREVIEW_SIZE, withAlpha(PEACH_MEDIUM, animProgress * 0.6f))

        val itemCenterX = previewX + PREVIEW_SIZE / 2
        val itemCenterY = previewY + PREVIEW_SIZE / 2

        val scale = 4.0f
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(itemCenterX.toFloat(), itemCenterY.toFloat())
        pose.scale(scale, scale)
        pose.translate(-8f, -8f)
        graphics.renderItem(previewStack, 0, 0)
        pose.popMatrix()

        val nameY = previewY + PREVIEW_SIZE + 8
        val displayName = if (nameText.isNotEmpty()) processColorCodes(nameText) else previewStack.hoverName.string
        val nameW = font.width(displayName)
        val nameX = guiLeft + (GUI_WIDTH - nameW) / 2
        graphics.drawString(font, displayName, nameX, nameY, withAlpha(TEXT_DARK, animProgress), false)
    }

    private fun drawGeneralTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        var y = contentY
        graphics.drawString(font, "§7Display Name", fieldL, y - 11, withAlpha(TEXT_MUTED, animProgress), false)
        drawTextField(graphics, fieldL, y, fieldR - fieldL, nameText, nameFocused, nameFieldHover, nameCursor, nameBlink, "Enter name...")

        y += FIELD_H + 24
        graphics.drawString(font, "§7Item Model", fieldL, y - 11, withAlpha(TEXT_MUTED, animProgress), false)
        drawTextFieldWithIcon(graphics, fieldL, y, fieldR - fieldL, itemSearchText, itemFocused, itemFieldHover, itemCursor, itemBlink, "Search items...", filteredItems.firstOrNull())

        y += FIELD_H + 24
        graphics.drawString(font, "§7Enchant Glint", fieldL, y + 2, withAlpha(TEXT_MUTED, animProgress), false)
        drawToggle(graphics, fieldR - 36, y, 36, 16, enchantGlint, glintHover)
    }

    private fun drawArmorTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        graphics.drawString(font, "§7Armor Model", fieldL, contentY - 11, withAlpha(TEXT_MUTED, animProgress), false)

        val currentArmor = filteredArmor.find { it.id.removePrefix("minecraft:") == armorSearchText }
        drawTextFieldWithIcon(graphics, fieldL, contentY, fieldR - fieldL, armorSearchText, armorFocused, armorFieldHover, armorCursor, armorBlink, "Search armor...", currentArmor?.item)
    }

    private fun drawDyeTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val fieldL = guiLeft + PADDING

        graphics.drawString(font, "§7Leather Color:", fieldL, contentY + 6, withAlpha(TEXT_MUTED, animProgress), false)

        val colorBoxX = fieldL + 85
        val colorBoxY = contentY
        val displayColor = currentDyeColor ?: 0xFFA06540.toInt()

        val borderColor = lerpColor(PEACH_DARK, ACCENT_BLUE, dyeColorHover * 0.5f)
        graphics.fill(colorBoxX - 2, colorBoxY - 2, colorBoxX + 26, colorBoxY + 26, withAlpha(borderColor, animProgress))
        graphics.fill(colorBoxX, colorBoxY, colorBoxX + 24, colorBoxY + 24, withAlpha(displayColor or (0xFF shl 24), animProgress))

        graphics.drawString(font, "§8(click to edit)", colorBoxX + 30, colorBoxY + 8, withAlpha(TEXT_MUTED, animProgress * 0.7f), false)

        val resetY = contentY + 34
        val resetColor = lerpColor(ACCENT_RED, brighten(ACCENT_RED, 20), dyeResetHover)
        graphics.fill(fieldL, resetY, fieldL + 50, resetY + 16, withAlpha(resetColor, animProgress))
        graphics.drawString(font, "Reset", fieldL + 10, resetY + 4, withAlpha(TEXT_LIGHT, animProgress), false)
    }

    private fun drawHeadTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        var y = contentY
        graphics.drawString(font, "§7Static Head Texture (Base64)", fieldL, y - 11, withAlpha(TEXT_MUTED, animProgress), false)
        drawTextField(graphics, fieldL, y, fieldR - fieldL, headTextureText, headTextureFocused, headTextureFieldHover, headTextureCursor, headTextureBlink, "Paste texture Base64...")

        y += FIELD_H + 24
        graphics.drawString(font, "§7Animated Head", fieldL, y - 11, withAlpha(TEXT_MUTED, animProgress), false)
        drawTextField(graphics, fieldL, y, fieldR - fieldL, animatedHeadSearchText, animatedHeadFocused, animatedHeadFieldHover, animatedHeadCursor, animatedHeadBlink, "Search animated heads...")

        // Show hint about animated heads
        y += FIELD_H + 6
        graphics.drawString(font, "§8Animated heads override static textures", fieldL, y, withAlpha(TEXT_MUTED, animProgress * 0.6f), false)

        y += 18
        val resetColor = lerpColor(ACCENT_RED, brighten(ACCENT_RED, 20), headResetHover)
        graphics.fill(fieldL, y, fieldL + 50, y + 16, withAlpha(resetColor, animProgress))
        graphics.drawString(font, "Reset", fieldL + 10, y + 4, withAlpha(TEXT_LIGHT, animProgress), false)
    }

    private fun drawTrimTab(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        var y = contentY
        graphics.drawString(font, "§7Trim Material", fieldL, y - 11, withAlpha(TEXT_MUTED, animProgress), false)
        drawTextField(graphics, fieldL, y, fieldR - fieldL, trimMaterialText, trimMaterialFocused, trimMaterialFieldHover, trimMaterialCursor, trimMaterialBlink, "Search materials...")

        y += FIELD_H + 24
        graphics.drawString(font, "§7Trim Pattern", fieldL, y - 11, withAlpha(TEXT_MUTED, animProgress), false)
        drawTextField(graphics, fieldL, y, fieldR - fieldL, trimPatternText, trimPatternFocused, trimPatternFieldHover, trimPatternCursor, trimPatternBlink, "Search patterns...")

        // Show validation status
        y += FIELD_H + 6
        if (trimMaterialText.isNotEmpty() && trimPatternText.isNotEmpty()) {
            val materialId = try { ResourceLocation.parse(if (trimMaterialText.contains(":")) trimMaterialText else "minecraft:$trimMaterialText") } catch (e: Exception) { null }
            val patternId = try { ResourceLocation.parse(if (trimPatternText.contains(":")) trimPatternText else "minecraft:$trimPatternText") } catch (e: Exception) { null }

            if (materialId != null && patternId != null && ItemCustomizeManager.isTrimValid(materialId, patternId)) {
                graphics.drawString(font, "§a✓ Valid trim combination", fieldL, y, withAlpha(ACCENT_GREEN, animProgress), false)
            } else {
                graphics.drawString(font, "§c✗ Invalid material or pattern", fieldL, y, withAlpha(ACCENT_RED, animProgress), false)
            }
        } else if (trimMaterialText.isNotEmpty() || trimPatternText.isNotEmpty()) {
            graphics.drawString(font, "§7Both material and pattern required", fieldL, y, withAlpha(TEXT_MUTED, animProgress * 0.7f), false)
        }

        y += 18
        val resetColor = lerpColor(ACCENT_RED, brighten(ACCENT_RED, 20), trimResetHover)
        graphics.fill(fieldL, y, fieldL + 50, y + 16, withAlpha(resetColor, animProgress))
        graphics.drawString(font, "Reset", fieldL + 10, y + 4, withAlpha(TEXT_LIGHT, animProgress), false)
    }

    private fun drawTextField(graphics: GuiGraphics, x: Int, y: Int, w: Int, text: String, focused: Boolean, hoverProg: Float, cursor: Int, blink: Int, placeholder: String) {
        val borderColor = lerpColor(PEACH_DARK, ACCENT_BLUE, if (focused) 0.7f else hoverProg * 0.3f)
        val bgColor = lerpColor(0xFFFFFFFF.toInt(), PEACH_GLOW, hoverProg * 0.3f)

        graphics.fill(x - 1, y - 1, x + w + 1, y + FIELD_H + 1, withAlpha(borderColor, animProgress))
        graphics.fill(x, y, x + w, y + FIELD_H, withAlpha(bgColor, animProgress))

        val displayText = if (text.isEmpty() && !focused) "§o$placeholder" else processColorCodes(text)
        val textColor = if (text.isEmpty() && !focused) TEXT_PLACEHOLDER else TEXT_DARK

        // Truncate text if too long to display
        val maxWidth = w - 8
        var renderText = displayText
        if (font.width(renderText) > maxWidth) {
            renderText = "..." + text.takeLast(text.length - 3)
            while (font.width(renderText) > maxWidth && renderText.length > 4) {
                renderText = "..." + renderText.drop(4)
            }
        }

        graphics.drawString(font, renderText, x + 4, y + (FIELD_H - font.lineHeight) / 2, withAlpha(textColor, animProgress), false)

        if (focused && (blink / 15) % 2 == 0) {
            val cursorX = x + 4 + font.width(text.substring(0, cursor.coerceAtMost(text.length)))
            graphics.fill(cursorX, y + 2, cursorX + 1, y + FIELD_H - 2, withAlpha(TEXT_DARK, animProgress))
        }
    }

    private fun drawTextFieldWithIcon(graphics: GuiGraphics, x: Int, y: Int, w: Int, text: String, focused: Boolean, hoverProg: Float, cursor: Int, blink: Int, placeholder: String, iconItem: Item?) {
        val borderColor = lerpColor(PEACH_DARK, ACCENT_BLUE, if (focused) 0.7f else hoverProg * 0.3f)
        val bgColor = lerpColor(0xFFFFFFFF.toInt(), PEACH_GLOW, hoverProg * 0.3f)

        graphics.fill(x - 1, y - 1, x + w + 1, y + FIELD_H + 1, withAlpha(borderColor, animProgress))
        graphics.fill(x, y, x + w, y + FIELD_H, withAlpha(bgColor, animProgress))

        val textOffset = if (iconItem != null) 18 else 4
        if (iconItem != null) {
            graphics.renderItem(ItemStack(iconItem), x + 1, y + 1)
        }

        val displayText = if (text.isEmpty() && !focused) "§o$placeholder" else text
        val textColor = if (text.isEmpty() && !focused) TEXT_PLACEHOLDER else TEXT_DARK
        graphics.drawString(font, displayText, x + textOffset, y + (FIELD_H - font.lineHeight) / 2, withAlpha(textColor, animProgress), false)

        if (text.isNotEmpty()) {
            val valid = iconItem != null
            val dotColor = if (valid) ACCENT_GREEN else ACCENT_RED
            graphics.fill(x + w - 6, y + FIELD_H / 2 - 2, x + w - 2, y + FIELD_H / 2 + 2, withAlpha(dotColor, animProgress))
        }

        if (focused && (blink / 15) % 2 == 0) {
            val cursorX = x + textOffset + font.width(text.substring(0, cursor.coerceAtMost(text.length)))
            graphics.fill(cursorX, y + 2, cursorX + 1, y + FIELD_H - 2, withAlpha(TEXT_DARK, animProgress))
        }
    }

    private fun drawToggle(graphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int, enabled: Boolean, hoverProg: Float) {
        val bgColor = lerpColor(TOGGLE_OFF, TOGGLE_ON, if (enabled) 1f else 0f)
        val brightenAmt = (hoverProg * 20).toInt()

        graphics.fill(x, y, x + w, y + h, withAlpha(brighten(bgColor, brightenAmt), animProgress))

        val knobSize = h - 4
        val knobX = if (enabled) x + w - knobSize - 2 else x + 2
        graphics.fill(knobX, y + 2, knobX + knobSize, y + 2 + knobSize, withAlpha(0xFFFFFFFF.toInt(), animProgress))
    }

    private fun drawItemDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val itemY = contentY + FIELD_H + 24
        val dropY = itemY + FIELD_H + 2
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        val visibleCount = min(filteredItems.size, DROPDOWN_MAX)
        val dropH = (visibleCount * DROPDOWN_ITEM_H * itemDropdownAnim).toInt()
        if (dropH <= 0) return

        graphics.fill(fieldL + 2, dropY + 2, fieldR + 2, dropY + dropH + 2, withAlpha(SHADOW, animProgress * 0.3f))
        graphics.fill(fieldL - 1, dropY - 1, fieldR + 1, dropY + dropH + 1, withAlpha(PEACH_DARK, animProgress))
        graphics.fill(fieldL, dropY, fieldR, dropY + dropH, withAlpha(0xFFFFFFFF.toInt(), animProgress))

        graphics.enableScissor(fieldL, dropY, fieldR, dropY + dropH)

        for (i in 0 until visibleCount) {
            val idx = i + itemDropdownScroll
            if (idx >= filteredItems.size) break

            val item = filteredItems[idx]
            val itemYPos = dropY + i * DROPDOWN_ITEM_H
            val key = BuiltInRegistries.ITEM.getKey(item)?.toString()?.removePrefix("minecraft:") ?: continue

            val hoverProg = dropdownHovers.getOrDefault(idx, 0f)
            if (hoverProg > 0.01f) {
                graphics.fill(fieldL, itemYPos, fieldR, itemYPos + DROPDOWN_ITEM_H, withAlpha(PEACH_GLOW, animProgress * hoverProg))
            }

            if (idx == selectedItemIndex) {
                graphics.fill(fieldL, itemYPos, fieldL + 2, itemYPos + DROPDOWN_ITEM_H, withAlpha(ACCENT_BLUE, animProgress))
            }

            graphics.renderItem(ItemStack(item), fieldL + 1, itemYPos + 1)
            graphics.drawString(font, key, fieldL + 18, itemYPos + (DROPDOWN_ITEM_H - font.lineHeight) / 2, withAlpha(TEXT_DARK, animProgress), false)
        }

        graphics.disableScissor()
    }

    private fun drawArmorDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val dropY = contentY + FIELD_H + 2
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        val visibleCount = min(filteredArmor.size, DROPDOWN_MAX)
        val dropH = (visibleCount * DROPDOWN_ITEM_H * armorDropdownAnim).toInt()
        if (dropH <= 0) return

        graphics.fill(fieldL + 2, dropY + 2, fieldR + 2, dropY + dropH + 2, withAlpha(SHADOW, animProgress * 0.3f))
        graphics.fill(fieldL - 1, dropY - 1, fieldR + 1, dropY + dropH + 1, withAlpha(PEACH_DARK, animProgress))
        graphics.fill(fieldL, dropY, fieldR, dropY + dropH, withAlpha(0xFFFFFFFF.toInt(), animProgress))

        graphics.enableScissor(fieldL, dropY, fieldR, dropY + dropH)

        for (i in 0 until visibleCount) {
            val idx = i + armorDropdownScroll
            if (idx >= filteredArmor.size) break

            val info = filteredArmor[idx]
            val itemYPos = dropY + i * DROPDOWN_ITEM_H

            val hoverProg = dropdownHovers.getOrDefault(idx + 1000, 0f)
            if (hoverProg > 0.01f) {
                graphics.fill(fieldL, itemYPos, fieldR, itemYPos + DROPDOWN_ITEM_H, withAlpha(PEACH_GLOW, animProgress * hoverProg))
            }

            if (idx == selectedArmorIndex) {
                graphics.fill(fieldL, itemYPos, fieldL + 2, itemYPos + DROPDOWN_ITEM_H, withAlpha(ACCENT_PURPLE, animProgress))
            }

            graphics.renderItem(ItemStack(info.item), fieldL + 1, itemYPos + 1)
            graphics.drawString(font, info.displayName, fieldL + 18, itemYPos + (DROPDOWN_ITEM_H - font.lineHeight) / 2, withAlpha(TEXT_DARK, animProgress), false)
        }

        graphics.disableScissor()
    }

    private fun drawAnimatedHeadDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val animatedY = contentY + FIELD_H + 24
        val dropY = animatedY + FIELD_H + 2
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        val visibleCount = min(filteredAnimatedHeads.size, DROPDOWN_MAX)
        val dropH = (visibleCount * DROPDOWN_ITEM_H * animatedHeadDropdownAnim).toInt()
        if (dropH <= 0) return

        graphics.fill(fieldL + 2, dropY + 2, fieldR + 2, dropY + dropH + 2, withAlpha(SHADOW, animProgress * 0.3f))
        graphics.fill(fieldL - 1, dropY - 1, fieldR + 1, dropY + dropH + 1, withAlpha(PEACH_DARK, animProgress))
        graphics.fill(fieldL, dropY, fieldR, dropY + dropH, withAlpha(0xFFFFFFFF.toInt(), animProgress))

        graphics.enableScissor(fieldL, dropY, fieldR, dropY + dropH)

        for (i in 0 until visibleCount) {
            val idx = i + animatedHeadDropdownScroll
            if (idx >= filteredAnimatedHeads.size) break

            val headId = filteredAnimatedHeads[idx]
            val itemYPos = dropY + i * DROPDOWN_ITEM_H
            val displayName = ItemCustomizeManager.formatAnimatedHeadName(headId)

            val hoverProg = dropdownHovers.getOrDefault(idx + 2000, 0f)
            if (hoverProg > 0.01f) {
                graphics.fill(fieldL, itemYPos, fieldR, itemYPos + DROPDOWN_ITEM_H, withAlpha(PEACH_GLOW, animProgress * hoverProg))
            }

            if (idx == selectedAnimatedHeadIndex) {
                graphics.fill(fieldL, itemYPos, fieldL + 2, itemYPos + DROPDOWN_ITEM_H, withAlpha(ACCENT_TEAL, animProgress))
            }

            graphics.drawString(font, displayName, fieldL + 4, itemYPos + (DROPDOWN_ITEM_H - font.lineHeight) / 2, withAlpha(TEXT_DARK, animProgress), false)
        }

        graphics.disableScissor()
    }

    private fun drawTrimMaterialDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val dropY = contentY + FIELD_H + 2
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        val visibleCount = min(filteredTrimMaterials.size, DROPDOWN_MAX)
        val dropH = (visibleCount * DROPDOWN_ITEM_H * trimMaterialDropdownAnim).toInt()
        if (dropH <= 0) return

        graphics.fill(fieldL + 2, dropY + 2, fieldR + 2, dropY + dropH + 2, withAlpha(SHADOW, animProgress * 0.3f))
        graphics.fill(fieldL - 1, dropY - 1, fieldR + 1, dropY + dropH + 1, withAlpha(PEACH_DARK, animProgress))
        graphics.fill(fieldL, dropY, fieldR, dropY + dropH, withAlpha(0xFFFFFFFF.toInt(), animProgress))

        graphics.enableScissor(fieldL, dropY, fieldR, dropY + dropH)

        for (i in 0 until visibleCount) {
            val idx = i + trimMaterialDropdownScroll
            if (idx >= filteredTrimMaterials.size) break

            val material = filteredTrimMaterials[idx]
            val itemYPos = dropY + i * DROPDOWN_ITEM_H
            val displayName = material.path.replace("_", " ").replaceFirstChar { it.uppercase() }

            val hoverProg = dropdownHovers.getOrDefault(idx + 3000, 0f)
            if (hoverProg > 0.01f) {
                graphics.fill(fieldL, itemYPos, fieldR, itemYPos + DROPDOWN_ITEM_H, withAlpha(PEACH_GLOW, animProgress * hoverProg))
            }

            if (idx == selectedTrimMaterialIndex) {
                graphics.fill(fieldL, itemYPos, fieldL + 2, itemYPos + DROPDOWN_ITEM_H, withAlpha(ACCENT_GREEN, animProgress))
            }

            graphics.drawString(font, displayName, fieldL + 4, itemYPos + (DROPDOWN_ITEM_H - font.lineHeight) / 2, withAlpha(TEXT_DARK, animProgress), false)
        }

        graphics.disableScissor()
    }

    private fun drawTrimPatternDropdown(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val patternY = contentY + FIELD_H + 24
        val dropY = patternY + FIELD_H + 2
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        val visibleCount = min(filteredTrimPatterns.size, DROPDOWN_MAX)
        val dropH = (visibleCount * DROPDOWN_ITEM_H * trimPatternDropdownAnim).toInt()
        if (dropH <= 0) return

        graphics.fill(fieldL + 2, dropY + 2, fieldR + 2, dropY + dropH + 2, withAlpha(SHADOW, animProgress * 0.3f))
        graphics.fill(fieldL - 1, dropY - 1, fieldR + 1, dropY + dropH + 1, withAlpha(PEACH_DARK, animProgress))
        graphics.fill(fieldL, dropY, fieldR, dropY + dropH, withAlpha(0xFFFFFFFF.toInt(), animProgress))

        graphics.enableScissor(fieldL, dropY, fieldR, dropY + dropH)

        for (i in 0 until visibleCount) {
            val idx = i + trimPatternDropdownScroll
            if (idx >= filteredTrimPatterns.size) break

            val pattern = filteredTrimPatterns[idx]
            val itemYPos = dropY + i * DROPDOWN_ITEM_H
            val displayName = pattern.path.replace("_", " ").replaceFirstChar { it.uppercase() }

            val hoverProg = dropdownHovers.getOrDefault(idx + 4000, 0f)
            if (hoverProg > 0.01f) {
                graphics.fill(fieldL, itemYPos, fieldR, itemYPos + DROPDOWN_ITEM_H, withAlpha(PEACH_GLOW, animProgress * hoverProg))
            }

            if (idx == selectedTrimPatternIndex) {
                graphics.fill(fieldL, itemYPos, fieldL + 2, itemYPos + DROPDOWN_ITEM_H, withAlpha(ACCENT_GREEN, animProgress))
            }

            graphics.drawString(font, displayName, fieldL + 4, itemYPos + (DROPDOWN_ITEM_H - font.lineHeight) / 2, withAlpha(TEXT_DARK, animProgress), false)
        }

        graphics.disableScissor()
    }

    private fun drawResetButton(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val y = guiTop + GUI_HEIGHT - 36
        val x = guiLeft + (GUI_WIDTH - 60) / 2
        val yOff = (-resetHover * 2).toInt()
        val brightenAmt = (resetHover * 20).toInt()

        if (resetHover > 0.01f) {
            graphics.fill(x + 1, y + 2 + yOff, x + 61, y + 20 + yOff, withAlpha(SHADOW, animProgress * resetHover * 0.3f))
        }

        graphics.fill(x, y + yOff, x + 60, y + 18 + yOff, withAlpha(brighten(ACCENT_RED, brightenAmt), animProgress))

        val text = "Reset"
        val tw = font.width(text)
        graphics.drawString(font, text, x + (60 - tw) / 2, y + (18 - font.lineHeight) / 2 + yOff, withAlpha(TEXT_LIGHT, animProgress), false)
    }

    private fun drawHelpText(graphics: GuiGraphics) {
        val y = guiTop + GUI_HEIGHT - 14
        val help = "ESC to close"
        val hw = font.width(help)
        graphics.drawString(font, help, guiLeft + (GUI_WIDTH - hw) / 2, y, withAlpha(TEXT_MUTED, animProgress * 0.7f), false)
    }

    override fun mouseClicked(event: MouseButtonEvent, bl: Boolean): Boolean {
        val mouseX = event.x().toInt()
        val mouseY = event.y().toInt()
        if (event.button() != 0) return super.mouseClicked(event, bl)

        if (colorPicker != null) {
            if (colorPicker!!.isInside(mouseX, mouseY)) {
                colorPicker!!.mouseClicked(mouseX, mouseY, event.button())
                return true
            } else {
                colorPicker = null
            }
        }

        val contentY = guiTop + HEADER_H + TAB_H + PREVIEW_SIZE + 50
        val fieldL = guiLeft + PADDING
        val fieldR = guiLeft + GUI_WIDTH - PADDING

        // Tab clicking
        val tabY = guiTop + HEADER_H
        val tabs = buildTabList()
        val numTabs = tabs.size
        val tabW = (GUI_WIDTH - PADDING * 2) / numTabs

        for (i in tabs.indices) {
            val tabX = guiLeft + PADDING + i * tabW
            if (mouseX in tabX..(tabX + tabW) && mouseY in tabY..(tabY + TAB_H)) {
                currentTab = tabs[i].second
                closeAllDropdowns()
                clearAllFocus()
                playClick()
                return true
            }
        }

        // Dropdown clicks
        if (handleDropdownClicks(mouseX, mouseY, contentY, fieldL, fieldR)) return true

        // Tab-specific clicks
        when (currentTab) {
            TAB_GENERAL -> {
                val nameY = contentY
                if (mouseX in fieldL..fieldR && mouseY in nameY..(nameY + FIELD_H)) {
                    setFocus(name = true)
                    playClick()
                    return true
                }

                val itemY = nameY + FIELD_H + 24
                if (mouseX in fieldL..fieldR && mouseY in itemY..(itemY + FIELD_H)) {
                    setFocus(item = true)
                    itemDropdownOpen = itemSearchText.isNotEmpty() && filteredItems.isNotEmpty()
                    playClick()
                    return true
                }

                val glintY = itemY + FIELD_H + 24
                val toggleX = fieldR - 36
                if (mouseX in toggleX..fieldR && mouseY in glintY..(glintY + 16)) {
                    enchantGlint = !enchantGlint
                    playClick()
                    return true
                }
            }
            TAB_ARMOR -> if (originalIsArmor || isEffectiveArmor()){
                if (mouseX in fieldL..fieldR && mouseY in contentY..(contentY + FIELD_H)) {
                    setFocus(armor = true)
                    armorDropdownOpen = filteredArmor.isNotEmpty()
                    playClick()
                    return true
                }
            }
            TAB_DYE -> if (isEffectiveDyeable()) {
                val colorBoxX = fieldL + 85
                val colorBoxY = contentY
                if (mouseX in colorBoxX..(colorBoxX + 24) && mouseY in colorBoxY..(colorBoxY + 24)) {
                    openColorPicker(mouseX, mouseY)
                    playClick()
                    return true
                }

                val resetY = contentY + 34
                if (mouseX in fieldL..(fieldL + 50) && mouseY in resetY..(resetY + 16)) {
                    currentDyeColor = null
                    playClick()
                    return true
                }
            }
            TAB_HEAD -> if (originalIsHelmet || isEffectiveHelmet()) {
                val textureY = contentY
                if (mouseX in fieldL..fieldR && mouseY in textureY..(textureY + FIELD_H)) {
                    setFocus(headTexture = true)
                    playClick()
                    return true
                }

                val animatedY = textureY + FIELD_H + 24
                if (mouseX in fieldL..fieldR && mouseY in animatedY..(animatedY + FIELD_H)) {
                    setFocus(animatedHead = true)
                    updateFilteredAnimatedHeads()
                    animatedHeadDropdownOpen = filteredAnimatedHeads.isNotEmpty()
                    playClick()
                    return true
                }

                val resetY = animatedY + FIELD_H + 42
                if (mouseX in fieldL..(fieldL + 50) && mouseY in resetY..(resetY + 16)) {
                    headTextureText = ""
                    animatedHeadSearchText = ""
                    playClick()
                    return true
                }
            }
            TAB_TRIM -> if (isEffectiveTrimmable()) {
                val materialY = contentY
                if (mouseX in fieldL..fieldR && mouseY in materialY..(materialY + FIELD_H)) {
                    setFocus(trimMaterial = true)
                    updateFilteredTrimMaterials()
                    trimMaterialDropdownOpen = filteredTrimMaterials.isNotEmpty()
                    playClick()
                    return true
                }

                val patternY = materialY + FIELD_H + 24
                if (mouseX in fieldL..fieldR && mouseY in patternY..(patternY + FIELD_H)) {
                    setFocus(trimPattern = true)
                    updateFilteredTrimPatterns()
                    trimPatternDropdownOpen = filteredTrimPatterns.isNotEmpty()
                    playClick()
                    return true
                }

                val resetY = patternY + FIELD_H + 42
                if (mouseX in fieldL..(fieldL + 50) && mouseY in resetY..(resetY + 16)) {
                    trimMaterialText = ""
                    trimPatternText = ""
                    playClick()
                    return true
                }
            }
        }

        clearAllFocus()
        closeAllDropdowns()

        // Reset button
        val resetY = guiTop + GUI_HEIGHT - 36
        val resetX = guiLeft + (GUI_WIDTH - 60) / 2
        if (mouseX in resetX..(resetX + 60) && mouseY in resetY..(resetY + 18)) {
            resetCustomization()
            playClick()
            return true
        }

        return super.mouseClicked(event, bl)
    }

    private fun handleDropdownClicks(mouseX: Int, mouseY: Int, contentY: Int, fieldL: Int, fieldR: Int): Boolean {
        // Item dropdown
        if (itemDropdownOpen && filteredItems.isNotEmpty() && itemDropdownAnim > 0.5f) {
            val itemY = contentY + FIELD_H + 24
            val dropY = itemY + FIELD_H + 2
            val dropH = min(filteredItems.size, DROPDOWN_MAX) * DROPDOWN_ITEM_H
            if (mouseX in fieldL..fieldR && mouseY in dropY..(dropY + dropH)) {
                val idx = (mouseY - dropY) / DROPDOWN_ITEM_H + itemDropdownScroll
                if (idx in filteredItems.indices) {
                    selectItem(idx)
                }
                return true
            }
        }

        // Armor dropdown
        if (armorDropdownOpen && filteredArmor.isNotEmpty() && armorDropdownAnim > 0.5f) {
            val dropY = contentY + FIELD_H + 2
            val dropH = min(filteredArmor.size, DROPDOWN_MAX) * DROPDOWN_ITEM_H
            if (mouseX in fieldL..fieldR && mouseY in dropY..(dropY + dropH)) {
                val idx = (mouseY - dropY) / DROPDOWN_ITEM_H + armorDropdownScroll
                if (idx in filteredArmor.indices) {
                    selectArmor(idx)
                }
                return true
            }
        }

        // Animated head dropdown
        if (animatedHeadDropdownOpen && filteredAnimatedHeads.isNotEmpty() && animatedHeadDropdownAnim > 0.5f) {
            val animatedY = contentY + FIELD_H + 24
            val dropY = animatedY + FIELD_H + 2
            val dropH = min(filteredAnimatedHeads.size, DROPDOWN_MAX) * DROPDOWN_ITEM_H
            if (mouseX in fieldL..fieldR && mouseY in dropY..(dropY + dropH)) {
                val idx = (mouseY - dropY) / DROPDOWN_ITEM_H + animatedHeadDropdownScroll
                if (idx in filteredAnimatedHeads.indices) {
                    selectAnimatedHead(idx)
                }
                return true
            }
        }

        // Trim material dropdown
        if (trimMaterialDropdownOpen && filteredTrimMaterials.isNotEmpty() && trimMaterialDropdownAnim > 0.5f) {
            val dropY = contentY + FIELD_H + 2
            val dropH = min(filteredTrimMaterials.size, DROPDOWN_MAX) * DROPDOWN_ITEM_H
            if (mouseX in fieldL..fieldR && mouseY in dropY..(dropY + dropH)) {
                val idx = (mouseY - dropY) / DROPDOWN_ITEM_H + trimMaterialDropdownScroll
                if (idx in filteredTrimMaterials.indices) {
                    selectTrimMaterial(idx)
                }
                return true
            }
        }

        // Trim pattern dropdown
        if (trimPatternDropdownOpen && filteredTrimPatterns.isNotEmpty() && trimPatternDropdownAnim > 0.5f) {
            val patternY = contentY + FIELD_H + 24
            val dropY = patternY + FIELD_H + 2
            val dropH = min(filteredTrimPatterns.size, DROPDOWN_MAX) * DROPDOWN_ITEM_H
            if (mouseX in fieldL..fieldR && mouseY in dropY..(dropY + dropH)) {
                val idx = (mouseY - dropY) / DROPDOWN_ITEM_H + trimPatternDropdownScroll
                if (idx in filteredTrimPatterns.indices) {
                    selectTrimPattern(idx)
                }
                return true
            }
        }

        return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        colorPicker?.mouseDragged(event.x().toInt(), event.y().toInt())
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        colorPicker?.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (itemDropdownOpen && filteredItems.size > DROPDOWN_MAX) {
            val maxScroll = filteredItems.size - DROPDOWN_MAX
            itemDropdownScroll = (itemDropdownScroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
            return true
        }
        if (armorDropdownOpen && filteredArmor.size > DROPDOWN_MAX) {
            val maxScroll = filteredArmor.size - DROPDOWN_MAX
            armorDropdownScroll = (armorDropdownScroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
            return true
        }
        if (animatedHeadDropdownOpen && filteredAnimatedHeads.size > DROPDOWN_MAX) {
            val maxScroll = filteredAnimatedHeads.size - DROPDOWN_MAX
            animatedHeadDropdownScroll = (animatedHeadDropdownScroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
            return true
        }
        if (trimMaterialDropdownOpen && filteredTrimMaterials.size > DROPDOWN_MAX) {
            val maxScroll = filteredTrimMaterials.size - DROPDOWN_MAX
            trimMaterialDropdownScroll = (trimMaterialDropdownScroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
            return true
        }
        if (trimPatternDropdownOpen && filteredTrimPatterns.size > DROPDOWN_MAX) {
            val maxScroll = filteredTrimPatterns.size - DROPDOWN_MAX
            trimPatternDropdownScroll = (trimPatternDropdownScroll - verticalAmount.toInt()).coerceIn(0, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (colorPicker?.keyPressed(event.key()) == true) return true

        val isCtrlHeld = mc.hasControlDown()

        if (event.key() == GLFW.GLFW_KEY_V && isCtrlHeld) {
            val clipboard = mc.keyboardHandler.clipboard
            if (clipboard.isNotEmpty()) {
                when {
                    nameFocused -> {
                        nameText = nameText.substring(0, nameCursor) + clipboard + nameText.substring(nameCursor)
                        nameCursor += clipboard.length
                        return true
                    }
                    headTextureFocused -> {
                        val filtered = clipboard.filter { isValidBase64Char(it) }
                        headTextureText = headTextureText.substring(0, headTextureCursor) + filtered + headTextureText.substring(headTextureCursor)
                        headTextureCursor += filtered.length
                        return true
                    }
                    itemFocused -> {
                        val filtered = clipboard.filter { isValidItemChar(it) }
                        itemSearchText = itemSearchText.substring(0, itemCursor) + filtered + itemSearchText.substring(itemCursor)
                        itemCursor += filtered.length
                        onItemSearchChanged()
                        return true
                    }
                    armorFocused -> {
                        val filtered = clipboard.filter { isValidItemChar(it) }
                        armorSearchText = armorSearchText.substring(0, armorCursor) + filtered + armorSearchText.substring(armorCursor)
                        armorCursor += filtered.length
                        onArmorSearchChanged()
                        return true
                    }
                    animatedHeadFocused -> {
                        val filtered = clipboard.filter { isValidItemChar(it) }
                        animatedHeadSearchText = animatedHeadSearchText.substring(0, animatedHeadCursor) + filtered + animatedHeadSearchText.substring(animatedHeadCursor)
                        animatedHeadCursor += filtered.length
                        onAnimatedHeadSearchChanged()
                        return true
                    }
                    trimMaterialFocused -> {
                        val filtered = clipboard.filter { isValidItemChar(it) }
                        trimMaterialText = trimMaterialText.substring(0, trimMaterialCursor) + filtered + trimMaterialText.substring(trimMaterialCursor)
                        trimMaterialCursor += filtered.length
                        onTrimMaterialSearchChanged()
                        return true
                    }
                    trimPatternFocused -> {
                        val filtered = clipboard.filter { isValidItemChar(it) }
                        trimPatternText = trimPatternText.substring(0, trimPatternCursor) + filtered + trimPatternText.substring(trimPatternCursor)
                        trimPatternCursor += filtered.length
                        onTrimPatternSearchChanged()
                        return true
                    }
                }
            }
        }

        if (event.key() == 256) {
            if (colorPicker != null) {
                colorPicker = null
                return true
            }
            if (hasAnyFocus()) {
                clearAllFocus()
                closeAllDropdowns()
                return true
            }
            onClose()
            return true
        }

        if (hasAnyFocus()) {
            if (mc.options.keyInventory.matches(event)) return true
        } else {
            if (mc.options.keyInventory.matches(event)) { onClose(); return true }
        }

        // Handle arrow keys for dropdowns
        handleDropdownNavigation(event)

        // Handle text input for focused fields
        handleTextFieldKeyPress(event)

        return super.keyPressed(event)
    }

    private fun handleDropdownNavigation(event: KeyEvent) {
        if (itemDropdownOpen && filteredItems.isNotEmpty()) {
            when (event.key()) {
                265 -> { selectedItemIndex = (selectedItemIndex - 1).coerceAtLeast(0); adjustItemScroll() }
                264 -> { selectedItemIndex = (selectedItemIndex + 1).coerceAtMost(filteredItems.size - 1); adjustItemScroll() }
            }
        }
        if (armorDropdownOpen && filteredArmor.isNotEmpty()) {
            when (event.key()) {
                265 -> { selectedArmorIndex = (selectedArmorIndex - 1).coerceAtLeast(0); adjustArmorScroll() }
                264 -> { selectedArmorIndex = (selectedArmorIndex + 1).coerceAtMost(filteredArmor.size - 1); adjustArmorScroll() }
            }
        }
        if (animatedHeadDropdownOpen && filteredAnimatedHeads.isNotEmpty()) {
            when (event.key()) {
                265 -> { selectedAnimatedHeadIndex = (selectedAnimatedHeadIndex - 1).coerceAtLeast(0); adjustAnimatedHeadScroll() }
                264 -> { selectedAnimatedHeadIndex = (selectedAnimatedHeadIndex + 1).coerceAtMost(filteredAnimatedHeads.size - 1); adjustAnimatedHeadScroll() }
            }
        }
        if (trimMaterialDropdownOpen && filteredTrimMaterials.isNotEmpty()) {
            when (event.key()) {
                265 -> { selectedTrimMaterialIndex = (selectedTrimMaterialIndex - 1).coerceAtLeast(0); adjustTrimMaterialScroll() }
                264 -> { selectedTrimMaterialIndex = (selectedTrimMaterialIndex + 1).coerceAtMost(filteredTrimMaterials.size - 1); adjustTrimMaterialScroll() }
            }
        }
        if (trimPatternDropdownOpen && filteredTrimPatterns.isNotEmpty()) {
            when (event.key()) {
                265 -> { selectedTrimPatternIndex = (selectedTrimPatternIndex - 1).coerceAtLeast(0); adjustTrimPatternScroll() }
                264 -> { selectedTrimPatternIndex = (selectedTrimPatternIndex + 1).coerceAtMost(filteredTrimPatterns.size - 1); adjustTrimPatternScroll() }
            }
        }

        // Enter key for selection
        if (event.key() == 257 || event.key() == 335) {
            when {
                itemDropdownOpen && filteredItems.isNotEmpty() -> selectItem(selectedItemIndex.coerceIn(0, filteredItems.size - 1))
                armorDropdownOpen && filteredArmor.isNotEmpty() -> selectArmor(selectedArmorIndex.coerceIn(0, filteredArmor.size - 1))
                animatedHeadDropdownOpen && filteredAnimatedHeads.isNotEmpty() -> selectAnimatedHead(selectedAnimatedHeadIndex.coerceIn(0, filteredAnimatedHeads.size - 1))
                trimMaterialDropdownOpen && filteredTrimMaterials.isNotEmpty() -> selectTrimMaterial(selectedTrimMaterialIndex.coerceIn(0, filteredTrimMaterials.size - 1))
                trimPatternDropdownOpen && filteredTrimPatterns.isNotEmpty() -> selectTrimPattern(selectedTrimPatternIndex.coerceIn(0, filteredTrimPatterns.size - 1))
                else -> {
                    clearAllFocus()
                    closeAllDropdowns()
                }
            }
        }
    }

    private fun handleTextFieldKeyPress(event: KeyEvent) {
        if (nameFocused) handleFieldKeyPress(event, { nameText = it }, { nameText }, ::nameCursor)
        if (itemFocused) handleFieldKeyPress(event, { itemSearchText = it; onItemSearchChanged() }, { itemSearchText }, ::itemCursor)
        if (armorFocused) handleFieldKeyPress(event, { armorSearchText = it; onArmorSearchChanged() }, { armorSearchText }, ::armorCursor)
        if (headTextureFocused) handleFieldKeyPress(event, { headTextureText = it }, { headTextureText }, ::headTextureCursor)
        if (animatedHeadFocused) handleFieldKeyPress(event, { animatedHeadSearchText = it; onAnimatedHeadSearchChanged() }, { animatedHeadSearchText }, ::animatedHeadCursor)
        if (trimMaterialFocused) handleFieldKeyPress(event, { trimMaterialText = it; onTrimMaterialSearchChanged() }, { trimMaterialText }, ::trimMaterialCursor)
        if (trimPatternFocused) handleFieldKeyPress(event, { trimPatternText = it; onTrimPatternSearchChanged() }, { trimPatternText }, ::trimPatternCursor)
    }

    private fun handleFieldKeyPress(event: KeyEvent, setter: (String) -> Unit, getter: () -> String, cursorProp: kotlin.reflect.KMutableProperty0<Int>) {
        val text = getter()
        val cursor = cursorProp.get()
        when (event.key()) {
            259 -> { // Backspace
                if (cursor > 0) {
                    setter(text.removeRange(cursor - 1, cursor))
                    cursorProp.set(cursor - 1)
                }
            }
            261 -> { // Delete
                if (cursor < text.length) {
                    setter(text.removeRange(cursor, cursor + 1))
                }
            }
            263 -> cursorProp.set((cursor - 1).coerceAtLeast(0)) // Left
            262 -> cursorProp.set((cursor + 1).coerceAtMost(text.length)) // Right
            268 -> cursorProp.set(0) // Home
            269 -> cursorProp.set(text.length) // End
        }
    }

    private fun isValidItemChar(char: Char) = char.isLetterOrDigit() || char == '_' || char == ':' || char == ' '

    // Add a new function for base64 characters
    private fun isValidBase64Char(char: Char) = char.isLetterOrDigit() || char == '+' || char == '/' || char == '='

    // Update charTyped to use the right validation for each field:
    override fun charTyped(event: net.minecraft.client.input.CharacterEvent): Boolean {
        if (colorPicker?.charTyped(event.codepoint().toChar()) == true) return true

        val char = event.codepoint().toChar()
        if (char.code < 32) return super.charTyped(event)

        if (nameFocused) {
            nameText = nameText.substring(0, nameCursor) + char + nameText.substring(nameCursor)
            nameCursor++
            return true
        }
        if (itemFocused && isValidItemChar(char)) {
            itemSearchText = itemSearchText.substring(0, itemCursor) + char + itemSearchText.substring(itemCursor)
            itemCursor++
            onItemSearchChanged()
            return true
        }
        if (armorFocused && isValidItemChar(char)) {
            armorSearchText = armorSearchText.substring(0, armorCursor) + char + armorSearchText.substring(armorCursor)
            armorCursor++
            onArmorSearchChanged()
            return true
        }
        // HEAD TEXTURE FIELD - Allow all Base64 characters
        if (headTextureFocused && isValidBase64Char(char)) {
            headTextureText = headTextureText.substring(0, headTextureCursor) + char + headTextureText.substring(headTextureCursor)
            headTextureCursor++
            return true
        }
        if (animatedHeadFocused && isValidItemChar(char)) {
            animatedHeadSearchText = animatedHeadSearchText.substring(0, animatedHeadCursor) + char + animatedHeadSearchText.substring(animatedHeadCursor)
            animatedHeadCursor++
            onAnimatedHeadSearchChanged()
            return true
        }
        if (trimMaterialFocused && isValidItemChar(char)) {
            trimMaterialText = trimMaterialText.substring(0, trimMaterialCursor) + char + trimMaterialText.substring(trimMaterialCursor)
            trimMaterialCursor++
            onTrimMaterialSearchChanged()
            return true
        }
        if (trimPatternFocused && isValidItemChar(char)) {
            trimPatternText = trimPatternText.substring(0, trimPatternCursor) + char + trimPatternText.substring(trimPatternCursor)
            trimPatternCursor++
            onTrimPatternSearchChanged()
            return true
        }

        return super.charTyped(event)
    }


    // Selection methods
    private fun selectItem(index: Int) {
        if (index in filteredItems.indices) {
            val item = filteredItems[index]
            val key = BuiltInRegistries.ITEM.getKey(item)?.toString()?.removePrefix("minecraft:") ?: return
            itemSearchText = key
            itemCursor = itemSearchText.length
            itemDropdownOpen = false
            updatePreviewStack()
            playClick()
        }
    }

    private fun selectArmor(index: Int) {
        if (index in filteredArmor.indices) {
            val info = filteredArmor[index]
            armorSearchText = info.id.removePrefix("minecraft:")
            armorCursor = armorSearchText.length
            armorDropdownOpen = false
            playClick()
        }
    }

    private fun selectAnimatedHead(index: Int) {
        if (index in filteredAnimatedHeads.indices) {
            animatedHeadSearchText = filteredAnimatedHeads[index]
            animatedHeadCursor = animatedHeadSearchText.length
            animatedHeadDropdownOpen = false
            playClick()
        }
    }

    private fun selectTrimMaterial(index: Int) {
        if (index in filteredTrimMaterials.indices) {
            val material = filteredTrimMaterials[index]
            trimMaterialText = material.path
            trimMaterialCursor = trimMaterialText.length
            trimMaterialDropdownOpen = false
            playClick()
        }
    }

    private fun selectTrimPattern(index: Int) {
        if (index in filteredTrimPatterns.indices) {
            val pattern = filteredTrimPatterns[index]
            trimPatternText = pattern.path
            trimPatternCursor = trimPatternText.length
            trimPatternDropdownOpen = false
            playClick()
        }
    }

    // Search change handlers
    private fun onItemSearchChanged() {
        updateFilteredItems()
        itemDropdownOpen = filteredItems.isNotEmpty()
        itemDropdownScroll = 0
        selectedItemIndex = if (filteredItems.isNotEmpty()) 0 else -1

        updateEffectiveItemData()
    }

    private fun updateEffectiveItemData() {
        // Update trim lists if now trimmable
        if (isEffectiveTrimmable()) {
            ItemCustomizeManager.ensureTrimsInitialized()
            filteredTrimMaterials = ItemCustomizeManager.getAllTrimMaterials()
            filteredTrimPatterns = ItemCustomizeManager.getAllTrimPatterns()
        }

        // Update animated heads list if now a helmet
        if (isEffectiveHelmet()) {
            filteredAnimatedHeads = ItemCustomizeManager.getAnimatedHeadIds().toList()
        }

        // If current tab is no longer valid, switch to General
        val validTabs = buildTabList().map { it.second }
        if (currentTab !in validTabs) {
            currentTab = TAB_GENERAL
        }
    }

    private fun onArmorSearchChanged() {
        updateFilteredArmor()
        armorDropdownOpen = filteredArmor.isNotEmpty()
        armorDropdownScroll = 0
        selectedArmorIndex = if (filteredArmor.isNotEmpty()) 0 else -1
    }

    private fun onAnimatedHeadSearchChanged() {
        updateFilteredAnimatedHeads()
        animatedHeadDropdownOpen = filteredAnimatedHeads.isNotEmpty()
        animatedHeadDropdownScroll = 0
        selectedAnimatedHeadIndex = if (filteredAnimatedHeads.isNotEmpty()) 0 else -1
    }

    private fun onTrimMaterialSearchChanged() {
        updateFilteredTrimMaterials()
        trimMaterialDropdownOpen = filteredTrimMaterials.isNotEmpty()
        trimMaterialDropdownScroll = 0
        selectedTrimMaterialIndex = if (filteredTrimMaterials.isNotEmpty()) 0 else -1
    }

    private fun onTrimPatternSearchChanged() {
        updateFilteredTrimPatterns()
        trimPatternDropdownOpen = filteredTrimPatterns.isNotEmpty()
        trimPatternDropdownScroll = 0
        selectedTrimPatternIndex = if (filteredTrimPatterns.isNotEmpty()) 0 else -1
    }

    // Scroll adjustments
    private fun adjustItemScroll() {
        if (selectedItemIndex < itemDropdownScroll) itemDropdownScroll = selectedItemIndex
        if (selectedItemIndex >= itemDropdownScroll + DROPDOWN_MAX) itemDropdownScroll = selectedItemIndex - DROPDOWN_MAX + 1
    }

    private fun adjustArmorScroll() {
        if (selectedArmorIndex < armorDropdownScroll) armorDropdownScroll = selectedArmorIndex
        if (selectedArmorIndex >= armorDropdownScroll + DROPDOWN_MAX) armorDropdownScroll = selectedArmorIndex - DROPDOWN_MAX + 1
    }

    private fun adjustAnimatedHeadScroll() {
        if (selectedAnimatedHeadIndex < animatedHeadDropdownScroll) animatedHeadDropdownScroll = selectedAnimatedHeadIndex
        if (selectedAnimatedHeadIndex >= animatedHeadDropdownScroll + DROPDOWN_MAX) animatedHeadDropdownScroll = selectedAnimatedHeadIndex - DROPDOWN_MAX + 1
    }

    private fun adjustTrimMaterialScroll() {
        if (selectedTrimMaterialIndex < trimMaterialDropdownScroll) trimMaterialDropdownScroll = selectedTrimMaterialIndex
        if (selectedTrimMaterialIndex >= trimMaterialDropdownScroll + DROPDOWN_MAX) trimMaterialDropdownScroll = selectedTrimMaterialIndex - DROPDOWN_MAX + 1
    }

    private fun adjustTrimPatternScroll() {
        if (selectedTrimPatternIndex < trimPatternDropdownScroll) trimPatternDropdownScroll = selectedTrimPatternIndex
        if (selectedTrimPatternIndex >= trimPatternDropdownScroll + DROPDOWN_MAX) trimPatternDropdownScroll = selectedTrimPatternIndex - DROPDOWN_MAX + 1
    }

    private fun setFocus(
        name: Boolean = false,
        item: Boolean = false,
        armor: Boolean = false,
        headTexture: Boolean = false,
        animatedHead: Boolean = false,
        trimMaterial: Boolean = false,
        trimPattern: Boolean = false
    ) {
        nameFocused = name
        itemFocused = item
        armorFocused = armor
        headTextureFocused = headTexture
        animatedHeadFocused = animatedHead
        trimMaterialFocused = trimMaterial
        trimPatternFocused = trimPattern
        closeAllDropdowns()
    }

    private fun clearAllFocus() {
        nameFocused = false
        itemFocused = false
        armorFocused = false
        headTextureFocused = false
        animatedHeadFocused = false
        trimMaterialFocused = false
        trimPatternFocused = false
    }

    private fun hasAnyFocus() = nameFocused || itemFocused || armorFocused || headTextureFocused || animatedHeadFocused || trimMaterialFocused || trimPatternFocused

    private fun closeAllDropdowns() {
        itemDropdownOpen = false
        armorDropdownOpen = false
        animatedHeadDropdownOpen = false
        trimMaterialDropdownOpen = false
        trimPatternDropdownOpen = false
    }

    private fun openColorPicker(x: Int, y: Int) {
        val currentColor = currentDyeColor ?: 0xFFA06540.toInt()
        val r = (currentColor shr 16) and 0xFF
        val g = (currentColor shr 8) and 0xFF
        val b = currentColor and 0xFF

        colorPicker = ColorPickerPopup(r, g, b, 255F,
            onApply = { nr, ng, nb, _ ->
                currentDyeColor = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
                colorPicker = null
            },
            onCancel = { colorPicker = null }
        ).apply {
            this.x = x.coerceIn(guiLeft, guiLeft + GUI_WIDTH - 160)
            this.y = y.coerceIn(guiTop, guiTop + GUI_HEIGHT - 180)
        }
    }

    private fun resetCustomization() {
        nameText = ""; nameCursor = 0
        itemSearchText = ""; itemCursor = 0
        armorSearchText = ""; armorCursor = 0
        headTextureText = ""; headTextureCursor = 0
        animatedHeadSearchText = ""; animatedHeadCursor = 0
        trimMaterialText = ""; trimMaterialCursor = 0
        trimPatternText = ""; trimPatternCursor = 0
        enchantGlint = stack.hasFoil()
        currentDyeColor = null
        previewStack = stack.copy()
        ItemCustomizeManager.removeItemData(itemUUID)
        mc.player?.displayClientMessage(Component.literal("§aCustomization reset!"), true)
    }

    private fun processColorCodes(text: String): String {
        var result = text
        result = result.replace("&&", "§")
        result = result.replace("**", "✪")
        for (i in 1..9) {
            result = result.replace("*$i", "➊➋➌➍➎➏➐➑➒"[i - 1].toString())
        }
        return result
    }

    private fun easeOutCubic(t: Float) = 1f - (1f - t).pow(3)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun lerpColor(colorA: Int, colorB: Int, t: Float): Int {
        val aA = (colorA ushr 24) and 0xFF; val rA = (colorA ushr 16) and 0xFF
        val gA = (colorA ushr 8) and 0xFF; val bA = colorA and 0xFF
        val aB = (colorB ushr 24) and 0xFF; val rB = (colorB ushr 16) and 0xFF
        val gB = (colorB ushr 8) and 0xFF; val bB = colorB and 0xFF
        val a = (aA + (aB - aA) * t).toInt().coerceIn(0, 255)
        val r = (rA + (rB - rA) * t).toInt().coerceIn(0, 255)
        val g = (gA + (gB - gA) * t).toInt().coerceIn(0, 255)
        val b = (bA + (bB - bA) * t).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = ((color ushr 24) and 0xFF) * alpha
        return (a.toInt().coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)
    }

    private fun brighten(color: Int, amount: Int): Int {
        val a = (color ushr 24) and 0xFF
        var r = (color ushr 16) and 0xFF
        var g = (color ushr 8) and 0xFF
        var b = color and 0xFF
        r = (r + amount).coerceIn(0, 255)
        g = (g + amount).coerceIn(0, 255)
        b = (b + amount).coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun playClick() {
        mc.soundManager.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f))
    }
}