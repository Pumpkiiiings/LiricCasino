package liric.casino.games.coinflip

import liric.casino.CasinoPlugin
import liric.casino.util.TaxUtil
import liric.casino.util.ValidationUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CoinFlipManager(private val plugin: CasinoPlugin) {

    private val sessions      = ConcurrentHashMap<UUID, CoinFlipSession>()
    private val playerSession = ConcurrentHashMap<UUID, UUID>()

    private val pendingChat   = ConcurrentHashMap.newKeySet<UUID>()

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)
    private fun minBet() = plugin.config.getDouble("coinflip.bet.min", 100.0)
    private fun maxBet() = plugin.config.getDouble("coinflip.bet.max", 500000.0)


    fun setPendingChat(uuid: UUID)    { pendingChat.add(uuid) }
    fun removePendingChat(uuid: UUID) { pendingChat.remove(uuid) }
    fun hasPendingChat(uuid: UUID)    = pendingChat.contains(uuid)


    fun createGame(player: Player, amount: Double): CoinFlipSession? {
        if (!ValidationUtil.canPlayDaily(plugin, player, "coinflip")) return null
        if (!ValidationUtil.validateBet(plugin, player, "coinflip", amount)) return null
        if (playerSession.containsKey(player.uniqueId)) {
            player.sendMessage(msg("coinflip.already-in-game")); return null
        }
        if (!plugin.economyManager.withdrawPlayer(player, amount).transactionSuccess()) {
            player.sendMessage(msg("coinflip.no-funds")); return null
        }

        plugin.statsManager.recordGameUse(player.uniqueId, "coinflip")
        val session = CoinFlipSession(creatorId = player.uniqueId, creatorName = player.name, betAmount = amount)
        sessions[session.id]             = session
        playerSession[player.uniqueId]   = session.id

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        player.sendMessage(msg("coinflip.created",
            "amount" to CoinFlipMenu.formatAmount(amount),
            "id"     to session.id.toString().take(8)))


        plugin.server.onlinePlayers.forEach { p ->
            if (p.uniqueId != player.uniqueId)
                p.sendMessage(msg("coinflip.new-game-broadcast",
                    "player" to player.name,
                    "amount" to CoinFlipMenu.formatAmount(amount)))
        }
        return session
    }


    fun joinGame(joiner: Player, creatorName: String): Boolean {
        val session = sessions.values.firstOrNull {
            it.state == CoinFlipState.WAITING &&
            it.creatorName.equals(creatorName, ignoreCase = true) &&
            it.creatorId != joiner.uniqueId
        } ?: run { joiner.sendMessage(msg("coinflip.game-not-found", "player" to creatorName)); return false }

        if (!ValidationUtil.canPlayDaily(plugin, joiner, "coinflip")) return false
        if (!ValidationUtil.validateBet(plugin, joiner, "coinflip", session.betAmount)) return false

        if (playerSession.containsKey(joiner.uniqueId)) {
            joiner.sendMessage(msg("coinflip.already-in-game")); return false
        }

        if (!plugin.economyManager.withdrawPlayer(joiner, session.betAmount).transactionSuccess()) {
            joiner.sendMessage(msg("coinflip.no-funds")); return false
        }

        plugin.statsManager.recordGameUse(joiner.uniqueId, "coinflip")
        session.joinerId   = joiner.uniqueId
        session.joinerName = joiner.name
        session.state      = CoinFlipState.ANIMATING
        playerSession[joiner.uniqueId] = session.id

        val creator = Bukkit.getPlayer(session.creatorId)
        CoinFlipMenu(plugin, session, creator, joiner).startAnimation()
        return true
    }


    fun cancelGame(player: Player): Boolean {
        val sessionId = playerSession[player.uniqueId]
            ?: run { player.sendMessage(msg("coinflip.no-game")); return false }
        val session = sessions[sessionId] ?: return false

        if (session.state != CoinFlipState.WAITING) {
            player.sendMessage(msg("coinflip.cannot-cancel")); return false
        }
        plugin.economyManager.depositPlayer(player, session.betAmount)
        removeSession(session)
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 0.8f)
        player.sendMessage(msg("coinflip.cancelled", "amount" to CoinFlipMenu.formatAmount(session.betAmount)))
        return true
    }


    fun resolveGame(session: CoinFlipSession, winnerId: UUID) {
        val totalPot        = session.betAmount * 2
        val (netWin, tax)   = TaxUtil.applyTax(plugin, totalPot, "coinflip")
        val loserId         = if (winnerId == session.creatorId) session.joinerId!! else session.creatorId
        val winner          = Bukkit.getPlayer(winnerId)
        val loser           = Bukkit.getPlayer(loserId)

        if (winner != null && winner.isOnline) {
            plugin.economyManager.depositPlayer(winner, netWin)
            winner.sendMessage(msg("coinflip.win",
                "amount" to CoinFlipMenu.formatAmount(netWin),
                "tax"    to TaxUtil.taxMessage(plugin, tax)))
            winner.playSound(winner.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        } else {
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerId), netWin)
        }
        loser?.sendMessage(msg("coinflip.lose", "amount" to CoinFlipMenu.formatAmount(session.betAmount)))


        plugin.statsManager.recordCoinFlip(session.creatorId, session.betAmount,
            if (winnerId == session.creatorId) netWin else 0.0, winnerId == session.creatorId)
        plugin.statsManager.recordCoinFlip(loserId, session.betAmount, 0.0, false)


        val winnerName = winner?.name ?: Bukkit.getOfflinePlayer(winnerId).name ?: "?"
        val loserName  = loser?.name  ?: Bukkit.getOfflinePlayer(loserId).name  ?: "?"
        plugin.server.broadcast(plugin.messages.get("coinflip.broadcast-result",
            "winner" to winnerName,
            "loser"  to loserName,
            "amount" to CoinFlipMenu.formatAmount(netWin)
        ))

        session.state = CoinFlipState.FINISHED
        removeSession(session)
    }

    fun getOpenGames(): List<CoinFlipSession> =
        sessions.values.filter { it.state == CoinFlipState.WAITING }.sortedByDescending { it.betAmount }

    fun getPlayerSession(uuid: UUID): CoinFlipSession? = playerSession[uuid]?.let { sessions[it] }

    fun removeSession(session: CoinFlipSession) {
        sessions.remove(session.id)
        playerSession.remove(session.creatorId)
        session.joinerId?.let { playerSession.remove(it) }
    }

    fun handleDisconnect(playerId: UUID) {
        val sessionId = playerSession[playerId] ?: return
        val session = sessions[sessionId] ?: return

        if (session.state == CoinFlipState.WAITING) {
            if (session.creatorId == playerId) {
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(playerId), session.betAmount)
                removeSession(session)
            }
        }

    }

    fun cleanupAll() {
        sessions.values.forEach { session ->
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), session.betAmount)
            session.joinerId?.let { plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it), session.betAmount) }
        }
        sessions.clear()
        playerSession.clear()
        pendingChat.clear()
    }
}
