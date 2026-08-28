package liric.casino.games.rps

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractMatchmakingManager
import liric.casino.util.SchedulerUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

class RPSManager(plugin: CasinoPlugin) : AbstractMatchmakingManager<RPSSession>(plugin, "rps") {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    override fun createGame(player: Player, amount: Double): RPSSession? {
        if (!canPlay(player, amount)) return null
        if (playerSession.containsKey(player.uniqueId)) {
            player.sendMessage(msg("rps.already-in-game")); return null
        }
        if (!takeBet(player, amount)) return null

        plugin.statsManager.recordGameUse(player.uniqueId, "rps")
        val session = RPSSession(creatorId = player.uniqueId, creatorName = player.name, betAmount = amount)
        addSession(session.id, session)
        playerSession[player.uniqueId] = session.id

        player.sendMessage(msg("rps.created", "amount" to amount.toLong().toString()))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

        plugin.server.onlinePlayers.filter { it.uniqueId != player.uniqueId }.forEach { p ->
            p.sendMessage(msg("rps.broadcast-new", "player" to player.name, "amount" to amount.toLong().toString()))
        }
        return session
    }

    override fun joinGame(joiner: Player, creatorName: String): Boolean {
        val session = activeSessions.values.firstOrNull {
            it.isOpen() &&
            it.creatorName.equals(creatorName, ignoreCase = true) &&
            it.creatorId != joiner.uniqueId
        } ?: run { joiner.sendMessage(msg("rps.game-not-found", "player" to creatorName)); return false }

        if (!canPlay(joiner, session.betAmount)) return false
        if (playerSession.containsKey(joiner.uniqueId)) {
            joiner.sendMessage(msg("rps.already-in-game")); return false
        }
        if (!takeBet(joiner, session.betAmount)) return false

        plugin.statsManager.recordGameUse(joiner.uniqueId, "rps")
        session.joinerId   = joiner.uniqueId
        session.joinerName = joiner.name
        session.state      = RPSState.CHOOSING
        playerSession[joiner.uniqueId] = session.id

        val creator = Bukkit.getPlayer(session.creatorId)

        joiner.sendMessage(msg("rps.joined", "player" to session.creatorName, "amount" to session.betAmount.toLong().toString()))
        creator?.sendMessage(msg("rps.opponent-joined", "player" to joiner.name))

        SchedulerUtil.runGlobalLater(plugin, 5L) {
            RPSChoiceGUI(plugin, session, joiner, false).open()
            creator?.let { RPSChoiceGUI(plugin, session, it, true).open() }
        }
        return true
    }

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

        if (session.creatorChoice != null && session.joinerChoice != null) {
            SchedulerUtil.runGlobalLater(plugin, 20L) { resolveGame(session) }
        }
    }

    private fun resolveGame(session: RPSSession) {
        session.state = RPSState.FINISHED
        val c1 = session.creatorChoice!!
        val c2 = session.joinerChoice!!

        val creator = Bukkit.getPlayer(session.creatorId)
        val joiner  = session.joinerId?.let { Bukkit.getPlayer(it) }

        when {
            c1 == c2 -> {
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), session.betAmount)
                session.joinerId?.let { plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it), session.betAmount) }
                creator?.sendMessage(msg("rps.tie", "c1" to "${c1.emoji} ${c1.displayName}", "c2" to "${c2.emoji} ${c2.displayName}"))
                joiner?.sendMessage(msg("rps.tie",  "c1" to "${c2.emoji} ${c2.displayName}", "c2" to "${c1.emoji} ${c1.displayName}"))
                creator?.playSound(creator.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
                joiner?.playSound(joiner.location,  Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
            }
            c1.beats(c2) -> {
                if (creator != null) {
                    processWin(creator, session.betAmount)
                    creator.sendMessage(msg("rps.win",  "myChoice" to "${c1.emoji} ${c1.displayName}", "opChoice" to "${c2.emoji} ${c2.displayName}", "amount" to (session.betAmount * 2).toLong().toString(), "tax" to "")) // tax logic already handled by processWin, keeping placeholder if needed. Wait, in original code it sent the tax message here. Let's just pass empty or recalculate for message if required. But processWin sends tax deduction message separately! So this is cleaner.
                    creator.playSound(creator.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                } else {
                    val totalPot = session.betAmount * 2
                    val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, totalPot, "rps")
                    plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(session.creatorId), netWin)
                }
                
                joiner?.sendMessage(msg("rps.lose",  "myChoice" to "${c2.emoji} ${c2.displayName}", "opChoice" to "${c1.emoji} ${c1.displayName}", "amount" to session.betAmount.toLong().toString()))
                joiner?.playSound(joiner.location,   Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
                broadcastResult(session.creatorName, session.joinerName ?: "?", c1, c2, session.betAmount * 2)
            }
            else -> {
                if (joiner != null) {
                    processWin(joiner, session.betAmount)
                    joiner.sendMessage(msg("rps.win",  "myChoice" to "${c2.emoji} ${c2.displayName}", "opChoice" to "${c1.emoji} ${c1.displayName}", "amount" to (session.betAmount * 2).toLong().toString(), "tax" to ""))
                    joiner.playSound(joiner.location,  Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                } else {
                    session.joinerId?.let { jid ->
                        val totalPot = session.betAmount * 2
                        val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, totalPot, "rps")
                        plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(jid), netWin)
                    }
                }
                
                creator?.sendMessage(msg("rps.lose", "myChoice" to "${c1.emoji} ${c1.displayName}", "opChoice" to "${c2.emoji} ${c2.displayName}", "amount" to session.betAmount.toLong().toString()))
                creator?.playSound(creator.location, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
                broadcastResult(session.joinerName ?: "?", session.creatorName, c2, c1, session.betAmount * 2)
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

    override fun cancelGame(player: Player): Boolean {
        val session = getPlayerSession(player.uniqueId)
            ?: run { player.sendMessage(msg("rps.no-game")); return false }
        if (!session.isOpen()) {
            player.sendMessage(msg("rps.cannot-cancel")); return false
        }
        plugin.economyManager.depositPlayer(player, session.betAmount)
        removeSession(session)
        player.sendMessage(msg("rps.cancelled", "amount" to session.betAmount.toLong().toString()))
        return true
    }

    override fun handleDisconnect(playerId: UUID) {
        val session = getPlayerSession(playerId) ?: return

        if (session.isOpen()) {
            if (session.creatorId == playerId) {
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(playerId), session.betAmount)
                removeSession(session)
            }
        } else if (session.state == RPSState.CHOOSING) {
            val isCreator = playerId == session.creatorId
            val winnerId = if (isCreator) session.joinerId!! else session.creatorId
            
            val winner = Bukkit.getPlayer(winnerId)
            if (winner != null) {
                processWin(winner, session.betAmount)
                val msg = plugin.messagesConfig.getString("rps-extra.opponent-disconnected", "<red>Your opponent disconnected. <green>You won the bet automatically!")!!
                winner.sendMessage(plugin.format(msg.replace("{prefix}", plugin.messagesConfig.getString("prefix", "")!!)))
                winner.playSound(winner.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                winner.closeInventory()
            } else {
                val totalPot = session.betAmount * 2
                val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, totalPot, "rps")
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerId), netWin)
            }
            removeSession(session)
        }
    }
}
