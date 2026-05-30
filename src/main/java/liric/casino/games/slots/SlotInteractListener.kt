package liric.casino.games.slots

import liric.casino.CasinoPlugin
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class SlotInteractListener(private val plugin: CasinoPlugin) : Listener {

    @EventHandler
    fun onClickMachine(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.LEFT_CLICK_BLOCK) return

        val machine = plugin.slotManager.getMachine(block.location) ?: return
        event.isCancelled = true

        val player = event.player
        if (!plugin.isGameEnabled("slots")) {
            player.sendMessage(plugin.messages.get("general.game-disabled"))
            return
        }

        // Verificar ocupación
        if (machine.occupant != null && machine.occupant != player.uniqueId) {
            val occupantPlayer = Bukkit.getPlayer(machine.occupant!!)

            // Si el ocupante está desconectado o cerró el menú, liberamos la máquina
            if (occupantPlayer == null || !occupantPlayer.isOnline || !occupantPlayer.openInventory.title.contains("MÁQUINA 777")) {
                plugin.slotManager.freeMachine(block.location)
            } else {
                // ¡ESTÁ OCUPADA! Lo empujamos hacia atrás
                val pushDir = player.location.toVector().subtract(block.location.toVector())
                if (pushDir.lengthSquared() > 0) {
                    pushDir.normalize().multiply(1.2).setY(0.3)
                    player.velocity = pushDir
                }

                player.sendActionBar(plugin.format("<#FF5555><bold>¡Esta máquina ya está siendo usada!</bold></#FF5555>"))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                return
            }
        }

        // Si está libre, la ocupamos y abrimos el menú
        plugin.slotManager.occupyMachine(block.location, player.uniqueId)
        SlotMachineMenu(plugin, player, block.location).open()
    }
}
