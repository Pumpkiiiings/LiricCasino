package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

enum class GameStatus { PLAYER_TURN, DEALER_TURN, FINISHED }

class BlackjackSession(val plugin: CasinoPlugin, val player: Player, var betAmount: Double) {


    private val deck = BjDeck()

    private val playerHand = mutableListOf<BjCard>()
    private val dealerHand = mutableListOf<BjCard>()

    private var status = GameStatus.PLAYER_TURN
    private var isFirstAction = true

    private val gui = Gui.gui()
        .title(plugin.format("<#FF0000><bold>♠ BLACKJACK 21 ♠</bold></#FF0000>"))
        .rows(6)
        .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        .create()

    fun start() {

        playerHand.add(deck.draw())
        dealerHand.add(deck.draw())
        playerHand.add(deck.draw())
        dealerHand.add(deck.draw())

        val playerScore = BlackjackLogic.calculateScore(playerHand)
        val dealerScore = BlackjackLogic.calculateScore(dealerHand)


        if (playerScore == 21) {
            status = GameStatus.FINISHED
            if (dealerScore == 21) {
                endGame("Empate. Ambos sacaron Blackjack.", betAmount)
            } else {
                endGame("¡BLACKJACK NATURAL!", betAmount * 2.5)
            }
        }

        render()
        gui.open(player)
        player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
    }

    private fun render() {
        val filler = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE).name(plugin.format(" ")).asGuiItem()
        gui.filler.fill(filler)

        val playerScore = BlackjackLogic.calculateScore(playerHand)
        val dealerScore = BlackjackLogic.calculateScore(dealerHand)


        val dealerStatus = if (status == GameStatus.PLAYER_TURN) "?" else dealerScore.toString()
        val dealerHead = ItemBuilder.from(Material.SKELETON_SKULL)
            .name(plugin.format("<#FF5555><bold>MANO DEL DEALER</bold>"))
            .lore(plugin.format("<gray>Puntaje: <white>$dealerStatus"))
            .asGuiItem()
        gui.setItem(4, dealerHead)

        dealerHand.forEachIndexed { index, card ->
            val slot = 11 + index
            if (index == 1 && status == GameStatus.PLAYER_TURN) {
                gui.setItem(slot, ItemBuilder.from(card.toItemStack(plugin, true)).asGuiItem())
            } else {
                gui.setItem(slot, ItemBuilder.from(card.toItemStack(plugin)).asGuiItem())
            }
        }


        val playerHead = ItemBuilder.from(Material.PLAYER_HEAD)
            .name(plugin.format("<#00FF7F><bold>TU MANO</bold>"))
            .lore(plugin.format("<gray>Puntaje: <white>$playerScore"), plugin.format("<gray>Apuesta: <#FFD700>$${betAmount}"))
            .asGuiItem()
        gui.setItem(31, playerHead)

        playerHand.forEachIndexed { index, card ->
            gui.setItem(38 + index, ItemBuilder.from(card.toItemStack(plugin)).asGuiItem())
        }


        if (status == GameStatus.PLAYER_TURN) {

            val hitBtn = ItemBuilder.from(Material.LIME_DYE)
                .name(plugin.format("<#00FF7F><bold>✚ PEDIR CARTA (Hit)</bold>"))
                .asGuiItem {
                    if (status != GameStatus.PLAYER_TURN) return@asGuiItem
                    isFirstAction = false
                    playerHand.add(deck.draw())
                    player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)

                    if (BlackjackLogic.calculateScore(playerHand) > 21) {
                        status = GameStatus.FINISHED
                        endGame("¡Te pasaste de 21! Has perdido.", 0.0)
                    } else {
                        render()
                    }
                }
            gui.setItem(48, hitBtn)


            val standBtn = ItemBuilder.from(Material.RED_DYE)
                .name(plugin.format("<#FF5555><bold>✋ PLANTARSE (Stand)</bold>"))
                .asGuiItem {
                    if (status != GameStatus.PLAYER_TURN) return@asGuiItem
                    isFirstAction = false
                    status = GameStatus.DEALER_TURN
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    render()
                    playDealerTurn()
                }
            gui.setItem(50, standBtn)


            if (isFirstAction) {
                val doubleBtn = ItemBuilder.from(Material.GOLD_BLOCK)
                    .name(plugin.format("<#FFD700><bold>💰 DOBLAR (Double Down)</bold>"))
                    .lore(plugin.format("<gray>Duplica tu apuesta ($${betAmount}), recibes"), plugin.format("<gray>EXACTAMENTE 1 carta y te plantas."))
                    .asGuiItem {
                        if (status != GameStatus.PLAYER_TURN || !isFirstAction) return@asGuiItem

                        isFirstAction = false
                        if (plugin.economyManager.withdrawPlayer(player, betAmount).transactionSuccess()) {
                            betAmount *= 2
                            playerHand.add(deck.draw())
                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

                            status = if (BlackjackLogic.calculateScore(playerHand) > 21) {
                                endGame("\u00a1Te pasaste de 21 al doblar! Has perdido.", 0.0)
                                GameStatus.FINISHED
                            } else {
                                GameStatus.DEALER_TURN
                            }
                            render()
                            if (status == GameStatus.DEALER_TURN) playDealerTurn()
                        } else {
                            isFirstAction = true
                            player.sendMessage(plugin.messages.get("blackjack.no-funds"))
                            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        }
                    }
                gui.setItem(49, doubleBtn)
            } else {
                val empty = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE).name(plugin.format(" ")).asGuiItem()
                gui.setItem(49, empty)
            }
        } else {

            val empty = ItemBuilder.from(Material.GREEN_STAINED_GLASS_PANE).name(plugin.format(" ")).asGuiItem()
            gui.setItem(48, empty)
            gui.setItem(49, empty)
            gui.setItem(50, empty)
        }
        gui.update()
    }

    private fun playDealerTurn() {
        var dealerCancel: Runnable? = null
        dealerCancel = SchedulerUtil.runGlobalTimer(plugin, 20L, 20L) {
            if (status == GameStatus.FINISHED) {
                dealerCancel?.run()
                return@runGlobalTimer
            }

            val dealerScore = BlackjackLogic.calculateScore(dealerHand)

            if (dealerScore < 17) {
                dealerHand.add(deck.draw())
                player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                render()
            } else {

                val playerScore = BlackjackLogic.calculateScore(playerHand)
                status = GameStatus.FINISHED

                when {
                    dealerScore > 21 -> endGame("¡El Dealer se pasó! Tú ganas.", betAmount * 2)
                    playerScore > dealerScore -> endGame("¡Le ganaste al Dealer!", betAmount * 2)
                    playerScore == dealerScore -> endGame("Empate (Push). Recuperas tu apuesta.", betAmount)
                    else -> endGame("El Dealer ganó. Suerte para la próxima.", 0.0)
                }
                dealerCancel?.run()
            }
        }
    }

    private fun endGame(reason: String, payout: Double) {
        render()


        val resultItem = ItemBuilder.from(if (payout > betAmount) Material.DIAMOND else if (payout == betAmount) Material.GOLD_INGOT else Material.COAL)
            .name(plugin.format(if (payout > betAmount) "<#00FF7F><bold>¡VICTORIA!</bold>" else if (payout == betAmount) "<#FFD700><bold>EMPATE</bold>" else "<#FF5555><bold>DERROTA</bold>"))
            .lore(plugin.format("<white>$reason"), plugin.format("<gray>Pago final: <#FFD700>$$payout"))
            .asGuiItem()

        gui.setItem(22, resultItem)
        gui.update()

        if (payout > 0) {
            plugin.economyManager.depositPlayer(player, payout)
            if (payout > betAmount) {
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                player.sendMessage(plugin.messages.get("blackjack.win", "amount" to payout.toString()))
                plugin.statsManager.recordBjGame(player.uniqueId, betAmount, payout, isWin = true, isLoss = false, isBlackjack = payout == betAmount * 2.5)
            } else {
                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                player.sendMessage(plugin.messages.get("blackjack.push", "amount" to payout.toString()))
                plugin.statsManager.recordBjGame(player.uniqueId, betAmount, payout, isWin = false, isLoss = false, isBlackjack = false)
            }
        } else {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            player.sendMessage(plugin.messages.get("blackjack.lose"))
            plugin.statsManager.recordBjGame(player.uniqueId, betAmount, 0.0, isWin = false, isLoss = true, isBlackjack = false)
        }
    }
}
