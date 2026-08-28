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
    private var dealerCancel: Runnable? = null


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
                val gui = Gui.gui().title(plugin.format("<#FF0000><bold>♠ MESA VIP BLACKJACK ♠</bold></#FF0000>"))
                    .rows(6).disableAllInteractions().create()
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

            isDealerTurn = true
            renderAll()
            playDealerTurn()
        } else {

            playersData[currentTurnIndex].status = PlayerStatus.PLAYING
            val pTurn = Bukkit.getPlayer(playersData[currentTurnIndex].uuid)
            pTurn?.playSound(pTurn.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f)
            pTurn?.sendMessage(plugin.format("<#00FF7F>¡ES TU TURNO! Elige tu jugada en el menú.</#00FF7F>"))
            renderAll()
        }
    }

    private fun renderAll() {
        playersData.forEach { data ->
            val player = Bukkit.getPlayer(data.uuid) ?: return@forEach
            val gui = playerGuis[data.uuid] ?: return@forEach

            val filler = ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(plugin.format(" ")).asGuiItem()
            gui.filler.fill(filler)


            val dealerScore = BlackjackLogic.calculateScore(dealerHand)
            val dStatus = if (!isDealerTurn) "?" else dealerScore.toString()
            gui.setItem(4, ItemBuilder.from(Material.SKELETON_SKULL).name(plugin.format("<#FF5555><bold>DEALER</bold>")).lore(plugin.format("<gray>Puntos: <white>$dStatus")).asGuiItem())

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
                val pName = Bukkit.getOfflinePlayer(pData.uuid).name ?: "Desconocido"
                val pScore = BlackjackLogic.calculateScore(pData.hand)

                val headMat = if (idx == currentTurnIndex) Material.PLAYER_HEAD else Material.ZOMBIE_HEAD
                val color = if (idx == currentTurnIndex) "<#00FF7F>" else "<gray>"

                val stateText = when(pData.status) {
                    PlayerStatus.WAITING_TURN -> "<gray>Esperando..."
                    PlayerStatus.PLAYING -> "<#FFB400>Pensando..."
                    PlayerStatus.STOOD -> "<#00FF7F>Se Plantó"
                    PlayerStatus.BUSTED -> "<#FF5555>Voló (Se pasó)"
                    PlayerStatus.BLACKJACK -> "<#FFD700>¡BLACKJACK!"
                }

                val headItem = ItemBuilder.from(headMat)
                    .name(plugin.format("$color<bold>$pName"))
                    .lore(
                        plugin.format("<gray>Apuesta: <#FFD700>$${pData.bet}"),
                        plugin.format("<gray>Puntos: <white>$pScore"),
                        plugin.format(stateText)
                    ).asGuiItem()

                gui.setItem(displaySlot, headItem)
                displaySlot += 2
            }


            val myScore = BlackjackLogic.calculateScore(data.hand)
            gui.setItem(31, ItemBuilder.from(Material.DIAMOND)
                .name(plugin.format("<#00FFFF><bold>TUS CARTAS</bold>"))
                .lore(plugin.format("<gray>Total: <white>$myScore")).asGuiItem())

            data.hand.forEachIndexed { index, card ->
                gui.setItem(38 + index, ItemBuilder.from(card.toItemStack(plugin)).asGuiItem())
            }


            if (data.status == PlayerStatus.PLAYING) {
                gui.setItem(48, ItemBuilder.from(Material.LIME_DYE).name(plugin.format("<#00FF7F><bold>✚ PEDIR CARTA (Hit)</bold>")).asGuiItem {
                    data.hand.add(deck.draw())
                    player.playSound(player.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                    if (BlackjackLogic.calculateScore(data.hand) > 21) {
                        data.status = PlayerStatus.BUSTED
                        player.sendMessage(plugin.format("<#FF5555>¡Te pasaste de 21! Has volado.</#FF5555>"))
                        advanceToNextValidTurn()
                    } else {
                        renderAll()
                    }
                })

                gui.setItem(50, ItemBuilder.from(Material.RED_DYE).name(plugin.format("<#FF5555><bold>✋ PLANTARSE (Stand)</bold>")).asGuiItem {
                    data.status = PlayerStatus.STOOD
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    advanceToNextValidTurn()
                })


                if (data.hand.size == 2) {
                    gui.setItem(49, ItemBuilder.from(Material.GOLD_BLOCK).name(plugin.format("<#FFD700><bold>💰 DOBLAR (Double)</bold>")).asGuiItem {
                        if (plugin.economyManager.withdrawPlayer(player, data.bet).transactionSuccess()) {
                            data.bet *= 2
                            data.hand.add(deck.draw())
                            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)

                            data.status = if (BlackjackLogic.calculateScore(data.hand) > 21) PlayerStatus.BUSTED else PlayerStatus.STOOD
                            advanceToNextValidTurn()
                        } else {
                            player.sendMessage(plugin.format("<red>No tienes dinero para doblar."))
                        }
                    })
                }
            }

            gui.update()
        }
    }

    private fun playDealerTurn() {
        SchedulerUtil.runGlobalTimer(plugin, 20L, 20L) {
            val dealerScore = BlackjackLogic.calculateScore(dealerHand)
            if (dealerScore < 17) {
                dealerHand.add(deck.draw())
                renderAll()
            } else {
                payoutTable()
                dealerCancel?.run()
            }
        }.also { dealerCancel = it }
    }

    private fun payoutTable() {
        val dealerScore = BlackjackLogic.calculateScore(dealerHand)
        val dealerBusted = dealerScore > 21
        val dealerHasBJ = dealerScore == 21 && dealerHand.size == 2

        playersData.forEach { data ->
            val player = Bukkit.getPlayer(data.uuid)
            val pScore = BlackjackLogic.calculateScore(data.hand)
            var winAmount = 0.0
            var message = ""

            when (data.status) {
                PlayerStatus.BUSTED -> {
                    message = "<#FF5555>Volaste. Perdiste $${data.bet}.</#FF5555>"
                }
                PlayerStatus.BLACKJACK -> {
                    if (dealerHasBJ) {
                        winAmount = data.bet
                        message = "<#FFD700>Empate de Blackjacks (Push). Recuperas $${data.bet}.</#FFD700>"
                    } else {
                        winAmount = data.bet * 2.5
                        message = "<#00FF7F>¡BLACKJACK! Ganaste $$winAmount.</#00FF7F>"
                    }
                }
                PlayerStatus.STOOD -> {
                    if (dealerBusted) {
                        winAmount = data.bet * 2
                        message = "<#00FF7F>El Dealer se pasó. ¡Ganaste $$winAmount!</#00FF7F>"
                    } else if (pScore > dealerScore) {
                        winAmount = data.bet * 2
                        message = "<#00FF7F>¡Le ganaste al Dealer! Recibes $$winAmount.</#00FF7F>"
                    } else if (pScore == dealerScore) {
                        winAmount = data.bet
                        message = "<#FFD700>Empate (Push). Recuperas $${data.bet}.</#FFD700>"
                    } else {
                        message = "<#FF5555>El Dealer gana. Perdiste $${data.bet}.</#FF5555>"
                    }
                }
                else -> {}
            }

            if (winAmount > 0) {
                plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(data.uuid), winAmount)
            }
            player?.sendMessage(plugin.format("<#FF0000>CASINO</#FF0000> <gray>» $message"))
            player?.closeInventory()
        }


        SchedulerUtil.runGlobalLater(plugin, 60L) {
            gameManager.resetGame()
        }
    }
}
