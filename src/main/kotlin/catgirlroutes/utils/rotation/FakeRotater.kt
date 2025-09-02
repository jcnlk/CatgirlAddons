package catgirlroutes.utils.rotation

import catgirlroutes.events.impl.MotionUpdateEvent
import catgirlroutes.utils.PlayerUtils.airClick
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

open class Rotater {
    companion object {
        var rotater: IRotater? = null
        var pitch: Float = 0f
        var yaw: Float = 0f
        var fakeRotating: Boolean = false
        var fakeClicking: Boolean = false
        var shouldClick: Boolean = false

        @SubscribeEvent
        fun onMotionUpdatePre(event: MotionUpdateEvent.Pre) {
            if (fakeRotating) {
                event.yaw = yaw
                event.pitch = pitch
                if (shouldClick) {
                    shouldClick = false
                    fakeClicking = true
                }
            }
        }

        @SubscribeEvent
        fun onMotionUpdatePost(event: MotionUpdateEvent.Post) {
            if (fakeClicking) {
                airClick()
                fakeRotating = false
                fakeClicking = false
            }
        }
    }
}

object FakeRotater : Rotater(), IRotater { // useless ass shit
    override fun rotate(yaw: Float, pitch: Float) {
        Companion.yaw = yaw
        Companion.pitch = pitch
        fakeRotating = true
    }

    override fun clickAt(yaw: Float, pitch: Float) {
        Companion.yaw = yaw
        Companion.pitch = pitch
        fakeRotating = true
        shouldClick = true
    }

    fun stopRotating() {
        fakeRotating = false
    }
}

interface IRotater {
    fun rotate(yaw: Float, pitch: Float)
    fun clickAt(yaw: Float, pitch: Float)
}