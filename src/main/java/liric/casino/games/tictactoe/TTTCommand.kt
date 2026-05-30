package liric.casino.games.tictactoe

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class TTTCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return true }
        if (!plugin.isGameEnabled("ttt")) { sender.sendMessage(msg("general.game-disabled")); return true }

        when (args.getOrNull(0)?.lowercase()) {
            null, "ayuda", "help" -> sendHelp(sender)
            "crear", "create" -> {
                val raw = args.getOrNull(1)
                val amount = raw?.replace("k", "000")?.replace("m", "000000")?.toDoubleOrNull()
                    ?: run { sender.sendMessage(msg("ttt.usage")); return true }
                if (amount <= 0 || amount.isNaN()) { sender.sendMessage(msg("ttt.usage")); return true }
                plugin.tttManager.createGame(sender, amount)
            }
            "unirse", "join" -> {
                val target = args.getOrNull(1) ?: run { sender.sendMessage(msg("ttt.usage")); return true }
                plugin.tttManager.joinGame(sender, target)
            }
            "cancelar", "cancel" -> plugin.tttManager.cancelGame(sender)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun sendHelp(player: Player) {
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#00FFFF>⬛ <gold><bold>TIC TAC TOE — Ayuda</bold>"))
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFB400>/ttt crear <monto> <dark_gray>— <gray>Crear una partida"))
        player.sendMessage(plugin.format(" <#FFB400>/ttt unirse <jugador> <dark_gray>— <gray>Unirse a una partida"))
        player.sendMessage(plugin.format(" <#FFB400>/ttt cancelar <dark_gray>— <gray>Cancelar tu partida abierta"))
        val open = plugin.tttManager.getOpenGames()
        if (open.isNotEmpty()) {
            player.sendMessage(plugin.format("<dark_gray>─────────────────────────────────────────────"))
            player.sendMessage(plugin.format(" <gray>Partidas abiertas:"))
            open.take(5).forEach { s ->
                player.sendMessage(plugin.format("  <white>${s.creatorName} <dark_gray>• <#FFD700>$${s.betAmount.toLong()}"))
            }
        }
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("crear", "unirse", "cancelar", "ayuda").filter { it.startsWith(args[0], true) }
            2 -> when (args[0].lowercase()) {
                "crear", "create" -> listOf("100", "500", "1000", "5k", "10k").filter { it.startsWith(args[1]) }
                "unirse", "join"  -> plugin.tttManager.getOpenGames().map { it.creatorName }.filter { it.startsWith(args[1], true) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
