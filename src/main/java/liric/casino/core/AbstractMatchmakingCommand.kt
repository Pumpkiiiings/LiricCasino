package liric.casino.core

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Base command class for games that support matchmaking (create, join, cancel).
 */
abstract class AbstractMatchmakingCommand(
    val plugin: CasinoPlugin,
    val gameId: String,
    val gameName: String
) : CommandExecutor, TabCompleter {

    protected fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(msg("general.only-players"))
            return true
        }
        if (!plugin.isGameEnabled(gameId)) {
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            null, "help" -> sendHelp(sender)
            "create" -> {
                val raw = args.getOrNull(1)
                val amount = raw?.replace("k", "000")?.replace("m", "000000")?.toDoubleOrNull()
                    ?: run { sender.sendMessage(msg("$gameId.usage")); return true }
                if (amount <= 0 || amount.isNaN()) {
                    sender.sendMessage(msg("$gameId.usage"))
                    return true
                }
                onCreate(sender, amount)
            }
            "join" -> {
                val target = args.getOrNull(1) ?: run { sender.sendMessage(msg("$gameId.usage")); return true }
                onJoin(sender, target)
            }
            "cancel" -> onCancel(sender)
            else -> sendHelp(sender)
        }
        return true
    }

    abstract fun onCreate(player: Player, amount: Double)
    abstract fun onJoin(player: Player, targetName: String)
    abstract fun onCancel(player: Player)
    abstract fun getOpenGameCreators(): List<String>
    abstract fun getOpenGameLines(): List<String>

    protected open fun sendHelp(player: Player) {
        player.sendMessage(plugin.messages.get("general.command-separator"))
        player.sendMessage(plugin.format(" <#00FFFF>⬛ <gold><bold>$gameName — Help</bold>"))
        player.sendMessage(plugin.messages.get("general.command-separator"))
        player.sendMessage(plugin.format(" <#FFB400>/$gameId create <amount> <dark_gray>— <gray>Create a game"))
        player.sendMessage(plugin.format(" <#FFB400>/$gameId join <player> <dark_gray>— <gray>Join a game"))
        player.sendMessage(plugin.format(" <#FFB400>/$gameId cancel <dark_gray>— <gray>Cancel your open game"))
        
        val lines = getOpenGameLines()
        if (lines.isNotEmpty()) {
            player.sendMessage(plugin.format("<dark_gray>─────────────────────────────────────────────"))
            player.sendMessage(plugin.format(" <gray>Open games:"))
            lines.take(5).forEach { player.sendMessage(plugin.format(it)) }
        }
        player.sendMessage(plugin.messages.get("general.command-separator"))
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("create", "join", "cancel", "help").filter { it.startsWith(args[0], true) }
            2 -> when (args[0].lowercase()) {
                "create" -> listOf("100", "500", "1000", "5k", "10k").filter { it.startsWith(args[1]) }
                "join" -> getOpenGameCreators().filter { it.startsWith(args[1], true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
