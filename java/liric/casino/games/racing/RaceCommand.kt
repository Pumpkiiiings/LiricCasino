package liric.casino.games.racing

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class RaceCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return true }
        if (!plugin.isGameEnabled("racing")) { return true }

        when (args.getOrNull(0)?.lowercase()) {
            null, "menu", "jugar" -> {
                val track = plugin.raceManager.getNearestTrack(sender.location)
                    ?: plugin.raceManager.createTrack(sender.location)
                RaceBetGUI(plugin, track, sender).open()
            }
            "ayuda", "help" -> sendHelp(sender)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun sendHelp(player: Player) {
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFD700>🏇 <gold><bold>CARRERAS — Ayuda</bold>"))
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFB400>/carreras <dark_gray>— <gray>Abre el menú de apuestas"))
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("jugar", "ayuda").filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
