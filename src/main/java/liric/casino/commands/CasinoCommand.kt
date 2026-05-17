package liric.casino.commands

import liric.casino.CasinoPlugin
import liric.casino.blackjack.BlackjackChoiceMenu
import liric.casino.coinflip.CoinFlipGUI
import liric.casino.lottery.LotteryGUI
import liric.casino.slots.SlotMachineMenu
import liric.casino.stats.StatsManager.TopMode
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.NumberFormat
import java.util.Locale

/**
 * /casino — Comando hub central del Casino.
 *
 * Subcategorías:
 *   /casino ruleta [jugar|setup|delete|forcestart|purge]
 *   /casino blackjack [jugar|setup|delete]
 *   /casino tragamonedas [jugar|setup|delete|purge]
 *   /casino poker [jugar|setup|delete]
 *   /casino boleto [comprar|get|give ...]
 *   /casino loteria [info|comprar|forcestart|give ...]
 *   /casino coinflip [menu|crear|unirse|cancelar]
 *   /casino stats [modo]
 *   /casino top [modo]
 *   /casino reload
 */
class CasinoCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)
    private fun Double.fmt() = "$" + NumberFormat.getNumberInstance(Locale.US).format(this)
    private fun isAdmin(sender: CommandSender) = sender.hasPermission("casino.admin")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendMainHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            // ── Juegos ───────────────────────────────────────────────────────
            "ruleta", "roulette"       -> handleRuleta(sender, args.drop(1))
            "blackjack", "bj", "21"   -> handleBlackjack(sender, args.drop(1))
            "tragamonedas", "slots","777" -> handleSlots(sender, args.drop(1))
            "poker"                    -> handlePoker(sender, args.drop(1))
            "boleto", "scratch"        -> handleBoleto(sender, args.drop(1))
            "loteria", "lottery"       -> handleLoteria(sender, args.drop(1))
            "coinflip", "cf", "moneda" -> handleCoinFlip(sender, args.drop(1))
            "rps"                      -> handleRPS(sender, args.drop(1))
            "ttt", "tictactoe"         -> handleTTT(sender, args.drop(1))
            "carreras", "racing"       -> handleCarreras(sender, args.drop(1))
            // ── Stats / Admin ─────────────────────────────────────────────
            "stats"                    -> handleStats(sender, args.drop(1))
            "top"                      -> handleTop(sender, args.drop(1))
            "reload"                   -> handleReload(sender)
            else                       -> sendMainHelp(sender)
        }
        return true
    }

    // ── Ruleta ────────────────────────────────────────────────────────────────
    private fun handleRuleta(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        val sub = args.getOrNull(0)?.lowercase()
        if (sub == null || sub == "jugar") {
            plugin.rouletteMenu.openBetMenu(sender); return
        }
        when (sub) {
            "ayuda", "help" -> sendRuletaHelp(sender)
            "setup", "delete", "forcestart", "purge", "escala" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                when (sub) {
                    "setup" -> {
                        plugin.rouletteManager.spawnRoulette(sender.location)
                        sender.sendMessage(msg("roulette.created"))
                    }
                    "delete" -> {
                        if (plugin.rouletteManager.deleteNearestRoulette(sender.location))
                            sender.sendMessage(msg("roulette.deleted"))
                        else sender.sendMessage(msg("roulette.not-found"))
                    }
                    "forcestart" -> plugin.rouletteGame.forceStart(sender)
                    "purge" -> {
                        val n = plugin.rouletteManager.purgeAllData(sender.world)
                        sender.sendMessage(msg("roulette.purged", "count" to n.toString()))
                    }
                    "escala" -> {
                        val scaleArg  = args.getOrNull(1)?.toFloatOrNull()
                        val radiusArg = args.getOrNull(2)?.toFloatOrNull()
                        if (scaleArg == null || scaleArg !in 0.1f..2.0f) {
                            sender.sendMessage(msg("roulette.escala-usage"))
                            return
                        }
                        val radius = radiusArg?.coerceIn(2f, 15f)
                            ?: plugin.config.getDouble("roulette.radius", 5.5).toFloat()
                        // Guardar en config para persistencia
                        plugin.config.set("roulette.block-scale", scaleArg)
                        plugin.config.set("roulette.radius", radius)
                        plugin.saveConfig()
                        plugin.rouletteManager.rescaleAll(scaleArg, radius)
                        sender.sendMessage(msg("roulette.escala-set",
                            "scale"  to scaleArg.toString(),
                            "radius" to radius.toString()
                        ))
                    }
                }
            }
            else -> sender.sendMessage(msg("general.invalid-action"))
        }
    }

    private fun sendRuletaHelp(player: Player) {
        val admin = isAdmin(player)
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FF00FF>🌀 <gold><bold>RULETA — Ayuda</bold>"))
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        player.sendMessage(plugin.format(" <#FFB400>/casino ruleta <dark_gray>— <gray>Abrir menú de apuesta"))
        player.sendMessage(plugin.format(" <#FFB400>/casino ruleta jugar <dark_gray>— <gray>Abrir menú de apuesta"))
        if (admin) {
            player.sendMessage(plugin.format("<dark_gray>─────────────────────────────────────────────"))
            player.sendMessage(plugin.format(" <red>/casino ruleta setup <dark_gray>— <gray>Crear ruleta donde estás"))
            player.sendMessage(plugin.format(" <red>/casino ruleta delete <dark_gray>— <gray>Eliminar ruleta cercana"))
            player.sendMessage(plugin.format(" <red>/casino ruleta forcestart <dark_gray>— <gray>Forzar giro ahora"))
            player.sendMessage(plugin.format(" <red>/casino ruleta purge <dark_gray>— <gray>Borrar todas las ruletas del mundo"))
            player.sendMessage(plugin.format(" <red>/casino ruleta escala <escala> [radio] <dark_gray>— <gray>Ajustar tamaño (0.1–2.0)"))
        }
        player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
    }

    // ── Blackjack ─────────────────────────────────────────────────────────────
    private fun handleBlackjack(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        when (args.getOrNull(0)?.lowercase()) {
            null, "jugar", "play" -> BlackjackChoiceMenu(plugin, sender).open()
            "leave", "salir"      -> plugin.blackjackMultiGame.removePlayer(sender)
            "setup" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                plugin.blackjackManager.spawnTable(sender.location)
                sender.sendMessage(msg("blackjack.table-created"))
            }
            "delete" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                if (plugin.blackjackManager.deleteNearest(sender.location))
                    sender.sendMessage(msg("blackjack.table-deleted"))
                else sender.sendMessage(msg("blackjack.table-not-found"))
            }
            else -> sender.sendMessage(msg("blackjack.usage"))
        }
    }

    // ── Tragamonedas ──────────────────────────────────────────────────────────
    private fun handleSlots(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        when (args.getOrNull(0)?.lowercase()) {
            null, "jugar", "play" -> {
                // Abrir el menú de apuesta si está parado en una máquina
                sender.sendMessage(msg("slots.usage"))
            }
            "setup" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                val block = sender.getTargetBlockExact(5)
                if (block != null && !block.type.isAir) {
                    plugin.slotManager.spawnMachine(block.location)
                    sender.sendMessage(msg("slots.machine-created"))
                } else sender.sendMessage(msg("slots.look-block"))
            }
            "delete" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                if (plugin.slotManager.deleteNearestMachine(sender.location))
                    sender.sendMessage(msg("slots.machine-deleted"))
                else sender.sendMessage(msg("slots.machine-not-found"))
            }
            "purge" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                val n = plugin.slotManager.purgeAllData(sender.world)
                sender.sendMessage(msg("slots.purged", "count" to n.toString()))
            }
            else -> sender.sendMessage(msg("slots.usage"))
        }
    }

    // ── Poker ─────────────────────────────────────────────────────────────────
    private fun handlePoker(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        when (args.getOrNull(0)?.lowercase()) {
            null, "jugar", "join" -> plugin.pokerGame.addPlayer(sender)
            "leave", "salir"      -> plugin.pokerGame.removePlayer(sender)
            "setup" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                plugin.pokerManager.spawnTable(sender.location)
                sender.sendMessage(msg("poker.table-created"))
            }
            "delete" -> {
                if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
                if (plugin.pokerManager.deleteNearest(sender.location))
                    sender.sendMessage(msg("poker.table-deleted"))
                else sender.sendMessage(msg("poker.table-not-found"))
            }
            else -> sender.sendMessage(msg("poker.usage"))
        }
    }

    // ── Boleto (Scratch) ──────────────────────────────────────────────────────
    private fun handleBoleto(sender: CommandSender, args: List<String>) {
        // Redirige al ScratchCommand existente pasando los args
        val cmd = plugin.server.getPluginCommand("boleto") ?: return
        cmd.execute(sender, "boleto", args.toTypedArray())
    }

    // ── Lotería ───────────────────────────────────────────────────────────────
    private fun handleLoteria(sender: CommandSender, args: List<String>) {
        val cmd = plugin.server.getPluginCommand("loteria") ?: return
        cmd.execute(sender, "loteria", args.toTypedArray())
    }

    // ── CoinFlip ──────────────────────────────────────────────────────────────
    private fun handleCoinFlip(sender: CommandSender, args: List<String>) {
        val cmd = plugin.server.getPluginCommand("coinflip") ?: return
        cmd.execute(sender, "coinflip", args.toTypedArray())
    }

    // ── RPS ───────────────────────────────────────────────────────────────────
    private fun handleRPS(sender: CommandSender, args: List<String>) {
        val cmd = plugin.server.getPluginCommand("rps") ?: return
        cmd.execute(sender, "rps", args.toTypedArray())
    }

    // ── TTT ───────────────────────────────────────────────────────────────────
    private fun handleTTT(sender: CommandSender, args: List<String>) {
        val cmd = plugin.server.getPluginCommand("ttt") ?: return
        cmd.execute(sender, "ttt", args.toTypedArray())
    }

    // ── Carreras ──────────────────────────────────────────────────────────────
    private fun handleCarreras(sender: CommandSender, args: List<String>) {
        val cmd = plugin.server.getPluginCommand("carreras") ?: return
        cmd.execute(sender, "carreras", args.toTypedArray())
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private fun handleStats(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        showStats(sender, args.getOrNull(0)?.lowercase())
    }

    private fun handleTop(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        showTop(sender, resolveMode(args.getOrNull(0)))
    }

    // ── Reload ────────────────────────────────────────────────────────────────
    private fun handleReload(sender: CommandSender) {
        if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
        sender.sendMessage(plugin.format("<#FFB400>Recargando configuración..."))
        plugin.reloadConfig()
        plugin.messages.load()
        sender.sendMessage(plugin.format("<#00FF7F>Configuración recargada."))
    }

    // ── Ayuda principal ───────────────────────────────────────────────────────
    private fun sendMainHelp(sender: CommandSender) {
        val admin = isAdmin(sender)
        sender.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        sender.sendMessage(plugin.format(" <#FFD700>🎰 <gold><bold>CASINO</bold></gold>  <gray>—  Comandos disponibles"))
        sender.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        sender.sendMessage(plugin.format(" <#FF00FF>/casino ruleta     <dark_gray>— <gray>Ruleta giratoria"))
        sender.sendMessage(plugin.format(" <#FFB400>/casino tragamonedas <dark_gray>— <gray>Máquina 777"))
        sender.sendMessage(plugin.format(" <#00FFFF>/casino blackjack  <dark_gray>— <gray>21 Blackjack"))
        sender.sendMessage(plugin.format(" <white>/casino poker      <dark_gray>— <gray>Mesa de Poker"))
        sender.sendMessage(plugin.format(" <#FF88FF>/casino boleto     <dark_gray>— <gray>Rasca y Gana"))
        sender.sendMessage(plugin.format(" <#FFD700>/casino loteria    <dark_gray>— <gray>Lotería"))
        sender.sendMessage(plugin.format(" <#FFD700>/casino coinflip   <dark_gray>— <gray>CoinFlip PvP"))
        sender.sendMessage(plugin.format(" <#FF5555>/casino rps        <dark_gray>— <gray>Piedra Papel Tijera PvP"))
        sender.sendMessage(plugin.format(" <#00FFFF>/casino ttt        <dark_gray>— <gray>Tic Tac Toe PvP"))
        sender.sendMessage(plugin.format(" <#FFD700>/casino carreras   <dark_gray>— <gray>Carreras de caballos"))
        sender.sendMessage(plugin.format("<dark_gray>─────────────────────────────────────────────"))
        sender.sendMessage(plugin.format(" <gray>/casino stats [juego]  <dark_gray>•  <gray>/casino top [juego]"))
        if (admin) {
            sender.sendMessage(plugin.format("<dark_gray>─────────────────────────────────────────────"))
            sender.sendMessage(plugin.format(" <red>/casino <juego> setup<dark_gray>/<gray>delete<dark_gray>/<gray>purge  <dark_gray>— <gray>Admin"))
            sender.sendMessage(plugin.format(" <red>/casino reload  <dark_gray>— <gray>Recargar configuración"))
        }
        sender.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
    }

    // ── showStats / showTop (sin cambios de lógica) ───────────────────────────
    private fun showStats(player: Player, mode: String?) {
        val stats = plugin.statsManager.getOrCreate(player.uniqueId, player.name)
        val lines = mutableListOf<String>()
        lines += "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        lines += " <#FFD700>🎰 <gold><bold>CASINO STATS</bold></gold>  <gray>—  <white>${player.name}"
        lines += "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        when (mode) {
            null, "global" -> {
                lines += " <#FFD700>💰 <gray>Total Apostado:   <white>${stats.totalWagered.fmt()}"
                lines += " <#00FF7F>💵 <gray>Total Ganado:     <#00FF7F>${stats.totalWon.fmt()}"
                val pc = if (stats.profit >= 0) "<#00FF7F>" else "<#FF5555>"
                lines += " ${pc}📊 <gray>Beneficio neto:  ${pc}${stats.profit.fmt()}"
                lines += "<dark_gray>─────────────────────────────────────────────"
                lines += " <#FF00FF>🌀 <#FF00FF><bold>Ruleta</bold>  <gray>Apuestas: <white>${stats.rouletteBets}  <gray>G/P: <#00FF7F>${stats.rouletteWins}<gray>/<#FF5555>${stats.rouletteLosses}"
                lines += "    <gray>Apostado: <white>${stats.rouletteWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.rouletteWon.fmt()}"
                lines += " <#FFB400>🎰 <#FFB400><bold>Slots</bold>  <gray>Giros: <white>${stats.slotsSpins}   <gray>Jackpots: <#FFD700>${stats.slotsJackpots}"
                lines += "    <gray>Apostado: <white>${stats.slotsWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.slotsWon.fmt()}"
                lines += " <#00FFFF>♠ <#00FFFF><bold>Blackjack</bold>  <gray>Partidas: <white>${stats.bjGames}  <gray>G/P: <#00FF7F>${stats.bjWins}<gray>/<#FF5555>${stats.bjLosses}  <gray>BJ: <#FFD700>${stats.bjBlackjacks}"
                lines += "    <gray>Apostado: <white>${stats.bjWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.bjWon.fmt()}"
                lines += " <#FF88FF>🎟 <#FF88FF><bold>Rasca y Gana</bold>  <gray>Boletos: <white>${stats.scratchUsed}  <gray>Ganados: <#00FF7F>${stats.scratchWins}"
                lines += "    <gray>Gastado: <white>${stats.scratchSpent.fmt()}   <gray>Ganado: <#00FF7F>${stats.scratchWon.fmt()}"
                lines += " <#FFD700>🎟 <#FFD700><bold>Lotería</bold>  <gray>Boletos: <white>${stats.lotteryTickets}  <gray>Premios: <#00FF7F>${stats.lotteryWins}"
                lines += "    <gray>Gastado: <white>${stats.lotterySpent.fmt()}   <gray>Ganado: <#00FF7F>${stats.lotteryWon.fmt()}"
                lines += " <#FFD700>🪙 <#FFD700><bold>CoinFlip</bold>  <gray>Partidas: <white>${stats.coinFlipFlips}  <gray>G/P: <#00FF7F>${stats.coinFlipWins}<gray>/<#FF5555>${stats.coinFlipLosses}"
                lines += "    <gray>Apostado: <white>${stats.coinFlipWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.coinFlipWon.fmt()}"
                lines += " <#FFD700>🏇 <#FFD700><bold>Carreras</bold>  <gray>G/P: <#00FF7F>${stats.racingWins}<gray>/<#FF5555>${stats.racingLosses}"
                lines += "    <gray>Apostado: <white>${stats.racingWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.racingWon.fmt()}"
            }
            "ruleta" -> {
                lines += " <#FF00FF>🌀 <gold><bold>RULETA</bold>"
                lines += " <gray>Apuestas: <white>${stats.rouletteBets}  <gray>G/P: <#00FF7F>${stats.rouletteWins}<gray>/<#FF5555>${stats.rouletteLosses}"
                val wr = if (stats.rouletteBets > 0) "${stats.rouletteWins * 100 / stats.rouletteBets}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Apostado: <white>${stats.rouletteWagered.fmt()}  <gray>Ganado: <#00FF7F>${stats.rouletteWon.fmt()}"
            }
            "slots", "tragamonedas" -> {
                lines += " <#FFB400>🎰 <gold><bold>TRAGAMONEDAS 777</bold>"
                lines += " <gray>Giros: <white>${stats.slotsSpins}  <gray>Jackpots: <#FFD700>${stats.slotsJackpots}"
                lines += " <gray>Apostado: <white>${stats.slotsWagered.fmt()}  <gray>Ganado: <#00FF7F>${stats.slotsWon.fmt()}"
            }
            "blackjack", "bj", "21" -> {
                lines += " <#00FFFF>♠ <gold><bold>BLACKJACK</bold>"
                lines += " <gray>Partidas: <white>${stats.bjGames}  <gray>G/P/BJ: <#00FF7F>${stats.bjWins}<gray>/<#FF5555>${stats.bjLosses}<gray>/<#FFD700>${stats.bjBlackjacks}"
                val wr = if (stats.bjGames > 0) "${stats.bjWins * 100 / stats.bjGames}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Apostado: <white>${stats.bjWagered.fmt()}  <gray>Ganado: <#00FF7F>${stats.bjWon.fmt()}"
            }
            "scratch", "rasca", "boleto" -> {
                lines += " <#FF88FF>🎟 <gold><bold>RASCA Y GANA</bold>"
                lines += " <gray>Boletos: <white>${stats.scratchUsed}  <gray>Ganados: <#00FF7F>${stats.scratchWins}"
                lines += " <gray>Gastado: <white>${stats.scratchSpent.fmt()}  <gray>Ganado: <#00FF7F>${stats.scratchWon.fmt()}"
            }
            "loteria", "lottery" -> {
                lines += " <#FFD700>🎟 <gold><bold>LOTERÍA</bold>"
                lines += " <gray>Boletos: <white>${stats.lotteryTickets}  <gray>Premios: <#00FF7F>${stats.lotteryWins}"
                lines += " <gray>Gastado: <white>${stats.lotterySpent.fmt()}  <gray>Ganado: <#00FF7F>${stats.lotteryWon.fmt()}"
            }
            "coinflip", "cf" -> {
                lines += " <#FFD700>🪙 <gold><bold>COINFLIP</bold>"
                lines += " <gray>Partidas: <white>${stats.coinFlipFlips}  <gray>G/P: <#00FF7F>${stats.coinFlipWins}<gray>/<#FF5555>${stats.coinFlipLosses}"
                val wr = if (stats.coinFlipFlips > 0) "${stats.coinFlipWins * 100 / stats.coinFlipFlips}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Apostado: <white>${stats.coinFlipWagered.fmt()}  <gray>Ganado: <#00FF7F>${stats.coinFlipWon.fmt()}"
            }
            "carreras", "racing" -> {
                lines += " <#FFD700>🏇 <gold><bold>CARRERAS</bold>"
                lines += " <gray>G/P: <#00FF7F>${stats.racingWins}<gray>/<#FF5555>${stats.racingLosses}"
                val total = stats.racingWins + stats.racingLosses
                val wr = if (total > 0) "${stats.racingWins * 100 / total}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Apostado: <white>${stats.racingWagered.fmt()}  <gray>Ganado: <#00FF7F>${stats.racingWon.fmt()}"
            }
            else -> { player.sendMessage(msg("stats.usage")); return }
        }
        lines += "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        lines.forEach { player.sendMessage(plugin.format(it)) }
    }

    private fun showTop(player: Player, mode: TopMode) {
        player.sendMessage(plugin.format("<#FFB400>Cargando top <gold>${mode.label}<#FFB400>..."))
        plugin.statsManager.getTopAsync(mode, 10) { entries ->
            player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
            player.sendMessage(plugin.format(" <#FFD700>🏆 <gold><bold>TOP 10 — ${mode.label.uppercase()}</bold>"))
            player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
            if (entries.isEmpty()) {
                player.sendMessage(plugin.format(" <gray>Aún no hay datos disponibles."))
            } else {
                entries.forEach { e ->
                    val medal = when (e.position) { 1 -> "<#FFD700>🥇"; 2 -> "<#C0C0C0>🥈"; 3 -> "<#CD7F32>🥉"; else -> "<gray>#${e.position}" }
                    player.sendMessage(plugin.format(" $medal <white>${e.name} <dark_gray>→ <#00FF7F>${e.value.fmt()}"))
                }
            }
            player.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
        }
    }

    private fun resolveMode(arg: String?): TopMode = when (arg?.lowercase()) {
        "ruleta"                   -> TopMode.RULETA
        "slots", "tragamonedas"    -> TopMode.SLOTS
        "blackjack", "bj", "21"   -> TopMode.BLACKJACK
        "scratch", "rasca"         -> TopMode.SCRATCH
        "loteria", "lottery"       -> TopMode.LOTTERY
        "coinflip", "cf"           -> TopMode.COINFLIP
        "carreras", "racing"       -> TopMode.RACING
        else                       -> TopMode.GLOBAL
    }

    // ── Tab Complete ──────────────────────────────────────────────────────────
    private val adminSubs      = listOf("setup", "delete", "purge", "forcestart")
    private val ruletaSubs     get() = mutableListOf("jugar", "ayuda").also { if (true) it.addAll(adminSubs + "escala") }
    private val blackjackSubs  = listOf("jugar", "leave")
    private val slotsSubs      = listOf("jugar")
    private val pokerSubs      = listOf("jugar", "leave")
    private val boletoSubs     = listOf("comprar")
    private val loteriaSubs    = listOf("info", "comprar")
    private val coinflipSubs   = listOf("menu", "crear", "unirse", "cancelar")
    private val statsModes     = listOf("ruleta", "slots", "blackjack", "scratch", "loteria", "coinflip", "carreras")
    private val games          = listOf("ruleta", "blackjack", "tragamonedas", "poker", "boleto", "loteria", "coinflip", "rps", "ttt", "carreras")

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        val admin = isAdmin(sender)
        return when (args.size) {
            1 -> {
                val opts = mutableListOf("stats", "top") + games
                (if (admin) opts + "reload" else opts).filter { it.startsWith(args[0], true) }
            }
            2 -> when (args[0].lowercase()) {
                "ruleta", "roulette" -> {
                    val subs = mutableListOf("jugar", "ayuda")
                    if (admin) subs.addAll(adminSubs + "escala")
                    subs.filter { it.startsWith(args[1], true) }
                }
                "blackjack", "bj", "21" -> {
                    val subs = mutableListOf("jugar", "leave")
                    if (admin) subs.addAll(listOf("setup", "delete"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "tragamonedas", "slots", "777" -> {
                    val subs = mutableListOf("jugar")
                    if (admin) subs.addAll(listOf("setup", "delete", "purge"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "poker" -> {
                    val subs = mutableListOf("jugar", "leave")
                    if (admin) subs.addAll(listOf("setup", "delete"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "boleto", "scratch" -> listOf("comprar", "get", "give").filter { it.startsWith(args[1], true) }
                "loteria", "lottery" -> {
                    val subs = mutableListOf("info", "comprar")
                    if (admin) subs.addAll(listOf("forcestart", "give"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "coinflip", "cf", "moneda" -> listOf("menu", "crear", "unirse", "cancelar").filter { it.startsWith(args[1], true) }
                "rps" -> listOf("crear", "unirse", "cancelar", "ayuda").filter { it.startsWith(args[1], true) }
                "ttt", "tictactoe" -> listOf("crear", "unirse", "cancelar", "ayuda").filter { it.startsWith(args[1], true) }
                "carreras", "racing" -> listOf("jugar", "ayuda").filter { it.startsWith(args[1], true) }
                "stats", "top" -> statsModes.filter { it.startsWith(args[1], true) }
                else -> emptyList()
            }
            3 -> when {
                args[0].equals("ruleta", true) && args[1].equals("escala", true) ->
                    listOf("0.3", "0.45", "0.6", "0.8", "1.0", "1.5").filter { it.startsWith(args[2]) }
                else -> emptyList()
            }
            4 -> when {
                args[0].equals("ruleta", true) && args[1].equals("escala", true) ->
                    listOf("3.5", "5.5", "7.0", "10.0").filter { it.startsWith(args[3]) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
