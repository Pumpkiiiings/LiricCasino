package liric.casino.config

import liric.casino.util.ColorUtil
import net.kyori.adventure.text.Component
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Wrapper de messages.yml.
 * Uso:
 *   plugin.messages.get("roulette.win", "amount" to "500", "booster" to "")
 *   plugin.messages.getComponent("prefix")
 */
class MessagesConfig(private val plugin: JavaPlugin) {

    private lateinit var cfg: YamlConfiguration
    private lateinit var messagesFile: File

    fun load() {
        messagesFile = File(plugin.dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        cfg = YamlConfiguration.loadConfiguration(messagesFile)
        // Completar con defaults del jar si hay claves nuevas
        val defaults = plugin.getResource("messages.yml")
        if (defaults != null) {
            val defCfg = YamlConfiguration.loadConfiguration(defaults.reader())
            cfg.setDefaults(defCfg)
            cfg.options().copyDefaults(true)
            cfg.save(messagesFile)
        }
    }

    /** Devuelve el raw string del yaml (sin parsear). */
    fun getRaw(key: String): String =
        cfg.getString(key) ?: "<red>[MISSING: $key]"

    /** Devuelve el prefix raw. */
    private fun rawPrefix(): String = cfg.getString("prefix") ?: ""

    /**
     * Obtiene el mensaje por clave, reemplaza {prefix} y cualquier
     * placeholder {key} con su valor, luego parsea colores.
     */
    fun get(key: String, vararg placeholders: Pair<String, String>): Component {
        var text = getRaw(key)
        text = text.replace("{prefix}", rawPrefix())
        for ((k, v) in placeholders) {
            text = text.replace("{$k}", v)
        }
        return ColorUtil.parse(text)
    }

    /**
     * Igual que get() pero devuelve el Component del prefix solo.
     */
    fun prefix(): Component = ColorUtil.parse(rawPrefix())

    /**
     * Devuelve el raw del prefix (para concatenar con strings antes de parsear).
     */
    fun prefixRaw(): String = rawPrefix()
}
