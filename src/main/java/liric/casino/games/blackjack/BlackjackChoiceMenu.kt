package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class BlackjackChoiceMenu(private val plugin: CasinoPlugin, private val player: Player) {

    private val cfg get() = plugin.menuConfig("blackjack_choice.yml")

    private val gui = Gui.gui()
        .title(cfg.getComponent("title"))
        .rows(cfg.getInt("rows", 3))
        .disableAllInteractions()
        .create()

    fun open() {
        cfg.applyDecorations(gui)

        // VS HOUSE
        val vsHouseSlot = cfg.getInt("items.vs-house.slot", 11)
        val vsHouseMat  = cfg.getMaterial("items.vs-house.material", Material.RED_DYE)
        val vsHouseName = cfg.getComponent("items.vs-house.name")
        val vsHouseLore = cfg.getComponentList("items.vs-house.lore")
        gui.setItem(vsHouseSlot, ItemBuilder.from(vsHouseMat).name(vsHouseName).lore(vsHouseLore)
            .flags(*ItemFlag.values()).asGuiItem {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                BlackjackBetMenu(plugin, player, isMultiplayer = false).open()
            })

        // TUTORIAL
        val tutSlot = cfg.getInt("items.tutorial.slot", 13)
        val tutMat  = cfg.getMaterial("items.tutorial.material", Material.WRITTEN_BOOK)
        val tutName = cfg.getComponent("items.tutorial.name")
        val tutLore = cfg.getComponentList("items.tutorial.lore")
        gui.setItem(tutSlot, ItemBuilder.from(tutMat).name(tutName).lore(tutLore)
            .flags(*ItemFlag.values()).asGuiItem {
                player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                BlackjackTutorialMenu(plugin, player).open()
            })

        // VS PLAYERS
        val vsPlayersSlot = cfg.getInt("items.vs-players.slot", 15)
        val vsPlayersMat  = cfg.getMaterial("items.vs-players.material", Material.DIAMOND)
        val vsPlayersName = cfg.getComponent("items.vs-players.name")
        val vsPlayersLore = cfg.getComponentList("items.vs-players.lore")
        gui.setItem(vsPlayersSlot, ItemBuilder.from(vsPlayersMat).name(vsPlayersName).lore(vsPlayersLore)
            .flags(*ItemFlag.values()).asGuiItem {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                BlackjackBetMenu(plugin, player, isMultiplayer = true).open()
            })

        gui.open(player)
    }
}
