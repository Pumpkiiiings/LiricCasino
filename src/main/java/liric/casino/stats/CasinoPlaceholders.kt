package liric.casino.stats

import liric.casino.CasinoPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import java.text.NumberFormat
import java.util.Locale

class CasinoPlaceholders(private val plugin: CasinoPlugin) : PlaceholderExpansion() {

    override fun getIdentifier() = "casino"
    override fun getAuthor()     = "Liric Casino"
    override fun getVersion()    = plugin.description.version
    override fun persist()       = true
    override fun canRegister()   = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return "0"

        val stats = plugin.statsManager.getCached(player.uniqueId)
            ?: plugin.statsManager.getOrCreate(player.uniqueId, player.name).also { return "..." }

        return when (params.lowercase()) {

            "total_wagered"        -> stats.totalWagered.fmt()
            "total_won"            -> stats.totalWon.fmt()
            "profit"               -> stats.profit.fmt()


            "roulette_bets"        -> stats.rouletteBets.toString()
            "roulette_wagered"     -> stats.rouletteWagered.fmt()
            "roulette_won"         -> stats.rouletteWon.fmt()
            "roulette_wins"        -> stats.rouletteWins.toString()
            "roulette_losses"      -> stats.rouletteLosses.toString()
            "roulette_winrate"     -> stats.rouletteBets.let { total ->
                if (total > 0) "${(stats.rouletteWins * 100 / total)}%" else "N/A"
            }


            "slots_spins"          -> stats.slotsSpins.toString()
            "slots_wagered"        -> stats.slotsWagered.fmt()
            "slots_won"            -> stats.slotsWon.fmt()
            "slots_jackpots"       -> stats.slotsJackpots.toString()


            "bj_games"             -> stats.bjGames.toString()
            "bj_wagered"           -> stats.bjWagered.fmt()
            "bj_won"               -> stats.bjWon.fmt()
            "bj_wins"              -> stats.bjWins.toString()
            "bj_losses"            -> stats.bjLosses.toString()
            "bj_blackjacks"        -> stats.bjBlackjacks.toString()
            "bj_winrate"           -> stats.bjGames.let { g ->
                if (g > 0) "${(stats.bjWins * 100 / g)}%" else "N/A"
            }


            "scratch_used"         -> stats.scratchUsed.toString()
            "scratch_spent"        -> stats.scratchSpent.fmt()
            "scratch_won"          -> stats.scratchWon.fmt()
            "scratch_wins"         -> stats.scratchWins.toString()

            else -> null
        }
    }

    private fun Double.fmt(): String =
        "$" + NumberFormat.getNumberInstance(Locale.US).format(this)
}
