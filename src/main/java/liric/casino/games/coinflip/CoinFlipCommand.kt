package liric.casino.games.coinflip

import liric.casino.CasinoPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CoinFlipCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return true }
        val mgr = plugin.coinFlipManager

        when (args.getOrNull(0)?.lowercase()) {
            // Sin args o "menu" → abrir GUI principal
            null, "menu", "lista", "list" -> CoinFlipGUI.open(plugin, sender)

            // Crear con monto directo
            "crear", "create" -> {
                val raw = args.getOrNull(1) ?: run { sendHelp(sender); return true }
                val amount = CoinFlipMenu.parseAmount(raw)
                    ?: run { sender.sendMessage(msg("coinflip.invalid-number")); return true }
                if (amount <= 0 || amount.isNaN()) { sender.sendMessage(msg("coinflip.invalid-number")); return true }
                mgr.createGame(sender, amount)
            }

            // Unirse a la partida de alguien
            "unirse", "join" -> {
                val target = args.getOrNull(1) ?: run { sendHelp(sender); return true }
                mgr.joinGame(sender, target)
            }

            // Cancelar la partida propia
            "cancelar", "cancel" -> mgr.cancelGame(sender)

            // Ayuda
            "help", "ayuda" -> sendHelp(sender)

            // Admin: reload (delegado a CasinoCommand reload)
            "reload" -> {
                if (!sender.hasPermission("casino.admin")) {
                    sender.sendMessage(msg("general.no-permission")); return true
                }
                plugin.reloadConfig()
                plugin.messages.load()
                sender.sendMessage(plugin.format("<#00FF7F>CoinFlip — configuración recargada."))
            }

            else -> sendHelp(sender)
        }
        return true
    }

    private fun sendHelp(player: Player) {
        val isAdmin = player.hasPermission("casino.admin")
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFD700>🪙 <gold><bold>COINFLIP — Comandos</bold>"))
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFB400>/cf menu <dark_gray>— <gray>Ver juegos activos"))
        player.sendMessage(plugin.format(" <#FFB400>/cf create <monto> <dark_gray>— <gray>Crear un juego"))
        player.sendMessage(plugin.format(" <#FFB400>/cf join <jugador> <dark_gray>— <gray>Unirse a un juego"))
        player.sendMessage(plugin.format(" <#FFB400>/cf cancel <dark_gray>— <gray>Cancelar tu juego"))
        if (isAdmin) {
            player.sendMessage(plugin.format("<dark_gray>─────────────────────────────────────────────"))
            player.sendMessage(plugin.format(" <red>/cf reload <dark_gray>— <gray>Recargar configuración"))
        }
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> {
                val opts = mutableListOf("menu", "crear", "unirse", "cancelar", "help")
                if (sender.hasPermission("casino.admin")) opts += "reload"
                opts.filter { it.startsWith(args[0], true) }
            }
            2 -> when (args[0].lowercase()) {
                "unirse", "join" ->
                    plugin.coinFlipManager.getOpenGames().map { it.creatorName }
                        .filter { it.startsWith(args[1], true) }
                "crear", "create" ->
                    listOf("100", "1k", "5k", "10k", "50k", "100k", "1m", "5m")
                        .filter { it.startsWith(args[1]) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
