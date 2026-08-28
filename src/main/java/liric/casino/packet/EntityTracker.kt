package liric.casino.packet

import liric.casino.CasinoPlugin
import liric.casino.util.SchedulerUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object EntityTracker : Listener {
    val entities: MutableList<FakeEntity> = mutableListOf()

    fun register(entity: FakeEntity) {
        entities.add(entity)
    }

    fun unregister(entity: FakeEntity) {
        entity.hideAll()
        entities.remove(entity)
    }

    fun start(plugin: CasinoPlugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)

        SchedulerUtil.runGlobalTimer(plugin, 20L, 20L) {
            val players = Bukkit.getOnlinePlayers()
            for (entity in entities) {
                val loc = entity.location
                if (loc.world == null) continue

                for (player in players) {
                    if (player.world.uid == loc.world!!.uid && player.location.distanceSquared(loc) <= 48.0 * 48.0) {
                        if (!entity.viewers.contains(player)) {
                            entity.show(player)
                        }
                    } else {
                        if (entity.viewers.contains(player)) {
                            entity.hide(player)
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        for (entity in entities) {
            entity.viewers.remove(event.player)
        }
    }
}
