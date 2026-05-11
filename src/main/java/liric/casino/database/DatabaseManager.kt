package liric.casino.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import liric.casino.CasinoPlugin
import java.io.File
import java.sql.Connection

class DatabaseManager(private val plugin: CasinoPlugin) {

    private lateinit var dataSource: HikariDataSource

    val isSQLite: Boolean
        get() = plugin.config.getString("database.type", "sqlite").equals("sqlite", true)

    fun connect() {
        val cfg = HikariConfig()
        cfg.poolName = "CasinoLiric-Pool"
        cfg.connectionTimeout = 30_000
        cfg.idleTimeout = 600_000
        cfg.maxLifetime = 1_800_000

        if (isSQLite) {
            val file = File(plugin.dataFolder, "casino_stats.db")
            cfg.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
            cfg.driverClassName = "org.sqlite.JDBC"
            cfg.maximumPoolSize = 1
            cfg.connectionTestQuery = "SELECT 1"
            // SQLite: activar WAL para mayor rendimiento
            cfg.connectionInitSql = "PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;"
        } else {
            val host = plugin.config.getString("database.host", "localhost")
            val port = plugin.config.getInt("database.port", 3306)
            val db   = plugin.config.getString("database.database", "casino")
            val user = plugin.config.getString("database.username", "root")
            val pass = plugin.config.getString("database.password", "")
            cfg.jdbcUrl        = "jdbc:mariadb://$host:$port/$db?autoReconnect=true&useSSL=false"
            cfg.username       = user
            cfg.password       = pass
            cfg.maximumPoolSize = plugin.config.getInt("database.pool-size", 5)
            cfg.minimumIdle    = 2
        }

        dataSource = HikariDataSource(cfg)
        createTables()
    }

    fun disconnect() {
        if (::dataSource.isInitialized && !dataSource.isClosed) dataSource.close()
    }

    fun getConnection(): Connection = dataSource.connection

    // ─── DDL ─────────────────────────────────────────────────────────────────
    private fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS casino_stats (
                        uuid              VARCHAR(36)  NOT NULL,
                        player_name       VARCHAR(16)  NOT NULL,
                        roulette_bets     INT          DEFAULT 0,
                        roulette_wagered  DOUBLE       DEFAULT 0,
                        roulette_won      DOUBLE       DEFAULT 0,
                        roulette_wins     INT          DEFAULT 0,
                        roulette_losses   INT          DEFAULT 0,
                        slots_spins       INT          DEFAULT 0,
                        slots_wagered     DOUBLE       DEFAULT 0,
                        slots_won         DOUBLE       DEFAULT 0,
                        slots_jackpots    INT          DEFAULT 0,
                        bj_games          INT          DEFAULT 0,
                        bj_wagered        DOUBLE       DEFAULT 0,
                        bj_won            DOUBLE       DEFAULT 0,
                        bj_wins           INT          DEFAULT 0,
                        bj_losses         INT          DEFAULT 0,
                        bj_blackjacks     INT          DEFAULT 0,
                        scratch_used      INT          DEFAULT 0,
                        scratch_spent     DOUBLE       DEFAULT 0,
                        scratch_won       DOUBLE       DEFAULT 0,
                        scratch_wins      INT          DEFAULT 0,
                        last_seen         BIGINT       DEFAULT 0,
                        PRIMARY KEY (uuid)
                    )
                """.trimIndent())
            }
        }
    }

    // ─── UPSERT (compatible SQLite e MariaDB) ────────────────────────────────
    fun upsert(sql: String, params: List<Any?>) {
        getConnection().use { conn ->
            conn.prepareStatement(sql).use { ps ->
                params.forEachIndexed { i, v ->
                    when (v) {
                        is String -> ps.setString(i + 1, v)
                        is Int    -> ps.setInt(i + 1, v)
                        is Double -> ps.setDouble(i + 1, v)
                        is Long   -> ps.setLong(i + 1, v)
                        null      -> ps.setNull(i + 1, java.sql.Types.NULL)
                        else      -> ps.setObject(i + 1, v)
                    }
                }
                ps.executeUpdate()
            }
        }
    }

    fun buildUpsertSql(columns: List<String>): String {
        val cols  = columns.joinToString(", ")
        val marks = columns.joinToString(", ") { "?" }
        return if (isSQLite) {
            val updates = columns.drop(1).joinToString(", ") { "$it = excluded.$it" }
            "INSERT INTO casino_stats ($cols) VALUES ($marks) ON CONFLICT(uuid) DO UPDATE SET $updates"
        } else {
            val updates = columns.drop(1).joinToString(", ") { "$it = VALUES($it)" }
            "INSERT INTO casino_stats ($cols) VALUES ($marks) ON DUPLICATE KEY UPDATE $updates"
        }
    }
}
