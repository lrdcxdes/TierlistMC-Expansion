package com.tierlistmc.papi.expansion

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration

class Config {
    private val file = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")!!.dataFolder.resolve("tierlistmc.yml")
    private val config: YamlConfiguration

    init {
        if (!file.exists()) {
            file.createNewFile()
        }
        config = YamlConfiguration.loadConfiguration(file)
        config.options().copyDefaults(true)
        config.addDefault("api-key", "paste-your-api-key-here")
        config.addDefault("language", "en")
        config.addDefault("cache-seconds", 60)
        config.addDefault("read-timeout", 10)
        config.addDefault("retry-on-connection-failure", true)
        config.addDefault("connect-timeout", 10)
        config.addDefault("max-idle-connections", 256)
        config.addDefault("keep-alive-duration", 10)
        Color.entries.forEach { color ->
            config.addDefault("colors.${color.name}", " &f")
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

    fun getColor(tierType: String): String {
        return config.getString("colors.$tierType", " &f")!!
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
}