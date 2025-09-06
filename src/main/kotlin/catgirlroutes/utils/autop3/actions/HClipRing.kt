package catgirlroutes.utils.autop3.actions

import catgirlroutes.module.impl.player.HClip
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("hclip")
data object HClipRing : RingAction() {
    override fun execute(ring: Ring) {
        HClip.hClip(ring.yaw)
    }
}
