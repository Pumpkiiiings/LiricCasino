package liric.casino.core

import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.config.MenuConfig

abstract class BaseMenu(val plugin: CasinoPlugin, val configName: String) {
    
    val config: MenuConfig
        get() = plugin.menuConfig(configName)

    /**
     * Builds and initializes a Gui based on the config file.
     * Automatically applies decorations.
     */
    protected fun buildGui(): Gui {
        val gui = Gui.gui()
            .title(plugin.format(config.getString("title", "Menu")))
            .rows(config.getInt("rows", 3))
            .create()

        // Apply background decorations if defined
        config.getMapList("decorations").forEach { dec ->
            val slots = dec["slots"] as? List<*>
            val itemKey = dec["item"] as? String
            if (slots != null && itemKey != null) {
                val item = config.getItemBuilder(itemKey).asGuiItem()
                slots.forEach { s ->
                    val slot = (s as? Number)?.toInt() ?: return@forEach
                    gui.setItem(slot, item)
                }
            }
        }
        
        setupItems(gui)
        return gui
    }

    /**
     * Subclasses must implement this to define interactive items.
     */
    abstract fun setupItems(gui: Gui)
}
