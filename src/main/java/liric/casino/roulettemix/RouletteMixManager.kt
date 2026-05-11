package liric.casino.roulettemix

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.File
import java.util.UUID
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class RouletteMixInstance(
    val id: UUID = UUID.randomUUID(),
    val center: Location,
    val statusText: TextDisplay,
    val radius: Float,
    var isSpinning: Boolean = false,
    val entityUUIDs: MutableList<UUID> = mutableListOf(),
    val blocks: Map<Int, BlockDisplay> = mutableMapOf()
)

class RouletteMixManager(private val plugin: CasinoPlugin) {

    private val activeRoulettes = mutableListOf<RouletteMixInstance>()
    val rouletteMixKey = NamespacedKey(plugin, "casino_roulettemix_id")
    private val sequence = IntArray(37) { it }

    // Guard: evita re-spawns simultáneos de la misma ruleta (FIX duplicación)
    private val respawningKeys = mutableSetOf<String>()

    private val dataFile = File(plugin.dataFolder, "data.yml")
    private val dataConfig = YamlConfiguration.loadConfiguration(dataFile)

    init {
        cleanupAll()
        startEntityMonitor()
        startPlayerRepeller()
    }

    fun loadRoulettes() {
        val list = dataConfig.getStringList("roulettes")
        var loaded = 0
        list.forEach { locStr ->
            val loc = stringToLoc(locStr)
            if (loc != null) {
                spawnRoulette(loc, isNew = false)
                loaded++
            }
        }
        if (loaded > 0) {
            plugin.server.consoleSender.sendMessage(plugin.format("<#00FF7F>Se cargaron $loaded ruletas desde data.yml.</#00FF7F>"))
        }
    }

    private fun purgeArea(center: Location) {
        val world = center.world ?: return
        world.getNearbyEntities(center, 15.0, 15.0, 15.0).forEach { entity ->
            if (entity.persistentDataContainer.has(rouletteMixKey, PersistentDataType.STRING)) {
                entity.remove()
            }
        }
    }

    // NUEVO: PURGA NUCLEAR (Borra todo del mundo y del archivo)
    fun purgeAllData(world: org.bukkit.World): Int {
        var removedEntities = 0

        // 1. Borrar entidades del mundo
        world.entities.forEach { entity ->
            if (entity.persistentDataContainer.has(rouletteMixKey, PersistentDataType.STRING)) {
                entity.remove()
                removedEntities++
            }
        }

        // 2. Limpiar memoria RAM
        activeRoulettes.removeIf { it.center.world?.name == world.name }

        // 3. Limpiar Archivo data.yml (Arranca la raíz del problema)
        val list = dataConfig.getStringList("roulettes").toMutableList()
        val sizeBefore = list.size
        list.removeIf { it.startsWith("${world.name},") }

        if (list.size != sizeBefore) {
            dataConfig.set("roulettes", list)
            dataConfig.save(dataFile)
        }

        return removedEntities
    }

    fun spawnRoulette(location: Location, isNew: Boolean = true) {
        val center = if (isNew) {
            location.clone().apply {
                x = blockX + 0.5
                y = blockY.toDouble() + 0.1
                z = blockZ + 0.5
            }
        } else {
            location.clone()
        }

        purgeArea(center)

        if (isNew) saveToData(center)

        val radius = 5.5f
        val instanceId = UUID.randomUUID()
        val instanceUUIDs = mutableListOf<UUID>()
        val blocksMap = mutableMapOf<Int, BlockDisplay>()

        for (i in sequence.indices) {
            val number = sequence[i]
            val angle = i * (2.0 * Math.PI / 37.0)

            val slotLoc = center.clone().add(radius * cos(angle), 0.0, radius * sin(angle))
            val blockDisplay = center.world.spawnEntity(slotLoc, EntityType.BLOCK_DISPLAY) as BlockDisplay
            blockDisplay.persistentDataContainer.set(rouletteMixKey, PersistentDataType.STRING, instanceId.toString())
            instanceUUIDs.add(blockDisplay.uniqueId)
            blockDisplay.block = getOriginalMaterial(number).createBlockData()
            blocksMap[number] = blockDisplay

            val scale = 0.45f
            val half = scale / 2.0f
            val theta = -angle.toFloat()
            blockDisplay.transformation = Transformation(
                Vector3f(-(half * cos(theta) + half * sin(theta)), 0f, -(-half * sin(theta) + half * cos(theta))),
                Quaternionf().rotationY(theta), Vector3f(scale, scale, scale), Quaternionf()
            )

            val textLoc = slotLoc.clone().add(0.0, 0.6, 0.0)
            val textDisplay = center.world.spawnEntity(textLoc, EntityType.TEXT_DISPLAY) as TextDisplay
            textDisplay.persistentDataContainer.set(rouletteMixKey, PersistentDataType.STRING, instanceId.toString())
            instanceUUIDs.add(textDisplay.uniqueId)
            textDisplay.text(plugin.format("<white><bold>$number</bold></white>"))
            textDisplay.billboard = Display.Billboard.CENTER
            textDisplay.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            textDisplay.isShadowed = true
        }

        val statusLoc = center.clone().add(0.0, 2.0, 0.0)
        val statusText = center.world.spawnEntity(statusLoc, EntityType.TEXT_DISPLAY) as TextDisplay
        statusText.persistentDataContainer.set(rouletteMixKey, PersistentDataType.STRING, instanceId.toString())
        instanceUUIDs.add(statusText.uniqueId)
        statusText.billboard = Display.Billboard.CENTER
        statusText.backgroundColor = Color.fromARGB(0, 0, 0, 0)
        statusText.isShadowed = true

        val interaction = center.world.spawnEntity(center, EntityType.INTERACTION) as Interaction
        interaction.persistentDataContainer.set(rouletteMixKey, PersistentDataType.STRING, instanceId.toString())
        interaction.interactionWidth = (radius * 2) - 1.0f
        interaction.interactionHeight = 3.0f
        instanceUUIDs.add(interaction.uniqueId)

        val instance = RouletteMixInstance(instanceId, center, statusText, radius, false, instanceUUIDs, blocksMap)
        activeRoulettes.add(instance)

        plugin.rouletteMixGame.updateStatusHologram()
        if (isNew) plugin.server.sendMessage(plugin.messages.get("roulette.created"))
        respawningKeys.remove(locKey(location))
    }

    private fun startPlayerRepeller() {
        object : BukkitRunnable() {
            override fun run() {
                activeRoulettes.forEach { inst ->
                    if (inst.statusText.isDead) return@forEach
                    val center = inst.center
                    val world = center.world ?: return@forEach

                    world.getNearbyEntities(center, 6.0, 3.0, 6.0) { it is Player }.forEach { entity ->
                        val player = entity as Player
                        if (player.hasPermission("casino.admin") && player.gameMode == GameMode.CREATIVE) return@forEach

                        val distance = player.location.distance(center)
                        val yDiff = player.location.y - center.y

                        if (distance <= 5.8 && yDiff in -1.0..4.0) {
                            var pushDir = player.location.toVector().subtract(center.toVector())
                            if (pushDir.lengthSquared() == 0.0) {
                                pushDir = org.bukkit.util.Vector(1.0, 0.0, 0.0)
                            } else {
                                pushDir = pushDir.normalize()
                            }
                            pushDir.multiply(1.5).setY(0.5)
                            player.velocity = pushDir
                            player.sendActionBar(plugin.format("<#FF5555><bold>¡No puedes subirte a la mesa!</bold></#FF5555>"))
                            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L)  // FIX: era 5L — reducido a 10L para menos lag
    }

    private fun startEntityMonitor() {
        // FIX: 600 ticks (30s) en lugar de 100 ticks (5s) — reduce lag masivamente
        object : BukkitRunnable() {
            override fun run() {
                val toRespawn = mutableListOf<Location>()
                val iterator = activeRoulettes.iterator()

                while (iterator.hasNext()) {
                    val inst = iterator.next()
                    val world = inst.center.world ?: continue
                    // Solo verificar si el chunk está cargado (evita cargar chunks innecesariamente)
                    if (!world.isChunkLoaded(inst.center.blockX shr 4, inst.center.blockZ shr 4)) continue
                    if (inst.statusText.isDead) {
                        val key = locKey(inst.center)
                        if (!respawningKeys.contains(key)) {
                            toRespawn.add(inst.center)
                            respawningKeys.add(key)
                            iterator.remove()
                        }
                    }
                }

                // FIX: purgar duplicados antes de re-spawnar
                toRespawn.forEach { loc ->
                    purgeArea(loc)
                    spawnRoulette(loc, isNew = false)
                }
            }
        }.runTaskTimer(plugin, 600L, 600L)  // FIX: era 100L — duplicaba entidades
    }

    private fun locKey(loc: Location) = "${loc.world?.name},${loc.blockX},${loc.blockY},${loc.blockZ}"

    private fun saveToData(loc: Location) {
        val list = dataConfig.getStringList("roulettes")
        val locStr = locToString(loc)
        if (!list.contains(locStr)) {
            list.add(locStr)
            dataConfig.set("roulettes", list)
            dataConfig.save(dataFile)
        }
    }

    // FIX DEFINITIVO: Borrado inteligente que ignora fallos de decimales (Compara distancia)
    private fun removeFromData(loc: Location) {
        val list = dataConfig.getStringList("roulettes").toMutableList()
        val iterator = list.iterator()
        var modified = false

        while (iterator.hasNext()) {
            val str = iterator.next()
            val parsedLoc = stringToLoc(str)
            // Si el mundo es el mismo y está a menos de 1 bloque de diferencia, lo borra seguro.
            if (parsedLoc != null && parsedLoc.world?.name == loc.world?.name && parsedLoc.distanceSquared(loc) < 1.0) {
                iterator.remove()
                modified = true
            }
        }

        if (modified) {
            dataConfig.set("roulettes", list)
            dataConfig.save(dataFile)
        }
    }

    private fun locToString(loc: Location): String {
        return "${loc.world.name},${loc.x},${loc.y},${loc.z}"
    }

    private fun stringToLoc(str: String): Location? {
        val parts = str.split(",")
        if (parts.size == 4) {
            val world = Bukkit.getWorld(parts[0]) ?: return null
            return Location(world, parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
        }
        return null
    }

    fun updateHolograms(text: String) {
        activeRoulettes.forEach { inst ->
            if (!inst.isSpinning && !inst.statusText.isDead) inst.statusText.text(plugin.format(text))
        }
    }

    fun setSpinningHologram(text: String) {
        activeRoulettes.forEach { inst ->
            inst.isSpinning = true
            if (!inst.statusText.isDead) inst.statusText.text(plugin.format(text))
        }
    }

    fun spinAllDisplays(durationTicks: Int, winningNumber: Int, winningColor: BetColor, onFinish: () -> Unit) {
        val targetIndex = sequence.indexOf(winningNumber)
        if (targetIndex == -1) return
        val totalJumps = (8 * 37) + targetIndex

        object : BukkitRunnable() {
            var ticks = 0
            val previousIndices = activeRoulettes.associateWith { 0 }.toMutableMap()

            override fun run() {
                if (ticks >= durationTicks) {
                    val finishHologram = """
                        <#FF00FF><bold>🌀 RULETA MIXTA 🌀</bold></#FF00FF>
                        <#E0E0E0>¡Ha caído el:</#E0E0E0>
                        ${winningColor.chatColor}<bold>$winningNumber (${winningColor.displayName})</bold>
                    """.trimIndent().replace("\n", "<br>")

                    activeRoulettes.forEach { inst ->
                        if (inst.statusText.isDead) return@forEach
                        val prevNum = sequence[previousIndices[inst]!! % 37]
                        inst.blocks[prevNum]?.block = getOriginalMaterial(prevNum).createBlockData()
                        inst.blocks[winningNumber]?.block = Material.GOLD_BLOCK.createBlockData()

                        inst.statusText.text(plugin.format(finishHologram))
                        inst.center.world.playSound(inst.center, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                    }
                    onFinish()
                    cancel()
                    return
                }

                val t = ticks.toFloat() / durationTicks.toFloat()
                val easeOut = 1.0 - (1.0 - t).pow(3)
                val currentJump = (easeOut * totalJumps).toInt()

                activeRoulettes.forEach { inst ->
                    if (inst.statusText.isDead) return@forEach
                    val prevJump = previousIndices[inst]!!
                    if (currentJump > prevJump) {
                        val prevNum = sequence[prevJump % 37]
                        val currentNum = sequence[currentJump % 37]
                        inst.blocks[prevNum]?.block = getOriginalMaterial(prevNum).createBlockData()
                        inst.blocks[currentNum]?.block = Material.GOLD_BLOCK.createBlockData()
                        inst.center.world.playSound(inst.center, Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 2f)
                        previousIndices[inst] = currentJump
                    }
                }
                ticks++
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    private fun getOriginalMaterial(number: Int): Material {
        if (number == 0) return Material.LIME_CONCRETE
        val redNumbers = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
        return if (number in redNumbers) Material.RED_CONCRETE else Material.BLACK_CONCRETE
    }

    fun deleteNearestRoulette(location: Location): Boolean {
        val nearest = activeRoulettes.minByOrNull { it.center.distanceSquared(location) } ?: return false
        if (nearest.center.distanceSquared(location) < 100.0) {

            // BORRADO DEL DATA.YML PRIMERO (Ahora sí funciona al 100%)
            removeFromData(nearest.center)

            // LUEGO DESTRUIR FÍSICAMENTE TODO EL RADIO
            purgeArea(nearest.center)
            activeRoulettes.remove(nearest)
            return true
        }
        return false
    }

    fun cleanupAll() {
        plugin.server.worlds.forEach { world ->
            world.entities.forEach { entity ->
                if (entity.persistentDataContainer.has(rouletteMixKey, PersistentDataType.STRING)) {
                    entity.remove()
                }
            }
        }
        activeRoulettes.clear()
    }

    fun resetDisplays() {
        activeRoulettes.forEach { inst ->
            inst.isSpinning = false
            inst.blocks.forEach { (num, blockDisplay) ->
                if (!blockDisplay.isDead) {
                    blockDisplay.block = getOriginalMaterial(num).createBlockData()
                }
            }
        }
        plugin.rouletteMixGame.updateStatusHologram()
    }
}
