package liric.casino.economy

import liric.casino.CasinoPlugin
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class EconomyManager(private val plugin: CasinoPlugin) {
    var vault: Economy? = null

    // El Prefix Oficial
    private val prefix = "<#FF0000><bold>CASINO</bold></#FF0000> <gray>»</gray>"

    fun setupVault(): Boolean {
        if (plugin.server.pluginManager.getPlugin("Vault") == null) return false
        val rsp = plugin.server.servicesManager.getRegistration(Economy::class.java)
        if (rsp != null) vault = rsp.provider
        return vault != null
    }

    // Cambiado a 'getPlayerBooster' para que RouletteGame lo encuentre
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

    // Cambiado a 'withdrawPlayer' y ahora devuelve el objeto de respuesta de Vault
    fun withdrawPlayer(player: Player, amount: Double): EconomyResponse {
        return vault?.withdrawPlayer(player, amount) ?: EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Vault no cargado")
    }

    // Función para depositar a jugadores Online (con mensajes bonitos)
    fun depositPlayer(player: Player, amount: Double) {
        vault?.depositPlayer(player, amount)
        // Aquí podrías añadir un log o mensaje, pero RouletteGame ya lo maneja
    }

    // Función para depositar a jugadores Offline (Necesario para el bucle de ganadores)
    fun depositPlayer(player: OfflinePlayer, amount: Double) {
        vault?.depositPlayer(player, amount)
    }

    // Función antigua por si la usas en otros lados, ahora con colores premium
    fun depositWin(player: Player, baseAmount: Double) {
        val multiplier = getPlayerBooster(player)
        val finalAmount = baseAmount * multiplier
        vault?.depositPlayer(player, finalAmount)

        player.sendMessage(plugin.format("$prefix <#00FF7F><bold>¡GANASTE!</bold> <#FFB400>$$finalAmount <#E0E0E0>(Booster: x$multiplier)</#E0E0E0>"))
    }
}
