package com.peachsoju.sync

/**
 * Data classes for PeachSoju sync API
 */

data class SyncPayload(
    val uuid: String,
    val nickHider: NickHiderConfig
)

data class NickHiderConfig(
    val enabled: Boolean = false,
    val useCustomNick: Boolean = false,
    val nick: String = "",
    val color: String = "§f",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val rankEnabled: Boolean = false,
    val rankOrdinal: Int = 0,
    val plusColor: String = "§c",
    val bracketColor: String = "§6"
)

data class PlayerSyncData(
    val uuid: String,
    val nickHider: NickHiderConfig,
    val updatedAt: Long
)

data class PlayersResponse(
    val players: Map<String, PlayerSyncData>
)

data class SyncResponse(
    val success: Boolean,
    val uuid: String? = null,
    val error: String? = null
)