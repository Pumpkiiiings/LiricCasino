package liric.casino.games.coinflip

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import java.text.NumberFormat
import java.util.Locale
import kotlin.collections.get

object CoinFlipGUI {

    fun open(plugin: CasinoPlugin, viewer: Player) {
        val cfg = plugin.menuConfig("coinflip.yml")
        val gui = Gui.gui()
            .title(cfg.getComponent("title", "<dark_gray><bold>CoinFlip</bold></dark_gray> <dark_gray>▸</dark_gray> <white>Juegos"))
            .rows(cfg.getInt("rows", 6))
            .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
            .create()

        // Aplicar decoraciones desde el YAML
        cfg.applyDecorations(gui)

        // ── Botón Crear ────────────────────────────
        val createMat = cfg.getMaterial("create-btn.material", Material.NETHER_STAR)
        val createBtn = ItemBuilder.from(createMat)
            .name(cfg.getComponent("create-btn.name", "<#00FF7F><bold>✚ Crear Juego</bold>"))
            .lore(cfg.getComponentList("create-btn.lore"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                viewer.closeInventory()
                viewer.playSound(viewer.location, Sound.UI_BUTTON_CLICK, 1f, 1.2f)
                plugin.coinFlipManager.setPendingChat(viewer.uniqueId)
                viewer.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
                viewer.sendMessage(plugin.format(" <#FFD700>🪙 <white>Escribe el monto de tu apuesta en el chat."))
                viewer.sendMessage(plugin.format(" <gray>Ejemplos: <white>1000 <gray>· <white>5k <gray>· <white>1.5m <gray>· <white>10b"))
                viewer.sendMessage(plugin.format(" <gray>Escribe <red>cancelar</red> para abortar."))
                viewer.sendMessage(plugin.format("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"))
                viewer.sendActionBar(plugin.format("<#FFD700>💬 Escribe el monto a apostar en el chat..."))
            }
        gui.setItem(cfg.getInt("create-btn.slot", 4), createBtn)

        // ── Botón Actualizar ───────────────────
        val refreshMat = cfg.getMaterial("refresh-btn.material", Material.COMPASS)
        val refresh = ItemBuilder.from(refreshMat)
            .name(cfg.getComponent("refresh-btn.name", "<white><bold>↺ Actualizar</bold>"))
            .lore(cfg.getComponentList("refresh-btn.lore"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                viewer.playSound(viewer.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                open(plugin, viewer)
            }
        gui.setItem(cfg.getInt("refresh-btn.slot", 49), refresh)

        // ── Juegos activos ─────────────────────────────────
        val games = plugin.coinFlipManager.getOpenGames()
        val gameSlots = cfg.getStringList("games-slots").mapNotNull { it.toIntOrNull() }

        if (games.isEmpty()) {
            val noGamesMat = cfg.getMaterial("no-games.material", Material.BELL)
            val bell = ItemBuilder.from(noGamesMat)
                .name(cfg.getComponent("no-games.name"))
                .lore(cfg.getComponentList("no-games.lore"))
                .flags(*ItemFlag.values()).asGuiItem()
            gui.setItem(cfg.getInt("no-games.slot", 22), bell)
        } else {
            games.take(gameSlots.size).forEachIndexed { index, session ->
                gui.setItem(gameSlots[index], buildGameItem(plugin, session, viewer))
            }
        }

        gui.open(viewer)
    }

    private fun buildGameItem(plugin: CasinoPlugin, session: CoinFlipSession, viewer: Player): GuiItem {
        val cfg = plugin.menuConfig("coinflip.yml")
        val isOwn = session.creatorId == viewer.uniqueId
        val amountFmt = "$" + NumberFormat.getNumberInstance(Locale.US).format(session.betAmount)
        val clickTextKey = if (isOwn) "game-item.click-cancel" else "game-item.click-join"
        val clickText = cfg.getString(clickTextKey, "<white>Click")

        val rawName = cfg.getString("game-item.name", "<white>%creator%")
            .replace("%creator%", session.creatorName)
            .replace("%amount%", amountFmt)
        
        val rawLore = cfg.getStringList("game-item.lore").map { line ->
            line.replace("%creator%", session.creatorName)
                .replace("%amount%", amountFmt)
                .replace("%click_action%", clickText)
        }

        val item = ItemBuilder.skull()
            .owner(Bukkit.getOfflinePlayer(session.creatorId))
            .name(plugin.format(rawName))
            .lore(rawLore.map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                if (isOwn) {
                    plugin.coinFlipManager.cancelGame(viewer)
                    open(plugin, viewer)
                } else {
                    plugin.coinFlipManager.joinGame(viewer, session.creatorName)
                }
            }
        return item
    }
}
