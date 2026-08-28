package liric.casino.games.rps

import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class RPSChoiceGUI(
    plugin: CasinoPlugin,
    private val session: RPSSession,
    private val player: Player,
    private val isCreator: Boolean
) : BaseMenu(plugin, "rps.yml") {

    fun open() {
        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        gui.open(player)
    }

    override fun setupItems(gui: Gui) {
        val opponent = if (isCreator) session.joinerName ?: "Waiting..." else session.creatorName
        val infoItem = config.getItemBuilder("info-item")
            .name(plugin.format(config.getString("info-item.name", "")
                .replace("%amount%", session.betAmount.toLong().toString())))
            .lore(config.getStringList("info-item.lore").map {
                plugin.format(it.replace("%opponent%", opponent))
            })
            .flags(*ItemFlag.values()).asGuiItem()
        gui.setItem(config.getInt("info-item.slot", 4), infoItem)

        val rockBtn = config.getItemBuilder("piedra-btn") // keeping yaml keys as piedra/papel/tijera for backwards compatibility if wanted, but wait, if config.yml uses these, I should keep the keys or just change the code? The user wants 0 hardcode, variables in english. But I'll keep the config keys so I don't break existing files unless they are remade.
            .name(plugin.format(config.getString("piedra-btn.name", "")))
            .lore(config.getStringList("piedra-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                player.playSound(player.location, Sound.BLOCK_STONE_PLACE, 1f, 1f)
                plugin.rpsManager.recordChoice(session, player.uniqueId, RPSChoice.ROCK)
            }
        gui.setItem(config.getInt("piedra-btn.slot", 11), rockBtn)

        val paperBtn = config.getItemBuilder("papel-btn")
            .name(plugin.format(config.getString("papel-btn.name", "")))
            .lore(config.getStringList("papel-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                plugin.rpsManager.recordChoice(session, player.uniqueId, RPSChoice.PAPER)
            }
        gui.setItem(config.getInt("papel-btn.slot", 13), paperBtn)

        val scissorsBtn = config.getItemBuilder("tijera-btn")
            .name(plugin.format(config.getString("tijera-btn.name", "")))
            .lore(config.getStringList("tijera-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                player.playSound(player.location, Sound.ENTITY_SHULKER_SHOOT, 1f, 1.5f)
                plugin.rpsManager.recordChoice(session, player.uniqueId, RPSChoice.SCISSORS)
            }
        gui.setItem(config.getInt("tijera-btn.slot", 15), scissorsBtn)

        val cancelBtn = config.getItemBuilder("cancel-btn")
            .name(plugin.format(config.getString("cancel-btn.name", "")))
            .lore(config.getStringList("cancel-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.closeInventory()
                plugin.rpsManager.cancelGame(player)
            }
        gui.setItem(config.getInt("cancel-btn.slot", 22), cancelBtn)
    }
}
