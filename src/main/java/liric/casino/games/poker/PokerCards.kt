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
                .name(plugin.format("<#FFB400><bold>🂠 CARTA OCULTA</bold>"))
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

    fun evaluate(holeCards: List<Card>, communityCards: List<Card>): Long {
        val all = holeCards + communityCards
        if (all.size < 5) return 0L

        val ranks = all.groupBy { it.rank.value }.mapValues { it.value.size }
        val suits = all.groupBy { it.suit }.mapValues { it.value.size }

        val isFlush = suits.values.any { it >= 5 }
        val sortedRanks = all.map { it.rank.value }.distinct().sortedDescending()
        var isStraight = false

        for (i in 0..sortedRanks.size - 5) {
            if (sortedRanks[i] - sortedRanks[i + 4] == 4) {
                isStraight = true; break
            }
        }

        val pairs = ranks.filter { it.value == 2 }.keys.sortedDescending()
        val trips = ranks.filter { it.value == 3 }.keys.sortedDescending()
        val quads = ranks.filter { it.value == 4 }.keys.firstOrNull()

        var score = 0L
        val highCard = sortedRanks.firstOrNull() ?: 0

        when {
            isFlush && isStraight -> score = 8000000L + highCard
            quads != null -> score = 7000000L + quads
            trips.isNotEmpty() && pairs.isNotEmpty() -> score = 6000000L + trips.first()
            isFlush -> score = 5000000L + highCard
            isStraight -> score = 4000000L + highCard
            trips.isNotEmpty() -> score = 3000000L + trips.first()
            pairs.size >= 2 -> score = 2000000L + (pairs[0] * 100) + pairs[1]
            pairs.size == 1 -> score = 1000000L + pairs.first()
            else -> score = highCard.toLong()
        }
        return score
    }

    fun getHandName(score: Long): String {
        return when (score / 1000000L) {
            8L -> "Escalera de Color"
            7L -> "Póker (4 Iguales)"
            6L -> "Full House"
            5L -> "Color (Flush)"
            4L -> "Escalera"
            3L -> "Trío"
            2L -> "Doble Par"
            1L -> "Un Par"
            else -> "Carta Alta"
        }
    }
}
