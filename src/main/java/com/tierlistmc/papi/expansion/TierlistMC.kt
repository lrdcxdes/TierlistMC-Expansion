package com.tierlistmc.papi.expansion

import com.google.common.cache.CacheBuilder
import com.google.common.cache.LoadingCache
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Logger


@Suppress("unused")
class TierlistMC : PlaceholderExpansion(), Listener {
    private val logger = Logger.getLogger("PlaceholderAPI")

    private val config = Config(logger)
    private val api = Api(config, logger)
    private var isEnabled = false

    private val loader = object : com.google.common.cache.CacheLoader<String, Map<String, Any?>>() {
        override fun load(key: String): Map<String, Any?> {
            val args = key.split("-")
            val playerId = args[0]
            val tierType = args[1]
            val future = api.playerCurrentTierData(playerId, tierType)
            val tier: Tier = future.get() ?: return mapOf()
            return mapOf(
                "id" to tier.id, "name" to tier.name, "timestamp" to Instant.now().epochSecond
            )
        }
    }

    private val cacheSeconds = config.getCacheSeconds()
    private val removeOnQuit = config.removeOnQuit()
    private val cache: LoadingCache<String, Map<String, Any?>>

    init {
        if (api.test()) {
            isEnabled = true
        } else {
            logger.warning("Failed to connect to TierlistMC API")
        }

        cache = CacheBuilder.newBuilder()
            .expireAfterWrite(cacheSeconds, TimeUnit.SECONDS)
            .build(loader)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!removeOnQuit) return
        onPlayerLeave(event.player.name)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        if (!removeOnQuit) return
        onPlayerLeave(event.player.name)
    }

    override fun canRegister(): Boolean {
        return isEnabled
    }

    override fun getAuthor(): String {
        return "tierlistmc"
    }

    override fun getIdentifier(): String {
        return "tierlistmc"
    }

    override fun getVersion(): String {
        return "1.3"
    }

    private fun getFromMap(map: Map<String, Any?>, tierType: String, field: String): String {
        return if (map[field] != null) {
            if (field == "id") {
                (map[field] as? Double)?.toInt()?.toString() ?: ""
            } else {
                if (map[field] is String) {
                    val name = map[field] as? String? ?: ""
                    if (name.isNotBlank()) {
                        config.getFormat(tierType).replace("{name}", name)
                    } else {
                        ""
                    }
                } else {
                    ""
                }
            }
        } else {
            ""
        }
    }

    private fun onPlayerLeave(playerName: String) {
        val key = "$playerName-"
        val keys = cache.asMap().keys.filter { it.startsWith(key) }
        keys.forEach { cache.invalidate(it) }
    }

    override fun onRequest(player: OfflinePlayer, params: String): String {
        val args: List<String> = if (params.startsWith("find_")) {
            params.substring(5).split("_")
        } else {
            params.split("_")
        }

        val field = args.getOrNull(0) ?: return "%field_is_invalid%"

        if (field.isBlank() || field !in listOf("name", "id")) {
            return "%field_is_invalid%"
        }

        var tierType = args.getOrNull(1) ?: return "%tier_type_is_invalid%"
        if (tierType.isBlank() || !isTierTypeExists(tierType)) {
            return "%tier_type_is_invalid%"
        }
        tierType = tierType.lowercase()

        var playerId = player.name

        if (playerId == null) {
            playerId = args.getOrNull(2) ?: return "%player_is_invalid%"

            if (playerId.isBlank() || playerId.length < 3 || playerId.length > 16) {
                return "%player_is_invalid%"
            }
        }

        val cacheKey = "$playerId-$tierType"
        return getFromMap(cache.get(cacheKey), tierType, field)
    }
}