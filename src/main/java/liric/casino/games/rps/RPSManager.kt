package liric.casino.games.rps

import liric.casino.CasinoPlugin
import liric.casino.util.TaxUtil
import liric.casino.util.ValidationUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.containsKey
import kotlin.collections.get
import kotlin.collections.remove
import kotlin.text.clear
import kotlin.text.get
import kotlin.text.toLong

class RPSManager(private val plugin: CasinoPlugin) {

    private val sessions      = ConcurrentHashMap<UUID, RPSSession>()
    private val playerSession = ConcurrentHashMap<UUID, UUID>()

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)
    private fun minBet() = plugin.config.getDouble("rps.bet.min", 100.0)
    private fun maxBet() = plugin.config.getDouble("rps.bet.max", 500000.0)

    // ── Crear ─────────────────────────────────────────────────────────────────
    fun createGame(player: Player, amount: Double) {
        if (!ValidationUtil.canPlayDaily(plugin, player, "rps")) return
        if (!ValidationUtil.validateBet(plugin, player, "rps", amount)) return
        if (playerSession.containsKey(player.uniqueId)) {
            player.sendMessage(msg("rps.already-in-game")); return
        }
        if (!plugin.economyManager.withdrawPlayer(player, amount).transactionSuccess()) {
            player.sendMessage(msg("rps.no-funds")); return
        }
        
        plugin.statsManager.recordGameUse(player.uniqueId, "rps")
        val session = RPSSession(creatorId = player.uniqueId, creatorName = player.name, betAmount = amount)
        sessions[session.id] = session
        playerSession[player.uniqueId] = session.id

        player.sendMessage(msg("rps.created", "amount" to amount.toLong().toString()))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

        // Broadcast
        plugin.server.onlinePlayers.filter { it.uniqueId != player.uniqueId }.forEach { p ->
            p.sendMessage(msg("rps.broadcast-new", "player" to player.name, "amount" to amount.toLong().toString()))
        }
    }

    // ── Unirse ────────────────────────────────────────────────────────────────
    fun joinGame(joiner: Player, creatorName: String) {
        val session = sessions.values.firstOrNull {
            it.state == RPSState.WAITING &&
            it.creatorName.equals(creatorName, ignoreCase = true) &&
            it.creatorId != joiner.uniqueId
        } ?: run { joiner.sendMessage(msg("rps.game-not-found", "player" to creatorName)); return }

        if (!ValidationUtil.canPlayDaily(plugin, joiner, "rps")) return
        if (!ValidationUtil.validateBet(plugin, joiner, "rps", session.betAmount)) return

        if (playerSession.containsKey(joiner.uniqueId)) {
            joiner.sendMessage(msg("rps.already-in-game")); return
        }

        if (!plugin.economyManager.withdrawPlayer(joiner, session.betAmount).transactionSuccess()) {
            joiner.sendMessage(msg("rps.no-funds")); return
        }

        plugin.statsManager.recordGameUse(joiner.uniqueId, "rps")
        session.joinerId   = joiner.uniqueId
        session.joinerName = joiner.name
        session.state      = RPSState.CHOOSING
        playerSession[joiner.uniqueId] = session.id

        val creator = Bukkit.getPlayer(session.creatorId)

        // Abrir GUI a ambos jugadores
        joiner.sendMessage(msg("rps.joined", "player" to session.creatorName, "amount" to session.betAmount.toLong().toString()))
        creator?.sendMessage(msg("rps.opponent-joined", "player" to joiner.name))

        // Delay de 1 tick para que el mensaje se vea antes de abrir GUI
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            RPSChoiceGUI(plugin, session, joiner, false).open()
            creator?.let { RPSChoiceGUI(plugin, session, it, true).open() }
        }, 5L)
    }

    // ── Registrar elección ────────────────────────────────────────────────────
    fun recordChoice(session: RPSSession, playerId: UUID, choice: RPSChoice) {
        if (session.state != RPSState.CHOOSING) return
        val isCreator = playerId == session.creatorId

        if (isCreator) {
            if (session.creatorChoice != null) return
            session.creatorChoice = choice
        } else {
            if (session.joinerChoice != null) return
            session.joinerChoice = choice
        }

        val player = Bukkit.getPlayer(playerId)
        player?.sendMessage(msg("rps.choice-locked", "choice" to "${choice.emoji} ${choice.displayName}"))

        // Si ambos eligieron, resolver
        if (session.creatorChoice != null && session.joinerChoice != null) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable { resolveGame(session) }, 20L)
        }
    }

    // ── Resolver ──────────────────────────────────────────────────────────────
    private fun resolveGame(session: RPSSession) {
        session.state = RPSState.FINISHED
        val c1 = session.creatorChoice!!
        val c2 = session.joinerChoice!!
        val totalPot = session.betAmount * 2
        val (netWin, tax) = TaxUtil.applyTax(plugin, totalPot, "rps")

        val creator = Bukkit.getPlayer(session.creatorId)
        val joiner  = session.joinerId?.let { Bukkit.getPlayer(it) }

        val taxMsg = TaxUtil.taxMessage(plugin, tax)

        when {
            c1 == c2 -> {
                // Empate — devolver apuestas
                plugin.economyManager.depositPlayer(
                    Bukkit.getOfflinePlayer(session.creatorId), session.betAmount)
                session.joinerId?.let {
                    plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it), session.betAmount)
                }
                creator?.sendMessage(msg("rps.tie", "c1" to "${c1.emoji} ${c1.displayName}", "c2" to "${c2.emoji} ${c2.displayName}"))
                joiner?.sendMessage(msg("rps.tie",  "c1" to "${c2.emoji} ${c2.displayName}", "c2" to "${c1.emoji} ${c1.displayName}"))
                creator?.playSound(creator.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
                joiner?.playSound(joiner.location,  Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
            }
            c1.beats(c2) -> {
                // Creator gana
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), netWin)
                creator?.sendMessage(msg("rps.win",  "myChoice" to "${c1.emoji} ${c1.displayName}", "opChoice" to "${c2.emoji} ${c2.displayName}", "amount" to netWin.toLong().toString(), "tax" to taxMsg))
                joiner?.sendMessage(msg("rps.lose",  "myChoice" to "${c2.emoji} ${c2.displayName}", "opChoice" to "${c1.emoji} ${c1.displayName}", "amount" to session.betAmount.toLong().toString()))
                creator?.playSound(creator.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                joiner?.playSound(joiner.location,   Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
                broadcastResult(session.creatorName, session.joinerName ?: "?", c1, c2, netWin)
            }
            else -> {
                // Joiner gana
                session.joinerId?.let { jid ->
                    plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(jid), netWin)
                }
                joiner?.sendMessage(msg("rps.win",  "myChoice" to "${c2.emoji} ${c2.displayName}", "opChoice" to "${c1.emoji} ${c1.displayName}", "amount" to netWin.toLong().toString(), "tax" to taxMsg))
                creator?.sendMessage(msg("rps.lose", "myChoice" to "${c1.emoji} ${c1.displayName}", "opChoice" to "${c2.emoji} ${c2.displayName}", "amount" to session.betAmount.toLong().toString()))
                joiner?.playSound(joiner.location,  Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                creator?.playSound(creator.location, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
                broadcastResult(session.joinerName ?: "?", session.creatorName, c2, c1, netWin)
            }
        }

        removeSession(session)
    }

    private fun broadcastResult(winner: String, loser: String, wChoice: RPSChoice, lChoice: RPSChoice, amount: Double) {
        plugin.server.broadcast(msg("rps.broadcast-result",
            "winner" to winner, "loser" to loser,
            "wChoice" to "${wChoice.emoji} ${wChoice.displayName}",
            "lChoice" to "${lChoice.emoji} ${lChoice.displayName}",
            "amount"  to amount.toLong().toString()
        ))
    }

    // ── Cancelar ──────────────────────────────────────────────────────────────
    fun cancelGame(player: Player) {
        val sessionId = playerSession[player.uniqueId]
            ?: run { player.sendMessage(msg("rps.no-game")); return }
        val session = sessions[sessionId] ?: return
        if (session.state != RPSState.WAITING) {
            player.sendMessage(msg("rps.cannot-cancel")); return
        }
        plugin.economyManager.depositPlayer(player, session.betAmount)
        removeSession(session)
        player.sendMessage(msg("rps.cancelled", "amount" to session.betAmount.toLong().toString()))
    }

    fun getOpenGames(): List<RPSSession> = sessions.values.filter { it.state == RPSState.WAITING }

    private fun removeSession(session: RPSSession) {
        sessions.remove(session.id)
        playerSession.remove(session.creatorId)
        session.joinerId?.let { playerSession.remove(it) }
    }

    fun handleDisconnect(playerId: UUID) {
        val sessionId = playerSession[playerId] ?: return
        val session = sessions[sessionId] ?: return

        if (session.state == RPSState.WAITING) {
            if (session.creatorId == playerId) {
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(playerId), session.betAmount)
                removeSession(session)
            }
        } else if (session.state == RPSState.CHOOSING) {
            val isCreator = playerId == session.creatorId
            val winnerId = if (isCreator) session.joinerId!! else session.creatorId
            val totalPot = session.betAmount * 2
            val (netWin, _) = TaxUtil.applyTax(plugin, totalPot, "rps")

            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerId), netWin)
            
            val winner = Bukkit.getPlayer(winnerId)
            winner?.sendMessage(plugin.format("<red>Your opponent disconnected. <green>You won the bet automatically!"))
            winner?.playSound(winner.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            
            removeSession(session)
            winner?.closeInventory()
        }
    }

    fun cleanupAll() {
        sessions.values.forEach { session ->
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), session.betAmount)
            session.joinerId?.let { plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it), session.betAmount) }
        }
        sessions.clear()
        playerSession.clear()
    }
}
