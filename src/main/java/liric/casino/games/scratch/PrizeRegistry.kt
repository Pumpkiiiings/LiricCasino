package liric.casino.games.scratch

import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import kotlin.collections.get

data class ScratchPrize(
    val material: Material,
    val displayName: String,
    val basePayout: Double,
    val weight: Int
)


class PrizeRegistry(private val prizesSection: List<Map<*, *>>) {

    val prizes: List<ScratchPrize> = loadPrizes()

    private fun loadPrizes(): List<ScratchPrize> {
        return prizesSection.mapNotNull { entry ->
            runCatching {
                val matName = entry["material"]?.toString() ?: return@mapNotNull null
                val mat     = Material.valueOf(matName.uppercase())
                val name    = entry["display-name"]?.toString() ?: matName
                val payout  = (entry["base-payout"] as? Number)?.toDouble() ?: 0.0
                val weight  = (entry["weight"] as? Number)?.toInt() ?: 1
                ScratchPrize(mat, name, payout, weight)
            }.getOrNull()
        }
    }

    fun getRandomPrize(): ScratchPrize {
        val totalWeight = prizes.sumOf { it.weight }
        var random = (0 until totalWeight).random()
        for (prize in prizes) {
            if (random < prize.weight) return prize
            random -= prize.weight
        }
        return prizes.last()
    }

    companion object {

        fun fromConfig(config: FileConfiguration): PrizeRegistry {
            @Suppress("UNCHECKED_CAST")
            val list = config.getList("scratch.prizes") as? List<Map<*, *>> ?: emptyList()
            return PrizeRegistry(list)
        }
    }
}
