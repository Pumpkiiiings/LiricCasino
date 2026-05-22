package liric.casino.games.rps

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.config.MenuConfig
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import kotlin.text.format
import kotlin.text.get
import kotlin.text.toLong

class RPSChoiceGUI(
    private val plugin: CasinoPlugin,
    private val session: RPSSession,
    private val player: Player,
    private val isCreator: Boolean
) {
    fun open() {
        val config = plugin.menuConfig("rps.yml")
        val gui = Gui.gui()
            .title(plugin.format(config.getString("title", "<#FF5555>⚔ <gold><bold>Piedra Papel Tijera</bold> <#FF5555>⚔")))
            .rows(config.getInt("rows", 3))
            .disableAllInteractions()
            .create()

        // Decoraciones
        config.getMapList("decorations").forEach { dec ->
            val mat = Material.valueOf(dec["material"].toString())
            val name = plugin.format(dec["name"].toString())
            val slots = dec["slots"] as? List<*> ?: emptyList<Any>()
            val item = ItemBuilder.from(mat).name(name).asGuiItem()
            slots.forEach { slot -> gui.setItem(slot.toString().toInt(), item) }
        }

        // Info
        val opponent = if (isCreator) session.joinerName ?: "Esperando..." else session.creatorName
        val infoItem = config.getItemBuilder("info-item")
            .name(plugin.format(config.getString("info-item.name", "")
                .replace("%amount%", session.betAmount.toLong().toString())))
            .lore(config.getStringList("info-item.lore").map {
                plugin.format(it.replace("%opponent%", opponent))
            })
            .flags(*ItemFlag.values()).asGuiItem()
        gui.setItem(config.getInt("info-item.slot", 4), infoItem)

        // Piedra
        val piedraBtn = config.getItemBuilder("piedra-btn")
            .name(plugin.format(config.getString("piedra-btn.name", "")))
            .lore(config.getStringList("piedra-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                player.playSound(player.location, Sound.BLOCK_STONE_PLACE, 1f, 1f)
                plugin.rpsManager.recordChoice(session, player.uniqueId, RPSChoice.PIEDRA)
            }
        gui.setItem(config.getInt("piedra-btn.slot", 11), piedraBtn)

        // Papel
        val papelBtn = config.getItemBuilder("papel-btn")
            .name(plugin.format(config.getString("papel-btn.name", "")))
            .lore(config.getStringList("papel-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                plugin.rpsManager.recordChoice(session, player.uniqueId, RPSChoice.PAPEL)
            }
        gui.setItem(config.getInt("papel-btn.slot", 13), papelBtn)

        // Tijera
        val tijeraBtn = config.getItemBuilder("tijera-btn")
            .name(plugin.format(config.getString("tijera-btn.name", "")))
            .lore(config.getStringList("tijera-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                player.playSound(player.location, Sound.ENTITY_SHULKER_SHOOT, 1f, 1.5f)
                plugin.rpsManager.recordChoice(session, player.uniqueId, RPSChoice.TIJERA)
            }
        gui.setItem(config.getInt("tijera-btn.slot", 15), tijeraBtn)

        // Cancelar
        val cancelBtn = config.getItemBuilder("cancel-btn")
            .name(plugin.format(config.getString("cancel-btn.name", "")))
            .lore(config.getStringList("cancel-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                plugin.rpsManager.cancelGame(player)
            }
        gui.setItem(config.getInt("cancel-btn.slot", 22), cancelBtn)

        gui.open(player)
    }
}
