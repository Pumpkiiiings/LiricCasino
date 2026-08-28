package liric.casino.packet

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerInteractFakeEntityEvent(
    val player: Player,
    val fakeEntity: FakeEntity,
    val action: WrapperPlayClientInteractEntity.InteractAction
) : Event(!Bukkit.isPrimaryThread()) {
    
    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList = HANDLER_LIST
}

class FakeEntityPacketListener : PacketListenerAbstract() {

    override fun onPacketReceive(event: PacketReceiveEvent) {
        try {
            if (event.packetType == PacketType.Play.Client.INTERACT_ENTITY) {
                val interact = WrapperPlayClientInteractEntity(event)
                val entityId = interact.entityId
                val player = Bukkit.getPlayer(event.user.uuid) ?: return

                var fakeEntity: FakeEntity? = null
                for (entity in EntityTracker.entities) {
                    if (entity.entityId == entityId) {
                        fakeEntity = entity
                        break
                    }
                }
                
                if (fakeEntity != null) {
                    // Firing event so our RouletteInteractListener can handle it
                    val customEvent = PlayerInteractFakeEntityEvent(player, fakeEntity, interact.action)
                    Bukkit.getPluginManager().callEvent(customEvent)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}
