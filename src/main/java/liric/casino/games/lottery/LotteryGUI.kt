package liric.casino.games.lottery

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import java.text.NumberFormat
import java.util.Locale

object LotteryGUI {

    fun open(plugin: CasinoPlugin, viewer: Player) {
        val cfg = plugin.menuConfig("lottery.yml")
        val manager = plugin.lotteryManager

        val gui = Gui.gui()
            .title(cfg.getComponent("title", "<dark_gray><bold>Lotería</bold></dark_gray> <dark_gray>▸</dark_gray> <white>Casino"))
            .rows(cfg.getInt("rows", 5))
            .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
            .create()

        cfg.applyDecorations(gui)


        val jackpotFmt = "$" + NumberFormat.getNumberInstance(Locale.US).format(manager.getJackpot())
        val ticketsCount = manager.getTicketCount().toString()
        val timeFmt = formatTime(manager.getSecondsUntilDraw())

        val myTicketsList = manager.getPlayerTickets(viewer.uniqueId)
        val myTicketsStr = if (myTicketsList.isEmpty()) {
            cfg.getString("no-tickets-format", "<gray>No tienes boletos actualmente.")
        } else {
            myTicketsList.joinToString(", ") { it.number.toString() }
        }


        val infoMat = cfg.getMaterial("info.material", Material.GOLD_INGOT)
        val infoItem = ItemBuilder.from(infoMat)
            .name(cfg.getComponent("info.name"))
            .lore(cfg.getStringList("info.lore").map { line ->
                plugin.format(line
                    .replace("%jackpot%", jackpotFmt)
                    .replace("%tickets%", ticketsCount)
                    .replace("%time%", timeFmt)
                    .replace("%my_tickets%", myTicketsStr))
            })
            .flags(*ItemFlag.values())
            .asGuiItem()
        gui.setItem(cfg.getInt("info.slot", 13), infoItem)


        val buy1Mat = cfg.getMaterial("buy-1.material", Material.PAPER)
        val buy1Btn = ItemBuilder.from(buy1Mat)
            .name(cfg.getComponent("buy-1.name"))
            .lore(cfg.getComponentList("buy-1.lore"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                viewer.playSound(viewer.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                viewer.closeInventory()
                manager.buyTicket(viewer, 1)
            }
        gui.setItem(cfg.getInt("buy-1.slot", 29), buy1Btn)


        val buy5Mat = cfg.getMaterial("buy-5.material", Material.MAP)
        val buy5Btn = ItemBuilder.from(buy5Mat)
            .name(cfg.getComponent("buy-5.name"))
            .lore(cfg.getComponentList("buy-5.lore"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                viewer.playSound(viewer.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                viewer.closeInventory()
                manager.buyTicket(viewer, 5)
            }
        gui.setItem(cfg.getInt("buy-5.slot", 33), buy5Btn)

        gui.open(viewer)
    }

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
