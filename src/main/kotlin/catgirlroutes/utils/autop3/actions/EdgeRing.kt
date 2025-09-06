package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("edge")
data object EdgeRing : RingAction() {
//    override val description: String = "jumps from block's edge"
//    override val aliases: List<String> = listOf("goon")

    override fun execute(ring: Ring) {
        MovementUtils.edge()
    }
}
