package liric.casino.scratch

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.ConcurrentHashMap

class ScratchMenu(private val plugin: CasinoPlugin, private val player: Player, private val tier: TicketTier) {

    private val cfg get() = plugin.menuConfig("scratch.yml")
    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    // PrizeRegistry cargado desde config en ScratchManager/plugin
    private val prizeRegistry = PrizeRegistry.fromConfig(plugin.config)

    private val scratchableSlots = tier.getScratchableSlots()
    private val board = List(scratchableSlots.size) { prizeRegistry.getRandomPrize() }
    // FIX DUPE: ConcurrentHashSet + @Volatile para evitar race condition por click spam
    private val scratchedSlots = ConcurrentHashMap.newKeySet<Int>()
    private val revealedCounts = ConcurrentHashMap<ScratchPrize, Int>()
    @Volatile private var locked = false

    fun open() {
        val titleRaw = cfg.getString("title", "<#FFD700><bold>🎟 RASCA Y GANA {tier} 🎟</bold>")
            .replace("{tier}", tier.displayName)
        val gui = Gui.gui()
            .title(plugin.format(titleRaw))
            .rows(tier.rows)
            .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
            .create()

        val fillerMat  = cfg.getMaterial("filler.material", Material.BLACK_STAINED_GLASS_PANE)
        val filler = ItemBuilder.from(fillerMat).name(plugin.format(" ")).asGuiItem()
        for (i in 0 until (tier.rows * 9)) {
            if (i !in scratchableSlots) gui.setItem(i, filler)
        }
        cfg.applyDecorations(gui)

        val scratchMat  = cfg.getMaterial("scratch-block.material", Material.BLACK_CONCRETE)
        val scratchName = cfg.getComponent("scratch-block.name")
        val scratchLore = cfg.getComponentList("scratch-block.lore")
        val missedSuffix = cfg.getString("missed-suffix", " <dark_gray>(Oculto)")
        val closeDelay   = cfg.getInt("close-delay-ticks", 50).toLong()

        scratchableSlots.forEachIndexed { index, slot ->
            val hiddenItem = ItemBuilder.from(scratchMat)
                .name(scratchName).lore(scratchLore).flags(*ItemFlag.values())
                .asGuiItem {
                    // FIX: double-check atómico
                    if (locked || !scratchedSlots.add(slot)) return@asGuiItem

                    val prize = board[index]
                    revealedCounts.merge(prize, 1, Int::plus)

                    val revealedItem = ItemBuilder.from(prize.material)
                        .name(plugin.format(prize.displayName)).flags(*ItemFlag.values()).asGuiItem()
                    gui.updateItem(slot, revealedItem)
                    player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f)
                    checkWinCondition(gui, prize, missedSuffix, closeDelay)
                }
            gui.setItem(slot, hiddenItem)
        }

        gui.open(player)
    }

    private fun checkWinCondition(gui: Gui, lastPrize: ScratchPrize, missedSuffix: String, closeDelay: Long) {
        if ((revealedCounts[lastPrize] ?: 0) == tier.matchRequired) {
            if (!locked) locked = true else return  // FIX: previene doble pago atómicamente
            val totalPayout = lastPrize.basePayout * tier.payoutMultiplier
            if (totalPayout > 0) {
                plugin.economyManager.depositPlayer(player, totalPayout)
                player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                player.sendMessage(msg("scratch.win",
                    "count"  to tier.matchRequired.toString(),
                    "prize"  to lastPrize.displayName,
                    "amount" to totalPayout.toString()
                ))
                plugin.statsManager.recordScratch(player.uniqueId, tier.price, totalPayout, didWin = true)
            } else {
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                player.sendMessage(msg("scratch.win-trash",
                    "count" to tier.matchRequired.toString(),
                    "prize" to lastPrize.displayName
                ))
                plugin.statsManager.recordScratch(player.uniqueId, tier.price, 0.0, didWin = false)
            }
            closeDelayed(gui, missedSuffix, closeDelay)
            return
        }

        if (scratchedSlots.size == scratchableSlots.size) {
            if (!locked) locked = true else return
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            player.sendMessage(msg("scratch.no-prize"))
            plugin.statsManager.recordScratch(player.uniqueId, tier.price, 0.0, didWin = false)
            closeDelayed(gui, missedSuffix, closeDelay)
        }
    }

    private fun closeDelayed(gui: Gui, missedSuffix: String, closeDelay: Long) {
        scratchableSlots.forEachIndexed { index, slot ->
            if (!scratchedSlots.contains(slot)) {
                val prize = board[index]
                val missedItem = ItemBuilder.from(prize.material)
                    .name(plugin.format("${prize.displayName}$missedSuffix")).flags(*ItemFlag.values()).asGuiItem()
                gui.updateItem(slot, missedItem)
            }
        }
        object : BukkitRunnable() { override fun run() { gui.close(player) } }.runTaskLater(plugin, closeDelay)
    }
}
