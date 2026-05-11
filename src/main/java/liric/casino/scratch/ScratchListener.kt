package liric.casino.scratch

import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.inventory.EquipmentSlot

class ScratchListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
            val item = event.item ?: return

            if (item.type == Material.PAPER && item.itemMeta?.persistentDataContainer?.has(ScratchTicket.getKey(plugin), PersistentDataType.BYTE) == true) {
                event.isCancelled = true
                if (event.hand != EquipmentSlot.HAND) return

                val pdc = item.itemMeta?.persistentDataContainer
                val tierName = pdc?.get(ScratchTicket.getTierKey(plugin), PersistentDataType.STRING)
                val tier = TicketTier.values().find { it.name == tierName } ?: TicketTier.BASIC

                item.amount -= 1

                ScratchMenu(plugin, event.player, tier).open()
            }
        }
    }
}
