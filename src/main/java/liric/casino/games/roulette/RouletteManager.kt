package liric.casino.games.roulette

import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import liric.casino.packet.*
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.File
import java.util.UUID
import kotlin.collections.get
import kotlin.collections.remove
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import net.kyori.adventure.text.Component

data class RouletteInstance(
    val id: UUID = UUID.randomUUID(),
    val center: Location,
    val statusText: FakeTextDisplay,
    val pointer: FakeBlockDisplay,
    val interaction: FakeInteraction,
    val radius: Float,
    var isSpinning: Boolean = false,
    val blocks: Map<Int, FakeBlockDisplay> = mutableMapOf(),
    val texts: Map<Int, FakeTextDisplay> = mutableMapOf(),
    val glasses: Map<Int, FakeBlockDisplay> = mutableMapOf(),
    var currentOffset: Double = 0.0
)

class RouletteManager(private val plugin: CasinoPlugin) {

    private val activeRoulettes = mutableListOf<RouletteInstance>()
    val rouletteKey = NamespacedKey(plugin, "casino_roulette_id")
    private val sequence = intArrayOf(0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26)

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
            val msg = plugin.messagesConfig.getString("roulette-extra.loaded", "<#00FF7F>Loaded {count} roulettes from data.yml.</#00FF7F>")!!
            plugin.server.consoleSender.sendMessage(plugin.format(msg.replace("{count}", loaded.toString())))
        }
    }

    private fun isChunkLoaded(loc: Location): Boolean {
        if (loc.world == null) return false
        return loc.world!!.isChunkLoaded(loc.blockX shr 4, loc.blockZ shr 4)
    }

    private fun purgeArea(center: Location) {
        if (!isChunkLoaded(center)) return

        // Mantenemos esto para limpiar entidades nativas viejas de versiones anteriores
        val world = center.world ?: return
        world.getNearbyEntities(center, 15.0, 15.0, 15.0).forEach { entity ->
            if (entity.persistentDataContainer.has(rouletteKey, PersistentDataType.STRING)) {
                entity.remove()
            }
        }
    }

    fun purgeAllData(world: World): Int {
        var removedEntities = 0
        world.entities.forEach { entity ->
            if (entity.persistentDataContainer.has(rouletteKey, PersistentDataType.STRING)) {
                entity.remove()
                removedEntities++
            }
        }
        
        activeRoulettes.filter { it.center.world?.name == world.name }.forEach { unregisterInstance(it) }
        activeRoulettes.removeIf { it.center.world?.name == world.name }

        val list = dataConfig.getStringList("roulettes").toMutableList()
        val sizeBefore = list.size
        list.removeIf { it.startsWith("${world.name},") }

        if (list.size != sizeBefore) {
            dataConfig.set("roulettes", list)
            dataConfig.save(dataFile)
        }

        return removedEntities
    }

    private fun unregisterInstance(inst: RouletteInstance) {
        EntityTracker.unregister(inst.statusText)
        EntityTracker.unregister(inst.pointer)
        EntityTracker.unregister(inst.interaction)
        inst.blocks.values.forEach { EntityTracker.unregister(it) }
        inst.texts.values.forEach { EntityTracker.unregister(it) }
        inst.glasses.values.forEach { EntityTracker.unregister(it) }
    }

    fun spawnRoulette(location: Location, isNew: Boolean = true, customScale: Float? = null, customRadius: Float? = null) {

        val center = if (isNew) {
            location.clone().apply {
                x = blockX + 0.5
                y = blockY.toDouble() + 0.1
                z = blockZ + 0.5
                pitch = 0f
                yaw = 0f
            }
        } else {
            location.clone().apply {
                pitch = 0f
                yaw = 0f
            }
        }

        purgeArea(center)

        if (isNew) saveToData(center)

        val radius = customRadius ?: plugin.config.getDouble("roulette.radius", 5.5).toFloat()
        val scale  = customScale  ?: plugin.config.getDouble("roulette.block-scale", 0.45).toFloat()
        val instanceId = UUID.randomUUID()
        val mutableBlocks = mutableMapOf<Int, FakeBlockDisplay>()
        val mutableTexts = mutableMapOf<Int, FakeTextDisplay>()
        val mutableGlasses = mutableMapOf<Int, FakeBlockDisplay>()

        val cfg = plugin.menuConfig("ruleta.yml")
        val pointerMat = cfg.getMaterial("wheel.pointer-material", Material.GOLD_BLOCK)
        val pointer = FakeBlockDisplay(center, pointerMat.createBlockData())
        val pScaleX = 0.8f
        val pScaleY = 0.15f
        val pScaleZ = 0.3f
        pointer.updateTransformation(Transformation(
            Vector3f(radius + 0.1f, 0.2f, -pScaleZ / 2f),
            Quaternionf(),
            Vector3f(pScaleX, pScaleY, pScaleZ),
            Quaternionf()
        ))
        EntityTracker.register(pointer)

        for (i in sequence.indices) {
            val number = sequence[i]
            val angle = i * (2.0 * Math.PI / 37.0)
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            val blockDisplay = FakeBlockDisplay(center, getOriginalMaterial(number).createBlockData())
            val half = scale / 2.0f
            blockDisplay.updateTransformation(Transformation(
                Vector3f(radius * cosA - half, 0f, radius * sinA - half),
                Quaternionf(),
                Vector3f(scale, scale, scale),
                Quaternionf()
            ))
            val redNumbers = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
            val glassMat = if (number == 0) Material.LIME_STAINED_GLASS else if (number in redNumbers) Material.RED_STAINED_GLASS else Material.BLACK_STAINED_GLASS
            val glassDisplay = FakeBlockDisplay(center, glassMat.createBlockData())

            mutableBlocks[number] = blockDisplay
            EntityTracker.register(blockDisplay)
            mutableGlasses[number] = glassDisplay
            EntityTracker.register(glassDisplay)

            val textDisplay = FakeTextDisplay(center, plugin.format("<white><bold>$number</bold></white>"))
            textDisplay.billboard = Display.Billboard.FIXED
            textDisplay.backgroundColor = 0
            textDisplay.isShadowed = true
            textDisplay.updateTransformation(Transformation(
                Vector3f(radius * cosA, 0.6f, radius * sinA),
                Quaternionf().rotateY((angle - Math.PI / 2.0).toFloat()),
                Vector3f(1f, 1f, 1f),
                Quaternionf()
            ))
            mutableTexts[number] = textDisplay
            EntityTracker.register(textDisplay)
        }

        val statusLoc = center.clone().add(0.0, 2.0, 0.0)
        val statusText = FakeTextDisplay(statusLoc, Component.empty())
        statusText.billboard = Display.Billboard.CENTER
        statusText.backgroundColor = 0
        statusText.isShadowed = true
        EntityTracker.register(statusText)

        val interaction = FakeInteraction(center, (radius * 2) - 1.0f, 3.0f)
        EntityTracker.register(interaction)

        val instance = RouletteInstance(
            id = instanceId,
            center = center,
            statusText = statusText,
            pointer = pointer,
            interaction = interaction,
            radius = radius,
            blocks = mutableBlocks,
            texts = mutableTexts,
            glasses = mutableGlasses
        )
        activeRoulettes.add(instance)

        plugin.rouletteGame.updateStatusHologram()
        if (isNew) {
            plugin.messages.get("roulette.created")?.let { msg ->
                plugin.server.consoleSender.sendMessage(msg)
            }
        }

        respawningKeys.remove(locKey(location))
    }

    private fun updateWheel(inst: RouletteInstance, currentSpinOffset: Double, scale: Float) {
        inst.currentOffset = currentSpinOffset
        val radius = inst.radius
        val half = scale / 2.0f

        for (i in sequence.indices) {
            val number = sequence[i]
            val angle = i * (2.0 * Math.PI / 37.0) + currentSpinOffset
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            val scaleGlass = scale + 0.05f
            val halfGlass = scaleGlass / 2.0f

            inst.blocks[number]?.updateTransformation(Transformation(
                Vector3f(radius * cosA - half, 0f, radius * sinA - half),
                Quaternionf(),
                Vector3f(scale, scale, scale),
                Quaternionf()
            ), 0, 0)

            inst.glasses[number]?.updateTransformation(Transformation(
                Vector3f(radius * cosA - halfGlass, -0.01f, radius * sinA - halfGlass),
                Quaternionf(),
                Vector3f(scaleGlass, scaleGlass, scaleGlass),
                Quaternionf()
            ), 0, 0)

            inst.texts[number]?.updateTransformation(Transformation(
                Vector3f(radius * cosA, 0.6f, radius * sinA),
                Quaternionf().rotateY((angle - Math.PI / 2.0).toFloat()),
                Vector3f(1f, 1f, 1f),
                Quaternionf()
            ), 0, 0)
        }
    }

    fun rescaleAll(scale: Float, radius: Float) {
        val locations = activeRoulettes.map { it.center.clone() }
        activeRoulettes.toList().forEach {
            unregisterInstance(it)
            purgeArea(it.center) 
        }
        activeRoulettes.clear()
        locations.forEach { loc -> spawnRoulette(loc, isNew = false, customScale = scale, customRadius = radius) }
    }

    private fun startPlayerRepeller() {
        SchedulerUtil.runGlobalTimer(plugin, 0L, 10L) {
            activeRoulettes.forEach { inst ->
                if (!activeRoulettes.contains(inst)) return@runGlobalTimer
                val center = inst.center
                val world = center.world ?: return@runGlobalTimer

                world.getNearbyEntities(center, 6.0, 3.0, 6.0) { it is Player }.forEach { entity ->
                    val player = entity as Player
                    if (player.hasPermission("casino.admin") && player.gameMode == GameMode.CREATIVE) return@forEach

                    val distance = player.location.distance(center)
                    val yDiff = player.location.y - center.y

                    if (distance <= 5.8 && yDiff in -1.0..4.0) {
                        var pushDir = player.location.toVector().subtract(center.toVector())
                        if (pushDir.lengthSquared() == 0.0) {
                            pushDir = Vector(1.0, 0.0, 0.0)
                        } else {
                            pushDir = pushDir.normalize()
                        }
                        pushDir.multiply(1.5).setY(0.5)
                        player.velocity = pushDir
                        player.sendActionBar(plugin.messages.get("roulette.cannot-climb"))
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    }
                }
            }
        }
    }

    private fun startEntityMonitor() {
        SchedulerUtil.runGlobalTimer(plugin, 600L, 600L) {
            val toRespawn = mutableListOf<Location>()
            val iterator = activeRoulettes.iterator()

            while (iterator.hasNext()) {
                val inst = iterator.next()
                val world = inst.center.world ?: continue
                if (!world.isChunkLoaded(inst.center.blockX shr 4, inst.center.blockZ shr 4)) continue

                // Con packet entities no mueren, pero podemos regenerarlas si falló algo
                // En este caso, ya no es tan necesario, pero lo mantendremos para consistencia.
            }

            toRespawn.forEach { loc ->
                purgeArea(loc)
                spawnRoulette(loc, isNew = false)
            }
        }
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

    private fun removeFromData(loc: Location) {
        val list = dataConfig.getStringList("roulettes").toMutableList()
        val iterator = list.iterator()
        var modified = false

        while (iterator.hasNext()) {
            val str = iterator.next()
            val parsedLoc = stringToLoc(str)
            if (parsedLoc != null && parsedLoc.world?.name == loc.world?.name && parsedLoc.distanceSquared(loc) <= 1.0) {
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
        return "${loc.world!!.name},${loc.x},${loc.y},${loc.z}"
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
            if (!inst.isSpinning) inst.statusText.updateText(plugin.format(text))
        }
    }

    fun setSpinningHologram(text: String) {
        activeRoulettes.forEach { inst ->
            inst.isSpinning = true
            inst.statusText.updateText(plugin.format(text))
        }
    }

    fun spinAllDisplays(durationTicks: Int, winningNumber: Int, winningColor: BetColor, onFinish: () -> Unit) {
        val targetIndex = sequence.indexOf(winningNumber)
        if (targetIndex == -1) return

        var ticks = 0
        val scale = plugin.config.getDouble("roulette.block-scale", 0.45).toFloat()

        val startOffsets = activeRoulettes.associateWith { it.currentOffset }
        val targetOffsets = activeRoulettes.associateWith { inst ->
            val baseAngle = targetIndex * (2.0 * Math.PI / 37.0)
            val currentMod = (baseAngle + inst.currentOffset) % (2.0 * Math.PI)
            inst.currentOffset + (8 * 2 * Math.PI) + (2.0 * Math.PI - currentMod)
        }

        val previousSpunSlots = activeRoulettes.associateWith { 0 }.toMutableMap()

        var spinCanceller: Runnable? = null
        spinCanceller = SchedulerUtil.runGlobalTimer(plugin, 0L, 1L) {
            if (ticks >= durationTicks) {
                val finishHologram = """
                    <#FF00FF><bold>🌀 ROULETTE 🌀</bold></#FF00FF>
                    <#E0E0E0>Winning Number:</#E0E0E0>
                    ${winningColor.chatColor}<bold>$winningNumber (${winningColor.displayName})</bold>
                """.trimIndent().replace("\n", "<br>")

                activeRoulettes.forEach { inst ->
                    val target = targetOffsets[inst] ?: 0.0
                    updateWheel(inst, target, scale)
                    inst.statusText.updateText(plugin.format(finishHologram))
                    inst.center.world!!.playSound(inst.center, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                }
                
                onFinish()
                spinCanceller?.run()
                return@runGlobalTimer
            }

            val t = ticks.toFloat() / durationTicks.toFloat()
            val easeOut = 1.0 - (1.0 - t).pow(4)

            activeRoulettes.forEach { inst ->
                val start = startOffsets[inst] ?: 0.0
                val target = targetOffsets[inst] ?: 0.0
                val currentOffset = start + easeOut * (target - start)
                
                if (ticks % 2 == 0 || ticks == durationTicks - 1) {
                    updateWheel(inst, currentOffset, scale)
                }

                val spunSlots = (currentOffset / (2.0 * Math.PI / 37.0)).toInt()
                val prev = previousSpunSlots[inst] ?: 0
                if (spunSlots > prev) {
                    inst.center.world!!.playSound(inst.center, Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 2f)
                    previousSpunSlots[inst] = spunSlots
                }
            }
            ticks++
        }
    }

    private fun getOriginalMaterial(number: Int): Material {
        val cfg = plugin.menuConfig("ruleta.yml")
        if (number == 0) return cfg.getMaterial("wheel.colors.zero", Material.LIME_CONCRETE)
        val redNumbers = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
        return if (number in redNumbers) cfg.getMaterial("wheel.colors.red", Material.RED_CONCRETE) 
               else cfg.getMaterial("wheel.colors.black", Material.BLACK_CONCRETE)
    }

    fun deleteNearestRoulette(location: Location): Boolean {
        val nearest = activeRoulettes.minByOrNull { it.center.distanceSquared(location) } ?: return false
        if (nearest.center.distanceSquared(location) <= 100.0) {
            removeFromData(nearest.center)
            unregisterInstance(nearest)
            purgeArea(nearest.center)
            activeRoulettes.remove(nearest)
            return true
        }
        return false
    }

    fun cleanupAll() {
        plugin.server.worlds.forEach { world ->
            world.entities.forEach { entity ->
                if (entity.persistentDataContainer.has(rouletteKey, PersistentDataType.STRING)) {
                    entity.remove()
                }
            }
        }
        activeRoulettes.forEach { unregisterInstance(it) }
        activeRoulettes.clear()
    }

    fun resetDisplays() {
        activeRoulettes.forEach { inst ->
            inst.isSpinning = false
        }
        plugin.rouletteGame.updateStatusHologram()
    }

    fun isRouletteInteraction(entity: FakeEntity): Boolean {
        return activeRoulettes.any { it.interaction == entity }
    }
}
