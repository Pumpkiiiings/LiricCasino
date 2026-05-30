package liric.casino.games.blackjack

import liric.casino.CasinoPlugin
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType

class BlackjackInteractListener(private val plugin: CasinoPlugin) : Listener {
    @EventHandler
    fun onBlackjackClick(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (entity is Interaction && entity.persistentDataContainer.has(plugin.blackjackManager.bjKey, PersistentDataType.BYTE)) {
            if (!plugin.isGameEnabled("blackjack")) {
                event.player.sendMessage(plugin.messages.get("general.game-disabled"))
                return
            }
            // Ya no abre las apuestas, abre el Menú de Selección (Vs House o Vs Jugadores)
            BlackjackChoiceMenu(plugin, event.player).open()
        }
    }
}
