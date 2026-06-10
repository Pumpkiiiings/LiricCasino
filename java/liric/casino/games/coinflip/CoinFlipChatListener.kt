package liric.casino.games.coinflip

import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent

class CoinFlipChatListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (!plugin.coinFlipManager.hasPendingChat(player.uniqueId)) return

        event.isCancelled = true
        val input = event.message.trim()

        SchedulerUtil.runGlobal(plugin) {
            plugin.coinFlipManager.removePendingChat(player.uniqueId)

            if (input.equals("cancelar", ignoreCase = true) || input.equals("cancel", ignoreCase = true)) {
                player.sendMessage(plugin.format("<gray>Creación de juego cancelada."))
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 0.8f)
                return@runGlobal
            }

            val amount = CoinFlipMenu.parseAmount(input)
            if (amount == null) {
                player.sendMessage(plugin.messages.get("coinflip.invalid-number"))
                return@runGlobal
            }

            plugin.coinFlipManager.createGame(player, amount)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.coinFlipManager.removePendingChat(event.player.uniqueId)
    }
}
