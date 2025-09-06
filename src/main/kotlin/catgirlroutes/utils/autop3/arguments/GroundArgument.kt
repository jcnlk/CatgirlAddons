package catgirlroutes.utils.autop3.arguments

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("onGround")
data object GroundArgument : RingArgument() {
//    override val description: String = "executes the ring when the player is on the ground"
//    override val aliases: List<String> = listOf("ground")

    override fun check(ring: Ring): Boolean {
        return mc.thePlayer.onGround
    }
}
