package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("jump")
data object JumpRing : RingAction() {

    override fun execute(ring: Ring) {
        MovementUtils.jump()
    }
}
