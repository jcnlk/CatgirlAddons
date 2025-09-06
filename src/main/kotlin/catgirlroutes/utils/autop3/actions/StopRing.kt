package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("stop")
class StopRing(val full: Boolean = false) : RingAction() {
//    override val description get() = if (!full) "sets player's velocity to 0" else "fully stops the player "

    override fun execute(ring: Ring) {
        MovementUtils.stopVelo()
        if (full) MovementUtils.stopMovement()
    }
}
