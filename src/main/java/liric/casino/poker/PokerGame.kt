package liric.casino.poker

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

enum class PokerState { WAITING, STARTING, PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN }

class PokerPlayer(val uuid: UUID, val name: String) {
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
    private var task: BukkitRunnable? = null

    val maxPlayers = 6
    val minPlayers = 2
    val entryFee = 5000.0 // Ciega Grande inicial

    // ==========================================
    // LOBBY Y RECLUTAMIENTO
    // ==========================================
    fun addPlayer(player: Player) {
        if (state != PokerState.WAITING && state != PokerState.STARTING) {
            player.sendMessage(plugin.format("$prefix <#FF5555>Juego en curso. ¡Espera!</#FF5555>"))
            return
        }
        if (players.any { it.uuid == player.uniqueId }) return
        if (players.size >= maxPlayers) return

        if (plugin.economyManager.withdrawPlayer(player, entryFee).transactionSuccess()) {
            players.add(PokerPlayer(player.uniqueId, player.name))
            plugin.server.broadcast(plugin.format("$prefix <#00FF7F>¡<#FFB400>${player.name}</#FFB400> entró al Poker! (${players.size}/$maxPlayers)</#00FF7F>"))
            plugin.pokerManager.updateHolograms()
            checkLobby()
        } else {
            player.sendMessage(plugin.format("$prefix <#FF5555>Necesitas $$entryFee para entrar.</#FF5555>"))
        }
    }

    fun removePlayer(player: Player) {
        val pokerPlayer = players.find { it.uuid == player.uniqueId }

        if (pokerPlayer == null) {
            player.sendMessage(plugin.format("$prefix <#FF5555>No estás en ninguna mesa de Poker.</#FF5555>"))
            return
        }

        if (state == PokerState.WAITING || state == PokerState.STARTING) {
            // El juego aún no empieza: Se sale de la lista y se le REEMBOLSA el dinero
            plugin.economyManager.depositPlayer(player, entryFee)
            players.remove(pokerPlayer)

            plugin.server.broadcast(plugin.format("$prefix <#FF5555>¡<#FFB400>${player.name}</#FFB400> abandonó la mesa de Poker! (${players.size}/$maxPlayers)</#FF5555>"))
            plugin.pokerManager.updateHolograms()
            player.sendMessage(plugin.format("$prefix <#00FF7F>Has salido de la mesa. Tu entrada de $$entryFee fue reembolsada.</#00FF7F>"))
        } else {
            // El juego está en curso: Hace "Fold" automático y pierde el dinero que apostó
            if (!pokerPlayer.hasFolded) {
                handleAction(player.uniqueId, "FOLD", 0.0)
            }
            player.sendMessage(plugin.format("$prefix <#FF5555>Has abandonado la partida en curso (Te has retirado automáticamente).</#FF5555>"))
        }

        // Cierra el inventario por si tenía el menú abierto
        player.closeInventory()
    }

    private fun checkLobby() {
        if (state == PokerState.WAITING && players.size >= minPlayers) {
            startCountdown(60)
        } else if (state == PokerState.STARTING && players.size == maxPlayers) {
            countdownSeconds = 5
        }
    }

    private fun startCountdown(seconds: Int) {
        state = PokerState.STARTING
        countdownSeconds = seconds
        task = object : BukkitRunnable() {
            override fun run() {
                if (players.size < minPlayers) {
                    state = PokerState.WAITING
                    plugin.server.broadcast(plugin.format("$prefix <#FF5555>Faltan jugadores. Inicio cancelado.</#FF5555>"))
                    cancel()
                    return
                }
                if (countdownSeconds <= 0) {
                    startRealGame()
                    cancel()
                    return
                }
                plugin.pokerManager.updateHolograms()
                countdownSeconds--
            }
        }
        task?.runTaskTimer(plugin, 0L, 20L)
    }

    // ==========================================
    // MOTOR DE TEXAS HOLD'EM
    // ==========================================
    private fun startRealGame() {
        deck = Deck()
        communityCards.clear()
        pot = players.size * entryFee
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

        // Abrir menú a todos
        players.forEach { p ->
            val bukkitPlayer = Bukkit.getPlayer(p.uuid)
            if (bukkitPlayer != null) {
                PokerMenu(plugin, this, bukkitPlayer).open()
            }
        }
        startTurnTimer()
    }

    // Temporizador de turno: Si no juega en 15s, se retira (Fold)
    private fun startTurnTimer() {
        task?.cancel()
        countdownSeconds = 15
        task = object : BukkitRunnable() {
            override fun run() {
                // Actualiza visualmente el menú para todos en vivo
                updateMenus()

                if (countdownSeconds <= 0) {
                    val currentPlayer = getCurrentPlayer()
                    if (currentPlayer != null && !currentPlayer.hasFolded) {
                        handleAction(currentPlayer.uuid, "FOLD", 0.0) // Auto-Fold
                    }
                }
                countdownSeconds--
            }
        }
        task?.runTaskTimer(plugin, 0L, 20L)
    }

    fun getCurrentPlayer(): PokerPlayer? {
        val p = players.getOrNull(turnIndex)
        if (p?.hasFolded == true) {
            advanceTurn() // Saltar a los que se retiraron
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
                Bukkit.getPlayer(player.uuid)?.sendMessage(plugin.format("$prefix <gray>Te has retirado (Fold).</gray>"))
            }
            "CALL" -> {
                val callAmount = currentHighestBet - player.currentBet
                if (plugin.economyManager.withdrawPlayer(Bukkit.getPlayer(playerUuid)!!, callAmount).transactionSuccess()) {
                    player.currentBet += callAmount
                    pot += callAmount
                }
            }
            "RAISE" -> {
                val raiseAmount = (currentHighestBet - player.currentBet) + amount
                if (plugin.economyManager.withdrawPlayer(Bukkit.getPlayer(playerUuid)!!, raiseAmount).transactionSuccess()) {
                    player.currentBet += raiseAmount
                    pot += raiseAmount
                    currentHighestBet = player.currentBet
                }
            }
        }
        advanceTurn()
    }

    private fun advanceTurn() {
        turnIndex++

        // Verifica si todos jugaron
        val activeParticipants = players.filter { !it.hasFolded }
        if (activeParticipants.size == 1) {
            // Todos se retiraron menos 1, gana automáticamente
            endGame(activeParticipants.first())
            return
        }

        // Si ya dio la vuelta a la mesa y todos igualaron la apuesta, avanza la ronda
        if (turnIndex >= players.size) {
            val betsMatched = activeParticipants.all { it.currentBet == currentHighestBet }
            if (betsMatched) {
                nextRound()
                return
            } else {
                turnIndex = 0 // Da otra vuelta si alguien subió
            }
        }

        // Si el siguiente se retiró, lo salta
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
        task?.cancel()
        val activeParticipants = players.filter { !it.hasFolded }

        var bestPlayer = activeParticipants.first()
        var bestScore = 0L

        activeParticipants.forEach { p ->
            val score = HandEvaluator.evaluate(p.holeCards, communityCards)
            if (score > bestScore) {
                bestScore = score
                bestPlayer = p
            }
        }

        updateMenus() // Para que vean todas las cartas
        endGame(bestPlayer, HandEvaluator.getHandName(bestScore))
    }

    private fun endGame(winnerInfo: PokerPlayer, handName: String = "Todos se retiraron") {
        val winner = Bukkit.getPlayer(winnerInfo.uuid)
        if (winner != null) {
            plugin.economyManager.depositPlayer(winner, pot)
            plugin.server.broadcast(plugin.format("$prefix <#00FF7F><bold>¡${winner.name} ganó el Poker ($$pot) con $handName!</bold></#00FF7F>"))
            winner.playSound(winner.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
        }

        object : BukkitRunnable() {
            override fun run() {
                players.forEach { p -> Bukkit.getPlayer(p.uuid)?.closeInventory() }
                players.clear()
                communityCards.clear()
                pot = 0.0
                state = PokerState.WAITING
                plugin.pokerManager.updateHolograms()
            }
        }.runTaskLater(plugin, 100L) // Cierra el menú tras 5 segundos
    }

    private fun updateMenus() {
        players.forEach { p ->
            val bp = Bukkit.getPlayer(p.uuid)
            if (bp != null && bp.openInventory.title.contains("TEXAS HOLD'EM")) {
                // Forzar actualización del menú activo
                PokerMenu(plugin, this, bp).updateExisting(bp)
            }
        }
    }
}
