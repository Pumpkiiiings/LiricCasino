package liric.casino.util

import liric.casino.CasinoPlugin
import org.bukkit.Sound
import org.bukkit.entity.Player

object ValidationUtil {

    /**
     * Checks if the player has exceeded their daily play limit for a given game.
     * Automatically sends a message and sound to the player if the limit is reached.
     * @return true if the player CAN play, false if they have reached the limit.
     */
    fun canPlayDaily(plugin: CasinoPlugin, player: Player, gameName: String): Boolean {
        val pathPrefix = "${gameName.lowercase()}.uses"
        
        // If config doesn't exist for uses, we assume unlimited.
        if (!plugin.config.contains(pathPrefix)) return true

        val dailyLimit = plugin.config.getInt("$pathPrefix.default.max-uses-per-day", 2)
        var maxUses = dailyLimit
        val ranks = plugin.config.getConfigurationSection("$pathPrefix.ranks")

        if (ranks != null) {
            for (rank in ranks.getKeys(false)) {
                val perm = ranks.getString("$rank.permission") ?: continue
                if (player.hasPermission(perm)) {
                    val limit = ranks.getInt("$rank.max-uses-per-day", dailyLimit)
                    if (limit == -1 || limit > maxUses) {
                        maxUses = limit
                    }
                }
            }
        }

        if (maxUses == -1) return true // Unlimited

        val used = plugin.statsManager.getGameDailyUses(player.uniqueId, gameName)
        if (used >= maxUses) {
            val limitMsgRaw = plugin.messages.getRaw("general.limit-reached")
            if (limitMsgRaw.contains("[MISSING")) {
                // Fallback message if not configured in messages.yml
                player.sendMessage(plugin.format("<#FF5555><bold>❌ Limit Reached!</bold> <gray>You have exhausted your $maxUses daily uses for ${gameName.replaceFirstChar { it.uppercase() }}."))
            } else {
                player.sendMessage(plugin.messages.get("general.limit-reached", "max" to maxUses.toString()))
            }
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return false
        }
        return true
    }

    /**
     * Validates if the bet amount is within the configured limits for the player's rank.
     * Sends messages if the bet is too high or too low.
     * @return true if the bet is valid, false otherwise.
     */
    fun validateBet(plugin: CasinoPlugin, player: Player, gameName: String, amount: Double): Boolean {
        val pathPrefix = "${gameName.lowercase()}.bet"
        
        // If config doesn't exist for bets, we just allow the bet if it's > 0 (or rely on game defaults).
        if (!plugin.config.contains(pathPrefix)) {
            if (amount <= 0 || amount.isNaN()) {
                player.sendMessage(plugin.messages.get("general.invalid-bet"))
                return false
            }
            return true
        }

        val defaultMin = plugin.config.getDouble("$pathPrefix.default.min", 100.0)
        val defaultMax = plugin.config.getDouble("$pathPrefix.default.max", 100000.0)
        
        var maxBet = defaultMax
        val ranks = plugin.config.getConfigurationSection("$pathPrefix.ranks")

        if (ranks != null) {
            for (rank in ranks.getKeys(false)) {
                val perm = ranks.getString("$rank.permission") ?: continue
                if (player.hasPermission(perm)) {
                    val limit = ranks.getDouble("$rank.max", defaultMax)
                    if (limit > maxBet) {
                        maxBet = limit
                    }
                }
            }
        }

        if (amount <= 0 || amount.isNaN()) {
            player.sendMessage(plugin.messages.get("general.invalid-bet"))
            return false
        }

        if (amount < defaultMin) {
            // We use game-specific bet-range if possible, else generic
            val msgRaw = plugin.messages.getRaw("${gameName.lowercase()}.bet-range")
            if (msgRaw.contains("[MISSING")) { // Missing translation
                player.sendMessage(plugin.format("<red>Minimum bet is $defaultMin.</red>"))
            } else {
                player.sendMessage(plugin.messages.get("${gameName.lowercase()}.bet-range", "max" to maxBet.toLong().toString()))
            }
            return false
        }

        if (amount > maxBet) {
            val msgRaw = plugin.messages.getRaw("${gameName.lowercase()}.bet-range")
            if (msgRaw.contains("[MISSING")) {
                player.sendMessage(plugin.format("<red>Your maximum bet limit is $maxBet.</red>"))
            } else {
                player.sendMessage(plugin.messages.get("${gameName.lowercase()}.bet-range", "max" to maxBet.toLong().toString()))
            }
            return false
        }

        return true
    }
}
