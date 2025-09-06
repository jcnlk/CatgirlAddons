package catgirlroutes.utils.autop3.arguments

import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.autop3.actions.StopRing
import catgirlroutes.utils.TypeName

@TypeName("stop")
class StopArgument(val full: Boolean = false) : RingArgument() {
//    override val description get() = if (!full) "sets player's velocity to 0" else "fully stops the player"

    override fun check(ring: Ring): Boolean {
        StopRing(full).execute(ring)
        return true
    }
}
