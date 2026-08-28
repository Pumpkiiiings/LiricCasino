package liric.casino.config

import liric.casino.util.ColorUtil
import net.kyori.adventure.text.Component
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class MessagesConfig(private val plugin: JavaPlugin) {

    private lateinit var cfg: YamlConfiguration
    val config get() = cfg
    private lateinit var messagesFile: File

    fun load() {
        messagesFile = File(plugin.dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        cfg = YamlConfiguration.loadConfiguration(messagesFile)

        val defaults = plugin.getResource("messages.yml")
        if (defaults != null) {
            val defCfg = YamlConfiguration.loadConfiguration(defaults.reader())
            cfg.setDefaults(defCfg)
            cfg.options().copyDefaults(true)
            cfg.save(messagesFile)
        }
    }


    fun getRaw(key: String): String =
        cfg.getString(key) ?: "<red>[MISSING: $key]"


    private fun rawPrefix(): String = cfg.getString("prefix") ?: ""


    fun get(key: String, vararg placeholders: Pair<String, String>): Component {
        var text = getRaw(key)
        text = text.replace("{prefix}", rawPrefix(), ignoreCase = true)
        for ((k, v) in placeholders) {
            text = text.replace("{$k}", v, ignoreCase = true)
        }
        return ColorUtil.parse(text)
    }


    fun prefix(): Component = ColorUtil.parse(rawPrefix())


    fun prefixRaw(): String = rawPrefix()
}
