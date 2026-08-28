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
        val item = ItemBuilder.from(Material.PAPER)
            .amount(amount)
            .name(plugin.format("<#FFD700><bold>🎟 BOLETO ${tier.displayName} 🎟</bold>"))
            .lore(
                plugin.format("<#E0E0E0>Haz <#00FF7F><bold>Click Derecho</bold> <#E0E0E0>al aire"),
                plugin.format("<#E0E0E0>con este boleto para jugar.</#E0E0E0>"),
                plugin.format(""),
                plugin.format("<#FFB400>¡Encuentra ${tier.matchRequired} iguales para ganar!</#FFB400>")
            )
            .flags(*ItemFlag.values())
            .build()

        val meta = item.itemMeta
        meta.persistentDataContainer.set(getKey(plugin), PersistentDataType.BYTE, 1.toByte())
        meta.persistentDataContainer.set(getTierKey(plugin), PersistentDataType.STRING, tier.name)
        item.itemMeta = meta
        return item
    }
}
