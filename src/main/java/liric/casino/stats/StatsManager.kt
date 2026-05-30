package liric.casino.stats

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class StatsManager(private val plugin: CasinoPlugin) {

    private val cache = ConcurrentHashMap<UUID, PlayerStats>()

    // ─── Ciclo de vida ────────────────────────────────────────────────────────
    fun startAutoSave() {
        object : BukkitRunnable() {
            override fun run() { saveAllAsync() }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L)
    }

    fun shutdown() {
        cache.values.filter { it.dirty }.forEach { saveToDB(it) }
    }

    // ─── Carga / obtención ───────────────────────────────────────────────────
    fun loadAsync(uuid: UUID, name: String) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val stats = loadFromDB(uuid, name)
            cache[uuid] = stats
        })
    }

    fun unload(uuid: UUID) {
        val stats = cache.remove(uuid) ?: return
        if (stats.dirty) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable { saveToDB(stats) })
        }
    }

    fun getCached(uuid: UUID): PlayerStats? = cache[uuid]

    fun getOrCreate(uuid: UUID, name: String): PlayerStats =
        cache.getOrPut(uuid) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val loaded = loadFromDB(uuid, name)
                cache[uuid] = loaded
            })
            PlayerStats(uuid, name)
        }

    // ─── Registros ───────────────────────────────────────────────────────────

    fun recordRouletteBet(uuid: UUID, wagered: Double) = update(uuid) {
        rouletteBets++; rouletteWagered += wagered
    }
    fun recordRouletteWin(uuid: UUID, won: Double) = update(uuid) {
        rouletteWon += won; rouletteWins++
    }
    fun recordRouletteLoss(uuid: UUID) = update(uuid) { rouletteLosses++ }

    fun recordSlotSpin(uuid: UUID, wagered: Double) = update(uuid) {
        slotsSpins++; slotsWagered += wagered
    }
    fun recordSlotWin(uuid: UUID, won: Double, isJackpot: Boolean) = update(uuid) {
        slotsWon += won
        if (isJackpot) slotsJackpots++
    }

    fun recordBjGame(uuid: UUID, wagered: Double, won: Double, isWin: Boolean, isLoss: Boolean, isBlackjack: Boolean) =
        update(uuid) {
            bjGames++; bjWagered += wagered; bjWon += won
            if (isWin) bjWins++
            if (isLoss) bjLosses++
            if (isBlackjack) bjBlackjacks++
        }

    fun recordScratch(uuid: UUID, spent: Double, won: Double, didWin: Boolean) = update(uuid) {
        scratchUsed++; scratchSpent += spent; scratchWon += won
        if (didWin) scratchWins++
    }

    fun recordLotteryBuy(uuid: UUID, spent: Double) = update(uuid) {
        lotteryTickets++; lotterySpent += spent
    }

    fun recordLotteryWin(uuid: UUID, won: Double) = update(uuid) {
        lotteryWon += won; lotteryWins++
    }

    fun recordCoinFlip(uuid: UUID, wagered: Double, won: Double, didWin: Boolean) = update(uuid) {
        coinFlipFlips++; coinFlipWagered += wagered; coinFlipWon += won
        if (didWin) coinFlipWins++ else coinFlipLosses++
    }

    fun recordGameUse(uuid: UUID, gameName: String) {
        update(uuid) {
            val now = System.currentTimeMillis()
            if (now - lastDailyReset > 86400000L) {
                rouletteDailyUses = 0
                slotsDailyUses = 0
                bjDailyUses = 0
                scratchDailyUses = 0
                lotteryDailyUses = 0
                coinflipDailyUses = 0
                racingDailyUses = 0
                rpsDailyUses = 0
                tttDailyUses = 0
                pokerDailyUses = 0
                lastDailyReset = now
            }
            when (gameName.lowercase()) {
                "roulette" -> rouletteDailyUses++
                "slots" -> slotsDailyUses++
                "blackjack" -> bjDailyUses++
                "scratch" -> scratchDailyUses++
                "lottery" -> lotteryDailyUses++
                "coinflip" -> coinflipDailyUses++
                "racing" -> racingDailyUses++
                "rps" -> rpsDailyUses++
                "ttt" -> tttDailyUses++
                "poker" -> pokerDailyUses++
            }
        }
    }

    fun getGameDailyUses(uuid: UUID, gameName: String): Int {
        val stats = cache[uuid] ?: return 0
        val now = System.currentTimeMillis()
        if (now - stats.lastDailyReset > 86400000L) return 0
        return when (gameName.lowercase()) {
            "roulette" -> stats.rouletteDailyUses
            "slots" -> stats.slotsDailyUses
            "blackjack" -> stats.bjDailyUses
            "scratch" -> stats.scratchDailyUses
            "lottery" -> stats.lotteryDailyUses
            "coinflip" -> stats.coinflipDailyUses
            "racing" -> stats.racingDailyUses
            "rps" -> stats.rpsDailyUses
            "ttt" -> stats.tttDailyUses
            "poker" -> stats.pokerDailyUses
            else -> 0
        }
    }

    fun recordRacingWin(uuid: UUID, won: Double) = update(uuid) {
        racingWon += won; racingWins++
    }

    fun recordRacingLoss(uuid: UUID, wagered: Double) = update(uuid) {
        racingWagered += wagered; racingLosses++
    }


    // ─── Top leaderboard ─────────────────────────────────────────────────────
    enum class TopMode(val sqlColumn: String, val label: String) {
        GLOBAL("(roulette_won + slots_won + bj_won + scratch_won + lottery_won + coinflip_won)", "Global"),
        RULETA("roulette_won", "Roulette"),
        SLOTS("slots_won", "Slots"),
        BLACKJACK("bj_won", "Blackjack"),
        SCRATCH("scratch_won", "Scratch Card"),
        LOTTERY("lottery_won", "Lottery"),
        COINFLIP("coinflip_won", "CoinFlip"),
        RACING("racing_won", "Horse Racing")
    }

    fun getTopAsync(mode: TopMode, limit: Int = 10, callback: (List<TopEntry>) -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val results = mutableListOf<TopEntry>()
            val sql = "SELECT player_name, ${mode.sqlColumn} AS val FROM casino_stats " +
                    "ORDER BY val DESC LIMIT $limit"
            runCatching {
                plugin.db.getConnection().use { conn ->
                    conn.createStatement().use { stmt ->
                        val rs: ResultSet = stmt.executeQuery(sql)
                        var pos = 1
                        while (rs.next()) {
                            results.add(TopEntry(pos++, rs.getString("player_name"), rs.getDouble("val")))
                        }
                    }
                }
            }.onFailure { plugin.logger.warning("StatsManager.getTop() error: ${it.message}") }

            Bukkit.getScheduler().runTask(plugin, Runnable { callback(results) })
        })
    }

    // ─── Internos ─────────────────────────────────────────────────────────────
    private fun update(uuid: UUID, block: PlayerStats.() -> Unit) {
        cache[uuid]?.apply { block(); dirty = true }
    }

    private fun saveAllAsync() {
        val dirty = cache.values.filter { it.dirty }.toList()
        if (dirty.isEmpty()) return
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            dirty.forEach { saveToDB(it) }
        })
    }

    private fun saveToDB(stats: PlayerStats) {
        val columns = listOf(
            "uuid", "player_name",
            "roulette_bets", "roulette_wagered", "roulette_won", "roulette_wins", "roulette_losses", "roulette_daily_uses",
            "slots_spins", "slots_wagered", "slots_won", "slots_jackpots", "slots_daily_uses",
            "bj_games", "bj_wagered", "bj_won", "bj_wins", "bj_losses", "bj_blackjacks", "bj_daily_uses",
            "scratch_used", "scratch_spent", "scratch_won", "scratch_wins", "scratch_daily_uses",
            "lottery_tickets", "lottery_spent", "lottery_won", "lottery_wins", "lottery_daily_uses",
            "coinflip_flips", "coinflip_wagered", "coinflip_won", "coinflip_wins", "coinflip_losses", "coinflip_daily_uses",
            "racing_wagered", "racing_won", "racing_wins", "racing_losses", "racing_daily_uses",
            "rps_daily_uses", "ttt_daily_uses", "poker_daily_uses", "last_daily_reset",
            "last_seen"
        )
        val sql = plugin.db.buildUpsertSql(columns)
        val params = listOf(
            stats.uuid.toString(), stats.name,
            stats.rouletteBets, stats.rouletteWagered, stats.rouletteWon, stats.rouletteWins, stats.rouletteLosses, stats.rouletteDailyUses,
            stats.slotsSpins, stats.slotsWagered, stats.slotsWon, stats.slotsJackpots, stats.slotsDailyUses,
            stats.bjGames, stats.bjWagered, stats.bjWon, stats.bjWins, stats.bjLosses, stats.bjBlackjacks, stats.bjDailyUses,
            stats.scratchUsed, stats.scratchSpent, stats.scratchWon, stats.scratchWins, stats.scratchDailyUses,
            stats.lotteryTickets, stats.lotterySpent, stats.lotteryWon, stats.lotteryWins, stats.lotteryDailyUses,
            stats.coinFlipFlips, stats.coinFlipWagered, stats.coinFlipWon, stats.coinFlipWins, stats.coinFlipLosses, stats.coinflipDailyUses,
            stats.racingWagered, stats.racingWon, stats.racingWins, stats.racingLosses, stats.racingDailyUses,
            stats.rpsDailyUses, stats.tttDailyUses, stats.pokerDailyUses, stats.lastDailyReset,
            System.currentTimeMillis()
        )
        runCatching { plugin.db.upsert(sql, params); stats.dirty = false }
            .onFailure { plugin.logger.warning("Stats save error for ${stats.name}: ${it.message}") }
    }

    private fun loadFromDB(uuid: UUID, name: String): PlayerStats {
        val sql = "SELECT * FROM casino_stats WHERE uuid = ?"
        runCatching {
            plugin.db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, uuid.toString())
                    val rs = ps.executeQuery()
                    if (rs.next()) return fromRS(uuid, rs)
                }
            }
        }.onFailure { plugin.logger.warning("Stats load error for $name: ${it.message}") }
        return PlayerStats(uuid, name)
    }

    private fun fromRS(uuid: UUID, rs: ResultSet) = PlayerStats(
        uuid            = uuid,
        name            = rs.getString("player_name"),
        rouletteBets    = rs.getInt("roulette_bets"),
        rouletteWagered = rs.getDouble("roulette_wagered"),
        rouletteWon     = rs.getDouble("roulette_won"),
        rouletteWins    = rs.getInt("roulette_wins"),
        rouletteLosses  = rs.getInt("roulette_losses"),
        rouletteDailyUses = safeInt(rs, "roulette_daily_uses"),
        slotsSpins      = rs.getInt("slots_spins"),
        slotsWagered    = rs.getDouble("slots_wagered"),
        slotsWon        = rs.getDouble("slots_won"),
        slotsJackpots   = rs.getInt("slots_jackpots"),
        slotsDailyUses  = safeInt(rs, "slots_daily_uses"),
        bjGames         = rs.getInt("bj_games"),
        bjWagered       = rs.getDouble("bj_wagered"),
        bjWon           = rs.getDouble("bj_won"),
        bjWins          = rs.getInt("bj_wins"),
        bjLosses        = rs.getInt("bj_losses"),
        bjBlackjacks    = rs.getInt("bj_blackjacks"),
        bjDailyUses     = safeInt(rs, "bj_daily_uses"),
        scratchUsed     = rs.getInt("scratch_used"),
        scratchSpent    = rs.getDouble("scratch_spent"),
        scratchWon      = rs.getDouble("scratch_won"),
        scratchWins     = rs.getInt("scratch_wins"),
        scratchDailyUses = safeInt(rs, "scratch_daily_uses"),
        lotteryTickets  = safeInt(rs, "lottery_tickets"),
        lotterySpent    = safeDouble(rs, "lottery_spent"),
        lotteryWon      = safeDouble(rs, "lottery_won"),
        lotteryWins     = safeInt(rs, "lottery_wins"),
        lotteryDailyUses = safeInt(rs, "lottery_daily_uses"),
        coinFlipFlips   = safeInt(rs, "coinflip_flips"),
        coinFlipWagered = safeDouble(rs, "coinflip_wagered"),
        coinFlipWon     = safeDouble(rs, "coinflip_won"),
        coinFlipWins    = safeInt(rs, "coinflip_wins"),
        coinFlipLosses  = safeInt(rs, "coinflip_losses"),
        coinflipDailyUses = safeInt(rs, "coinflip_daily_uses"),
        racingWagered   = safeDouble(rs, "racing_wagered"),
        racingWon       = safeDouble(rs, "racing_won"),
        racingWins      = safeInt(rs, "racing_wins"),
        racingLosses    = safeInt(rs, "racing_losses"),
        racingDailyUses = safeInt(rs, "racing_daily_uses"),
        rpsDailyUses    = safeInt(rs, "rps_daily_uses"),
        tttDailyUses    = safeInt(rs, "ttt_daily_uses"),
        pokerDailyUses  = safeInt(rs, "poker_daily_uses"),
        lastDailyReset  = runCatching { rs.getLong("last_daily_reset") }.getOrDefault(System.currentTimeMillis()),
        lastSeen        = rs.getLong("last_seen"),
        dirty           = false
    )

    // Safe getters para columnas que pueden no existir aún en DB antigua
    private fun safeInt(rs: ResultSet, col: String): Int = runCatching { rs.getInt(col) }.getOrDefault(0)
    private fun safeDouble(rs: ResultSet, col: String): Double = runCatching { rs.getDouble(col) }.getOrDefault(0.0)
}
