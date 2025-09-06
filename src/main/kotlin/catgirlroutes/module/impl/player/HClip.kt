package catgirlroutes.module.impl.player

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.MovementUpdateEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.SelectorSetting
import catgirlroutes.module.settings.impl.StringSetting
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.MovementUtils.jump
import catgirlroutes.utils.MovementUtils.restartMovement
import catgirlroutes.utils.MovementUtils.stopMovement
import catgirlroutes.utils.Notifications
import catgirlroutes.utils.PlayerUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.cos
import kotlin.math.sin

object HClip : Module(
    "HClip",
    Category.PLAYER,
    "Boosts you forward when key bind is pressed"
){
    private val shouldJump by BooleanSetting("Auto jump", false, "Makes hclip automatically jump if on ground.")
    private val feedback by BooleanSetting("Keybind feedback", true)
    private val feedbackType by SelectorSetting("Type", "Notification", arrayListOf("Notification", "Message")).withDependency { feedback }

    private val dingdingding by BooleanSetting("Play sound", false)

    private val soundOptions = arrayListOf(
        "note.pling",
        "mob.blaze.hit",
        "fire.ignite",
        "random.orb",
        "random.break",
        "mob.guardian.land.hit",
        "Custom"
    )
    private val soundSelector by SelectorSetting("Sound", soundOptions[0], soundOptions, "Sound Selection").withDependency { dingdingding }
    private val customSound by StringSetting("Custom Sound", soundOptions[0], placeholder = "Sound name", description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting.").withDependency { dingdingding && soundSelector.selected == "Custom" }
    private val pitch by NumberSetting("Pitch", 1.0, 0.1, 2.0, 0.1).withDependency { dingdingding }

    private var pendingHClip = false
    private var yawToUse: Float? = null

    override fun onKeyBind() {
        if (!this.enabled) return
        if (feedback) when (feedbackType.index) {
            0 -> Notifications.send("Hclipping")
            1 -> modMessage("Hclipping")
        }
        hClip()
    }

    fun hClip(yaw: Float = mc.thePlayer.rotationYaw) {
        stopMovement()
        if (mc.thePlayer.onGround && shouldJump) jump()
        yawToUse = yaw
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
        pendingHClip = true
    }

    @SubscribeEvent
    fun onMovementUpdate(event: MovementUpdateEvent.Pre) {
        if (pendingHClip) {
            val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
            val radians = yawToUse!! * Math.PI / 180 // todo: MathUtils?
            val x = -sin(radians) * speed
            val z = cos(radians) * speed

            if (dingdingding) PlayerUtils.playLoudSound(getSound(), 100f, pitch.toFloat())
            mc.thePlayer.motionX = x
            mc.thePlayer.motionZ = z
            restartMovement()

            pendingHClip = false
        }
    }
    /**
    * Returns the sound from the selector setting, or the custom sound when the last element is selected
    */
    private fun getSound(): String {
        return if (soundSelector.index < soundSelector.options.size - 1)
            soundSelector.selected
        else
            customSound
    }
}