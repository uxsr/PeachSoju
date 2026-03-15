package com.peachsoju.modules.impl.misc.nickhider

/**
 * Hypixel rank definitions with their formatting.
 * Each rank has a base color, display name, and whether it supports plus symbols.
 */
enum class HypixelRank(
    val displayName: String,
    val baseColor: String,
    val prefix: String,
    val hasPlusSymbol: Boolean = false,
    val plusCount: Int = 0,
    val defaultPlusColor: String = ""
) {
    NONE(
        displayName = "None",
        baseColor = "§7",
        prefix = "§7",
        hasPlusSymbol = false
    ),
    VIP(
        displayName = "VIP",
        baseColor = "§a",
        prefix = "§a[VIP]",
        hasPlusSymbol = false
    ),
    VIP_PLUS(
        displayName = "VIP+",
        baseColor = "§a",
        prefix = "§a[VIP§6+§a]",
        hasPlusSymbol = true,
        plusCount = 1,
        defaultPlusColor = "§6"
    ),
    MVP(
        displayName = "MVP",
        baseColor = "§b",
        prefix = "§b[MVP]",
        hasPlusSymbol = false
    ),
    MVP_PLUS(
        displayName = "MVP+",
        baseColor = "§b",
        prefix = "§b[MVP§c+§b]",
        hasPlusSymbol = true,
        plusCount = 1,
        defaultPlusColor = "§c"
    ),
    MVP_PLUS_PLUS(
        displayName = "MVP++",
        baseColor = "§6",
        prefix = "§6[MVP§c++§6]",
        hasPlusSymbol = true,
        plusCount = 2,
        defaultPlusColor = "§c"
    ),
    YOUTUBE(
        displayName = "YOUTUBE",
        baseColor = "§c",
        prefix = "§c[§fYOUTUBE§c]",
        hasPlusSymbol = false
    ),
    HELPER(
        displayName = "HELPER",
        baseColor = "§9",
        prefix = "§9[HELPER]",
        hasPlusSymbol = false
    ),
    MOD(
        displayName = "MOD",
        baseColor = "§2",
        prefix = "§2[MOD]",
        hasPlusSymbol = false
    ),
    ADMIN(
        displayName = "ADMIN",
        baseColor = "§c",
        prefix = "§c[ADMIN]",
        hasPlusSymbol = false
    ),
    OWNER(
        displayName = "OWNER",
        baseColor = "§c",
        prefix = "§c[OWNER]",
        hasPlusSymbol = false
    );

    companion object {
        /**
         * All available plus colors for MVP+ and MVP++
         */
        val PLUS_COLORS = listOf(
            PlusColor("§c", "Red", 0xFF5555),
            PlusColor("§6", "Gold", 0xFFAA00),
            PlusColor("§a", "Green", 0x55FF55),
            PlusColor("§b", "Aqua", 0x55FFFF),
            PlusColor("§d", "Light Purple", 0xFF55FF),
            PlusColor("§f", "White", 0xFFFFFF),
            PlusColor("§e", "Yellow", 0xFFFF55),
            PlusColor("§0", "Black", 0x000000),
            PlusColor("§1", "Dark Blue", 0x0000AA),
            PlusColor("§2", "Dark Green", 0x00AA00),
            PlusColor("§3", "Dark Aqua", 0x00AAAA),
            PlusColor("§4", "Dark Red", 0xAA0000),
            PlusColor("§5", "Dark Purple", 0xAA00AA),
            PlusColor("§7", "Gray", 0xAAAAAA),
            PlusColor("§8", "Dark Gray", 0x555555),
            PlusColor("§9", "Blue", 0x5555FF)
        )

        /**
         * MVP++ bracket colors (the color of the [ ] and MVP text)
         */
        val MVP_PLUS_PLUS_BRACKET_COLORS = listOf(
            PlusColor("§6", "Gold", 0xFFAA00),
            PlusColor("§b", "Aqua", 0x55FFFF)
        )

        fun fromDisplayName(name: String): HypixelRank {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: NONE
        }

        fun fromOrdinal(ordinal: Int): HypixelRank {
            return entries.getOrElse(ordinal) { NONE }
        }
    }

    /**
     * Build the rank prefix with custom plus color
     */
    fun buildPrefix(plusColor: String? = null, mvpPlusPlusBracketColor: String? = null): String {
        return when (this) {
            NONE -> ""
            VIP -> "§a[VIP] "
            VIP_PLUS -> {
                val plus = plusColor ?: defaultPlusColor
                "§a[VIP${plus}+§a] "
            }
            MVP -> "§b[MVP] "
            MVP_PLUS -> {
                val plus = plusColor ?: defaultPlusColor
                "§b[MVP${plus}+§b] "
            }
            MVP_PLUS_PLUS -> {
                val bracket = mvpPlusPlusBracketColor ?: "§6"
                val plus = plusColor ?: defaultPlusColor
                "$bracket[MVP${plus}++$bracket] "
            }
            YOUTUBE -> "§c[§fYOUTUBE§c] "
            HELPER -> "§9[HELPER] "
            MOD -> "§2[MOD] "
            ADMIN -> "§c[ADMIN] "
            OWNER -> "§c[OWNER] "
        }
    }

    /**
     * Get the name color that follows the rank prefix
     */
    fun getNameColor(): String {
        return when (this) {
            NONE -> "§7"
            VIP, VIP_PLUS -> "§a"
            MVP, MVP_PLUS -> "§b"
            MVP_PLUS_PLUS -> "§6" // Can be gold or aqua
            YOUTUBE -> "§c"
            HELPER -> "§9"
            MOD -> "§2"
            ADMIN, OWNER -> "§c"
        }
    }

    fun getNameColor(mvpPlusPlusBracketColor: String? = null): String {
        return if (this == MVP_PLUS_PLUS && mvpPlusPlusBracketColor != null) {
            mvpPlusPlusBracketColor
        } else {
            getNameColor()
        }
    }
}

data class PlusColor(val code: String, val name: String, val rgb: Int)