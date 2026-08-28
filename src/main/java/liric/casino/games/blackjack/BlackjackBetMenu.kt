package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import liric.casino.util.ValidationUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import kotlin.math.min

class BlackjackBetMenu(
    plugin: CasinoPlugin,
    private val player: Player,
    private val isMultiplayer: Boolean
) : BaseMenu(plugin, "blackjack_bet.yml") {
    
    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    private val minLimit = plugin.config.getDouble("blackjack.bet.default.min", 100.0)
    private val maxLimit = plugin.economyManager.getMaxBet(player, "blackjack")
    private var currentBet = minLimit

    private val titleKey = if (isMultiplayer) "title-multi" else "title-solo"

    fun open() {
        val gui = Gui.gui()
            .title(config.getComponent(titleKey))
            .rows(config.getInt("rows", 3))
            .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
            .create()

        config.applyDecorations(gui)
        setupItems(gui)
        gui.open(player)
    }

    override fun setupItems(gui: Gui) {
        listOf("minus-big", "minus-medium", "minus-small", "plus-small", "plus-medium", "plus-big").forEach { key ->
            val slot   = config.getInt("buttons.$key.slot")
            val mat    = config.getMaterial("buttons.$key.material", Material.STONE)
            val label  = config.getString("buttons.$key.label")
            val amount = config.getDouble("buttons.$key.amount")
            val color  = config.getString("buttons.$key.color")
            gui.setItem(slot, createModifyButton(gui, mat, label, amount, color))
        }

        val customSlot = config.getInt("buttons.custom-bet.slot", 4)
        val customMat  = config.getMaterial("buttons.custom-bet.material", Material.PAPER)
        val customName = config.getComponent("buttons.custom-bet.name", "<#FFD700>Custom Amount")

        val customBtn = ItemBuilder.from(customMat)
            .name(customName)
            .lore(plugin.format("<gray>Click to type the amount in chat"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                gui.close(player)
                player.sendMessage(plugin.format("<#FFD700><b>✍ Type the exact amount you want to bet:</b>"))

                plugin.economyManager.openCustomBetChat(player) { amount ->
                    if (amount in minLimit..maxLimit) {
                        currentBet = amount
                        open()
                    } else {
                        player.sendMessage(plugin.format("<red>Invalid amount. Must be between $minLimit and $maxLimit"))
                    }
                }
            }
        gui.setItem(customSlot, customBtn)

        val confirmSlot = config.getInt("buttons.confirm.slot", 13)
        val confirmMat  = config.getMaterial("buttons.confirm.material", Material.EMERALD_BLOCK)
        val confirmName = config.getComponent("buttons.confirm.name")
        val confirmLore = plugin.format(config.getString("buttons.confirm.lore-bet").replace("\${amount}", currentBet.toString()))

        val confirmBtn = ItemBuilder.from(confirmMat)
            .name(confirmName).lore(confirmLore).flags(*ItemFlag.values())
            .asGuiItem {
                if (!ValidationUtil.validateBet(plugin, player, "blackjack", currentBet)) {
                    gui.close(player)
                    return@asGuiItem
                }
                if (plugin.economyManager.withdrawPlayer(player, currentBet)?.transactionSuccess() == true) {
                    plugin.statsManager.recordGameUse(player.uniqueId, "blackjack")
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

        val maxBalance = plugin.economyManager.vault?.getBalance(player) ?: 0.0
        val maxAllowed = min(maxBalance, maxLimit)
        val maxSlot    = config.getInt("buttons.max-bet.slot", 22)
        val maxMat     = config.getMaterial("buttons.max-bet.material", Material.GOLD_BLOCK)
        val maxName    = plugin.format(config.getString("buttons.max-bet.name").replace("{max}", maxAllowed.toString()))

        val allInBtn = ItemBuilder.from(maxMat).name(maxName).flags(*ItemFlag.values())
            .asGuiItem {
                if (maxAllowed >= minLimit) {
                    currentBet = maxAllowed
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                    setupItems(gui)
                }
            }
        gui.setItem(maxSlot, allInBtn)

        gui.update()
    }

    private fun createModifyButton(gui: Gui, material: Material, label: String, amount: Double, color: String): GuiItem {
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
                    setupItems(gui)
                } else {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                }
            }
    }
}
