package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import catgirlroutes.utils.rotation.RotationUtils

@TypeName("look")
data object LookRing : RingAction() {

    override fun execute(ring: Ring) {
        RotationUtils.snapTo(ring.yaw, ring.pitch)
    }
}
