package com.tierlistmc.papi.expansion

import me.clip.placeholderapi.PlaceholderAPIPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable
import java.util.logging.Logger


@Suppress("unused")
class TierlistMC : PlaceholderExpansion(), Listener {
    private val logger = Logger.getLogger("PlaceholderAPI")

    private val config = Config(logger)
    private val api = Api(config, logger)
    private var isEnabled = false

    private val cache: MutableMap<String, Map<String, Tier>> = mutableMapOf()
    private val cacheSeconds = config.getCacheSeconds()
    private val updateInterval = config.getUpdateInterval()
    private var plugin: PlaceholderAPIPlugin? = null

    private val queue: MutableList<String> = mutableListOf()

    private fun putInQueue(playerId: String) {
        queue.add(playerId)
    }

    init {
        if (api.test()) {
            isEnabled = true
        } else {
            logger.warning("Failed to connect to TierlistMC API")
        }

        try {
            createTasks()
        } catch (e: Exception) {
            logger.warning("Failed to create task")
            e.printStackTrace()
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
        return "1.5"
    }

    override fun onRequest(player: OfflinePlayer, params: String): String {
        val args: List<String> = if (params.startsWith("find_")) {
            params.substring(5).split("_")
        } else {
            params.split("_")
        }

        var tierType = args.getOrNull(0) ?: return "%tier_type_is_invalid%"
        if (tierType.isBlank() || !isTierTypeExists(tierType)) {
            return "%tier_type_is_invalid%"
        }
        tierType = tierType.lowercase()

        var playerId = player.name?.lowercase()

        if (playerId == null) {
            playerId = args.getOrNull(1) ?: return "%player_is_invalid%"

            if (playerId.isBlank() || playerId.length < 3 || playerId.length > 16) {
                return "%player_is_invalid%"
            }
        }

        if (!cache.containsKey(playerId)) {
            putInQueue(playerId)
            return ""
        }

        val data = cache[playerId]

        val name = data?.get(tierType)?.name ?: return ""
        return if (name.isNotBlank()) {
            config.getFormat(tierType).replace("{name}", name)
        } else {
            ""
        }
    }

    private fun createTasks() {
        if (plugin == null) {
            plugin = PlaceholderAPIPlugin.getInstance()
        }
        val updateRunnable = object : BukkitRunnable() {
            override fun run() {
                if (queue.isEmpty()) {
                    return
                }

                val batchPlayers = mutableListOf<String>()

                for (playerId in queue.toList()) {
                    if (batchPlayers.size >= config.getMaxBatchSize()) {
                        break
                    }

                    if (batchPlayers.contains(playerId)) {
                        continue
                    }

                    batchPlayers.add(playerId)
                }

                val future = api.batchRequest(Batch(batchPlayers))

                future.thenAcceptAsync { players ->
                    for (player in players) {
                        val tiers = mutableMapOf<String, Tier>()

                        for (tier in player.tiers) {
                            tiers[tier.key] = Tier(tier.value.name, tier.value.id)
                        }

                        cache[player.username.lowercase()] = tiers
                    }

                    queue.removeAll(batchPlayers)
                }
            }
        }

        val cacheRunnable = object : BukkitRunnable() {
            override fun run() {
                cache.clear()
            }
        }

        updateRunnable.runTaskTimerAsynchronously(plugin!!, 0, updateInterval)
        cacheRunnable.runTaskTimerAsynchronously(plugin!!, cacheSeconds * 20, cacheSeconds * 20)
    }
}