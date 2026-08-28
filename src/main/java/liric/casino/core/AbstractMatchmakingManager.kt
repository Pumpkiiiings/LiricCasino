package liric.casino.core

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractMatchmakingManager<S : MatchmakingSession>(
    plugin: CasinoPlugin,
    gameId: String
) : AbstractGameManager<S>(plugin, gameId) {

    protected val playerSession = ConcurrentHashMap<UUID, UUID>()

    fun getPlayerSession(uuid: UUID): S? = playerSession[uuid]?.let { activeSessions[it] }

    fun getOpenGames(): List<S> = activeSessions.values.filter { it.isOpen() }.sortedByDescending { it.betAmount }

    abstract fun createGame(player: Player, amount: Double): S?
    abstract fun joinGame(joiner: Player, creatorName: String): Boolean
    abstract fun cancelGame(player: Player): Boolean

    open fun removeSession(session: S) {
        activeSessions.remove(session.id)
        playerSession.remove(session.creatorId)
        session.joinerId?.let { playerSession.remove(it) }
    }

    open fun handleDisconnect(playerId: UUID) {
        val session = getPlayerSession(playerId) ?: return
        if (session.isOpen() && session.creatorId == playerId) {
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(playerId), session.betAmount)
            removeSession(session)
        }
    }

    open fun cleanupAll() {
        activeSessions.values.forEach { session ->
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), session.betAmount)
            session.joinerId?.let { plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it), session.betAmount) }
        }
        activeSessions.clear()
        playerSession.clear()
    }
}

interface MatchmakingSession {
    val id: UUID
    val creatorId: UUID
    val creatorName: String
    var joinerId: UUID?
    var joinerName: String?
    val betAmount: Double
    fun isOpen(): Boolean
}
