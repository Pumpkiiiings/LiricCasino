package liric.casino.games.lottery

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import java.text.NumberFormat
import java.util.Locale

class LotteryGUI(
    plugin: CasinoPlugin, 
    private val player: Player
) : BaseMenu(plugin, "lottery.yml") {

    fun open() {
        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        gui.open(player)
    }

    override fun setupItems(gui: Gui) {
        val manager = plugin.lotteryManager

        val jackpotFmt = "$" + NumberFormat.getNumberInstance(Locale.US).format(manager.getJackpot())
        val ticketsCount = manager.getTicketCount().toString()
        val timeFmt = formatTime(manager.getSecondsUntilDraw())

        val myTicketsList = manager.getPlayerTickets(player.uniqueId)
        val ticketPrice = plugin.config.getDouble("lottery.ticket-price", 5000.0)
        val maxTickets = plugin.config.getInt("lottery.max-tickets-per-player", 5)
        val remaining = (maxTickets - myTicketsList.size).coerceAtLeast(0)
        val myTicketsStr = if (myTicketsList.isEmpty()) {
            config.getString("no-tickets-format", "<gray>You don't have any tickets.")
        } else {
            myTicketsList.joinToString(", ") { it.number.toString() }
        }

        val infoMat = config.getMaterial("info.material", Material.GOLD_INGOT)
        val infoItem = ItemBuilder.from(infoMat)
            .name(config.getComponent("info.name"))
            .lore(config.getStringList("info.lore").map { line ->
                plugin.format(line
                    .replace("%jackpot%", jackpotFmt)
                    .replace("%tickets%", ticketsCount)
                    .replace("%time%", timeFmt)
                    .replace("%my_tickets%", myTicketsStr))
            })
            .flags(*ItemFlag.values())
            .asGuiItem()
        gui.setItem(config.getInt("info.slot", 13), infoItem)

        val buy1Mat = config.getMaterial("buy-1.material", Material.PAPER)
        val buy1Btn = ItemBuilder.from(buy1Mat)
            .name(config.getComponent("buy-1.name"))
            .lore(config.getStringList("buy-1.lore").map { plugin.format(it.replace("%price%", money(ticketPrice)).replace("%remaining%", remaining.toString())) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                player.closeInventory()
                if (remaining > 0) manager.buyTicket(player, 1)
            }
        gui.setItem(config.getInt("buy-1.slot", 29), buy1Btn)

        val bundleAmount = remaining.coerceAtMost(5)
        val buy5Mat = config.getMaterial("buy-5.material", Material.MAP)
        val buy5Btn = ItemBuilder.from(buy5Mat)
            .name(plugin.format(config.getString("buy-5.name").replace("%amount%", bundleAmount.toString())))
            .lore(config.getStringList("buy-5.lore").map { plugin.format(it.replace("%amount%", bundleAmount.toString()).replace("%price%", money(ticketPrice * bundleAmount)).replace("%remaining%", remaining.toString())) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                player.closeInventory()
                if (bundleAmount > 0) manager.buyTicket(player, bundleAmount)
            }
        gui.setItem(config.getInt("buy-5.slot", 33), buy5Btn)
    }

    private fun money(amount: Double): String = "$" + NumberFormat.getNumberInstance(Locale.US).format(amount)

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else  -> "${s}s"
        }
    }
}
