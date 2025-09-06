package catgirlroutes.utils.autop3.actions

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.utils.MovementUtils
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("align")
data object AlignRing : RingAction() {
//    override val description: String = "aligns the player with the centre of the ring"
//    override val aliases: List<String> = listOf("middle", "centre", "center")

    override fun execute(ring: Ring) {
        if (ring.length <= 1 && ring.width <= 1) {
            mc.thePlayer.setPosition(
                ring.position.xCoord,
                mc.thePlayer.posY,
                ring.position.zCoord
            )
        } else {
            MovementUtils.moveToBlock(ring.position.xCoord, ring.position.zCoord)
        }
    }
}
