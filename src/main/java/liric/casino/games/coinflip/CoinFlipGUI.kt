package liric.casino.games.coinflip

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import java.text.NumberFormat
import java.util.Locale

class CoinFlipGUI(plugin: CasinoPlugin, val viewer: Player) : BaseMenu(plugin, "coinflip.yml") {

    fun open() {
        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        gui.open(viewer)
    }

    override fun setupItems(gui: Gui) {
        val createMat = config.getMaterial("create-btn.material", Material.NETHER_STAR)
        val createBtn = ItemBuilder.from(createMat)
            .name(config.getComponent("create-btn.name", "<#00FF7F><bold>✚ Create Game</bold>"))
            .lore(config.getComponentList("create-btn.lore"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                viewer.closeInventory()
                viewer.playSound(viewer.location, Sound.UI_BUTTON_CLICK, 1f, 1.2f)
                plugin.coinFlipManager.setPendingChat(viewer.uniqueId)
                viewer.sendMessage(plugin.messages.get("general.command-separator"))
                viewer.sendMessage(plugin.format(" <#FFD700>🪙 <white>Type your bet amount in chat."))
                viewer.sendMessage(plugin.format(" <gray>Examples: <white>1000 <gray>· <white>5k <gray>· <white>1.5m <gray>· <white>10b"))
                viewer.sendMessage(plugin.format(" <gray>Type <red>cancel</red> to abort."))
                viewer.sendMessage(plugin.messages.get("general.command-separator"))
                viewer.sendActionBar(plugin.format("<#FFD700>💬 Type the bet amount in chat..."))
            }
        gui.setItem(config.getInt("create-btn.slot", 4), createBtn)

        val refreshMat = config.getMaterial("refresh-btn.material", Material.COMPASS)
        val refresh = ItemBuilder.from(refreshMat)
            .name(config.getComponent("refresh-btn.name", "<white><bold>↺ Refresh</bold>"))
            .lore(config.getComponentList("refresh-btn.lore"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                viewer.playSound(viewer.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                CoinFlipGUI(plugin, viewer).open()
            }
        gui.setItem(config.getInt("refresh-btn.slot", 49), refresh)

        val games = plugin.coinFlipManager.getOpenGames()
        val gameSlots = config.getStringList("games-slots").mapNotNull { it.toIntOrNull() }

        if (games.isEmpty()) {
            val noGamesMat = config.getMaterial("no-games.material", Material.BELL)
            val bell = ItemBuilder.from(noGamesMat)
                .name(config.getComponent("no-games.name"))
                .lore(config.getComponentList("no-games.lore"))
                .flags(*ItemFlag.values()).asGuiItem()
            gui.setItem(config.getInt("no-games.slot", 22), bell)
        } else {
            games.take(gameSlots.size).forEachIndexed { index, session ->
                gui.setItem(gameSlots[index], buildGameItem(session))
            }
        }
    }

    private fun buildGameItem(session: CoinFlipSession): GuiItem {
        val isOwn = session.creatorId == viewer.uniqueId
        val amountFmt = "$" + NumberFormat.getNumberInstance(Locale.US).format(session.betAmount)
        val clickTextKey = if (isOwn) "game-item.click-cancel" else "game-item.click-join"
        val clickText = config.getString(clickTextKey, "<white>Click")

        val rawName = config.getString("game-item.name", "<white>%creator%")
            .replace("%creator%", session.creatorName)
            .replace("%amount%", amountFmt)

        val rawLore = config.getStringList("game-item.lore").map { line ->
            line.replace("%creator%", session.creatorName)
                .replace("%amount%", amountFmt)
                .replace("%click_action%", clickText)
        }

        return ItemBuilder.skull()
            .owner(Bukkit.getOfflinePlayer(session.creatorId))
            .name(plugin.format(rawName))
            .lore(rawLore.map { plugin.format(it) })
            .flags(*ItemFlag.values())
            .asGuiItem {
                if (isOwn) {
                    plugin.coinFlipManager.cancelGame(viewer)
                    CoinFlipGUI(plugin, viewer).open()
                } else {
                    plugin.coinFlipManager.joinGame(viewer, session.creatorName)
                }
            }
    }
}
