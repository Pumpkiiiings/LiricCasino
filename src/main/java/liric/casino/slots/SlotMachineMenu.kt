package liric.casino.slots

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import liric.casino.CasinoPlugin
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class SlotMachineMenu(private val plugin: CasinoPlugin, private val player: Player, private val machineLoc: Location) {

    private val cfg  get() = plugin.menuConfig("slots.yml")
    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    private val minBet = plugin.config.getDouble("slots.bet.min", 100.0)
    private val maxBet = plugin.config.getDouble("slots.bet.max", 100000.0)
    private var currentBet = minBet
    // FIX RACE: AtomicBoolean para evitar doble spin
    private val isSpinning = AtomicBoolean(false)

    // SlotRegistry cargado por SlotManager
    private val registry get() = plugin.slotManager.registry

    private val gui = Gui.gui()
        .title(cfg.getComponent("title"))
        .rows(cfg.getInt("rows", 3))
        .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        .create()

    init {
        gui.setCloseGuiAction { plugin.slotManager.freeMachine(machineLoc) }
    }

    fun open() {
        cfg.applyDecorations(gui)
        resetMachineVisuals()
        gui.open(player)
    }

    private fun updateBottomRow() {
        if (isSpinning.get()) {
            val spinMat  = cfg.getMaterial("spinning-indicator.material", Material.BARRIER)
            val spinName = cfg.getComponent("spinning-indicator.name")
            val locked = ItemBuilder.from(spinMat).name(spinName).asGuiItem()
            listOf(20, 21, 22, 23, 24).forEach { gui.setItem(it, locked) }
            gui.update()
            return
        }

        // Botones de ajuste
        listOf("minus-big", "minus-small", "plus-small", "plus-big").forEach { key ->
            val slot   = cfg.getInt("buttons.$key.slot")
            val mat    = cfg.getMaterial("buttons.$key.material", Material.STONE)
            val label  = cfg.getString("buttons.$key.label")
            val amount = cfg.getDouble("buttons.$key.amount")
            val color  = cfg.getString("buttons.$key.color")
            gui.setItem(slot, createBetButton(mat, label, amount, color))
        }

        // SPIN button
        val spinSlot = cfg.getInt("buttons.spin.slot", 22)
        val spinMat  = cfg.getMaterial("buttons.spin.material", Material.TRIPWIRE_HOOK)
        val spinName = cfg.getComponent("buttons.spin.name")
        val spinLore1 = plugin.format(cfg.getString("buttons.spin.lore-bet").replace("\${amount}", currentBet.toString()))
        val spinLore2 = plugin.format(cfg.getString("buttons.spin.lore-empty", ""))
        val spinLore3 = cfg.getComponent("buttons.spin.lore-tip")

        val spinBtn = ItemBuilder.from(spinMat).name(spinName).lore(spinLore1, spinLore2, spinLore3)
            .flags(*ItemFlag.values()).asGuiItem {
                // FIX: compareAndSet evita doble-spin por click spam
                if (!isSpinning.compareAndSet(false, true)) return@asGuiItem
                if (plugin.economyManager.withdrawPlayer(player, currentBet).transactionSuccess()) {
                    plugin.statsManager.recordSlotSpin(player.uniqueId, currentBet)
                    startSpin()
                } else {
                    isSpinning.set(false)
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    player.sendMessage(msg("slots.no-funds"))
                }
            }
        gui.setItem(spinSlot, spinBtn)
        gui.update()
    }

    private fun createBetButton(material: Material, label: String, amount: Double, color: String): dev.triumphteam.gui.guis.GuiItem {
        return ItemBuilder.from(material)
            .name(plugin.format("$color<bold>$label</bold>")).flags(*ItemFlag.values())
            .asGuiItem {
                val maxBalance = plugin.economyManager.vault?.getBalance(player) ?: 0.0
                var newBet = currentBet + amount
                if (newBet < minBet) newBet = minBet
                if (newBet > maxBet) newBet = maxBet
                if (newBet > maxBalance) {
                    if (maxBalance >= minBet) newBet = maxBalance
                    else {
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        player.sendMessage(msg("slots.no-funds-general"))
                        return@asGuiItem
                    }
                }
                if (newBet == currentBet) player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                else { currentBet = newBet; player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f); updateBottomRow() }
            }
    }

    private fun startSpin() {
        // isSpinning ya está en true por compareAndSet
        updateBottomRow()

        gui.setItem(26, dev.triumphteam.gui.builder.item.ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(plugin.format(" ")).asGuiItem())
        gui.update()

        val reelSlots = cfg.getStringList("reel.slots").map { it.toInt() }
            .takeIf { it.size == 3 } ?: listOf(12, 13, 14)

        var reel1 = registry.getRandomItem(player)
        var reel2 = registry.getRandomItem(player)
        var reel3 = registry.getRandomItem(player)

        object : BukkitRunnable() {
            var ticks = 0
            override fun run() {
                if (ticks < 20) { reel1 = registry.getRandomItem(player); updateReel(reelSlots[0], reel1) }
                if (ticks < 40) { reel2 = registry.getRandomItem(player); updateReel(reelSlots[1], reel2) }
                if (ticks < 60) {
                    reel3 = registry.getRandomItem(player); updateReel(reelSlots[2], reel3)
                    if (ticks % 2 == 0) player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f)
                }
                if (ticks == 20) player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f)
                if (ticks == 40) player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f)
                if (ticks == 60) {
                    player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 0.8f, 1f)
                    checkWin(reel1, reel2, reel3)
                    isSpinning.set(false)
                    updateBottomRow()
                    spawnRestartShard()
                    cancel()
                }
                ticks++
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    private fun updateReel(slot: Int, prize: SlotPrize) {
        val item = ItemBuilder.from(prize.material).name(plugin.format(prize.displayName)).flags(*ItemFlag.values()).asGuiItem()
        gui.updateItem(slot, item)
    }

    private fun checkWin(r1: SlotPrize, r2: SlotPrize, r3: SlotPrize) {
        if (r1 == r2 && r2 == r3) {
            if (r1.multiplier > 0) {
                val winAmount = currentBet * r1.multiplier
                plugin.economyManager.depositPlayer(player, winAmount)
                val isJackpot = r1.material == Material.ENCHANTED_GOLDEN_APPLE
                plugin.statsManager.recordSlotWin(player.uniqueId, winAmount, isJackpot)
                if (isJackpot) {
                    plugin.server.broadcast(msg("slots.jackpot", "player" to player.name, "amount" to winAmount.toString()))
                    player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                    plugin.webhook.sendJackpot("Tragamonedas 777", player.name, winAmount)
                } else {
                    player.sendMessage(msg("slots.win", "mult" to r1.multiplier.toString(), "amount" to winAmount.toString()))
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                    plugin.webhook.sendBigWin("Tragamonedas 777", player.name, winAmount)
                }
            } else {
                player.sendMessage(msg("slots.trash"))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            }
        } else {
            player.sendMessage(msg("slots.no-luck"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }
    }

    private fun spawnRestartShard() {
        val restartSlot = cfg.getInt("buttons.restart.slot", 26)
        val restartMat  = cfg.getMaterial("buttons.restart.material", Material.AMETHYST_SHARD)
        val restartName = cfg.getComponent("buttons.restart.name")
        val restartLore = cfg.getComponentList("buttons.restart.lore")

        val shard = ItemBuilder.from(restartMat).name(restartName).lore(restartLore)
            .flags(*ItemFlag.values()).asGuiItem {
                player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f)
                resetMachineVisuals()
                gui.setItem(26, dev.triumphteam.gui.builder.item.ItemBuilder.from(Material.BLACK_STAINED_GLASS_PANE).name(plugin.format(" ")).asGuiItem())
                gui.update()
            }
        gui.setItem(restartSlot, shard)
        gui.update()
    }

    private fun resetMachineVisuals() {
        val reelSlots  = cfg.getStringList("reel.slots").map { it.toInt() }
            .takeIf { it.size == 3 } ?: listOf(12, 13, 14)
        val emptyMat   = cfg.getMaterial("reel.empty-material", Material.BLACK_CONCRETE)
        val emptyName  = cfg.getComponent("reel.empty-name")
        val reelBase   = ItemBuilder.from(emptyMat).name(emptyName).asGuiItem()
        reelSlots.forEach { gui.setItem(it, reelBase) }
        updateBottomRow()
    }
}
