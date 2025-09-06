package catgirlroutes.utils.autop3.actions

import catgirlroutes.module.impl.dungeons.AutoP3.boomType
import catgirlroutes.utils.PlayerUtils.leftClick2
import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.TypeName
import catgirlroutes.utils.rotation.RotationUtils
import net.minecraft.util.Vec3

@TypeName("boom")
class BoomRing(val vec3: Vec3?) : RingAction() {

    override fun execute(ring: Ring) {
        val type = if (boomType.selected == "Regular") "superboom tnt" else "infinityboom tnt"
        swapFromName(type) {
            RotationUtils.snapTo(ring.yaw, ring.pitch)
            leftClick2()
        }
    }
}
