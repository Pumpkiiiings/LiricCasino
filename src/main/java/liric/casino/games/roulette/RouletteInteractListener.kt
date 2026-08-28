package liric.casino.games.roulette

import liric.casino.CasinoPlugin
import liric.casino.packet.FakeInteraction
import liric.casino.packet.PlayerInteractFakeEntityEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class RouletteInteractListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler
    fun onRouletteClick(event: PlayerInteractFakeEntityEvent) {
        val entity = event.fakeEntity

        if (entity is FakeInteraction) {
            val isRoulette = plugin.rouletteManager.isRouletteInteraction(entity)
            if (!isRoulette) return

            // Since PacketEvents listeners fire asynchronously, we must sync to main thread for Bukkit API calls
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (!plugin.isGameEnabled("roulette")) {
                    return@Runnable
                }
                if (plugin.rouletteGame.state == GameState.SPINNING) {
                    event.player.sendMessage(plugin.messages.get("roulette.already-spinning-interact"))
                    return@Runnable
                }

                val menu = RouletteMenu(plugin, plugin.rouletteGame, event.player)
                menu.open()
            })
        }
    }
}
