package catgirlroutes.utils.autop3.arguments

import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.autop3.actions.LookRing
import catgirlroutes.utils.TypeName

@TypeName("look")
data object LookArgument : RingArgument() {

    override fun check(ring: Ring): Boolean {
        LookRing.execute(ring)
        return true
    }
}
