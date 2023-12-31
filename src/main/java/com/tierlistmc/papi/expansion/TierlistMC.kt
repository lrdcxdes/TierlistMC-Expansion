package com.tierlistmc.papi.expansion

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds


@Suppress("unused")
class TierlistMC : PlaceholderExpansion() {
    private val logger = Logger.getLogger("PlaceholderAPI")

    private val config = Config()
    private val api = Api(config, logger)
    private var isEnabled = false
    private val cache: TTLCache<String, Map<String, Any?>> = TTLCacheImpl(config.getCacheSeconds().seconds, 1)

    init {
        if (api.test()) {
            isEnabled = true
        } else {
            logger.warning("Failed to connect to TierlistMC API")
        }
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
        return "1.2"
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
        if (tierType.isBlank()) {
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
        val cached = cache.get(cacheKey)
        if (cached != null) {
            return if (cached[field] != null) {
                if (field == "id") {
                    (cached[field] as? Double)?.toInt()?.toString() ?: ""
                } else {
                    if (cached[field] is String) {
                        val name = cached[field] as? String? ?: ""
                        if (name.isNotBlank() && !params.contains("nocol")) {
                            config.getColor(tierType) + name
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

        val future = api.playerCurrentTierData(playerId, tierType)
        val data = future.get() ?: return ""
        return if (data[field] != null) {
            cache.add(cacheKey, data)
            if (field == "id") {
                (data[field] as? Double)?.toInt()?.toString() ?: ""
            } else {
                val name = data[field] as? String? ?: ""
                if (name.isNotBlank() && !params.contains("nocol")) {
                    config.getColor(tierType) + name
                } else {
                    ""
                }
            }
        } else {
            ""
        }
    }
}