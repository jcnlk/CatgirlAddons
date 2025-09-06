package catgirlroutes.utils.autop3.arguments

import catgirlroutes.module.impl.dungeons.AutoP3.termFound
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("term")
data object TermArgument : RingArgument() {

    override fun check(ring: Ring): Boolean {
        return termFound
    }
}
