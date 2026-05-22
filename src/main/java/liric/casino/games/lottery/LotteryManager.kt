package liric.casino.games.lottery

import liric.casino.CasinoPlugin
import liric.casino.util.TaxUtil
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.UUID
import kotlin.random.Random
import kotlin.text.clear
import kotlin.text.toLong

class LotteryManager(private val plugin: CasinoPlugin) {

    private val tickets = mutableListOf<LotteryTicket>()
    private var jackpot: Double = 0.0
    private var secondsUntilDraw: Int = 0
    private var drawTask: BukkitRunnable? = null

    private val dataFile = File(plugin.dataFolder, "lottery.yml")
    private var dataConfig = YamlConfiguration.loadConfiguration(dataFile)

    // ─── Config helpers ──────────────────────────────────────────────────
    private fun ticketPrice()     = plugin.config.getDouble("lottery.ticket-price", 5000.0)
    private fun maxPerPlayer()    = plugin.config.getInt("lottery.max-tickets-per-player", 5)
    private fun drawIntervalSec() = plugin.config.getInt("lottery.draw-interval-minutes", 60) * 60
    private fun numberRange()     = plugin.config.getInt("lottery.number-range", 1000)
    private fun jackpotStart()    = plugin.config.getDouble("lottery.jackpot-start", 50000.0)
    private fun jackpotContrib()  = plugin.config.getDouble("lottery.jackpot-contribution", 0.5)

    // ─── Inicio ──────────────────────────────────────────────────────────
    fun start() {
        loadData()
        if (jackpot < jackpotStart()) jackpot = jackpotStart()
        secondsUntilDraw = drawIntervalSec()
        startCountdown()
    }

    fun shutdown() {
        drawTask?.cancel()
        saveData()
    }

    // ─── Compra de boleto ────────────────────────────────────────────────
    fun buyTicket(player: Player, amount: Int = 1) {
        val price = ticketPrice()
        val maxPer = maxPerPlayer()
        val currentCount = tickets.count { it.ownerUuid == player.uniqueId }

        if (currentCount + amount > maxPer) {
            player.sendMessage(plugin.messages.get("lottery.max-tickets", "max" to maxPer.toString()))
            return
        }

        val totalCost = price * amount
        if (!plugin.economyManager.withdrawPlayer(player, totalCost).transactionSuccess()) {
            player.sendMessage(plugin.messages.get("lottery.no-funds", "cost" to String.format("%.0f", totalCost)))
            return
        }

        val contribution = totalCost * jackpotContrib()
        jackpot += contribution

        val boughtNumbers = mutableListOf<Int>()
        repeat(amount) {
            val number = Random.nextInt(1, numberRange() + 1)
            tickets.add(LotteryTicket(ownerUuid = player.uniqueId, ownerName = player.name, number = number))
            boughtNumbers.add(number)
        }

        plugin.statsManager.recordLotteryBuy(player.uniqueId, totalCost)
        saveData()

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f)
        player.sendMessage(plugin.messages.get(
            "lottery.bought",
            "amount" to amount.toString(),
            "cost"   to String.format("%.0f", totalCost),
            "numbers" to boughtNumbers.joinToString(", "),
            "jackpot" to String.format("%.0f", jackpot)
        ))
    }

    // ─── Admin: dar boleto gratis ─────────────────────────────────────────
    fun giveTicket(player: Player, amount: Int) {
        repeat(amount) {
            val number = Random.nextInt(1, numberRange() + 1)
            tickets.add(LotteryTicket(ownerUuid = player.uniqueId, ownerName = player.name, number = number))
        }
        saveData()
        player.sendMessage(plugin.messages.get("lottery.received", "amount" to amount.toString()))
    }

    // ─── Info ────────────────────────────────────────────────────────────
    fun getJackpot() = jackpot
    fun getTicketCount() = tickets.size
    fun getPlayerTickets(uuid: UUID) = tickets.filter { it.ownerUuid == uuid }
    fun getSecondsUntilDraw() = secondsUntilDraw

    // ─── Countdown & Draw ────────────────────────────────────────────────
    private fun startCountdown() {
        drawTask?.cancel()
        drawTask = object : BukkitRunnable() {
            override fun run() {
                secondsUntilDraw--

                // Anuncios
                when (secondsUntilDraw) {
                    3600 -> broadcast("lottery.announce-1h")
                    1800 -> broadcast("lottery.announce-30m")
                    600  -> broadcast("lottery.announce-10m")
                    60   -> broadcast("lottery.announce-1m")
                    30   -> broadcast("lottery.announce-30s")
                    10   -> broadcast("lottery.announce-10s")
                }

                if (secondsUntilDraw <= 0) {
                    cancel()
                    runDraw()
                }
            }
        }
        drawTask!!.runTaskTimer(plugin, 0L, 20L)
    }

    fun forceStart() {
        drawTask?.cancel()
        runDraw()
    }

    private fun runDraw() {
        val winningNumber = Random.nextInt(1, numberRange() + 1)

        // Buscar ganadores exactos
        val exactWinners = tickets.filter { it.number == winningNumber }

        if (exactWinners.isEmpty()) {
            // Nadie ganó: el pozo crece, anuncio
            plugin.server.broadcast(plugin.messages.get("lottery.no-winner",
                "number" to winningNumber.toString(),
                "jackpot" to String.format("%.0f", jackpot)
            ))
        } else {
            // Dividir el pozo entre ganadores
            val share = jackpot / exactWinners.size
            val (netShare, tax) = TaxUtil.applyTax(plugin, share, "lottery")

            exactWinners.forEach { ticket ->
                val player = Bukkit.getPlayer(ticket.ownerUuid)
                if (player != null && player.isOnline) {
                    plugin.economyManager.depositPlayer(player, netShare)
                    player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                    player.sendMessage(plugin.messages.get("lottery.winner-self",
                        "number" to winningNumber.toString(),
                        "amount" to String.format("%.0f", netShare),
                        "tax" to TaxUtil.taxMessage(plugin, tax * exactWinners.size)
                    ))
                } else {
                    plugin.economyManager.depositPlayer(Bukkit.getOfflinePlayer(ticket.ownerUuid), netShare)
                }
                plugin.statsManager.recordLotteryWin(ticket.ownerUuid, netShare)
            }

            // Anuncio global
            val winnersStr = exactWinners.joinToString(", ") { it.ownerName }
            plugin.server.broadcast(plugin.messages.get("lottery.winner-broadcast",
                "number" to winningNumber.toString(),
                "winners" to winnersStr,
                "amount" to String.format("%.0f", netShare)
            ))

            // Webhook Discord
            plugin.webhook.sendLotteryWinner(winnersStr, netShare, winningNumber)

            // Resetear pozo
            jackpot = jackpotStart()
        }

        // Limpiar tickets
        tickets.clear()
        saveData()

        // Reiniciar countdown
        secondsUntilDraw = drawIntervalSec()
        startCountdown()
    }

    private fun broadcast(msgKey: String) {
        plugin.server.broadcast(plugin.messages.get(msgKey,
            "jackpot" to String.format("%.0f", jackpot),
            "tickets" to tickets.size.toString(),
            "time" to formatTime(secondsUntilDraw)
        ))
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else  -> "${s}s"
        }
    }

    // ─── Persistencia ────────────────────────────────────────────────────
    private fun saveData() {
        dataConfig.set("jackpot", jackpot)
        val ticketList = tickets.map { t ->
            "${t.id}|${t.ownerUuid}|${t.ownerName}|${t.number}|${t.purchasedAt}"
        }
        dataConfig.set("tickets", ticketList)
        dataConfig.save(dataFile)
    }

    private fun loadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)
        jackpot = dataConfig.getDouble("jackpot", jackpotStart())
        tickets.clear()
        dataConfig.getStringList("tickets").forEach { line ->
            runCatching {
                val parts = line.split("|")
                if (parts.size == 5) {
                    tickets.add(LotteryTicket(
                        id          = UUID.fromString(parts[0]),
                        ownerUuid   = UUID.fromString(parts[1]),
                        ownerName   = parts[2],
                        number      = parts[3].toInt(),
                        purchasedAt = parts[4].toLong()
                    ))
                }
            }
        }
    }
}
