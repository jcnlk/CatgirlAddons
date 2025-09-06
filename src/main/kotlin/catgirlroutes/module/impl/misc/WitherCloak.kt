package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.ChatPacket
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.HudSetting
import catgirlroutes.module.settings.impl.StringSetting
import catgirlroutes.ui.clickgui.util.FontUtil.drawStringWithShadow
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.utils.dungeon.DungeonUtils.getMageCooldownMultiplier
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityCreeper
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

object WitherCloak: Module(
    "Wither Cloak",
    Category.MISC
){

    private val enabledText by StringSetting("Title", "some title", description = "Title to show when wither cloak is active")
    private val textColor by ColorSetting("Title color", Color.PINK, false)
    private val timer by BooleanSetting("Timer", "Shows the cooldown timer")
    private val hideCloak by BooleanSetting("Hide cloak", "Hides creepers around the player")
    private val hud by HudSetting {
        width { enabledText.getWidth() }
        height { fontHeight }
        render {
            if (inCloak) {
                drawStringWithShadow(enabledText, 0.0, 0.0, textColor.rgb)
                return@render
            }
            val currentTime = System.currentTimeMillis()
            if (currentTime < lastCloak + cloakCd && timer) {
                val timeLeft = (lastCloak + cloakCd - currentTime) / 1000.0
                val timeText = "%.2f".format(timeLeft)
                val xPad = (enabledText.getWidth() - timeText.getWidth()) / 2
                drawStringWithShadow(timeText, xPad, 0.0, textColor.rgb)
            }
        }
    }

    private var inCloak = false
    private var lastCloak: Long = 0
    private var cloakCd: Long = 0

    @SubscribeEvent
    fun onChat(event: ChatPacket) {
        when(event.message) {
            "Creeper Veil Activated!" -> inCloak = true
            "Creeper Veil De-activated!" -> disableCloak(5000L)
            "Creeper Veil De-activated! (Expired)",
            "Not enough mana! Creeper Veil De-activated!" -> disableCloak(10000L)
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        inCloak = false
        lastCloak = 0
        cloakCd = 0
    }

    @SubscribeEvent
    fun onPreRenderEntity(event: RenderLivingEvent.Pre<EntityLivingBase>) {
        if (mc.thePlayer == null || event.entity !is EntityCreeper || !hideCloak) return
        val creeper = event.entity as EntityCreeper
        if (creeper.health != 20f || !creeper.isInvisible || !creeper.powered || mc.thePlayer.getDistanceToEntity(creeper) > 6f) return
        mc.theWorld.removeEntity(creeper)
        event.isCanceled = true
    }

    private fun disableCloak(cd: Long) {
        inCloak = false
        lastCloak = (System.currentTimeMillis() * getMageCooldownMultiplier()).toLong()
        cloakCd = cd
    }
}