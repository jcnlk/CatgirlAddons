package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.utils.ChatUtils.createClickableText
import catgirlroutes.utils.ChatUtils.getPrefix
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.configList
import catgirlroutes.utils.render.WorldRenderUtils.drawEntityBox
import catgirlroutes.utils.render.WorldRenderUtils.drawTracer
import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.utils.GreedyString
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.EntityInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.abs

object CustomHighlight : Module(
    name = "Custom Highlight",
    category = Category.RENDER
) {
    private val hlColor by ColorSetting("Color", Color.PINK)
    private val tracer by BooleanSetting("Tracer", false)

    private val highlightList by configList<Highlight>("highlight.json")

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        val entities = mc.theWorld.loadedEntityList
        entities.forEach{e ->
            if (e == mc.thePlayer) return@forEach
            val highlightMatch = highlightList.find { e.name.contains(it.tag, true)}

            if (highlightMatch != null) {
                drawEntityBox(e, highlightMatch.color, highlightMatch.color, true, false, event.partialTicks, 4.0f)
                if (tracer) {
                    drawTracer(e.positionVector, Color.PINK)
                }
            }
        }
    }

    @SubscribeEvent
    fun onInteract(event: EntityInteractEvent) {
        if (!active) return
        mc.theWorld.loadedEntityList.forEach{e ->
            if (
                abs(e.posX - event.entity.posX) <= 3.0 &&
                abs(e.posY - event.entity.posY) <= 3.0 &&
                abs(e.posZ - event.entity.posZ) <= 3.0
            ) {
                val name = e.name
                if (e.name == mc.thePlayer.name) return@forEach
                createClickableText(getPrefix() + "Copy " + name, "Click to copy to clipboard!", "/hl schizophrenia ${name}")
                createClickableText(getPrefix() + "Add " + name, "Click to add to the list!", "/hl add ${name}")
            }
        }
    }

    private var active = false

    override fun onKeyBind() {
        active = !active
        modMessage("Tagging is now ${if (active) "active" else "inactive"}!")
    }

    val highlightCommands = Commodore("hl") {
        literal("add").runs {
            tag: GreedyString ->
            highlightList.add(Highlight(tag.toString(), hlColor))
            modMessage("Added $tag to the list!")
        }
        literal("clear").runs {
            highlightList.clear()
            modMessage("List cleared!")
        }
        literal("remove").runs {
            tag: GreedyString ->
            highlightList.removeAll{h ->
                tag.toString() == (h.tag)
            }
            highlightList.save()
            modMessage("Removed ${tag} from the list!")
        }
        literal("list").runs {
            highlightList.forEach{h ->
                modMessage(h.tag)
            }
        }
        literal("schizophrenia").runs {
            text: GreedyString ->
            GuiScreen.setClipboardString(text.toString())
        }
    }
}

data class Highlight(val tag: String, val color: Color)