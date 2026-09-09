package liric.casino.games.poker

import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import liric.casino.util.ValidationUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

enum class PokerState { WAITING, STARTING, PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN }

class PokerPlayer(val uuid: UUID, val name: String, var totalContributed: Double = 0.0) {
    var holeCards = mutableListOf<Card>()
    var currentBet = 0.0
    var hasFolded = false
    var isAllIn = false
}

class PokerGame(val plugin: CasinoPlugin) {
    val prefix = "<#FF0000><bold>CASINO</bold></#FF0000> <gray>»</gray>"
    var state = PokerState.WAITING
        private set

    val players = mutableListOf<PokerPlayer>()
    var communityCards = mutableListOf<Card>()
    var deck = Deck()
    var pot = 0.0
    var currentHighestBet = 0.0
    var turnIndex = 0
    var countdownSeconds = 0
    private var task: Runnable? = null

    val maxPlayers get() = plugin.config.getInt("poker.maximum-players", 6).coerceIn(2, 9)
    val minPlayers get() = plugin.config.getInt("poker.minimum-players", 2).coerceIn(2, maxPlayers)
    val entryFee get() = plugin.config.getDouble("poker.entry-fee", 5000.0).coerceAtLeast(1.0)
    val raiseAmount get() = plugin.config.getDouble("poker.raise-amount", 5000.0).coerceAtLeast(1.0)




    fun addPlayer(player: Player) {
        if (state != PokerState.WAITING && state != PokerState.STARTING) {
            player.sendMessage(plugin.format("$prefix <#FF5555>Game in progress. Wait!</#FF5555>"))
            return
        }
        if (players.any { it.uuid == player.uniqueId }) return
        if (players.size >= maxPlayers) return

        if (!ValidationUtil.canPlayDaily(plugin, player, "poker")) return
        if (!ValidationUtil.validateBet(plugin, player, "poker", entryFee)) return

        if (plugin.economyManager.withdrawPlayer(player, entryFee)?.transactionSuccess() == true) {
            plugin.statsManager.recordGameUse(player.uniqueId, "poker")
            players.add(PokerPlayer(player.uniqueId, player.name, entryFee))
            plugin.server.broadcast(plugin.format("$prefix <#00FF7F><#FFB400>${player.name}</#FFB400> joined Poker! (${players.size}/$maxPlayers)</#00FF7F>"))
            plugin.pokerManager.updateHolograms()
            checkLobby()
        } else {
            player.sendMessage(plugin.format("$prefix <#FF5555>You need $$entryFee to join.</#FF5555>"))
        }
    }

    fun removePlayer(player: Player) {
        val pokerPlayer = players.find { it.uuid == player.uniqueId }

        if (pokerPlayer == null) {
            player.sendMessage(plugin.format("$prefix <#FF5555>You are not at any Poker table.</#FF5555>"))
            return
        }

        if (state == PokerState.WAITING || state == PokerState.STARTING) {

            plugin.economyManager.depositPlayer(player, pokerPlayer.totalContributed)
            pokerPlayer.totalContributed = 0.0
            players.remove(pokerPlayer)

            plugin.server.broadcast(plugin.format("$prefix <#FF5555><#FFB400>${player.name}</#FFB400> left the Poker table! (${players.size}/$maxPlayers)</#FF5555>"))
            plugin.pokerManager.updateHolograms()
            player.sendMessage(plugin.format("$prefix <#00FF7F>You left the table. Your contribution was refunded.</#00FF7F>"))
        } else {

            if (!pokerPlayer.hasFolded) {
                handleAction(player.uniqueId, "FOLD", 0.0)
            }
            player.sendMessage(plugin.format("$prefix <#FF5555>You left the ongoing game (Auto-folded).</#FF5555>"))
        }


        player.closeInventory()
    }

    private fun checkLobby() {
        if (state == PokerState.WAITING && players.size >= minPlayers) {
            startCountdown(plugin.config.getInt("poker.start-countdown-seconds", 30).coerceAtLeast(5))
        } else if (state == PokerState.STARTING && players.size == maxPlayers) {
            countdownSeconds = 5
        }
    }

    private fun startCountdown(seconds: Int) {
        state = PokerState.STARTING
        countdownSeconds = seconds
        task = SchedulerUtil.runGlobalTimer(plugin, 0L, 20L) {
            if (players.size < minPlayers) {
                state = PokerState.WAITING
                plugin.server.broadcast(plugin.format("$prefix <#FF5555>Not enough players. Start cancelled.</#FF5555>"))
                task?.run()
                return@runGlobalTimer
            }
            if (countdownSeconds <= 0) {
                startRealGame()
                task?.run()
                return@runGlobalTimer
            }
            plugin.pokerManager.updateHolograms()
            countdownSeconds--
        }
    }




    private fun startRealGame() {
        deck = Deck()
        communityCards.clear()
        pot = players.sumOf { it.totalContributed }
        currentHighestBet = 0.0

        players.forEach {
            it.holeCards.clear()
            it.holeCards.add(deck.draw())
            it.holeCards.add(deck.draw())
            it.hasFolded = false
            it.currentBet = 0.0
        }

        turnIndex = 0
        state = PokerState.PRE_FLOP
        plugin.pokerManager.updateHolograms()


        players.forEach { p ->
            val bukkitPlayer = Bukkit.getPlayer(p.uuid)
            if (bukkitPlayer != null) {
                PokerMenu(plugin, this, bukkitPlayer).open()
            }
        }
        startTurnTimer()
    }


    private fun startTurnTimer() {
        task?.run()
        countdownSeconds = plugin.config.getInt("poker.turn-seconds", 15).coerceAtLeast(5)
        task = SchedulerUtil.runGlobalTimer(plugin, 0L, 20L) {
            updateMenus()

            if (countdownSeconds <= 0) {
                val currentPlayer = getCurrentPlayer()
                if (currentPlayer != null && !currentPlayer.hasFolded) {
                    handleAction(currentPlayer.uuid, "FOLD", 0.0)
                }
            }
            countdownSeconds--
        }
    }

    fun getCurrentPlayer(): PokerPlayer? {
        val p = players.getOrNull(turnIndex)
        if (p?.hasFolded == true) {
            advanceTurn()
            return players.getOrNull(turnIndex)
        }
        return p
    }

    fun handleAction(playerUuid: UUID, action: String, amount: Double) {
        val player = players.find { it.uuid == playerUuid } ?: return
        if (player != getCurrentPlayer()) return

        when (action) {
            "FOLD" -> {
                player.hasFolded = true
                Bukkit.getPlayer(player.uuid)?.sendMessage(plugin.format("$prefix <gray>You folded.</gray>"))
            }
            "CALL" -> {
                val callAmount = currentHighestBet - player.currentBet
                val bukkitPlayer = Bukkit.getPlayer(playerUuid) ?: return
                if (callAmount <= 0.0 || plugin.economyManager.withdrawPlayer(bukkitPlayer, callAmount)?.transactionSuccess() == true) {
                    player.currentBet += callAmount
                    player.totalContributed += callAmount
                    pot += callAmount
                } else return
            }
            "RAISE" -> {
                if (!amount.isFinite() || amount <= 0.0) return
                val raiseAmount = (currentHighestBet - player.currentBet) + amount
                val bukkitPlayer = Bukkit.getPlayer(playerUuid) ?: return
                if (plugin.economyManager.withdrawPlayer(bukkitPlayer, raiseAmount)?.transactionSuccess() == true) {
                    player.currentBet += raiseAmount
                    player.totalContributed += raiseAmount
                    pot += raiseAmount
                    currentHighestBet = player.currentBet
                } else return
            }
        }
        advanceTurn()
    }

    private fun advanceTurn() {
        turnIndex++


        val activeParticipants = players.filter { !it.hasFolded }
        if (activeParticipants.size == 1) {

            endGame(activeParticipants.first())
            return
        }


        if (turnIndex >= players.size) {
            val betsMatched = activeParticipants.all { it.currentBet == currentHighestBet }
            if (betsMatched) {
                nextRound()
                return
            } else {
                turnIndex = 0
            }
        }


        if (players[turnIndex].hasFolded) {
            advanceTurn()
        } else {
            startTurnTimer()
        }
    }

    private fun nextRound() {
        players.forEach { it.currentBet = 0.0 }
        currentHighestBet = 0.0
        turnIndex = 0

        when (state) {
            PokerState.PRE_FLOP -> {
                state = PokerState.FLOP
                communityCards.add(deck.draw())
                communityCards.add(deck.draw())
                communityCards.add(deck.draw())
            }
            PokerState.FLOP -> {
                state = PokerState.TURN
                communityCards.add(deck.draw())
            }
            PokerState.TURN -> {
                state = PokerState.RIVER
                communityCards.add(deck.draw())
            }
            PokerState.RIVER -> {
                state = PokerState.SHOWDOWN
                evaluateWinner()
                return
            }
            else -> return
        }
        startTurnTimer()
    }

    private fun evaluateWinner() {
        task?.run()
        val activeParticipants = players.filter { !it.hasFolded }

        if (activeParticipants.isEmpty()) {
            updateMenus()
            endGame(PokerPlayer(UUID.randomUUID(), "Nobody"), "Mass Fold")
            return
        }

        val scores = activeParticipants.associateWith { HandEvaluator.evaluate(it.holeCards, communityCards) }
        val bestScore = scores.values.maxOrNull() ?: 0L
        val winners = scores.filterValues { it == bestScore }.keys.toList()

        updateMenus()
        endGame(winners, HandEvaluator.getHandName(bestScore))
    }

    private fun endGame(winnerInfo: PokerPlayer, handName: String = "Everyone folded") {
        endGame(listOf(winnerInfo), handName)
    }

    private fun endGame(winners: List<PokerPlayer>, handName: String) {
        val eligible = winners.filter { winner -> players.any { it.uuid == winner.uuid } }
        if (eligible.isNotEmpty()) {
            val share = pot / eligible.size
            eligible.forEach { winnerInfo ->
                val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, share, "poker")
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(winnerInfo.uuid), netWin)
                Bukkit.getPlayer(winnerInfo.uuid)?.playSound(Bukkit.getPlayer(winnerInfo.uuid)!!.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
            }
            val names = eligible.joinToString(", ") { it.name }
            val result = if (eligible.size == 1) "$names won Poker with $handName" else "$names split the Poker pot with $handName"
            plugin.server.broadcast(plugin.format("$prefix <#00FF7F><bold>$result ($${pot.toLong()})!</bold></#00FF7F>"))
        }
        players.forEach { it.totalContributed = 0.0 }

        SchedulerUtil.runGlobalLater(plugin, 100L) {
            players.forEach { p -> Bukkit.getPlayer(p.uuid)?.closeInventory() }
            players.clear()
            communityCards.clear()
            pot = 0.0
            state = PokerState.WAITING
            plugin.pokerManager.updateHolograms()
        }
    }

    private fun updateMenus() {
        players.forEach { p ->
            val bp = Bukkit.getPlayer(p.uuid)
            if (bp != null && bp.openInventory.title.contains("TEXAS HOLD'EM")) {

                PokerMenu(plugin, this, bp).updateExisting(bp)
            }
        }
    }

    fun cleanupAll() {
        task?.run()
        task = null
        players.filter { it.totalContributed > 0.0 }.forEach { player ->
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(player.uuid), player.totalContributed)
            player.totalContributed = 0.0
        }
        players.clear()
        communityCards.clear()
        pot = 0.0
        currentHighestBet = 0.0
        state = PokerState.WAITING
    }

    fun handleDisconnect(uuid: UUID) {
        val player = players.firstOrNull { it.uuid == uuid } ?: return
        if (state == PokerState.WAITING || state == PokerState.STARTING) {
            plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(uuid), player.totalContributed)
            player.totalContributed = 0.0
            players.remove(player)
            plugin.pokerManager.updateHolograms()
            return
        }

        player.hasFolded = true
        val remaining = players.filter { !it.hasFolded }
        if (remaining.size == 1) endGame(remaining.first())
    }
}
