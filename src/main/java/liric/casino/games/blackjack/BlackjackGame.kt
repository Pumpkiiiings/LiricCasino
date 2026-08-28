package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import liric.casino.util.SchedulerUtil
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player

enum class GameStatus { PLAYER_TURN, DEALER_TURN, FINISHED }

class BlackjackSession(
    plugin: CasinoPlugin, 
    val player: Player, 
    var betAmount: Double
) : BaseMenu(plugin, "blackjack.yml") {

    private val deck = BjDeck()
    private val playerHand = mutableListOf<BjCard>()
    private val dealerHand = mutableListOf<BjCard>()
    private var status = GameStatus.PLAYER_TURN
    private var isFirstAction = true

    fun start() {
        playerHand.add(deck.draw())
        dealerHand.add(deck.draw())
        playerHand.add(deck.draw())
        dealerHand.add(deck.draw())

        val playerScore = BlackjackLogic.calculateScore(playerHand)
        val dealerScore = BlackjackLogic.calculateScore(dealerHand)

        if (playerScore == 21) {
            status = GameStatus.FINISHED
            val gui = buildGui()
            gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
            
            if (dealerScore == 21) {
                endGame(gui, "Tie. Both got Blackjack.", betAmount)
            } else {
                endGame(gui, "NATURAL BLACKJACK!", betAmount * 2.5)
            }
            setupItems(gui)
            gui.open(player)
            player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
            return
        }

        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        setupItems(gui)
        gui.open(player)
        player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
    }

    override fun setupItems(gui: Gui) {
        val filler = config.getItemBuilder("items.filler").asGuiItem()
        gui.filler.fill(filler)

        val playerScore = BlackjackLogic.calculateScore(playerHand)
        val dealerScore = BlackjackLogic.calculateScore(dealerHand)

        val dealerStatus = if (status == GameStatus.PLAYER_TURN) "?" else dealerScore.toString()
        val dealerMat = config.getMaterial("items.dealer-head.material", Material.SKELETON_SKULL)
        val dealerName = config.getString("items.dealer-head.name", "<#FF5555><bold>DEALER</bold>")
        val dealerLore = config.getStringList("items.dealer-head.lore").map { plugin.format(it.replace("{score}", dealerStatus)) }
        val dealerHead = ItemBuilder.from(dealerMat)
            .name(plugin.format(dealerName))
            .lore(dealerLore)
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

        val playerMat = config.getMaterial("items.player-head.material", Material.PLAYER_HEAD)
        val playerName = config.getString("items.player-head.name", "<#00FF7F><bold>YOUR HAND</bold>")
        val playerLore = config.getStringList("items.player-head.lore").map { 
            plugin.format(it.replace("{score}", playerScore.toString()).replace("{bet}", betAmount.toString())) 
        }
        val playerHead = ItemBuilder.from(playerMat)
            .name(plugin.format(playerName))
            .lore(playerLore)
            .asGuiItem()
        gui.setItem(31, playerHead)

        playerHand.forEachIndexed { index, card ->
            gui.setItem(38 + index, ItemBuilder.from(card.toItemStack(plugin)).asGuiItem())
        }

        if (status == GameStatus.PLAYER_TURN) {
            val hitBtn = config.getItemBuilder("items.hit")
                .asGuiItem {
                    if (status != GameStatus.PLAYER_TURN) return@asGuiItem
                    isFirstAction = false
                    playerHand.add(deck.draw())
                    player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)

                    if (BlackjackLogic.calculateScore(playerHand) > 21) {
                        status = GameStatus.FINISHED
                        endGame(gui, "You busted! You lost.", 0.0)
                        setupItems(gui)
                    } else {
                        setupItems(gui)
                    }
                }
            gui.setItem(48, hitBtn)

            val standBtn = config.getItemBuilder("items.stand")
                .asGuiItem {
                    if (status != GameStatus.PLAYER_TURN) return@asGuiItem
                    isFirstAction = false
                    status = GameStatus.DEALER_TURN
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    setupItems(gui)
                    playDealerTurn(gui)
                }
            gui.setItem(50, standBtn)

            if (isFirstAction) {
                val doubleMat = config.getMaterial("items.double.material", Material.GOLD_BLOCK)
                val doubleName = config.getString("items.double.name", "<#FFD700><bold>💰 DOUBLE DOWN</bold>")
                val doubleLore = config.getStringList("items.double.lore").map { 
                    plugin.format(it.replace("{bet}", betAmount.toString())) 
                }
                val doubleBtn = ItemBuilder.from(doubleMat)
                    .name(plugin.format(doubleName))
                    .lore(doubleLore)
                    .asGuiItem {
                        if (status != GameStatus.PLAYER_TURN || !isFirstAction) return@asGuiItem

                        isFirstAction = false
                        if (plugin.economyManager.withdrawPlayer(player, betAmount).transactionSuccess()) {
                            betAmount *= 2
                            playerHand.add(deck.draw())
                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

                            status = if (BlackjackLogic.calculateScore(playerHand) > 21) {
                                endGame(gui, plugin.messages.get("blackjack.double-busted").let { if (it is String) it else "Busted on double down! You lost." } as String, 0.0)
                                GameStatus.FINISHED
                            } else {
                                GameStatus.DEALER_TURN
                            }
                            setupItems(gui)
                            if (status == GameStatus.DEALER_TURN) playDealerTurn(gui)
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

    private fun playDealerTurn(gui: Gui) {
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
                setupItems(gui)
            } else {
                val playerScore = BlackjackLogic.calculateScore(playerHand)
                status = GameStatus.FINISHED

                when {
                    dealerScore > 21 -> endGame(gui, "Dealer busted! You win.", betAmount * 2)
                    playerScore > dealerScore -> endGame(gui, "You beat the Dealer!", betAmount * 2)
                    playerScore == dealerScore -> endGame(gui, "Push. You get your bet back.", betAmount)
                    else -> endGame(gui, "Dealer won. Better luck next time.", 0.0)
                }
                setupItems(gui)
                dealerCancel?.run()
            }
        }
    }

    private fun endGame(gui: Gui, reason: String, payout: Double) {
        val resultItem = ItemBuilder.from(if (payout > betAmount) Material.DIAMOND else if (payout == betAmount) Material.GOLD_INGOT else Material.COAL)
            .name(plugin.format(if (payout > betAmount) "<#00FF7F><bold>VICTORY!</bold>" else if (payout == betAmount) "<#FFD700><bold>TIE</bold>" else "<#FF5555><bold>DEFEAT</bold>"))
            .lore(plugin.format("<white>$reason"), plugin.format("<gray>Final payout: <#FFD700>$$payout"))
            .asGuiItem()

        gui.setItem(22, resultItem)
        gui.update()
        if (payout > 0) {
            val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, payout, "blackjack")
            plugin.economyManager.depositPlayer(player, netWin)
            if (payout > betAmount) {
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                player.sendMessage(plugin.messages.get("blackjack.win", "amount" to netWin.toString()))
                plugin.statsManager.recordBjGame(player.uniqueId, betAmount, netWin, isWin = true, isLoss = false, isBlackjack = payout == betAmount * 2.5)
            } else {
                player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                player.sendMessage(plugin.messages.get("blackjack.push", "amount" to netWin.toString()))
                plugin.statsManager.recordBjGame(player.uniqueId, betAmount, netWin, isWin = false, isLoss = false, isBlackjack = false)
            }
        } else {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            player.sendMessage(plugin.messages.get("blackjack.lose"))
            plugin.statsManager.recordBjGame(player.uniqueId, betAmount, 0.0, isWin = false, isLoss = true, isBlackjack = false)
        }
    }
}
