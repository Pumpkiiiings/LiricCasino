package liric.casino.games.poker

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag

class PokerMenu(private val plugin: CasinoPlugin, private val game: PokerGame, private val player: Player) {

    private val config = plugin.menuConfig("poker")
    private val gui = Gui.gui()
        .title(plugin.format(config.getString("title", "<#FF0000><bold>♠ TEXAS HOLD'EM ♠</bold></#FF0000>")!!))
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
        val filler = config.getItemBuilder("items.filler").asGuiItem()
        gui.filler.fill(filler)

        val me = game.players.find { it.uuid == player.uniqueId } ?: return
        val isMyTurn = game.getCurrentPlayer()?.uuid == player.uniqueId && game.state != PokerState.SHOWDOWN


        game.players.forEachIndexed { index, p ->
            if (p.uuid != player.uniqueId) {
                val status = if (p.hasFolded) "<#FF5555>Folded" else "<#00FF7F>Bet: $${p.currentBet}"
                val turnMark = if (game.getCurrentPlayer()?.uuid == p.uuid) "<#FFB400>▶ THEIR TURN ◀" else ""

                val headMat = config.getMaterial("items.player-head.material", Material.PLAYER_HEAD)
                val headName = config.getString("items.player-head.name", "<#E0E0E0><bold>{name}</bold></#E0E0E0>").replace("{name}", p.name)
                val headLoreList = config.getStringList("items.player-head.lore")
                val baseHead = ItemBuilder.from(headMat).name(plugin.format(headName))
                if (headLoreList.isNotEmpty()) {
                    baseHead.lore(headLoreList.map { plugin.format(it.replace("{status}", status).replace("{turn}", turnMark)) })
                } else {
                    baseHead.lore(plugin.format(status), plugin.format(turnMark))
                }
                gui.setItem(index, baseHead.asGuiItem())
            }
        }


        val tableSlots = listOf(20, 21, 22, 23, 24)
        for (i in 0..4) {
            if (i < game.communityCards.size) {

                val cardItem = ItemBuilder.from(game.communityCards[i].toItemStack(plugin)).asGuiItem()
                gui.setItem(tableSlots[i], cardItem)
            } else {
                val emptyCard = config.getItemBuilder("items.empty-card").flags(*ItemFlag.values()).asGuiItem()
                gui.setItem(tableSlots[i], emptyCard)
            }
        }


        val potMat = config.getMaterial("items.pot.material", Material.GOLD_BLOCK)
        val potName = config.getString("items.pot.name", "<#FFD700><bold>TOTAL POT: {pot}</bold></#FFD700>").replace("{pot}", game.pot.toString())
        val potLore = config.getStringList("items.pot.lore").map { plugin.format(it.replace("{state}", game.state.name)) }
        
        val potInfo = ItemBuilder.from(potMat).name(plugin.format(potName))
        if (potLore.isNotEmpty()) {
            potInfo.lore(potLore)
        } else {
            potInfo.lore(plugin.format("<#E0E0E0>State: ${game.state.name}</#E0E0E0>"))
        }
        gui.setItem(4, potInfo.asGuiItem())


        if (me.holeCards.size == 2) {

            val card1 = ItemBuilder.from(me.holeCards[0].toItemStack(plugin)).asGuiItem()
            val card2 = ItemBuilder.from(me.holeCards[1].toItemStack(plugin)).asGuiItem()

            gui.setItem(48, card1)
            gui.setItem(50, card2)
        }


        if (isMyTurn) {
            val timeMat = config.getMaterial("items.time.material", Material.CLOCK)
            val timeName = config.getString("items.time.name", "<#FFB400>Time: {time}s</#FFB400>").replace("{time}", game.countdownSeconds.toString())
            val timeItem = ItemBuilder.from(timeMat).name(plugin.format(timeName)).asGuiItem()
            gui.setItem(40, timeItem)


            val foldBtn = config.getItemBuilder("items.fold").asGuiItem {
                game.handleAction(player.uniqueId, "FOLD", 0.0)
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            gui.setItem(45, foldBtn)


            val callAmount = game.currentHighestBet - me.currentBet
            val callText = if (callAmount == 0.0) "CHECK" else "CALL ($$callAmount)"
            val callMat = config.getMaterial("items.call.material", Material.YELLOW_DYE)
            val callName = config.getString("items.call.name", "<#FFD700><bold>{text}</bold>").replace("{text}", callText)
            val callBtn = ItemBuilder.from(callMat).name(plugin.format(callName)).asGuiItem {
                game.handleAction(player.uniqueId, "CALL", 0.0)
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            }
            gui.setItem(49, callBtn)


            val raiseBtn = config.getItemBuilder("items.raise").asGuiItem {
                game.handleAction(player.uniqueId, "RAISE", 5000.0)
                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
            }
            gui.setItem(53, raiseBtn)
        } else {

            val waitBtn = config.getItemBuilder("items.wait").asGuiItem()
            gui.setItem(45, waitBtn)
            gui.setItem(49, waitBtn)
            gui.setItem(53, waitBtn)
        }
    }
}
