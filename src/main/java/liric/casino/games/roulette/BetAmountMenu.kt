package liric.casino.games.roulette

import dev.triumphteam.gui.builder.item.ItemBuilder
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import liric.casino.CasinoPlugin
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.text.toLong

class BetAmountMenu(
    private val plugin: CasinoPlugin,
    private val game: RouletteGame,
    private val target: BetType,
    private val player: Player
) {
    private var currentBet = 0.0
    private val processing = AtomicBoolean(false)

    private val absoluteMaxLimit = plugin.economyManager.getMaxBet(player, "roulette")
    private val playerBalance = plugin.economyManager.vault?.getBalance(player) ?: 0.0
    private val maxAllowed = min(playerBalance, absoluteMaxLimit)

    private val cfg get() = plugin.menuConfig("apuesta_ruleta.yml")
    private fun msg(key: String, vararg ph: Pair<String, String>) = plugin.messages.get(key, *ph)

    private val displayTargetName = when (target) {
        is BetType.Number -> "Número ${target.num}"
        is BetType.Color  -> target.color.displayName
    }

    private val colorTheme = when (target) {
        is BetType.Number -> game.getNumberColor(target.num)
        is BetType.Color  -> target.color
    }

    private val gui = Gui.gui()
        .title(plugin.format(cfg.getString("title", "<black><bold>Apuesta ➔ {target}").replace("{target}", displayTargetName)))
        .rows(cfg.getInt("rows", 3))
        .disableAllInteractions().disableItemTake().disableItemSwap().disableItemDrop().disableItemPlace()
        .create()

    fun open() { setupMenu(); gui.open(player) }

    private fun setupMenu() {
        cfg.applyDecorations(gui)
        listOf("minus-big", "minus-medium", "minus-small", "plus-small", "plus-medium", "plus-big").forEach { key ->
            val slot   = cfg.getInt("buttons.$key.slot")
            val mat    = cfg.getMaterial("buttons.$key.material", Material.STONE)
            val label  = cfg.getString("buttons.$key.label")
            val amount = cfg.getDouble("buttons.$key.amount")
            val color  = cfg.getString("buttons.$key.color")
            gui.setItem(slot, createModifyButton(slot, mat, label, amount, color))
        }


        val customSlot = cfg.getInt("buttons.custom-bet.slot", 4)
        val customMat  = cfg.getMaterial("buttons.custom-bet.material", Material.PAPER)
        val customName = cfg.getComponent("buttons.custom-bet.name", "<#FFD700>Cantidad Custom")

        val customBtn = ItemBuilder.from(customMat)
            .name(customName)
            .lore(plugin.format("<gray>Haz clic para escribir el monto en el chat"))
            .flags(*ItemFlag.values())
            .asGuiItem {
                gui.close(player)
                player.sendMessage(plugin.format("<#FFD700><b>✍ Escribe la cantidad exacta que deseas apostar:</b>"))

                plugin.economyManager.openCustomBetChat(player) { amount ->
                    if (amount in 0.0..absoluteMaxLimit) {
                        currentBet = amount
                        setupMenu()
                        gui.open(player)
                    } else {
                        player.sendMessage(plugin.format("<red>Cantidad inválida. El máximo es $absoluteMaxLimit"))
                    }
                }
            }
        gui.setItem(customSlot, customBtn)

        val maxSlot = cfg.getInt("buttons.max-bet.slot", 22)
        val maxMat  = cfg.getMaterial("buttons.max-bet.material", Material.GOLD_BLOCK)
        val maxName = cfg.getComponent("buttons.max-bet.name")

        var allInItem: GuiItem? = null
        allInItem = ItemBuilder.from(maxMat).name(maxName)
            .lore(plugin.format(cfg.getString("buttons.max-bet.lore").replace("\${max}", maxAllowed.toString())))
            .flags(*ItemFlag.values())
            .asGuiItem {
                if (maxAllowed > 0.0) {
                    currentBet = maxAllowed
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                    updateConfirmButton()
                } else {
                    showErrorBarrier(maxSlot, allInItem!!, cfg.getString("error.no-funds-msg"), cfg.getString("error.no-funds-lore"))
                }
            }
        gui.setItem(maxSlot, allInItem)

        val backSlot = cfg.getInt("buttons.back.slot", 18)
        val backMat  = cfg.getMaterial("buttons.back.material", Material.ARROW)
        val backName = cfg.getComponent("buttons.back.name")
        gui.setItem(backSlot, ItemBuilder.from(backMat).name(backName).flags(*ItemFlag.values()).asGuiItem {
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
            RouletteMenu(plugin, game).openBetMenu(player)
        })

        updateConfirmButton()
    }

    private fun createModifyButton(slot: Int, material: Material, label: String, amount: Double, colorTag: String): GuiItem {
        var guiItem: GuiItem? = null
        guiItem = ItemBuilder.from(material)
            .name(plugin.format("$colorTag<bold>$label")).flags(*ItemFlag.values())
            .asGuiItem {
                val newBet = currentBet + amount
                val maxKey = cfg.getString("error.limit-msg").replace("{max}", absoluteMaxLimit.toLong().toString())
                val maxLoreKey = cfg.getString("error.limit-lore").replace("{max}", absoluteMaxLimit.toLong().toString())
                when {
                    newBet in 0.0..maxAllowed -> {
                        currentBet = newBet
                        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                        updateConfirmButton()
                    }
                    newBet > absoluteMaxLimit -> showErrorBarrier(slot, guiItem!!, maxKey, maxLoreKey)
                    newBet > maxAllowed -> showErrorBarrier(slot, guiItem!!, cfg.getString("error.no-funds-msg"), cfg.getString("error.no-funds-lore"))
                    else                      -> showErrorBarrier(slot, guiItem!!, cfg.getString("error.min-msg"), cfg.getString("error.min-lore"))
                }
            }
        return guiItem
    }

    private fun showErrorBarrier(slot: Int, originalItem: GuiItem, title: String, loreStr: String) {
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        val barrier = ItemBuilder.from(Material.BARRIER)
            .name(plugin.format(title)).lore(plugin.format(loreStr)).flags(*ItemFlag.values()).asGuiItem()
        gui.updateItem(slot, barrier)
        object : BukkitRunnable() { override fun run() { gui.updateItem(slot, originalItem) } }.runTaskLater(plugin, 30L)
    }

    private fun updateConfirmButton() {
        val centerMaterial = when (colorTheme) {
            BetColor.RED   -> Material.RED_WOOL
            BetColor.BLACK -> Material.BLACK_WOOL
            BetColor.GREEN -> Material.GREEN_WOOL
        }

        val confirmName = cfg.getComponent("buttons.confirm.confirm-name")
        val targetLore  = plugin.format(cfg.getString("buttons.confirm.lore-target").replace("{target}", displayTargetName))
        val totalLore   = plugin.format(cfg.getString("buttons.confirm.lore-total").replace("{amount}", currentBet.toString()))
        val emptyLore   = plugin.format("")
        val clickLore   = cfg.getComponent("buttons.confirm.lore-action")

        val confirmButton = ItemBuilder.from(centerMaterial)
            .name(confirmName)
            .lore(targetLore, totalLore, emptyLore, clickLore)
            .flags(*ItemFlag.values())
            .asGuiItem {
                if (currentBet > 0.0) {
                    if (!processing.compareAndSet(false, true)) return@asGuiItem
                    gui.close(player)
                    game.addBet(player, target, currentBet)
                } else {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                }
            }

        if (target is BetType.Number) {
            confirmButton.itemStack.amount = if (target.num == 0) 1 else target.num
        }
        gui.updateItem(13, confirmButton)
    }
}
