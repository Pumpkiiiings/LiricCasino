package liric.casino

import liric.casino.config.MenuConfig
import liric.casino.config.MessagesConfig
import liric.casino.economy.EconomyManager
import liric.casino.poker.PokerCommand
import liric.casino.poker.PokerGame
import liric.casino.poker.PokerInteractListener
import liric.casino.poker.PokerManager
import liric.casino.roulettemix.RouletteMixCommand
import liric.casino.roulettemix.RouletteMixGame
import liric.casino.roulettemix.RouletteMixInteractListener
import liric.casino.roulettemix.RouletteMixManager
import liric.casino.roulettemix.RouletteMixMenu
import liric.casino.scratch.ScratchCommand
import liric.casino.scratch.ScratchListener
import liric.casino.scratch.TicketTier
import liric.casino.slots.SlotCommand
import liric.casino.slots.SlotInteractListener
import liric.casino.slots.SlotManager
import liric.casino.blackjack.*
import liric.casino.commands.CasinoCommand
import liric.casino.database.DatabaseManager
import liric.casino.stats.CasinoPlaceholders
import liric.casino.stats.StatsListener
import liric.casino.stats.StatsManager
import liric.casino.util.ColorUtil
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class CasinoPlugin : JavaPlugin() {

    // ─── Configs ─────────────────────────────────────────────────────────
    lateinit var messages: MessagesConfig
    private val menuConfigMap = mutableMapOf<String, MenuConfig>()

    // ─── Economía ────────────────────────────────────────────────────────
    lateinit var economyManager: EconomyManager

    // ─── Ruleta Mixta ────────────────────────────────────────────────────
    lateinit var rouletteMixManager: RouletteMixManager
    lateinit var rouletteMixGame: RouletteMixGame
    private lateinit var rouletteMixMenu: RouletteMixMenu

    // ─── Poker ───────────────────────────────────────────────────────────
    lateinit var pokerManager: PokerManager
    lateinit var pokerGame: PokerGame

    // ─── Tragamonedas ────────────────────────────────────────────────────
    lateinit var slotManager: SlotManager

    // ─── Blackjack ───────────────────────────────────────────────────────
    lateinit var blackjackManager: BlackjackManager
    lateinit var blackjackMultiGame: BlackjackMultiGame

    // ─── Base de Datos y Estadísticas ────────────────────────────────────
    lateinit var db: DatabaseManager
    lateinit var statsManager: StatsManager

    // ─── Acceso al ColorUtil ──────────────────────────────────────────────
    /** Parsea cualquier string con colores Legacy/Hex/MiniMessage. */
    fun format(text: String): Component = ColorUtil.parse(text)

    // ─── Acceso a MenuConfig ──────────────────────────────────────────────
    fun menuConfig(name: String): MenuConfig =
        menuConfigMap[name] ?: error("MenuConfig '$name' not loaded!")

    // ═════════════════════════════════════════════════════════════════════
    override fun onEnable() {
        // 1. Config principal
        saveDefaultConfig()

        // 1b. Cargar TicketTier desde config (debe ser antes de cualquier uso de Scratch)
        TicketTier.loadFromConfig(config)

        // 2. MessagesConfig
        messages = MessagesConfig(this)
        messages.load()

        // 3. MenuConfigs (uno por menú)
        val menuNames = listOf(
            "ruleta.yml", "apuesta_ruleta.yml",
            "blackjack_choice.yml", "blackjack_bet.yml", "blackjack_tutorial.yml",
            "slots.yml", "scratch.yml"
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
        // --- Ruleta Mixta ---
        rouletteMixManager = RouletteMixManager(this)
        rouletteMixGame = RouletteMixGame(this)
        rouletteMixMenu = RouletteMixMenu(this, rouletteMixGame)
        rouletteMixManager.loadRoulettes()

        // --- Poker ---
        pokerManager = PokerManager(this)
        pokerGame = PokerGame(this)

        // --- Tragamonedas ---
        slotManager = SlotManager(this)
        slotManager.loadMachines()

        // --- Blackjack ---
        blackjackManager = BlackjackManager(this)
        blackjackMultiGame = BlackjackMultiGame(this)

        // 6. Comandos
        val rouletteMixCmd = RouletteMixCommand(this, rouletteMixMenu, rouletteMixGame)
        getCommand("ruletamix")?.apply { setExecutor(rouletteMixCmd); tabCompleter = rouletteMixCmd }

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

        // 7. Listeners
        server.pluginManager.registerEvents(RouletteMixInteractListener(this), this)
        server.pluginManager.registerEvents(ScratchListener(this), this)
        server.pluginManager.registerEvents(PokerInteractListener(this), this)
        server.pluginManager.registerEvents(BlackjackInteractListener(this), this)
        server.pluginManager.registerEvents(SlotInteractListener(this), this)
        server.pluginManager.registerEvents(StatsListener(this), this)

        // 8. Mensaje de inicio
        sendStartupMessage()
    }

    override fun onDisable() {
        if (::statsManager.isInitialized) statsManager.shutdown()
        if (::db.isInitialized) db.disconnect()

        if (::rouletteMixManager.isInitialized) rouletteMixManager.cleanupAll()
        if (::pokerManager.isInitialized) pokerManager.cleanupAll()
        if (::blackjackManager.isInitialized) blackjackManager.cleanupAll()
        if (::slotManager.isInitialized) slotManager.cleanupAll()

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
            "<green>  ✔ <gray>Ruleta Mixta VIP      <green><bold>ACTIVA",
            "<green>  ✔ <gray>Mesa de Blackjack     <green><bold>ACTIVA",
            "<green>  ✔ <gray>Mesa de Poker         <green><bold>ACTIVA",
            "<green>  ✔ <gray>Máquina 777           <green><bold>ACTIVA",
            "<green>  ✔ <gray>Rasca y Gana          <green><bold>ACTIVO",
            "<green>  ✔ <gray>Database & Stats      <green><bold>CONECTADO",
            "<green>  ✔ <gray>Messages / Menus      <green><bold>CONFIGURABLES",
            "<green>  ✔ <gray>Colores Legacy+Hex+MM <green><bold>HABILITADOS",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<gradient:#4facfe:#00f2fe>  ¡EL CASINO DEFINITIVO ESTÁ ABIERTO! 🎲</gradient>",
            "<dark_gray>  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "<gray>"
        )
        lines.forEach { server.consoleSender.sendMessage(format(it)) }
    }
}
