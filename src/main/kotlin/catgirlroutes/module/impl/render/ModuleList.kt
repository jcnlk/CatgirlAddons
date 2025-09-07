package catgirlroutes.module.impl.render

import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.ModuleManager.modules
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.module.settings.impl.ColorSetting
import catgirlroutes.module.settings.impl.HudSetting
import catgirlroutes.ui.clickgui.util.FontUtil.drawStringWithShadow
import catgirlroutes.ui.clickgui.util.FontUtil.fontHeight
import catgirlroutes.ui.clickgui.util.FontUtil.getWidth
import catgirlroutes.utils.render.HUDRenderUtils.renderRect
import catgirlroutes.utils.render.HUDRenderUtils.sr
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

object ModuleList : Module(
    "Array List",
    Category.RENDER,
    "Displays a list of enabled modules"
) {
    private val textColour by ColorSetting("Text colour", Color.PINK, false, collapsible = false)
    private val renderLine by BooleanSetting("Render line", "Renders a line on the side.")
    private val renderBackground by BooleanSetting("Render background", "Renders a background box behind each entry.")
    val hud by HudSetting {
        visibleIf { activeModuleList.isNotEmpty() }
        render {
            width = 150.0 * scale
            height = activeModuleList.size * (fontHeight + 2.0)
            val isLeft = x + width / 2 < sr.scaledWidth / 2
            val isTop = y + height / 2 < sr.scaledHeight / 2

            activeModuleList.sortedBy { it.getWidth() * if (isTop) -1 else 1 }
                .forEachIndexed { i, module ->
                    val w = module.getWidth()
                    val xOffset = if (isLeft) 4.0 else width - w
                    val stickX = if (isLeft) 0.0 else width

                    GlStateManager.pushMatrix()
                    if (renderBackground) {
                        renderRect(xOffset - 2.0, i * 11.0 - 2.0, w + 2.0, fontHeight + 2.0, Color(0, 0, 0, 128))
                    }
                    drawStringWithShadow(module, xOffset, i * (fontHeight + 2.0), textColour.rgb)
                    if (renderLine) renderRect(stickX, i * 11.0 - 2.0, 2.0, fontHeight + 2.0, textColour)
                    GlStateManager.popMatrix()
                }
        }
    }
    
    private val activeModuleList: List<String>
        get() = modules.mapNotNull {
            if (it.enabled && it.name != this.name && it.showInArrayList) it.name else null
        }
}
