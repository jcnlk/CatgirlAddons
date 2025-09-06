package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("motion")
data object MotionRing : RingAction() {
//    override val description: String = "modifies player motion to move"
//    override val aliases: List<String> = listOf("velo", "velocity")

    override fun execute(ring: Ring) {
        MovementUtils.stopMovement()
        MovementUtils.motion(ring.yaw)
    }
}
