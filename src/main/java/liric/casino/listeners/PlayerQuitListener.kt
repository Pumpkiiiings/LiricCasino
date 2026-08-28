package liric.casino.listeners

import liric.casino.CasinoPlugin
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId

        plugin.handleDisconnect(uuid)
    }
}
