package liric.casino.commands

import liric.casino.CasinoPlugin
import liric.casino.games.blackjack.BlackjackChoiceMenu
import liric.casino.games.coinflip.CoinFlipGUI
import liric.casino.games.lottery.LotteryGUI
import liric.casino.games.slots.SlotMachineMenu
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
    private fun isAdmin(sender: CommandSender) = sender.hasPermission("casino.admin")

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendMainHelp(sender)
            return true
        }

        when (args[0].lowercase()) {

            "roulette"                 -> handleRoulette(sender, args.drop(1))
            "blackjack", "bj", "21"    -> handleBlackjack(sender, args.drop(1))
            "slots", "777"             -> handleSlots(sender, args.drop(1))
            "poker"                    -> handlePoker(sender, args.drop(1))
            "scratch"                  -> handleScratch(sender, args.drop(1))
            "lottery"                  -> handleLottery(sender, args.drop(1))
            "coinflip", "cf"           -> handleCoinFlip(sender, args.drop(1))
            "rps"                      -> handleRPS(sender, args.drop(1))
            "ttt", "tictactoe"         -> handleTTT(sender, args.drop(1))
            "racing"                   -> handleRacing(sender, args.drop(1))

            "stats"                    -> handleStats(sender, args.drop(1))
            "top"                      -> handleTop(sender, args.drop(1))
            "reload"                   -> handleReload(sender)
            else                       -> sendMainHelp(sender)
        }
        return true
    }


    private fun handleRoulette(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        val sub = args.getOrNull(0)?.lowercase()
        val isAdminSub = sub != null && sub in listOf("setup", "delete", "forcestart", "purge", "scale", "help")
        if (!isAdminSub && !plugin.isGameEnabled("roulette")) {
            return
        }
        if (sub == null || sub == "play") {
            liric.casino.games.roulette.RouletteMenu(plugin, plugin.rouletteGame, sender).open(); return
        }
        when (sub) {
            "help" -> sendRouletteHelp(sender)
            "setup", "delete", "forcestart", "purge", "scale" -> {
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
                    "scale" -> {
                        val scaleArg  = args.getOrNull(1)?.toFloatOrNull()
                        val radiusArg = args.getOrNull(2)?.toFloatOrNull()
                        if (scaleArg == null || scaleArg !in 0.1f..2.0f) {
                            sender.sendMessage(msg("roulette.escala-usage"))
                            return
                        }
                        val radius = radiusArg?.coerceIn(2f, 15f)
                            ?: plugin.config.getDouble("roulette.radius", 5.5).toFloat()

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

    private fun sendRouletteHelp(player: Player) {
        val admin = isAdmin(player)
        plugin.messagesConfig.getStringList("help.roulette-header").forEach { player.sendMessage(plugin.format(it)) }
        if (admin) {
            plugin.messagesConfig.getStringList("help.roulette-admin").forEach { player.sendMessage(plugin.format(it)) }
        }
        player.sendMessage(plugin.format(plugin.messagesConfig.getString("help.footer-line", "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")!!))
    }


    private fun handleBlackjack(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        val sub0 = args.getOrNull(0)?.lowercase()
        if (sub0 !in listOf("setup", "delete") && !plugin.isGameEnabled("blackjack")) {
            return
        }
        when (sub0) {
            null, "play" -> BlackjackChoiceMenu(plugin, sender).open()
            "leave"      -> plugin.blackjackMultiGame.removePlayer(sender)
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


    private fun handleSlots(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        val sub0 = args.getOrNull(0)?.lowercase()
        if (sub0 !in listOf("setup", "delete", "purge") && !plugin.isGameEnabled("slots")) {
            return
        }
        when (sub0) {
            null, "play" -> {

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


    private fun handlePoker(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        val sub0 = args.getOrNull(0)?.lowercase()
        if (sub0 !in listOf("setup", "delete") && !plugin.isGameEnabled("poker")) {
            return
        }
        when (sub0) {
            null, "play", "join" -> plugin.pokerGame.addPlayer(sender)
            "leave"              -> plugin.pokerGame.removePlayer(sender)
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


    private fun handleScratch(sender: CommandSender, args: List<String>) {
        if (!plugin.isGameEnabled("scratch")) { return }
        val cmd = plugin.server.getPluginCommand("scratch") ?: return
        cmd.execute(sender, "scratch", args.toTypedArray())
    }


    private fun handleLottery(sender: CommandSender, args: List<String>) {
        val isAdminSub = args.getOrNull(0)?.lowercase() in listOf("forcestart", "give")
        if (!isAdminSub && !plugin.isGameEnabled("lottery")) { return }
        val cmd = plugin.server.getPluginCommand("lottery") ?: return
        cmd.execute(sender, "lottery", args.toTypedArray())
    }


    private fun handleCoinFlip(sender: CommandSender, args: List<String>) {
        if (!plugin.isGameEnabled("coinflip")) { return }
        val cmd = plugin.server.getPluginCommand("coinflip") ?: return
        cmd.execute(sender, "coinflip", args.toTypedArray())
    }


    private fun handleRPS(sender: CommandSender, args: List<String>) {
        if (!plugin.isGameEnabled("rps")) { return }
        val cmd = plugin.server.getPluginCommand("rps") ?: return
        cmd.execute(sender, "rps", args.toTypedArray())
    }


    private fun handleTTT(sender: CommandSender, args: List<String>) {
        if (!plugin.isGameEnabled("ttt")) { return }
        val cmd = plugin.server.getPluginCommand("ttt") ?: return
        cmd.execute(sender, "ttt", args.toTypedArray())
    }


    private fun handleRacing(sender: CommandSender, args: List<String>) {
        if (!plugin.isGameEnabled("racing")) { return }
        val cmd = plugin.server.getPluginCommand("racing") ?: return
        cmd.execute(sender, "racing", args.toTypedArray())
    }


    private fun handleStats(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        showStats(sender, args.getOrNull(0)?.lowercase())
    }

    private fun handleTop(sender: CommandSender, args: List<String>) {
        if (sender !is Player) { sender.sendMessage(msg("general.only-players")); return }
        showTop(sender, resolveMode(args.getOrNull(0)))
    }


    private fun handleReload(sender: CommandSender) {
        if (!isAdmin(sender)) { sender.sendMessage(msg("general.no-permission")); return }
        val reloadingMsg = plugin.messagesConfig.getString("stats-menu.reloading", "<#FFB400>Reloading configuration...")!!
        sender.sendMessage(plugin.format(reloadingMsg.replace("{prefix}", plugin.messagesConfig.getString("prefix", "")!!)))
        plugin.reloadConfig()
        plugin.messages.load()
        val reloadedMsg = plugin.messagesConfig.getString("stats-menu.reloaded", "<#00FF7F>Configuration reloaded.")!!
        sender.sendMessage(plugin.format(reloadedMsg.replace("{prefix}", plugin.messagesConfig.getString("prefix", "")!!)))
    }


    private fun sendMainHelp(sender: CommandSender) {
        val admin = isAdmin(sender)
        plugin.messagesConfig.getStringList("help.main-header").forEach { sender.sendMessage(plugin.format(it)) }
        plugin.messagesConfig.getStringList("help.main-commands").forEach { sender.sendMessage(plugin.format(it)) }
        plugin.messagesConfig.getStringList("help.main-footer").forEach { sender.sendMessage(plugin.format(it)) }
        if (admin) {
            plugin.messagesConfig.getStringList("help.main-admin").forEach { sender.sendMessage(plugin.format(it)) }
        }
        sender.sendMessage(plugin.format(plugin.messagesConfig.getString("help.footer-line", "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")!!))
    }


    private fun showStats(player: Player, mode: String?) {
        val stats = plugin.statsManager.getOrCreate(player.uniqueId, player.name)
        val lines = mutableListOf<String>()
        lines += "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        lines += " <#FFD700>🎰 <gold><bold>CASINO STATS</bold></gold>  <gray>—  <white>${player.name}"
        lines += "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        when (mode) {
            null, "global" -> {
                lines += " <#FFD700>💰 <gray>Total Wagered:   <white>${stats.totalWagered.fmt()}"
                lines += " <#00FF7F>💵 <gray>Total Won:       <#00FF7F>${stats.totalWon.fmt()}"
                val pc = if (stats.profit >= 0) "<#00FF7F>" else "<#FF5555>"
                lines += " ${pc}📊 <gray>Net Profit:      ${pc}${stats.profit.fmt()}"
                lines += "<dark_gray>─────────────────────────────────────────────"
                lines += " <#FF00FF>🌀 <#FF00FF><bold>Roulette</bold>  <gray>Bets: <white>${stats.rouletteBets}  <gray>W/L: <#00FF7F>${stats.rouletteWins}<gray>/<#FF5555>${stats.rouletteLosses}"
                lines += "    <gray>Wagered: <white>${stats.rouletteWagered.fmt()}   <gray>Won: <#00FF7F>${stats.rouletteWon.fmt()}"
                lines += " <#FFB400>🎰 <#FFB400><bold>Slots</bold>  <gray>Spins: <white>${stats.slotsSpins}   <gray>Jackpots: <#FFD700>${stats.slotsJackpots}"
                lines += "    <gray>Wagered: <white>${stats.slotsWagered.fmt()}   <gray>Won: <#00FF7F>${stats.slotsWon.fmt()}"
                lines += " <#00FFFF>♠ <#00FFFF><bold>Blackjack</bold>  <gray>Games: <white>${stats.bjGames}  <gray>W/L: <#00FF7F>${stats.bjWins}<gray>/<#FF5555>${stats.bjLosses}  <gray>BJ: <#FFD700>${stats.bjBlackjacks}"
                lines += "    <gray>Wagered: <white>${stats.bjWagered.fmt()}   <gray>Won: <#00FF7F>${stats.bjWon.fmt()}"
                lines += " <#FF88FF>🎟 <#FF88FF><bold>Scratch Card</bold>  <gray>Cards: <white>${stats.scratchUsed}  <gray>Wins: <#00FF7F>${stats.scratchWins}"
                lines += "    <gray>Spent: <white>${stats.scratchSpent.fmt()}   <gray>Won: <#00FF7F>${stats.scratchWon.fmt()}"
                lines += " <#FFD700>🎟 <#FFD700><bold>Lottery</bold>  <gray>Tickets: <white>${stats.lotteryTickets}  <gray>Wins: <#00FF7F>${stats.lotteryWins}"
                lines += "    <gray>Spent: <white>${stats.lotterySpent.fmt()}   <gray>Won: <#00FF7F>${stats.lotteryWon.fmt()}"
                lines += " <#FFD700>🪙 <#FFD700><bold>CoinFlip</bold>  <gray>Games: <white>${stats.coinFlipFlips}  <gray>W/L: <#00FF7F>${stats.coinFlipWins}<gray>/<#FF5555>${stats.coinFlipLosses}"
                lines += "    <gray>Wagered: <white>${stats.coinFlipWagered.fmt()}   <gray>Won: <#00FF7F>${stats.coinFlipWon.fmt()}"
                lines += " <#FFD700>🏇 <#FFD700><bold>Horse Racing</bold>  <gray>W/L: <#00FF7F>${stats.racingWins}<gray>/<#FF5555>${stats.racingLosses}"
                lines += "    <gray>Wagered: <white>${stats.racingWagered.fmt()}   <gray>Won: <#00FF7F>${stats.racingWon.fmt()}"
            }
            "roulette" -> {
                lines += " <#FF00FF>🌀 <gold><bold>ROULETTE</bold>"
                lines += " <gray>Bets: <white>${stats.rouletteBets}  <gray>W/L: <#00FF7F>${stats.rouletteWins}<gray>/<#FF5555>${stats.rouletteLosses}"
                val wr = if (stats.rouletteBets > 0) "${stats.rouletteWins * 100 / stats.rouletteBets}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Wagered: <white>${stats.rouletteWagered.fmt()}  <gray>Won: <#00FF7F>${stats.rouletteWon.fmt()}"
            }
            "slots" -> {
                lines += " <#FFB400>🎰 <gold><bold>777 SLOTS</bold>"
                lines += " <gray>Spins: <white>${stats.slotsSpins}  <gray>Jackpots: <#FFD700>${stats.slotsJackpots}"
                lines += " <gray>Wagered: <white>${stats.slotsWagered.fmt()}  <gray>Won: <#00FF7F>${stats.slotsWon.fmt()}"
            }
            "blackjack", "bj", "21" -> {
                lines += " <#00FFFF>♠ <gold><bold>BLACKJACK</bold>"
                lines += " <gray>Games: <white>${stats.bjGames}  <gray>W/L/BJ: <#00FF7F>${stats.bjWins}<gray>/<#FF5555>${stats.bjLosses}<gray>/<#FFD700>${stats.bjBlackjacks}"
                val wr = if (stats.bjGames > 0) "${stats.bjWins * 100 / stats.bjGames}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Wagered: <white>${stats.bjWagered.fmt()}  <gray>Won: <#00FF7F>${stats.bjWon.fmt()}"
            }
            "scratch" -> {
                lines += " <#FF88FF>🎟 <gold><bold>SCRATCH CARDS</bold>"
                lines += " <gray>Cards: <white>${stats.scratchUsed}  <gray>Wins: <#00FF7F>${stats.scratchWins}"
                lines += " <gray>Spent: <white>${stats.scratchSpent.fmt()}  <gray>Won: <#00FF7F>${stats.scratchWon.fmt()}"
            }
            "lottery" -> {
                lines += " <#FFD700>🎟 <gold><bold>LOTTERY</bold>"
                lines += " <gray>Tickets: <white>${stats.lotteryTickets}  <gray>Prizes: <#00FF7F>${stats.lotteryWins}"
                lines += " <gray>Spent: <white>${stats.lotterySpent.fmt()}  <gray>Won: <#00FF7F>${stats.lotteryWon.fmt()}"
            }
            "coinflip", "cf" -> {
                lines += " <#FFD700>🪙 <gold><bold>COINFLIP</bold>"
                lines += " <gray>Games: <white>${stats.coinFlipFlips}  <gray>W/L: <#00FF7F>${stats.coinFlipWins}<gray>/<#FF5555>${stats.coinFlipLosses}"
                val wr = if (stats.coinFlipFlips > 0) "${stats.coinFlipWins * 100 / stats.coinFlipFlips}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Wagered: <white>${stats.coinFlipWagered.fmt()}  <gray>Won: <#00FF7F>${stats.coinFlipWon.fmt()}"
            }
            "racing" -> {
                lines += " <#FFD700>🏇 <gold><bold>HORSE RACING</bold>"
                lines += " <gray>W/L: <#00FF7F>${stats.racingWins}<gray>/<#FF5555>${stats.racingLosses}"
                val total = stats.racingWins + stats.racingLosses
                val wr = if (total > 0) "${stats.racingWins * 100 / total}%" else "N/A"
                lines += " <gray>Winrate: <#FFD700>$wr"
                lines += " <gray>Wagered: <white>${stats.racingWagered.fmt()}  <gray>Won: <#00FF7F>${stats.racingWon.fmt()}"
            }
            else -> { player.sendMessage(msg("stats.usage")); return }
        }
        lines += "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        lines.forEach { player.sendMessage(plugin.format(it)) }
    }

    private fun showTop(player: Player, mode: TopMode) {
        val loadMsg = plugin.messagesConfig.getString("stats-menu.loading-top", "{prefix} <#FFB400>Loading top <gold>{mode}<#FFB400>...")!!
            .replace("{mode}", mode.label)
        player.sendMessage(plugin.format(loadMsg.replace("{prefix}", plugin.messagesConfig.getString("prefix", "")!!)))
        plugin.statsManager.getTopAsync(mode, 10) { entries ->
            plugin.messagesConfig.getStringList("stats-menu.top-header").forEach { 
                player.sendMessage(plugin.format(it.replace("{mode}", mode.label.uppercase()))) 
            }
            if (entries.isEmpty()) {
                val empty = plugin.messagesConfig.getString("stats-menu.top-empty", " <gray>No data available yet.")!!
                player.sendMessage(plugin.format(empty))
            } else {
                val format = plugin.messagesConfig.getString("stats-menu.top-format", " {medal} <white>{player} <dark_gray>→ <#00FF7F>{value}")!!
                entries.forEach { e ->
                    val medal = when (e.position) { 1 -> "<#FFD700>🥇"; 2 -> "<#C0C0C0>🥈"; 3 -> "<#CD7F32>🥉"; else -> "<gray>#${e.position}" }
                    player.sendMessage(plugin.format(format.replace("{medal}", medal).replace("{player}", e.name).replace("{value}", e.value.fmt())))
                }
            }
            player.sendMessage(plugin.format(plugin.messagesConfig.getString("help.footer-line", "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")!!))
        }
    }

    private fun resolveMode(arg: String?): TopMode = when (arg?.lowercase()) {
        "roulette"                 -> TopMode.RULETA
        "slots"                    -> TopMode.SLOTS
        "blackjack", "bj", "21"    -> TopMode.BLACKJACK
        "scratch"                  -> TopMode.SCRATCH
        "lottery"                  -> TopMode.LOTTERY
        "coinflip", "cf"           -> TopMode.COINFLIP
        "racing"                   -> TopMode.RACING
        else                       -> TopMode.GLOBAL
    }


    private val adminSubs      = listOf("setup", "delete", "purge", "forcestart")
    private val rouletteSubs   get() = mutableListOf("play", "help").also { if (true) it.addAll(adminSubs + "scale") }
    private val blackjackSubs  = listOf("play", "leave")
    private val slotsSubs      = listOf("play")
    private val pokerSubs      = listOf("play", "leave")
    private val scratchSubs    = listOf("buy")
    private val lotterySubs    = listOf("info", "buy")
    private val coinflipSubs   = listOf("menu", "create", "join", "cancel")
    private val statsModes     = listOf("roulette", "slots", "blackjack", "scratch", "lottery", "coinflip", "racing")
    private val games          = listOf("roulette", "blackjack", "slots", "poker", "scratch", "lottery", "coinflip", "rps", "ttt", "racing")

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<out String>): List<String> {
        val admin = isAdmin(sender)
        return when (args.size) {
            1 -> {
                val opts = mutableListOf("stats", "top") + games
                (if (admin) opts + "reload" else opts).filter { it.startsWith(args[0], true) }
            }
            2 -> when (args[0].lowercase()) {
                "roulette" -> {
                    val subs = mutableListOf("play", "help")
                    if (admin) subs.addAll(adminSubs + "scale")
                    subs.filter { it.startsWith(args[1], true) }
                }
                "blackjack", "bj", "21" -> {
                    val subs = mutableListOf("play", "leave")
                    if (admin) subs.addAll(listOf("setup", "delete"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "slots", "777" -> {
                    val subs = mutableListOf("play")
                    if (admin) subs.addAll(listOf("setup", "delete", "purge"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "poker" -> {
                    val subs = mutableListOf("play", "leave")
                    if (admin) subs.addAll(listOf("setup", "delete"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "scratch" -> listOf("buy", "get", "give").filter { it.startsWith(args[1], true) }
                "lottery" -> {
                    val subs = mutableListOf("info", "buy")
                    if (admin) subs.addAll(listOf("forcestart", "give"))
                    subs.filter { it.startsWith(args[1], true) }
                }
                "coinflip", "cf" -> listOf("menu", "create", "join", "cancel").filter { it.startsWith(args[1], true) }
                "rps" -> listOf("create", "join", "cancel", "help").filter { it.startsWith(args[1], true) }
                "ttt", "tictactoe" -> listOf("create", "join", "cancel", "help").filter { it.startsWith(args[1], true) }
                "racing" -> listOf("play", "help").filter { it.startsWith(args[1], true) }
                "stats", "top" -> statsModes.filter { it.startsWith(args[1], true) }
                else -> emptyList()
            }
            3 -> when {
                args[0].equals("roulette", true) && args[1].equals("scale", true) ->
                    listOf("0.3", "0.45", "0.6", "0.8", "1.0", "1.5").filter { it.startsWith(args[2]) }
                else -> emptyList()
            }
            4 -> when {
                args[0].equals("roulette", true) && args[1].equals("scale", true) ->
                    listOf("3.5", "5.5", "7.0", "10.0").filter { it.startsWith(args[3]) }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
}
