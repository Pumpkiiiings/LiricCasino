package liric.casino.games.racing

import org.bukkit.Location
import java.util.UUID

data class Horse(
    val id: Int,
    val name: String,
    val emoji: String,
    val oddsMult: Double,  // multiplicador si gana (ej. 3.0 = x3)
    val winChance: Int     // peso de probabilidad (mayor = más probable de ganar)
)

data class RaceTrack(
    val id: UUID = UUID.randomUUID(),
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double
) {
    fun toKey() = "$world,$x,$y,$z"
}

enum class RaceState { WAITING, RACING, FINISHED }

data class RacePlayerBet(
    val playerId: UUID,
    val playerName: String,
    val horseId: Int,
    val amount: Double
)

data class RaceSession(
    val trackId: UUID,
    var state: RaceState = RaceState.WAITING,
    val bets: MutableList<RacePlayerBet> = mutableListOf(),
    var winnerHorseId: Int = -1,
    var countdownSeconds: Int = 30
)
