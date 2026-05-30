package liric.casino.util

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object ConfigUpdater {


    fun updateConfig(file: File, resourcePath: String) {
        if (!file.exists()) return

        val currentConfig = YamlConfiguration.loadConfiguration(file)
        val defaultResource = this::class.java.getResourceAsStream(resourcePath) ?: return
        val defaultConfig = YamlConfiguration.loadConfiguration(java.io.InputStreamReader(defaultResource, Charsets.UTF_8))

        if (mergeConfigs(currentConfig, defaultConfig)) {
            currentConfig.save(file)
        }
    }

    private fun mergeConfigs(current: ConfigurationSection, default: ConfigurationSection): Boolean {
        var changed = false

        for (key in default.getKeys(false)) {
            if (!current.contains(key)) {
                current.set(key, default.get(key))
                changed = true
            } else if (default.getConfigurationSection(key) != null) {
                val currentSection = current.getConfigurationSection(key)
                val defaultSection = default.getConfigurationSection(key)

                if (currentSection == null || (defaultSection != null && mergeConfigs(currentSection, defaultSection))) {
                    changed = true
                }
            }
        }
        return changed
    }
}