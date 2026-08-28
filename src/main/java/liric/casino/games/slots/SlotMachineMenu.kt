package liric.casino.games.slots

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import liric.casino.CasinoPlugin
import liric.casino.core.BaseMenu
import liric.casino.util.SchedulerUtil
import liric.casino.util.ValidationUtil
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import java.util.concurrent.atomic.AtomicBoolean

class SlotMachineMenu(
    plugin: CasinoPlugin, 
    private val player: Player, 
    private val machineLoc: Location
) : BaseMenu(plugin, "slots.yml") {

    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    private val minBet = plugin.config.getDouble("slots.bet.default.min", 100.0)
    private val maxBet = plugin.economyManager.getMaxBet(player, "slots")
    private var currentBet = minBet

    private val isSpinning = AtomicBoolean(false)
    private val registry get() = plugin.slotManager.registry

    fun open() {
        val gui = buildGui()
        gui.disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        gui.setCloseGuiAction { plugin.slotManager.freeMachine(machineLoc) }

        resetMachineVisuals(gui)
        gui.open(player)
    }

    override fun setupItems(gui: Gui) {
        // Handled in resetMachineVisuals and updateBottomRow since this is dynamic
    }

    private fun updateBottomRow(gui: Gui) {
        if (isSpinning.get()) {
            val spinMat  = config.getMaterial("spinning-indicator.material", Material.BARRIER)
            val spinName = config.getComponent("spinning-indicator.name")
            val locked = ItemBuilder.from(spinMat).name(spinName).asGuiItem()
            listOf(20, 21, 22, 23, 24).forEach { gui.setItem(it, locked) }
            gui.update()
            return
        }

        listOf("minus-big", "minus-small", "plus-small", "plus-big").forEach { key ->
            val slot   = config.getInt("buttons.$key.slot")
            val mat    = config.getMaterial("buttons.$key.material", Material.STONE)
            val label  = config.getString("buttons.$key.label")
            val amount = config.getDouble("buttons.$key.amount")
            val color  = config.getString("buttons.$key.color")
            gui.setItem(slot, createBetButton(gui, mat, label, amount, color))
        }

        val customSlot = config.getInt("buttons.custom-bet.slot", 4)
        val customMat  = config.getMaterial("buttons.custom-bet.material", Material.PAPER)
        val customName = config.getComponent("buttons.custom-bet.name", "<#FFD700>Custom Bet")

        val customBtn = ItemBuilder.from(customMat)
            .name(customName)
            .lore(plugin.format(plugin.messagesConfig.getString("slots-extra.custom-bet-lore", "<gray>Click to type amount in chat")!!))
            .flags(*ItemFlag.values())
            .asGuiItem {
                gui.close(player)
                player.sendMessage(plugin.messages.get("slots.write-bet-chat"))

                plugin.economyManager.openCustomBetChat(player) { amount ->
                    if (amount in minBet..maxBet) {
                        currentBet = amount
                        open()
                    } else {
                        val msg = plugin.messagesConfig.getString("slots-extra.invalid-amount", "<red>Invalid amount. Must be between {min} and {max}</red>")!!
                            .replace("{min}", minBet.toString()).replace("{max}", maxBet.toString())
                        player.sendMessage(plugin.format(msg.replace("{prefix}", plugin.messagesConfig.getString("prefix", "")!!)))
                    }
                }
            }
        gui.setItem(customSlot, customBtn)

        val spinSlot = config.getInt("buttons.spin.slot", 22)
        val spinMat  = config.getMaterial("buttons.spin.material", Material.TRIPWIRE_HOOK)
        val spinName = config.getComponent("buttons.spin.name")
        val spinLore1 = plugin.format(config.getString("buttons.spin.lore-bet").replace("\${amount}", currentBet.toString()))
        val spinLore2 = plugin.format(config.getString("buttons.spin.lore-empty", ""))
        val spinLore3 = config.getComponent("buttons.spin.lore-tip")

        val spinBtn = ItemBuilder.from(spinMat).name(spinName).lore(spinLore1, spinLore2, spinLore3)
            .flags(*ItemFlag.values()).asGuiItem {
                if (!ValidationUtil.canPlayDaily(plugin, player, "slots")) return@asGuiItem
                if (!ValidationUtil.validateBet(plugin, player, "slots", currentBet)) return@asGuiItem

                if (!isSpinning.compareAndSet(false, true)) return@asGuiItem
                if (plugin.economyManager.withdrawPlayer(player, currentBet).transactionSuccess()) {
                    plugin.statsManager.recordGameUse(player.uniqueId, "slots")
                    plugin.statsManager.recordSlotSpin(player.uniqueId, currentBet)
                    startSpin(gui)
                } else {
                    isSpinning.set(false)
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    player.sendMessage(msg("slots.no-funds"))
                }
            }
        gui.setItem(spinSlot, spinBtn)
        gui.update()
    }

    private fun createBetButton(gui: Gui, material: Material, label: String, amount: Double, color: String): GuiItem {
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
                else { currentBet = newBet; player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f); updateBottomRow(gui) }
            }
    }

    private fun startSpin(gui: Gui) {
        updateBottomRow(gui)

        val hideMat = config.getMaterial("buttons.restart.hide-material", Material.BLACK_STAINED_GLASS_PANE)
        gui.setItem(26, ItemBuilder.from(hideMat).name(plugin.format(" ")).asGuiItem())
        gui.update()

        val reelSlots = config.getStringList("reel.slots").map { it.toInt() }
            .takeIf { it.size == 3 } ?: listOf(12, 13, 14)

        var reel1 = registry.getRandomItem(player)
        var reel2 = registry.getRandomItem(player)
        var reel3 = registry.getRandomItem(player)

        var ticks = 0
        var spinCancel: Runnable? = null
        spinCancel = SchedulerUtil.runGlobalTimer(plugin, 0L, 1L) {
            if (ticks < 20) { reel1 = registry.getRandomItem(player); updateReel(gui, reelSlots[0], reel1) }
            if (ticks < 40) { reel2 = registry.getRandomItem(player); updateReel(gui, reelSlots[1], reel2) }
            if (ticks < 60) {
                reel3 = registry.getRandomItem(player); updateReel(gui, reelSlots[2], reel3)
                if (ticks % 2 == 0) player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 2f)
            }
            if (ticks == 20) player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f)
            if (ticks == 40) player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f)
            if (ticks == 60) {
                player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 0.8f, 1f)
                checkWin(reel1, reel2, reel3)
                isSpinning.set(false)
                updateBottomRow(gui)
                spawnRestartShard(gui)
                spinCancel?.run()
            }
            ticks++
        }
    }

    private fun updateReel(gui: Gui, slot: Int, prize: SlotPrize) {
        val item = ItemBuilder.from(prize.material).name(plugin.format(prize.displayName)).flags(*ItemFlag.values()).asGuiItem()
        gui.updateItem(slot, item)
    }

    private fun checkWin(r1: SlotPrize, r2: SlotPrize, r3: SlotPrize) {
        if (r1 == r2 && r2 == r3) {
            if (r1.multiplier > 0) {
                val winAmount = currentBet * r1.multiplier
                
                // Using standard processWin isn't applicable since we don't store 1 session.
                // But we can manually deposit and tax.
                val (netWin, tax) = liric.casino.util.TaxUtil.applyTax(plugin, winAmount, "slots")
                plugin.economyManager.depositPlayer(player, netWin)
                
                val isJackpot = r1.material == Material.ENCHANTED_GOLDEN_APPLE
                plugin.statsManager.recordSlotWin(player.uniqueId, netWin, isJackpot)
                if (isJackpot) {
                    plugin.server.broadcast(msg("slots.jackpot", "player" to player.name, "amount" to netWin.toString()))
                    player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                    plugin.webhook.sendJackpot("Slots 777", player.name, netWin)
                } else {
                    player.sendMessage(msg("slots.win", "mult" to r1.multiplier.toString(), "amount" to netWin.toString()))
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                    plugin.webhook.sendBigWin("Slots 777", player.name, netWin)
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

    private fun spawnRestartShard(gui: Gui) {
        val restartSlot = config.getInt("buttons.restart.slot", 26)
        val restartMat  = config.getMaterial("buttons.restart.material", Material.AMETHYST_SHARD)
        val restartName = config.getComponent("buttons.restart.name")
        val restartLore = config.getComponentList("buttons.restart.lore")

        val shard = ItemBuilder.from(restartMat).name(restartName).lore(restartLore)
            .flags(*ItemFlag.values()).asGuiItem {
                player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f)
                resetMachineVisuals(gui)
                val hideMat = config.getMaterial("buttons.restart.hide-material", Material.BLACK_STAINED_GLASS_PANE)
                gui.setItem(26, ItemBuilder.from(hideMat).name(plugin.format(" ")).asGuiItem())
                gui.update()
            }
        gui.setItem(restartSlot, shard)
        gui.update()
    }

    private fun resetMachineVisuals(gui: Gui) {
        val reelSlots  = config.getStringList("reel.slots").map { it.toInt() }
            .takeIf { it.size == 3 } ?: listOf(12, 13, 14)
        val emptyMat   = config.getMaterial("reel.empty-material", Material.BLACK_CONCRETE)
        val emptyName  = config.getComponent("reel.empty-name")
        val reelBase   = ItemBuilder.from(emptyMat).name(emptyName).asGuiItem()
        reelSlots.forEach { gui.setItem(it, reelBase) }
        updateBottomRow(gui)
    }
}
