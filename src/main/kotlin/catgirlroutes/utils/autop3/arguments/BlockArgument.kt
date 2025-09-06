package catgirlroutes.utils.autop3.arguments

import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import catgirlroutes.utils.rotation.RotationUtils
import net.minecraft.util.Vec3

@TypeName("block")
class BlockArgument(val vec: Vec3) : RingArgument() {
//    override val description: String = "looks at specified block instead of yaw and pitch"

    override fun check(ring: Ring): Boolean {
        val (y, p) = RotationUtils.getYawAndPitch(vec)
        ring.yaw = y
        ring.pitch = p
        return true
    }
}
