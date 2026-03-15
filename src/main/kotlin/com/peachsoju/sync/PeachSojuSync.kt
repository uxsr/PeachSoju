package com.peachsoju.sync

import com.google.gson.Gson
import com.peachsoju.PeachSoju.mc
import com.peachsoju.config
import com.peachsoju.eventbus.SubscribeEvent
import com.peachsoju.eventbus.events.TickEvent
import com.peachsoju.modules.impl.misc.nickhider.HypixelRank
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Manages syncing PeachSoju configs between players.
 *
 * - Uploads your config when it changes
 * - Fetches nearby players' configs periodically
 * - Caches configs for rendering
 */
object PeachSojuSync {

    private const val API_BASE_URL = "http://103.90.162.69:3000/api"

    private val gson = Gson()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "PeachSoju-Sync").apply { isDaemon = true }
    }

    // Wrapper to track when we fetched the data (not when they uploaded)
    private data class CachedPlayer(
        val data: PlayerSyncData,
        val fetchedAt: Long = System.currentTimeMillis()
    )

    // Cache of other players' configs (UUID -> cached data)
    private val playerCache = ConcurrentHashMap<String, CachedPlayer>()

    // Track last upload to avoid spamming
    private var lastUploadTime = 0L
    private var lastFetchTime = 0L
    private var pendingUpload = false

    // Tick counter for periodic tasks
    private var tickCounter = 0

    private const val UPLOAD_COOLDOWN_MS = 5000L  // 5 seconds between uploads
    private const val FETCH_INTERVAL_TICKS = 60   // Fetch every 3 seconds (60 ticks) - more frequent
    private const val CACHE_EXPIRY_MS = 120000L   // Cache entries expire after 2 minutes

    /**
     * Initialize the sync system
     */
    fun init() {
        println("[PeachSoju] Sync system initialized")
        // Force an upload after 5 seconds to test
        pendingUpload = true
        println("[PeachSoju-Sync] Forced pendingUpload=true for testing")
    }

    /**
     * Called when config changes - marks for upload
     */
    fun markDirty() {
        pendingUpload = true
    }

    /**
     * Get a player's synced config from cache
     */
    fun getPlayerConfig(uuid: UUID): PlayerSyncData? {
        return getPlayerConfig(uuid.toString())
    }

    fun getPlayerConfig(uuid: String): PlayerSyncData? {
        val normalizedUuid = uuid.lowercase()
        val cached = playerCache[normalizedUuid] ?: return null

        // Check if cache entry is still valid based on FETCH time (not upload time)
        if (System.currentTimeMillis() - cached.fetchedAt > CACHE_EXPIRY_MS) {
            playerCache.remove(normalizedUuid)
            return null
        }

        return cached.data
    }

    /**
     * Check if a player has a synced config
     */
    fun hasPlayerConfig(uuid: UUID): Boolean {
        return getPlayerConfig(uuid) != null
    }

    /**
     * Build the formatted nick for another player based on their synced config
     */
    fun buildNickForPlayer(uuid: UUID): String? {
        val config = getPlayerConfig(uuid) ?: return null
        val nh = config.nickHider

        // Must have sync enabled AND custom nick enabled with a nick set
        if (!nh.enabled) return null
        if (!nh.useCustomNick || nh.nick.isEmpty()) return null

        val rank = HypixelRank.fromOrdinal(nh.rankOrdinal)
        val prefix = if (nh.rankEnabled) {
            rank.buildPrefix(nh.plusColor, nh.bracketColor)
        } else {
            ""
        }

        val formatting = buildString {
            if (nh.bold) append("§l")
            if (nh.italic) append("§o")
            if (nh.underline) append("§n")
            if (nh.strikethrough) append("§m")
        }

        return "$prefix${nh.color}$formatting${nh.nick}§r"
    }

    /**
     * Upload current config to server
     */
    private fun uploadConfig() {
        val player = mc.player ?: return
        val uuid = player.uuid.toString()

        val payload = SyncPayload(
            uuid = uuid,
            nickHider = NickHiderConfig(
                enabled = config.nickHider(),
                useCustomNick = config.nickHiderUseCustomNick(),
                nick = config.nickHiderNickRaw(),
                color = config.nickHiderColor(),
                bold = config.nickHiderBold(),
                italic = config.nickHiderItalic(),
                underline = config.nickHiderUnderline(),
                strikethrough = config.nickHiderStrikethrough(),
                rankEnabled = config.nickHiderRankEnabled(),
                rankOrdinal = config.nickHiderRankOrdinal(),
                plusColor = config.nickHiderPlusColor(),
                bracketColor = config.nickHiderMvpPlusPlusBracketColor()
            )
        )

        executor.submit {
            try {
                val json = gson.toJson(payload)
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$API_BASE_URL/sync"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    println("[PeachSoju] Config synced successfully")
                } else {
                    println("[PeachSoju] Failed to sync config: ${response.statusCode()}")
                }
            } catch (e: Exception) {
                println("[PeachSoju] Error syncing config: ${e.message}")
            }
        }
    }

    /**
     * Fetch configs for nearby players
     */
    private fun fetchNearbyPlayers() {
        val player = mc.player ?: return
        val world = mc.level ?: return

        // Get UUIDs of nearby players (within 64 blocks)
        val nearbyUuids = world.players()
            .filter { it != player && it.distanceTo(player) < 64 }
            .map { it.uuid.toString().lowercase() }
            .distinct()
            .take(50) // Limit to 50 players

        if (nearbyUuids.isEmpty()) return

        executor.submit {
            try {
                val uuidsParam = nearbyUuids.joinToString(",")
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$API_BASE_URL/players?uuids=$uuidsParam"))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    val playersResponse = gson.fromJson(response.body(), PlayersResponse::class.java)

                    // Update cache with fresh fetch time
                    val now = System.currentTimeMillis()
                    for ((uuid, data) in playersResponse.players) {
                        playerCache[uuid.lowercase()] = CachedPlayer(data, now)
                    }

                    if (playersResponse.players.isNotEmpty()) {
                        println("[PeachSoju] Fetched ${playersResponse.players.size} player configs")
                    }
                }
            } catch (e: Exception) {
                // Silent fail - don't spam logs
            }
        }
    }

    /**
     * Clean up expired cache entries
     */
    private fun cleanupCache() {
        val now = System.currentTimeMillis()
        playerCache.entries.removeIf { (_, cached) ->
            now - cached.fetchedAt > CACHE_EXPIRY_MS
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.End) {
        if (mc.player == null) return

        tickCounter++

        // Handle pending upload
        if (pendingUpload) {
            val now = System.currentTimeMillis()
            if (now - lastUploadTime >= UPLOAD_COOLDOWN_MS) {
                lastUploadTime = now
                pendingUpload = false
                uploadConfig()
            }
        }

        // Periodic fetch of nearby players
        if (tickCounter % FETCH_INTERVAL_TICKS == 0) {
            fetchNearbyPlayers()
        }

        // Cleanup cache every 30 seconds
        if (tickCounter % 600 == 0) {
            cleanupCache()
        }
    }
}