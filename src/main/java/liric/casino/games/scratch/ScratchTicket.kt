package liric.casino.games.scratch

import dev.triumphteam.gui.builder.item.ItemBuilder
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ScratchTicket {
    fun getKey(plugin: CasinoPlugin) = NamespacedKey(plugin, "scratch_ticket")
    fun getTierKey(plugin: CasinoPlugin) = NamespacedKey(plugin, "scratch_tier")

    fun create(plugin: CasinoPlugin, tier: TicketTier, amount: Int): ItemStack {
        val config = plugin.menuConfig("scratch")
        val nameRaw = config.getString("ticket.name", "<#FFD700><bold>🎟 BOLETO {tier} 🎟</bold>")!!
            .replace("{tier}", tier.displayName)
            
        val loreRaw = config.getStringList("ticket.lore").map { 
            plugin.format(it.replace("{matches}", tier.matchRequired.toString())) 
        }

        val item = ItemBuilder.from(Material.PAPER)
            .amount(amount)
            .name(plugin.format(nameRaw))
            .lore(loreRaw)
            .flags(*ItemFlag.values())
            .build()

        val meta = item.itemMeta
        meta.persistentDataContainer.set(getKey(plugin), PersistentDataType.BYTE, 1.toByte())
        meta.persistentDataContainer.set(getTierKey(plugin), PersistentDataType.STRING, tier.name)
        item.itemMeta = meta
        return item
    }
}
