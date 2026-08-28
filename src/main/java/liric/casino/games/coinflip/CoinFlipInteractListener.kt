package liric.casino.games.coinflip

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent

class CoinFlipInteractListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder
        if (holder is CoinFlipMenu) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val holder = event.inventory.holder
        if (holder is CoinFlipMenu) {
            event.isCancelled = true
        }
    }
}
