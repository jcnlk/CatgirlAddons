package catgirlroutes.module.impl.misc

import catgirlroutes.module.Module
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.events.impl.PacketSentEvent
import catgirlroutes.module.settings.impl.HudSetting
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.awt.Color

object BuffDisplay : Module(
    name = "Buff Display"
){
    var reaperTime = 0.0
    var ragTime = 0.0
    var endermanTime = 0.0
    var tubaTime = 0.0
    var soBHTime = 0.0 //Sword of Bad Health
    var endstoneSwordTime = 0.0
    var thisY = 0f
    var enderman = false
    var readyForS08 = false
    private val hud by HudSetting{
        size(mc.fontRendererObj.getStringWidth("Enderman: 10.0") + 6,
            (mc.fontRendererObj.FONT_HEIGHT + 2) * 3)
        render {
            if (!BuffDisplay.enabled) return@render
            thisY = 0f
            render(ragTime / 20.0, "Ragnarock: " )
            render(reaperTime / 20.0, "Enrage: " )
            render(endermanTime / 20.0, "Teleport Savvy: " )
            render(tubaTime / 20.0, "Howl: ")
            render(soBHTime / 20.0, "Bad Health: ")
            render(endstoneSwordTime / 20.0, "Extreme Focus: ")
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketReceiveEvent) {
        if (event.packet is S29PacketSoundEffect) {
            if (event.packet.soundName == "mob.wolf.howl") {
                if (mc.thePlayer.heldItem?.displayName?.contains("Ragnarock Axe") == true && event.packet.pitch == 1.4920635f) {
                    ragTime = 200.0
                }
                if (mc.thePlayer.heldItem?.displayName?.contains("Tuba") ?: return) {
                    tubaTime = 600.0
                }
            }
            if (event.packet.soundName == "mob.zombie.remedy" && mc.thePlayer?.inventory?.armorInventory?.get(2)?.displayName?.contains("Reaper") ?: return) {
                reaperTime = 120.0
            }
        }
        if (event.packet is S08PacketPlayerPosLook) {
            if (readyForS08) {
                endermanTime = 100.0
                readyForS08 = false
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val msg = event.message.unformattedText
        when {
            "You summoned your" in msg || "Autopet" in msg -> enderman = msg.contains("Enderman")
            "You despawned your Enderman" in msg -> enderman = false
        }

        if (msg.contains("You buffed yourself for")) {
            soBHTime = 100.0
        }
        if (msg.startsWith("Used Extreme Focus!")) {
            endstoneSwordTime = 100.0
        }
    }

    @SubscribeEvent
    fun onPacketSent(event: PacketSentEvent) {
        if (event.packet !is C08PacketPlayerBlockPlacement) return
        if (event.packet.stack?.displayName?.contains("Aspect of the Void") == true && enderman) {
            readyForS08 = true
        }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        reaperTime -= 1
        ragTime -= 1
        endermanTime -= 1
        tubaTime -= 1
        soBHTime -= 1
        endstoneSwordTime -= 1
    }

    fun render(time: Double, type: String) {
        if (time <= 0) return
        val formattedTime = String.format("%.2f", time)
        mc.fontRendererObj.drawStringWithShadow("§l$type$formattedTime", 6.0F, thisY, Color.PINK.rgb)
        thisY += 10f
    }
}