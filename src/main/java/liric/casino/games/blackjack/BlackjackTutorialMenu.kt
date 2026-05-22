package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class BlackjackTutorialMenu(private val plugin: CasinoPlugin, private val player: Player) {

    private val cfg get() = plugin.menuConfig("blackjack_tutorial.yml")

    private val gui = Gui.gui()
        .title(cfg.getComponent("title"))
        .rows(cfg.getInt("rows", 3))
        .disableAllInteractions()
        .create()

    fun open() {
        cfg.applyDecorations(gui)

        // Páginas dinámicas desde YAML
        val pageKeys = cfg.getKeys("pages")
        pageKeys.forEach { pageKey ->
            val path = "pages.$pageKey"
            val slot  = cfg.getInt("$path.slot", 0)
            val mat   = cfg.getMaterial("$path.material", Material.BOOK)
            val name  = cfg.getComponent("$path.name")
            val lore  = cfg.getComponentList("$path.lore")
            gui.setItem(slot, ItemBuilder.from(mat).name(name).lore(lore).flags(*ItemFlag.values()).asGuiItem())
        }

        // Botón volver
        val backSlot = cfg.getInt("back-button.slot", 22)
        val backMat  = cfg.getMaterial("back-button.material", Material.ARROW)
        val backName = cfg.getComponent("back-button.name")
        gui.setItem(backSlot, ItemBuilder.from(backMat).name(backName).asGuiItem {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            BlackjackChoiceMenu(plugin, player).open()
        })

        gui.open(player)
    }
}
