package liric.casino.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.util.Transformation
import org.joml.Vector3f
import org.joml.Quaternionf

class FakeBlockDisplay(
    initialLocation: Location,
    val blockData: BlockData
) : FakeEntity(EntityTypes.BLOCK_DISPLAY, initialLocation) {

    var transformation: Transformation? = null
    var interpolationDuration: Int = 0
    var interpolationDelay: Int = 0

    fun updateTransformation(transformation: Transformation, interpolationDuration: Int = 0, interpolationDelay: Int = 0) {
        this.transformation = transformation
        this.interpolationDuration = interpolationDuration
        this.interpolationDelay = interpolationDelay
        
        // Update all viewers
        for (player in viewers) {
            sendTransformationMetadata(player)
        }
    }

    override fun sendMetadataPacket(player: Player) {
        val dataList = mutableListOf<EntityData<*>>()
        
        // Byte 0: Flags
        dataList.add(EntityData(0, EntityDataTypes.BYTE, 0.toByte()))

        // Display base indices (1.20.4)
        // 10: Interpolation Delay
        // 11: Interpolation Duration
        // 12: Translation
        // 13: Scale
        // 14: Rotation Left
        // 15: Rotation Right
        
        transformation?.let {
            dataList.add(EntityData(8, EntityDataTypes.INT, interpolationDelay))
            dataList.add(EntityData(9, EntityDataTypes.INT, interpolationDuration))
            dataList.add(EntityData(11, EntityDataTypes.VECTOR3F, it.translation.toPE()))
            dataList.add(EntityData(12, EntityDataTypes.VECTOR3F, it.scale.toPE()))
            dataList.add(EntityData(13, EntityDataTypes.QUATERNION, it.leftRotation.toPE()))
            dataList.add(EntityData(14, EntityDataTypes.QUATERNION, it.rightRotation.toPE()))
        }

        // BlockState for 1.20+ is index 23
        try {
            val state = WrappedBlockState.getByString(blockData.asString)
            if (state != null) {
                dataList.add(EntityData(23, EntityDataTypes.BLOCK_STATE, state.globalId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val metadata = WrapperPlayServerEntityMetadata(entityId, dataList)
        PacketEvents.getAPI().playerManager.sendPacket(player, metadata)
    }

    private fun sendTransformationMetadata(player: Player) {
        val dataList = mutableListOf<EntityData<*>>()
        transformation?.let {
            dataList.add(EntityData(8, EntityDataTypes.INT, interpolationDelay))
            dataList.add(EntityData(9, EntityDataTypes.INT, interpolationDuration))
            dataList.add(EntityData(11, EntityDataTypes.VECTOR3F, it.translation.toPE()))
            dataList.add(EntityData(12, EntityDataTypes.VECTOR3F, it.scale.toPE()))
            dataList.add(EntityData(13, EntityDataTypes.QUATERNION, it.leftRotation.toPE()))
            dataList.add(EntityData(14, EntityDataTypes.QUATERNION, it.rightRotation.toPE()))
        }
        val metadata = WrapperPlayServerEntityMetadata(entityId, dataList)
        PacketEvents.getAPI().playerManager.sendPacket(player, metadata)
    }
}
