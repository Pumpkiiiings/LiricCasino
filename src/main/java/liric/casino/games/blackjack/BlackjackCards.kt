package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

enum class BjSuit(val symbol: String, val color: String) {
    HEARTS("♥", "<#FF5555>"),
    DIAMONDS("♦", "<#FF5555>"),
    CLUBS("♣", "<dark_gray>"),
    SPADES("♠", "<black>")
}

enum class BjRank(val display: String, val value: Int) {
    TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5), SIX("6", 6),
    SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9), TEN("10", 10),
    JACK("J", 10), QUEEN("Q", 10), KING("K", 10), ACE("A", 11)
}

data class BjCard(val rank: BjRank, val suit: BjSuit) {
    fun toItemStack(plugin: CasinoPlugin, hidden: Boolean = false): ItemStack {
        if (hidden) {
            return ItemBuilder.from(Material.MAP)
                .name(plugin.format("<#FFB400><bold>🂠 CARTA OCULTA</bold>"))
                .lore(plugin.format("<gray>El Dealer revelará esta carta"), plugin.format("<gray>cuando sea su turno."))
                .flags(*ItemFlag.values()).build()
        }
        return ItemBuilder.from(Material.PAPER)
            .name(plugin.format("${suit.color}<bold>${rank.display} ${suit.symbol}</bold>"))
            .lore(plugin.format("<gray>Valor: <white>${if (rank == BjRank.ACE) "1 u 11" else rank.value}"))
            .flags(*ItemFlag.values()).build()
    }
}

class BjDeck {
    private val cards = mutableListOf<BjCard>()
    init {
        for (suit in BjSuit.values()) {
            for (rank in BjRank.values()) {
                cards.add(BjCard(rank, suit))
            }
        }
        cards.shuffle()
    }
    fun draw(): BjCard = cards.removeAt(0)
}

object BlackjackLogic {
    // Suma los puntos y ajusta los Ases automáticamente para no pasarse de 21
    fun calculateScore(hand: List<BjCard>): Int {
        var sum = hand.sumOf { it.rank.value }
        var aces = hand.count { it.rank == BjRank.ACE }

        while (sum > 21 && aces > 0) {
            sum -= 10
            aces--
        }
        return sum
    }
}
