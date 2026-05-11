package liric.casino.roulettemix

import liric.casino.CasinoPlugin
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.persistence.PersistentDataType

class RouletteMixInteractListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler
    fun onRouletteClick(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked

        if (entity is Interaction && entity.persistentDataContainer.has(plugin.rouletteMixManager.rouletteMixKey, PersistentDataType.STRING)) {
            if (plugin.rouletteMixGame.state == GameState.SPINNING) {
                event.player.sendMessage(plugin.format("<#FF0000><bold>CASINO</bold></#FF0000> <gray>»</gray> <#FF5555>¡La ruleta está girando, espera a que termine!</#FF5555>"))
                return
            }

            val menu = RouletteMixMenu(plugin, plugin.rouletteMixGame)
            menu.openBetMenu(event.player)
        }
    }
}
