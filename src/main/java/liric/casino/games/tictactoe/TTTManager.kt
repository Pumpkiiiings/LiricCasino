package liric.casino.games.tictactoe

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractMatchmakingManager
import liric.casino.util.SchedulerUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

class TTTManager(plugin: CasinoPlugin) : AbstractMatchmakingManager<TTTSession>(plugin, "ttt") {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun createGame(player: Player, amount: Double): TTTSession? {
        if (!canPlay(player, amount)) return null
        if (playerSession.containsKey(player.uniqueId)) {
            player.sendMessage(msg("ttt.already-in-game")); return null
        }
        if (!takeBet(player, amount)) return null

        plugin.statsManager.recordGameUse(player.uniqueId, "ttt")
        val session = TTTSession(creatorId = player.uniqueId, creatorName = player.name, betAmount = amount)
        addSession(session.id, session)
        playerSession[player.uniqueId] = session.id

        player.sendMessage(msg("ttt.created", "amount" to amount.toLong().toString()))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

        plugin.server.onlinePlayers.filter { it.uniqueId != player.uniqueId }.forEach { p ->
            p.sendMessage(msg("ttt.broadcast-new", "player" to player.name, "amount" to amount.toLong().toString()))
        }
        return session
    }

    override fun joinGame(joiner: Player, creatorName: String): Boolean {
        val session = activeSessions.values.firstOrNull {
            it.isOpen() &&
            it.creatorName.equals(creatorName, ignoreCase = true) &&
            it.creatorId != joiner.uniqueId
        } ?: run { joiner.sendMessage(msg("ttt.game-not-found", "player" to creatorName)); return false }

        if (!canPlay(joiner, session.betAmount)) return false
        if (playerSession.containsKey(joiner.uniqueId)) {
            joiner.sendMessage(msg("ttt.already-in-game")); return false
        }
        if (!takeBet(joiner, session.betAmount)) return false

        plugin.statsManager.recordGameUse(joiner.uniqueId, "ttt")
        session.joinerId   = joiner.uniqueId
        session.joinerName = joiner.name
        session.state      = TTTState.PLAYING
        playerSession[joiner.uniqueId] = session.id

        val creator = Bukkit.getPlayer(session.creatorId)
        joiner.sendMessage(msg("ttt.joined", "player" to session.creatorName))
        creator?.sendMessage(msg("ttt.opponent-joined", "player" to joiner.name))

        SchedulerUtil.runGlobalLater(plugin, 5L) {
            TTTBoard(plugin, session, joiner).open()
            creator?.let { TTTBoard(plugin, session, it).open() }
        }
        return true
    }

    fun playMove(session: TTTSession, playerId: UUID, cellIndex: Int) {
        if (session.state != TTTState.PLAYING) return
        if (session.currentTurn != playerId) return
        if (session.board[cellIndex] != TTTMark.NONE) return

        session.board[cellIndex] = session.getMarkOf(playerId)
        val sound = if (session.board[cellIndex] == TTTMark.X) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BELL

        Bukkit.getPlayer(playerId)?.playSound(Bukkit.getPlayer(playerId)!!.location, sound, 1f, 1.5f)
        val otherId = if (playerId == session.creatorId) session.joinerId else session.creatorId
        otherId?.let { Bukkit.getPlayer(it)?.playSound(Bukkit.getPlayer(it)!!.location, sound, 1f, 1.5f) }

        val winner = session.checkWinner()
        when {
            winner != TTTMark.NONE -> finishGame(session, winnerId = if (winner == session.creatorMark) session.creatorId else session.joinerId!!)
            session.isFull()       -> finishGame(session, winnerId = null)
            else -> {
                session.currentTurn = otherId ?: session.creatorId
                updateBothBoards(session)
            }
        }
    }

    private fun updateBothBoards(session: TTTSession) {
        Bukkit.getPlayer(session.creatorId)?.let { TTTBoard(plugin, session, it).update() }
        session.joinerId?.let { Bukkit.getPlayer(it)?.let { p -> TTTBoard(plugin, session, p).update() } }
    }

    private fun finishGame(session: TTTSession, winnerId: UUID?) {
        session.state = TTTState.FINISHED
        val creator = Bukkit.getPlayer(session.creatorId)
        val joiner  = session.joinerId?.let { Bukkit.getPlayer(it) }

        creator?.closeInventory()
        joiner?.closeInventory()

        if (winnerId == null) {
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), session.betAmount)
            session.joinerId?.let { plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it), session.betAmount) }
            creator?.sendMessage(msg("ttt.tie"))
            joiner?.sendMessage(msg("ttt.tie"))
            creator?.playSound(creator.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
            joiner?.playSound(joiner.location,  Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
        } else {
            val loserId  = if (winnerId == session.creatorId) session.joinerId!! else session.creatorId
            val winnerPlayer = Bukkit.getPlayer(winnerId)
            val loserPlayer  = Bukkit.getPlayer(loserId)

            if (winnerPlayer != null) {
                processWin(winnerPlayer, session.betAmount)
                winnerPlayer.playSound(winnerPlayer.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            } else {
                val totalPot = session.betAmount * 2
                val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, totalPot, "ttt")
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerId), netWin)
            }
            
            loserPlayer?.sendMessage(msg("ttt.lose",  "amount" to session.betAmount.toLong().toString()))
            loserPlayer?.playSound(loserPlayer.location,   Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)

            val winnerName = winnerPlayer?.name ?: Bukkit.getOfflinePlayer(winnerId).name ?: "?"
            val loserName  = loserPlayer?.name  ?: Bukkit.getOfflinePlayer(loserId).name  ?: "?"
            plugin.server.broadcast(msg("ttt.broadcast-result",
                "winner" to winnerName, "loser" to loserName,
                "amount" to (session.betAmount * 2).toLong().toString())) // Rough net win for broadcast
        }
        removeSession(session)
    }

    override fun cancelGame(player: Player): Boolean {
        val session = getPlayerSession(player.uniqueId)
            ?: run { player.sendMessage(msg("ttt.no-game")); return false }
        if (!session.isOpen()) { player.sendMessage(msg("ttt.cannot-cancel")); return false }
        plugin.economyManager.depositPlayer(player, session.betAmount)
        removeSession(session)
        player.sendMessage(msg("ttt.cancelled", "amount" to session.betAmount.toLong().toString()))
        return true
    }

    override fun handleDisconnect(playerId: UUID) {
        val session = getPlayerSession(playerId) ?: return
        if (session.isOpen() && session.creatorId == playerId) {
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(playerId), session.betAmount)
            removeSession(session)
        } else if (session.state == TTTState.PLAYING) {
            val isCreator = playerId == session.creatorId
            val winnerId = if (isCreator) session.joinerId!! else session.creatorId
            
            val winner = Bukkit.getPlayer(winnerId)
            if (winner != null) {
                processWin(winner, session.betAmount)
                val msg = plugin.messagesConfig.getString("ttt-extra.opponent-disconnected", "<red>Your opponent disconnected. <green>You won the bet automatically!")!!
                winner.sendMessage(plugin.format(msg.replace("{prefix}", plugin.messagesConfig.getString("prefix", "")!!)))
                winner.playSound(winner.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                winner.closeInventory()
            } else {
                val totalPot = session.betAmount * 2
                val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, totalPot, "ttt")
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerId), netWin)
            }
            removeSession(session)
        }
    }
}
