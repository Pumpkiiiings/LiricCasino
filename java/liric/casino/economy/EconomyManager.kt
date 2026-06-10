package liric.casino.economy

import liric.casino.CasinoPlugin
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import liric.casino.util.SchedulerUtil
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EconomyManager(private val plugin: CasinoPlugin) : Listener {
    var vault: Economy? = null

    private val pendingBets = ConcurrentHashMap<UUID, (Double) -> Unit>()

    private val prefix = "<#FF0000><bold>CASINO</bold></#FF0000> <gray>»</gray>"

    fun setupVault(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) return false
        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp != null) vault = rsp.provider
        return vault != null
    }


    fun getPlayerBooster(player: Player): Double {
        val config = plugin.config.getConfigurationSection("boosters") ?: return 1.0
        var highestMultiplier = 1.0

        for (key in config.getKeys(false)) {
            val perm = config.getString("$key.permiso") ?: continue
            val boost = config.getDouble("$key.booster", 1.0)

            if (player.hasPermission(perm) && boost > highestMultiplier) {
                highestMultiplier = boost
            }
        }
        return highestMultiplier
    }


    fun withdrawPlayer(player: Player, amount: Double): EconomyResponse {
        return vault?.withdrawPlayer(player, amount) ?: EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Vault no cargado")
    }


    fun depositPlayer(player: Player, amount: Double) {
        vault?.depositPlayer(player, amount)

    }


    fun depositPlayer(player: OfflinePlayer, amount: Double) {
        vault?.depositPlayer(player, amount)
    }


    fun depositWin(player: Player, baseAmount: Double) {
        val multiplier = getPlayerBooster(player)
        val finalAmount = baseAmount * multiplier
        vault?.depositPlayer(player, finalAmount)

        player.sendMessage(plugin.format("$prefix <#00FF7F><bold>¡GANASTE!</bold> <#FFB400>$$finalAmount <#E0E0E0>(Booster: x$multiplier)</#E0E0E0>"))
    }

    fun getMaxBet(player: Player, gameKey: String): Double {
        val betSection = plugin.config.getConfigurationSection("$gameKey.bet") ?: return 100000.0
        val defaultMax = betSection.getDouble("default.max", 100000.0)
        val ranksSection = betSection.getConfigurationSection("ranks") ?: return defaultMax

        var highestMax = defaultMax
        for (rank in ranksSection.getKeys(false)) {
            val perm = ranksSection.getString("$rank.permission") ?: continue
            val max = ranksSection.getDouble("$rank.max", defaultMax)
            if (player.hasPermission(perm) && max > highestMax) {
                highestMax = max
            }
        }
        return highestMax
    }

    fun openCustomBetChat(player: Player, callback: (Double) -> Unit) {
        pendingBets[player.uniqueId] = callback
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val callback = pendingBets[player.uniqueId] ?: return

        event.isCancelled = true
        val input = event.message.trim()

        SchedulerUtil.runGlobal(plugin) {
            pendingBets.remove(player.uniqueId)

            if (input.equals("cancelar", ignoreCase = true) || input.equals("cancel", ignoreCase = true)) {
                player.sendMessage(plugin.format("<gray>Apuesta cancelada."))
                return@runGlobal
            }

            val amount = input.toDoubleOrNull()
            if (amount == null || amount < 0) {
                player.sendMessage(plugin.format("<red>Cantidad inválida. Ingresa un número válido."))
                return@runGlobal
            }

            callback(amount)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        pendingBets.remove(event.player.uniqueId)
    }
}
