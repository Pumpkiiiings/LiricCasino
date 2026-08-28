package liric.casino.games.roulette

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class RouletteCommand(
    private val plugin: CasinoPlugin,
    private val menu: RouletteMenu,
    private val game: RouletteGame
) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val sub = args.getOrNull(0)?.lowercase()
        val isAdminSub = sub != null && sub in listOf("setup", "delete", "forcestart", "purge", "scale")
        if (!isAdminSub && !plugin.isGameEnabled("roulette")) {

            return true
        }
        if (args.isEmpty() || args[0].equals("jugar", ignoreCase = true)) {
            if (sender is Player) menu.openBetMenu(sender)
            else sender.sendMessage(msg("general.only-players"))
            return true
        }

        if (sender.hasPermission("casino.admin")) {
            when (args[0].lowercase()) {
                "setup"      -> if (sender is Player) {
                    plugin.rouletteManager.spawnRoulette(sender.location)
                    sender.sendMessage(msg("roulette.created"))
                }
                "delete"     -> if (sender is Player) {
                    if (plugin.rouletteManager.deleteNearestRoulette(sender.location))
                        sender.sendMessage(msg("roulette.deleted"))
                    else
                        sender.sendMessage(msg("roulette.not-found"))
                }
                "forcestart" -> game.forceStart(sender)
                "purge"      -> if (sender is Player) {
                    val removed = plugin.rouletteManager.purgeAllData(sender.world)
                    sender.sendMessage(msg("roulette.purged", "count" to removed.toString()))
                }
                "scale"      -> {
                    if (args.size < 2) {
                        sender.sendMessage(plugin.format("<red>Usage: /roulette scale <value>"))
                        return true
                    }
                    val newScale = args[1].toFloatOrNull()
                    if (newScale == null || newScale <= 0) {
                        sender.sendMessage(plugin.format("<red>Invalid scale value."))
                        return true
                    }
                    plugin.config.set("roulette.block-scale", newScale.toDouble())
                    plugin.saveConfig()
                    val radius = plugin.config.getDouble("roulette.radius", 5.5).toFloat()
                    plugin.rouletteManager.rescaleAll(newScale, radius)
                    sender.sendMessage(plugin.format("<green>Roulette scale updated to $newScale. All active roulettes were updated."))
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
            return listOf("jugar", "setup", "delete", "forcestart", "purge", "scale").filter { it.startsWith(args[0], true) }
        }
        return emptyList()
    }
}
