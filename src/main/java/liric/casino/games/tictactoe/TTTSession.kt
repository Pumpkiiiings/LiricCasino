package liric.casino.games.tictactoe

import java.util.UUID
import kotlin.collections.get
import kotlin.hashCode

enum class TTTState { WAITING, PLAYING, FINISHED }
enum class TTTMark  { NONE, X, O }

data class TTTSession(
    val id: UUID = UUID.randomUUID(),
    val creatorId: UUID,
    val creatorName: String,
    val betAmount: Double,
    var joinerId: UUID? = null,
    var joinerName: String? = null,
    var state: TTTState = TTTState.WAITING,
    val board: Array<TTTMark> = Array(9) { TTTMark.NONE },  // índices 0-8
    var currentTurn: UUID = creatorId,  // X siempre empieza (creator)
    var creatorMark: TTTMark = TTTMark.X,
    var joinerMark: TTTMark = TTTMark.O
) {
    fun checkWinner(): TTTMark {
        val lines = arrayOf(
            intArrayOf(0,1,2), intArrayOf(3,4,5), intArrayOf(6,7,8), // filas
            intArrayOf(0,3,6), intArrayOf(1,4,7), intArrayOf(2,5,8), // columnas
            intArrayOf(0,4,8), intArrayOf(2,4,6)                     // diagonales
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
