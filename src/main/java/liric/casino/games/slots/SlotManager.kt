package liric.casino.games.slots

import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.io.File
import java.util.UUID

data class SlotMachineInstance(
    val location: Location,
    var hologram: TextDisplay? = null,
    var occupant: UUID? = null
)

class SlotManager(private val plugin: CasinoPlugin) {

    private val activeSlots = mutableMapOf<Location, SlotMachineInstance>()
    val slotKey = NamespacedKey(plugin, "casino_slot_id")


    val registry: SlotRegistry = SlotRegistry.fromConfig(plugin.config)

    private val dataFile = File(plugin.dataFolder, "data.yml")
    private val dataConfig = YamlConfiguration.loadConfiguration(dataFile)

    init {
        cleanupAll()
        startHologramMonitor()
    }

    fun loadMachines() {
        val list = dataConfig.getStringList("slots_777")
        var loaded = 0
        list.forEach { locStr ->
            val loc = stringToLoc(locStr)
            if (loc != null) {
                spawnMachine(loc, isNew = false)
                loaded++
            }
        }
        if (loaded > 0) {
            val msg = plugin.messagesConfig.getString("slots-extra.loaded", "<#00FF7F>Loaded {count} 777 Machines from data.yml.</#00FF7F>")!!
            plugin.server.consoleSender.sendMessage(plugin.format(msg.replace("{count}", loaded.toString())))
        }
    }

    fun spawnMachine(blockLoc: Location, isNew: Boolean = true) {

        if (!isChunkLoaded(blockLoc)) return

        purgeExactBlock(blockLoc)

        if (isNew) saveToData(blockLoc)

        val holoLoc = blockLoc.clone().apply {
            x += 0.5
            y += 1.5
            z += 0.5
        }

        val textDisplay = blockLoc.world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY) as TextDisplay



        textDisplay.isPersistent = false

        textDisplay.persistentDataContainer.set(slotKey, PersistentDataType.BYTE, 1.toByte())

        val maxMult = registry.items.maxOfOrNull { it.multiplier } ?: 100.0
        val holoText = plugin.menuConfig("slots.yml").getString("title", "<#FFB400><bold>🎰 777 MACHINE 🎰</bold>") +
                "<br><#E0E0E0>Click to play</#E0E0E0>" +
                "<br><#00FF7F>Win up to x${maxMult.toInt()}!</#00FF7F>"

        textDisplay.text(plugin.format(holoText))
        textDisplay.billboard = Display.Billboard.CENTER
        textDisplay.backgroundColor = Color.fromARGB(0, 0, 0, 0)
        textDisplay.isShadowed = true

        activeSlots[blockLoc] = SlotMachineInstance(blockLoc, textDisplay)
    }

    private fun startHologramMonitor() {
        SchedulerUtil.runGlobalTimer(plugin, 200L, 200L) {
            activeSlots.values.forEach { machine ->

                if (!isChunkLoaded(machine.location)) return@forEach


                if (machine.hologram == null || machine.hologram!!.isDead || !machine.hologram!!.isValid) {
                    purgeExactBlock(machine.location)

                    val holoLoc = machine.location.clone().apply { x += 0.5; y += 1.5; z += 0.5 }
                    val textDisplay = machine.location.world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY) as TextDisplay


                    textDisplay.isPersistent = false
                    textDisplay.persistentDataContainer.set(slotKey, PersistentDataType.BYTE, 1.toByte())

                    val maxMult = registry.items.maxOfOrNull { it.multiplier } ?: 100.0
                    val holoText = plugin.menuConfig("slots.yml").getString("title", "<#FFB400><bold>🎰 777 MACHINE 🎰</bold>") +
                            "<br><#E0E0E0>Click to play</#E0E0E0>" +
                            "<br><#00FF7F>Win up to x${maxMult.toInt()}!</#00FF7F>"

                    textDisplay.text(plugin.format(holoText))
                    textDisplay.billboard = Display.Billboard.CENTER
                    textDisplay.backgroundColor = Color.fromARGB(0, 0, 0, 0)
                    textDisplay.isShadowed = true

                    machine.hologram = textDisplay
                }
            }
        }
    }


    private fun isChunkLoaded(loc: Location): Boolean {
        if (loc.world == null) return false
        val chunkX = loc.blockX shr 4
        val chunkZ = loc.blockZ shr 4
        return loc.world!!.isChunkLoaded(chunkX, chunkZ)
    }

    fun getMachine(loc: Location): SlotMachineInstance? {
        return activeSlots.values.find {
            it.location.world?.name == loc.world?.name &&
                    it.location.blockX == loc.blockX &&
                    it.location.blockY == loc.blockY &&
                    it.location.blockZ == loc.blockZ
        }
    }

    fun occupyMachine(loc: Location, playerUuid: UUID) {
        getMachine(loc)?.occupant = playerUuid
    }

    fun freeMachine(loc: Location) {
        getMachine(loc)?.occupant = null
    }

    fun deleteNearestMachine(location: Location): Boolean {
        val nearest = activeSlots.values.minByOrNull { it.location.distanceSquared(location) } ?: return false
        if (nearest.location.distanceSquared(location) < 25.0) {

            removeFromData(nearest.location)


            if (isChunkLoaded(nearest.location)) {
                nearest.hologram?.remove()
                purgeExactBlock(nearest.location)
            }

            activeSlots.remove(nearest.location)

            return true
        }
        return false
    }

    fun purgeAllData(world: World): Int {
        var removed = 0


        world.entities.forEach { entity ->
            if (entity.persistentDataContainer.has(slotKey, PersistentDataType.BYTE)) {
                entity.remove()
                removed++
            }
        }

        activeSlots.entries.removeIf { it.key.world?.name == world.name }

        val list = dataConfig.getStringList("slots_777").toMutableList()
        val sizeBefore = list.size
        list.removeIf { it.startsWith("${world.name},") }
        if (list.size != sizeBefore) {
            dataConfig.set("slots_777", list)
            dataConfig.save(dataFile)
        }
        return removed
    }

    fun purgeExactBlock(blockLoc: Location) {
        if (!isChunkLoaded(blockLoc)) return

        val holoLoc = blockLoc.clone().apply { x += 0.5; y += 1.5; z += 0.5 }
        blockLoc.world!!.getNearbyEntities(holoLoc, 2.0, 3.0, 2.0).forEach { entity ->
            if (entity.persistentDataContainer.has(slotKey, PersistentDataType.BYTE)) {
                entity.remove()
            }
        }
    }

    fun cleanupAll() {
        plugin.server.worlds.forEach { world ->
            world.entities.forEach { entity ->
                if (entity.persistentDataContainer.has(slotKey, PersistentDataType.BYTE)) {
                    entity.remove()
                }
            }
        }
        activeSlots.clear()
    }

    private fun saveToData(loc: Location) {
        val list = dataConfig.getStringList("slots_777")
        val locStr = locToString(loc)
        if (!list.contains(locStr)) {
            list.add(locStr)
            dataConfig.set("slots_777", list)
            dataConfig.save(dataFile)
        }
    }

    private fun removeFromData(loc: Location) {
        val list = dataConfig.getStringList("slots_777").toMutableList()
        val iterator = list.iterator()
        var modified = false

        while (iterator.hasNext()) {
            val str = iterator.next()
            val parsedLoc = stringToLoc(str)
            if (parsedLoc != null && parsedLoc.world?.name == loc.world?.name && parsedLoc.distanceSquared(loc) < 2.0) {
                iterator.remove()
                modified = true
            }
        }

        if (modified) {
            dataConfig.set("slots_777", list)
            dataConfig.save(dataFile)
        }
    }

    private fun locToString(loc: Location): String = "${loc.world!!.name},${loc.blockX},${loc.blockY},${loc.blockZ}"

    private fun stringToLoc(str: String): Location? {
        val p = str.split(",")
        if (p.size == 4) return Location(Bukkit.getWorld(p[0]), p[1].toDouble(), p[2].toDouble(), p[3].toDouble())
        return null
    }
}
