package liric.casino.commands

import liric.casino.CasinoPlugin
import liric.casino.stats.StatsManager.TopMode
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.text.NumberFormat
import java.util.Locale

class CasinoCommand(private val plugin: CasinoPlugin) : CommandExecutor, TabCompleter {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)
    private fun Double.fmt() = "$" + NumberFormat.getNumberInstance(Locale.US).format(this)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return true }

        when (args.getOrNull(0)?.lowercase()) {
            null, "stats" -> {
                val sub = args.getOrNull(1)?.lowercase()
                if (sub == "top") {
                    val mode = resolveMode(args.getOrNull(2))
                    showTop(sender, mode)
                } else {
                    showStats(sender, sub)
                }
            }
            "top" -> {
                val mode = resolveMode(args.getOrNull(1))
                showTop(sender, mode)
            }
            "reload" -> {
                if (!sender.hasPermission("casino.admin")) { sender.sendMessage(msg("general.no-permission")); return true }
                sender.sendMessage(plugin.format("<#FFB400>Recargando configuración..."))
                plugin.reloadConfig()
                plugin.messages.load()
                sender.sendMessage(plugin.format("<#00FF7F>Configuración recargada."))
            }
            else -> sender.sendMessage(msg("stats.usage"))
        }
        return true
    }

    // ── Mostrar stats del jugador ─────────────────────────────────────────────
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
                val profitColor = if (stats.profit >= 0) "<#00FF7F>" else "<#FF5555>"
                lines += " ${profitColor}📊 <gray>Beneficio neto:  ${profitColor}${stats.profit.fmt()}"
                lines += "<dark_gray>─────────────────────────────────────────────"
                lines += " <#FF00FF>🌀 <#FF00FF><bold>Ruleta</bold>  <gray>Apuestas: <white>${stats.rouletteBets}  <gray>G/P: <#00FF7F>${stats.rouletteWins}<gray>/<#FF5555>${stats.rouletteLosses}"
                lines += "    <gray>Apostado: <white>${stats.rouletteWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.rouletteWon.fmt()}"
                lines += " <#FFB400>🎰 <#FFB400><bold>Slots</bold>  <gray>Giros: <white>${stats.slotsSpins}   <gray>Jackpots: <#FFD700>${stats.slotsJackpots}"
                lines += "    <gray>Apostado: <white>${stats.slotsWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.slotsWon.fmt()}"
                lines += " <#00FFFF>♠ <#00FFFF><bold>Blackjack</bold>  <gray>Partidas: <white>${stats.bjGames}  <gray>G/P: <#00FF7F>${stats.bjWins}<gray>/<#FF5555>${stats.bjLosses}  <gray>BJ: <#FFD700>${stats.bjBlackjacks}"
                lines += "    <gray>Apostado: <white>${stats.bjWagered.fmt()}   <gray>Ganado: <#00FF7F>${stats.bjWon.fmt()}"
                lines += " <#FF88FF>🎟 <#FF88FF><bold>Rasca y Gana</bold>  <gray>Boletos: <white>${stats.scratchUsed}  <gray>Ganados: <#00FF7F>${stats.scratchWins}"
                lines += "    <gray>Gastado: <white>${stats.scratchSpent.fmt()}   <gray>Ganado: <#00FF7F>${stats.scratchWon.fmt()}"
            }
            "ruleta" -> {
                lines += " <#FF00FF>🌀 <gold><bold>RULETA MIXTA</bold>"
                lines += " <gray>Apuestas totales:  <white>${stats.rouletteBets}"
                lines += " <gray>Victorias / Derrotas: <#00FF7F>${stats.rouletteWins} <gray>/ <#FF5555>${stats.rouletteLosses}"
                val wr = if (stats.rouletteBets > 0) "${stats.rouletteWins * 100 / stats.rouletteBets}%" else "N/A"
                lines += " <gray>Winrate:           <#FFD700>$wr"
                lines += " <gray>Total apostado:    <white>${stats.rouletteWagered.fmt()}"
                lines += " <gray>Total ganado:      <#00FF7F>${stats.rouletteWon.fmt()}"
            }
            "slots", "tragamonedas" -> {
                lines += " <#FFB400>🎰 <gold><bold>TRAGAMONEDAS 777</bold>"
                lines += " <gray>Giros totales: <white>${stats.slotsSpins}"
                lines += " <gray>Jackpots:      <#FFD700>${stats.slotsJackpots}"
                lines += " <gray>Total apostado: <white>${stats.slotsWagered.fmt()}"
                lines += " <gray>Total ganado:   <#00FF7F>${stats.slotsWon.fmt()}"
            }
            "blackjack", "bj", "21" -> {
                lines += " <#00FFFF>♠ <gold><bold>BLACKJACK</bold>"
                lines += " <gray>Partidas: <white>${stats.bjGames}"
                lines += " <gray>Victorias / Derrotas / Blackjacks: <#00FF7F>${stats.bjWins} <gray>/ <#FF5555>${stats.bjLosses} <gray>/ <#FFD700>${stats.bjBlackjacks}"
                val wr = if (stats.bjGames > 0) "${stats.bjWins * 100 / stats.bjGames}%" else "N/A"
                lines += " <gray>Winrate:        <#FFD700>$wr"
                lines += " <gray>Total apostado: <white>${stats.bjWagered.fmt()}"
                lines += " <gray>Total ganado:   <#00FF7F>${stats.bjWon.fmt()}"
            }
            "scratch", "rasca", "boleto" -> {
                lines += " <#FF88FF>🎟 <gold><bold>RASCA Y GANA</bold>"
                lines += " <gray>Boletos usados: <white>${stats.scratchUsed}"
                lines += " <gray>Boletos ganados: <#00FF7F>${stats.scratchWins}"
                lines += " <gray>Total gastado:  <white>${stats.scratchSpent.fmt()}"
                lines += " <gray>Total ganado:   <#00FF7F>${stats.scratchWon.fmt()}"
            }
            else -> { player.sendMessage(msg("stats.usage")); return }
        }
        lines += "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        lines.forEach { player.sendMessage(plugin.format(it)) }
    }

    // ── Leaderboard ───────────────────────────────────────────────────────────
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
                    val medal = when (e.position) { 1 -> "<#FFD700>🥇" ; 2 -> "<#C0C0C0>🥈" ; 3 -> "<#CD7F32>🥉" ; else -> "<gray>#${e.position}" }
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
        else                       -> TopMode.GLOBAL
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        val modes = listOf("ruleta", "slots", "blackjack", "scratch", "top")
        return when {
            args.size == 1 -> listOf("stats", "top").filter { it.startsWith(args[0], true) }
            args.size == 2 -> modes.filter { it.startsWith(args[1], true) }
            args.size == 3 && args[1].equals("top", true) ->
                listOf("ruleta", "slots", "blackjack", "scratch").filter { it.startsWith(args[2], true) }
            else -> emptyList()
        }
    }
}
