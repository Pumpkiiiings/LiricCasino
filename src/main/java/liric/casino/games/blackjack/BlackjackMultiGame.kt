package liric.casino.games.blackjack

import liric.casino.CasinoPlugin
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

enum class MultiGameState { WAITING, STARTING, PLAYING }

class BlackjackMultiGame(private val plugin: CasinoPlugin) {
    val maxPlayers = 4
    var state = MultiGameState.WAITING
        private set
    var countdownSeconds = 15
        private set

    // UUID a la cantidad apostada
    val activePlayers = mutableMapOf<UUID, Double>()
    private var countdownTask: BukkitRunnable? = null

    fun addPlayer(player: Player, betAmount: Double) {
        if (state == MultiGameState.PLAYING) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>La partida multijugador ya está en curso. Espera a que termine."))
            return
        }

        if (activePlayers.size >= maxPlayers) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>La mesa está llena."))
            return
        }

        if (activePlayers.containsKey(player.uniqueId)) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>Ya estás en la mesa."))
            return
        }

        activePlayers[player.uniqueId] = betAmount
        broadcast("<#00FF7F>${player.name}</#00FF7F> <gray>se ha sentado en la mesa con <#FFD700>$${betAmount}</#FFD700>. (${activePlayers.size}/$maxPlayers)")

        plugin.blackjackManager.updateHolograms()

        if (state == MultiGameState.WAITING) {
            startCountdown()
        } else if (activePlayers.size == maxPlayers) {
            // Fuerza inicio si se llena
            countdownSeconds = 2
        }
    }

    // --- NUEVA FUNCIÓN AÑADIDA PARA SALIR DEL LOBBY ---
    fun removePlayer(player: Player) {
        if (!activePlayers.containsKey(player.uniqueId)) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>No estás en la mesa multijugador."))
            return
        }

        if (state == MultiGameState.PLAYING) {
            player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <red>La partida ya empezó, ¡no puedes abandonar ahora!"))
            return
        }

        // Obtener apuesta, eliminarlo de la mesa y devolverle el dinero
        val betAmount = activePlayers.remove(player.uniqueId) ?: 0.0

        // Devolvemos el dinero usando Vault/EconomyManager
        plugin.economyManager.vault?.depositPlayer(player, betAmount)

        player.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» <#00FF7F>Has salido de la mesa. Se te devolvieron tus $${betAmount}.</#00FF7F>"))
        broadcast("<#FF5555>${player.name}</#FF5555> <gray>ha abandonado la mesa. (${activePlayers.size}/$maxPlayers)")

        plugin.blackjackManager.updateHolograms()

        // Si la mesa se queda vacía, cancelamos el contador
        if (activePlayers.isEmpty()) {
            state = MultiGameState.WAITING
            countdownTask?.cancel()
            countdownTask = null
        }
    }
    // --------------------------------------------------

    private fun startCountdown() {
        state = MultiGameState.STARTING
        countdownSeconds = 15

        countdownTask = object : BukkitRunnable() {
            override fun run() {
                if (activePlayers.isEmpty()) {
                    state = MultiGameState.WAITING
                    cancel()
                    return
                }

                plugin.blackjackManager.updateHolograms()

                if (countdownSeconds <= 0) {
                    startGame()
                    cancel()
                    return
                }

                if (countdownSeconds == 15 || countdownSeconds <= 5) {
                    broadcast("<gray>La mesa empieza a repartir en <yellow>$countdownSeconds</yellow>s...")
                }
                countdownSeconds--
            }
        }.apply { runTaskTimer(plugin, 0L, 20L) }
    }

    private fun startGame() {
        state = MultiGameState.PLAYING
        plugin.blackjackManager.updateHolograms()
        broadcast("<#FFB400>¡NO VA MÁS! El Dealer empieza a repartir...</#FFB400>")

        // Instanciar la verdadera mesa
        val session = BlackjackMultiSession(plugin, activePlayers.toMap(), this)
        session.start()
    }

    fun resetGame() {
        activePlayers.clear()
        state = MultiGameState.WAITING
        plugin.blackjackManager.updateHolograms()
    }

    private fun broadcast(msg: String) {
        val formatted = plugin.format("<#FF0000><bold>♠ MESA BLACKJACK</bold></#FF0000> <gray>»</gray> $msg")
        activePlayers.keys.forEach { uuid ->
            plugin.server.getPlayer(uuid)?.sendMessage(formatted)
            plugin.server.getPlayer(uuid)?.playSound(plugin.server.getPlayer(uuid)!!.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
        }
    }
}
