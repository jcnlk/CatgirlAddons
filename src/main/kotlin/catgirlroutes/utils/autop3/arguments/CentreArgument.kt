package catgirlroutes.utils.autop3.arguments

import catgirlroutes.utils.PlayerUtils.posX
import catgirlroutes.utils.PlayerUtils.posZ
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("centre")
data object CentreArgument : RingArgument() {
//    override val description: String = "executes the ring when the player is in the middle of it"
//    override val aliases: List<String> = listOf("center", "middle")

    override fun check(ring: Ring): Boolean {
        return ring.position.xCoord == posX && ring.position.zCoord == posZ
    }
}
