package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.AlwaysActive
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.NumberSetting
import catgirlroutes.module.settings.impl.SelectorSetting
import catgirlroutes.utils.equalsOneOf
import catgirlroutes.utils.render.WorldRenderUtils.drawLine
import catgirlroutes.utils.render.WorldRenderUtils.renderManager
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

@AlwaysActive
object Trail: Module(
    "Trail",
    Category.RENDER
){

    private var trailTicks by NumberSetting("Ticks", 20.0, 1.0, 400.0, unit = "t")
    private var trailStyle by SelectorSetting("Style", "Color", arrayListOf("Color", "Trans", "Lesbian"))
    private var trailColor by ColorSetting("Color", Color.PINK).withDependency { trailStyle.selected == "Color"}
    private var segmentSize by NumberSetting("Segment size", 3.0, 2.0, 10.0).withDependency { trailStyle.selected.equalsOneOf("Trans", "Lesbian")}
    private var trailThickness by NumberSetting("Thickness", 3.0, 1.0, 5.0)
    private var trailStay by BooleanSetting("Stay", true)
    private var trailPhase by BooleanSetting("Phase", false)
    private var trailTrail by ActionSetting("Reset Trail") {
        posToRender.clear()
    }

    private var posToRender = mutableListOf<Vec3>()

    private val transColors = mutableListOf(Color(91, 206, 250), Color(245, 169, 184), Color(255, 255, 255), Color(245, 169, 184)) // todo: move to ColorUtil prob
    private val lesbianColors = mutableListOf(Color(213, 45, 0), Color(239, 118, 39), Color(255, 154, 86), Color(255, 255, 255), Color(209, 98, 164), Color(181, 86, 144), Color(163, 2, 98))

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        posToRender.clear()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null || !this.enabled) return
        if (!hasMoved() && trailStay) return
        posToRender.add(Vec3(renderManager.viewerPosX, renderManager.viewerPosY + 0.05, renderManager.viewerPosZ))
        if (posToRender.size.toDouble() == trailTicks * 2) posToRender.removeFirst()
        if (posToRender.size > trailTicks * 2) posToRender.clear()
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (trailStyle.selected == "Trans") {
            for (i in 0 until posToRender.size - 1) {
                val colorIndex = (i / segmentSize.toInt()) % transColors.size
                drawLine(
                    posToRender[i],
                    posToRender[i + 1],
                    transColors[colorIndex],
                    trailThickness.toFloat(),
                    trailPhase
                )
            }
        } else if (trailStyle.selected == "Lesbian") {
            for (i in 0 until posToRender.size - 1) {
                val colorIndex = (i / segmentSize.toInt()) % lesbianColors.size
                drawLine(
                    posToRender[i],
                    posToRender[i + 1],
                    lesbianColors[colorIndex],
                    trailThickness.toFloat(),
                    trailPhase
                )
            }
        } else if (trailStyle.selected == "Color") {
            for (i in 0 until posToRender.size - 1) {
                drawLine(
                    posToRender[i],
                    posToRender[i + 1],
                    trailColor,
                    trailThickness.toFloat(),
                    trailPhase
                )
            }
        }
    }

    private fun hasMoved(): Boolean {
        return mc.thePlayer.posZ != mc.thePlayer.prevPosZ || mc.thePlayer.posY != mc.thePlayer.prevPosY || mc.thePlayer.posX != mc.thePlayer.prevPosX
    }
}