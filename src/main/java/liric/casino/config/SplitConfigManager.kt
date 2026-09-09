package liric.casino.config

import liric.casino.CasinoPlugin
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/** Keeps the public configuration split while exposing one merged read view to the game code. */
class SplitConfigManager(private val plugin: CasinoPlugin) {

    private val extraFiles = listOf("economy.yml", "prizes.yml")

    fun initialize() {
        plugin.saveDefaultConfig()
        extraFiles.forEach(::ensureExtraFile)
        plugin.reloadConfig()
        migrateLegacyConfig()
        reload()
    }

    fun reload() {
        plugin.reloadConfig()
        extraFiles.forEach { name ->
            val loaded = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, name))
            mergeLeaves(loaded, plugin.config)
        }
    }

    fun setMain(path: String, value: Any?) {
        val file = File(plugin.dataFolder, "config.yml")
        val main = YamlConfiguration.loadConfiguration(file)
        main.set(path, value)
        main.save(file)
        plugin.config.set(path, value)
    }

    private fun ensureExtraFile(name: String) {
        val file = File(plugin.dataFolder, name)
        if (!file.exists()) {
            plugin.saveResource(name, false)
            return
        }

        val current = YamlConfiguration.loadConfiguration(file)
        val defaults = plugin.getResource(name)?.reader()?.use(YamlConfiguration::loadConfiguration) ?: return
        current.setDefaults(defaults)
        current.options().copyDefaults(true)
        current.save(file)
    }

    private fun migrateLegacyConfig() {
        val main = plugin.config
        val hasLegacySections = main.contains("taxes") || main.contains("boosters") ||
            main.getKeys(false).any { key -> key in GAME_KEYS && hasLegacyGameData(main, key) }
        if (!hasLegacySections) return

        val economyFile = File(plugin.dataFolder, "economy.yml")
        val prizesFile = File(plugin.dataFolder, "prizes.yml")
        val economy = YamlConfiguration.loadConfiguration(economyFile)
        val prizes = YamlConfiguration.loadConfiguration(prizesFile)

        copySection(main, economy, "taxes")
        copySection(main, economy, "boosters")
        GAME_KEYS.forEach { game ->
            copySection(main, economy, "$game.bet")
            copySection(main, economy, "$game.uses")
        }
        copyValue(main, economy, "roulette.max-bet")
        copyValue(main, economy, "roulette.number-payout")
        copySection(main, economy, "roulette.color-multipliers")
        listOf("ticket-price", "max-tickets-per-player", "jackpot-start", "jackpot-contribution")
            .forEach { copyValue(main, economy, "lottery.$it") }

        copySection(main, prizes, "slots.luck-boosters")
        copyValue(main, prizes, "slots.prizes")
        mapOf(
            "basico" to "basic",
            "avanzado" to "advanced",
            "maestro" to "master",
            "gigante" to "giant"
        ).forEach { (oldName, newName) ->
            copySection(main, prizes, "scratch.tiers.$oldName", "scratch.tiers.$newName")
        }
        listOf("basic", "advanced", "master", "giant").forEach { tier ->
            copySection(main, prizes, "scratch.tiers.$tier")
        }
        copyValue(main, prizes, "scratch.prizes")

        economy.set("boosters.vip.permission", main.getString("boosters.vip.permission")
            ?: main.getString("boosters.vip.permiso")
            ?: economy.getString("boosters.vip.permission"))
        economy.set("boosters.mvp.permission", main.getString("boosters.mvp.permission")
            ?: main.getString("boosters.mvp.permiso")
            ?: economy.getString("boosters.mvp.permission"))
        economy.set("boosters.vip.permiso", null)
        economy.set("boosters.mvp.permiso", null)

        economy.save(economyFile)
        prizes.save(prizesFile)

        main.set("taxes", null)
        main.set("boosters", null)
        GAME_KEYS.forEach { game ->
            main.set("$game.bet", null)
            main.set("$game.uses", null)
        }
        main.set("roulette.max-bet", null)
        main.set("roulette.number-payout", null)
        main.set("roulette.color-multipliers", null)
        listOf("ticket-price", "max-tickets-per-player", "jackpot-start", "jackpot-contribution")
            .forEach { main.set("lottery.$it", null) }
        main.set("slots.luck-boosters", null)
        main.set("slots.prizes", null)
        main.set("scratch.tiers", null)
        main.set("scratch.prizes", null)
        plugin.saveConfig()
    }

    private fun hasLegacyGameData(config: ConfigurationSection, game: String): Boolean =
        config.contains("$game.bet") || config.contains("$game.uses") ||
            config.contains("$game.prizes") || config.contains("$game.tiers") ||
            config.contains("$game.luck-boosters") || config.contains("$game.max-bet")

    private fun copySection(
        source: ConfigurationSection,
        target: ConfigurationSection,
        sourcePath: String,
        targetPath: String = sourcePath
    ) {
        val section = source.getConfigurationSection(sourcePath) ?: return
        section.getKeys(true).forEach { child ->
            if (section.getConfigurationSection(child) == null) target.set("$targetPath.$child", section.get(child))
        }
    }

    private fun copyValue(source: ConfigurationSection, target: ConfigurationSection, path: String) {
        if (source.contains(path)) target.set(path, source.get(path))
    }

    private fun mergeLeaves(source: ConfigurationSection, target: ConfigurationSection) {
        source.getKeys(true).forEach { path ->
            if (source.getConfigurationSection(path) == null) target.set(path, source.get(path))
        }
    }

    companion object {
        private val GAME_KEYS = setOf(
            "roulette", "blackjack", "slots", "scratch", "lottery",
            "coinflip", "rps", "ttt", "racing", "poker"
        )
    }
}
