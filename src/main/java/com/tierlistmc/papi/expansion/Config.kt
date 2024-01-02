package com.tierlistmc.papi.expansion

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.util.logging.Logger

class Config(private val logger: Logger) {
    private val file = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")!!.dataFolder.resolve("tierlistmc.yml")
    private val config: YamlConfiguration

    init {
        val resource = TierlistMC::class.java.getResourceAsStream("/tierlistmc.yml")!!
        val defaultConfig = YamlConfiguration.loadConfiguration(resource.reader())
        config = if (file.exists()) {
            YamlConfiguration.loadConfiguration(file.reader()).apply {
                setDefaults(defaultConfig)
                options().copyDefaults(true)
            }
        } else {
            defaultConfig
        }
        config.save(file)
    }

    fun getApiKey(): String? {
        return config.getString("api-key")
    }

    fun getLanguage(): String? {
        return config.getString("language", "en")
    }

    fun getCacheSeconds(): Long {
        return config.getLong("cache-seconds", 60)
    }

    fun getReadTimeout(): Long {
        return config.getLong("read-timeout", 10)
    }

    fun isRetryOnConnectionFailure(): Boolean {
        return config.getBoolean("retry-on-connection-failure", true)
    }

    fun getConnectTimeout(): Long {
        return config.getLong("connect-timeout", 10)
    }

    fun getMaxIdleConnections(): Int {
        return config.getInt("max-idle-connections", 256)
    }

    fun getKeepAliveDuration(): Long {
        return config.getLong("keep-alive-duration", 10)
    }

    fun getFormat(tierType: String): String {
        val value = config.getString("formats.${tierType.uppercase()}", "&f{name}")!!
        if (!value.contains("{name}")) {
            logger.warning("Invalid format for tier type $tierType: $value")
        }
        return value
    }

    fun getUpdateInterval(): Long {
        return config.getLong("update-interval", 20)
    }

    fun getMaxBatchSize(): Int {
        return config.getInt("max-batch-size", 100)
    }
}