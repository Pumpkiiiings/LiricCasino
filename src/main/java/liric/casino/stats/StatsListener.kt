package liric.casino.stats

import liric.casino.CasinoPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class StatsListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        plugin.statsManager.loadAsync(event.player.uniqueId, event.player.name)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        plugin.statsManager.unload(event.player.uniqueId)
    }
}
