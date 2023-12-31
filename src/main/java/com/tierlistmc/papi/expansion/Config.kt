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

    fun getCacheSeconds(): Int {
        return config.getInt("cache-seconds", 60)
    }

    fun getColor(tierType: String): String {
        return config.getString("colors.$tierType", " &f")!!
    }
}