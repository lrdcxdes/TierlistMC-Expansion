package com.tierlistmc.papi.expansion

import me.clip.placeholderapi.PlaceholderAPIPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.clip.placeholderapi.expansion.Taskable
import org.bukkit.OfflinePlayer
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger


@Suppress("unused")
class TierlistMC : PlaceholderExpansion(), Listener, Taskable {
    private val logger = Logger.getLogger("PlaceholderAPI")

    private val config = Config(logger)
    private val api = Api(config, logger)
    private var isEnabled = false

    private val cache: MutableMap<String, Map<String, Tier>> = mutableMapOf()
    private val cacheSeconds = config.getCacheSeconds()
    private val updateInterval = config.getUpdateInterval()
    private var plugin: PlaceholderAPIPlugin? = null

    private val queue: MutableSet<String> = mutableSetOf()

    private var updateRunnableTask: BukkitTask? = null
    private var cacheRunnableTask: BukkitTask? = null

    private var nowUpdating = false

    private fun putInQueue(playerId: String) {
        if (queue.contains(playerId)) {
            return
        }
        queue.add(playerId)
    }

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
        return "1.6"
    }

    override fun onRequest(player: OfflinePlayer, params: String): String {
        val args: List<String> = if (params.startsWith("find_")) {
            params.substring(5).split("_")
        } else {
            params.split("_")
        }

        val tierType = args.getOrNull(0)?.lowercase() ?: return "%tier_type_is_invalid%"
        if (tierType.isBlank() || !isTierTypeExists(tierType)) {
            return "%tier_type_is_invalid%"
        }

        var playerId = player.name?.lowercase()
        playerId = playerId ?: args.getOrNull(1)?.lowercase() ?: return "%player_is_invalid%"

        if (playerId.isBlank() || playerId.length < 3 || playerId.length > 16) {
            return "%player_is_invalid%"
        }

        val data = cache.computeIfAbsent(playerId) {
            putInQueue(playerId)
            mutableMapOf()
        }

        val name = data[tierType]?.name ?: return ""
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
                if (nowUpdating || queue.isEmpty()) {
                    return
                }

                nowUpdating = true

                val batchPlayers = queue.take(config.getMaxBatchSize())

                val future: CompletableFuture<List<Player>>

                try {
                    future = api.batchRequest(Batch(batchPlayers))
                } catch (e: Exception) {
                    logger.warning("Failed to get JSON")
                    e.printStackTrace()
                    logger.warning("BatchPlayers: $batchPlayers")
                    nowUpdating = false
                    return
                }

                future.thenAcceptAsync { player ->
                    for (p in player) {
                        val tiers = mutableMapOf<String, Tier>()

                        for (tier in p.tiers) {
                            tiers[tier.key] = Tier(tier.value.name, tier.value.id)
                        }

                        cache[p.username.lowercase()] = tiers
                    }
                }

                queue -= batchPlayers.toSet()

                nowUpdating = false
            }
        }

        val cacheRunnable = object : BukkitRunnable() {
            override fun run() {
                cache.clear()
            }
        }

        updateRunnableTask = updateRunnable.runTaskTimerAsynchronously(plugin!!, 0, updateInterval)
        cacheRunnableTask = cacheRunnable.runTaskTimerAsynchronously(plugin!!, cacheSeconds * 20, cacheSeconds * 20)
    }

    override fun start() {
        try {
            createTasks()
        } catch (e: Exception) {
            logger.warning("Failed to create task")
            e.printStackTrace()
        }
    }

    override fun stop() {
        updateRunnableTask?.cancel()
        cacheRunnableTask?.cancel()

        updateRunnableTask = null
        cacheRunnableTask = null

        cache.clear()
        queue.clear()
    }
}