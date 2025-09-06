package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("motion")
data object MotionRing : RingAction() {

    override fun execute(ring: Ring) {
        MovementUtils.stopMovement()
        MovementUtils.motion(ring.yaw)
    }
}
