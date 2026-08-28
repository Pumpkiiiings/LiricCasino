package liric.casino.games.rps

import liric.casino.core.MatchmakingSession
import java.util.UUID

enum class RPSChoice(val emoji: String, val displayName: String) {
    ROCK("🪨", "Rock"),
    PAPER("📄", "Paper"),
    SCISSORS("✂", "Scissors");

    fun beats(other: RPSChoice) = (this == ROCK && other == SCISSORS) ||
                                  (this == SCISSORS && other == PAPER)  ||
                                  (this == PAPER  && other == ROCK)
}

enum class RPSState { WAITING, CHOOSING, FINISHED }

data class RPSSession(
    override val id: UUID = UUID.randomUUID(),
    override val creatorId: UUID,
    override val creatorName: String,
    override val betAmount: Double,
    override var joinerId: UUID? = null,
    override var joinerName: String? = null,
    var creatorChoice: RPSChoice? = null,
    var joinerChoice: RPSChoice? = null,
    var state: RPSState = RPSState.WAITING
) : MatchmakingSession {
    override fun isOpen(): Boolean = state == RPSState.WAITING
}
