package liric.casino.games.coinflip

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractMatchmakingManager
import liric.casino.util.ValidationUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CoinFlipManager(plugin: CasinoPlugin) : AbstractMatchmakingManager<CoinFlipSession>(plugin, "coinflip") {

    private val pendingChat = ConcurrentHashMap.newKeySet<UUID>()

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    fun setPendingChat(uuid: UUID)    { pendingChat.add(uuid) }
    fun removePendingChat(uuid: UUID) { pendingChat.remove(uuid) }
    fun hasPendingChat(uuid: UUID)    = pendingChat.contains(uuid)

    override fun createGame(player: Player, amount: Double): CoinFlipSession? {
        if (!canPlay(player, amount)) return null
        if (playerSession.containsKey(player.uniqueId)) {
            player.sendMessage(msg("coinflip.already-in-game")); return null
        }
        if (!takeBet(player, amount)) return null

        plugin.statsManager.recordGameUse(player.uniqueId, "coinflip")
        val session = CoinFlipSession(creatorId = player.uniqueId, creatorName = player.name, betAmount = amount)
        addSession(session.id, session)
        playerSession[player.uniqueId] = session.id

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

    override fun joinGame(joiner: Player, creatorName: String): Boolean {
        val session = activeSessions.values.firstOrNull {
            it.isOpen() &&
            it.creatorName.equals(creatorName, ignoreCase = true) &&
            it.creatorId != joiner.uniqueId
        } ?: run { joiner.sendMessage(msg("coinflip.game-not-found", "player" to creatorName)); return false }

        if (!canPlay(joiner, session.betAmount)) return false
        if (playerSession.containsKey(joiner.uniqueId)) {
            joiner.sendMessage(msg("coinflip.already-in-game")); return false
        }
        if (!takeBet(joiner, session.betAmount)) return false

        plugin.statsManager.recordGameUse(joiner.uniqueId, "coinflip")
        session.joinerId   = joiner.uniqueId
        session.joinerName = joiner.name
        session.state      = CoinFlipState.ANIMATING
        playerSession[joiner.uniqueId] = session.id

        val creator = Bukkit.getPlayer(session.creatorId)
        CoinFlipMenu(plugin, session, creator, joiner).startAnimation()
        return true
    }

    override fun cancelGame(player: Player): Boolean {
        val session = getPlayerSession(player.uniqueId)
            ?: run { player.sendMessage(msg("coinflip.no-game")); return false }

        if (!session.isOpen()) {
            player.sendMessage(msg("coinflip.cannot-cancel")); return false
        }
        plugin.economyManager.depositPlayer(player, session.betAmount)
        removeSession(session)
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 0.8f)
        player.sendMessage(msg("coinflip.cancelled", "amount" to CoinFlipMenu.formatAmount(session.betAmount)))
        return true
    }

    fun resolveGame(session: CoinFlipSession, winnerId: UUID) {
        val loserId = if (winnerId == session.creatorId) session.joinerId!! else session.creatorId
        val winner  = Bukkit.getPlayer(winnerId)
        val loser   = Bukkit.getPlayer(loserId)

        if (winner != null && winner.isOnline) {
            processWin(winner, session.betAmount)
            winner.playSound(winner.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        } else {
            // Process offline win manually or allow processWin to handle it?
            // Since processWin takes a Player, we must manually deposit if offline.
            val totalPot = session.betAmount * 2
            val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, totalPot, "coinflip")
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerId), netWin)
        }
        loser?.sendMessage(msg("coinflip.lose", "amount" to CoinFlipMenu.formatAmount(session.betAmount)))

        plugin.statsManager.recordCoinFlip(session.creatorId, session.betAmount,
            if (winnerId == session.creatorId) session.betAmount * 2 else 0.0, winnerId == session.creatorId)
        plugin.statsManager.recordCoinFlip(loserId, session.betAmount, 0.0, false)

        val winnerName = winner?.name ?: Bukkit.getOfflinePlayer(winnerId).name ?: "?"
        val loserName  = loser?.name  ?: Bukkit.getOfflinePlayer(loserId).name  ?: "?"
        plugin.server.broadcast(plugin.messages.get("coinflip.broadcast-result",
            "winner" to winnerName,
            "loser"  to loserName,
            "amount" to CoinFlipMenu.formatAmount(session.betAmount * 2) // Roughly net win
        ))

        session.state = CoinFlipState.FINISHED
        removeSession(session)
    }

    override fun cleanupAll() {
        super.cleanupAll()
        pendingChat.clear()
    }
}
