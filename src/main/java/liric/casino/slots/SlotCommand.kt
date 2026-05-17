package liric.casino.slots

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class SlotCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) {
            sender.sendMessage(msg("slots.usage"))
            return true
        }

        if (sender.hasPermission("casino.admin")) {
            when (args[0].lowercase()) {
                "setup" -> {
                    val targetBlock = sender.getTargetBlockExact(5)
                    if (targetBlock != null && !targetBlock.type.isAir) {
                        plugin.slotManager.spawnMachine(targetBlock.location)
                        sender.sendMessage(msg("slots.machine-created"))
                    } else {
                        sender.sendMessage(msg("slots.look-block"))
                    }
                }
                "delete" -> {
                    if (plugin.slotManager.deleteNearestMachine(sender.location))
                        sender.sendMessage(msg("slots.machine-deleted"))
                    else
                        sender.sendMessage(msg("slots.machine-not-found"))
                }
                "purge" -> {
                    val removed = plugin.slotManager.purgeAllData(sender.world)
                    sender.sendMessage(msg("slots.purged", "count" to removed.toString()))
                }
                else -> sender.sendMessage(msg("slots.usage"))
            }
        } else {
            sender.sendMessage(msg("general.no-permission"))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1 && sender.hasPermission("casino.admin")) {
            return listOf("setup", "delete", "purge").filter { it.startsWith(args[0], true) }
        }
        return emptyList()
    }
}
