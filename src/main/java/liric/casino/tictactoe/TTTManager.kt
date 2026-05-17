package liric.casino.tictactoe

import liric.casino.CasinoPlugin
import liric.casino.util.TaxUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TTTManager(private val plugin: CasinoPlugin) {

    private val sessions      = ConcurrentHashMap<UUID, TTTSession>()
    private val playerSession = ConcurrentHashMap<UUID, UUID>()

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)
    private fun minBet() = plugin.config.getDouble("ttt.bet.min", 100.0)
    private fun maxBet() = plugin.config.getDouble("ttt.bet.max", 500000.0)

    // ── Crear ─────────────────────────────────────────────────────────────────
    fun createGame(player: Player, amount: Double) {
        if (playerSession.containsKey(player.uniqueId)) {
            player.sendMessage(msg("ttt.already-in-game")); return
        }
        if (amount < minBet() || amount > maxBet()) {
            player.sendMessage(msg("ttt.invalid-amount",
                "min" to minBet().toLong().toString(),
                "max" to maxBet().toLong().toString())); return
        }
        if (!plugin.economyManager.withdrawPlayer(player, amount).transactionSuccess()) {
            player.sendMessage(msg("ttt.no-funds")); return
        }
        val session = TTTSession(creatorId = player.uniqueId, creatorName = player.name, betAmount = amount)
        sessions[session.id] = session
        playerSession[player.uniqueId] = session.id

        player.sendMessage(msg("ttt.created", "amount" to amount.toLong().toString()))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

        plugin.server.onlinePlayers.filter { it.uniqueId != player.uniqueId }.forEach { p ->
            p.sendMessage(msg("ttt.broadcast-new", "player" to player.name, "amount" to amount.toLong().toString()))
        }
    }

    // ── Unirse ────────────────────────────────────────────────────────────────
    fun joinGame(joiner: Player, creatorName: String) {
        if (playerSession.containsKey(joiner.uniqueId)) {
            joiner.sendMessage(msg("ttt.already-in-game")); return
        }
        val session = sessions.values.firstOrNull {
            it.state == TTTState.WAITING &&
            it.creatorName.equals(creatorName, ignoreCase = true) &&
            it.creatorId != joiner.uniqueId
        } ?: run { joiner.sendMessage(msg("ttt.game-not-found", "player" to creatorName)); return }

        if (!plugin.economyManager.withdrawPlayer(joiner, session.betAmount).transactionSuccess()) {
            joiner.sendMessage(msg("ttt.no-funds")); return
        }

        session.joinerId   = joiner.uniqueId
        session.joinerName = joiner.name
        session.state      = TTTState.PLAYING
        playerSession[joiner.uniqueId] = session.id

        val creator = Bukkit.getPlayer(session.creatorId)
        joiner.sendMessage(msg("ttt.joined", "player" to session.creatorName))
        creator?.sendMessage(msg("ttt.opponent-joined", "player" to joiner.name))

        // Abrir tablero a ambos (delay para que lean el mensaje)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            TTTBoard(plugin, session, joiner).open()
            creator?.let { TTTBoard(plugin, session, it).open() }
        }, 5L)
    }

    // ── Jugada ────────────────────────────────────────────────────────────────
    fun playMove(session: TTTSession, playerId: UUID, cellIndex: Int) {
        if (session.state != TTTState.PLAYING) return
        if (session.currentTurn != playerId) return
        if (session.board[cellIndex] != TTTMark.NONE) return

        session.board[cellIndex] = session.getMarkOf(playerId)
        val sound = if (session.board[cellIndex] == TTTMark.X) Sound.BLOCK_NOTE_BLOCK_PLING else Sound.BLOCK_NOTE_BLOCK_BELL

        // Sonido y actualizar ambas vistas
        Bukkit.getPlayer(playerId)?.playSound(Bukkit.getPlayer(playerId)!!.location, sound, 1f, 1.5f)
        val otherId = if (playerId == session.creatorId) session.joinerId else session.creatorId
        otherId?.let { Bukkit.getPlayer(it)?.playSound(Bukkit.getPlayer(it)!!.location, sound, 1f, 1.5f) }

        val winner = session.checkWinner()
        when {
            winner != TTTMark.NONE -> finishGame(session, winnerId = if (winner == session.creatorMark) session.creatorId else session.joinerId!!)
            session.isFull()       -> finishGame(session, winnerId = null) // empate
            else -> {
                // Cambiar turno y actualizar GUIs
                session.currentTurn = otherId ?: session.creatorId
                updateBothBoards(session)
            }
        }
    }

    private fun updateBothBoards(session: TTTSession) {
        Bukkit.getPlayer(session.creatorId)?.let { TTTBoard(plugin, session, it).update() }
        session.joinerId?.let { Bukkit.getPlayer(it)?.let { p -> TTTBoard(plugin, session, p).update() } }
    }

    // ── Resultado ─────────────────────────────────────────────────────────────
    private fun finishGame(session: TTTSession, winnerId: UUID?) {
        session.state = TTTState.FINISHED
        val creator = Bukkit.getPlayer(session.creatorId)
        val joiner  = session.joinerId?.let { Bukkit.getPlayer(it) }

        // Cerrar GUIs
        creator?.closeInventory()
        joiner?.closeInventory()

        if (winnerId == null) {
            // Empate — devolver apuestas
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), session.betAmount)
            session.joinerId?.let { plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it), session.betAmount) }
            creator?.sendMessage(msg("ttt.tie"))
            joiner?.sendMessage(msg("ttt.tie"))
            creator?.playSound(creator.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
            joiner?.playSound(joiner.location,  Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
        } else {
            val loserId  = if (winnerId == session.creatorId) session.joinerId!! else session.creatorId
            val totalPot = session.betAmount * 2
            val (netWin, tax) = TaxUtil.applyTax(plugin, totalPot, "ttt")
            val taxMsg = TaxUtil.taxMessage(plugin, tax)

            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerId), netWin)

            val winnerPlayer = Bukkit.getPlayer(winnerId)
            val loserPlayer  = Bukkit.getPlayer(loserId)
            winnerPlayer?.sendMessage(msg("ttt.win",  "amount" to netWin.toLong().toString(), "tax" to taxMsg))
            loserPlayer?.sendMessage(msg("ttt.lose",  "amount" to session.betAmount.toLong().toString()))
            winnerPlayer?.playSound(winnerPlayer.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            loserPlayer?.playSound(loserPlayer.location,   Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)

            val winnerName = winnerPlayer?.name ?: Bukkit.getOfflinePlayer(winnerId).name ?: "?"
            val loserName  = loserPlayer?.name  ?: Bukkit.getOfflinePlayer(loserId).name  ?: "?"
            plugin.server.broadcast(msg("ttt.broadcast-result",
                "winner" to winnerName, "loser" to loserName,
                "amount" to netWin.toLong().toString()))
        }

        removeSession(session)
    }

    // ── Cancelar ──────────────────────────────────────────────────────────────
    fun cancelGame(player: Player) {
        val sessionId = playerSession[player.uniqueId]
            ?: run { player.sendMessage(msg("ttt.no-game")); return }
        val session = sessions[sessionId] ?: return
        if (session.state != TTTState.WAITING) { player.sendMessage(msg("ttt.cannot-cancel")); return }
        plugin.economyManager.depositPlayer(player, session.betAmount)
        removeSession(session)
        player.sendMessage(msg("ttt.cancelled", "amount" to session.betAmount.toLong().toString()))
    }

    fun getOpenGames(): List<TTTSession> = sessions.values.filter { it.state == TTTState.WAITING }

    private fun removeSession(session: TTTSession) {
        sessions.remove(session.id)
        playerSession.remove(session.creatorId)
        session.joinerId?.let { playerSession.remove(it) }
    }

    fun cleanupAll() { sessions.clear(); playerSession.clear() }
}
