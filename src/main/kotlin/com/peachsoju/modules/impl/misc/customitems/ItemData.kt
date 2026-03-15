package com.peachsoju.modules.impl.misc.customitems

import com.google.gson.JsonObject


data class ItemData(

    var customName: String? = null,
    var customNamePrefix: String? = null,


    var overrideEnchantGlint: Boolean = false,
    var enchantGlintValue: Boolean = false,


    var customLeatherColour: String? = null,
    var animatedLeatherColours: Array<String>? = null,
    var animatedDyeTicks: Int = 2,
    var dyeMode: DyeMode = DyeMode.CYCLING,


    var defaultItem: String? = null,
    var customItem: String? = null,


    var customArmorModel: String? = null,


    var customHelmetTexture: String? = null,
    var animatedHelmetId: String? = null,


    var customTrimMaterial: String? = null,
    var customTrimPattern: String? = null
) {
    companion object {
        fun fromJson(json: JsonObject): ItemData {
            return ItemData(
                customName = json.get("customName")?.takeIf { !it.isJsonNull }?.asString,
                customNamePrefix = json.get("customNamePrefix")?.takeIf { !it.isJsonNull }?.asString ?: "",
                overrideEnchantGlint = json.get("overrideEnchantGlint")?.asBoolean ?: false,
                enchantGlintValue = json.get("enchantGlintValue")?.asBoolean ?: false,
                customLeatherColour = json.get("customLeatherColour")?.takeIf { !it.isJsonNull }?.asString,
                animatedLeatherColours = json.get("animatedLeatherColours")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString }?.toTypedArray(),
                animatedDyeTicks = json.get("animatedDyeTicks")?.asInt ?: 2,
                dyeMode = json.get("dyeMode")?.asString?.let { runCatching { DyeMode.valueOf(it) }.getOrNull() } ?: DyeMode.CYCLING,
                defaultItem = json.get("defaultItem")?.takeIf { !it.isJsonNull }?.asString,
                customItem = json.get("customItem")?.takeIf { !it.isJsonNull }?.asString,
                customArmorModel = json.get("customArmorModel")?.takeIf { !it.isJsonNull }?.asString,
                customHelmetTexture = json.get("customHelmetTexture")?.takeIf { !it.isJsonNull }?.asString,
                animatedHelmetId = json.get("animatedHelmetId")?.takeIf { !it.isJsonNull }?.asString,
                customTrimMaterial = json.get("customTrimMaterial")?.takeIf { !it.isJsonNull }?.asString,
                customTrimPattern = json.get("customTrimPattern")?.takeIf { !it.isJsonNull }?.asString
            )
        }
    }

    fun toJson(): JsonObject {
        val json = JsonObject()
        customName?.let { json.addProperty("customName", it) }
        if (customNamePrefix?.isNotEmpty() == true) json.addProperty("customNamePrefix", customNamePrefix)
        if (overrideEnchantGlint) {
            json.addProperty("overrideEnchantGlint", true)
            json.addProperty("enchantGlintValue", enchantGlintValue)
        }
        customLeatherColour?.let { json.addProperty("customLeatherColour", it) }
        animatedLeatherColours?.let { arr ->
            val jsonArr = com.google.gson.JsonArray()
            arr.forEach { jsonArr.add(it) }
            json.add("animatedLeatherColours", jsonArr)
        }
        if (animatedDyeTicks != 2) json.addProperty("animatedDyeTicks", animatedDyeTicks)
        if (dyeMode != DyeMode.CYCLING) json.addProperty("dyeMode", dyeMode.name)
        defaultItem?.let { json.addProperty("defaultItem", it) }
        customItem?.let { json.addProperty("customItem", it) }
        customArmorModel?.let { json.addProperty("customArmorModel", it) }
        customHelmetTexture?.let { json.addProperty("customHelmetTexture", it) }
        animatedHelmetId?.let { json.addProperty("animatedHelmetId", it) }
        customTrimMaterial?.let { json.addProperty("customTrimMaterial", it) }
        customTrimPattern?.let { json.addProperty("customTrimPattern", it) }
        return json
    }

    fun isEmpty(): Boolean {
        return customName == null &&
                !overrideEnchantGlint &&
                customLeatherColour == null &&
                animatedLeatherColours == null &&
                customItem == null &&
                customArmorModel == null &&
                customHelmetTexture == null &&
                animatedHelmetId == null &&
                customTrimMaterial == null &&
                customTrimPattern == null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemData) return false
        return customName == other.customName &&
                customNamePrefix == other.customNamePrefix &&
                overrideEnchantGlint == other.overrideEnchantGlint &&
                enchantGlintValue == other.enchantGlintValue &&
                customLeatherColour == other.customLeatherColour &&
                animatedLeatherColours.contentEquals(other.animatedLeatherColours) &&
                animatedDyeTicks == other.animatedDyeTicks &&
                dyeMode == other.dyeMode &&
                defaultItem == other.defaultItem &&
                customItem == other.customItem &&
                customArmorModel == other.customArmorModel &&
                customHelmetTexture == other.customHelmetTexture &&
                animatedHelmetId == other.animatedHelmetId &&
                customTrimMaterial == other.customTrimMaterial &&
                customTrimPattern == other.customTrimPattern
    }

    override fun hashCode(): Int {
        var result = customName?.hashCode() ?: 0
        result = 31 * result + (customNamePrefix?.hashCode() ?: 0)
        result = 31 * result + overrideEnchantGlint.hashCode()
        result = 31 * result + enchantGlintValue.hashCode()
        result = 31 * result + (customLeatherColour?.hashCode() ?: 0)
        result = 31 * result + (animatedLeatherColours?.contentHashCode() ?: 0)
        result = 31 * result + animatedDyeTicks
        result = 31 * result + dyeMode.hashCode()
        result = 31 * result + (defaultItem?.hashCode() ?: 0)
        result = 31 * result + (customItem?.hashCode() ?: 0)
        result = 31 * result + (customArmorModel?.hashCode() ?: 0)
        result = 31 * result + (customHelmetTexture?.hashCode() ?: 0)
        result = 31 * result + (animatedHelmetId?.hashCode() ?: 0)
        result = 31 * result + (customTrimMaterial?.hashCode() ?: 0)
        result = 31 * result + (customTrimPattern?.hashCode() ?: 0)
        return result
    }
}


enum class DyeMode {
    CYCLING,
    GRADIENT
}


enum class ArmorSlot(val slotName: String) {
    HELMET("helmet"),
    CHESTPLATE("chestplate"),
    LEGGINGS("leggings"),
    BOOTS("boots");

    companion object {
        fun fromItemId(itemId: String): ArmorSlot? {
            val lower = itemId.lowercase()
            return when {
                lower.contains("helmet") || lower.contains("cap") || lower.contains("hood") -> HELMET
                lower.contains("chestplate") || lower.contains("tunic") || lower.contains("jacket") -> CHESTPLATE
                lower.contains("leggings") || lower.contains("pants") || lower.contains("trousers") -> LEGGINGS
                lower.contains("boots") || lower.contains("shoes") || lower.contains("slippers") -> BOOTS
                else -> null
            }
        }

        
        fun areCompatible(sourceId: String, targetId: String): Boolean {
            val sourceSlot = fromItemId(sourceId) ?: return false
            val targetSlot = fromItemId(targetId) ?: return false
            return sourceSlot == targetSlot
        }
    }
}