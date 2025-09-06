package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("stop")
class StopRing(val full: Boolean = false) : RingAction() {

    override fun execute(ring: Ring) {
        MovementUtils.stopVelo()
        if (full) MovementUtils.stopMovement()
    }
}
