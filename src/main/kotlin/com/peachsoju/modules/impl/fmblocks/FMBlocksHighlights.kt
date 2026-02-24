package com.peachsoju.modules.impl.fmblocks

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.peachsoju.utils.handlers.FileHandler
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

object FMBlocksHighlights {

    data class HighlightColor(
        var r: Int = 255,
        var g: Int = 255,
        var b: Int = 255,
        var a: Float = 0.5f
    ) {
        fun toArgb(): Int {
            val alpha = (a * 255).toInt().coerceIn(0, 255)
            return (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }
        fun copy() = HighlightColor(r, g, b, a)
    }

    data class BlockHighlight(
        var itemId: String,
        var enabled: Boolean = true,
        var color: HighlightColor = HighlightColor()
    ) {
        fun getItem(): Item? {
            val loc = ResourceLocation.tryParse(itemId) ?: return null
            val item = BuiltInRegistries.ITEM.getValue(loc)
            return if (item != Items.AIR || itemId == "minecraft:air") item else null
        }
        fun copy() = BlockHighlight(itemId, enabled, color.copy())
    }

    private val highlights = mutableMapOf<String, BlockHighlight>()
    private var loaded = false

    private val defaultHighlights = listOf(
        BlockHighlight("minecraft:beacon", true, HighlightColor(85, 255, 255, 0.6f)),
        BlockHighlight("minecraft:chest", true, HighlightColor(255, 215, 0, 0.4f)),
        BlockHighlight("minecraft:trapped_chest", true, HighlightColor(255, 100, 100, 0.5f)),
        BlockHighlight("minecraft:lever", true, HighlightColor(255, 85, 85, 0.5f)),
        BlockHighlight("minecraft:stone_button", true, HighlightColor(170, 170, 170, 0.4f)),
        BlockHighlight("minecraft:skull", true, HighlightColor(255, 255, 255, 0.5f)),
        BlockHighlight("minecraft:wither_skeleton_skull", true, HighlightColor(50, 50, 50, 0.6f))
    )

    fun reloadFromDisk() {
        load()
        loaded = true
    }

    fun getHighlight(itemId: String): BlockHighlight? {
        ensureLoaded()
        return highlights[itemId]
    }

    fun getAllHighlights(): List<BlockHighlight> {
        ensureLoaded()
        return highlights.values.toList()
    }

    fun getEnabledHighlights(): List<BlockHighlight> {
        ensureLoaded()
        return highlights.values.filter { it.enabled }
    }

    private fun ensureLoaded() {
        if (!loaded) {
            load()
            loaded = true
        }
    }

    fun addHighlight(itemId: String, color: HighlightColor = HighlightColor()): BlockHighlight {
        val highlight = BlockHighlight(itemId, true, color)
        highlights[itemId] = highlight
        save()
        return highlight
    }

    fun removeHighlight(itemId: String) {
        highlights.remove(itemId)
        save()
    }

    fun setHighlightEnabled(itemId: String, enabled: Boolean) {
        highlights[itemId]?.let {
            it.enabled = enabled
            save()
        }
    }

    fun setHighlightColor(itemId: String, r: Int, g: Int, b: Int, a: Float) {
        highlights[itemId]?.let {
            it.color = HighlightColor(r, g, b, a)
            save()
        }
    }

    fun isHighlighted(itemId: String): Boolean = highlights[itemId]?.enabled == true

    fun resetToDefaults() {
        highlights.clear()
        initDefaults()
        save()
    }

    private fun initDefaults() {
        defaultHighlights.forEach { highlight ->
            highlights[highlight.itemId] = highlight.copy()
        }
    }

    private fun save() {
        if (!loaded) {
            load()
            loaded = true
        }

        val json = JsonObject()
        val array = JsonArray()

        highlights.values.forEach { highlight ->
            val obj = JsonObject().apply {
                addProperty("itemId", highlight.itemId)
                addProperty("enabled", highlight.enabled)
                addProperty("r", highlight.color.r)
                addProperty("g", highlight.color.g)
                addProperty("b", highlight.color.b)
                addProperty("a", highlight.color.a)
            }
            array.add(obj)
        }

        json.add("highlights", array)
        FileHandler.writeToFile("fmblocks_highlights.json", json)
    }

    fun load() {
        val json = FileHandler.readFromFile("fmblocks_highlights.json")
        highlights.clear()

        val array = json.getAsJsonArray("highlights")
        if (array == null || array.isEmpty) {
            initDefaults()
            return
        }

        array.forEach { element ->
            val obj = element.asJsonObject
            val itemId = obj.get("itemId")?.asString ?: return@forEach
            val enabled = obj.get("enabled")?.asBoolean ?: true
            val r = obj.get("r")?.asInt ?: 255
            val g = obj.get("g")?.asInt ?: 255
            val b = obj.get("b")?.asInt ?: 255
            val a = obj.get("a")?.asFloat ?: 0.5f

            highlights[itemId] = BlockHighlight(itemId, enabled, HighlightColor(r, g, b, a))
        }
    }
}