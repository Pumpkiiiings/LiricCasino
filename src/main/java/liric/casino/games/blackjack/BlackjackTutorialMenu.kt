package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class BlackjackTutorialMenu(
    plugin: CasinoPlugin, 
    private val player: Player
) : BaseMenu(plugin, "blackjack_tutorial.yml") {

    fun open() {
        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        gui.open(player)
    }

    override fun setupItems(gui: Gui) {
        val pageKeys = config.getKeys("pages")
        pageKeys.forEach { pageKey ->
            val path = "pages.$pageKey"
            val slot  = config.getInt("$path.slot", 0)
            val mat   = config.getMaterial("$path.material", Material.BOOK)
            val name  = config.getComponent("$path.name")
            val lore  = config.getComponentList("$path.lore")
            gui.setItem(slot, ItemBuilder.from(mat).name(name).lore(lore).flags(*ItemFlag.values()).asGuiItem())
        }

        val backSlot = config.getInt("back-button.slot", 22)
        val backMat  = config.getMaterial("back-button.material", Material.ARROW)
        val backName = config.getComponent("back-button.name")
        gui.setItem(backSlot, ItemBuilder.from(backMat).name(backName).asGuiItem {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            BlackjackChoiceMenu(plugin, player).open()
        })
    }
}
