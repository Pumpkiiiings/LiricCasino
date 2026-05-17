package liric.casino.tictactoe

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.config.MenuConfig
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

/**
 * GUI del tablero de Tic Tac Toe.
 *
 * Layout (6 filas, 54 slots):
 * Row 0 (0-8)  : borde
 * Row 1 (9-17) : borde | celda0 | borde | celda1 | borde | celda2 | borde
 * Row 2 (18-26): borde | celda3 | borde | celda4 | borde | celda5 | borde
 * Row 3 (27-35): borde | celda6 | borde | celda7 | borde | celda8 | borde
 * Row 4 (36-44): borde
 * Row 5 (45-53): info
 *
 * Celdas en slots: 10, 12, 14,  19, 21, 23,  28, 30, 32
 */
class TTTBoard(
    private val plugin: CasinoPlugin,
    private val session: TTTSession,
    private val player: Player
) {
    fun open() {
        val config = plugin.menuConfig("ttt.yml")
        val gui = Gui.gui()
            .title(plugin.format(buildTitle(config)))
            .rows(config.getInt("rows", 5))
            .disableAllInteractions()
            .create()

        refresh(gui, config)
        gui.open(player)
    }

    private fun buildTitle(config: MenuConfig): String {
        val myMark  = session.getMarkOf(player.uniqueId)
        val isMyTurn = session.currentTurn == player.uniqueId
        val opp = if (player.uniqueId == session.creatorId) session.joinerName ?: "Esperando..." else session.creatorName
        return if (isMyTurn)
            "<#FFD700><bold>TU TURNO</bold> <gray>| <white>vs $opp <gray>| ${myMark.name}"
        else
            "<gray>Turno de <white>$opp <gray>| ${myMark.name}"
    }

    private fun refresh(gui: Gui, config: MenuConfig) {
        val isMyTurn = session.currentTurn == player.uniqueId
        
        // Bordes decorativos
        val myTurnBorder = Material.valueOf(config.getString("my-turn-border-material", "YELLOW_STAINED_GLASS_PANE"))
        val oppTurnBorder = Material.valueOf(config.getString("opp-turn-border-material", "GRAY_STAINED_GLASS_PANE"))
        val borderM  = if (isMyTurn) myTurnBorder else oppTurnBorder

        // Decoraciones (Reemplazando el material en slots configurados)
        config.getMapList("decorations").forEach { dec ->
            val matStr = dec["material"].toString()
            val mat = if (matStr == "BLACK_STAINED_GLASS_PANE") borderM else Material.valueOf(matStr)
            val name = plugin.format(dec["name"].toString())
            val slots = dec["slots"] as? List<*> ?: emptyList<Any>()
            val item = ItemBuilder.from(mat).name(name).asGuiItem()
            slots.forEach { slot -> gui.setItem(slot.toString().toInt(), item) }
        }

        // Celdas del tablero
        val cellSlots = config.getIntegerList("cell-slots")
        if (cellSlots.size == 9) {
            cellSlots.forEachIndexed { cellIndex, slot ->
                val mark = session.board[cellIndex]
                val item = when (mark) {
                    TTTMark.X -> config.getItemBuilder("cell-x").asGuiItem()
                    TTTMark.O -> config.getItemBuilder("cell-o").asGuiItem()
                    TTTMark.NONE -> {
                        if (isMyTurn) {
                            config.getItemBuilder("cell-empty-my-turn").asGuiItem {
                                plugin.tttManager.playMove(session, player.uniqueId, cellIndex)
                            }
                        } else {
                            config.getItemBuilder("cell-empty-opp-turn").asGuiItem()
                        }
                    }
                }
                gui.setItem(slot, item)
            }
        }

        // Info row
        val myMark = session.getMarkOf(player.uniqueId)
        val opp = if (player.uniqueId == session.creatorId) session.joinerName ?: "Esperando..." else session.creatorName
        val turnStatusStr = if (isMyTurn) config.getString("turn-status-my-turn", "") else config.getString("turn-status-opp-turn", "")
        
        val infoItem = config.getItemBuilder("info-item")
            .name(plugin.format(config.getString("info-item.name", "")))
            .lore(config.getStringList("info-item.lore").map { 
                plugin.format(it.replace("%my_mark%", myMark.name)
                    .replace("%opponent%", opp)
                    .replace("%amount%", session.betAmount.toLong().toString())
                    .replace("%turn_status%", turnStatusStr))
            }).flags(*ItemFlag.values()).asGuiItem()
        gui.setItem(config.getInt("info-item.slot", 40), infoItem)
    }

    /** Recrea el GUI del jugador con estado actualizado. */
    fun update() {
        player.closeInventory()
        plugin.server.scheduler.runTaskLater(plugin, Runnable { open() }, 2L)
    }
}
