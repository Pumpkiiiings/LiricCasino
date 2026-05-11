package liric.casino.roulettemix

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class RouletteMixCommand(
    private val plugin: CasinoPlugin,
    private val menu: RouletteMixMenu,
    private val game: RouletteMixGame
) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty() || args[0].equals("jugar", ignoreCase = true)) {
            if (sender is Player) menu.openBetMenu(sender)
            else sender.sendMessage(msg("general.only-players"))
            return true
        }

        if (sender.hasPermission("casino.admin")) {
            when (args[0].lowercase()) {
                "setup"      -> if (sender is Player) plugin.rouletteMixManager.spawnRoulette(sender.location)
                "delete"     -> if (sender is Player) {
                    if (plugin.rouletteMixManager.deleteNearestRoulette(sender.location))
                        sender.sendMessage(msg("roulette.deleted"))
                    else
                        sender.sendMessage(msg("roulette.not-found"))
                }
                "forcestart" -> game.forceStart(sender)
                "purge"      -> if (sender is Player) {
                    val removed = plugin.rouletteMixManager.purgeAllData(sender.world)
                    sender.sendMessage(msg("roulette.purged", "count" to removed.toString()))
                }
                else         -> sender.sendMessage(msg("general.invalid-action"))
            }
        } else {
            sender.sendMessage(msg("general.no-permission"))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1 && sender.hasPermission("casino.admin")) {
            return listOf("jugar", "setup", "delete", "forcestart", "purge").filter { it.startsWith(args[0], true) }
        }
        return emptyList()
    }
}
