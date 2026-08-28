package liric.casino.games.poker

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class PokerCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg("poker.only-players"))
            return true
        }
        val sub = args.getOrNull(0)?.lowercase()
        if (sub !in listOf("setup", "delete") && !plugin.isGameEnabled("poker")) {

            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(msg("poker.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "join"         -> plugin.pokerGame.addPlayer(sender)
            "leave", "salir" -> plugin.pokerGame.removePlayer(sender)
            "setup" -> {
                if (sender.hasPermission("casino.admin")) {
                    plugin.pokerManager.spawnTable(sender.location)
                    sender.sendMessage(msg("poker.table-created"))
                } else sender.sendMessage(msg("poker.no-permission"))
            }
            "delete" -> {
                if (sender.hasPermission("casino.admin")) {
                    if (plugin.pokerManager.deleteNearest(sender.location))
                        sender.sendMessage(msg("poker.table-deleted"))
                    else
                        sender.sendMessage(msg("poker.table-not-found"))
                } else sender.sendMessage(msg("poker.no-permission"))
            }
            else -> sender.sendMessage(msg("poker.usage"))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val options = mutableListOf("join", "leave")
            if (sender.hasPermission("casino.admin")) { options.add("setup"); options.add("delete") }
            return options.filter { it.startsWith(args[0], true) }
        }
        return emptyList()
    }
}
