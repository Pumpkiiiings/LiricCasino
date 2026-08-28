package liric.casino.games.racing

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
import kotlin.toString

class RaceBetGUI(
    private val plugin: CasinoPlugin,
    private val track: RaceTrack,
    private val player: Player
) {
    private val mgr get() = plugin.raceManager
    private var selectedBet = 1000.0
    private var selectedHorse: Int? = null

    fun open() {
        val config = plugin.menuConfig("racing.yml")
        val gui = Gui.gui()
            .title(plugin.format(config.getString("title", "<#FFD700>🏇 <gold><bold>CARRERAS — Elige tu caballo</bold>")))
            .rows(config.getInt("rows", 5))
            .disableAllInteractions()
            .create()

        val betAmounts = config.getDoubleList("bet-amounts")
        if (betAmounts.isNotEmpty() && !betAmounts.contains(selectedBet)) {
            selectedBet = betAmounts.first()
        }

        refresh(gui, config)
        gui.open(player)
    }

    private fun refresh(gui: Gui, config: MenuConfig) {

        config.getMapList("decorations").forEach { dec ->
            val mat = Material.valueOf(dec["material"].toString())
            val name = plugin.format(dec["name"].toString())
            val slots = dec["slots"] as? List<*> ?: emptyList<Any>()
            val item = ItemBuilder.from(mat).name(name).asGuiItem()
            slots.forEach { slot -> gui.setItem(slot.toString().toInt(), item) }
        }

        val horses = mgr.getHorses()


        val horseSlots = config.getIntegerList("horse-slots")
        horses.forEachIndexed { idx, horse ->
            val slot = if (idx < horseSlots.size) horseSlots[idx] else return@forEachIndexed
            val isSelected = selectedHorse == horse.id

            val itemKey = if (isSelected) "horse-item-selected" else "horse-item"
            val itemStrName = config.getString("$itemKey.name", "")
                .replace("%horse_emoji%", horse.emoji)
                .replace("%horse_name%", horse.name)

            val loreLines = config.getStringList("$itemKey.lore").map {
                plugin.format(it.replace("%horse_odds%", horse.oddsMult.toString())
                    .replace("%win_chance%", winChancePct(horse, horses).toString()))
            }

            val btn = config.getItemBuilder(itemKey)
                .name(plugin.format(itemStrName))
                .lore(loreLines)
                .flags(*ItemFlag.values())
                .asGuiItem {
                    selectedHorse = horse.id
                    player.playSound(player.location, Sound.ENTITY_HORSE_ARMOR, 1f, 1.2f)
                    refresh(gui, config)
                    gui.update()
                }
            gui.setItem(slot, btn)
        }


        val betAmounts = config.getDoubleList("bet-amounts")
        betAmounts.forEachIndexed { idx, amount ->
            val slot = 36 + idx
            val isSelected = selectedBet == amount
            val itemKey = if (isSelected) "bet-item-selected" else "bet-item"

            val btnName = config.getString("$itemKey.name", "")
                .replace("%amount%", amount.toLong().toString())

            val btn = config.getItemBuilder(itemKey)
                .name(plugin.format(btnName))
                .flags(*ItemFlag.values())
                .asGuiItem {
                    selectedBet = amount
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                    refresh(gui, config)
                    gui.update()
                }
            gui.setItem(slot, btn)
        }


        val horseSelected = selectedHorse
        if (horseSelected != null) {
            val horse = horses.firstOrNull { it.id == horseSelected }
            val potWin = selectedBet * (horse?.oddsMult ?: 3.0)

            val betBtnStr = config.getString("bet-action-btn.name", "")
            val betBtnLore = config.getStringList("bet-action-btn.lore").map {
                plugin.format(it.replace("%horse_emoji%", horse?.emoji ?: "")
                    .replace("%horse_name%", horse?.name ?: "")
                    .replace("%amount%", selectedBet.toLong().toString())
                    .replace("%win_pot%", potWin.toLong().toString()))
            }

            val betBtn = config.getItemBuilder("bet-action-btn")
                .name(plugin.format(betBtnStr))
                .lore(betBtnLore)
                .flags(*ItemFlag.values())
                .asGuiItem {
                    player.closeInventory()
                    mgr.placeBet(player, track, horseSelected, selectedBet)
                }
            gui.setItem(config.getInt("bet-action-btn.slot", 41), betBtn)
        } else {
            val noSelItem = config.getItemBuilder("bet-no-selection")
                .name(plugin.format(config.getString("bet-no-selection.name", "")))
                .lore(config.getStringList("bet-no-selection.lore").map { plugin.format(it) })
                .flags(*ItemFlag.values()).asGuiItem()
            gui.setItem(config.getInt("bet-no-selection.slot", 41), noSelItem)
        }


        val cancelBtn = config.getItemBuilder("cancel-btn")
            .name(plugin.format(config.getString("cancel-btn.name", "")))
            .lore(config.getStringList("cancel-btn.lore").map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem { player.closeInventory() }
        gui.setItem(config.getInt("cancel-btn.slot", 44), cancelBtn)

        gui.update()
    }

    private fun winChancePct(horse: Horse, all: List<Horse>): Int {
        val total = all.sumOf { it.winChance }
        return if (total == 0) 0 else (horse.winChance * 100 / total)
    }
}
