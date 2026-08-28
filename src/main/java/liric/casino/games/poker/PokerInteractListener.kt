package liric.casino.games.poker

import liric.casino.CasinoPlugin
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType

class PokerInteractListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler
    fun onPokerClick(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked

        if (entity is Interaction && entity.persistentDataContainer.has(plugin.pokerManager.pokerKey, PersistentDataType.BYTE)) {
            if (!plugin.isGameEnabled("poker")) {

                return
            }
            plugin.pokerGame.addPlayer(event.player)
        }
    }
}
