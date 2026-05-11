package liric.casino.slots

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

data class SlotPrize(
    val material: Material,
    val displayName: String,
    val multiplier: Double,
    val weight: Int
)

/**
 * Registry de premios del tragamonedas, cargado desde config.yml (slots.prizes).
 * Ya no es un object estático — se instancia con la sección de config.
 */
class SlotRegistry(private val prizesSection: List<Map<*, *>>, private val boostersMap: Map<String, Double>) {

    val items: List<SlotPrize> = loadPrizes()
    val luckBoosters: Map<String, Double> = boostersMap

    private fun loadPrizes(): List<SlotPrize> {
        return prizesSection.mapNotNull { entry ->
            runCatching {
                val matName = entry["material"]?.toString() ?: return@mapNotNull null
                val mat     = Material.valueOf(matName.uppercase())
                val name    = entry["display-name"]?.toString() ?: matName
                val mult    = (entry["multiplier"] as? Number)?.toDouble() ?: 0.0
                val weight  = (entry["weight"] as? Number)?.toInt() ?: 1
                SlotPrize(mat, name, mult, weight)
            }.getOrNull()
        }
    }

    fun getRandomItem(player: Player): SlotPrize {
        var currentLuck = 1.0
        for ((permission, luckMultiplier) in luckBoosters) {
            if (player.hasPermission(permission) && luckMultiplier > currentLuck) {
                currentLuck = luckMultiplier
            }
        }

        val dynamicWeights = items.map { prize ->
            if (prize.multiplier >= 1.0) (prize.weight * currentLuck).toInt() else prize.weight
        }

        val totalWeight = dynamicWeights.sum()
        var random = (0 until totalWeight).random()
        for (i in items.indices) {
            if (random < dynamicWeights[i]) return items[i]
            random -= dynamicWeights[i]
        }
        return items.last()
    }

    companion object {
        /** Crea un SlotRegistry desde la configuración del plugin. */
        fun fromConfig(config: org.bukkit.configuration.file.FileConfiguration): SlotRegistry {
            @Suppress("UNCHECKED_CAST")
            val prizes   = config.getList("slots.prizes") as? List<Map<*, *>> ?: emptyList()
            val boostersSection = config.getConfigurationSection("slots.luck-boosters")
            val boosters = boostersSection?.getKeys(false)?.associateWith { boostersSection.getDouble(it) } ?: emptyMap()
            return SlotRegistry(prizes, boosters)
        }
    }
}
