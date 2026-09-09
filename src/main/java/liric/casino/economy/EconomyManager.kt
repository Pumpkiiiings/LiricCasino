package liric.casino.economy

import liric.casino.CasinoPlugin
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class EconomyManager(private val plugin: CasinoPlugin) : Listener {

    var vault: Economy? = null

    fun setupVault(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            return false
        }
        vault = rsp.provider
        return vault != null
    }

    fun has(player: OfflinePlayer, amount: Double): Boolean {
        return vault?.has(player, amount) ?: false
    }

    fun withdrawPlayer(player: OfflinePlayer, amount: Double): net.milkbowl.vault.economy.EconomyResponse? {
        return vault?.withdrawPlayer(player, amount)
    }

    fun depositPlayer(player: OfflinePlayer, amount: Double): net.milkbowl.vault.economy.EconomyResponse? {
        return vault?.depositPlayer(player, amount)
    }

    fun withdraw(player: OfflinePlayer, amount: Double) = withdrawPlayer(player, amount)
    fun deposit(player: OfflinePlayer, amount: Double) = depositPlayer(player, amount)
    
    fun getPlayerBooster(player: Player): Double {
        val section = plugin.config.getConfigurationSection("boosters") ?: return 1.0
        return section.getKeys(false).mapNotNull { key ->
            val permission = section.getString("$key.permission")
                ?: section.getString("$key.permiso")
                ?: return@mapNotNull null
            if (player.hasPermission(permission)) section.getDouble("$key.booster", 1.0) else null
        }.filter { it.isFinite() && it > 0.0 }.maxOrNull() ?: 1.0
    }
    
    fun getMaxBet(player: Player?, gameKey: String = ""): Double {
        if (gameKey.isBlank()) return plugin.config.getDouble("max-bet", 10000.0)
        val direct = "$gameKey.max-bet"
        if (plugin.config.contains(direct)) return plugin.config.getDouble(direct, 10000.0)

        val defaultMax = plugin.config.getDouble("$gameKey.bet.default.max", 10000.0)
        if (player == null) return defaultMax
        val ranks = plugin.config.getConfigurationSection("$gameKey.bet.ranks") ?: return defaultMax
        return ranks.getKeys(false).mapNotNull { rank ->
            val permission = ranks.getString("$rank.permission") ?: return@mapNotNull null
            if (player.hasPermission(permission)) ranks.getDouble("$rank.max", defaultMax) else null
        }.maxOrNull()?.coerceAtLeast(defaultMax) ?: defaultMax
    }

    private val betCallbacks = mutableMapOf<java.util.UUID, (Double) -> Unit>()

    fun openCustomBetChat(player: Player, callback: (Double) -> Unit) {
        betCallbacks[player.uniqueId] = callback
    }

    @org.bukkit.event.EventHandler
    fun onChat(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
        val callback = betCallbacks.remove(event.player.uniqueId) ?: return
        event.isCancelled = true
        val amount = event.message.toDoubleOrNull() ?: -1.0
        liric.casino.util.SchedulerUtil.runGlobal(plugin, Runnable {
            callback(amount)
        })
    }
}
