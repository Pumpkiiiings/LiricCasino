package liric.casino.games.lottery

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.NumberFormat
import java.util.Locale

class LotteryCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)
    private fun Double.fmt() = "$" + NumberFormat.getNumberInstance(Locale.US).format(this)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val manager = plugin.lotteryManager
        val sub0 = args.getOrNull(0)?.lowercase()
        val isAdminSub = sub0 in listOf("forcestart", "give")
        if (!isAdminSub && !plugin.isGameEnabled("lottery")) {

            return true
        }

        when (sub0) {
            null, "info" -> {
                if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return true }
                LotteryGUI.open(plugin, sender)
            }

            "comprar", "buy" -> {
                if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return true }
                val amount = args.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                manager.buyTicket(sender, amount)
            }

            "forcestart" -> {
                if (!sender.hasPermission("casino.admin")) { sender.sendMessage(msg("general.no-permission")); return true }
                sender.sendMessage(plugin.format("<#FFB400>Forzando sorteo de lotería..."))
                manager.forceStart()
            }

            "give" -> {
                if (!sender.hasPermission("casino.admin")) { sender.sendMessage(msg("general.no-permission")); return true }
                val targetName = args.getOrNull(1) ?: run { sender.sendMessage(msg("lottery.usage")); return true }
                val amount = args.getOrNull(2)?.toIntOrNull() ?: 1
                val target = plugin.server.getPlayerExact(targetName)
                    ?: run { sender.sendMessage(msg("scratch.player-offline")); return true }
                manager.giveTicket(target, amount)
                sender.sendMessage(msg("lottery.give-sender", "amount" to amount.toString(), "player" to target.name))
            }

            else -> sender.sendMessage(msg("lottery.usage"))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> {
                val options = mutableListOf("info", "comprar")
                if (sender.hasPermission("casino.admin")) options += listOf("forcestart", "give")
                options.filter { it.startsWith(args[0], true) }
            }
            2 -> if (args[0].equals("give", true) && sender.hasPermission("casino.admin"))
                plugin.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], true) }
            else emptyList()
            else -> emptyList()
        }
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
