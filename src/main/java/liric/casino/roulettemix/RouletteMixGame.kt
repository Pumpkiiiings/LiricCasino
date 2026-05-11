package liric.casino.roulettemix

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import kotlin.random.Random

enum class BetColor(val displayName: String, val chatColor: String) {
    RED("<#FF5555><bold>ROJO</bold></#FF5555>", "<#FF5555>"),
    BLACK("<dark_gray><bold>NEGRO</bold></dark_gray>", "<dark_gray>"),
    GREEN("<#00FF7F><bold>VERDE</bold></#00FF7F>", "<#00FF7F>")
}

sealed class MixBetType {
    data class Number(val num: Int) : MixBetType()
    data class Color(val color: BetColor) : MixBetType()
}

enum class GameState { WAITING, COUNTDOWN, SPINNING }

class RouletteMixGame(private val plugin: CasinoPlugin) {

    var state = GameState.WAITING
        private set

    private val bets = mutableMapOf<UUID, Pair<MixBetType, Double>>()
    private var countdownTask: BukkitRunnable? = null

    // ─── Config helpers ──────────────────────────────────────────────────
    private fun cfg() = plugin.config
    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    private fun maxBet()         = cfg().getDouble("roulette.max-bet", 200000.0)
    private fun minPlayers()     = cfg().getInt("roulette.min-players-to-start", 1)
    private fun countdownSecs()  = cfg().getInt("roulette.countdown-seconds", 180)

    private fun colorMultiplier(color: BetColor): Double =
        cfg().getDouble("roulette.color-multipliers.${color.name}", 2.0)

    // ─── Hologramas ──────────────────────────────────────────────────────
    fun updateStatusHologram() {
        val text = "<#FF00FF><bold>🌀 RULETA MIXTA 🌀</bold></#FF00FF><br>" +
                "<#E0E0E0>Apuesta máxima: <#00FF7F>$${maxBet().toLong()}</#00FF7F></#E0E0E0><br>" +
                "<#FFB400>Jugadores actuales: ${bets.size}</#FFB400>"
        plugin.rouletteMixManager.updateHolograms(text)
    }

    // ─── Utilidad ─────────────────────────────────────────────────────────
    fun getNumberColor(number: Int): BetColor {
        if (number == 0) return BetColor.GREEN
        val redNumbers = setOf(1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36)
        return if (number in redNumbers) BetColor.RED else BetColor.BLACK
    }

    // ─── Apuesta ─────────────────────────────────────────────────────────
    fun addBet(player: Player, betType: MixBetType, amount: Double) {
        if (state == GameState.SPINNING) {
            player.sendMessage(msg("roulette.already-spinning"))
            return
        }
        if (bets.containsKey(player.uniqueId)) {
            player.sendMessage(msg("roulette.already-bet"))
            return
        }
        val max = maxBet()
        if (amount <= 0 || amount > max) {
            player.sendMessage(msg("roulette.bet-range", "max" to max.toLong().toString()))
            return
        }
        if (plugin.economyManager.withdrawPlayer(player, amount).transactionSuccess()) {
            bets[player.uniqueId] = Pair(betType, amount)
            plugin.statsManager.recordRouletteBet(player.uniqueId, amount)

            val targetName = when (betType) {
                is MixBetType.Number -> "al número ${getNumberColor(betType.num).chatColor}<bold>${betType.num}</bold>"
                is MixBetType.Color  -> "al color ${betType.color.chatColor}${betType.color.displayName}"
            }
            player.sendMessage(msg("roulette.bet-placed", "amount" to amount.toString(), "target" to targetName))
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

            updateStatusHologram()
            checkStartCondition()
        } else {
            player.sendMessage(msg("roulette.no-funds"))
        }
    }

    // ─── Lógica de inicio ─────────────────────────────────────────────────
    private fun checkStartCondition() {
        if (state == GameState.WAITING && bets.size >= minPlayers()) startCountdown()
    }

    private fun startCountdown() {
        state = GameState.COUNTDOWN
        var seconds = countdownSecs()

        plugin.server.broadcast(msg("roulette.spin-announce"))

        countdownTask = object : BukkitRunnable() {
            override fun run() {
                if (seconds <= 0) { startSpin(); cancel(); return }

                when (seconds) {
                    120 -> plugin.server.broadcast(msg("roulette.countdown-2min", "players" to bets.size.toString()))
                    60  -> plugin.server.broadcast(msg("roulette.countdown-1min"))
                    30  -> plugin.server.broadcast(msg("roulette.countdown-30s"))
                    10  -> plugin.server.broadcast(msg("roulette.countdown-10s"))
                }

                val holo = "<#FF00FF><bold>🌀 RULETA MIXTA 🌀</bold></#FF00FF><br>" +
                        "<#E0E0E0>Apuesta máxima: <#00FF7F>$${maxBet().toLong()}</#00FF7F></#E0E0E0><br>" +
                        "<#FF5555>¡Girando en $seconds segundos!</#FF5555>"
                plugin.rouletteMixManager.updateHolograms(holo.replace("\n", "<br>"))
                seconds--
            }
        }
        countdownTask?.runTaskTimer(plugin, 0L, 20L)
    }

    fun forceStart(sender: CommandSender) {
        if (state == GameState.SPINNING) return
        countdownTask?.cancel()
        startSpin()
    }

    // ─── Spin ─────────────────────────────────────────────────────────────
    private fun startSpin() {
        state = GameState.SPINNING
        plugin.server.broadcast(msg("roulette.spinning"))

        val spinHolo = "<#FF00FF><bold>🌀 RULETA MIXTA 🌀</bold></#FF00FF><br>" +
                "<#E0E0E0>¡No va más!</#E0E0E0><br>" +
                "<#FFB400><bold>¡Girando!</bold></#FFB400>"
        plugin.rouletteMixManager.setSpinningHologram(spinHolo)

        val resultNumber = Random.nextInt(0, 37)
        val winningColor = getNumberColor(resultNumber)

        plugin.rouletteMixManager.spinAllDisplays(140, resultNumber, winningColor) {
            finishGame(winningColor, resultNumber)
        }
    }

    private fun finishGame(winningColor: BetColor, resultNumber: Int) {
        plugin.server.broadcast(msg("roulette.result",
            "number" to resultNumber.toString(),
            "color"  to winningColor.displayName
        ))

        for ((uuid, betData) in bets) {
            val (betType, amount) = betData
            val player = Bukkit.getPlayer(uuid)

            var won = false
            var multiplier = 0.0

            when (betType) {
                is MixBetType.Number -> if (betType.num == resultNumber) { won = true; multiplier = 36.0 }
                is MixBetType.Color  -> if (betType.color == winningColor) { won = true; multiplier = colorMultiplier(betType.color) }
            }

            if (won) {
                var finalWin = amount * multiplier
                if (player != null && player.isOnline) {
                    val booster = plugin.economyManager.getPlayerBooster(player)
                    finalWin *= booster
                    plugin.economyManager.depositPlayer(player, finalWin)

                    val bMsg = if (booster > 1.0) plugin.messages.get("roulette.win-booster", "booster" to booster.toString()).let {
                        // convertir a raw string para concatenar en el mensaje win
                        plugin.messages.getRaw("roulette.win-booster").replace("{booster}", booster.toString())
                    } else ""

                    player.sendMessage(msg("roulette.win",
                        "amount"  to finalWin.toString(),
                        "booster" to bMsg
                    ))
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                } else {
                    plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(uuid), finalWin)
                }
                plugin.statsManager.recordRouletteWin(uuid, finalWin)
            } else {
                player?.sendMessage(msg("roulette.lose", "amount" to amount.toString()))
                plugin.statsManager.recordRouletteLoss(uuid)
            }
        }

        bets.clear()
        state = GameState.WAITING

        object : BukkitRunnable() {
            override fun run() { if (state == GameState.WAITING) plugin.rouletteMixManager.resetDisplays() }
        }.runTaskLater(plugin, 100L)
    }
}
