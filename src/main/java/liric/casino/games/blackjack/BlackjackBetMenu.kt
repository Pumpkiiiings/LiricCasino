package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import kotlin.math.min

class BlackjackBetMenu(
    private val plugin: CasinoPlugin,
    private val player: Player,
    private val isMultiplayer: Boolean
) {
    private val cfg      get() = plugin.menuConfig("blackjack_bet.yml")
    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    private val minLimit = plugin.config.getDouble("blackjack.bet.min", 100.0)
    private val maxLimit = plugin.config.getDouble("blackjack.bet.max", 100000.0)
    private var currentBet = minLimit

    private val titleKey = if (isMultiplayer) "title-multi" else "title-solo"

    private val gui = Gui.gui()
        .title(cfg.getComponent(titleKey))
        .rows(cfg.getInt("rows", 3))
        .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        .create()

    fun open() {
        cfg.applyDecorations(gui)
        updateMenu()
        gui.open(player)
    }

    private fun updateMenu() {
        // Botones de ajuste
        listOf("minus-big", "minus-medium", "minus-small", "plus-small", "plus-medium", "plus-big").forEach { key ->
            val slot   = cfg.getInt("buttons.$key.slot")
            val mat    = cfg.getMaterial("buttons.$key.material", Material.STONE)
            val label  = cfg.getString("buttons.$key.label")
            val amount = cfg.getDouble("buttons.$key.amount")
            val color  = cfg.getString("buttons.$key.color")
            gui.setItem(slot, createModifyButton(mat, label, amount, color))
        }

        // CONFIRMAR
        val confirmSlot = cfg.getInt("buttons.confirm.slot", 13)
        val confirmMat  = cfg.getMaterial("buttons.confirm.material", Material.EMERALD_BLOCK)
        val confirmName = cfg.getComponent("buttons.confirm.name")
        val confirmLore = plugin.format(cfg.getString("buttons.confirm.lore-bet").replace("\${amount}", currentBet.toString()))

        val confirmBtn = ItemBuilder.from(confirmMat)
            .name(confirmName).lore(confirmLore).flags(*ItemFlag.values())
            .asGuiItem {
                if (plugin.economyManager.withdrawPlayer(player, currentBet).transactionSuccess()) {
                    if (isMultiplayer) {
                        gui.close(player)
                        plugin.blackjackMultiGame.addPlayer(player, currentBet)
                    } else {
                        BlackjackSession(plugin, player, currentBet).start()
                    }
                } else {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    player.sendMessage(msg("blackjack.no-funds"))
                }
            }
        gui.setItem(confirmSlot, confirmBtn)

        // MAX BET
        val maxBalance = plugin.economyManager.vault?.getBalance(player) ?: 0.0
        val maxAllowed = min(maxBalance, maxLimit)
        val maxSlot    = cfg.getInt("buttons.max-bet.slot", 22)
        val maxMat     = cfg.getMaterial("buttons.max-bet.material", Material.GOLD_BLOCK)
        val maxName    = plugin.format(cfg.getString("buttons.max-bet.name").replace("{max}", maxAllowed.toString()))

        val allInBtn = ItemBuilder.from(maxMat).name(maxName).flags(*ItemFlag.values())
            .asGuiItem {
                if (maxAllowed >= minLimit) {
                    currentBet = maxAllowed
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                    updateMenu()
                }
            }
        gui.setItem(maxSlot, allInBtn)

        gui.update()
    }

    private fun createModifyButton(material: Material, label: String, amount: Double, color: String): GuiItem {
        return ItemBuilder.from(material)
            .name(plugin.format("$color<bold>$label</bold>")).flags(*ItemFlag.values())
            .asGuiItem {
                val maxBalance = plugin.economyManager.vault?.getBalance(player) ?: 0.0
                var newBet = currentBet + amount
                if (newBet < minLimit) newBet = minLimit
                if (newBet > maxLimit) newBet = maxLimit
                if (newBet > maxBalance) {
                    if (maxBalance >= minLimit) newBet = maxBalance else return@asGuiItem
                }
                if (newBet != currentBet) {
                    currentBet = newBet
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    updateMenu()
                } else {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                }
            }
    }
}
