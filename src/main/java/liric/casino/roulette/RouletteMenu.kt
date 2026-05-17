package liric.casino.roulette

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class RouletteMenu(private val plugin: CasinoPlugin, private val game: RouletteGame) {

    private fun cfg() = plugin.menuConfig("ruleta.yml")
    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    fun openBetMenu(player: Player) {
        if (game.state == GameState.SPINNING) {
            player.sendMessage(msg("roulette.already-spinning"))
            return
        }

        val cfg = cfg()

        val gui = Gui.gui()
            .title(cfg.getComponent("title"))
            .rows(cfg.getInt("rows", 6))
            .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
            .create()

        cfg.applyDecorations(gui)

        // ── Número 0 ──────────────────────────────────────────────────────
        val zeroSlot = cfg.getInt("zero-slot", 4)
        val zeroMat  = cfg.getMaterial("items.zero.material", Material.LIME_DYE)
        val zeroName = cfg.getComponent("items.zero.name")
        val zeroLore = cfg.getComponentList("items.zero.lore")

        val zeroBet = ItemBuilder.from(zeroMat).amount(1)
            .name(zeroName).lore(zeroLore).flags(*ItemFlag.values())
            .asGuiItem { BetAmountMenu(plugin, game, BetType.Number(0), player).open() }
        gui.setItem(zeroSlot, zeroBet)

        // ── Números 1-36 ──────────────────────────────────────────────────
        val numberBase  = cfg.getInt("number-start-slot", 8)
        val redTemplate = cfg.getString("items.red-number.name-template", "<#FF2A2A><bold>NÚMERO {num} <gray>(Paga x36)")
        val blkTemplate = cfg.getString("items.black-number.name-template", "<#666666><bold>NÚMERO {num} <gray>(Paga x36)")
        val numLore      = cfg.getComponentList("items.red-number.lore")

        for (i in 1..36) {
            val color    = game.getNumberColor(i)
            val material = if (color == BetColor.RED) Material.RED_DYE else Material.INK_SAC
            val template = if (color == BetColor.RED) redTemplate else blkTemplate
            val name     = plugin.format(template.replace("{num}", i.toString()))

            val item = ItemBuilder.from(material).amount(i)
                .name(name).lore(numLore).flags(*ItemFlag.values())
                .asGuiItem { BetAmountMenu(plugin, game, BetType.Number(i), player).open() }
            gui.setItem(numberBase + i, item)
        }

        // ── Apuestas de color ─────────────────────────────────────────────
        data class ColorBtn(val cfgKey: String, val betColor: BetColor, val slotKey: String)
        val colorBtns = listOf(
            ColorBtn("items.color-red",   BetColor.RED,   "color-slots.red"),
            ColorBtn("items.color-green", BetColor.GREEN, "color-slots.green"),
            ColorBtn("items.color-black", BetColor.BLACK, "color-slots.black")
        )

        colorBtns.forEach { btn ->
            val mat  = cfg.getMaterial("${btn.cfgKey}.material", Material.WHITE_WOOL)
            val name = cfg.getComponent("${btn.cfgKey}.name")
            val lore = cfg.getComponentList("${btn.cfgKey}.lore")
            val slot = cfg.getInt(btn.slotKey)

            val guiItem = ItemBuilder.from(mat).name(name).lore(lore)
                .flags(*ItemFlag.values())
                .asGuiItem { BetAmountMenu(plugin, game, BetType.Color(btn.betColor), player).open() }
            gui.setItem(slot, guiItem)
        }

        gui.open(player)
    }
}
