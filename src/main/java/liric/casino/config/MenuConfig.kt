package liric.casino.config

import liric.casino.util.ColorUtil
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Cargador genérico de un archivo YAML dentro de la carpeta menus/.
 * Uso:
 *   val cfg = MenuConfig(plugin, "ruleta.yml")
 *   cfg.load()
 *   val title = cfg.getComponent("title")
 *   val mat   = cfg.getMaterial("items.zero.material", Material.LIME_DYE)
 */
class MenuConfig(private val plugin: JavaPlugin, private val fileName: String) {

    private lateinit var cfg: YamlConfiguration

    fun load() {
        val file = File(plugin.dataFolder, "menus/$fileName")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            plugin.saveResource("menus/$fileName", false)
        }
        cfg = YamlConfiguration.loadConfiguration(file)
        // Completar con defaults del jar
        val defaults = plugin.getResource("menus/$fileName")
        if (defaults != null) {
            val defCfg = YamlConfiguration.loadConfiguration(defaults.reader())
            cfg.setDefaults(defCfg)
            cfg.options().copyDefaults(true)
            cfg.save(file)
        }
    }

    // ─── Getters básicos ──────────────────────────────────────────────────

    fun getString(path: String, default: String = ""): String =
        cfg.getString(path, default) ?: default

    fun getInt(path: String, default: Int = 0): Int =
        cfg.getInt(path, default)

    fun getDouble(path: String, default: Double = 0.0): Double =
        cfg.getDouble(path, default)

    fun getBoolean(path: String, default: Boolean = false): Boolean =
        cfg.getBoolean(path, default)

    fun getStringList(path: String): List<String> =
        cfg.getStringList(path)

    fun getIntegerList(path: String): List<Int> =
        cfg.getIntegerList(path)

    fun getDoubleList(path: String): List<Double> =
        cfg.getDoubleList(path)

    fun getMapList(path: String): List<Map<*, *>> =
        cfg.getMapList(path)

    // ─── Getters de Adventure ─────────────────────────────────────────────

    /** Parsea un string del YAML como Component (soporta &, &#, <mm>). */
    fun getComponent(path: String, default: String = ""): Component =
        ColorUtil.parse(getString(path, default))

    /** Parsea una lista de strings como lista de Components. */
    fun getComponentList(path: String): List<Component> =
        getStringList(path).map { ColorUtil.parse(it) }

    // ─── ItemBuilder ──────────────────────────────────────────────────────

    fun getItemBuilder(path: String): dev.triumphteam.gui.builder.item.ItemBuilder {
        val matStr = getString("$path.material", "STONE")
        val mat = runCatching { Material.valueOf(matStr.uppercase()) }.getOrDefault(Material.STONE)
        val name = getString("$path.name", "")
        val loreList = getStringList("$path.lore")
        
        val builder = dev.triumphteam.gui.builder.item.ItemBuilder.from(mat)
            .flags(*org.bukkit.inventory.ItemFlag.values())
        
        if (name.isNotEmpty()) {
            builder.name(ColorUtil.parse(name))
        }
        if (loreList.isNotEmpty()) {
            builder.lore(loreList.map { ColorUtil.parse(it) })
        }
        return builder
    }

    // ─── Material ─────────────────────────────────────────────────────────

    fun getMaterial(path: String, default: Material = Material.STONE): Material =
        runCatching { Material.valueOf(getString(path, default.name).uppercase()) }
            .getOrDefault(default)

    // ─── ConfigurationSection ────────────────────────────────────────────

    fun getSection(path: String) = cfg.getConfigurationSection(path)

    fun getKeys(path: String, deep: Boolean = false): Set<String> =
        cfg.getConfigurationSection(path)?.getKeys(deep) ?: emptySet()

    fun has(path: String): Boolean = cfg.contains(path)

    // ─── Decoraciones (Triumph-GUI) ──────────────────────────────────────

    fun applyDecorations(gui: dev.triumphteam.gui.guis.BaseGui) {
        val decorations = cfg.getMapList("decorations")
        for (map in decorations) {
            val materialStr = map["material"]?.toString() ?: continue
            val material = runCatching { Material.valueOf(materialStr.uppercase()) }.getOrNull() ?: continue
            val nameStr = map["name"]?.toString() ?: " "
            val slotsRaw = map["slots"] as? List<*> ?: continue

            val item = dev.triumphteam.gui.builder.item.ItemBuilder.from(material)
                .name(ColorUtil.parse(nameStr))
                .flags(*org.bukkit.inventory.ItemFlag.values())
                .asGuiItem()

            slotsRaw.forEach { slotRaw ->
                val slot = slotRaw.toString().toIntOrNull()
                if (slot != null) gui.setItem(slot, item)
            }
        }
    }
}
