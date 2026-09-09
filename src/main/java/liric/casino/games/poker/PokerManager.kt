package liric.casino.games.poker

import liric.casino.CasinoPlugin
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class PokerManager(private val plugin: CasinoPlugin) {
    val pokerKey = NamespacedKey(plugin, "casino_poker_id")
    private val activeTables = mutableListOf<TextDisplay>()
    private val dataFile = File(plugin.dataFolder, "data.yml")

    init {
        cleanupAll()
    }

    fun loadTables() {
        val data = YamlConfiguration.loadConfiguration(dataFile)
        data.getStringList("poker.tables").forEach { serialized ->
            parseLocation(serialized)?.let { spawnTable(it, false) }
        }
    }

    fun spawnTable(location: Location, save: Boolean = true) {
        val center = if (save) location.clone().apply {
            x = blockX + 0.5
            y = blockY.toDouble() + 1.2
            z = blockZ + 0.5
        } else location.clone()

        val textDisplay = center.world.spawnEntity(center, EntityType.TEXT_DISPLAY) as TextDisplay
        textDisplay.persistentDataContainer.set(pokerKey, PersistentDataType.BYTE, 1.toByte())
        textDisplay.billboard = Display.Billboard.CENTER
        textDisplay.backgroundColor = Color.fromARGB(0, 0, 0, 0)
        textDisplay.isShadowed = true
        activeTables.add(textDisplay)


        val interaction = center.world.spawnEntity(center, EntityType.INTERACTION) as Interaction
        interaction.persistentDataContainer.set(pokerKey, PersistentDataType.BYTE, 1.toByte())
        interaction.interactionWidth = 2.0f
        interaction.interactionHeight = 2.0f

        updateHolograms()
        if (save) saveTables()
    }

    fun updateHolograms() {
        val game = plugin.pokerGame



        val text = when (game.state) {
            PokerState.WAITING -> """
                <#FF0000><bold>♠ POKER TABLE ♠</bold></#FF0000>
                <#E0E0E0>Right Click to join</#E0E0E0>
                <#00FF7F>Players: ${game.players.size}/${game.maxPlayers}</#00FF7F>
                <#FFB400>Entry: $${game.entryFee}</#FFB400>
            """.trimIndent().replace("\n", "<br>")

            PokerState.STARTING -> """
                <#FF0000><bold>♠ POKER TABLE ♠</bold></#FF0000>
                <#E0E0E0>Starting in <#FFB400>${game.countdownSeconds}s</#FFB400></#E0E0E0>
                <#00FF7F>Players: ${game.players.size}/${game.maxPlayers}</#00FF7F>
            """.trimIndent().replace("\n", "<br>")

            else -> """
                <#FF0000><bold>♠ POKER TABLE ♠</bold></#FF0000>
                <#FF5555>Phase: ${game.state.name}</#FF5555>
                <#00FF7F>Playing: ${game.players.size}</#00FF7F>
                <#FFD700>Total Pot: $${game.pot}</#FFD700>
            """.trimIndent().replace("\n", "<br>")
        }

        activeTables.forEach {
            if (!it.isDead) it.text(plugin.format(text))
        }
    }

    fun deleteNearest(location: Location): Boolean {
        val nearest = activeTables.minByOrNull { it.location.distanceSquared(location) } ?: return false
        if (nearest.location.distanceSquared(location) < 25.0) {
            nearest.world.entities.forEach {
                if (it.location.distanceSquared(nearest.location) < 4.0 && it.persistentDataContainer.has(pokerKey, PersistentDataType.BYTE)) {
                    it.remove()
                }
            }
            activeTables.remove(nearest)
            saveTables()
            return true
        }
        return false
    }

    fun cleanupAll() {
        plugin.server.worlds.forEach { world ->
            world.entities.filter { it.persistentDataContainer.has(pokerKey, PersistentDataType.BYTE) }.forEach { it.remove() }
        }
        activeTables.clear()
    }

    private fun saveTables() {
        val data = YamlConfiguration.loadConfiguration(dataFile)
        data.set("poker.tables", activeTables.filter { it.isValid }.map { serializeLocation(it.location) })
        data.save(dataFile)
    }

    private fun serializeLocation(location: Location): String =
        "${location.world.name},${location.x},${location.y},${location.z}"

    private fun parseLocation(value: String): Location? {
        val parts = value.split(',')
        if (parts.size != 4) return null
        val world = Bukkit.getWorld(parts[0]) ?: return null
        val x = parts[1].toDoubleOrNull() ?: return null
        val y = parts[2].toDoubleOrNull() ?: return null
        val z = parts[3].toDoubleOrNull() ?: return null
        return Location(world, x, y, z)
    }
}
