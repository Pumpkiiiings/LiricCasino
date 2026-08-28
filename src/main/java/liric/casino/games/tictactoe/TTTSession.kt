package liric.casino.games.tictactoe

import liric.casino.core.MatchmakingSession
import java.util.UUID
import kotlin.collections.get
import kotlin.hashCode

enum class TTTState { WAITING, PLAYING, FINISHED }
enum class TTTMark  { NONE, X, O }

data class TTTSession(
    override val id: UUID = UUID.randomUUID(),
    override val creatorId: UUID,
    override val creatorName: String,
    override val betAmount: Double,
    override var joinerId: UUID? = null,
    override var joinerName: String? = null,
    var state: TTTState = TTTState.WAITING,
    val board: Array<TTTMark> = Array(9) { TTTMark.NONE },
    var currentTurn: UUID = creatorId,
    var creatorMark: TTTMark = TTTMark.X,
    var joinerMark: TTTMark = TTTMark.O
) : MatchmakingSession {
    override fun isOpen(): Boolean = state == TTTState.WAITING
    fun checkWinner(): TTTMark {
        val lines = arrayOf(
            intArrayOf(0,1,2), intArrayOf(3,4,5), intArrayOf(6,7,8),
            intArrayOf(0,3,6), intArrayOf(1,4,7), intArrayOf(2,5,8),
            intArrayOf(0,4,8), intArrayOf(2,4,6)
        )
        for (line in lines) {
            val (a, b, c) = line
            if (board[a] != TTTMark.NONE && board[a] == board[b] && board[b] == board[c])
                return board[a]
        }
        return TTTMark.NONE
    }

    fun isFull() = board.none { it == TTTMark.NONE }

    fun getMarkOf(uuid: UUID) = if (uuid == creatorId) creatorMark else joinerMark

    override fun equals(other: Any?) = other is TTTSession && id == other.id
    override fun hashCode() = id.hashCode()
}
