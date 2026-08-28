package liric.casino.games.rps

import java.util.UUID

enum class RPSChoice(val emoji: String, val displayName: String) {
    PIEDRA("🪨", "Piedra"),
    PAPEL("📄", "Papel"),
    TIJERA("✂", "Tijera");

    fun beats(other: RPSChoice) = (this == PIEDRA && other == TIJERA) ||
                                  (this == TIJERA && other == PAPEL)  ||
                                  (this == PAPEL  && other == PIEDRA)
}

enum class RPSState { WAITING, CHOOSING, FINISHED }

data class RPSSession(
    val id: UUID = UUID.randomUUID(),
    val creatorId: UUID,
    val creatorName: String,
    val betAmount: Double,
    var joinerId: UUID? = null,
    var joinerName: String? = null,
    var creatorChoice: RPSChoice? = null,
    var joinerChoice: RPSChoice? = null,
    var state: RPSState = RPSState.WAITING
)
