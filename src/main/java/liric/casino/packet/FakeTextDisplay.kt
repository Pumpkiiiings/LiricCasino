package liric.casino.packet

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Transformation

class FakeTextDisplay(
    initialLocation: Location,
    var text: Component
) : FakeEntity(EntityTypes.TEXT_DISPLAY, initialLocation) {

    var transformation: Transformation? = null
    var interpolationDuration: Int = 0
    var interpolationDelay: Int = 0
    
    var billboard: Display.Billboard = Display.Billboard.CENTER
    var backgroundColor: Int = 0
    var isShadowed: Boolean = true

    fun updateText(newText: Component) {
        this.text = newText
        for (player in viewers) {
            sendTextMetadata(player)
        }
    }

    fun updateTransformation(transformation: Transformation, interpolationDuration: Int = 0, interpolationDelay: Int = 0) {
        this.transformation = transformation
        this.interpolationDuration = interpolationDuration
        this.interpolationDelay = interpolationDelay
        
        for (player in viewers) {
            sendTransformationMetadata(player)
        }
    }

    override fun sendMetadataPacket(player: Player) {
        val dataList = mutableListOf<EntityData<*>>()
        
        // Byte 0: Flags
        dataList.add(EntityData(0, EntityDataTypes.BYTE, 0.toByte()))

        // Transformation
        transformation?.let {
            dataList.add(EntityData(8, EntityDataTypes.INT, interpolationDelay))
            dataList.add(EntityData(9, EntityDataTypes.INT, interpolationDuration))
            dataList.add(EntityData(11, EntityDataTypes.VECTOR3F, it.translation.toPE()))
            dataList.add(EntityData(12, EntityDataTypes.VECTOR3F, it.scale.toPE()))
            dataList.add(EntityData(13, EntityDataTypes.QUATERNION, it.leftRotation.toPE()))
            dataList.add(EntityData(14, EntityDataTypes.QUATERNION, it.rightRotation.toPE()))
        }

        // Billboard
        val billboardByte: Byte = when (billboard) {
            Display.Billboard.FIXED -> 0
            Display.Billboard.VERTICAL -> 1
            Display.Billboard.HORIZONTAL -> 2
            Display.Billboard.CENTER -> 3
        }
        dataList.add(EntityData(15, EntityDataTypes.BYTE, billboardByte))

        // Text Component (Index 23)
        // PacketEvents 2.x supports Adventure components directly through ADV_COMPONENT if available
        try {
            // Using ADV_COMPONENT
            val field = EntityDataTypes::class.java.getField("ADV_COMPONENT")
            val advComponentType = field.get(null)
            @Suppress("UNCHECKED_CAST")
            dataList.add(EntityData(23, advComponentType as com.github.retrooper.packetevents.protocol.entity.data.EntityDataType<Component>, text))
        } catch (e: Exception) {
            // Fallback if ADV_COMPONENT is not defined: serialize to JSON
            val json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(text)
            dataList.add(EntityData(23, EntityDataTypes.STRING, json))
        }

        // Background Color (Index 25)
        dataList.add(EntityData(25, EntityDataTypes.INT, backgroundColor))

        // Bitmask for Shadow (Index 27)
        var bitmask: Byte = 0
        if (isShadowed) {
            bitmask = (bitmask.toInt() or 0x01).toByte()
        }
        dataList.add(EntityData(27, EntityDataTypes.BYTE, bitmask))

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

    private fun sendTextMetadata(player: Player) {
        val dataList = mutableListOf<EntityData<*>>()
        try {
            val field = EntityDataTypes::class.java.getField("ADV_COMPONENT")
            val advComponentType = field.get(null)
            @Suppress("UNCHECKED_CAST")
            dataList.add(EntityData(23, advComponentType as com.github.retrooper.packetevents.protocol.entity.data.EntityDataType<Component>, text))
        } catch (e: Exception) {
            val json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(text)
            dataList.add(EntityData(23, EntityDataTypes.STRING, json))
        }
        val metadata = WrapperPlayServerEntityMetadata(entityId, dataList)
        PacketEvents.getAPI().playerManager.sendPacket(player, metadata)
    }
}
