package liric.casino.games.lottery

import java.util.UUID

data class LotteryTicket(
    val id: UUID = UUID.randomUUID(),
    val ownerUuid: UUID,
    val ownerName: String,
    val number: Int,
    val purchasedAt: Long = System.currentTimeMillis()
)
