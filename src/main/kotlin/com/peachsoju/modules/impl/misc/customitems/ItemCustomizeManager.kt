package com.peachsoju.modules.impl.misc.customitems

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.PropertyMap
import com.peachsoju.PeachSoju.mc
import com.peachsoju.utils.handlers.FileHandler
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.equipment.trim.ArmorTrim
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimPattern
import java.io.InputStream
import java.util.*

object ItemCustomizeManager {

    private const val FILE_NAME = "itemcustomize.json"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private var itemDataCache: MutableMap<String, ItemData> = mutableMapOf()
    private var loaded = false

    private val armorBySlot: Map<ArmorSlot, List<ArmorInfo>> by lazy { loadArmorItems() }

    // Armor Trim Cache
    private val trimsCache: MutableMap<ArmorTrimId, ArmorTrim> = mutableMapOf()
    private var trimsInitialized = false

    // Animated Head Cache
    private val animatedHeads: MutableMap<String, AnimatedHead> = mutableMapOf()
    private val headStateTrackers: MutableMap<AnimatedHead, AnimatedHeadStateTracker> = mutableMapOf()
    private var animatedHeadsLoaded = false
    private var tickCounter = 0

    data class ArmorInfo(
        val item: Item,
        val id: String,
        val displayName: String,
        val material: String
    )

    data class ArmorTrimId(
        val material: ResourceLocation,
        val pattern: ResourceLocation
    )

    data class AnimatedHead(
        val tickThreshold: Int,
        val frames: List<ResolvableProfile>
    )

    private class AnimatedHeadStateTracker(private val head: AnimatedHead) {
        var lastRecordedTick = 0
        private var currentFrame: ResolvableProfile = head.frames.first()

        fun advanceTo(ticks: Int) {
            val advancedIndex = ticks / head.tickThreshold
            currentFrame = head.frames[advancedIndex % head.frames.size]
        }

        fun getCurrentFrame(): ResolvableProfile = currentFrame
    }

    fun load() {
        try {
            val json = FileHandler.readFromFile(FILE_NAME)
            itemDataCache.clear()
            json.entrySet().forEach { (uuid, element) ->
                if (element.isJsonObject) {
                    itemDataCache[uuid] = ItemData.fromJson(element.asJsonObject)
                }
            }
            loaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            itemDataCache.clear()
            loaded = true
        }

        // Initialize trim cache
        initializeTrimsCache()
    }

    fun save() {
        try {
            val json = JsonObject()
            itemDataCache.forEach { (uuid, data) ->
                if (!data.isEmpty()) {
                    json.add(uuid, data.toJson())
                }
            }
            FileHandler.writeToFile(FILE_NAME, json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== TRIM CACHE ====================

    private fun initializeTrimsCache() {
        if (trimsInitialized) return
        try {
            val world = mc.level ?: return
            val wrapperLookup = world.registryAccess()

            trimsCache.clear()
            val materialRegistry = wrapperLookup.lookupOrThrow(Registries.TRIM_MATERIAL)
            val patternRegistry = wrapperLookup.lookupOrThrow(Registries.TRIM_PATTERN)

            materialRegistry.listElements().forEach { materialRef ->
                patternRegistry.listElements().forEach { patternRef ->
                    val trim = ArmorTrim(materialRef, patternRef)
                    val trimId = ArmorTrimId(
                        materialRef.key().location(),
                        patternRef.key().location()
                    )
                    trimsCache[trimId] = trim
                }
            }

            trimsInitialized = true
            println("[PeachSoju] Successfully cached ${trimsCache.size} armor trims!")
        } catch (e: Exception) {
            println("[PeachSoju] Failed to initialize armor trims cache: ${e.message}")
        }
    }

    fun ensureTrimsInitialized() {
        if (!trimsInitialized) {
            initializeTrimsCache()
        }
    }

    fun getArmorTrim(materialId: ResourceLocation, patternId: ResourceLocation): ArmorTrim? {
        ensureTrimsInitialized()
        return trimsCache[ArmorTrimId(materialId, patternId)]
    }

    fun getAllTrimMaterials(): List<ResourceLocation> {
        ensureTrimsInitialized()
        return trimsCache.keys.map { it.material }.distinct().sorted()
    }

    fun getAllTrimPatterns(): List<ResourceLocation> {
        ensureTrimsInitialized()
        return trimsCache.keys.map { it.pattern }.distinct().sorted()
    }

    fun isTrimValid(materialId: ResourceLocation, patternId: ResourceLocation): Boolean {
        ensureTrimsInitialized()
        return trimsCache.containsKey(ArmorTrimId(materialId, patternId))
    }

    // ==================== ANIMATED HEADS ====================

    fun incrementTick() {
        tickCounter++
    }

    fun loadAnimatedHeads(jsonData: String) {
        try {
            println("[PeachSoju] Parsing JSON data of length: ${jsonData.length}")
            val jsonObj = JsonParser.parseString(jsonData).asJsonObject
            println("[PeachSoju] Root keys: ${jsonObj.keySet()}")

            val skinsObj = jsonObj.getAsJsonObject("skins")
            if (skinsObj == null) {
                println("[PeachSoju] ERROR: 'skins' object is null! Trying root object...")
                // Maybe it's at root level
                return
            }

            println("[PeachSoju] Found ${skinsObj.entrySet().size} entries in skins")

            animatedHeads.clear()
            headStateTrackers.clear()

            var successCount = 0
            var failCount = 0
            var noFramesCount = 0

            skinsObj.entrySet().take(5).forEach { (id, element) ->  // Just test first 5
                println("[PeachSoju] Processing: $id")
                if (element.isJsonObject) {
                    val headObj = element.asJsonObject
                    val ticks = headObj.get("ticks")?.asInt?.coerceAtLeast(1) ?: 1
                    println("[PeachSoju]   ticks: $ticks")

                    val texturesArray = headObj.getAsJsonArray("textures")
                    if (texturesArray == null) {
                        println("[PeachSoju]   ERROR: textures array is null")
                        return@forEach
                    }
                    println("[PeachSoju]   textures count: ${texturesArray.size()}")

                    val frames = texturesArray.mapNotNull { textureElement ->
                        try {
                            val textureStr = textureElement.asString
                            println("[PeachSoju]   Parsing texture: ${textureStr.take(50)}...")
                            val result = parseHeadTexture(textureStr)
                            if (result == null) {
                                println("[PeachSoju]   ERROR: parseHeadTexture returned null")
                            }
                            result
                        } catch (e: Exception) {
                            println("[PeachSoju]   Exception parsing texture: ${e.message}")
                            null
                        }
                    }

                    println("[PeachSoju]   Valid frames: ${frames.size}")
                    if (frames.isNotEmpty()) {
                        animatedHeads[id] = AnimatedHead(ticks, frames)
                        successCount++
                    } else {
                        noFramesCount++
                    }
                }
            }

            // Now process the rest without logging
            skinsObj.entrySet().drop(5).forEach { (id, element) ->
                if (element.isJsonObject) {
                    val headObj = element.asJsonObject
                    val ticks = headObj.get("ticks")?.asInt?.coerceAtLeast(1) ?: 1
                    val texturesArray = headObj.getAsJsonArray("textures") ?: return@forEach
                    val frames = texturesArray.mapNotNull { parseHeadTexture(it.asString) }
                    if (frames.isNotEmpty()) {
                        animatedHeads[id] = AnimatedHead(ticks, frames)
                        successCount++
                    } else {
                        noFramesCount++
                    }
                }
            }

            animatedHeadsLoaded = true
            println("[PeachSoju] Loaded $successCount animated heads, $noFramesCount had no valid frames")
        } catch (e: Exception) {
            println("[PeachSoju] Failed to load animated heads: ${e.message}")
            e.printStackTrace()
        }
    }

    fun loadAnimatedHeadsFromResources() {
        try {
            println("[PeachSoju] Attempting to load animated heads from resources...")
            val stream = ItemCustomizeManager::class.java.getResourceAsStream("/assets/peachsoju/animatedskulls.json")
            if (stream != null) {
                val data = String(stream.readAllBytes())
                println("[PeachSoju] Read ${data.length} bytes from animatedskulls.json")
                loadAnimatedHeads(data)
                println("[PeachSoju] After loading, animatedHeads.size = ${animatedHeads.size}")
            } else {
                println("[PeachSoju] ERROR: Could not find animatedskulls.json in resources!")
            }
        } catch (e: Exception) {
            println("[PeachSoju] Failed to load animated heads: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun parseHeadTexture(textureStr: String): ResolvableProfile? {
        return try {
            val parts = textureStr.split(":", limit = 2)
            if (parts.size >= 2) {
                val uuid = UUID.fromString(parts[0])
                val texture = parts[1]

                // Build PropertyMap
                val properties = com.google.common.collect.ImmutableMultimap.of(
                    "textures",
                    com.mojang.authlib.properties.Property("textures", texture)
                )
                val propertyMap = com.mojang.authlib.properties.PropertyMap(properties)

                // Create GameProfile with 3 args
                val profile = GameProfile(uuid, "custom", propertyMap)
                ResolvableProfile.createResolved(profile)
            } else {
                null
            }
        } catch (e: Exception) {
            println("[PeachSoju] parseHeadTexture error: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    fun getAnimatedHeadIds(): Set<String> = animatedHeads.keys

    fun formatAnimatedHeadName(id: String): String {
        return id.replace('_', ' ').split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun animateHeadTexture(id: String): ResolvableProfile? {
        val head = animatedHeads[id] ?: return null

        val tracker = headStateTrackers.getOrPut(head) { AnimatedHeadStateTracker(head) }

        if (tracker.lastRecordedTick == tickCounter) {
            return tracker.getCurrentFrame()
        }

        tracker.advanceTo(tickCounter)
        tracker.lastRecordedTick = tickCounter

        return tracker.getCurrentFrame()
    }

    // ==================== STATIC HEAD TEXTURES ====================

    fun createHeadProfile(textureBase64: String): ResolvableProfile? {
        return try {
            val uuid = UUID.randomUUID()

            val properties = com.google.common.collect.ImmutableMultimap.of(
                "textures",
                com.mojang.authlib.properties.Property("textures", textureBase64)
            )
            val propertyMap = com.mojang.authlib.properties.PropertyMap(properties)

            val profile = GameProfile(uuid, "custom", propertyMap)
            ResolvableProfile.createResolved(profile)
        } catch (e: Exception) {
            println("[PeachSoju] createHeadProfile error: ${e.message}")
            null
        }
    }

    // ==================== EXISTING METHODS ====================

    fun getDataForUUID(uuid: String): ItemData? {
        if (!loaded) load()
        return itemDataCache[uuid]
    }

    fun getCustomItemString(stack: ItemStack): String? {
        val data = getDataForItem(stack) ?: return null
        return data.customItem
    }

    fun getDataForItem(stack: ItemStack): ItemData? {
        val uuid = getUUIDForItem(stack) ?: return null
        return getDataForUUID(uuid)
    }

    fun putItemData(uuid: String, data: ItemData) {
        if (!loaded) load()
        if (data.isEmpty()) {
            itemDataCache.remove(uuid)
        } else {
            itemDataCache[uuid] = data
        }
        save()
    }

    fun removeItemData(uuid: String) {
        if (!loaded) load()
        itemDataCache.remove(uuid)
        save()
    }

    fun getUUIDForItem(stack: ItemStack): String? {
        if (stack.isEmpty) return null
        try {
            val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return null
            val nbt = customData.copyTag()
            val extraAttrs = nbt.getCompound("ExtraAttributes")
            if (extraAttrs.isPresent) {
                val uuid = extraAttrs.get().getString("uuid")
                if (uuid.isPresent && uuid.get().isNotEmpty()) {
                    return uuid.get()
                }
            }
            val directUuid = nbt.getString("uuid")
            if (directUuid.isPresent && directUuid.get().isNotEmpty()) {
                return directUuid.get()
            }
        } catch (e: Exception) { }
        return null
    }

    fun hasUUID(stack: ItemStack): Boolean = getUUIDForItem(stack) != null

    fun getDisplayName(stack: ItemStack): String? {
        val data = getDataForItem(stack) ?: return null
        val customName = data.customName ?: return null
        val prefix = data.customNamePrefix ?: ""
        return prefix + processColorCodes(customName)
    }

    fun shouldShowGlint(stack: ItemStack): Boolean? {
        val data = getDataForItem(stack) ?: return null
        if (!data.overrideEnchantGlint) return null
        return data.enchantGlintValue
    }

    fun getLeatherColor(stack: ItemStack): Int? {
        val data = getDataForItem(stack) ?: return null
        val animatedColors = data.animatedLeatherColours
        if (animatedColors != null && animatedColors.isNotEmpty()) {
            val ticks = data.animatedDyeTicks.coerceAtLeast(1)
            val player = mc.player ?: return null
            return when (data.dyeMode) {
                DyeMode.CYCLING -> {
                    val index = (player.tickCount / ticks) % animatedColors.size
                    parseSpecialColor(animatedColors[index])
                }
                DyeMode.GRADIENT -> {
                    val index = (player.tickCount / ticks) % animatedColors.size
                    val nextIndex = (index + 1) % animatedColors.size
                    val progress = (player.tickCount % ticks) / ticks.toFloat()
                    blendColors(parseSpecialColor(animatedColors[index]), parseSpecialColor(animatedColors[nextIndex]), progress)
                }
            }
        }
        val staticColor = data.customLeatherColour ?: return null
        return parseSpecialColor(staticColor)
    }

    fun hasCustomItem(stack: ItemStack): Boolean {
        val data = getDataForItem(stack) ?: return false

        val customItem = data.customItem
        if (!customItem.isNullOrEmpty()) {
            val defaultItem = data.defaultItem ?: return true
            val parsedCustom = parseItemId(customItem)
            if (parsedCustom != defaultItem) return true
        }

        if (data.customLeatherColour != null) return true
        if (data.animatedLeatherColours != null && data.animatedLeatherColours!!.isNotEmpty()) return true

        return false
    }

    fun getCustomItem(stack: ItemStack): Item? {
        val data = getDataForItem(stack) ?: return null
        val customItemStr = data.customItem ?: return null
        if (customItemStr.isEmpty()) return null
        return parseItemToItem(customItemStr)
    }

    fun getCustomizedStackForRendering(stack: ItemStack): ItemStack {
        if (stack.isEmpty) return stack
        val data = getDataForItem(stack) ?: return stack

        val customItemStr = data.customItem
        val baseItem = if (!customItemStr.isNullOrEmpty()) {
            parseItemToItem(customItemStr) ?: stack.item
        } else {
            stack.item
        }

        val customStack = if (baseItem != stack.item) {
            ItemStack(baseItem, stack.count)
        } else {
            stack.copy()
        }

        // Apply dye color - check if the CUSTOM STACK can be dyed, not the original
        val dyeColor = getLeatherColor(stack)
        if (dyeColor != null && isLeatherArmor(customStack)) {
            customStack.set(DataComponents.DYED_COLOR,
                net.minecraft.world.item.component.DyedItemColor(dyeColor and 0x00FFFFFF))
        }

        if (isHelmetStack(customStack)) {
            val animatedId = data.animatedHelmetId
            if (!animatedId.isNullOrEmpty()) {
                val profile = animateHeadTexture(animatedId)
                if (profile != null) {
                    customStack.set(DataComponents.PROFILE, profile)
                }
            } else {
                val staticTexture = data.customHelmetTexture
                if (!staticTexture.isNullOrEmpty()) {
                    val profile = createHeadProfile(staticTexture)
                    if (profile != null) {
                        customStack.set(DataComponents.PROFILE, profile)
                    }
                }
            }
        }

        if (isTrimmableArmorStack(customStack) && hasCustomArmorTrim(stack)) {
            val materialStr = data.customTrimMaterial
            val patternStr = data.customTrimPattern
            if (!materialStr.isNullOrEmpty() && !patternStr.isNullOrEmpty()) {
                try {
                    val materialId = ResourceLocation.parse(materialStr)
                    val patternId = ResourceLocation.parse(patternStr)
                    val trim = getArmorTrim(materialId, patternId)
                    if (trim != null) {
                        customStack.set(DataComponents.TRIM, trim)
                    }
                } catch (e: Exception) {
                    // Invalid resource location
                }
            }
        }

        return customStack
    }

    /**
     * Check if a stack is leather armor (can be dyed)
     */
    private fun isLeatherArmor(stack: ItemStack): Boolean {
        val itemId = BuiltInRegistries.ITEM.getKey(stack.item)?.toString() ?: return false
        return itemId.contains("leather")
    }

    /**
     * Check if a stack is a helmet (without relying on original item's slot)
     */
    private fun isHelmetStack(stack: ItemStack): Boolean {
        val equippable = stack.get(DataComponents.EQUIPPABLE) ?: return false
        return equippable.slot() == EquipmentSlot.HEAD
    }

    /**
     * Check if a stack is trimmable armor (checking the actual stack, not by UUID lookup)
     */
    private fun isTrimmableArmorStack(stack: ItemStack): Boolean {
        return stack.`is`(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR)
    }

    fun hasAnyCustomization(stack: ItemStack): Boolean {
        val data = getDataForItem(stack) ?: return false
        return hasCustomItem(stack) ||
                getLeatherColor(stack) != null ||
                hasCustomArmorModel(stack) ||
                hasCustomHelmetTexture(stack) ||
                hasCustomArmorTrim(stack)
    }

    fun hasCustomArmorModel(stack: ItemStack): Boolean {
        val data = getDataForItem(stack) ?: return false
        return data.customArmorModel != null && data.customArmorModel!!.isNotEmpty()
    }

    fun getCustomArmorModel(stack: ItemStack): Item? {
        val data = getDataForItem(stack) ?: return null
        val customArmorStr = data.customArmorModel ?: return null
        if (customArmorStr.isEmpty()) return null
        return parseItemToItem(customArmorStr)
    }

    fun getCustomArmorStack(stack: ItemStack): ItemStack? {
        val data = getDataForItem(stack) ?: return null
        val customItem = getCustomArmorModel(stack) ?: return null
        val newStack = ItemStack(customItem)

        // Apply dye color only if the new stack is leather
        val dyeColor = getLeatherColor(stack)
        if (dyeColor != null && isLeatherArmor(newStack)) {
            newStack.set(DataComponents.DYED_COLOR,
                net.minecraft.world.item.component.DyedItemColor(dyeColor and 0x00FFFFFF))
        }

        // Apply trim if the new stack supports trims
        if (isTrimmableArmorStack(newStack) && hasCustomArmorTrim(stack)) {
            val materialStr = data.customTrimMaterial
            val patternStr = data.customTrimPattern
            if (!materialStr.isNullOrEmpty() && !patternStr.isNullOrEmpty()) {
                try {
                    val materialId = ResourceLocation.parse(materialStr)
                    val patternId = ResourceLocation.parse(patternStr)
                    val trim = getArmorTrim(materialId, patternId)
                    if (trim != null) {
                        newStack.set(DataComponents.TRIM, trim)
                    }
                } catch (e: Exception) {
                    // Invalid resource location
                }
            }
        }

        return newStack
    }

    fun getCompatibleArmorItems(stack: ItemStack): List<ArmorInfo> {
        val slot = getArmorSlot(stack) ?: return emptyList()
        return armorBySlot[slot] ?: emptyList()
    }

    fun isArmorCompatible(stack: ItemStack, targetId: String): Boolean {
        val sourceSlot = getArmorSlot(stack) ?: return false
        val targetItem = parseItemToItem(targetId) ?: return false
        val targetSlot = getArmorSlotForItem(targetItem) ?: return false
        return sourceSlot == targetSlot
    }

    fun isArmor(stack: ItemStack): Boolean {
        return getArmorSlot(stack) != null
    }

    fun isHelmet(stack: ItemStack): Boolean {
        return getArmorSlot(stack) == ArmorSlot.HELMET
    }

    fun isTrimmableArmor(stack: ItemStack): Boolean {
        // Check if item is in the trimmable armor tag
        return isArmor(stack) && stack.`is`(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR)
    }

    private fun getArmorSlot(stack: ItemStack): ArmorSlot? {
        val equippable = stack.get(DataComponents.EQUIPPABLE) ?: return null
        return equipmentSlotToArmorSlot(equippable.slot())
    }

    private fun getArmorSlotForItem(item: Item): ArmorSlot? {
        val stack = ItemStack(item)
        return getArmorSlot(stack)
    }

    private fun equipmentSlotToArmorSlot(slot: EquipmentSlot): ArmorSlot? {
        return when (slot) {
            EquipmentSlot.HEAD -> ArmorSlot.HELMET
            EquipmentSlot.CHEST -> ArmorSlot.CHESTPLATE
            EquipmentSlot.LEGS -> ArmorSlot.LEGGINGS
            EquipmentSlot.FEET -> ArmorSlot.BOOTS
            else -> null
        }
    }

    fun hasCustomHelmetTexture(stack: ItemStack): Boolean {
        val data = getDataForItem(stack) ?: return false
        return (data.customHelmetTexture != null && data.customHelmetTexture!!.isNotEmpty()) ||
                (data.animatedHelmetId != null && data.animatedHelmetId!!.isNotEmpty())
    }

    fun getCustomHelmetTexture(stack: ItemStack): String? {
        val data = getDataForItem(stack) ?: return null
        return data.customHelmetTexture
    }

    fun getAnimatedHelmetId(stack: ItemStack): String? {
        val data = getDataForItem(stack) ?: return null
        return data.animatedHelmetId
    }

    fun hasCustomArmorTrim(stack: ItemStack): Boolean {
        val data = getDataForItem(stack) ?: return false
        return data.customTrimMaterial != null && data.customTrimPattern != null
    }

    fun getCustomTrimMaterial(stack: ItemStack): String? {
        return getDataForItem(stack)?.customTrimMaterial
    }

    fun getCustomTrimPattern(stack: ItemStack): String? {
        return getDataForItem(stack)?.customTrimPattern
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

    fun parseSpecialColor(colorStr: String): Int {
        val parts = colorStr.split(":")
        if (parts.size >= 4) {
            try {
                val alpha = parts[1].toInt()
                val r = parts[2].toInt()
                val g = parts[3].toInt()
                val b = if (parts.size > 4) parts[4].toInt() else 255
                return (alpha shl 24) or (r shl 16) or (g shl 8) or b
            } catch (e: Exception) { }
        }
        return try {
            if (colorStr.startsWith("#")) {
                colorStr.substring(1).toLong(16).toInt() or (0xFF shl 24)
            } else {
                colorStr.toLong(16).toInt() or (0xFF shl 24)
            }
        } catch (e: Exception) {
            0xFF8B00FF.toInt()
        }
    }

    fun createSpecialColor(chroma: Int, alpha: Int, r: Int, g: Int, b: Int): String = "$chroma:$alpha:$r:$g:$b"

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val a1 = (color1 shr 24) and 0xFF; val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF; val b1 = color1 and 0xFF
        val a2 = (color2 shr 24) and 0xFF; val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF; val b2 = color2 and 0xFF
        val a = (a1 + (a2 - a1) * ratio).toInt(); val r = (r1 + (r2 - r1) * ratio).toInt()
        val g = (g1 + (g2 - g1) * ratio).toInt(); val b = (b1 + (b2 - b1) * ratio).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun parseItemToItem(itemStr: String): Item? {
        val cleanStr = parseItemId(itemStr)
        return try {
            val resourceLocation = ResourceLocation.parse(cleanStr)
            val itemOptional = BuiltInRegistries.ITEM.getOptional(resourceLocation)
            if (itemOptional.isPresent) {
                val item = itemOptional.get()
                val itemId = BuiltInRegistries.ITEM.getKey(item)
                if (itemId?.path != "air") item else null
            } else null
        } catch (e: Exception) { null }
    }

    private fun parseItemId(itemStr: String): String {
        val parts = itemStr.split(":")
        return when {
            parts.size == 1 -> "minecraft:${parts[0]}"
            parts[0] == "minecraft" -> itemStr
            parts.size == 2 && parts[1].toIntOrNull() != null -> "minecraft:${parts[0]}"
            else -> if (parts[0].contains(".")) itemStr else "minecraft:${parts[0]}"
        }
    }

    private fun loadArmorItems(): Map<ArmorSlot, List<ArmorInfo>> {
        val result = mutableMapOf<ArmorSlot, MutableList<ArmorInfo>>()
        ArmorSlot.entries.forEach { result[it] = mutableListOf() }

        BuiltInRegistries.ITEM.forEach { item ->
            val stack = ItemStack(item)
            val equippable = stack.get(DataComponents.EQUIPPABLE)
            if (equippable != null) {
                val slot = equipmentSlotToArmorSlot(equippable.slot())
                if (slot != null) {
                    val id = BuiltInRegistries.ITEM.getKey(item)?.toString() ?: return@forEach
                    val displayName = stack.hoverName.string
                    val material = extractMaterial(id)
                    result[slot]?.add(ArmorInfo(item, id, displayName, material))
                }
            }
        }

        val materialOrder = listOf("leather", "chainmail", "chain", "iron", "gold", "golden", "diamond", "netherite", "turtle")
        result.forEach { (_, list) ->
            list.sortBy { info ->
                val idx = materialOrder.indexOfFirst { info.material.contains(it) }
                if (idx >= 0) idx else 999
            }
        }

        return result
    }

    private fun extractMaterial(itemId: String): String {
        val name = itemId.removePrefix("minecraft:").lowercase()
        return when {
            name.contains("leather") -> "leather"
            name.contains("chainmail") || name.contains("chain") -> "chainmail"
            name.contains("iron") -> "iron"
            name.contains("gold") || name.contains("golden") -> "gold"
            name.contains("diamond") -> "diamond"
            name.contains("netherite") -> "netherite"
            name.contains("turtle") -> "turtle"
            else -> "other"
        }
    }

    fun getAllItemIds(): List<String> {
        return BuiltInRegistries.ITEM.keySet().map { it.toString() }
    }

    fun getAllCustomizedUUIDs(): Set<String> {
        if (!loaded) load()
        return itemDataCache.keys.toSet()
    }

    fun clearAll() {
        itemDataCache.clear()
        save()
    }

    fun exportToClipboard(uuid: String): String? {
        val data = getDataForUUID(uuid) ?: return null
        val json = data.toJson().toString()
        return java.util.Base64.getEncoder().encodeToString("PSCUSTOMIZE$json".toByteArray())
    }

    fun importFromClipboard(base64: String, uuid: String): Boolean {
        try {
            val decoded = String(java.util.Base64.getDecoder().decode(base64.trim()))
            if (!decoded.startsWith("PSCUSTOMIZE")) return false
            val json = decoded.substring("PSCUSTOMIZE".length)
            val jsonObj = gson.fromJson(json, JsonObject::class.java)
            val data = ItemData.fromJson(jsonObj)
            putItemData(uuid, data)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun isValidClipboardData(base64: String): Boolean {
        return try {
            val decoded = String(java.util.Base64.getDecoder().decode(base64.trim()))
            decoded.startsWith("PSCUSTOMIZE")
        } catch (e: Exception) { false }
    }
}