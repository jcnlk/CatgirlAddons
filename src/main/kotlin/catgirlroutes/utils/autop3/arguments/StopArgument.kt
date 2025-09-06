package catgirlroutes.utils.autop3.arguments

import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.autop3.actions.StopRing
import catgirlroutes.utils.TypeName

@TypeName("stop")
class StopArgument(val full: Boolean = false) : RingArgument() {

    override fun check(ring: Ring): Boolean {
        StopRing(full).execute(ring)
        return true
    }
}
