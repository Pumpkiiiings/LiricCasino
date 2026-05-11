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
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L) // cada 5 min
    }

    fun shutdown() {
        // Guardar sincrónicamente al apagar
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

    /** Devuelve stats del caché o crea uno en blanco (carga async). */
    fun getOrCreate(uuid: UUID, name: String): PlayerStats =
        cache.getOrPut(uuid) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val loaded = loadFromDB(uuid, name)
                cache[uuid] = loaded
            })
            PlayerStats(uuid, name)
        }

    // ─── Registros (llamados desde los módulos del casino) ───────────────────

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

    // ─── Top leaderboard ─────────────────────────────────────────────────────
    enum class TopMode(val sqlColumn: String, val label: String) {
        GLOBAL("(roulette_won + slots_won + bj_won + scratch_won)", "Global"),
        RULETA("roulette_won", "Ruleta"),
        SLOTS("slots_won", "Tragamonedas"),
        BLACKJACK("bj_won", "Blackjack"),
        SCRATCH("scratch_won", "Rasca y Gana")
    }

    /** Devuelve el top 10 de forma asíncrona y llama al callback en el hilo principal. */
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
            "roulette_bets", "roulette_wagered", "roulette_won", "roulette_wins", "roulette_losses",
            "slots_spins", "slots_wagered", "slots_won", "slots_jackpots",
            "bj_games", "bj_wagered", "bj_won", "bj_wins", "bj_losses", "bj_blackjacks",
            "scratch_used", "scratch_spent", "scratch_won", "scratch_wins",
            "last_seen"
        )
        val sql = plugin.db.buildUpsertSql(columns)
        val params = listOf(
            stats.uuid.toString(), stats.playerName,
            stats.rouletteBets, stats.rouletteWagered, stats.rouletteWon, stats.rouletteWins, stats.rouletteLosses,
            stats.slotsSpins, stats.slotsWagered, stats.slotsWon, stats.slotsJackpots,
            stats.bjGames, stats.bjWagered, stats.bjWon, stats.bjWins, stats.bjLosses, stats.bjBlackjacks,
            stats.scratchUsed, stats.scratchSpent, stats.scratchWon, stats.scratchWins,
            System.currentTimeMillis()
        )
        runCatching { plugin.db.upsert(sql, params); stats.dirty = false }
            .onFailure { plugin.logger.warning("Stats save error for ${stats.playerName}: ${it.message}") }
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
        uuid           = uuid,
        playerName     = rs.getString("player_name"),
        rouletteBets   = rs.getInt("roulette_bets"),
        rouletteWagered = rs.getDouble("roulette_wagered"),
        rouletteWon    = rs.getDouble("roulette_won"),
        rouletteWins   = rs.getInt("roulette_wins"),
        rouletteLosses = rs.getInt("roulette_losses"),
        slotsSpins     = rs.getInt("slots_spins"),
        slotsWagered   = rs.getDouble("slots_wagered"),
        slotsWon       = rs.getDouble("slots_won"),
        slotsJackpots  = rs.getInt("slots_jackpots"),
        bjGames        = rs.getInt("bj_games"),
        bjWagered      = rs.getDouble("bj_wagered"),
        bjWon          = rs.getDouble("bj_won"),
        bjWins         = rs.getInt("bj_wins"),
        bjLosses       = rs.getInt("bj_losses"),
        bjBlackjacks   = rs.getInt("bj_blackjacks"),
        scratchUsed    = rs.getInt("scratch_used"),
        scratchSpent   = rs.getDouble("scratch_spent"),
        scratchWon     = rs.getDouble("scratch_won"),
        scratchWins    = rs.getInt("scratch_wins"),
        lastSeen       = rs.getLong("last_seen"),
        dirty          = false
    )
}
