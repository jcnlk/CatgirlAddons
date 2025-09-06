package catgirlroutes.utils.autop3.actions

import catgirlroutes.module.impl.dungeons.LavaClip
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("lavaclip")
class LavaClipRing(val distance: Double) : RingAction() {

    override fun execute(ring: Ring) {
        LavaClip.lavaClipToggle(distance)
    }
}
