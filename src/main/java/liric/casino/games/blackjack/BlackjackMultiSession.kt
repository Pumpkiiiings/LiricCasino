package liric.casino.games.blackjack

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import java.util.UUID

enum class PlayerStatus { WAITING_TURN, PLAYING, STOOD, BUSTED, BLACKJACK }

class PlayerHandData(val uuid: UUID, var bet: Double, val hand: MutableList<BjCard>, var status: PlayerStatus)

class BlackjackMultiSession(
    private val plugin: CasinoPlugin,
    private val initialBets: Map<UUID, Double>,
    private val gameManager: BlackjackMultiGame
) {
    private val deck = BjDeck()
    private val dealerHand = mutableListOf<BjCard>()
    private val playersData = mutableListOf<PlayerHandData>()

    private var currentTurnIndex = 0
    private var isDealerTurn = false
    private var isFinished = false

    private val config = plugin.menuConfig("blackjack")


    private val playerGuis = mutableMapOf<UUID, Gui>()

    fun start() {

        initialBets.forEach { (uuid, bet) ->
            playersData.add(PlayerHandData(uuid, bet, mutableListOf(), PlayerStatus.WAITING_TURN))
        }


        repeat(2) {
            playersData.forEach { it.hand.add(deck.draw()) }
            dealerHand.add(deck.draw())
        }


        playersData.forEach { data ->
            val player = Bukkit.getPlayer(data.uuid)
            if (player != null) {
                val titleRaw = config.getString("multi-title", "<#FF0000><bold>♠ VIP BLACKJACK TABLE ♠</bold></#FF0000>")!!
                val gui = Gui.gui().title(plugin.format(titleRaw))
                    .rows(6).disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace().create()
                playerGuis[data.uuid] = gui
                gui.open(player)
            }

            if (BlackjackLogic.calculateScore(data.hand) == 21) {
                data.status = PlayerStatus.BLACKJACK
            }
        }

        advanceToNextValidTurn()
    }

    private fun advanceToNextValidTurn() {

        while (currentTurnIndex < playersData.size && playersData[currentTurnIndex].status != PlayerStatus.WAITING_TURN) {
            currentTurnIndex++
        }

        if (currentTurnIndex >= playersData.size) {
            if (!isDealerTurn) {
                isDealerTurn = true
                renderAll()
                playDealerTurn()
            }
        } else {

            playersData[currentTurnIndex].status = PlayerStatus.PLAYING
            val pTurn = Bukkit.getPlayer(playersData[currentTurnIndex].uuid)
            pTurn?.playSound(pTurn.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
            pTurn?.sendMessage(plugin.messages.get("blackjack.multi-turn"))
            renderAll()
        }
    }

    private fun renderAll() {
        playersData.forEach { data ->
            val player = Bukkit.getPlayer(data.uuid) ?: return@forEach
            val gui = playerGuis[data.uuid] ?: return@forEach

            val filler = config.getItemBuilder("items.multi-filler").asGuiItem()
            gui.filler.fill(filler)


            val dealerScore = BlackjackLogic.calculateScore(dealerHand)
            val dStatus = if (!isDealerTurn) "?" else dealerScore.toString()
            val dealerMat = config.getMaterial("items.dealer-head.material", Material.SKELETON_SKULL)
            val dealerName = config.getString("items.dealer-head.name", "<#FF5555><bold>DEALER</bold>")
            val dealerLore = config.getStringList("items.dealer-head.lore").map { plugin.format(it.replace("{score}", dStatus)) }
            gui.setItem(4, ItemBuilder.from(dealerMat).name(plugin.format(dealerName)).lore(dealerLore).asGuiItem())

            dealerHand.forEachIndexed { index, card ->
                val slot = 11 + index
                if (index == 1 && !isDealerTurn) {
                    gui.setItem(slot, ItemBuilder.from(card.toItemStack(plugin, true)).asGuiItem())
                } else {
                    gui.setItem(slot, ItemBuilder.from(card.toItemStack(plugin)).asGuiItem())
                }
            }


            var displaySlot = 19
            playersData.forEachIndexed { idx, pData ->
                val pName = Bukkit.getOfflinePlayer(pData.uuid).name ?: "Unknown"
                val pScore = BlackjackLogic.calculateScore(pData.hand)

                val headMatConfig = config.getMaterial("items.multi-player-head.material", Material.PLAYER_HEAD)
                val headMat = if (idx == currentTurnIndex) headMatConfig else Material.ZOMBIE_HEAD
                val color = if (idx == currentTurnIndex) "<#00FF7F>" else "<gray>"
                val stateText = when(pData.status) {
                    PlayerStatus.WAITING_TURN -> plugin.messages.get("blackjack.state-waiting")
                    PlayerStatus.PLAYING -> plugin.messages.get("blackjack.state-playing")
                    PlayerStatus.STOOD -> plugin.messages.get("blackjack.state-stood")
                    PlayerStatus.BUSTED -> plugin.messages.get("blackjack.state-busted")
                    PlayerStatus.BLACKJACK -> plugin.messages.get("blackjack.state-blackjack")
                }
                val headName = config.getString("items.multi-player-head.name", "{color}<bold>{name}")
                    .replace("{color}", color).replace("{name}", pName)
                val headLore = config.getStringList("items.multi-player-head.lore").map { line ->
                    if (line.contains("{state}")) stateText
                    else plugin.format(line.replace("{bet}", pData.bet.toString()).replace("{score}", pScore.toString()))
                }
                val headItem = ItemBuilder.from(headMat)
                    .name(plugin.format(headName))
                    .lore(headLore)
                    .asGuiItem()

                gui.setItem(displaySlot, headItem)
                displaySlot += 2
            }


            val myScore = BlackjackLogic.calculateScore(data.hand)
            val yourCardsMat = config.getMaterial("items.your-cards.material", Material.DIAMOND)
            val yourCardsName = config.getString("items.your-cards.name", "<#00FFFF><bold>YOUR CARDS</bold>")
            val yourCardsLore = config.getStringList("items.your-cards.lore").map { plugin.format(it.replace("{score}", myScore.toString())) }
            gui.setItem(31, ItemBuilder.from(yourCardsMat)
                .name(plugin.format(yourCardsName))
                .lore(yourCardsLore).asGuiItem())

            data.hand.forEachIndexed { index, card ->
                gui.setItem(38 + index, ItemBuilder.from(card.toItemStack(plugin)).asGuiItem())
            }


            if (data.status == PlayerStatus.PLAYING) {
                val hitBtn = config.getItemBuilder("items.hit").asGuiItem {
                    if (data.status != PlayerStatus.PLAYING) return@asGuiItem
                    data.hand.add(deck.draw())
                    player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                    if (BlackjackLogic.calculateScore(data.hand) > 21) {
                        data.status = PlayerStatus.BUSTED
                        player.sendMessage(plugin.messages.get("blackjack.multi-busted"))
                        advanceToNextValidTurn()
                    } else {
                        renderAll()
                    }
                }
                gui.setItem(48, hitBtn)

                val standBtn = config.getItemBuilder("items.stand").asGuiItem {
                    if (data.status != PlayerStatus.PLAYING) return@asGuiItem
                    data.status = PlayerStatus.STOOD
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    advanceToNextValidTurn()
                }
                gui.setItem(50, standBtn)

                if (data.hand.size == 2) {
                    val doubleMat = config.getMaterial("items.double.material", Material.GOLD_BLOCK)
                    val doubleName = config.getString("items.double.name", "<#FFD700><bold>💰 DOUBLE DOWN</bold>")
                    val doubleLore = config.getStringList("items.double.lore").map { plugin.format(it.replace("{bet}", data.bet.toString())) }
                    
                    val doubleBtn = ItemBuilder.from(doubleMat).name(plugin.format(doubleName)).lore(doubleLore).asGuiItem {
                        if (data.status != PlayerStatus.PLAYING) return@asGuiItem
                        if (plugin.economyManager.withdrawPlayer(player, data.bet)?.transactionSuccess() == true) {
                            data.bet *= 2
                            data.hand.add(deck.draw())
                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

                            data.status = if (BlackjackLogic.calculateScore(data.hand) > 21) PlayerStatus.BUSTED else PlayerStatus.STOOD
                            advanceToNextValidTurn()
                        } else {
                            player.sendMessage(plugin.format("<red>You don't have enough money to double down."))
                        }
                    }
                    gui.setItem(49, doubleBtn)
                }
            }

            gui.update()
        }
    }

    private fun playDealerTurn() {
        var dealerCancel: Runnable? = null
        dealerCancel = SchedulerUtil.runGlobalTimer(plugin, 20L, 20L) {
            if (isFinished) {
                dealerCancel?.run()
                return@runGlobalTimer
            }
            val dealerScore = BlackjackLogic.calculateScore(dealerHand)
            if (dealerScore < 17) {
                dealerHand.add(deck.draw())
                renderAll()
            } else {
                isFinished = true
                payoutTable()
                dealerCancel?.run()
            }
        }
    }

    private fun payoutTable() {
        val dealerScore = BlackjackLogic.calculateScore(dealerHand)
        val dealerBusted = dealerScore > 21
        val dealerHasBJ = dealerScore == 21 && dealerHand.size == 2

        playersData.forEach { data ->
            val player = Bukkit.getPlayer(data.uuid)
            val pScore = BlackjackLogic.calculateScore(data.hand)
            var winAmount = 0.0
            var message: net.kyori.adventure.text.Component = net.kyori.adventure.text.Component.empty()

            when (data.status) {
                PlayerStatus.BUSTED -> {
                    message = plugin.messages.get("blackjack.multi-lose-busted", "amount" to data.bet.toString())
                }
                PlayerStatus.BLACKJACK -> {
                    if (dealerHasBJ) {
                        winAmount = data.bet
                        message = plugin.messages.get("blackjack.multi-push-blackjack", "amount" to data.bet.toString())
                    } else {
                        winAmount = data.bet * 2.5
                        message = plugin.messages.get("blackjack.multi-win-blackjack", "amount" to winAmount.toString())
                    }
                }
                PlayerStatus.STOOD -> {
                    if (dealerBusted) {
                        winAmount = data.bet * 2
                        message = plugin.messages.get("blackjack.multi-win-dealer-busted", "amount" to winAmount.toString())
                    } else if (pScore > dealerScore) {
                        winAmount = data.bet * 2
                        message = plugin.messages.get("blackjack.multi-win-normal", "amount" to winAmount.toString())
                    } else if (pScore == dealerScore) {
                        winAmount = data.bet
                        message = plugin.messages.get("blackjack.multi-push", "amount" to data.bet.toString())
                    } else {
                        message = plugin.messages.get("blackjack.multi-lose-normal", "amount" to data.bet.toString())
                    }
                }
                else -> {}
            }

            if (winAmount > 0) {
                val (netWin, _) = liric.casino.util.TaxUtil.applyTax(plugin, winAmount, "blackjack")
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(data.uuid), netWin)
            }
            player?.sendMessage(message)
            player?.closeInventory()
        }


        SchedulerUtil.runGlobalLater(plugin, 60L) {
            gameManager.resetGame()
        }
    }
}
