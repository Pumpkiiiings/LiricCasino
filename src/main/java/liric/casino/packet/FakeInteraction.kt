package liric.casino.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import org.bukkit.Location
import org.bukkit.entity.Player

class FakeInteraction(
    initialLocation: Location,
    var width: Float = 1.0f,
    var height: Float = 1.0f
) : FakeEntity(EntityTypes.INTERACTION, initialLocation) {

    fun updateSize(width: Float, height: Float) {
        this.width = width
        this.height = height
        for (player in viewers) {
            sendMetadataPacket(player)
        }
    }

    override fun sendMetadataPacket(player: Player) {
        val dataList = mutableListOf<EntityData<*>>()
        
        // Byte 0: Flags
        dataList.add(EntityData(0, EntityDataTypes.BYTE, 0.toByte()))

        // Interaction dimensions (1.20.4 indices)
        // 8: Width (Float)
        // 9: Height (Float)
        // 10: Responsive (Boolean)
        dataList.add(EntityData(8, EntityDataTypes.FLOAT, width))
        dataList.add(EntityData(9, EntityDataTypes.FLOAT, height))
        dataList.add(EntityData(10, EntityDataTypes.BOOLEAN, true))

        val metadata = WrapperPlayServerEntityMetadata(entityId, dataList)
        PacketEvents.getAPI().playerManager.sendPacket(player, metadata)
    }
}
