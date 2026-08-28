package liric.casino.stats

import java.util.UUID

data class PlayerStats(
    val uuid: UUID,
    var name: String,

    var rouletteBets: Int = 0,
    var rouletteWagered: Double = 0.0,
    var rouletteWon: Double = 0.0,
    var rouletteWins: Int = 0,
    var rouletteLosses: Int = 0,
    var rouletteDailyUses: Int = 0,

    var slotsSpins: Int = 0,
    var slotsWagered: Double = 0.0,
    var slotsWon: Double = 0.0,
    var slotsJackpots: Int = 0,
    var slotsDailyUses: Int = 0,

    var bjGames: Int = 0,
    var bjWagered: Double = 0.0,
    var bjWon: Double = 0.0,
    var bjWins: Int = 0,
    var bjLosses: Int = 0,
    var bjBlackjacks: Int = 0,
    var bjDailyUses: Int = 0,

    var scratchUsed: Int = 0,
    var scratchSpent: Double = 0.0,
    var scratchWon: Double = 0.0,
    var scratchWins: Int = 0,
    var scratchDailyUses: Int = 0,

    var lotteryTickets: Int = 0,
    var lotterySpent: Double = 0.0,
    var lotteryWon: Double = 0.0,
    var lotteryWins: Int = 0,
    var lotteryDailyUses: Int = 0,

    var coinFlipFlips: Int = 0,
    var coinFlipWagered: Double = 0.0,
    var coinFlipWon: Double = 0.0,
    var coinFlipWins: Int = 0,
    var coinFlipLosses: Int = 0,
    var coinflipDailyUses: Int = 0,

    var racingWagered: Double = 0.0,
    var racingWon: Double = 0.0,
    var racingWins: Int = 0,
    var racingLosses: Int = 0,
    var racingDailyUses: Int = 0,

    var rpsDailyUses: Int = 0,
    var tttDailyUses: Int = 0,
    var pokerDailyUses: Int = 0,

    var lastDailyReset: Long = System.currentTimeMillis(),
    var lastSeen: Long = System.currentTimeMillis(),
    @Volatile var dirty: Boolean = false
) {
    val totalWagered get() = rouletteWagered + slotsWagered + bjWagered + scratchSpent + lotterySpent + coinFlipWagered + racingWagered
    val totalWon     get() = rouletteWon + slotsWon + bjWon + scratchWon + lotteryWon + coinFlipWon + racingWon
    val profit       get() = totalWon - totalWagered
}


data class TopEntry(val position: Int, val name: String, val value: Double)
