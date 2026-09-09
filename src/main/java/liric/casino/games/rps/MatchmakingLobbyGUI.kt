package liric.casino.games.rps

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import liric.casino.core.MatchmakingSession
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import java.text.NumberFormat
import java.util.Locale

/** Shared, visual lobby for RPS and Tic Tac Toe. */
class MatchmakingLobbyGUI(
    plugin: CasinoPlugin,
    private val viewer: Player,
    private val gameId: String
) : BaseMenu(plugin, "matchmaking.yml") {

    fun open() {
        val title = config.getString("$gameId.title", gameId.uppercase())
        val gui = Gui.gui().title(plugin.format(title)).rows(config.getInt("rows", 6))
            .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
            .create()
        setupItems(gui)
        gui.open(viewer)
    }

    override fun setupItems(gui: Gui) {
        config.applyDecorations(gui)
        val amounts = config.getDoubleList("bet-amounts")
        val betSlots = config.getIntegerList("bet-slots")
        amounts.zip(betSlots).forEach { (amount, slot) ->
            val label = money(amount)
            val item = ItemBuilder.from(config.getMaterial("create-item.material", Material.EMERALD))
                .name(plugin.format(config.getString("create-item.name", "<green>Create %amount%").replace("%amount%", label)))
                .lore(config.getStringList("create-item.lore").map { plugin.format(it.replace("%amount%", label)) })
                .flags(*ItemFlag.values()).asGuiItem {
                    val created = if (gameId == "rps") plugin.rpsManager.createGame(viewer, amount)
                                  else plugin.tttManager.createGame(viewer, amount)
                    if (created != null) viewer.closeInventory()
                }
            gui.setItem(slot, item)
        }

        val games: List<MatchmakingSession> = if (gameId == "rps") plugin.rpsManager.getOpenGames() else plugin.tttManager.getOpenGames()
        val gameSlots = config.getIntegerList("game-slots")
        if (games.isEmpty()) {
            gui.setItem(config.getInt("no-games.slot", 31), config.getItemBuilder("no-games").asGuiItem())
        } else {
            games.take(gameSlots.size).forEachIndexed { index, session ->
                val own = session.creatorId == viewer.uniqueId
                val action = config.getString(if (own) "game-item.cancel" else "game-item.join", "<green>Click")
                val name = config.getString("game-item.name", "<white>%creator%").replace("%creator%", session.creatorName)
                val lore = config.getStringList("game-item.lore").map {
                    plugin.format(it.replace("%amount%", money(session.betAmount)).replace("%action%", action))
                }
                val item = ItemBuilder.skull().owner(Bukkit.getOfflinePlayer(session.creatorId))
                    .name(plugin.format(name)).lore(lore).flags(*ItemFlag.values()).asGuiItem {
                        if (own) {
                            if (gameId == "rps") plugin.rpsManager.cancelGame(viewer) else plugin.tttManager.cancelGame(viewer)
                            open()
                        } else {
                            if (gameId == "rps") plugin.rpsManager.joinGame(viewer, session.creatorName)
                            else plugin.tttManager.joinGame(viewer, session.creatorName)
                        }
                    }
                gui.setItem(gameSlots[index], item)
            }
        }

        gui.setItem(config.getInt("refresh.slot", 49), config.getItemBuilder("refresh").asGuiItem {
            viewer.playSound(viewer.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            open()
        })
    }

    private fun money(amount: Double) = "$" + NumberFormat.getNumberInstance(Locale.US).format(amount)
}
