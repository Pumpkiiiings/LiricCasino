package liric.casino.games.racing

import liric.casino.CasinoPlugin
import liric.casino.core.AbstractGameManager
import liric.casino.util.SchedulerUtil
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Horse as BukkitHorse
import org.bukkit.entity.Player
import java.io.File
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class RaceManager(plugin: CasinoPlugin) : AbstractGameManager<RaceSession>(plugin, "racing") {

    private val tracks = mutableListOf<RaceTrack>()
    private val raceHorses = mutableMapOf<UUID, MutableList<BukkitHorse>>()
    private val dataFile = File(plugin.dataFolder, "data.yml")
    private var defaultHorses = listOf(
        Horse(1, "Lightning", "⚡", 2.0, 40),
        Horse(2, "Shadow", "🌑", 3.0, 30),
        Horse(3, "Tornado", "🌪", 5.0, 20),
        Horse(4, "Comet", "☄", 10.0, 10)
    )

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    init {
        loadTracks()
    }

    fun getHorses() = defaultHorses

    fun createTrack(loc: Location): RaceTrack {
        val track = RaceTrack(world = loc.world.name, x = loc.blockX + 0.5, y = loc.blockY.toDouble(), z = loc.blockZ + 0.5, yaw = loc.yaw)
        tracks.add(track)
        addSession(track.id, RaceSession(track.id))
        saveTracks()
        return track
    }

    fun getNearestTrack(loc: Location): RaceTrack? {
        val radius = plugin.config.getDouble("racing.track-access-radius", 24.0).coerceAtLeast(4.0)
        return tracks.filter { it.world == loc.world.name }
            .filter {
                val dx = it.x - loc.x
                val dy = it.y - loc.y
                val dz = it.z - loc.z
                dx * dx + dy * dy + dz * dz <= radius * radius
            }
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
        removeRaceHorses(track.id)
        saveTracks()
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

        val horse = defaultHorses.firstOrNull { it.id == horseId } ?: return
        if (!takeBet(player, amount)) return

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
        session.countdownSeconds = plugin.config.getInt("racing.countdown-seconds", 15).coerceAtLeast(3)

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

        val track = tracks.firstOrNull { it.id == session.trackId } ?: run {
            refundSession(session)
            return
        }
        val winnerHorse = chooseWinner()
        val spawned = spawnRaceHorses(track)
        if (spawned.isEmpty()) {
            refundSession(session)
            return
        }

        var ticks = 0
        val duration = plugin.config.getInt("racing.duration-seconds", 8).coerceAtLeast(3)
        var raceCancel: Runnable? = null
        raceCancel = SchedulerUtil.runGlobalTimer(plugin, 0L, 20L) {
            ticks++
            moveRaceHorses(track, spawned, winnerHorse.id, ticks, duration)
            if (ticks >= duration) {
                finishRace(session, winnerHorse)
                raceCancel?.run()
            } else {
                session.bets.mapNotNull { Bukkit.getPlayer(it.playerId) }.forEach { p ->
                    p.playSound(p.location, Sound.ENTITY_HORSE_GALLOP, 1f, 1f + (ticks * 0.1f))
                }
            }
        }
    }

    private fun finishRace(session: RaceSession, winnerHorse: Horse) {
        session.state = RaceState.FINISHED

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
        SchedulerUtil.runGlobalLater(plugin, 60L) { removeRaceHorses(session.trackId) }
    }

    private fun chooseWinner(): Horse {
        val totalWeight = defaultHorses.sumOf { it.winChance }.coerceAtLeast(1)
        var value = kotlin.random.Random.nextInt(totalWeight)
        return defaultHorses.first { horse ->
            value -= horse.winChance
            value < 0
        }
    }

    private fun spawnRaceHorses(track: RaceTrack): MutableList<BukkitHorse> {
        removeRaceHorses(track.id)
        val world = Bukkit.getWorld(track.world) ?: return mutableListOf()
        val yawRadians = Math.toRadians(track.yaw.toDouble())
        val sideX = cos(yawRadians)
        val sideZ = sin(yawRadians)
        val horses = defaultHorses.mapIndexed { index, horseData ->
            val laneOffset = (index - (defaultHorses.size - 1) / 2.0) * 2.0
            val location = Location(world, track.x + sideX * laneOffset, track.y, track.z + sideZ * laneOffset, track.yaw, 0f)
            world.spawn(location, BukkitHorse::class.java).apply {
                customName(plugin.format("${horseData.emoji} <white>${horseData.name}</white>"))
                isCustomNameVisible = true
                isInvulnerable = true
                isSilent = true
                isPersistent = false
                setAI(false)
                getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.3
            }
        }.toMutableList()
        raceHorses[track.id] = horses
        return horses
    }

    private fun moveRaceHorses(track: RaceTrack, horses: List<BukkitHorse>, winnerId: Int, tick: Int, duration: Int) {
        val yawRadians = Math.toRadians(track.yaw.toDouble())
        val forwardX = -sin(yawRadians)
        val forwardZ = cos(yawRadians)
        val progress = tick.toDouble() / duration
        val distance = plugin.config.getDouble("racing.track-length", 24.0).coerceAtLeast(8.0)
        horses.forEachIndexed { index, entity ->
            if (!entity.isValid) return@forEachIndexed
            val horseId = defaultHorses.getOrNull(index)?.id ?: return@forEachIndexed
            val finishBias = if (horseId == winnerId) 1.0 else (0.82 + horseId * 0.025).coerceAtMost(0.94)
            val side = (index - (horses.size - 1) / 2.0) * 2.0
            val x = track.x + cos(yawRadians) * side + forwardX * distance * progress * finishBias
            val z = track.z + sin(yawRadians) * side + forwardZ * distance * progress * finishBias
            entity.teleport(Location(entity.world, x, track.y, z, track.yaw, 0f))
        }
    }

    private fun removeRaceHorses(trackId: UUID) {
        raceHorses.remove(trackId)?.forEach { if (it.isValid) it.remove() }
    }

    private fun refundSession(session: RaceSession) {
        broadcastToBettors(session, "racing.cancelled")
        session.bets.forEach { plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(it.playerId), it.amount) }
        session.bets.clear()
        session.state = RaceState.WAITING
    }

    private fun loadTracks() {
        val data = YamlConfiguration.loadConfiguration(dataFile)
        data.getMapList("racing.tracks").forEach { raw ->
            val world = raw["world"]?.toString() ?: return@forEach
            val id = runCatching { UUID.fromString(raw["id"]?.toString()) }.getOrNull() ?: UUID.randomUUID()
            val track = RaceTrack(id, world, raw["x"].toString().toDouble(), raw["y"].toString().toDouble(), raw["z"].toString().toDouble(), raw["yaw"]?.toString()?.toFloatOrNull() ?: 0f)
            tracks.add(track)
            addSession(track.id, RaceSession(track.id))
        }
    }

    private fun saveTracks() {
        val data = YamlConfiguration.loadConfiguration(dataFile)
        data.set("racing.tracks", tracks.map { mapOf("id" to it.id.toString(), "world" to it.world, "x" to it.x, "y" to it.y, "z" to it.z, "yaw" to it.yaw) })
        data.save(dataFile)
    }

    private fun broadcastToBettors(session: RaceSession, msgKey: String, vararg ph: Pair<String, String>) {
        val message = msg(msgKey, *ph)
        session.bets.mapNotNull { Bukkit.getPlayer(it.playerId) }.forEach { p ->
            p.sendMessage(message)
        }
    }

    fun cleanupAll() {
        activeSessions.values.forEach { session ->
            session.bets.forEach { bet ->
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(bet.playerId), bet.amount)
            }
            session.bets.clear()
        }
        activeSessions.clear()
        raceHorses.keys.toList().forEach(::removeRaceHorses)
    }
}
