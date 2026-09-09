package liric.casino.games.poker

import dev.triumphteam.gui.builder.item.ItemBuilder
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

enum class Suit(val symbol: String, val color: String) {
    HEARTS("♥", "<#FF5555>"),
    DIAMONDS("♦", "<#FF5555>"),
    CLUBS("♣", "<dark_gray>"),
    SPADES("♠", "<black>")
}

enum class Rank(val value: Int, val display: String) {
    TWO(2, "2"), THREE(3, "3"), FOUR(4, "4"), FIVE(5, "5"), SIX(6, "6"),
    SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"), TEN(10, "10"),
    JACK(11, "J"), QUEEN(12, "Q"), KING(13, "K"), ACE(14, "A")
}

data class Card(val rank: Rank, val suit: Suit) {
    fun toItemStack(plugin: CasinoPlugin, hidden: Boolean = false): ItemStack {
        if (hidden) {
            return ItemBuilder.from(Material.MAP)
                .name(plugin.format("<#FFB400><bold>🂠 HIDDEN CARD</bold>"))
                .flags(*ItemFlag.values()).build()
        }
        return ItemBuilder.from(Material.PAPER)
            .name(plugin.format("${suit.color}<bold>${rank.display} ${suit.symbol}</bold>"))
            .flags(*ItemFlag.values()).build()
    }
}

class Deck {
    private val cards = mutableListOf<Card>()
    init {
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                cards.add(Card(rank, suit))
            }
        }
        cards.shuffle()
    }
    fun draw(): Card = cards.removeAt(0)
}

object HandEvaluator {

    private const val CATEGORY_BASE = 10_000_000_000L

    fun evaluate(holeCards: List<Card>, communityCards: List<Card>): Long {
        val all = holeCards + communityCards
        if (all.size < 5) return 0L
        var best = 0L
        for (a in 0 until all.size - 4)
            for (b in a + 1 until all.size - 3)
                for (c in b + 1 until all.size - 2)
                    for (d in c + 1 until all.size - 1)
                        for (e in d + 1 until all.size) {
                            best = maxOf(best, evaluateFive(listOf(all[a], all[b], all[c], all[d], all[e])))
                        }
        return best
    }

    private fun evaluateFive(cards: List<Card>): Long {
        val groups = cards.groupBy { it.rank.value }
        val orderedGroups = groups.entries.sortedWith(compareByDescending<Map.Entry<Int, List<Card>>> { it.value.size }.thenByDescending { it.key })
        val ranksDesc = cards.map { it.rank.value }.sortedDescending()
        val flush = cards.map { it.suit }.distinct().size == 1
        val distinct = ranksDesc.distinct()
        val straightHigh = when {
            distinct == listOf(14, 5, 4, 3, 2) -> 5
            distinct.size == 5 && distinct.first() - distinct.last() == 4 -> distinct.first()
            else -> null
        }

        return when {
            flush && straightHigh != null -> encode(8, listOf(straightHigh))
            orderedGroups[0].value.size == 4 -> encode(7, listOf(orderedGroups[0].key, orderedGroups[1].key))
            orderedGroups[0].value.size == 3 && orderedGroups[1].value.size == 2 -> encode(6, listOf(orderedGroups[0].key, orderedGroups[1].key))
            flush -> encode(5, ranksDesc)
            straightHigh != null -> encode(4, listOf(straightHigh))
            orderedGroups[0].value.size == 3 -> encode(3, listOf(orderedGroups[0].key) + orderedGroups.drop(1).map { it.key }.sortedDescending())
            orderedGroups[0].value.size == 2 && orderedGroups[1].value.size == 2 -> {
                val pairs = orderedGroups.take(2).map { it.key }.sortedDescending()
                encode(2, pairs + orderedGroups[2].key)
            }
            orderedGroups[0].value.size == 2 -> encode(1, listOf(orderedGroups[0].key) + orderedGroups.drop(1).map { it.key }.sortedDescending())
            else -> encode(0, ranksDesc)
        }
    }

    private fun encode(category: Int, ranks: List<Int>): Long {
        var detail = 0L
        ranks.take(5).forEach { detail = detail * 15L + it }
        repeat(5 - ranks.take(5).size) { detail *= 15L }
        return category * CATEGORY_BASE + detail
    }

    fun getHandName(score: Long): String {
        return when (score / CATEGORY_BASE) {
            8L -> "Straight Flush"
            7L -> "Four of a Kind"
            6L -> "Full House"
            5L -> "Flush"
            4L -> "Straight"
            3L -> "Three of a Kind"
            2L -> "Two Pair"
            1L -> "One Pair"
            else -> "High Card"
        }
    }
}
