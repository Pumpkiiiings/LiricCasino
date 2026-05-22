package liric.casino.games.coinflip

import java.util.UUID

enum class CoinFlipState { WAITING, ANIMATING, FINISHED }

data class CoinFlipSession(
    val id: UUID = UUID.randomUUID(),
    val creatorId: UUID,
    val creatorName: String,
    val betAmount: Double,
    var joinerId: UUID? = null,
    var joinerName: String? = null,
    var state: CoinFlipState = CoinFlipState.WAITING,
    val createdAt: Long = System.currentTimeMillis()
)
