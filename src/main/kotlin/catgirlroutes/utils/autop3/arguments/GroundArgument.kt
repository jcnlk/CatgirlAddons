package catgirlroutes.utils.autop3.arguments

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("onGround")
data object GroundArgument : RingArgument() {

    override fun check(ring: Ring): Boolean {
        return mc.thePlayer.onGround
    }
}
