package liric.casino.games.poker

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class PokerMenu(private val plugin: CasinoPlugin, private val game: PokerGame, private val player: Player) {

    private val gui = Gui.gui()
        .title(plugin.format("<#FF0000><bold>♠ TEXAS HOLD'EM ♠</bold></#FF0000>"))
        .rows(6)
        .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        .create()

    fun open() {
        render()
        gui.open(player)
    }

    fun updateExisting(target: Player) {
        render()
        gui.update()
    }

    private fun render() {
        val filler = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE).name(plugin.format(" ")).asGuiItem()
        gui.filler.fill(filler)

        val me = game.players.find { it.uuid == player.uniqueId } ?: return
        val isMyTurn = game.getCurrentPlayer()?.uuid == player.uniqueId && game.state != PokerState.SHOWDOWN


        game.players.forEachIndexed { index, p ->
            if (p.uuid != player.uniqueId) {
                val status = if (p.hasFolded) "<#FF5555>Se retiró" else "<#00FF7F>Apostado: $${p.currentBet}"
                val turnMark = if (game.getCurrentPlayer()?.uuid == p.uuid) "<#FFB400>▶ SU TURNO ◀" else ""

                val head = ItemBuilder.from(Material.PLAYER_HEAD)
                    .name(plugin.format("<#E0E0E0><bold>${p.name}</bold></#E0E0E0>"))
                    .lore(plugin.format(status), plugin.format(turnMark))
                    .asGuiItem()
                gui.setItem(index, head)
            }
        }


        val tableSlots = listOf(20, 21, 22, 23, 24)
        for (i in 0..4) {
            if (i < game.communityCards.size) {

                val cardItem = ItemBuilder.from(game.communityCards[i].toItemStack(plugin)).asGuiItem()
                gui.setItem(tableSlots[i], cardItem)
            } else {
                val emptyCard = ItemBuilder.from(Material.MAP).name(plugin.format("<gray>CARTA EN MESA</gray>")).flags(*ItemFlag.values()).asGuiItem()
                gui.setItem(tableSlots[i], emptyCard)
            }
        }


        val potInfo = ItemBuilder.from(Material.GOLD_BLOCK)
            .name(plugin.format("<#FFD700><bold>POZO TOTAL: $${game.pot}</bold></#FFD700>"))
            .lore(plugin.format("<#E0E0E0>Estado: ${game.state.name}</#E0E0E0>"))
            .asGuiItem()
        gui.setItem(4, potInfo)


        if (me.holeCards.size == 2) {

            val card1 = ItemBuilder.from(me.holeCards[0].toItemStack(plugin)).asGuiItem()
            val card2 = ItemBuilder.from(me.holeCards[1].toItemStack(plugin)).asGuiItem()

            gui.setItem(48, card1)
            gui.setItem(50, card2)
        }


        if (isMyTurn) {
            val timeItem = ItemBuilder.from(Material.CLOCK).name(plugin.format("<#FFB400>Tiempo: ${game.countdownSeconds}s</#FFB400>")).asGuiItem()
            gui.setItem(40, timeItem)


            val foldBtn = ItemBuilder.from(Material.RED_DYE).name(plugin.format("<#FF5555><bold>RETIRARSE (Fold)</bold>")).asGuiItem {
                game.handleAction(player.uniqueId, "FOLD", 0.0)
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            gui.setItem(45, foldBtn)


            val callAmount = game.currentHighestBet - me.currentBet
            val callText = if (callAmount == 0.0) "PASAR (Check)" else "IGUALAR ($$callAmount)"
            val callBtn = ItemBuilder.from(Material.YELLOW_DYE).name(plugin.format("<#FFD700><bold>$callText</bold>")).asGuiItem {
                game.handleAction(player.uniqueId, "CALL", 0.0)
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            gui.setItem(49, callBtn)


            val raiseBtn = ItemBuilder.from(Material.LIME_DYE).name(plugin.format("<#00FF7F><bold>SUBIR (Raise +$5,000)</bold>")).asGuiItem {
                game.handleAction(player.uniqueId, "RAISE", 5000.0)
                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
            }
            gui.setItem(53, raiseBtn)
        } else {

            val waitBtn = ItemBuilder.from(Material.BARRIER).name(plugin.format("<#555555>Espera tu turno...</#555555>")).asGuiItem()
            gui.setItem(45, waitBtn)
            gui.setItem(49, waitBtn)
            gui.setItem(53, waitBtn)
        }
    }
}
