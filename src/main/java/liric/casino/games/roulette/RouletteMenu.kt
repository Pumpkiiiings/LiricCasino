package liric.casino.games.roulette

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class RouletteMenu(
    plugin: CasinoPlugin, 
    private val game: RouletteGame,
    private val player: Player
) : BaseMenu(plugin, "ruleta.yml") {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    fun open() {
        if (game.state == GameState.SPINNING) {
            player.sendMessage(msg("roulette.already-spinning"))
            return
        }

        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        gui.open(player)
    }

    override fun setupItems(gui: Gui) {
        val zeroSlot = config.getInt("zero-slot", 4)
        val zeroMat  = config.getMaterial("items.zero.material", Material.LIME_DYE)
        val numberPayout = plugin.config.getDouble("roulette.number-payout", 36.0)
        val payoutText = if (numberPayout % 1.0 == 0.0) numberPayout.toInt().toString() else numberPayout.toString()
        val zeroName = plugin.format(config.getString("items.zero.name").replace("{payout}", payoutText))
        val zeroLore = config.getComponentList("items.zero.lore")

        val zeroBet = ItemBuilder.from(zeroMat).amount(1)
            .name(zeroName).lore(zeroLore).flags(*ItemFlag.values())
            .asGuiItem { BetAmountMenu(plugin, game, BetType.Number(0), player).open() }
        gui.setItem(zeroSlot, zeroBet)

        val numberBase  = config.getInt("number-start-slot", 8)
        val redTemplate = config.getString("items.red-number.name-template", "<#FF2A2A><bold>NUMBER {num} <gray>(Pays x{payout})")
        val blkTemplate = config.getString("items.black-number.name-template", "<#666666><bold>NUMBER {num} <gray>(Pays x{payout})")
        val numLore      = config.getComponentList("items.red-number.lore")

        for (i in 1..36) {
            val color    = game.getNumberColor(i)
            val material = if (color == BetColor.RED) Material.RED_DYE else Material.INK_SAC
            val template = if (color == BetColor.RED) redTemplate else blkTemplate
            val name     = plugin.format(template.replace("{num}", i.toString()).replace("{payout}", payoutText))

            val item = ItemBuilder.from(material).amount(i)
                .name(name).lore(numLore).flags(*ItemFlag.values())
                .asGuiItem { BetAmountMenu(plugin, game, BetType.Number(i), player).open() }
            gui.setItem(numberBase + i, item)
        }

        data class ColorBtn(val cfgKey: String, val betColor: BetColor, val slotKey: String)
        val colorBtns = listOf(
            ColorBtn("items.color-red",   BetColor.RED,   "color-slots.red"),
            ColorBtn("items.color-green", BetColor.GREEN, "color-slots.green"),
            ColorBtn("items.color-black", BetColor.BLACK, "color-slots.black")
        )

        colorBtns.forEach { btn ->
            val mat  = config.getMaterial("${btn.cfgKey}.material", Material.WHITE_WOOL)
            val name = config.getComponent("${btn.cfgKey}.name")
            val lore = config.getComponentList("${btn.cfgKey}.lore")
            val slot = config.getInt(btn.slotKey)

            val guiItem = ItemBuilder.from(mat).name(name).lore(lore)
                .flags(*ItemFlag.values())
                .asGuiItem { BetAmountMenu(plugin, game, BetType.Color(btn.betColor), player).open() }
            gui.setItem(slot, guiItem)
        }
    }
}
