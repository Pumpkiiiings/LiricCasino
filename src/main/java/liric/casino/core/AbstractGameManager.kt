package liric.casino.core

import liric.casino.CasinoPlugin
import liric.casino.util.TaxUtil
import liric.casino.util.ValidationUtil
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractGameManager<S>(val plugin: CasinoPlugin, val gameId: String) {
    
    protected val activeSessions = ConcurrentHashMap<UUID, S>()

    /**
     * Retrieves all active sessions.
     */
    fun getActiveSessions(): List<S> = activeSessions.values.toList()

    /**
     * Validates if the player is allowed to play (checks limits and bets).
     */
    fun canPlay(player: Player, bet: Double): Boolean {
        if (!ValidationUtil.canPlayDaily(plugin, player, gameId)) return false
        if (!ValidationUtil.validateBet(plugin, player, gameId, bet)) return false
        return true
    }

    /**
     * Adds an active session for a player.
     */
    protected fun addSession(playerId: UUID, session: S) {
        activeSessions[playerId] = session
    }

    /**
     * Removes an active session for a player.
     */
    protected fun removeSession(playerId: UUID) {
        activeSessions.remove(playerId)
    }

    /**
     * Validates economy and takes the bet from the player.
     * Returns true if successful.
     */
    fun takeBet(player: Player, amount: Double): Boolean {
        if (!plugin.economyManager.has(player, amount)) {
            player.sendMessage(plugin.messages.get("general.no-money", "amount" to amount.toString()))
            return false
        }
        plugin.economyManager.withdraw(player, amount)
        return true
    }

    /**
     * Handles rewarding the player and processing taxes.
     * Use multiplier = 2.0 for standard 1v1 payouts.
     */
    fun processWin(player: Player, betAmount: Double, multiplier: Double = 2.0) {
        val rawWinnings = betAmount * multiplier
        val (finalWinnings, tax) = TaxUtil.calculateTax(plugin, gameId, rawWinnings)
        
        plugin.economyManager.deposit(player, finalWinnings)
        
        if (tax > 0) {
            player.sendMessage(plugin.messages.get("general.tax-deducted", 
                "amount" to finalWinnings.toString(), 
                "tax" to tax.toString()
            ))
        }
    }
}
