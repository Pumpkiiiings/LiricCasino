package liric.casino.blackjack

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class BlackjackCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty() || args[0].equals("play", true)) {
            BlackjackChoiceMenu(plugin, sender).open()
            return true
        }

        when (args[0].lowercase()) {
            "leave"  -> plugin.blackjackMultiGame.removePlayer(sender)
            "setup"  -> {
                if (sender.hasPermission("casino.admin")) {
                    plugin.blackjackManager.spawnTable(sender.location)
                    sender.sendMessage(msg("blackjack.table-created"))
                } else sender.sendMessage(msg("general.no-permission"))
            }
            "delete" -> {
                if (sender.hasPermission("casino.admin")) {
                    if (plugin.blackjackManager.deleteNearest(sender.location))
                        sender.sendMessage(msg("blackjack.table-deleted"))
                    else
                        sender.sendMessage(msg("blackjack.table-not-found"))
                } else sender.sendMessage(msg("general.no-permission"))
            }
            else -> sender.sendMessage(msg("blackjack.usage"))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val options = mutableListOf("play", "leave")
            if (sender.hasPermission("casino.admin")) { options.add("setup"); options.add("delete") }
            return options.filter { it.startsWith(args[0], true) }
        }
        return emptyList()
    }
}
