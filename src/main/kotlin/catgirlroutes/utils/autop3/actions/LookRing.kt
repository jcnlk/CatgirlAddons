package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import catgirlroutes.utils.rotation.RotationUtils

@TypeName("look")
data object LookRing : RingAction() {
//    override val description: String = "turns player's head"
//    override val aliases: List<String> = listOf("rotate")

    override fun execute(ring: Ring) {
        RotationUtils.snapTo(ring.yaw, ring.pitch)
    }
}
