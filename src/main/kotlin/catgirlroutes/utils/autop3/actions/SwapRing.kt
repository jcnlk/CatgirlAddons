package catgirlroutes.utils.autop3.actions

import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("swap")
class SwapRing(val itemName: String) : RingAction() {
//    override val description: String = "swaps to a specified item"
    override fun execute(ring: Ring) {
        swapFromName(itemName)
    }
}
