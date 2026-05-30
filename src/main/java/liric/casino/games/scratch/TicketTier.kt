package liric.casino.games.scratch

import org.bukkit.configuration.file.FileConfiguration


enum class TicketTier(val id: String) {
    BASIC("basico"),
    ADVANCED("avanzado"),
    MASTER("maestro"),
    GIANT("gigante");

    var displayName: String = id
    var price: Double = 1000.0
    var rows: Int = 3
    var matchRequired: Int = 3
    var payoutMultiplier: Double = 1.0


    fun getScratchableSlots(): List<Int> {
        return when (this) {
            BASIC    -> listOf(3, 4, 5, 12, 13, 14, 21, 22, 23)
            ADVANCED -> (0..4).flatMap { r -> (2..6).map { c -> r * 9 + c } }
            MASTER   -> (0..4).flatMap { r -> (1..7).map { c -> r * 9 + c } }
            GIANT    -> (0..53).toList()
        }
    }

    companion object {

        fun loadFromConfig(config: FileConfiguration) {
            values().forEach { tier ->
                val path = "scratch.tiers.${tier.id}"
                if (config.contains(path)) {
                    tier.displayName      = config.getString("$path.display-name", tier.id) ?: tier.id
                    tier.price            = config.getDouble("$path.price", tier.price)
                    tier.rows             = config.getInt("$path.rows", tier.rows)
                    tier.matchRequired    = config.getInt("$path.match-required", tier.matchRequired)
                    tier.payoutMultiplier = config.getDouble("$path.payout-multiplier", tier.payoutMultiplier)
                }
            }
        }
    }
}
