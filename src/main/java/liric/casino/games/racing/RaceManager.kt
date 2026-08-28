package liric.casino.games.racing

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractGameManager
import liric.casino.util.SchedulerUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player

class RaceManager(plugin: CasinoPlugin) : AbstractGameManager<RaceSession>(plugin, "racing") {

    private val tracks = mutableListOf<RaceTrack>()
    private var defaultHorses = listOf(
        Horse(1, "Lightning", "⚡", 2.0, 40),
        Horse(2, "Shadow", "🌑", 3.0, 30),
        Horse(3, "Tornado", "🌪", 5.0, 20),
        Horse(4, "Comet", "☄", 10.0, 10)
    )

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    fun getHorses() = defaultHorses

    fun createTrack(loc: Location): RaceTrack {
        val track = RaceTrack(world = loc.world.name, x = loc.x, y = loc.y, z = loc.z)
        tracks.add(track)
        addSession(track.id, RaceSession(track.id))
        return track
    }

    fun getNearestTrack(loc: Location): RaceTrack? {
        return tracks.filter { it.world == loc.world.name }
            .minByOrNull {
                val dx = it.x - loc.x
                val dy = it.y - loc.y
                val dz = it.z - loc.z
                dx*dx + dy*dy + dz*dz
            }
    }

    fun deleteTrack(track: RaceTrack) {
        tracks.remove(track)
        removeSession(track.id)
    }

    fun getSession(trackId: java.util.UUID): RaceSession? = activeSessions[trackId]

    fun placeBet(player: Player, track: RaceTrack, horseId: Int, amount: Double) {
        val session = getSession(track.id) ?: return

        if (session.state == RaceState.RACING) {
            player.sendMessage(msg("racing.already-started"))
            return
        }

        if (!canPlay(player, amount)) return

        if (session.bets.any { it.playerId == player.uniqueId }) {
            player.sendMessage(msg("racing.already-bet"))
            return
        }

        if (!takeBet(player, amount)) return

        val horse = defaultHorses.firstOrNull { it.id == horseId } ?: return

        plugin.statsManager.recordGameUse(player.uniqueId, "racing")
        session.bets.add(RacePlayerBet(player.uniqueId, player.name, horseId, amount))

        player.sendMessage(msg("racing.bet-placed",
            "horse" to "${horse.emoji} ${horse.name}",
            "amount" to amount.toLong().toString()
        ))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

        if (session.state == RaceState.WAITING && session.bets.size == 1) {
            startCountdown(session)
        }
    }

    private fun startCountdown(session: RaceSession) {
        session.state = RaceState.WAITING
        session.countdownSeconds = 30

        var countdownCancel: Runnable? = null
        countdownCancel = SchedulerUtil.runGlobalTimer(plugin, 0L, 20L) {
            if (session.state != RaceState.WAITING) {
                countdownCancel?.run()
                return@runGlobalTimer
            }

            if (session.countdownSeconds <= 0) {
                startRace(session)
                countdownCancel?.run()
                return@runGlobalTimer
            }

            if (session.countdownSeconds % 10 == 0 || session.countdownSeconds <= 5) {
                broadcastToBettors(session, "racing.countdown", "time" to session.countdownSeconds.toString())
            }

            session.countdownSeconds--
        }
    }

    private fun startRace(session: RaceSession) {
        session.state = RaceState.RACING
        broadcastToBettors(session, "racing.started")

        var ticks = 0
        var raceCancel: Runnable? = null
        raceCancel = SchedulerUtil.runGlobalTimer(plugin, 0L, 20L) {
            ticks++
            if (ticks >= 5) {
                finishRace(session)
                raceCancel?.run()
            } else {
                session.bets.mapNotNull { Bukkit.getPlayer(it.playerId) }.forEach { p ->
                    p.playSound(p.location, Sound.ENTITY_HORSE_GALLOP, 1f, 1f + (ticks * 0.1f))
                }
            }
        }
    }

    private fun finishRace(session: RaceSession) {
        session.state = RaceState.FINISHED

        val totalWeight = defaultHorses.sumOf { it.winChance }
        var randomVal = kotlin.random.Random.nextInt(totalWeight)
        var winnerHorse = defaultHorses.first()

        for (horse in defaultHorses) {
            randomVal -= horse.winChance
            if (randomVal < 0) {
                winnerHorse = horse
                break
            }
        }

        session.winnerHorseId = winnerHorse.id
        broadcastToBettors(session, "racing.winner", "horse" to "${winnerHorse.emoji} ${winnerHorse.name}")

        session.bets.forEach { bet ->
            val player = Bukkit.getPlayer(bet.playerId)
            if (bet.horseId == winnerHorse.id) {
                val rawWin = bet.amount * winnerHorse.oddsMult
                
                if (player != null) {
                    processWin(player, bet.amount, winnerHorse.oddsMult)
                    player.sendMessage(msg("racing.win", "amount" to rawWin.toLong().toString(), "tax" to "")) 
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                    plugin.webhook.sendBigWin("Racing", player.name, rawWin)
                } else {
                    val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, rawWin, "racing")
                    plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(bet.playerId), netWin)
                }
                plugin.statsManager.recordRacingWin(bet.playerId, rawWin)

            } else {
                plugin.statsManager.recordRacingLoss(bet.playerId, bet.amount)
                player?.let {
                    it.sendMessage(msg("racing.lose", "amount" to bet.amount.toLong().toString()))
                    it.playSound(it.location, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f)
                }
            }
        }

        session.bets.clear()
        session.winnerHorseId = -1
        session.state = RaceState.WAITING
    }

    private fun broadcastToBettors(session: RaceSession, msgKey: String, vararg ph: Pair<String, String>) {
        val message = msg(msgKey, *ph)
        session.bets.mapNotNull { Bukkit.getPlayer(it.playerId) }.forEach { p ->
            p.sendMessage(message)
        }
    }

    fun cleanupAll() {
        activeSessions.clear()
        tracks.clear()
    }
}
