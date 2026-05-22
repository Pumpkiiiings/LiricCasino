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

class CasinoPlugin : JavaPlugin() {

    // ─── Configs ─────────────────────────────────────────────────────────
    lateinit var messages: MessagesConfig
    private val menuConfigMap = mutableMapOf<String, MenuConfig>()

    // ─── Economy ────────────────────────────────────────────────────────
    lateinit var economyManager: EconomyManager

    // ─── Roulette ────────────────────────────────────────────────────────
    lateinit var rouletteManager: RouletteManager
    lateinit var rouletteGame: RouletteGame
    lateinit var rouletteMenu: RouletteMenu

    // ─── Poker ───────────────────────────────────────────────────────────
    lateinit var pokerManager: PokerManager
    lateinit var pokerGame: PokerGame

    // ─── Slots ────────────────────────────────────────────────────────────
    lateinit var slotManager: SlotManager

    // ─── Blackjack ───────────────────────────────────────────────────────
    lateinit var blackjackManager: BlackjackManager
    lateinit var blackjackMultiGame: BlackjackMultiGame

    // ─── Lottery ─────────────────────────────────────────────────────────
    lateinit var lotteryManager: LotteryManager

    // ─── CoinFlip ────────────────────────────────────────────────────────
    lateinit var coinFlipManager: CoinFlipManager

    // ─── RPS & TTT & Racing ──────────────────────────────────────────────
    lateinit var rpsManager: RPSManager
    lateinit var tttManager: TTTManager
    lateinit var raceManager: RaceManager

    // ─── Webhooks ─────────────────────────────────────────────────────────
    lateinit var webhook: WebhookManager

    // ─── Database and Stats ──────────────────────────────────────────────
    lateinit var db: DatabaseManager
    lateinit var statsManager: StatsManager

    // ─── Utilities ──────────────────────────────────────────────────────
    fun format(text: String): Component = ColorUtil.parse(text)

    fun menuConfig(name: String): MenuConfig =
        menuConfigMap[name] ?: error("MenuConfig '$name' not loaded!")

    // ═════════════════════════════════════════════════════════════════════
    override fun onEnable() {
        // 1. Main config
        saveDefaultConfig()

        // 1b. TicketTier for Scratch & Win
        TicketTier.loadFromConfig(config)

        // 2. MessagesConfig
        messages = MessagesConfig(this)
        messages.load()

        // 2b. WebhookManager
        webhook = WebhookManager(this)

        // 3. MenuConfigs
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

        // 4. Economy (Vault)
        economyManager = EconomyManager(this)
        if (!economyManager.setupVault()) {
            logger.severe("Vault not found or no economy plugin! Disabling...")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 4b. Database and Stats
        db = DatabaseManager(this)
        db.connect()
        statsManager = StatsManager(this)
        statsManager.startAutoSave()

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            CasinoPlaceholders(this).register()
        }

        // 5. Managers and game logic
        // --- Roulette ---
        rouletteManager = RouletteManager(this)
        rouletteGame    = RouletteGame(this)
        rouletteMenu    = RouletteMenu(this, rouletteGame)
        rouletteManager.loadRoulettes()

        // --- Poker ---
        pokerManager = PokerManager(this)
        pokerGame    = PokerGame(this)

        // --- Slots ---
        slotManager = SlotManager(this)
        slotManager.loadMachines()

        // --- Blackjack ---
        blackjackManager  = BlackjackManager(this)
        blackjackMultiGame = BlackjackMultiGame(this)

        // --- Lottery ---
        lotteryManager = LotteryManager(this)
        lotteryManager.start()

        // --- CoinFlip ---
        coinFlipManager = CoinFlipManager(this)

        // --- New Games ---
        rpsManager = RPSManager(this)
        tttManager = TTTManager(this)
        raceManager = RaceManager(this)

        // 6. Commands
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

        // 7. Listeners
        server.pluginManager.registerEvents(RouletteInteractListener(this), this)
        server.pluginManager.registerEvents(ScratchListener(this), this)
        server.pluginManager.registerEvents(PokerInteractListener(this), this)
        server.pluginManager.registerEvents(BlackjackInteractListener(this), this)
        server.pluginManager.registerEvents(SlotInteractListener(this), this)
        server.pluginManager.registerEvents(StatsListener(this), this)
        server.pluginManager.registerEvents(CoinFlipChatListener(this), this)
        server.pluginManager.registerEvents(liric.casino.listeners.PlayerQuitListener(this), this)

        // 8. Startup message
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

    // ─── Startup message ────────────────────────────────────────────────
    private fun sendStartupMessage() {
        val v = description.version
        val lines = listOf(
            "<gray>",
            "<gradient:#FFD700:#FF6B6B>  ██████╗ █████╗ ███████╗██╗███╗   ██╗ ██████╗ </gradient>",
            "<gradient:#FFD700:#FF6B6B> ██╔════╝██╔══██╗██╔════╝██║████╗  ██║██╔═══██╗</gradient>",
            "<gradient:#FFD700:#FF6B6B> ██║     ███████║███████╗██║██╔██╗ ██║██║   ██║</gradient>",
            "<gradient:#FFD700:#FF6B6B> ██║     ██╔══██║╚════██║██║██║╚██╗██║██║   ██║</gradient>",
            "<gradient:#FFD700:#FF6B6B> ╚██████╗██║  ██║███████║██║██║ ╚████║╚██████╔╝</gradient>",
            "<gradient:#FFD700:#FF6B6B>  ╚═════╝╚═╝  ╚═╝╚══════╝╚═╝╚═╝  ╚═══╝ ╚═════╝</gradient>",
            "<gray>",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<white>  🎰 <gold><bold>Liric Casino</bold></gold>  <gray>•  <yellow>v$v  <gray>•  <aqua>Paper 1.21.4",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<green>  ✔ <gray>Economy (Vault)      <green><bold>CONNECTED",
            "<green>  ✔ <gray>Roulette             <green><bold>ACTIVE",
            "<green>  ✔ <gray>Blackjack Table      <green><bold>ACTIVE",
            "<green>  ✔ <gray>Poker Table          <green><bold>ACTIVE",
            "<green>  ✔ <gray>777 Slots Machine    <green><bold>ACTIVE",
            "<green>  ✔ <gray>Scratch Card         <green><bold>ACTIVE",
            "<green>  ✔ <gray>Lottery              <green><bold>ACTIVE",
            "<green>  ✔ <gray>CoinFlip PvP         <green><bold>ACTIVE",
            "<green>  ✔ <gray>Rock Paper Scissors  <green><bold>ACTIVE",
            "<green>  ✔ <gray>Tic Tac Toe PvP      <green><bold>ACTIVE",
            "<green>  ✔ <gray>Horse Racing         <green><bold>ACTIVE",
            "<green>  ✔ <gray>Configurable Taxes   <green><bold>ACTIVE",
            "<green>  ✔ <gray>Database & Stats     <green><bold>CONNECTED",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<gradient:#4facfe:#00f2fe>  THE ULTIMATE CASINO IS OPEN! 🎲</gradient>",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<gray>"
        )
        lines.forEach { server.consoleSender.sendMessage(format(it)) }
    }
}
