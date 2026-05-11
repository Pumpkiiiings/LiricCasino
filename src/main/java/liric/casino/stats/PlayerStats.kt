package liric.casino.stats

import java.util.UUID

data class PlayerStats(
    val uuid: UUID,
    var playerName: String,
    // ── Ruleta ───────────────────────────────────────────
    var rouletteBets: Int = 0,
    var rouletteWagered: Double = 0.0,
    var rouletteWon: Double = 0.0,
    var rouletteWins: Int = 0,
    var rouletteLosses: Int = 0,
    // ── Slots 777 ────────────────────────────────────────
    var slotsSpins: Int = 0,
    var slotsWagered: Double = 0.0,
    var slotsWon: Double = 0.0,
    var slotsJackpots: Int = 0,
    // ── Blackjack ────────────────────────────────────────
    var bjGames: Int = 0,
    var bjWagered: Double = 0.0,
    var bjWon: Double = 0.0,
    var bjWins: Int = 0,
    var bjLosses: Int = 0,
    var bjBlackjacks: Int = 0,
    // ── Rasca y Gana ─────────────────────────────────────
    var scratchUsed: Int = 0,
    var scratchSpent: Double = 0.0,
    var scratchWon: Double = 0.0,
    var scratchWins: Int = 0,
    // ── Meta ─────────────────────────────────────────────
    var lastSeen: Long = System.currentTimeMillis(),
    @Volatile var dirty: Boolean = false  // true = necesita guardarse en DB
) {
    val totalWagered get() = rouletteWagered + slotsWagered + bjWagered + scratchSpent
    val totalWon     get() = rouletteWon + slotsWon + bjWon + scratchWon
    val profit       get() = totalWon - totalWagered
}

/** Entrada del leaderboard */
data class TopEntry(val position: Int, val name: String, val value: Double)
