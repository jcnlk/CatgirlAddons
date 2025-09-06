package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("walk")
data object WalkRing : RingAction() {

    override fun execute(ring: Ring) {
        MovementUtils.stopMovement()
        MovementUtils.setKey("w", true)
    }
}
