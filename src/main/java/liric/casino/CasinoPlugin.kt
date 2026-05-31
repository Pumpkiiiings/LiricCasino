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
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class CasinoPlugin : JavaPlugin() {


    lateinit var messages: MessagesConfig
    private val menuConfigMap = mutableMapOf<String, MenuConfig>()


    lateinit var economyManager: EconomyManager


    lateinit var rouletteManager: RouletteManager
    lateinit var rouletteGame: RouletteGame
    lateinit var rouletteMenu: RouletteMenu


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



        rouletteManager = RouletteManager(this)
        rouletteGame    = RouletteGame(this)
        rouletteMenu    = RouletteMenu(this, rouletteGame)
        rouletteManager.loadRoulettes()


        pokerManager = PokerManager(this)
        pokerGame    = PokerGame(this)


        slotManager = SlotManager(this)
        slotManager.loadMachines()


        blackjackManager  = BlackjackManager(this)
        blackjackMultiGame = BlackjackMultiGame(this)


        lotteryManager = LotteryManager(this)
        lotteryManager.start()


        coinFlipManager = CoinFlipManager(this)


        rpsManager = RPSManager(this)
        tttManager = TTTManager(this)
        raceManager = RaceManager(this)


        val rouletteCmd = RouletteCommand(this, rouletteMenu, rouletteGame)
        getCommand("ruleta")?.apply { setExecutor(rouletteCmd); tabCompleter = rouletteCmd }

        val scratchCmd = ScratchCommand(this)
        getCommand("boleto")?.apply { setExecutor(scratchCmd); tabCompleter = scratchCmd }

        val slotCmd = SlotCommand(this)
        getCommand("tragamonedas")?.apply { setExecutor(slotCmd); tabCompleter = slotCmd }

        val pokerCmd = PokerCommand(this)
        getCommand("poker")?.apply { setExecutor(pokerCmd); tabCompleter = pokerCmd }

        val bjCmd = BlackjackCommand(this)
        getCommand("blackjack")?.apply { setExecutor(bjCmd); tabCompleter = bjCmd }

        val casinoCmd = CasinoCommand(this)
        getCommand("casino")?.apply { setExecutor(casinoCmd); tabCompleter = casinoCmd }

        val lotteryCmd = LotteryCommand(this)
        getCommand("loteria")?.apply { setExecutor(lotteryCmd); tabCompleter = lotteryCmd }

        val coinFlipCmd = CoinFlipCommand(this)
        getCommand("coinflip")?.apply { setExecutor(coinFlipCmd); tabCompleter = coinFlipCmd }
        getCommand("cf")?.apply { setExecutor(coinFlipCmd); tabCompleter = coinFlipCmd }

        val rpsCmd = RPSCommand(this)
        getCommand("rps")?.apply { setExecutor(rpsCmd); tabCompleter = rpsCmd }

        val tttCmd = TTTCommand(this)
        getCommand("ttt")?.apply { setExecutor(tttCmd); tabCompleter = tttCmd }

        val raceCmd = RaceCommand(this)
        getCommand("carreras")?.apply { setExecutor(raceCmd); tabCompleter = raceCmd }


        server.pluginManager.registerEvents(RouletteInteractListener(this), this)
        server.pluginManager.registerEvents(ScratchListener(this), this)
        server.pluginManager.registerEvents(PokerInteractListener(this), this)
        server.pluginManager.registerEvents(BlackjackInteractListener(this), this)
        server.pluginManager.registerEvents(SlotInteractListener(this), this)
        server.pluginManager.registerEvents(StatsListener(this), this)
        server.pluginManager.registerEvents(CoinFlipChatListener(this), this)
        server.pluginManager.registerEvents(liric.casino.listeners.PlayerQuitListener(this), this)


        sendStartupMessage()
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
            "<red>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "<dark_red><bold>  Liric Casino</bold> <gray>v${description.version}\n" +
            "<red>  Disabling systems and cleaning up entities...\n" +
            "<red>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        ))
    }


    private fun sendStartupMessage() {
        val v = description.version
        val folia = isFolia()
        val platform = if (folia) "<light_purple><bold>Folia</bold></light_purple>" else "<aqua>Paper"
        val foliaLine = if (folia)
            "<light_purple>  ✦ <white>Folia detected — <yellow>running in threaded-region mode"
        else null

        // Helper: returns a coloured status tag based on config key
        fun gameStatus(key: String): String = if (isGameEnabled(key))
            "<green><bold>ACTIVE</bold></green>"
        else
            "<red><bold>DISABLED</bold></red>"

        val taxStatus = if (config.getBoolean("taxes.enabled", false))
            "<green><bold>ACTIVE</bold></green>" else "<red><bold>DISABLED</bold></red>"

        val lines = buildList {
            add("<gray>")
            add("<gradient:#FFD700:#FF6B6B>  ██████╗ █████╗ ███████╗██╗███╗   ██╗ ██████╗ </gradient>")
            add("<gradient:#FFD700:#FF6B6B> ██╔════╝██╔══██╗██╔════╝██║████╗  ██║██╔═══██╗</gradient>")
            add("<gradient:#FFD700:#FF6B6B> ██║     ███████║███████╗██║██╔██╗ ██║██║   ██║</gradient>")
            add("<gradient:#FFD700:#FF6B6B> ██║     ██╔══██║╚════██║██║██║╚██╗██║██║   ██║</gradient>")
            add("<gradient:#FFD700:#FF6B6B> ╚██████╗██║  ██║███████║██║██║ ╚████║╚██████╔╝</gradient>")
            add("<gradient:#FFD700:#FF6B6B>  ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝╚═╝  ╚═══╝ ╚═════╝</gradient>")
            add("<gray>")
            add("<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            add("<white>  🎰 <gold><bold>Liric Casino</bold></gold>  <gray>•  <yellow>v$v  <gray>•  $platform")
            add("<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            if (foliaLine != null) {
                add(foliaLine)
                add("<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
            add("<green>  ✔ <gray>Economy (Vault)      <green><bold>CONNECTED")
            add("<gray>  ◈ <gray>Roulette             ${gameStatus("roulette")}")
            add("<gray>  ◈ <gray>Blackjack Table      ${gameStatus("blackjack")}")
            add("<gray>  ◈ <gray>Poker Table          ${gameStatus("poker")}")
            add("<gray>  ◈ <gray>777 Slots Machine    ${gameStatus("slots")}")
            add("<gray>  ◈ <gray>Scratch Card         ${gameStatus("scratch")}")
            add("<gray>  ◈ <gray>Lottery              ${gameStatus("lottery")}")
            add("<gray>  ◈ <gray>CoinFlip PvP         ${gameStatus("coinflip")}")
            add("<gray>  ◈ <gray>Rock Paper Scissors  ${gameStatus("rps")}")
            add("<gray>  ◈ <gray>Tic Tac Toe PvP      ${gameStatus("ttt")}")
            add("<gray>  ◈ <gray>Horse Racing         ${gameStatus("racing")}")
            add("<gray>  ◈ <gray>Configurable Taxes   $taxStatus")
            add("<green>  ✔ <gray>Database & Stats     <green><bold>CONNECTED")
            add("<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            add("<gradient:#4facfe:#00f2fe>  THE ULTIMATE CASINO IS OPEN! 🎲</gradient>")
            add("<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            add("<gray>")
        }
        lines.forEach { server.consoleSender.sendMessage(format(it)) }
    }
}
