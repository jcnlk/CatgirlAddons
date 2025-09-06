package catgirlroutes.module.impl.dungeons

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.CatgirlRoutes.Companion.scope
import catgirlroutes.events.impl.TermOpenEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.Setting.Companion.withDependency
import catgirlroutes.module.settings.impl.*
import catgirlroutes.utils.*
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.autop3.Ring
import catgirlroutes.utils.autop3.RingsManager.currentRoute
import catgirlroutes.utils.autop3.RingsManager.ringEditMode
import catgirlroutes.utils.dungeon.DungeonUtils.floorNumber
import catgirlroutes.utils.render.WorldRenderUtils.drawCustomSizedBoxAt
import catgirlroutes.utils.render.WorldRenderUtils.drawEllipse
import catgirlroutes.utils.render.WorldRenderUtils.drawP3boxWithLayers
import catgirlroutes.utils.render.WorldRenderUtils.renderGayFlag
import catgirlroutes.utils.render.WorldRenderUtils.renderLesbianFlag
import catgirlroutes.utils.render.WorldRenderUtils.renderTransFlag
import kotlinx.coroutines.launch
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color
import java.awt.Color.black
import java.util.Locale


object AutoP3 : Module( // todo make it on tick; fix schizophrenia; add more args
    "Auto P3",
    Category.DUNGEON,
    "A module that allows you to place down rings that execute various actions.",
    tag = TagType.WHIP
) {
    var selectedRoute by StringSetting("Selected route", "1", 0, "Route name(-s)", "Name of the selected route for Auto P3.")
    val inBossOnly by BooleanSetting("Boss only", true, "Active in boss room only.")
    private val editTitle by BooleanSetting("EditMode title", "Renders a title when edit mode is enabled.")
    private val chatFeedback by BooleanSetting("Chat feedback", true, "Sends chat messages when the ring is activated.")
    val legsOffset by NumberSetting("Legs bounding box offset", 0.0, 0.0, 1.0, 0.05, "Offsets bounding box for in ring detection")
    val boomType by SelectorSetting("Boom type", "Regular", arrayListOf("Regular", "Infinity"), "Superboom TNT type to use for BOOM ring.")

    private val style by SelectorSetting("Ring style", "Layers", arrayListOf("Layers", "Box", "Ellipse", "Lesbian", "Gay", "Trans"), "Ring render style to be used.")
    private val layers by NumberSetting("Ring layers amount", 3.0, 1.0, 5.0, 1.0, "Amount of ring layers to render").withDependency { style.selected == "Layers" }
    private val lineThickness by NumberSetting("Line thickness", 2.0, 1.0, 10.0, 1.0, "Ellipse line thickness").withDependency { style.selected.equalsOneOf("Box", "Ellipse") }
    private val ellipseSlices by NumberSetting("Ellipse slices", 30.0, 5.0, 60.0, 1.0, "Ellipse slices").withDependency { style.selected == "Ellipse" }

    private val colour1 by ColorSetting("Ring colour (inactive)", black, true, "Colour of Normal ring style while inactive").withDependency { style.selected.equalsOneOf("Layers", "Ellipse", "Box") }
    private val colour2 by ColorSetting("Ring colour (active)", Color.white, true, "Colour of Normal ring style while active").withDependency { style.selected.equalsOneOf("Layers", "Ellipse", "Box") }

    var termFound = false

    private val visited = mutableListOf<Ring>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null || event.phase != TickEvent.Phase.END || ringEditMode || (inBossOnly && floorNumber != 7)) return
        currentRoute.forEach { route ->
            route.rings.forEach { ring ->
                if (ring.inside() && !visited.contains(ring)) {
                    if (ring.checkArgs()) {
                        if (chatFeedback) modMessage(ring.action.typeName.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() })
                        scope.launch { ring.execute() }
                        visited.add(ring)
                    }
                }
                visited.removeIf { !it.inside() }
            }
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (mc.theWorld == null || (inBossOnly && floorNumber != 7)) return

        currentRoute.forEach { route ->
            route.rings.forEach { ring ->
                val (x, y, z) = ring.position
                val colour = if (ring.inside()) colour2 else colour1
                when (style.selected) {
                    "Layers"    -> drawP3boxWithLayers(x, y, z, ring.length, ring.width, ring.height, colour, layers.toInt())
                    "Box"       -> drawCustomSizedBoxAt(x - ring.width / 2, y + 0.01, z - ring.length / 2, ring.width.toDouble(), ring.height.toDouble() - 0.01, ring.length.toDouble(), colour, lineThickness.toFloat(), false)
                    "Ellipse"   -> drawEllipse(x, y, z, ring.width / 2, ring.length / 2, colour, ellipseSlices.toInt(), lineThickness.toFloat())
                    "Lesbian"   -> renderLesbianFlag(x, y, z, ring.length, ring.width, ring.height)
                    "Gay"       -> renderGayFlag(x, y, z, ring.length, ring.width, ring.height)
                    "Trans"     -> renderTransFlag(x, y, z, ring.length, ring.width, ring.height)
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderGameOverlay(event: RenderGameOverlayEvent.Post) {
        if (!editTitle || (inBossOnly && floorNumber != 7)) return
        renderText(
            when {
                ringEditMode -> "Edit Mode"
                else -> return
            }
        )
    }

    @SubscribeEvent
    fun onTerm(event: TermOpenEvent) {
        termFound = true
        scheduleTask(2) {
            termFound = false
        }
    }
}
