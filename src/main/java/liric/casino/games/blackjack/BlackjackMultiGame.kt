package liric.casino.games.blackjack

import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

enum class MultiGameState { WAITING, STARTING, PLAYING }

class BlackjackMultiGame(private val plugin: CasinoPlugin) {
    val maxPlayers = 4
    var state = MultiGameState.WAITING
        private set
    var countdownSeconds = 15
        private set


    val activePlayers = mutableMapOf<UUID, Double>()
    private var countdownTask: Runnable? = null

    fun addPlayer(player: Player, betAmount: Double) {
        if (state == MultiGameState.PLAYING) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>The multiplayer game is already in progress. Wait for it to finish."))
            return
        }

        if (activePlayers.size >= maxPlayers) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>The table is full."))
            return
        }

        if (activePlayers.containsKey(player.uniqueId)) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>You are already at the table."))
            return
        }

        activePlayers[player.uniqueId] = betAmount
        broadcast("<#00FF7F>${player.name}</#00FF7F> <gray>sat at the table with <#FFD700>$${betAmount}</#FFD700>. (${activePlayers.size}/$maxPlayers)")

        plugin.blackjackManager.updateHolograms()

        if (state == MultiGameState.WAITING) {
            startCountdown()
        } else if (activePlayers.size == maxPlayers) {

            countdownSeconds = 2
        }
    }


    fun removePlayer(player: Player) {
        if (!activePlayers.containsKey(player.uniqueId)) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>You are not at the multiplayer table."))
            return
        }

        if (state == MultiGameState.PLAYING) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>The game has already started, you can't leave now!"))
            return
        }


        val betAmount = activePlayers.remove(player.uniqueId) ?: 0.0


        plugin.economyManager.vault?.depositPlayer(player, betAmount)

        player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <#00FF7F>You left the table. Your $${betAmount} was returned.</#00FF7F>"))
        broadcast("<#FF5555>${player.name}</#FF5555> <gray>left the table. (${activePlayers.size}/$maxPlayers)")

        plugin.blackjackManager.updateHolograms()


        if (activePlayers.isEmpty()) {
            state = MultiGameState.WAITING
            countdownTask?.run()
            countdownTask = null
        }
    }


    private fun startCountdown() {
        state = MultiGameState.STARTING
        countdownSeconds = 15

        countdownTask = SchedulerUtil.runGlobalTimer(plugin, 0L, 20L) {
            if (activePlayers.isEmpty()) {
                state = MultiGameState.WAITING
                countdownTask?.run()
                return@runGlobalTimer
            }

            plugin.blackjackManager.updateHolograms()

            if (countdownSeconds <= 0) {
                startGame()
                countdownTask?.run()
                return@runGlobalTimer
            }

            if (countdownSeconds == 15 || countdownSeconds <= 5) {
                broadcast("<gray>The table starts dealing in <yellow>$countdownSeconds</yellow>s...")
            }
            countdownSeconds--
        }
    }

    private fun startGame() {
        state = MultiGameState.PLAYING
        plugin.blackjackManager.updateHolograms()
        broadcast("<#FFB400>NO MORE BETS! The Dealer starts dealing...</#FFB400>")


        val session = BlackjackMultiSession(plugin, activePlayers.toMap(), this)
        session.start()
    }

    fun resetGame() {
        activePlayers.clear()
        state = MultiGameState.WAITING
        plugin.blackjackManager.updateHolograms()
    }

    private fun broadcast(msg: String) {
        val formatted = plugin.format("<#FF0000><bold>♠ BLACKJACK TABLE</bold></#FF0000> <gray>»</gray> $msg")
        activePlayers.keys.forEach { uuid ->
            plugin.server.getPlayer(uuid)?.sendMessage(formatted)
            plugin.server.getPlayer(uuid)?.playSound(plugin.server.getPlayer(uuid)!!.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
        }
    }
}
