package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import liric.casino.util.ValidationUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class BlackjackChoiceMenu(
    plugin: CasinoPlugin, 
    private val player: Player
) : BaseMenu(plugin, "blackjack_choice.yml") {

    fun open() {
        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        gui.open(player)
    }

    override fun setupItems(gui: Gui) {
        val vsHouseSlot = config.getInt("items.vs-house.slot", 11)
        val vsHouseMat  = config.getMaterial("items.vs-house.material", Material.RED_DYE)
        val vsHouseName = config.getComponent("items.vs-house.name")
        val vsHouseLore = config.getComponentList("items.vs-house.lore")
        gui.setItem(vsHouseSlot, ItemBuilder.from(vsHouseMat).name(vsHouseName).lore(vsHouseLore)
            .flags(*ItemFlag.values()).asGuiItem {
                if (!ValidationUtil.canPlayDaily(plugin, player, "blackjack")) {
                    gui.close(player)
                    return@asGuiItem
                }
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                BlackjackBetMenu(plugin, player, isMultiplayer = false).open()
            })

        val tutSlot = config.getInt("items.tutorial.slot", 13)
        val tutMat  = config.getMaterial("items.tutorial.material", Material.WRITTEN_BOOK)
        val tutName = config.getComponent("items.tutorial.name")
        val tutLore = config.getComponentList("items.tutorial.lore")
        gui.setItem(tutSlot, ItemBuilder.from(tutMat).name(tutName).lore(tutLore)
            .flags(*ItemFlag.values()).asGuiItem {
                player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                BlackjackTutorialMenu(plugin, player).open()
            })

        val vsPlayersSlot = config.getInt("items.vs-players.slot", 15)
        val vsPlayersMat  = config.getMaterial("items.vs-players.material", Material.DIAMOND)
        val vsPlayersName = config.getComponent("items.vs-players.name")
        val vsPlayersLore = config.getComponentList("items.vs-players.lore")
        gui.setItem(vsPlayersSlot, ItemBuilder.from(vsPlayersMat).name(vsPlayersName).lore(vsPlayersLore)
            .flags(*ItemFlag.values()).asGuiItem {
                if (!ValidationUtil.canPlayDaily(plugin, player, "blackjack")) {
                    gui.close(player)
                    return@asGuiItem
                }
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                BlackjackBetMenu(plugin, player, isMultiplayer = true).open()
            })
    }
}
