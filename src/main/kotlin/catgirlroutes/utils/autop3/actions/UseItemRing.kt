package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import catgirlroutes.utils.rotation.FakeRotater

@TypeName("item")
class UseItemRing(val itemName: String) : RingAction() {
//    override val description: String = "uses bonzo staff" // currently bonzo only
//    override val aliases: List<String> = listOf("bonzo")

    override fun execute(ring: Ring) {
        swapFromName(itemName) {
            FakeRotater.clickAt(ring.yaw, ring.pitch)
        }
    }
}
