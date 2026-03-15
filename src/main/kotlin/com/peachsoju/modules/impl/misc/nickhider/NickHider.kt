package com.peachsoju.modules.impl.misc.nickhider

import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.sync.PeachSojuSync
import net.minecraft.network.chat.Component
import java.util.UUID

object NickHider {

    private var cachedPlayerName: String? = null

    // Regex patterns to match Hypixel rank prefixes
    private val RANK_PREFIX_PATTERN = Regex(
        """(?:§[0-9a-fk-or])*\[(?:§[0-9a-fk-or])*(?:VIP|MVP|YOUTUBE|HELPER|MOD|ADMIN|OWNER|GAME MASTER|BUILD TEAM)(?:§[0-9a-fk-or])*\+*(?:§[0-9a-fk-or])*\](?:§[0-9a-fk-or])* ?"""
    )

    // Track if we're currently rendering the scoreboard
    @Volatile
    var isRenderingScoreboard: Boolean = false

    fun getPlayerName(): String? {
        if (cachedPlayerName == null) {
            cachedPlayerName = mc.player?.gameProfile?.name
        }
        return cachedPlayerName
    }

    fun clearCache() {
        cachedPlayerName = null
    }

    // ==================== SELF NICK HIDER ====================

    private fun isViewingOwnNametag(): Boolean {
        val options = mc.options ?: return false
        return !options.cameraType.isFirstPerson
    }

    private fun isChatMessage(text: String, playerName: String): Boolean {
        if (text.contains("> ") && text.contains(playerName) && text.contains(":")) {
            val gtIndex = text.lastIndexOf("> ")
            val colonIndex = text.indexOf(":", gtIndex)
            if (gtIndex != -1 && colonIndex != -1) {
                val namePart = text.substring(gtIndex, colonIndex)
                if (namePart.contains(playerName)) {
                    return true
                }
            }
        }

        if (text.contains(playerName) && text.contains(": ")) {
            val colonIndex = text.indexOf(": ")
            if (colonIndex != -1) {
                val beforeColon = text.substring(0, colonIndex)
                if (beforeColon.endsWith(playerName) || beforeColon.contains("$playerName§")) {
                    return true
                }
            }
        }

        return false
    }

    private fun isStatusMessage(text: String, playerName: String): Boolean {
        if (isRenderingScoreboard) return true
        if (text.contains("[Lv") && text.contains("]")) return true

        val classIndicatorPattern = Regex("""§.\[[HMBAT]\]""")
        if (classIndicatorPattern.containsMatchIn(text)) return true

        if (text.contains("$playerName is now ready")) return true
        if (text.contains("$playerName is no longer ready")) return true
        if (text.contains("Party Leader:") || text.contains("Party Moderators:") || text.contains("Party Members:")) return true
        if (text.contains("entered") && text.contains("Catacombs")) return true
        if (text.contains("selected the") && text.contains("Class")) return true
        if (text.contains("has obtained")) return true

        return false
    }

    fun buildFullNick(): String {
        val nick = config.nickHiderNickRaw()
        if (nick.isEmpty()) return getPlayerName() ?: ""

        val rank = getCurrentRank()
        val plusColor = config.nickHiderPlusColor()
        val bracketColor = config.nickHiderMvpPlusPlusBracketColor()

        val prefix = if (config.nickHiderRankEnabled()) {
            rank.buildPrefix(plusColor, bracketColor)
        } else {
            ""
        }

        val nameColor = config.nickHiderColor()
        val formatting = buildFormatting()

        return "$prefix$nameColor$formatting$nick§r"
    }

    fun buildFormattedNick(): String {
        val nick = config.nickHiderNickRaw()
        if (nick.isEmpty()) return getPlayerName() ?: ""

        val color = config.nickHiderColor()
        val formatting = buildFormatting()
        return "$color$formatting$nick§r"
    }

    fun buildNickWithoutRank(): String {
        val nick = config.nickHiderNickRaw()
        if (nick.isEmpty()) return getPlayerName() ?: ""

        val nameColor = config.nickHiderColor()
        val formatting = buildFormatting()
        return "$nameColor$formatting$nick§r"
    }

    fun buildPlainNick(): String {
        val nick = config.nickHiderNickRaw()
        return nick.ifEmpty { getPlayerName() ?: "" }
    }

    private fun buildFormatting(): String {
        val bold = if (config.nickHiderBold()) "§l" else ""
        val italic = if (config.nickHiderItalic()) "§o" else ""
        val underline = if (config.nickHiderUnderline()) "§n" else ""
        val strike = if (config.nickHiderStrikethrough()) "§m" else ""
        return "$bold$italic$underline$strike"
    }

    fun getCurrentRank(): HypixelRank {
        return HypixelRank.fromOrdinal(config.nickHiderRankOrdinal())
    }

    fun setCurrentRank(rank: HypixelRank) {
        config.setNickHiderRankOrdinal(rank.ordinal)
    }

    fun getNickToDisplay(): String {
        val nick = config.nickHiderNickRaw()
        if (nick.isEmpty()) return getPlayerName() ?: "Player"

        return if (config.nickHiderRankEnabled()) {
            buildFullNick()
        } else {
            buildFormattedNick()
        }
    }

    // ==================== OTHER PLAYERS (SYNC) ====================

    /**
     * Get a map of other players' real names to their synced nicks.
     * Only includes players who are nearby and have PeachSoju configs.
     */
    private fun getSyncedPlayerNames(): Map<String, SyncedPlayerInfo> {
        val result = mutableMapOf<String, SyncedPlayerInfo>()
        val world = mc.level ?: return result
        val localPlayer = mc.player ?: return result

        for (player in world.players()) {
            if (player == localPlayer) continue

            val uuid = player.uuid
            val realName = player.gameProfile.name
            val syncedNick = PeachSojuSync.buildNickForPlayer(uuid)

            if (syncedNick != null) {
                result[realName] = SyncedPlayerInfo(uuid, realName, syncedNick)
            }
        }

        return result
    }

    /**
     * Build a plain nick (no rank) for a synced player - used for scoreboard/status
     */
    private fun buildPlainNickForPlayer(uuid: UUID): String? {
        val config = PeachSojuSync.getPlayerConfig(uuid) ?: return null
        val nh = config.nickHider
        if (!nh.enabled || !nh.useCustomNick || nh.nick.isEmpty()) return null
        return nh.nick
    }

    // ==================== MAIN REPLACE FUNCTION ====================

    /**
     * Replace player names in text.
     * Handles both your own name AND other PeachSoju users' names.
     */
    fun replaceName(text: String?): String? {
        if (text == null) return null

        // Skip if this is the chat input text (prevents crash in EditBox)
        val screen = mc.screen
        if (screen is net.minecraft.client.gui.screens.ChatScreen) {
            try {
                val accessor = screen as com.peachsoju.mixin.ChatScreenAccessor
                val inputValue = accessor.input?.value
                if (inputValue != null && text == inputValue) {
                    return text
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        var result = text

        // 1. Replace your own name (if sync enabled AND custom nick enabled)
        result = replaceOwnName(result)

        // 2. Replace other PeachSoju users' names
        result = replaceOtherPlayersNames(result)

        return result
    }

    /**
     * Replace your own name in the text
     */
    private fun replaceOwnName(text: String): String {
        // Must have sync enabled
        if (!config.nickHider()) return text

        // Must have custom nick enabled
        if (!config.nickHiderUseCustomNick()) return text

        val playerName = getPlayerName() ?: return text
        if (playerName.isEmpty()) return text
        if (!text.contains(playerName)) return text

        val isF5 = isViewingOwnNametag()
        val isStatus = isStatusMessage(text, playerName)
        val isChat = !isStatus && isChatMessage(text, playerName)

        var result = text

        // If it's a chat message with custom rank enabled, remove existing rank prefix
        if (isChat && config.nickHiderRankEnabled()) {
            val rankBeforeNamePattern = Regex(
                """(${RANK_PREFIX_PATTERN.pattern})(\Q$playerName\E)"""
            )
            result = result.replace(rankBeforeNamePattern) { matchResult ->
                matchResult.groupValues[2]
            }
        }

        // Get the appropriate replacement based on context
        val replacement = when {
            isStatus -> buildPlainNick()
            isF5 -> buildNickWithoutRank()
            isChat && config.nickHiderRankEnabled() -> buildFullNick()
            else -> buildFormattedNick()
        }

        return result.replace(playerName, replacement)
    }

    /**
     * Replace other PeachSoju users' names in the text
     */
    private fun replaceOtherPlayersNames(text: String): String {
        val syncedPlayers = getSyncedPlayerNames()
        if (syncedPlayers.isEmpty()) return text

        var result = text

        for ((realName, info) in syncedPlayers) {
            if (!result.contains(realName)) continue

            val isStatus = isStatusMessage(result, realName)
            val isChat = !isStatus && isChatMessage(result, realName)

            // If it's a chat message, remove existing rank prefix before their name
            if (isChat) {
                val rankBeforeNamePattern = Regex(
                    """(${RANK_PREFIX_PATTERN.pattern})(\Q$realName\E)"""
                )
                result = result.replace(rankBeforeNamePattern) { matchResult ->
                    matchResult.groupValues[2]
                }
            }

            // Get appropriate replacement
            val replacement = when {
                isStatus -> buildPlainNickForPlayer(info.uuid) ?: realName
                isChat -> info.syncedNick  // Full nick with rank
                else -> info.syncedNick    // Default to full nick
            }

            result = result.replace(realName, replacement)
        }

        return result
    }

    fun replaceInComponent(component: Component): Component {
        val originalString = component.string
        val newString = replaceName(originalString) ?: return component

        if (newString == originalString) return component
        return Component.literal(newString)
    }

    fun containsPlayerName(text: String?): Boolean {
        if (text == null) return false
        val playerName = getPlayerName() ?: return false
        return text.contains(playerName)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.Start) {
        if (mc.player == null) {
            clearCache()
        }
    }

    /**
     * Info about a synced player
     */
    private data class SyncedPlayerInfo(
        val uuid: UUID,
        val realName: String,
        val syncedNick: String
    )
}