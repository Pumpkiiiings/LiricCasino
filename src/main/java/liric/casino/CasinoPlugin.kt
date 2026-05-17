package liric.casino

import liric.casino.blackjack.*
import liric.casino.coinflip.CoinFlipCommand
import liric.casino.coinflip.CoinFlipManager
import liric.casino.commands.CasinoCommand
import liric.casino.config.MenuConfig
import liric.casino.config.MessagesConfig
import liric.casino.database.DatabaseManager
import liric.casino.economy.EconomyManager
import liric.casino.lottery.LotteryCommand
import liric.casino.lottery.LotteryManager
import liric.casino.poker.PokerCommand
import liric.casino.poker.PokerGame
import liric.casino.poker.PokerInteractListener
import liric.casino.poker.PokerManager
import liric.casino.roulette.RouletteCommand
import liric.casino.roulette.RouletteGame
import liric.casino.roulette.RouletteInteractListener
import liric.casino.roulette.RouletteManager
import liric.casino.roulette.RouletteMenu
import liric.casino.scratch.ScratchCommand
import liric.casino.scratch.ScratchListener
import liric.casino.scratch.TicketTier
import liric.casino.slots.SlotCommand
import liric.casino.slots.SlotInteractListener
import liric.casino.slots.SlotManager
import liric.casino.stats.CasinoPlaceholders
import liric.casino.stats.StatsListener
import liric.casino.stats.StatsManager
import liric.casino.util.ColorUtil
import liric.casino.webhook.WebhookManager
import liric.casino.rps.RPSManager
import liric.casino.rps.RPSCommand
import liric.casino.tictactoe.TTTManager
import liric.casino.tictactoe.TTTCommand
import liric.casino.racing.RaceManager
import liric.casino.racing.RaceCommand
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class CasinoPlugin : JavaPlugin() {

    // ─── Configs ─────────────────────────────────────────────────────────
    lateinit var messages: MessagesConfig
    private val menuConfigMap = mutableMapOf<String, MenuConfig>()

    // ─── Economía ────────────────────────────────────────────────────────
    lateinit var economyManager: EconomyManager

    // ─── Ruleta ──────────────────────────────────────────────────────────
    lateinit var rouletteManager: RouletteManager
    lateinit var rouletteGame: RouletteGame
    lateinit var rouletteMenu: RouletteMenu

    // ─── Poker ───────────────────────────────────────────────────────────
    lateinit var pokerManager: PokerManager
    lateinit var pokerGame: PokerGame

    // ─── Tragamonedas ────────────────────────────────────────────────────
    lateinit var slotManager: SlotManager

    // ─── Blackjack ───────────────────────────────────────────────────────
    lateinit var blackjackManager: BlackjackManager
    lateinit var blackjackMultiGame: BlackjackMultiGame

    // ─── Lotería ─────────────────────────────────────────────────────────
    lateinit var lotteryManager: LotteryManager

    // ─── CoinFlip ────────────────────────────────────────────────────────
    lateinit var coinFlipManager: CoinFlipManager

    // ─── RPS & TTT & Racing ──────────────────────────────────────────────
    lateinit var rpsManager: RPSManager
    lateinit var tttManager: TTTManager
    lateinit var raceManager: RaceManager

    // ─── Webhooks ─────────────────────────────────────────────────────────
    lateinit var webhook: WebhookManager

    // ─── Base de Datos y Estadísticas ────────────────────────────────────
    lateinit var db: DatabaseManager
    lateinit var statsManager: StatsManager

    // ─── Utilidades ──────────────────────────────────────────────────────
    fun format(text: String): Component = ColorUtil.parse(text)

    fun menuConfig(name: String): MenuConfig =
        menuConfigMap[name] ?: error("MenuConfig '$name' not loaded!")

    // ═════════════════════════════════════════════════════════════════════
    override fun onEnable() {
        // 1. Config principal
        saveDefaultConfig()

        // 1b. TicketTier para Rasca y Gana
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

        // 4. Economía (Vault)
        economyManager = EconomyManager(this)
        if (!economyManager.setupVault()) {
            logger.severe("¡Vault no encontrado o no hay plugin de economía! Deshabilitando...")
            server.pluginManager.disablePlugin(this)
            return
        }

        // 4b. Base de Datos y Stats
        db = DatabaseManager(this)
        db.connect()
        statsManager = StatsManager(this)
        statsManager.startAutoSave()

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            CasinoPlaceholders(this).register()
        }

        // 5. Managers y lógica de juego
        // --- Ruleta ---
        rouletteManager = RouletteManager(this)
        rouletteGame    = RouletteGame(this)
        rouletteMenu    = RouletteMenu(this, rouletteGame)
        rouletteManager.loadRoulettes()

        // --- Poker ---
        pokerManager = PokerManager(this)
        pokerGame    = PokerGame(this)

        // --- Tragamonedas ---
        slotManager = SlotManager(this)
        slotManager.loadMachines()

        // --- Blackjack ---
        blackjackManager  = BlackjackManager(this)
        blackjackMultiGame = BlackjackMultiGame(this)

        // --- Lotería ---
        lotteryManager = LotteryManager(this)
        lotteryManager.start()

        // --- CoinFlip ---
        coinFlipManager = CoinFlipManager(this)

        // --- Nuevos Juegos ---
        rpsManager = RPSManager(this)
        tttManager = TTTManager(this)
        raceManager = RaceManager(this)

        // 6. Comandos
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
        server.pluginManager.registerEvents(liric.casino.coinflip.CoinFlipChatListener(this), this)
        server.pluginManager.registerEvents(liric.casino.listeners.PlayerQuitListener(this), this)

        // 8. Mensaje de inicio
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
            "<red>  Deshabilitando sistemas y limpiando entidades...\n" +
            "<red>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        ))
    }

    // ─── Mensaje de inicio ────────────────────────────────────────────────
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
            "<green>  ✔ <gray>Economía (Vault)     <green><bold>CONECTADO",
            "<green>  ✔ <gray>Ruleta               <green><bold>ACTIVA",
            "<green>  ✔ <gray>Mesa de Blackjack    <green><bold>ACTIVA",
            "<green>  ✔ <gray>Mesa de Poker        <green><bold>ACTIVA",
            "<green>  ✔ <gray>Máquina 777          <green><bold>ACTIVA",
            "<green>  ✔ <gray>Rasca y Gana         <green><bold>ACTIVO",
            "<green>  ✔ <gray>Lotería              <green><bold>ACTIVA",
            "<green>  ✔ <gray>CoinFlip PvP         <green><bold>ACTIVO",
            "<green>  ✔ <gray>Piedra Papel Tijera  <green><bold>ACTIVO",
            "<green>  ✔ <gray>Tic Tac Toe PvP      <green><bold>ACTIVO",
            "<green>  ✔ <gray>Carreras Caballos    <green><bold>ACTIVAS",
            "<green>  ✔ <gray>Taxes Configurables  <green><bold>ACTIVOS",
            "<green>  ✔ <gray>Database & Stats     <green><bold>CONECTADO",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<gradient:#4facfe:#00f2fe>  ¡EL CASINO DEFINITIVO ESTÁ ABIERTO! 🎲</gradient>",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<gray>"
        )
        lines.forEach { server.consoleSender.sendMessage(format(it)) }
    }
}
