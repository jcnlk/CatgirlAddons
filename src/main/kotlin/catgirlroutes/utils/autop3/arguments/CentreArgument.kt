package catgirlroutes.utils.autop3.arguments

import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("centre")
data object CentreArgument : RingArgument() {

    override fun check(ring: Ring): Boolean {
        return ring.position.xCoord == posX && ring.position.zCoord == posZ
    }
}
