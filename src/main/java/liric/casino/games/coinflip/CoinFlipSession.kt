package liric.casino.games.coinflip

import liric.casino.core.MatchmakingSession
import java.util.UUID

enum class CoinFlipState { WAITING, ANIMATING, FINISHED }

data class CoinFlipSession(
    override val id: UUID = UUID.randomUUID(),
    override val creatorId: UUID,
    override val creatorName: String,
    override val betAmount: Double,
    override var joinerId: UUID? = null,
    override var joinerName: String? = null,
    var state: CoinFlipState = CoinFlipState.WAITING,
    val createdAt: Long = System.currentTimeMillis()
) : MatchmakingSession {
    override fun isOpen(): Boolean = state == CoinFlipState.WAITING
}
