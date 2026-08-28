package liric.casino.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.Optional
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

abstract class FakeEntity(
    val entityType: EntityType,
    initialLocation: Location
) {
    companion object {
        private val ENTITY_ID_COUNTER = AtomicInteger(2000000)
    }

    val entityId: Int = ENTITY_ID_COUNTER.incrementAndGet()
    val uuid: UUID = UUID.randomUUID()
    var location: Location = initialLocation.clone()
        protected set
    val world: World = initialLocation.world!!
    val viewers = mutableSetOf<Player>()

    fun show(player: Player) {
        if (viewers.add(player)) {
            sendSpawnPacket(player)
            sendMetadataPacket(player)
        }
    }

    fun hide(player: Player) {
        if (viewers.remove(player)) {
            val destroy = WrapperPlayServerDestroyEntities(entityId)
            PacketEvents.getAPI().playerManager.sendPacket(player, destroy)
        }
    }

    fun hideAll() {
        val currentViewers = viewers.toList()
        currentViewers.forEach { hide(it) }
    }

    protected open fun sendSpawnPacket(player: Player) {
        val spawn = WrapperPlayServerSpawnEntity(
            entityId,
            Optional.of(uuid),
            entityType,
            Vector3d(location.x, location.y, location.z),
            location.pitch,
            location.yaw,
            location.yaw,
            0,
            Optional.empty()
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, spawn)
    }

    abstract fun sendMetadataPacket(player: Player)

    open fun teleport(newLoc: Location) {
        this.location = newLoc.clone()
        val teleport = WrapperPlayServerEntityTeleport(
            entityId,
            Vector3d(location.x, location.y, location.z),
            location.yaw,
            location.pitch,
            false
        )
        for (player in viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(player, teleport)
        }
    }
}
