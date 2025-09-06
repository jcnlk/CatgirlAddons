package catgirlroutes.utils.autop3.actions

import catgirlroutes.module.impl.dungeons.LavaClip
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName

@TypeName("lavaclip")
class LavaClipRing(val distance: Double) : RingAction() {
//    override val description: String = "lava clips with a specified depth"
//    override val aliases: List<String> = listOf("lc", "vclip", "clip")

    override fun execute(ring: Ring) {
        LavaClip.lavaClipToggle(distance)
    }
}
