package liric.casino

import liric.casino.games.blackjack.*
import liric.casino.games.coinflip.CoinFlipCommand
import liric.casino.games.coinflip.CoinFlipManager
import liric.casino.commands.CasinoCommand
import liric.casino.config.MenuConfig
import liric.casino.config.MessagesConfig
import liric.casino.database.DatabaseManager
import liric.casino.economy.EconomyManager
import liric.casino.games.blackjack.BlackjackCommand
import liric.casino.games.blackjack.BlackjackInteractListener
import liric.casino.games.blackjack.BlackjackManager
import liric.casino.games.blackjack.BlackjackMultiGame
import liric.casino.games.coinflip.CoinFlipChatListener
import liric.casino.games.lottery.LotteryCommand
import liric.casino.games.lottery.LotteryManager
import liric.casino.games.poker.PokerCommand
import liric.casino.games.poker.PokerGame
import liric.casino.games.poker.PokerInteractListener
import liric.casino.games.poker.PokerManager
import liric.casino.games.roulette.RouletteCommand
import liric.casino.games.roulette.RouletteGame
import liric.casino.games.roulette.RouletteInteractListener
import liric.casino.games.roulette.RouletteManager
import liric.casino.games.roulette.RouletteMenu
import liric.casino.games.scratch.ScratchCommand
import liric.casino.games.scratch.ScratchListener
import liric.casino.games.scratch.TicketTier
import liric.casino.games.slots.SlotCommand
import liric.casino.games.slots.SlotInteractListener
import liric.casino.games.slots.SlotManager
import liric.casino.stats.CasinoPlaceholders
import liric.casino.stats.StatsListener
import liric.casino.stats.StatsManager
import liric.casino.util.ColorUtil
import liric.casino.util.ConfigUpdater
import liric.casino.webhook.WebhookManager
import liric.casino.games.rps.RPSManager
import liric.casino.games.rps.RPSCommand
import liric.casino.games.tictactoe.TTTManager
import liric.casino.games.tictactoe.TTTCommand
import liric.casino.games.racing.RaceManager
import liric.casino.games.racing.RaceCommand
import net.kyori.adventure.text.Component
import liric.casino.packet.EntityTracker
import liric.casino.packet.FakeEntityPacketListener
import com.github.retrooper.packetevents.PacketEvents
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class CasinoPlugin : JavaPlugin() {


    lateinit var messages: MessagesConfig
    val messagesConfig get() = messages.config
    private val menuConfigMap = mutableMapOf<String, MenuConfig>()


    lateinit var economyManager: EconomyManager


    lateinit var rouletteManager: RouletteManager
    lateinit var rouletteGame: RouletteGame


    lateinit var pokerManager: PokerManager
    lateinit var pokerGame: PokerGame


    lateinit var slotManager: SlotManager


    lateinit var blackjackManager: BlackjackManager
    lateinit var blackjackMultiGame: BlackjackMultiGame


    lateinit var lotteryManager: LotteryManager


    lateinit var coinFlipManager: CoinFlipManager


    lateinit var rpsManager: RPSManager
    lateinit var tttManager: TTTManager
    lateinit var raceManager: RaceManager


    lateinit var webhook: WebhookManager


    lateinit var db: DatabaseManager
    lateinit var statsManager: StatsManager


    fun format(text: String): Component = ColorUtil.parse(text)

    fun menuConfig(name: String): MenuConfig =
        menuConfigMap[name] ?: error("MenuConfig '$name' not loaded!")

    fun isGameEnabled(key: String): Boolean = config.getBoolean("$key.active", true)

    /** Returns true when running on Folia (threaded-region server). */
    fun isFolia(): Boolean = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        true
    } catch (_: ClassNotFoundException) { false }


    override fun onEnable() {

        saveDefaultConfig()
        ConfigUpdater.updateConfig(File(dataFolder, "config.yml"), "config.yml")


        TicketTier.loadFromConfig(config)


        messages = MessagesConfig(this)
        messages.load()


        webhook = WebhookManager(this)


        val menuNames = listOf(
            "ruleta.yml", "apuesta_ruleta.yml",
            "blackjack_choice.yml", "blackjack_bet.yml", "blackjack_tutorial.yml",
            "slots.yml", "scratch.yml", "coinflip.yml", "lottery.yml",
            "rps.yml", "ttt.yml", "racing.yml"
        )
        menuNames.forEach { name ->
            val cfg = MenuConfig(this, name)
            cfg.load()
            menuConfigMap[name] = cfg
        }


        economyManager = EconomyManager(this)
        if (!economyManager.setupVault()) {
            logger.severe("Vault not found or no economy plugin! Disabling...")
            server.pluginManager.disablePlugin(this)
            return
        }


        db = DatabaseManager(this)
        db.connect()
        statsManager = StatsManager(this)
        statsManager.startAutoSave()

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            CasinoPlaceholders(this).register()
        }



        if (isGameEnabled("roulette")) {
            rouletteManager = RouletteManager(this)
            rouletteGame    = RouletteGame(this)
            rouletteManager.loadRoulettes()
        }

        if (isGameEnabled("poker")) {
            pokerManager = PokerManager(this)
            pokerGame    = PokerGame(this)
        }

        if (isGameEnabled("slots")) {
            slotManager = SlotManager(this)
            slotManager.loadMachines()
        }

        if (isGameEnabled("blackjack")) {
            blackjackManager  = BlackjackManager(this)
            blackjackMultiGame = BlackjackMultiGame(this)
        }

        if (isGameEnabled("lottery")) {
            lotteryManager = LotteryManager(this)
            lotteryManager.start()
        }

        if (isGameEnabled("coinflip")) {
            coinFlipManager = CoinFlipManager(this)
        }

        if (isGameEnabled("rps")) {
            rpsManager = RPSManager(this)
        }
        
        if (isGameEnabled("ttt")) {
            tttManager = TTTManager(this)
        }
        
        if (isGameEnabled("racing")) {
            raceManager = RaceManager(this)
        }


        if (isGameEnabled("roulette")) {
            val rouletteCmd = RouletteCommand(this, rouletteGame)
            getCommand("ruleta")?.apply { setExecutor(rouletteCmd); tabCompleter = rouletteCmd }
            server.pluginManager.registerEvents(RouletteInteractListener(this), this)
        }

        if (isGameEnabled("scratch")) {
            val scratchCmd = ScratchCommand(this)
            getCommand("boleto")?.apply { setExecutor(scratchCmd); tabCompleter = scratchCmd }
            server.pluginManager.registerEvents(ScratchListener(this), this)
        }

        if (isGameEnabled("slots")) {
            val slotCmd = SlotCommand(this)
            getCommand("tragamonedas")?.apply { setExecutor(slotCmd); tabCompleter = slotCmd }
            server.pluginManager.registerEvents(SlotInteractListener(this), this)
        }

        if (isGameEnabled("poker")) {
            val pokerCmd = PokerCommand(this)
            getCommand("poker")?.apply { setExecutor(pokerCmd); tabCompleter = pokerCmd }
            server.pluginManager.registerEvents(PokerInteractListener(this), this)
        }

        if (isGameEnabled("blackjack")) {
            val bjCmd = BlackjackCommand(this)
            getCommand("blackjack")?.apply { setExecutor(bjCmd); tabCompleter = bjCmd }
            server.pluginManager.registerEvents(BlackjackInteractListener(this), this)
        }

        val casinoCmd = CasinoCommand(this)
        getCommand("casino")?.apply { setExecutor(casinoCmd); tabCompleter = casinoCmd }

        if (isGameEnabled("lottery")) {
            val lotteryCmd = LotteryCommand(this)
            getCommand("loteria")?.apply { setExecutor(lotteryCmd); tabCompleter = lotteryCmd }
        }

        if (isGameEnabled("coinflip")) {
            val coinFlipCmd = CoinFlipCommand(this)
            getCommand("coinflip")?.apply { setExecutor(coinFlipCmd); tabCompleter = coinFlipCmd }
            getCommand("cf")?.apply { setExecutor(coinFlipCmd); tabCompleter = coinFlipCmd }
            server.pluginManager.registerEvents(CoinFlipChatListener(this), this)
            server.pluginManager.registerEvents(liric.casino.games.coinflip.CoinFlipInteractListener(), this)
        }

        if (isGameEnabled("rps")) {
            val rpsCmd = RPSCommand(this)
            getCommand("rps")?.apply { setExecutor(rpsCmd); tabCompleter = rpsCmd }
        }

        if (isGameEnabled("ttt")) {
            val tttCmd = TTTCommand(this)
            getCommand("ttt")?.apply { setExecutor(tttCmd); tabCompleter = tttCmd }
        }

        if (isGameEnabled("racing")) {
            val raceCmd = RaceCommand(this)
            getCommand("carreras")?.apply { setExecutor(raceCmd); tabCompleter = raceCmd }
        }


        server.pluginManager.registerEvents(StatsListener(this), this)
        server.pluginManager.registerEvents(liric.casino.listeners.PlayerQuitListener(this), this)
        server.pluginManager.registerEvents(economyManager, this)

        // Init Packet-Based Entity System
        EntityTracker.start(this)
        PacketEvents.getAPI().eventManager.registerListener(FakeEntityPacketListener())

        sendStartupMessage()
    }

    fun handleDisconnect(uuid: java.util.UUID) {
        if (::coinFlipManager.isInitialized) coinFlipManager.handleDisconnect(uuid)
        if (::rpsManager.isInitialized) rpsManager.handleDisconnect(uuid)
        if (::tttManager.isInitialized) tttManager.handleDisconnect(uuid)
    }

    override fun onDisable() {
        if (::statsManager.isInitialized) statsManager.shutdown()
        if (::db.isInitialized) db.disconnect()

        if (::rouletteManager.isInitialized) rouletteManager.cleanupAll()
        if (::pokerManager.isInitialized) pokerManager.cleanupAll()
        if (::blackjackManager.isInitialized) blackjackManager.cleanupAll()
        if (::slotManager.isInitialized) slotManager.cleanupAll()
        if (::lotteryManager.isInitialized) lotteryManager.shutdown()
        if (::coinFlipManager.isInitialized) coinFlipManager.cleanupAll()
        if (::rpsManager.isInitialized) rpsManager.cleanupAll()
        if (::tttManager.isInitialized) tttManager.cleanupAll()
        if (::raceManager.isInitialized) raceManager.cleanupAll()

        server.consoleSender.sendMessage(format(
            "\n<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "  <#FFD700>Liric Casino <gray>v${description.version}\n" +
            "  <gray>Shutting down systems and cleaning up...\n" +
            "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
        ))
    }


    private fun sendStartupMessage() {
        val v = description.version
        val folia = isFolia()
        val platform = if (folia) "<light_purple><bold>Folia</bold></light_purple>" else "<aqua>Paper"
        val foliaLine = if (folia)
            "<light_purple>  ✦ <white>Folia detected — <yellow>running in threaded-region mode"
        else null

        val lines = buildList {
            add("")
            add("<red>▄█████  ▄▄▄   ▄▄▄▄ ▄▄ ▄▄  ▄▄  ▄▄▄  </red>")
            add("<red>██     ██▀██ ███▄▄ ██ ███▄██ ██▀██ </red>")
            add("<red>▀█████ ██▀██ ▄▄██▀ ██ ██ ▀██ ▀███▀ </red>")
            add("")
            add("<#FFD700>  Liric Casino <gray>v$v <dark_gray>| <gray>Platform: $platform")
            if (foliaLine != null) {
                add("<#FFB400>  $foliaLine")
            }
            add("")
            add("  <gray>Loaded Modules:")
            
            val modules = listOf(
                "roulette" to "Roulette",
                "blackjack" to "Blackjack",
                "poker" to "Poker",
                "slots" to "Slots",
                "scratch" to "Scratch Cards",
                "lottery" to "Lottery",
                "coinflip" to "CoinFlip",
                "rps" to "RPS",
                "ttt" to "Tic Tac Toe",
                "racing" to "Horse Racing"
            )
            modules.forEach { (key, name) ->
                if (isGameEnabled(key)) {
                    add("    <dark_gray>▪ <gray>$name <dark_gray>» <#00FF7F>Active")
                }
            }
            if (config.getBoolean("taxes.enabled", false)) {
                 add("    <dark_gray>▪ <gray>Tax System <dark_gray>» <#00FF7F>Active")
            }
            add("")
            add("  <#00FF7F>Plugin loaded and ready.</#00FF7F>")
            add("")
        }
        lines.forEach { server.consoleSender.sendMessage(format(it)) }
    }
}
