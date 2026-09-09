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
            null, "menu", "play" -> {
                val track = plugin.raceManager.getNearestTrack(sender.location)
                if (track == null) {
                    sender.sendMessage(msg("racing.no-track"))
                    return true
                }
                RaceBetGUI(plugin, track, sender).open()
            }
            "setup" -> if (sender.hasPermission("casino.admin")) {
                plugin.raceManager.createTrack(sender.location)
                sender.sendMessage(msg("racing.track-created"))
            } else sender.sendMessage(msg("general.no-permission"))
            "delete" -> if (sender.hasPermission("casino.admin")) {
                val track = plugin.raceManager.getNearestTrack(sender.location)
                if (track != null) {
                    plugin.raceManager.deleteTrack(track)
                    sender.sendMessage(msg("racing.track-deleted"))
                } else sender.sendMessage(msg("racing.no-track"))
            } else sender.sendMessage(msg("general.no-permission"))
            "help" -> sendHelp(sender)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun sendHelp(player: Player) {
        player.sendMessage(plugin.messages.get("general.command-separator"))
        player.sendMessage(plugin.format(" <#FFD700>🏇 <gold><bold>RACING — Help</bold>"))
        player.sendMessage(plugin.messages.get("general.command-separator"))
        player.sendMessage(plugin.format(" <#FFB400>/racing <dark_gray>— <gray>Open betting menu"))
        if (player.hasPermission("casino.admin")) {
            player.sendMessage(plugin.format(" <red>/racing setup <dark_gray>— <gray>Create and save a track"))
            player.sendMessage(plugin.format(" <red>/racing delete <dark_gray>— <gray>Delete the nearest track"))
        }
        player.sendMessage(plugin.messages.get("general.command-separator"))
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> buildList {
                addAll(listOf("play", "help"))
                if (sender.hasPermission("casino.admin")) addAll(listOf("setup", "delete"))
            }.filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
