package liric.casino.packet

import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.util.Quaternion4f
import org.joml.Vector3f as JVector3f
import org.joml.Quaternionf as JQuaternionf

fun JVector3f.toPE() = Vector3f(x, y, z)
fun JQuaternionf.toPE() = Quaternion4f(x, y, z, w)
