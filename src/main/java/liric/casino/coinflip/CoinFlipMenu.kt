package liric.casino.coinflip

import liric.casino.CasinoPlugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.scheduler.BukkitRunnable
import java.security.SecureRandom
import java.text.NumberFormat
import java.util.Locale

class CoinFlipMenu(
    private val plugin: CasinoPlugin,
    private val session: CoinFlipSession,
    private val creator: Player?,
    private val joiner: Player
) {
    companion object {
        private val SECURE_RANDOM = SecureRandom()

        // ── Parseo de montos con sufijos k/m/b/t ─────────────────────────
        fun parseAmount(input: String): Double? {
            val s = input.trim().lowercase().replace(",", "").replace("$", "").replace(" ", "")
            val suffixes = listOf("t" to 1_000_000_000_000.0, "b" to 1_000_000_000.0, "m" to 1_000_000.0, "k" to 1_000.0)
            for ((suf, mult) in suffixes) {
                if (s.endsWith(suf)) return s.dropLast(1).toDoubleOrNull()?.times(mult)
            }
            return s.toDoubleOrNull()
        }

        fun formatAmount(amount: Double): String {
            return when {
                amount >= 1_000_000_000_000.0 -> "${String.format("%.1f", amount / 1_000_000_000_000.0)}T"
                amount >= 1_000_000_000.0     -> "${String.format("%.1f", amount / 1_000_000_000.0)}B"
                amount >= 1_000_000.0          -> "${String.format("%.1f", amount / 1_000_000.0)}M"
                amount >= 1_000.0              -> "${String.format("%.1f", amount / 1_000.0)}K"
                else -> "$" + NumberFormat.getNumberInstance(Locale.US).format(amount)
            }
        }
    }

    // Tira de 9 slots para la animación (fila 1 del GUI, slots 9-17)
    private val STRIP_SLOTS = intArrayOf(9, 10, 11, 12, 13, 14, 15, 16, 17)
    private val CENTER_SLOT = 13 // slot 4 de la tira (índice 4)

    private fun makeGlass(mat: Material, name: String): ItemStack = ItemStack(mat).also { s ->
        val m = s.itemMeta ?: return@also
        m.displayName(plugin.format(name))
        s.itemMeta = m
    }

    private fun makeSkull(uuid: java.util.UUID, displayName: String, lore: String): ItemStack {
        val skull = ItemStack(Material.PLAYER_HEAD)
        val meta = skull.itemMeta as? SkullMeta ?: return skull
        meta.owningPlayer = Bukkit.getOfflinePlayer(uuid)
        meta.displayName(plugin.format(displayName))
        if (lore.isNotBlank()) meta.lore(listOf(plugin.format(lore)))
        skull.itemMeta = meta
        return skull
    }

    fun startAnimation() {
        // 1. Decidir ganador con SecureRandom ANTES de la animación
        val winnerIsCreator = SECURE_RANDOM.nextBoolean()
        val capturedJoinerId = session.joinerId!!
        val winnerId = if (winnerIsCreator) session.creatorId else capturedJoinerId

        val amountFmt = "$" + NumberFormat.getNumberInstance(Locale.US).format(session.betAmount)

        // 2. Crear inventario (5 filas = 45 slots)
        val inv = Bukkit.createInventory(null, 45, plugin.format(
            "<dark_gray><bold>🪙 CoinFlip</bold></dark_gray>  <dark_gray>|</dark_gray>  <#FFD700>$amountFmt <dark_gray>vs <#FFD700>$amountFmt"
        ))

        val gray    = makeGlass(Material.GRAY_STAINED_GLASS_PANE, "")
        val magenta = makeGlass(Material.MAGENTA_STAINED_GLASS_PANE, "")
        val yellow  = makeGlass(Material.YELLOW_STAINED_GLASS_PANE, "<#FFD700>▼ AQUÍ")

        // Rellenar todo con gris
        for (i in 0 until 45) inv.setItem(i, gray)
        // Fila 0 y 4: magenta
        for (i in 0..8)  inv.setItem(i, magenta)
        for (i in 36..44) inv.setItem(i, magenta)
        // Indicador de posición (encima y debajo del centro de la tira)
        inv.setItem(4,  yellow)
        inv.setItem(40, makeGlass(Material.YELLOW_STAINED_GLASS_PANE, "<#FFD700>▲"))

        // Cabezas de información (fila 2)
        val creatorHead = makeSkull(session.creatorId, "<#FFD700><bold>${session.creatorName}", "<gray>Apuesta: <#FFD700>$amountFmt")
        val joinerHead  = makeSkull(capturedJoinerId, "<#FF5555><bold>${session.joinerName}", "<gray>Apuesta: <#FF5555>$amountFmt")
        inv.setItem(18, creatorHead)
        inv.setItem(26, joinerHead)

        // VS center (slot 22)
        val vsItem = makeGlass(Material.MAGENTA_STAINED_GLASS_PANE, "<#FF00FF><bold>⚔ VS")
        inv.setItem(22, vsItem)

        // Pozo (fila 3, center)
        val potItem = ItemStack(Material.GOLD_INGOT).also { s ->
            val m = s.itemMeta ?: return@also
            m.displayName(plugin.format("<#FFD700><bold>💰 POZO: ${formatAmount(session.betAmount * 2)}"))
            s.itemMeta = m
        }
        inv.setItem(31, potItem)

        // Abrir para ambos
        creator?.openInventory(inv)
        joiner.openInventory(inv)

        // 3. Construir la secuencia de la tira
        // Secuencia larga alternando C/J. Queremos que al final el centro sea el ganador.
        val seq = BooleanArray(80) { it % 2 == 0 } // true = creator
        // Determinar endPos: seq[endPos + 4] == winnerIsCreator
        var endPos = 45
        while (seq[endPos + 4] != winnerIsCreator) endPos++

        var currentPos = 0
        val animTicks = plugin.config.getInt("coinflip.animation-ticks", 80)

        // Cabezas para la tira
        val cHead = makeSkull(session.creatorId, session.creatorName, "")
        val jHead = makeSkull(capturedJoinerId, "<#FF5555>${session.joinerName}", "")

        fun updateStrip() {
            for (i in STRIP_SLOTS.indices) {
                val isCreatorSlot = seq[currentPos + i]
                inv.setItem(STRIP_SLOTS[i], if (isCreatorSlot) cHead.clone() else jHead.clone())
            }
        }

        object : BukkitRunnable() {
            var tick = 0

            override fun run() {
                val progress = tick.toDouble() / animTicks

                // Velocidad de avance (ticks entre cada paso)
                val speed = when {
                    progress < 0.40 -> 1
                    progress < 0.60 -> 2
                    progress < 0.72 -> 3
                    progress < 0.82 -> 5
                    progress < 0.90 -> 8
                    progress < 0.95 -> 13
                    else            -> 20
                }

                if (tick % speed == 0 && currentPos < endPos) {
                    currentPos++
                    updateStrip()
                    // Sonido de tick
                    val pitch = if (progress < 0.8) 1.5f else (1.5f - ((progress.toFloat() - 0.8f) * 3f).coerceAtMost(0.8f))
                    creator?.playSound(creator.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, pitch)
                    joiner.playSound(joiner.location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, pitch)
                }

                tick++

                if (currentPos >= endPos && tick % 20 == 0) {
                    // Animación terminada → mostrar resultado
                    finishAnimation(inv, winnerId, winnerIsCreator, creatorHead, joinerHead, amountFmt)
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    private fun finishAnimation(
        inv: org.bukkit.inventory.Inventory,
        winnerId: java.util.UUID,
        winnerIsCreator: Boolean,
        creatorHead: ItemStack,
        joinerHead: ItemStack,
        amountFmt: String
    ) {
        // Resaltar centro de la tira con el ganador
        val winnerHead = if (winnerIsCreator) creatorHead else joinerHead
        val winnerName = if (winnerIsCreator) session.creatorName else session.joinerName!!

        // Poner marco dorado alrededor del centro
        val gold = makeGlass(Material.GOLD_BLOCK, "<#FFD700>🏆 GANADOR")
        for (slot in intArrayOf(3, 5, 12, 14)) {
            if (slot < inv.size) {
                val g = ItemStack(Material.GOLD_BLOCK).also { s ->
                    val m = s.itemMeta ?: return@also
                    m.displayName(plugin.format("<#FFD700><bold>🏆"))
                    s.itemMeta = m
                }
                inv.setItem(slot, g)
            }
        }
        inv.setItem(13, winnerHead.clone().also { s ->
            val m = s.itemMeta ?: return@also
            m.displayName(plugin.format("<#FFD700><bold>🏆 ${winnerName}"))
            m.lore(listOf(plugin.format("<#00FF7F>¡GANÓ $amountFmt x2!")))
            s.itemMeta = m
        })

        // Sonidos de victoria
        creator?.playSound(creator.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        joiner.playSound(joiner.location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)

        // Fuegos artificiales al ganador
        val winnerPlayer = Bukkit.getPlayer(winnerId)
        winnerPlayer?.let { p ->
            val fw = p.world.spawnEntity(p.location, org.bukkit.entity.EntityType.FIREWORK_ROCKET) as org.bukkit.entity.Firework
            val meta = fw.fireworkMeta
            meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(org.bukkit.Color.fromRGB(255, 215, 0))
                .withFade(org.bukkit.Color.fromRGB(255, 140, 0))
                .with(org.bukkit.FireworkEffect.Type.STAR)
                .trail(true).flicker(true).build())
            meta.power = 1
            fw.fireworkMeta = meta
        }

        // Cerrar GUI y resolver tras 3 segundos
        object : BukkitRunnable() {
            override fun run() {
                creator?.closeInventory()
                joiner.closeInventory()
                plugin.coinFlipManager.resolveGame(session, winnerId)
            }
        }.runTaskLater(plugin, 60L)
    }
}
